package dku25.chatGraph.api.graph.service;

import dku25.chatGraph.api.exception.ResourceNotFoundException;
import dku25.chatGraph.api.graph.node.UserNode;
import dku25.chatGraph.api.graph.repository.UserNodeRepository;
import dku25.chatGraph.api.user.domain.User;
import dku25.chatGraph.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserNodeService {
    private final UserNodeRepository userNodeRepository;
    private final UserRepository userRepository;

    public UserNodeService(UserNodeRepository userNodeRepository, UserRepository userRepository) {
        this.userNodeRepository = userNodeRepository;
        this.userRepository = userRepository;
    }

    /**
     * 회원가입 시 호출: userId로 Neo4j에 UserNode 생성
     */
    public void createUserNode(String userId) {
        userNodeRepository.save(UserNode.createUser(userId));
    }

    /**
     * 회원 ID(user-**형식)로 회원 노드 가져오기
     */
    public UserNode getUserById(String userId) {
        return userNodeRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다."));
    }

    /**
     * 회원 Email-ID로 회원 노드 가져오기
     */
    public UserNode getUserByEmailId(String userEmailId) {
        User user = userRepository.findByEmail(userEmailId).orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자입니다."));
        return userNodeRepository.findById(user.getUserId()).orElseThrow(() -> new ResourceNotFoundException("유효하지 않은 사용자입니다."));
    }
}
