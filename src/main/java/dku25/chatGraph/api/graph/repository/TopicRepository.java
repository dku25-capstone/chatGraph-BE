package dku25.chatGraph.api.graph.repository;

import dku25.chatGraph.api.graph.node.TopicNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;

public interface TopicRepository extends Neo4jRepository<TopicNode, String> {

    @Query("MATCH (t:topic) Return t ORDER BY t.createdAt DESC")
    List<TopicNode> findBySessionId(String sessionId);
}
