//package com.example.act_dir.fetch_last_mod;
//
//import com.example.act_dir.db.DBConnection;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServlet;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//
//import java.io.IOException;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//
//public class FetchUserLog extends HttpServlet {
//
//    public static String fetchUserLogs(String accountName) {
//        String query = "SELECT * FROM logs WHERE AccountName = ? order by TimeCreated desc";
//        JSONArray jsonResponse = new JSONArray();
//
//        try (Connection conn = DBConnection.getConnection();
//             PreparedStatement stmt = conn.prepareStatement(query)) {
//
//            stmt.setString(1, accountName);
//            try (ResultSet rs = stmt.executeQuery()) {
//                while (rs.next()) {
//                    JSONObject logEntry = new JSONObject();
//                    logEntry.put("id", rs.getInt("id"));
//                    logEntry.put("TimeCreated", rs.getTimestamp("TimeCreated").toString());
//                    logEntry.put("AccountName", rs.getString("AccountName"));
//                    logEntry.put("AccountDomain", rs.getString("AccountDomain"));
//                    logEntry.put("OldValue", rs.getString("OldValue"));
//                    logEntry.put("NewValue", rs.getString("NewValue"));
//                    logEntry.put("Message", rs.getString("Message"));
//                    logEntry.put("ChangedOn", rs.getString("ChangedOn"));
//                    jsonResponse.add(logEntry);
//                }
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//            JSONObject error = new JSONObject();
//            error.put("error", "Database error: " + e.getMessage());
//            return error.toString();
//        }
//        System.out.println(jsonResponse.toString());
//        return jsonResponse.toString();
//    }
//
//    @Override
//    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        response.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
//        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
//        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
//
//        String accountName = request.getParameter("accountName");
//
//        System.out.println("-----" + accountName);
//
//        if (accountName != null && !accountName.isEmpty()) {
//            String userLogs = fetchUserLogs(accountName).trim();
//            response.setContentType("application/json");
//            response.getWriter().write(userLogs);
//        } else {
//            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//            response.getWriter().write("{\"error\": \"Missing accountName parameter\"}");
//        }
//    }
//}


package com.example.act_dir.fetch_last_mod;

import com.example.act_dir.db.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class FetchUserLog extends HttpServlet {

    public static String fetchUserLogs(String accountName) {
        String query = "SELECT * FROM logs WHERE AccountName = ? order by TimeCreated desc";
        JSONArray jsonResponse = new JSONArray();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, accountName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject logEntry = new JSONObject();
                    logEntry.put("id", rs.getInt("id"));
                    Timestamp timestamp = rs.getTimestamp("TimeCreated");
                    if (timestamp != null) {
                        logEntry.put("TimeCreated", sdf.format(timestamp));
                    }
                    logEntry.put("AccountName", rs.getString("AccountName"));
                    logEntry.put("AccountDomain", rs.getString("AccountDomain"));
                    logEntry.put("OldValue", rs.getString("OldValue"));
                    logEntry.put("NewValue", rs.getString("NewValue"));
                    logEntry.put("Message", rs.getString("Message"));
                    logEntry.put("ChangedOn", rs.getString("ChangedOn"));
                    jsonResponse.add(logEntry);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JSONObject error = new JSONObject();
            error.put("error", "Database error: " + e.getMessage());
            return error.toString();
        }
        System.out.println(jsonResponse.toString());
        return jsonResponse.toString();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        String accountName = request.getParameter("accountName");

        System.out.println("-----" + accountName);

        if (accountName != null && !accountName.isEmpty()) {
            String userLogs = fetchUserLogs(accountName).trim();
            response.setContentType("application/json");
            response.getWriter().write(userLogs);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Missing accountName parameter\"}");
        }
    }
}