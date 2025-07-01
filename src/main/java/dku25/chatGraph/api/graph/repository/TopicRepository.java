package dku25.chatGraph.api.graph.repository;

import dku25.chatGraph.api.graph.node.TopicNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface TopicRepository extends Neo4jRepository<TopicNode, String> {
}
