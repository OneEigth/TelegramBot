package oneEight.dao;

import oneEight.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserDao extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByTelegramId(Long telegramId);
    Optional<AppUser> findById(Long id);
    Optional<AppUser> findByEmail(String email);

}
