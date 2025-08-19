package io.github.nyg404.core.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DataConnection {

    private static final String URL = "jdbc:postgresql://localhost:5432/telegram_db";
    private static final String USER = "bot_users";
    private static final String PASSWORD = "3773";

    private static final String DEFAULT_USERS_TABLE = """
            CREATE TABLE IF NOT EXISTS users_table (
                id BIGINT PRIMARY KEY,
                is_activate_chat BOOLEAN DEFAULT FALSE,
                is_ban BOOLEAN DEFAULT FALSE
            );
        """;

    private static final String DEFAULT_GROUP_TABLE = """
            CREATE TABLE IF NOT EXISTS groups_table (
                id VARCHAR(1000) PRIMARY KEY,
                is_activate BOOLEAN DEFAULT FALSE,
                is_ban BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
        """;

    private static final String DEFAULT_GROUP_TO_USER = """
            CREATE TABLE IF NOT EXISTS user_group_state(
                user_id BIGINT REFERENCES users_table(id),
                group_id VARCHAR(1000) REFERENCES groups_table(id),
                user_bio VARCHAR(1000)
                );
            """;

    private static DataConnection instance;

    private HikariDataSource dataSource;
    // ебнул хикари через гпт:D
    private DataConnection() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(10000);

        dataSource = new HikariDataSource(config);
        System.out.println("Подключение к PostgreSQL успешно через HikariCP!");
    }

    public static DataConnection getInstance() {
        if (instance == null) {
            instance = new DataConnection();
        }
        return instance;
    }


    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }


    public static void createDefaultTable() {
        try (Connection conn = getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(DEFAULT_USERS_TABLE);
            stmt.execute(DEFAULT_GROUP_TABLE);
            stmt.execute(DEFAULT_GROUP_TO_USER);
            System.out.println("Таблицы users_table и groups_table созданы или уже существуют.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
