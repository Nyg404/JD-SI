package io.github.nyg404.db;

import org.yaml.snakeyaml.Yaml;

import java.sql.*;

public class DataConnection {
    private static final String URL = "jdbc:postgresql://localhost:5432/telegram_bot";
    private static final String USER = "bot_user";
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
                id BIGINT PRIMARY KEY,
                is_activate BOOLEAN DEFAULT FALSE,
                is_ban BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
        """;

    private static DataConnection instance;

    private Connection connection;

    private DataConnection() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Подключение к PostgreSQL успешно!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static DataConnection getInstance() {
        if (instance == null) {
            instance = new DataConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    public static void createDefaultTable() {
        try (Statement stmt = getInstance().getConnection().createStatement()) {
            stmt.execute(DEFAULT_USERS_TABLE);
            stmt.execute(DEFAULT_GROUP_TABLE);
            System.out.println("Таблицы users_table и groups_table созданы или уже существуют.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
