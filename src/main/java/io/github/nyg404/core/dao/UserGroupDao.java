package io.github.nyg404.core.dao;

import io.github.nyg404.core.models.UserKey;
import io.github.nyg404.core.models.UserStats;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class UserGroupDao {

    private final DataConnection db = DataConnection.getInstance();

    private static final String INSERT_USER_GROUP =
            "INSERT INTO user_group_state(user_id, group_id, user_bio) VALUES (?, ?, ?)";

    private static final String UPDATE_USER_BIO =
            "UPDATE user_group_state SET user_bio = ? WHERE user_id = ? AND group_id = ?";

    private static final String DELETE_USER_GROUP =
            "DELETE FROM user_group_state WHERE user_id = ? AND group_id = ?";

    private static final String SELECT_USER_GROUP =
            "SELECT user_id, group_id, user_bio FROM user_group_state WHERE user_id = ? AND group_id = ?";

    private static final String SELECT_USERS_IN_GROUP =
            "SELECT user_id FROM user_group_state WHERE group_id = ?";

    public void addUserToGroup(long userId, String groupId, String userBio) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_USER_GROUP)) {
            ps.setLong(1, userId);
            ps.setString(2, groupId);
            ps.setString(3, userBio);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Ошибка при добавлении пользователя {} в группу {}: {}", userId, groupId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void updateUserBio(long userId, String groupId, String userBio) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_USER_BIO)) {
            ps.setString(1, userBio);
            ps.setLong(2, userId);
            ps.setString(3, groupId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Ошибка при обновлении биографии пользователя {} в группе {}: {}", userId, groupId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void removeUserFromGroup(long userId, String groupId) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_USER_GROUP)) {
            ps.setLong(1, userId);
            ps.setString(2, groupId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Ошибка при удалении пользователя {} из группы {}: {}", userId, groupId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public UserStats getUserGroup(long userId, String groupId) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_USER_GROUP)) {
            ps.setLong(1, userId);
            ps.setString(2, groupId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new UserStats(rs.getLong("user_id"), rs.getString("group_id"), rs.getString("user_bio"));
            }
        } catch (SQLException e) {
            log.error("Ошибка при получении userGroup {}: {}", new UserKey(userId, groupId), e.getMessage());
        }
        return null;
    }

    public List<Long> getUsersInGroup(String groupId) {
        List<Long> users = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_USERS_IN_GROUP)) {
            ps.setString(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(rs.getLong("user_id"));
            }
        } catch (SQLException e) {
            log.error("Ошибка при получении пользователей группы {}: {}", groupId, e.getMessage());
        }
        return users;
    }
}
