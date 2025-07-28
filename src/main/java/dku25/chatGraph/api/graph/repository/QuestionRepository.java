package dku25.chatGraph.api.graph.repository;

import dku25.chatGraph.api.graph.dto.RenameQuestionResponseDTO;
import dku25.chatGraph.api.graph.dto.RenameTopicResponseDTO;
import dku25.chatGraph.api.graph.node.QuestionNode;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.transaction.annotation.Transactional;

public interface QuestionRepository extends Neo4jRepository<QuestionNode, String> {
  // 질문 ID로 소유자 조회
  @Query("MATCH (u:User)-[:OWNS]->(t:Topic)-[:START_CONVERSATION|FOLLOWED_BY*0]->(q:Question {questionId: $questionId}) RETURN u.userId AS ownerId")
  Optional<String> findUserIdByQuestionId(String questionId);

  //  모든 자식노드 조회
  @Query("MATCH (parent:Question {questionId: $parentId})-[:FOLLOWED_BY]->(q:Question) RETURN q")
  List<QuestionNode> findChildrenByParentId(String parentId);

  // 부모 - 특정자식노드 관계 설정
  @Modifying
  @Transactional
  @Query("MATCH (parent:Question {questionId: $parentId}), (child:Question {questionId: $childId}) MERGE (parent)-[:FOLLOWED_BY]->(child)")
  void createFollowedByRelation(String parentId, String childId);

  @Query("MATCH (parent:Question)-[:FOLLOWED_BY]->(q:Question {questionId: $currentQuestionId}) return parent")
  QuestionNode getPreviousQuestion(String currentQuestionId);

  @Modifying
  @Transactional
  @Query("MATCH (startNode:Question {questionId: $startNodeId})-[:FOLLOWED_BY*0..]->(descendant:Question) " +
         "SET descendant.level = descendant.level - 1")
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
}
