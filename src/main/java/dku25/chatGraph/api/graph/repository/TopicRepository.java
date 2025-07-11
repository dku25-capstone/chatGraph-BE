package dku25.chatGraph.api.graph.repository;

import dku25.chatGraph.api.graph.node.TopicNode;
import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;

public interface TopicRepository extends Neo4jRepository<TopicNode, String> {
    @Query("MATCH (t:Topic {topicId: $topicId})-[:start_conversation]->(q:Question) " +
           "OPTIONAL MATCH (q)-[:FOLLOWED_BY*0..]->(allQ:Question) " +
           "OPTIONAL MATCH (allQ)-[:HAS_ANSWER]->(allA:Answer) " +
           "RETURN allQ.questionId AS questionId, allQ.text AS questionText, allQ.level AS level, " +
           "allA.answerId AS answerId, allA.text AS answerText, allQ.createdAt AS createdAt " +
           "ORDER BY level")
    List<QuestionAnswerDTO> findQuestionsAndAnswersByTopicId(String topicId);
}
