package dku25.chatGraph.api.graph.repository;

import dku25.chatGraph.api.graph.dto.TopicResponseDTO;
import dku25.chatGraph.api.graph.node.TopicNode;
import dku25.chatGraph.api.graph.dto.QuestionAnswerDTO;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TopicRepository extends Neo4jRepository<TopicNode, String> {
    @Query("""
            MATCH (t:Topic {topicId: $topicId})-[:start_conversation]->(q:Question)
            OPTIONAL MATCH (q)-[:FOLLOWED_BY*0..]->(allQ:Question)
            OPTIONAL MATCH (allQ)-[:HAS_ANSWER]->(allA:Answer)
            RETURN allQ.questionId AS questionId, allQ.text AS questionText, allQ.level AS level,
            allA.answerId AS answerId, allA.text AS answerText, allQ.createdAt AS createdAt
            ORDER BY level""")
    List<QuestionAnswerDTO> findQuestionsAndAnswersByTopicId(String topicId);

    // DB내 데이터 수정 시 아래의 두 어노테이션 삽입.
    @Modifying // DB 수정관련 cyper의 경우 modifying 어노테이션 삽입
    @Transactional // Transactional을 통해서 해당 트랜잭션 안정화 및 데이터 안정성 보장
    @Query("""
              MATCH (t:Topic {topicId: $topicId})
              SET t.topicName = $newTopicName
              RETURN t
            """)
    TopicResponseDTO renameTopic(String topicId, String newTopicName);

    @Modifying
    @Transactional
    @Query("""
            MATCH (root:Topic {topicId: $topicId})
            OPTIONAL MATCH (root)-[*0..]->(n)
            DETACH DELETE n
            """)
    void deleteById(String topicId);
}
