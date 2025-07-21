package dku25.chatGraph.api.graph.repository;

import dku25.chatGraph.api.graph.node.QuestionNode;

import java.util.List;
import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

public interface QuestionRepository extends Neo4jRepository<QuestionNode, String> {
  //  모든 자식노드 조회
  @Query("MATCH (q:Question)-[:PREVIOUS_QUESTION]->(parent:Question {questionId: $parentId}) RETURN q")
  List<QuestionNode> findChildrenByParentId(String parentId);

  // 부모 - 특정자식노드 관계 삭제
  @Query("MATCH (parent:Question {questionId: $parentId})-[r:FOLLOWED_BY]->(child:Question {questionId: $childId}) DELETE r")
  void removeFollowedByRelation(String parentId, String childId);

  // 부모 - 특정자식노드 관계 설정
  @Query("MATCH (parent:Question {questionId: $parentId}), (child:Question {questionId: $childId}) MERGE (parent)-[:FOLLOWED_BY]->(child)")
  void createFollowedByRelation(String parentId, String childId);
}
