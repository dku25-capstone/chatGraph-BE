package dku25.chatGraph.api.graph.repository;

import dku25.chatGraph.api.graph.node.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface UserGraphRepository extends Neo4jRepository<UserNode, Long> {
    // 필요하다면 커스텀 쿼리 메서드 추가
}