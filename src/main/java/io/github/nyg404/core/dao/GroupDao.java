package io.github.nyg404.core.dao;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GroupDao {

    private final DataConnection db = DataConnection.getInstance();

    private static final String INSERT_GROUP = "INSERT INTO groups_table(id, is_activate, is_ban) VALUES (?, ?, ?)";
    private static final String SELECT_GROUP_BY_ID = "SELECT id FROM groups_table WHERE id = ?";
    private static final String UPDATE_GROUP_ACTIVATE = "UPDATE groups_table SET is_activate = ? WHERE id = ?";
    private static final String UPDATE_GROUP_BAN = "UPDATE groups_table SET is_ban = ? WHERE id = ?";
    private static final String DELETE_GROUP = "DELETE FROM groups_table WHERE id = ?";
    private static final String SELECT_ACTIVE_GROUPS = "SELECT id FROM groups_table WHERE is_activate = TRUE AND is_ban = FALSE";

    public void addGroup(String groupId, boolean isActivate) {
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(INSERT_GROUP)) {
            pr.setString(1, groupId);
            pr.setBoolean(2, isActivate);
            pr.setBoolean(3, false);
            pr.executeUpdate();
        } catch (SQLException e) {
            log.error("Ошибка при добавлении группы {}: {}", groupId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public boolean existsById(String groupId) {
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(SELECT_GROUP_BY_ID)) {
            pr.setString(1, groupId);
            ResultSet rs = pr.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            log.error("Ошибка при проверке группы {}: {}", groupId, e.getMessage());
            return false;
        }
    }

    public void updateActivate(String groupId, boolean activate) {
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(UPDATE_GROUP_ACTIVATE)) {
            pr.setBoolean(1, activate);
            pr.setString(2, groupId);
            pr.executeUpdate();
        } catch (SQLException e) {
            log.error("Ошибка при обновлении активации группы {}: {}", groupId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void updateBan(String groupId, boolean ban) {
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(UPDATE_GROUP_BAN)) {
            pr.setBoolean(1, ban);
            pr.setString(2, groupId);
            pr.executeUpdate();
        } catch (SQLException e) {
            log.error("Ошибка при обновлении бана группы {}: {}", groupId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void deleteGroup(String groupId) {
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(DELETE_GROUP)) {
            pr.setString(1, groupId);
            pr.executeUpdate();
        } catch (SQLException e) {
            log.error("Ошибка при удалении группы {}: {}", groupId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<String> getActiveGroups() {
        List<String> groups = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(SELECT_ACTIVE_GROUPS);
             ResultSet rs = pr.executeQuery()) {
            while (rs.next()) {
                groups.add(rs.getString("id"));
            }
        } catch (SQLException e) {
            log.error("Ошибка при получении активных групп: {}", e.getMessage());
        }
        return groups;
    }
}
