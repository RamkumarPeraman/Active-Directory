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
public class StoreLog extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StringBuilder jsonData = new StringBuilder();
        String line;
        try(BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                jsonData.append(line);
            }
        }
        System.out.println("-----------" + jsonData);
        JsonObject data;
        try(JsonReader jsonReader = Json.createReader(new StringReader(jsonData.toString()))) {
            data = jsonReader.readObject();
        }
        catch(Exception e){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid JSON format\"}");
            return;
        }
        String timeCreated = data.getString("TimeCreated", null);
        String accountName = data.getString("objectName", null);
        String accountDomain = data.getString("AccountDomain", null);
        String oldValue = data.getString("OldValue", null);
        String newValue = data.getString("NewValue", null);
        String message = data.getString("Message", null);
        String ChangedOn = data.getString("ChangedOn", null);
        String Organization = data.getString("Organization", null);
        if (timeCreated == null || accountName == null || accountDomain == null || oldValue == null || newValue == null || message == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing required fields\"}");
            return;
        }
        System.out.println("Received data: " + data.toString());
        try (Connection conn = DBConnection.getConnection()) {
            String formattedTime = convertDateFormat(timeCreated);
            if (formattedTime == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid date format\"}");
                return;
            }
            System.out.println("Formatted Time: " + formattedTime);
            if (insertEvent(conn, formattedTime, accountName, accountDomain, oldValue, newValue,message,ChangedOn,Organization)) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Event successfully inserted\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Duplicate event, not inserted\"}");
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
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private boolean insertEvent(Connection conn, String timeCreated, String accountName, String accountDomain, String oldValue, String newValue, String message,String ChangedOn, String Organization) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM logs WHERE TimeCreated = ? AND AccountName = ? AND AccountDomain = ? AND OldValue = ? AND NewValue = ? AND Message = ? AND ChangedOn = ? AND Organization = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, timeCreated);
            checkStmt.setString(2, accountName);
            checkStmt.setString(3, accountDomain);
            checkStmt.setString(4, oldValue);
            checkStmt.setString(5, newValue);
            checkStmt.setString(6, message);
            checkStmt.setString(7,ChangedOn);
            checkStmt.setString(8, Organization);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("Duplicate event found, not inserting.");
                    return false;
                }
            }
        }
        String insertSql = "INSERT INTO logs (TimeCreated, AccountName, AccountDomain, OldValue, NewValue, Message,ChangedOn,Organization) VALUES (?, ?, ?, ?, ?, ?,?,?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, timeCreated);
            insertStmt.setString(2, accountName);
            insertStmt.setString(3, accountDomain);
            insertStmt.setString(4, oldValue);
            insertStmt.setString(5, newValue);
            insertStmt.setString(6, message);
            insertStmt.setString(7, ChangedOn);
            insertStmt.setString(8, Organization);
            int rowsInserted = insertStmt.executeUpdate();
            System.out.println("Rows inserted: " + rowsInserted);
            return rowsInserted > 0;
        }
    }
}