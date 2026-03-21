package com.ecommerce.serivce.user.features.token;

import com.ecommerce.serivce.user.common.utils.TimeUtils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OAuthTokeRepository {
    private final JdbcClient jdbcClient;

    private static final String FIND_BY_TOKEN_HASH_SQL = "SELECT * FROM oauth_tokens WHERE token_hash = :token_hash";
    private static final String SAVE_OAUTH_TOKEN_SQL =
            """
                    INSERT INTO oauth_tokens(user_id, token_hash, device_info, expires_at, revoked)
                    VALUES (:userId, :tokenHash, :deviceInfo, :expiresAt, :revoked)
                    ON CONFLICT (token_hash) DO UPDATE SET revoked = EXCLUDED.revoked
                    """;

    private static final String REVOKED_ALL_TOKEN_BY_USER_ID_SQL =
            "UPDATE oauth_tokens SET revoked = true WHERE user_id = :userId";

    public Optional<OAuthToken> findByTokenHash(String token) {
        return jdbcClient
                .sql(FIND_BY_TOKEN_HASH_SQL)
                .param("token_hash", token)
                .query(this::mapRow)
                .optional();
    }

    public OAuthToken save(OAuthToken token) {
        jdbcClient
                .sql(SAVE_OAUTH_TOKEN_SQL)
                .param("userId", token.getUserId())
                .param("tokenHash", token.getTokenHash())
                .param("deviceInfo", token.getDeviceInfo())
                .param("expiresAt", TimeUtils.toDb(token.getExpiresAt()))
                .param("revoked", token.isRevoked())
                .update();
        return token;
    }

    public void revokedAllTokenByUserId(UUID userId) {
        jdbcClient.sql(REVOKED_ALL_TOKEN_BY_USER_ID_SQL).param("userId", userId).update();
    }

    private OAuthToken mapRow(ResultSet rs, int row) throws SQLException {
        return OAuthToken.builder()
                .id(UUID.fromString(rs.getString("id")))
                .userId(UUID.fromString(rs.getString("user_id")))
                .tokenHash(rs.getString("token_hash"))
                .deviceInfo(rs.getString("device_info"))
                .expiresAt(TimeUtils.fromDb(rs, "expires_at"))
                .revoked(rs.getBoolean("revoked"))
                .createdAt(TimeUtils.fromDb(rs, "created_at"))
                .build();
    }
}
