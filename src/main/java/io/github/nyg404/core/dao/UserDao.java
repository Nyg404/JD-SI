package io.github.nyg404.core.dao;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class UserDao {

    private final DataConnection db = DataConnection.getInstance();

    private static final String INSERT_USER = "INSERT INTO users_table(id, is_activate_chat, is_ban) VALUES (?, ?, ?)";
    private static final String SELECT_USER_BY_ID = "SELECT id FROM users_table WHERE id = ?";
    private static final String UPDATE_USER_ACTIVATE = "UPDATE users_table SET is_activate_chat = ? WHERE id = ?";
    private static final String UPDATE_USER_BAN = "UPDATE users_table SET is_ban = ? WHERE id = ?";
    private static final String DELETE_USER = "DELETE FROM users_table WHERE id = ?";
    private static final String SELECT_ACTIVE_USERS = "SELECT id FROM users_table WHERE is_activate_chat = TRUE AND is_ban = FALSE";

    public void addUser(long userId, boolean isActivate) {
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(INSERT_USER)) {
            pr.setLong(1, userId);
            pr.setBoolean(2, isActivate);
            pr.setBoolean(3, false);
            pr.executeUpdate();
        } catch (SQLException e) {
            log.error("Ошибка при добавлении пользователя {}: {}", userId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public boolean existsById(long userId) {
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(SELECT_USER_BY_ID)) {
            pr.setLong(1, userId);
            ResultSet rs = pr.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            log.error("Ошибка при проверке пользователя {}: {}", userId, e.getMessage());
            return false;
        }
    }

    public void updateActivate(long userId, boolean activate) {
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(UPDATE_USER_ACTIVATE)) {
            pr.setBoolean(1, activate);
            pr.setLong(2, userId);
            pr.executeUpdate();
        } catch (SQLException e) {
            log.error("Ошибка при обновлении активации пользователя {}: {}", userId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void updateBan(long userId, boolean ban) {
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(UPDATE_USER_BAN)) {
            pr.setBoolean(1, ban);
            pr.setLong(2, userId);
            pr.executeUpdate();
        } catch (SQLException e) {
            log.error("Ошибка при обновлении бана пользователя {}: {}", userId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void deleteUser(long userId) {
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(DELETE_USER)) {
            pr.setLong(1, userId);
            pr.executeUpdate();
        } catch (SQLException e) {
            log.error("Ошибка при удалении пользователя {}: {}", userId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<Long> getActiveUsers() {
        List<Long> users = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(SELECT_ACTIVE_USERS);
             ResultSet rs = pr.executeQuery()) {
            while (rs.next()) {
                users.add(rs.getLong("id"));
            }
        } catch (SQLException e) {
            log.error("Ошибка при получении активных пользователей: {}", e.getMessage());
        }
        return users;
    }
}
