package com.example.act_dir.store_event;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.json.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.example.act_dir.db.DBConnection;
public class StoreUserEventServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StringBuilder jsonData = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                jsonData.append(line);
            }
        }
        JsonObject data;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonData.toString()))) {
            data = jsonReader.readObject();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid JSON format\"}");
            return;
        }
        String timeCreated = data.getString("TimeCreated", null);
        String accountName = data.getString("AccountName", null);
        String message = data.getString("Message", null);

        if (timeCreated == null || accountName == null || message == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing required fields\"}");
            return;
        }
        try (Connection conn = DBConnection.getConnection()) {
            String formattedTime = convertDateFormat(timeCreated);
            if (formattedTime == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid date format\"}");
                return;
            }
            if(insertEvent(conn, formattedTime, accountName, message)) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Event successfully inserted\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Failed to insert event\"}");
            }

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Database error occurred\"}");
            e.printStackTrace();
        }
    }

    private String convertDateFormat(String timeCreatedStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = inputFormat.parse(timeCreatedStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private boolean insertEvent(Connection conn, String timeCreated, String accountName, String message) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM userLogs WHERE TimeCreated = ? AND AccountName = ? AND Message = ?";
        String insertSql = "INSERT INTO userLogs (TimeCreated, AccountName, Message) VALUES (?, ?, ?)";
        try(PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, timeCreated);
            checkStmt.setString(2, accountName);
            checkStmt.setString(3, message);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return false;
            }
        }
        try(PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, timeCreated);
            insertStmt.setString(2, accountName);
            insertStmt.setString(3, message);
            int rowsInserted = insertStmt.executeUpdate();
            return rowsInserted > 0;
        }
    }

}
