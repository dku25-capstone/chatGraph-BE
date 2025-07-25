package dku25.chatGraph.api.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dku25.chatGraph.api.user.domain.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
  
  Optional<User> findByEmail(String email);
  Optional<User> findByProviderAndProviderId(String provider, String providerId);
}
