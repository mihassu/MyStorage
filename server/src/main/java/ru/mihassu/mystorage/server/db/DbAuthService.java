package ru.mihassu.mystorage.server.db;

import java.sql.*;

public class DbAuthService {

    private static Connection connection;
    private static Statement stmt;


    public String getNicknameByLoginPass(String login, String password) {

        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM users;");

            while (rs.next()) {
                if (rs.getString("login").equals(login) && rs.getString("password").equals(password)) {
                    return rs.getString("nickname");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public int getIdByLoginPass(String login, String password) {

        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM users;");

            while (rs.next()) {
                if (rs.getString("login").equals(login) && rs.getString("password").equals(password)) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public String getNicknameById(int id) {

        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM users;");

            while (rs.next()) {
                if (rs.getInt("id") == id) {
                    return rs.getString("nickname");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }


    public void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:usersData.db");
        stmt = connection.createStatement();
    }

    public void disconnect() {

        try {
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
