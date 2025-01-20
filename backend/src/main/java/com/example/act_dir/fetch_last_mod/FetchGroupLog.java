package com.example.act_dir.fetch_last_mod;

import com.example.act_dir.db.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FetchGroupLog extends HttpServlet {

    public static String fetchGroupDetails(String groupName) {
        String query = "SELECT * FROM groupLog WHERE name = ?";
        JSONObject jsonResponse = new JSONObject();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, groupName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    jsonResponse.put("name", rs.getString("name"));
                    jsonResponse.put("mail", rs.getString("mail"));
                    jsonResponse.put("description", rs.getString("description"));
                    jsonResponse.put("whenChanged", rs.getTimestamp("whenChanged").toString());
                } else {
                    jsonResponse.put("message", "Group not found");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            jsonResponse.put("error", "Database error: " + e.getMessage());
        }

        return jsonResponse.toString();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Set CORS headers
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:4200"); // Allow frontend origin
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE"); // Allowed HTTP methods
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization"); // Allowed headers

        String groupName = request.getParameter("groupName");

        if (groupName != null && !groupName.isEmpty()) {
            String groupDetails = fetchGroupDetails(groupName);
            response.setContentType("application/json");
            response.getWriter().write(groupDetails);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Missing groupName parameter\"}");
        }
    }
}
