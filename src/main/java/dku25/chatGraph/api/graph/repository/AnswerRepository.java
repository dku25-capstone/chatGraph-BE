package dku25.chatGraph.api.graph.repository;

import dku25.chatGraph.api.graph.node.AnswerNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface AnswerRepository extends Neo4jRepository<AnswerNode, Long> {}