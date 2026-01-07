package vn.bluemoon.repository;

import vn.bluemoon.exception.DbException;
import vn.bluemoon.model.entity.PasswordResetToken;
import vn.bluemoon.util.JdbcUtils;

import java.sql.*;

/**
 * Repository for PasswordResetToken entity
 */
public class TokenRepository {
    
    /**
     * Create password reset token
     */
    public PasswordResetToken create(PasswordResetToken token) throws DbException {
        String sql = "INSERT INTO password_reset_tokens (user_id, token, expires_at, used) VALUES (?, ?, ?, ?)";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, token.getUserId());
            stmt.setString(2, token.getToken());
            stmt.setTimestamp(3, Timestamp.valueOf(token.getExpiresAt()));
            stmt.setBoolean(4, token.getUsed() != null ? token.getUsed() : false);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DbException("Creating token failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    token.setId(generatedKeys.getInt(1));
                } else {
                    throw new DbException("Creating token failed, no ID obtained.");
                }
            }
            return token;
        } catch (SQLException e) {
            throw new DbException("Error creating token: " + e.getMessage(), e);
        }
    }

    /**
     * Find token by token string
     */
    public PasswordResetToken findByToken(String token) throws DbException {
        String sql = "SELECT * FROM password_reset_tokens WHERE token = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToToken(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DbException("Error finding token: " + e.getMessage(), e);
        }
    }

    /**
     * Mark token as used
     */
    public void markAsUsed(String token) throws DbException {
        String sql = "UPDATE password_reset_tokens SET used = TRUE WHERE token = ?";
        try (Connection conn = JdbcUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Error marking token as used: " + e.getMessage(), e);
        }
    }

    /**
     * Delete expired tokens
     */
    public void deleteExpiredTokens() throws DbException {
        String sql = "DELETE FROM password_reset_tokens WHERE expires_at < NOW() OR used = TRUE";
        try (Connection conn = JdbcUtils.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new DbException("Error deleting expired tokens: " + e.getMessage(), e);
        }
    }

    private PasswordResetToken mapResultSetToToken(ResultSet rs) throws SQLException {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(rs.getInt("id"));
        token.setUserId(rs.getInt("user_id"));
        token.setToken(rs.getString("token"));
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) {
            token.setExpiresAt(expiresAt.toLocalDateTime());
        }
        token.setUsed(rs.getBoolean("used"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            token.setCreatedAt(createdAt.toLocalDateTime());
        }
        return token;
    }
}


