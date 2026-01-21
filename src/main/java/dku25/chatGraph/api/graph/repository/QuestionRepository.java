package dku25.chatGraph.api.graph.repository;

import dku25.chatGraph.api.graph.dto.RenameQuestionResponseDTO;
import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import dku25.chatGraph.api.graph.node.QuestionNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface QuestionRepository extends Neo4jRepository<QuestionNode, String> {
    // 질문 ID로 소유자 조회
    @Query("""
            MATCH (u:User)-[:OWNS]->(t:Topic)
            MATCH (t)-[:START_CONVERSATION]->(root:Question)
            MATCH (root)-[:FOLLOWED_BY*0..]->(q:Question {questionId: $questionId})
            RETURN u.userId AS ownerId
            """)
    Optional<String> findUserIdByQuestionId(String questionId);

    //  모든 자식노드 조회
    @Query("MATCH (parent:Question {questionId: $parentId})-[:FOLLOWED_BY]->(q:Question) RETURN q")
    List<QuestionNode> findChildrenByParentId(String parentId);

    //  Query Parameter로 질문노드 조회
    @Query("""
            MATCH (u:User {userId: $userId})-[:OWNS]->(t:Topic)-[:START_CONVERSATION|FOLLOWED_BY*]->(q:Question)
            WHERE q.text CONTAINS $keyword
            MATCH (q)-[:HAS_ANSWER]->(a:Answer)
            OPTIONAL MATCH (q)-[:FOLLOWED_BY]->(child:Question)
            RETURN
            q.questionId AS questionId,
            q.text AS questionText,
            q.level AS level,
            q.isFavorite AS isFavorite,
            a.answerId AS answerId,
            a.text AS answerText,
            q.createdAt AS createdAt,
            COLLECT(child.questionId) AS children
            """)
    List<QuestionAnswerDTO> findQuestionAndAnswerByKeyword(@Param("keyword") String keyword, @Param("userId") String userId);

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
            MATCH (parent)-[rel]->(toDelete:Question {questionId: $questionId})
            WHERE type(rel) = "START_CONVERSATION" OR type(rel) = "FOLLOWED_BY"
            OPTIONAL MATCH (toDelete)-[:HAS_ANSWER]->(answer:Answer)
            OPTIONAL MATCH (toDelete)-[:FOLLOWED_BY]->(child:Question)
            OPTIONAL MATCH (toDelete)-[:FOLLOWED_BY*]->(descendant:Question)
            WITH parent, rel, toDelete, answer, child, collect(descendant) AS descendants
            
            FOREACH (c IN CASE WHEN child IS NOT NULL AND type(rel) = "START_CONVERSATION" THEN [child] ELSE [] END |
                MERGE (parent)-[:START_CONVERSATION]->(c)
            )
            FOREACH (c IN CASE WHEN child IS NOT NULL AND type(rel) = "FOLLOWED_BY" THEN [child] ELSE [] END |
                MERGE (parent)-[:FOLLOWED_BY]->(c)
            )
            
            FOREACH (d IN descendants |
                SET d.level = d.level - 1
            )
            
            DETACH DELETE toDelete, answer
            """)
    void deleteAndRelink(String questionId);

    // 질문명 수정
    @Modifying
    @Transactional
    @Query("""
              MATCH (q:Question {questionId: $questionId})
              SET q.text = $newQuestionName
              RETURN q
            """)
    RenameQuestionResponseDTO renameQuestion(String questionId, String newQuestionName);

    // 질문 노드(단일, 복수) 복제
    @Modifying
    @Transactional
    @Query("""
            // (1) 부모 레벨 준비
            OPTIONAL MATCH (targetQ:Question {questionId: $targetParentId})
            WITH
            CASE WHEN targetQ IS NOT NULL THEN targetQ.level ELSE 0 END AS parentLevel,
            $targetParentId AS targetParentId,
            $sourceQuestionIds AS sourceIds
            
            // (2) 복제 트리 내 루트 노드 판별
            UNWIND sourceIds AS srcId
            OPTIONAL MATCH (maybeParent:Question)-[:FOLLOWED_BY]->(child:Question {questionId: srcId})
            WHERE maybeParent.questionId IN sourceIds
            WITH srcId, collect(maybeParent) AS maybeParent, sourceIds, parentLevel, targetParentId
            WITH collect(CASE WHEN size([p IN maybeParent WHERE p IS NOT NULL]) = 0 THEN srcId END) AS rootSourceIds, sourceIds, parentLevel, targetParentId
            
            // (3) 복제 대상 전체 조회 및 UUID 준비
            UNWIND sourceIds AS srcId
            MATCH (q:Question {questionId: srcId})
            OPTIONAL MATCH (q)-[:HAS_ANSWER]->(a:Answer)
            WITH q, a, srcId, q.level AS oldLevel, rootSourceIds, parentLevel, targetParentId,
            "question-" + toString(randomUUID()) AS newQuestionId,
            "answer-" + toString(randomUUID()) AS newAnswerId
            
            // (4) oldId-newId 매핑 + rootOldLevel 추출
            WITH collect({
             old: srcId,
             new: newQuestionId,
             answerId: CASE WHEN a IS NOT NULL THEN newAnswerId ELSE null END,
             question: q,
             answer: a,
             oldLevel: oldLevel,
             isRoot: srcId IN rootSourceIds
            }) AS nodes, rootSourceIds, parentLevel, targetParentId
            
            // rootOldLevel이 null일 수 있으니 coalesce로 방어
            WITH nodes, rootSourceIds, parentLevel, targetParentId,
              coalesce([n IN nodes WHERE n.isRoot][0].oldLevel, 0) AS rootOldLevel
            
            // (5) 각 노드별 newLevel 계산
            WITH [n IN nodes |
                 {
                   old: n.old,
                   new: n.new,
                   answerId: n.answerId,
                   question: n.question,
                   answer: n.answer,
                   oldLevel: n.oldLevel,
                   isRoot: n.isRoot,
                   newLevel: CASE
                               WHEN n.isRoot THEN parentLevel + 1
                               ELSE (parentLevel + 1) + (n.oldLevel - rootOldLevel)
                             END
                 }
              ] AS nodesWithLevel, targetParentId, rootSourceIds
            
            // (6) 새 질문 노드 생성
            UNWIND nodesWithLevel AS n
            CREATE (q2:Question {
            questionId: n.new,
            text: n.question.text,
            level: n.newLevel,
            isFavorite: false,
            createdAt: localdatetime()
            })
            WITH collect(n) AS createdNodes, targetParentId, rootSourceIds
            
            
            // (7) 새 답변 노드 생성 및 연결
            UNWIND createdNodes AS nAnswer
            WITH nAnswer, createdNodes, targetParentId, rootSourceIds
            WHERE nAnswer.answer IS NOT NULL
            CREATE (a2:Answer {
            answerId: nAnswer.answerId,
            text: nAnswer.answer.text,
            createdAt: localdatetime()
            })
            WITH a2, nAnswer, createdNodes, targetParentId, rootSourceIds
            MATCH (q2:Question {questionId: nAnswer.new})
            CREATE (q2)-[:HAS_ANSWER]->(a2)
            WITH createdNodes, targetParentId, rootSourceIds
            
            // (8) 🔥 **sourceIds 내 FOLLOWED_BY 관계 복제!**
            CALL {
              WITH createdNodes
              UNWIND createdNodes AS parent
              UNWIND createdNodes AS child
              // parent.old → child.old로 원래 FOLLOWED_BY 있었던 경우만
              MATCH (p:Question {questionId: parent.old})-[:FOLLOWED_BY]->(c:Question {questionId: child.old})
              // 새 노드들끼리 연결
              MATCH (newP:Question {questionId: parent.new})
              MATCH (newC:Question {questionId: child.new})
              MERGE (newP)-[:FOLLOWED_BY]->(newC)
              RETURN count(*) AS _
            }
            WITH createdNodes, targetParentId, rootSourceIds
            
            UNWIND createdNodes AS nRoot
            WITH nRoot, targetParentId, nRoot.new AS newQuestionId, createdNodes
            WHERE nRoot.isRoot
            MATCH (newRoot:Question {questionId: newQuestionId})
            OPTIONAL MATCH (t:Topic {topicId: targetParentId})
            OPTIONAL MATCH (q:Question {questionId: targetParentId})
            FOREACH (_ IN CASE WHEN t IS NOT NULL THEN [1] ELSE [] END |
             MERGE (t)-[:START_CONVERSATION]->(newRoot)
            )
            FOREACH (_ IN CASE WHEN q IS NOT NULL THEN [1] ELSE [] END |
             MERGE (q)-[:FOLLOWED_BY]->(newRoot)
            )
            
            WITH createdNodes
            UNWIND createdNodes AS n
            RETURN DISTINCT n.new AS questionId
            """)
    List<String> copyPartialQuestionTree(@Param("sourceQuestionIds") List<String> sourceQuestionIds,
                                         @Param("targetParentId") String targetParentId);

    @Query("""
            MATCH (q: Question {questionId: $questionId})
            OPTIONAL MATCH path = (parent: Question)-[:FOLLOWED_BY*0..4]->(target)
            WITH nodes(path) AS nodeList
            UNWIND nodeList AS q
            MATCH (q)-[:HAS_ANSWER]->(a:Answer)
            RETURN q, a
            ORDER BY q.level ASC
            """)
    List<QuestionNode> findContextChain(@Param("questionId") String questionId);

    /**
     * [리팩토링] 키워드 검색 (Map 반환)
     * - topicId: String
     * - q: QuestionNode
     * - a: AnswerNode (Optional)
     */
    @Query("""
        MATCH (t:Topic)-[:HAS_QUESTION*]->(q:Question)
        WHERE q.text CONTAINS $keyword OR q.answerText CONTAINS $keyword 
        OPTIONAL MATCH (q)-[:HAS_ANSWER]->(a:Answer)
        RETURN t.topicId AS topicId, q, a
        """)
    List<Map<String, Object>> findQuestionsWithTopicId(@Param("keyword") String keyword, @Param("userId") String userId);
}
