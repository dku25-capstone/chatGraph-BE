package dku25.chatGraph.api.graph.repository;

import dku25.chatGraph.api.graph.dto.TopicNodeDTO;
import dku25.chatGraph.api.graph.dto.RenameTopicResponseDTO;
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

    @Query("MATCH (t:Topic)-[:START_CONVERSATION]->(q1:Question)-[:FOLLOWED_BY*0..]->(q:Question {questionId: $questionId}) RETURN t.topicId")
    Optional<String> findTopicIdByQuestionId(String questionId);

    @Query("""
            MATCH (t:Topic {topicId: $topicId})
            OPTIONAL MATCH (t)-[:START_CONVERSATION]->(q:Question)
            WITH t, collect(q.questionId) AS children
            RETURN
              t.topicId AS topicId,
              t.topicName AS topicName,
              t.createdAt AS createdAt,
              children
            """) // TopicNodeDTO용 Topic 조회
    Optional<TopicNodeDTO> findTopicNodeDTOById(String topicId);

    @Query("""
            MATCH (t:Topic {topicId: $topicId})-[:START_CONVERSATION]->(q:Question)
                OPTIONAL MATCH (q)-[:FOLLOWED_BY*0..]->(allQ:Question)
                OPTIONAL MATCH (allQ)-[:HAS_ANSWER]->(allA:Answer)
                OPTIONAL MATCH (parent:Question)-[:FOLLOWED_BY]->(allQ)
                OPTIONAL MATCH (allQ)-[:FOLLOWED_BY]->(child:Question)
                RETURN
                  allQ.questionId AS questionId,
                  allQ.text AS questionText,
                  allQ.level AS level,
                  allA.answerId AS answerId,
                  allA.text AS answerText,
                  allQ.createdAt AS createdAt,
                  parent.questionId AS parentId,
                  collect(child.questionId) AS children
                ORDER BY allQ.level
            """
            ) // 부모, 자식 노드 조회 및 추가
    List<QuestionAnswerDTO> findQuestionsAndAnswersByTopicId(String topicId);

    // DB내 데이터 수정 시 아래의 두 어노테이션 삽입.
    @Modifying // DB 수정관련 cypher의 경우 modifying 어노테이션 삽입
    @Transactional // Transactional을 통해서 해당 트랜잭션 안정화 및 데이터 안정성 보장
    @Query("""
              MATCH (t:Topic {topicId: $topicId})
              SET t.topicName = $newTopicName
              RETURN t
            """)
    RenameTopicResponseDTO renameTopic(String topicId, String newTopicName);

    @Modifying
    @Transactional
    @Query("""
            MATCH (root:Topic {topicId: $topicId})
            OPTIONAL MATCH (root)-[*0..]->(n)
            DETACH DELETE n
            """)
    void deleteById(@NotNull String topicId);

    //토픽 노드 가져오기
    @Query("MATCH (t:Topic)-[:START_CONVERSATION]->(q:Question {questionId: $questionId}) RETURN t")
    Optional<TopicNode> findTopicByFirstQuestionId(String questionId);

    //토픽-자식 노드 관계 추가
    @Modifying
    @Transactional
    @Query("MATCH (t:Topic {topicId: $topicId}), (q:Question {questionId: $questionId}) MERGE (t)-[:START_CONVERSATION]->(q)")
    void createStartConversationRelation(String topicId, String questionId);

    @Modifying
    @Transactional
    @Query("""
        MATCH (topic:Topic)-[:START_CONVERSATION]->(toDelete:Question {questionId: $questionId})
        OPTIONAL MATCH (toDelete)-[:HAS_ANSWER]->(answer:Answer)
        OPTIONAL MATCH (toDelete)-[:FOLLOWED_BY]->(child:Question)

        // 자식이 있는 경우, 토픽과 자식을 연결
        CALL {
          WITH topic, toDelete
          OPTIONAL MATCH (toDelete)-[:FOLLOWED_BY]->(child:Question)
          WHERE child IS NOT NULL
          MERGE (topic)-[:START_CONVERSATION]->(child)
        }

        // 하위 트리 레벨 업데이트
        WITH toDelete, answer, COLLECT(child) AS children
        MATCH (toDelete)-[:FOLLOWED_BY*]->(descendant:Question)
        SET descendant.level = descendant.level - 1

        // 노드와 관계 삭제
        DETACH DELETE toDelete, answer
        """)
    void deleteFirstQuestionAndRelink(String questionId);
}
