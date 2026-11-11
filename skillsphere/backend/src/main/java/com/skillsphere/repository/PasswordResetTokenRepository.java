package com.skillsphere.repository;

import com.skillsphere.entity.PasswordResetToken;
import com.skillsphere.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @Query("select t from PasswordResetToken t join fetch t.user where t.tokenHash = :hash")
    Optional<PasswordResetToken> findByTokenHash(@Param("hash") String hash);

    @Modifying
    @Query("delete from PasswordResetToken t where t.user = :user")
    void deleteByUser(@Param("user") User user);
}
