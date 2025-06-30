package dku25.chatGraph.api.graph;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface AnswerRepository extends Neo4jRepository<AnswerNode, Long> {}