package com.example.act_dir.deletedObj;

import com.example.act_dir.db.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeleteGroupServlet extends HttpServlet {

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCorsHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCorsHeaders(response);

        try (JsonReader jsonReader = Json.createReader(request.getInputStream())) {
            JsonObject json = jsonReader.readObject();
            String groupName = json.getString("groupName");

            String resultMessage = deleteGroup(groupName);

            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            JsonObject jsonResponse = Json.createObjectBuilder()
                    .add("status", resultMessage.equals("Group deleted successfully") ? "success" : "failure")
                    .add("message", resultMessage)
                    .build();
            out.print(jsonResponse.toString());
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            PrintWriter out = response.getWriter();
            JsonObject jsonResponse = Json.createObjectBuilder()
                    .add("status", "failure")
                    .add("message", e.getMessage())
                    .build();
            out.print(jsonResponse.toString());
            out.flush();
        }
    }

    private void addCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private String deleteGroup(String groupName) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            // Delete from Active Directory
            ProcessBuilder processBuilder = new ProcessBuilder("/home/ram-pt7749/Music/prom/agent/delete/groupDelete", groupName);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Error deleting group from AD. Exit code: " + exitCode);
                System.err.println(output.toString());
                return output.toString();
            }

            System.out.println("AD Deletion Output: " + output.toString());

            // Delete from MySQL database
            conn = DBConnection.getConnection();
            String sql = "DELETE FROM act WHERE name=?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, groupName);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                return "Failed to delete group from database: No such group found";
            }

            System.out.println("Database deletion successful for group: " + groupName);
            return "Group deleted successfully";
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
}