package com.ecommerce.serivce.user.features.user;

import com.ecommerce.serivce.user.common.utils.TimeUtils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final JdbcClient jdbcClient;

    private static final String FIND_BY_ID_SQL = "SELECT * FROM users WHERE id = :id";
    private static final String FIND_BY_EMAIL_SQL = "SELECT * FROM users WHERE email = :email";
    private static final String EXSIST_BY_EMAIL_SQL = "SELECT COUNT(1) FROM users WHERE email = :email";
    private static final String INSERT_USER_SQL =
            """
                    INSERT INTO users(email, password, full_name, role, is_active)
                    values(:email, :password, :fullName, :role::user_role, :isActive)
                    RETURNING *
                    """;
    private static final String UPDATE_USER_SQL =
            """
                    UPDATE users
                    SET email = :email, full_name = :fullName, is_active = :isActive, updated_at = NOW()
                    WHERE id = :id
                    """;

    public Optional<User> findById(UUID id) {
        return jdbcClient
                .sql(FIND_BY_ID_SQL)
                .param("id", id)
                .query(this::mapRow)
                .optional();
    }

    public Optional<User> findByEmail(String email) {
        return jdbcClient
                .sql(FIND_BY_EMAIL_SQL)
                .param("email", email)
                .query(this::mapRow)
                .optional();
    }

    public boolean exsitsByEmail(String email) {
        Integer count = jdbcClient
                .sql(EXSIST_BY_EMAIL_SQL)
                .param("email", email)
                .query(Integer.class)
                .single();
        return count > 0;
    }

    public User saveOrUpdate(User user) {
        if (user.getId() == null) return insert(user);
        return update(user);
    }

    public User insert(User user) {
        var keyHolder = new GeneratedKeyHolder();
        jdbcClient
                .sql(INSERT_USER_SQL)
                .param("email", user.getEmail())
                .param("password", user.getPassword())
                .param("fullName", user.getFullName())
                .param("isActive", user.isActive())
                .param("role", user.getRole().name())
                .update(keyHolder);

        return findById(UUID.fromString(
                        Objects.requireNonNull(keyHolder.getKeys()).get("id").toString()))
                .orElseThrow();
    }

    private User update(User user) {
        jdbcClient
                .sql(UPDATE_USER_SQL)
                .param("id", user.getId())
                .param("email", user.getEmail())
                .param("fullName", user.getFullName())
                .param("isActive", user.isActive())
                .update();
        return findById(user.getId()).orElseThrow();
    }

    private User mapRow(ResultSet rs, int rowNum) throws SQLException {
        return User.builder()
                .id(UUID.fromString(rs.getString("id")))
                .email(rs.getString("email"))
                .password(rs.getString("password"))
                .fullName(rs.getString("full_name"))
                .role(User.Role.valueOf(rs.getString("role")))
                .isActive(rs.getBoolean("is_active"))
                .createdAt(TimeUtils.fromDb(rs, "created_at"))
                .updatedAt(TimeUtils.fromDb(rs, "updated_at"))
                .build();
    }
}
