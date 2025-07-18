package dku25.chatGraph.api.controller;

import dku25.chatGraph.api.user.dto.SignupRequest;
import dku25.chatGraph.api.user.service.UserService;
import dku25.chatGraph.api.graph.service.GraphService;
import dku25.chatGraph.api.user.domain.User;
import dku25.chatGraph.api.user.dto.LoginRequest;
import dku25.chatGraph.api.user.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    private final UserService userService;
    private final GraphService userGraphService;

    public UserController(UserService userService, GraphService userGraphService) {
        this.userService = userService;
        this.userGraphService = userGraphService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid SignupRequest request) {
        User user = userService.saveUser(request);
        try {
            userGraphService.createUserNode(user.getUserId());
        } catch (Exception e) {
            e.printStackTrace();// 실패 로그만 남기고 회원가입은 성공 처리
        }

        return ResponseEntity.ok("회원가입 성공");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }
}
