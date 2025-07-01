package dku25.chatGraph.api.graph.repository;

import dku25.chatGraph.api.graph.node.QuestionNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface QuestionRepository extends Neo4jRepository<QuestionNode, Long> {}
