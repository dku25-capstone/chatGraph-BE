package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.graph.node.UserNode;
import dku25.chatGraph.api.graph.repository.UserNodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserNodeService {
    private final UserNodeRepository userNodeRepository;

    @Autowired
    public UserNodeService(UserNodeRepository userNodeRepository) {
        this.userNodeRepository = userNodeRepository;
    }

    /**
     * 회원가입 시 호출: userId로 Neo4j에 UserNode 생성
     */
    public void createUserNode(String userId) {
        userNodeRepository.save(UserNode.createUser(userId));
    }

    /**
     * 회원 ID로 회원 노드 가져오기
     */
    public UserNode getUserById(String userId) {
        return userNodeRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다."));
    }
}
