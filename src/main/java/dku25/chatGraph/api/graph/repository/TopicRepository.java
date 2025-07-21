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
import java.util.Optional;

public interface TopicRepository extends Neo4jRepository<TopicNode, String> {
    @Query("MATCH (t:Topic {topicId: $topicId})-[:START_CONVERSATION]->(q:Question) " +
            "OPTIONAL MATCH (q)-[:FOLLOWED_BY*0..]->(allQ:Question) " +
            "OPTIONAL MATCH (allQ)-[:HAS_ANSWER]->(allA:Answer) " +
            "RETURN allQ.questionId AS questionId, allQ.text AS questionText, allQ.level AS level, " +
            "allA.answerId AS answerId, allA.text AS answerText, allQ.createdAt AS createdAt " +
            "ORDER BY level")
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

    //토픽 노드 가져오기
    @Query("MATCH (t:Topic)-[:START_CONVERSATION]->(q:Question {questionId: $questionId}) RETURN t")
    Optional<TopicNode> findTopicByFirstQuestionId(String questionId);

    //토픽-삭제 노드 관계 제거
    @Query("MATCH (t:Topic {topicId: $topicId})-[r:START_CONVERSATION]->(q:Question {questionId: $questionId}) DELETE r")
    void removeStartConversationRelation(String topicId, String questionId);

    //토픽-자식 노드 관계 추가
    @Query("MATCH (t:Topic {topicId: $topicId}), (q:Question {questionId: $questionId}) MERGE (t)-[:START_CONVERSATION]->(q)")
    void createStartConversationRelation(String topicId, String questionId);
}
