package com.example.act_dir.last_mod_db;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.json.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.*;
import java.text.SimpleDateFormat;
import com.example.act_dir.db.DBConnection;

public class StoreGroupLog extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StringBuilder jsonData = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                jsonData.append(line);
            }
        }
        System.out.println(jsonData.toString() + "------------------------------------------------------");

        JsonObject data;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonData.toString()))) {
            data = jsonReader.readObject();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid JSON format");
            return;
        }

        // Extract fields from JSON
        String name = data.getString("name", null);
        String lastModifiedField = data.getString("lastModifiedField", null);
        String value = data.getString("value", null);
        String whenChangedStr = data.getString("whenChanged", null);
        String uSNChanged = data.getString("uSNChanged", null);

        // Validate required fields
        if (name == null || lastModifiedField == null || value == null || whenChangedStr == null || uSNChanged == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing required fields in JSON");
            return;
        }

        Timestamp whenChanged;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date date = sdf.parse(whenChangedStr);
            whenChanged = new Timestamp(date.getTime());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid 'whenChanged' format. Expected format: yyyy-MM-dd HH:mm:ss");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            if (uSNChangedExists(conn, uSNChanged)) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("No update or insert performed. uSNChanged already exists: " + uSNChanged + "\n");
            } else if (groupExists(conn, name)) {
                updateGroup(conn, name, lastModifiedField, value, whenChanged, uSNChanged);
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("Group updated successfully: " + name + "\n");
            }
            else {
                insertGroup(conn, name, lastModifiedField, value, whenChanged, uSNChanged);
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("Group inserted successfully: " + name + "\n");
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
            response.getWriter().write("Database error: " + e.getMessage());
        }
    }

    private boolean uSNChangedExists(Connection conn, String uSNChanged) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM groupLog WHERE uSNChanged = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, uSNChanged);
            try (ResultSet rs = checkStmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
    private boolean groupExists(Connection conn, String name) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM groupLog WHERE name = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, name);
            try (ResultSet rs = checkStmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
    private void updateGroup(Connection conn, String name, String lastModifiedField, String value, Timestamp whenChanged, String uSNChanged) throws SQLException {
        String updateSQL = "UPDATE groupLog SET " + lastModifiedField + " = CONCAT(IFNULL(" + lastModifiedField + ", ''), IF(LENGTH(IFNULL(" + lastModifiedField + ", '')) > 0, ',', ''), ?), whenChanged = ?, uSNChanged = ? WHERE name = ?";
        try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
            updateStmt.setString(1, value);
            updateStmt.setTimestamp(2, whenChanged);
            updateStmt.setString(3, uSNChanged);
            updateStmt.setString(4, name);
            updateStmt.executeUpdate();
        }
    }

    private void insertGroup(Connection conn, String name, String lastModifiedField, String value, Timestamp whenChanged, String uSNChanged) throws SQLException {
        String insertSQL = "INSERT INTO groupLog (name, " + lastModifiedField + ", whenChanged, uSNChanged) VALUES (?, ?, ?, ?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
            insertStmt.setString(1, name);
            insertStmt.setString(2, value);
            insertStmt.setTimestamp(3, whenChanged);
            insertStmt.setString(4, uSNChanged);
            insertStmt.executeUpdate();
        }
    }
}
