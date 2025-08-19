package io.github.nyg404.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.nyg404.core.dao.GroupDao;
import io.github.nyg404.core.dao.UserDao;
import io.github.nyg404.core.dao.UserGroupDao;
import io.github.nyg404.core.models.UserKey;
import io.github.nyg404.core.models.UserStats;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CacheManager {

    private static CacheManager instance;
    private final UserDao userDao;
    private final GroupDao groupDao;
    private final UserGroupDao userGroupDao;

    // Caches
    private final LoadingCache<Long, Boolean> userCache;
    private final LoadingCache<String, Boolean> groupCache;
    private final LoadingCache<UserKey, UserStats> userGroupStatsCache;
    private final LoadingCache<String, List<Long>> groupUsersCache;

    private CacheManager() {
        this.userDao = new UserDao();
        this.groupDao = new GroupDao();
        this.userGroupDao = new UserGroupDao();

        this.userCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build(this::loadUserById);

        this.groupCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(5_000)
                .build(this::loadGroupById);

        this.userGroupStatsCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(50_000)
                .build(this::loadUserGroup);

        this.groupUsersCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(2_000)
                .build(this::loadUsersInGroupFromDb);
    }

    public static CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }

    // Cache access methods
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

    // Cache invalidation methods
    public void invalidateUserCache(long userId) {
        userCache.invalidate(userId);
        log.info("User cache invalidated for userId={}", userId);
    }

    public void invalidateGroupCache(String groupId) {
        groupCache.invalidate(groupId);
        groupUsersCache.invalidate(groupId);
        log.info("Group cache invalidated for groupId={}", groupId);
    }

    public void invalidateUserGroupCache(long userId, String groupId) {
        userGroupStatsCache.invalidate(new UserKey(userId, groupId));
        groupUsersCache.invalidate(groupId);
        log.info("UserGroup cache invalidated for userId={}, groupId={}", userId, groupId);
    }

    // Private loader methods
    private Boolean loadUserById(long userId) {
        return userDao.existsById(userId);
    }

    private Boolean loadGroupById(String groupId) {
        return groupDao.existsById(groupId);
    }

    private UserStats loadUserGroup(UserKey key) {
        return userGroupDao.getUserGroup(key.userId(), key.groupId());
    }

    private List<Long> loadUsersInGroupFromDb(String groupId) {
        return userGroupDao.getUsersInGroup(groupId);
    }
}