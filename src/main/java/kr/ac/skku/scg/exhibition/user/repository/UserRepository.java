package kr.ac.skku.scg.exhibition.user.repository;

import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.user.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByCi(String ci);
}
