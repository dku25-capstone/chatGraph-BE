package dku25.chatGraph.api.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; // 내부용
  @Column(nullable = false, unique = true) // "user-"+UUID, 외부/연동용
  private String userId;
  @Column(nullable = false, unique = true)
  private String email;
  @Column(nullable = true) // 소셜 로그인 시 비밀번호 x
  private String password; 
  @Column(nullable = true) // 일반 로그인 시 필요 x
  private String provider; // 소셜 로그인 제공자 구분
  @Column(nullable = true) // 일반 로그인 시 필요 x
  private String providerId; // 소셜 제공자에서 발급한 고유 사용자 id
  @Column(nullable = false)
  private String role = "USER"; // mvp 이므로 USER 고정
  
}
