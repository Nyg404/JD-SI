package io.github.nyg404.service;

import io.github.nyg404.cache.CacheManager;
import io.github.nyg404.dao.GroupDao;
import io.github.nyg404.dao.UserDao;
import io.github.nyg404.dao.UserGroupDao;
import io.github.nyg404.models.UserStats;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DataService {

    private static DataService instance;
    private final UserDao userDao;
    private final GroupDao groupDao;
    private final UserGroupDao userGroupDao;
    private final CacheManager cacheManager;
    private final ThreadPoolExecutor executor;

    private DataService() {
        this.userDao = new UserDao();
        this.groupDao = new GroupDao();
        this.userGroupDao = new UserGroupDao();
        this.cacheManager = CacheManager.getInstance();
        this.executor = new ThreadPoolExecutor(
                5, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>()
        );
    }

    public static DataService getInstance() {
        if (instance == null) {
            instance = new DataService();
        }
        return instance;
    }

    // User operations
    public void addUser(long userId, boolean isActivate) {
        executor.submit(() -> {
            try {
                userDao.addUser(userId, isActivate);
                cacheManager.invalidateUserCache(userId);
                log.info("Пользователь {} успешно добавлен.", userId);
            } catch (Exception e) {
                log.error("Ошибка при добавлении пользователя {}: {}", userId, e.getMessage());
                throw e;
            }
        });
    }

    public void setUserActivate(long userId, boolean activate) {
        executor.submit(() -> {
            try {
                userDao.updateActivate(userId, activate);
                cacheManager.invalidateUserCache(userId);
                log.info("Активация пользователя {} обновлена: {}", userId, activate);
            } catch (Exception e) {
                log.error("Ошибка при обновлении активации пользователя {}: {}", userId, e.getMessage());
                throw e;
            }
        });
    }

    public void setUserBan(long userId, boolean ban) {
        executor.submit(() -> {
            try {
                userDao.updateBan(userId, ban);
                cacheManager.invalidateUserCache(userId);
                log.info("Бан пользователя {} обновлен: {}", userId, ban);
            } catch (Exception e) {
                log.error("Ошибка при обновлении бана пользователя {}: {}", userId, e.getMessage());
                throw e;
            }
        });
    }

    public void removeUser(long userId) {
        executor.submit(() -> {
            try {
                userDao.deleteUser(userId);
                cacheManager.invalidateUserCache(userId);
                log.info("Пользователь {} удален.", userId);
            } catch (Exception e) {
                log.error("Ошибка при удалении пользователя {}: {}", userId, e.getMessage());
                throw e;
            }
        });
    }

    public boolean getUserById(long userId) {
        return cacheManager.getUserById(userId);
    }

    public List<Long> getActiveUsers() {
        return userDao.getActiveUsers();
    }

    // Group operations
    public void addGroup(String groupId, boolean isActivate) {
        executor.submit(() -> {
            try {
                groupDao.addGroup(groupId, isActivate);
                cacheManager.invalidateGroupCache(groupId);
                log.info("Группа {} успешно добавлена.", groupId);
            } catch (Exception e) {
                log.error("Ошибка при добавлении группы {}: {}", groupId, e.getMessage());
                throw e;
            }
        });
    }

    public void setGroupActivate(String groupId, boolean activate) {
        executor.submit(() -> {
            try {
                groupDao.updateActivate(groupId, activate);
                cacheManager.invalidateGroupCache(groupId);
                log.info("Активация группы {} обновлена: {}", groupId, activate);
            } catch (Exception e) {
                log.error("Ошибка при обновлении активации группы {}: {}", groupId, e.getMessage());
                throw e;
            }
        });
    }

    public void setGroupBan(String groupId, boolean ban) {
        executor.submit(() -> {
            try {
                groupDao.updateBan(groupId, ban);
                cacheManager.invalidateGroupCache(groupId);
                log.info("Бан группы {} обновлен: {}", groupId, ban);
            } catch (Exception e) {
                log.error("Ошибка при обновлении бана группы {}: {}", groupId, e.getMessage());
                throw e;
            }
        });
    }

    public void removeGroup(String groupId) {
        executor.submit(() -> {
            try {
                groupDao.deleteGroup(groupId);
                cacheManager.invalidateGroupCache(groupId);
                log.info("Группа {} удалена.", groupId);
            } catch (Exception e) {
                log.error("Ошибка при удалении группы {}: {}", groupId, e.getMessage());
                throw e;
            }
        });
    }

    public boolean getGroupById(String groupId) {
        return cacheManager.getGroupById(groupId);
    }

    public List<String> getActiveGroups() {
        return groupDao.getActiveGroups();
    }

    // User-Group operations
    public void addUserToGroup(long userId, String groupId, String userBio) {
        executor.submit(() -> {
            try {
                userGroupDao.addUserToGroup(userId, groupId, userBio);
                cacheManager.invalidateUserGroupCache(userId, groupId);
                log.info("Пользователь {} добавлен в группу {}.", userId, groupId);
            } catch (Exception e) {
                log.error("Ошибка при добавлении пользователя {} в группу {}: {}", userId, groupId, e.getMessage());
                throw e;
            }
        });
    }

    public void updateUserBio(long userId, String groupId, String userBio) {
        executor.submit(() -> {
            try {
                userGroupDao.updateUserBio(userId, groupId, userBio);
                cacheManager.invalidateUserGroupCache(userId, groupId);
                log.info("Биография пользователя {} в группе {} обновлена.", userId, groupId);
            } catch (Exception e) {
                log.error("Ошибка при обновлении биографии пользователя {} в группе {}: {}", userId, groupId, e.getMessage());
                throw e;
            }
        });
    }

    public void removeUserFromGroup(long userId, String groupId) {
        executor.submit(() -> {
            try {
                userGroupDao.removeUserFromGroup(userId, groupId);
                cacheManager.invalidateUserGroupCache(userId, groupId);
                log.info("Пользователь {} удален из группы {}.", userId, groupId);
            } catch (Exception e) {
                log.error("Ошибка при удалении пользователя {} из группы {}: {}", userId, groupId, e.getMessage());
                throw e;
            }
        });
    }

    public void removeUserBio(long userId, String groupId) {
        executor.submit(() -> {
            try {
                userGroupDao.updateUserBio(userId, groupId, null);
                cacheManager.invalidateUserGroupCache(userId, groupId);
                log.info("Биография пользователя {} в группе {} удалена.", userId, groupId);
            } catch (Exception e) {
                log.error("Ошибка при удалении биографии пользователя {} в группе {}: {}", userId, groupId, e.getMessage());
                throw e;
            }
        });
    }

    public UserStats getUserGroupStats(long userId, String groupId) {
        return cacheManager.getUserGroupStats(userId, groupId);
    }

    public List<Long> getUsersInGroup(String groupId) {
        return cacheManager.getUsersInGroup(groupId);
    }
}