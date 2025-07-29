package dku25.chatGraph.api.graph.repository;

import dku25.chatGraph.api.graph.dto.RenameQuestionResponseDTO;
import dku25.chatGraph.api.graph.dto.RenameTopicResponseDTO;
import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import dku25.chatGraph.api.graph.node.QuestionNode;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface QuestionRepository extends Neo4jRepository<QuestionNode, String> {
    // 질문 ID로 소유자 조회
    @Query("MATCH (u:User)-[:OWNS]->(t:Topic)-[:START_CONVERSATION|FOLLOWED_BY*0]->(q:Question {questionId: $questionId}) RETURN u.userId AS ownerId")
    Optional<String> findUserIdByQuestionId(String questionId);

    //  모든 자식노드 조회
    @Query("MATCH (parent:Question {questionId: $parentId})-[:FOLLOWED_BY]->(q:Question) RETURN q")
    List<QuestionNode> findChildrenByParentId(String parentId);

    //  Query Parameter로 질문노드 조회
    @Query("""
            MATCH (q:Question)-[:HAS_ANSWER]->(a:Answer)
            WHERE q.text CONTAINS $keyword
            OPTIONAL MATCH (q)-[:FOLLOWED_BY]->(child:Question)
            RETURN
            q.questionId AS questionId,
            q.text AS questionText,
            q.level AS level,
            a.answerId AS answerId,
            a.text AS answerText,
            q.createdAt AS createdAt,
            COLLECT(child.questionId) AS children
            """)
    List<QuestionAnswerDTO> findQuestionAndAnswerByKeyword(@Param("keyword") String keyword);

    // 부모 - 특정자식노드 관계 설정
    @Modifying
    @Transactional
    @Query("MATCH (parent:Question {questionId: $parentId}), (child:Question {questionId: $childId}) MERGE (parent)-[:FOLLOWED_BY]->(child)")
    void createFollowedByRelation(String parentId, String childId);

    @Query("MATCH (parent:Question)-[:FOLLOWED_BY]->(q:Question {questionId: $currentQuestionId}) return parent")
    QuestionNode getPreviousQuestion(String currentQuestionId);

    @Modifying
    @Transactional
    @Query("MATCH (startNode:Question {questionId: $startNodeId})-[:FOLLOWED_BY*0..]->(descendant:Question) " + "SET descendant.level = descendant.level - 1")
    void recursivelyUpdateLevels(String startNodeId);

    @Modifying
    @Transactional
    @Query("""
            MATCH (parent)-[:FOLLOWED_BY]->(toDelete:Question)
            OPTIONAL MATCH (toDelete)-[:HAS_ANSWER]->(answer:Answer)
            OPTIONAL MATCH (toDelete)-[:FOLLOWED_BY]->(child:Question)
            
            // 자식이 있는 경우, 부모와 자식을 연결
            CALL {
              WITH parent, toDelete
              OPTIONAL MATCH (toDelete)-[:FOLLOWED_BY]->(child:Question)
              WHERE child IS NOT NULL
              MERGE (parent)-[:FOLLOWED_BY]->(child)
            }
            
            // 하위 트리 레벨 업데이트
            WITH toDelete, answer, COLLECT(child) AS children
            MATCH (toDelete)-[:FOLLOWED_BY*]->(descendant:Question)
            SET descendant.level = descendant.level - 1
            
            // 노드와 관계 삭제
            DETACH DELETE toDelete, answer
            """)
    void deleteAndRelink(String questionId);

    // 질문명 수정
    @Modifying
    @Transactional
    @Query("""
              MATCH (q:Question {questionId: $questionId})
              SET q.question = $newQuestionName
              RETURN t
            """)
    RenameQuestionResponseDTO renameQuestion(String questionId, String newQuestionName);

    // 질문 노드(단일, 복수) 복제
    @Modifying
    @Transactional
    @Query("""
            // (1) 부모 레벨, 부모 타입 준비
            OPTIONAL MATCH (targetQ:Question {questionId: $targetParentId})
            OPTIONAL MATCH (targetT:Topic {topicId: $targetParentId})
            WITH
              CASE
                WHEN targetQ IS NOT NULL THEN targetQ.level
                ELSE 0
              END AS parentLevel,
              $targetParentId AS targetParentId,
              $targetParentType AS targetParentType,
              $sourceQuestionIds AS sourceIds
            
            // (2) 복제 대상 집합 트리 내 루트 노드 판별
            UNWIND sourceIds AS srcId
            OPTIONAL MATCH (maybeParent:Question)-[:FOLLOWED_BY]->(child:Question {questionId: srcId})
            WHERE NOT maybeParent.questionId IN sourceIds
            WITH collect(srcId) AS rootSourceIds, parentLevel, targetParentId, targetParentType, sourceIds
            
            // (3) 복제 대상 전체 조회 및 oldLevel 준비
            UNWIND sourceIds AS srcId
            MATCH (q:Question {questionId: srcId})
            OPTIONAL MATCH (q)-[:HAS_ANSWER]->(a:Answer)
            WITH q, a, srcId, q.level AS oldLevel, rootSourceIds, parentLevel, targetParentId, targetParentType, apoc.create.uuid() AS newQuestionId, apoc.create.uuid() AS newAnswerId
            
            // (4) oldId-newId 매핑
            WITH q, a, srcId, oldLevel, rootSourceIds, parentLevel, targetParentId, targetParentType, "question-" + newQuestionId AS newQId,
                 CASE WHEN a IS NOT NULL THEN "answer-" + newAnswerId ELSE null END AS newAId
            WITH collect({
                old: srcId,
                new: newQId,
                answerId: newAId,
                question: q,
                answer: a,
                oldLevel: oldLevel,
                isRoot: srcId IN rootSourceIds
            }) AS nodes, rootSourceIds, parentLevel, targetParentId, targetParentType
            
            // (5) 루트의 oldLevel 파악 (rootSourceIds의 첫번째 기준)
            WITH nodes, rootSourceIds, parentLevel, targetParentId, targetParentType,
                 [n IN nodes WHERE n.isRoot][0].oldLevel AS rootOldLevel
            
            // (6) 각 노드별 newLevel 계산
            WITH nodes, rootSourceIds, parentLevel, targetParentId, targetParentType, rootOldLevel,
                 [n IN nodes |
                    n +
                    {
                      newLevel:
                        CASE
                          WHEN n.isRoot THEN
                            CASE WHEN targetParentType = 'topic' THEN 1 ELSE parentLevel + 1 END
                          ELSE
                            (CASE WHEN targetParentType = 'topic' THEN 1 ELSE parentLevel + 1 END) + (n.oldLevel - rootOldLevel)
                        END
                    }
                 ] AS nodesWithLevel
            
            UNWIND nodesWithLevel AS n
            
            // (7) 새 질문 노드 생성 (newLevel 반영)
            CREATE (q2:Question {
              questionId: n.new,
              text: n.question.text,
              level: n.newLevel,
              createdAt: datetime()
            })
            
            // (8) 새 답변 노드 생성 및 연결
            FOREACH (_ IN CASE WHEN n.answer IS NOT NULL THEN [1] ELSE [] END |
              CREATE (a2:Answer {
                answerId: n.answerId,
                text: n.answer.text,
                createdAt: datetime()
              })
              WITH n, a2
              MATCH (q2:Question {questionId: n.new})
              CREATE (q2)-[:HAS_ANSWER]->(a2)
            )
            
            // (9) 선택 집합 내 FOLLOWED_BY 관계 복제
            UNWIND nodesWithLevel AS parent
            UNWIND nodesWithLevel AS child
            MATCH (parentQ:Question {questionId: parent.old})-[:FOLLOWED_BY]->(childQ:Question {questionId: child.old})
            MATCH (newParent:Question {questionId: parent.new})
            MATCH (newChild:Question {questionId: child.new})
            CREATE (newParent)-[:FOLLOWED_BY]->(newChild)
            
            // (10) 복제 루트의 부모 연결 (타입에 따라 분기)
            UNWIND nodesWithLevel AS n
            WITH n, targetParentId, targetParentType, rootSourceIds
            WHERE n.old IN rootSourceIds
            MATCH (newRoot:Question {questionId: n.new})
            FOREACH (_ IN CASE WHEN targetParentType = 'topic' THEN [1] ELSE [] END |
                MATCH (t:Topic {topicId: targetParentId})
                CREATE (t)-[:START_CONVERSATION]->(newRoot)
            )
            FOREACH (_ IN CASE WHEN targetParentType = 'question' THEN [1] ELSE [] END |
                MATCH (q:Question {questionId: targetParentId})
                CREATE (q)-[:FOLLOWED_BY]->(newRoot)
            )
            
            // (11) 복제된 새 questionId들 반환
            RETURN [n IN nodesWithLevel | n.new] AS newQuestionIds
            """)
    List<String> copyPartialQuestionTree(List<String> sourceQuestionIds, String targetParentId);
}
