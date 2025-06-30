package dku25.chatGraph.api.graph;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface QuestionRepository extends Neo4jRepository<QuestionNode, Long> {}
