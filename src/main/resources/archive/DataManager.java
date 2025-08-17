package io.github.nyg404.db;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.nyg404.models.UserKey;
import io.github.nyg404.models.UserStats;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ConstantConditions")
@Slf4j
public class DataManager {

    private static final DataConnection db = DataConnection.getInstance();
    private static DataManager instance;

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            5, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>()
    );

    // КЭШИ
    private static final LoadingCache<Long, Boolean> userCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .maximumSize(10_000)
                    .build(DataManager::loadUserById);

    private static final LoadingCache<String, Boolean> groupCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .maximumSize(5_000)
                    .build(DataManager::loadGroupById);

    private static final LoadingCache<UserKey, UserStats> userGroupStatsCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .maximumSize(50_000)
                    .build(DataManager::loadUserGroup);

    private static final LoadingCache<String, List<Long>> groupUsersCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(5, TimeUnit.MINUTES)
                    .maximumSize(2_000)
                    .build(DataManager::loadUsersInGroupFromDb);

    private DataManager() {}

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    // ------------------- ADD -------------------
    public void addUser(long userId, boolean isActivate) {
        executor.submit(() -> {
            String sql = "INSERT INTO users_table(id, is_activate_chat, is_ban) VALUES (?, ?, ?)";
            try (Connection conn = db.getConnection();
                 PreparedStatement pr = conn.prepareStatement(sql)) {

                pr.setLong(1, userId);
                pr.setBoolean(2, isActivate);
                pr.setBoolean(3, false);
                pr.executeUpdate();

                userCache.invalidate(userId);
                log.info("Пользователь {} успешно добавлен.", userId);

            } catch (SQLException e) {
                log.error("Ошибка при добавлении пользователя {}: {}", userId, e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    public void addGroup(String groupId, boolean isActivate) {
        executor.submit(() -> {
            String sql = "INSERT INTO groups_table(id, is_activate, is_ban) VALUES (?, ?, ?)";
            try (Connection conn = db.getConnection();
                 PreparedStatement pr = conn.prepareStatement(sql)) {

                pr.setString(1, groupId);
                pr.setBoolean(2, isActivate);
                pr.setBoolean(3, false);
                pr.executeUpdate();

                groupCache.invalidate(groupId);
                log.info("Группа {} успешно добавлена.", groupId);

            } catch (SQLException e) {
                log.error("Ошибка при добавлении группы {}: {}", groupId, e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    public void addUserWithGroup(long userId, String groupId, String userBio) {
        executor.submit(() -> {
            String sql = "INSERT INTO user_group_state(user_id, group_id, user_bio) VALUES (?, ?, ?)";
            try (Connection conn = db.getConnection();
                 PreparedStatement pr = conn.prepareStatement(sql)) {

                pr.setLong(1, userId);
                pr.setString(2, groupId);
                pr.setString(3, userBio);
                pr.executeUpdate();

                userGroupStatsCache.put(new UserKey(userId, groupId),
                        new UserStats(userId, groupId, userBio));
                groupUsersCache.invalidate(groupId);

                log.info("Пользователь {} добавлен в группу {}.", userId, groupId);

            } catch (SQLException e) {
                log.error("Ошибка при добавлении пользователя {} в группу {}: {}", userId, groupId, e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    // ------------------- GET -------------------
    public boolean getUserById(long userId) {
        return userCache.get(userId);
    }

    public boolean getGroupById(String groupId) {
        return groupCache.get(groupId);
    }

    public UserStats getUserGroupStats(long userId, String groupId) {
        return userGroupStatsCache.get(new UserKey(userId, groupId));
    }

    public List<Long> getUsersInGroup(String groupId) {
        return groupUsersCache.get(groupId);
    }

    public List<Long> getActiveUsers() {
        List<Long> activeUsers = new ArrayList<>();
        String sql = "SELECT id FROM users_table WHERE is_activate_chat = TRUE AND is_ban = FALSE";
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(sql);
             ResultSet rs = pr.executeQuery()) {
            while (rs.next()) {
                activeUsers.add(rs.getLong("id"));
            }
        } catch (SQLException e) {
            log.error("Ошибка при получении активных пользователей: {}", e.getMessage());
        }
        return activeUsers;
    }

    public List<String> getActiveGroups() {
        List<String> activeGroups = new ArrayList<>();
        String sql = "SELECT id FROM groups_table WHERE is_activate = TRUE AND is_ban = FALSE";
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(sql);
             ResultSet rs = pr.executeQuery()) {
            while (rs.next()) {
                activeGroups.add(rs.getString("id"));
            }
        } catch (SQLException e) {
            log.error("Ошибка при получении активных групп: {}", e.getMessage());
        }
        return activeGroups;
    }

    // ------------------- UPDATE -------------------
    public void setUserActivate(long userId, boolean activate) {
        executor.submit(() -> updateBooleanField("users_table", "is_activate_chat", userId, activate, true));
    }

    public void setUserBan(long userId, boolean ban) {
        executor.submit(() -> updateBooleanField("users_table", "is_ban", userId, ban, true));
    }

    public void setGroupActivate(String groupId, boolean activate) {
        executor.submit(() -> updateBooleanField("groups_table", "is_activate", groupId, activate, false));
    }

    public void setGroupBan(String groupId, boolean ban) {
        executor.submit(() -> updateBooleanField("groups_table", "is_ban", groupId, ban, false));
    }

    public void updateUserBio(long userId, String groupId, String userBio) {
        executor.submit(() -> {
            String sql = "UPDATE user_group_state SET user_bio = ? WHERE user_id = ? AND group_id = ?";
            try (Connection conn = db.getConnection();
                 PreparedStatement pr = conn.prepareStatement(sql)) {

                pr.setString(1, userBio);
                pr.setLong(2, userId);
                pr.setString(3, groupId);
                pr.executeUpdate();

                userGroupStatsCache.put(new UserKey(userId, groupId),
                        new UserStats(userId, groupId, userBio));

                log.info("Биография пользователя {} в группе {} обновлена.", userId, groupId);

            } catch (SQLException e) {
                log.error("Ошибка при обновлении биографии пользователя {} в группе {}: {}", userId, groupId, e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    private void updateBooleanField(String table, String field, Object id, boolean value, boolean isUser) {
        String sql = "UPDATE " + table + " SET " + field + " = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(sql)) {

            pr.setBoolean(1, value);
            if (id instanceof Long) pr.setLong(2, (Long) id);
            else pr.setString(2, (String) id);

            pr.executeUpdate();

            if (isUser) userCache.invalidate((Long) id);
            else groupCache.invalidate((String) id);

            log.info("Обновлено поле {}={} в таблице {} для id={}", field, value, table, id);

        } catch (SQLException e) {
            log.error("Ошибка при обновлении поля {} таблицы {}: {}", field, table, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // ------------------- DELETE -------------------
    public void removeUser(long userId) {
        executor.submit(() -> executeDelete("users_table", userId, true));
    }

    public void removeGroup(String groupId) {
        executor.submit(() -> executeDelete("groups_table", groupId, false));
    }

    public void removeUserFromGroup(long userId, String groupId) {
        executor.submit(() -> {
            String sql = "DELETE FROM user_group_state WHERE user_id = ? AND group_id = ?";
            try (Connection conn = db.getConnection();
                 PreparedStatement pr = conn.prepareStatement(sql)) {

                pr.setLong(1, userId);
                pr.setString(2, groupId);
                pr.executeUpdate();

                userGroupStatsCache.invalidate(new UserKey(userId, groupId));
                groupUsersCache.invalidate(groupId);
                log.info("Пользователь {} удален из группы {}.", userId, groupId);

            } catch (SQLException e) {
                log.error("Ошибка при удалении пользователя {} из группы {}: {}", userId, groupId, e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    public void removeUserBio(long userId, String groupId) {
        executor.submit(() -> {
            String sql = "UPDATE user_group_state SET user_bio = NULL WHERE user_id = ? AND group_id = ?";
            try (Connection conn = db.getConnection();
                 PreparedStatement pr = conn.prepareStatement(sql)) {

                pr.setLong(1, userId);
                pr.setString(2, groupId);
                pr.executeUpdate();

                userGroupStatsCache.put(new UserKey(userId, groupId),
                        new UserStats(userId, groupId, null));

                log.info("Биография пользователя {} в группе {} удалена.", userId, groupId);

            } catch (SQLException e) {
                log.error("Ошибка при удалении биографии пользователя {} в группе {}: {}", userId, groupId, e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    private void executeDelete(String table, Object id, boolean isUser) {
        String sql = "DELETE FROM " + table + " WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(sql)) {

            if (id instanceof Long) pr.setLong(1, (Long) id);
            else pr.setString(1, (String) id);
            pr.executeUpdate();

            if (isUser) userCache.invalidate((Long) id);
            else {
                groupCache.invalidate((String) id);
                groupUsersCache.invalidate((String) id);
            }

            log.info("Удалена запись из таблицы {} для id={}", table, id);

        } catch (SQLException e) {
            log.error("Ошибка при удалении из таблицы {}: {}", table, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // ------------------- PRIVATE LOADERS -------------------
    private static boolean loadUserById(long userId) {
        String sql = "SELECT id FROM users_table WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(sql)) {
            pr.setLong(1, userId);
            ResultSet rs = pr.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            log.error("Ошибка при получении пользователя {}: {}", userId, e.getMessage());
            return false;
        }
    }

    private static boolean loadGroupById(String groupId) {
        String sql = "SELECT id FROM groups_table WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(sql)) {
            pr.setString(1, groupId);
            ResultSet rs = pr.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            log.error("Ошибка при получении группы {}: {}", groupId, e.getMessage());
            return false;
        }
    }

    private static UserStats loadUserGroup(UserKey key) {
        String sql = "SELECT user_id, group_id, user_bio FROM user_group_state WHERE user_id = ? AND group_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(sql)) {
            pr.setLong(1, key.userId());
            pr.setString(2, key.groupId());
            ResultSet rs = pr.executeQuery();
            if (rs.next()) return new UserStats(rs.getLong("user_id"), rs.getString("group_id"), rs.getString("user_bio"));
        } catch (SQLException e) {
            log.error("Ошибка при получении userGroup {}: {}", key, e.getMessage());
        }
        return null;
    }

    private static List<Long> loadUsersInGroupFromDb(String groupId) {
        List<Long> users = new ArrayList<>();
        String sql = "SELECT user_id FROM user_group_state WHERE group_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement pr = conn.prepareStatement(sql)) {
            pr.setString(1, groupId);
            ResultSet rs = pr.executeQuery();
            while (rs.next()) users.add(rs.getLong("user_id"));
        } catch (SQLException e) {
            log.error("Ошибка при загрузке пользователей группы {}: {}", groupId, e.getMessage());
        }
        return users;
    }
}