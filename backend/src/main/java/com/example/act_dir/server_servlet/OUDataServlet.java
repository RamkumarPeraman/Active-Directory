package com.example.act_dir.server_servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.example.act_dir.db.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class OUDataServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(OUDataServlet.class.getName());

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Get parameters for multiple OUs
        String[] ouNames = request.getParameterValues("ouName");
        String[] descriptions = request.getParameterValues("description");
        String type = request.getParameter("type");

        // Prepare lists to hold the results
        List<String> successfulInserts = new ArrayList<>();
        List<String> failedInserts = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection()) {
            for (int i = 0; i < ouNames.length; i++) {
                String ouName = ouNames[i];
                String description = descriptions[i];
//                String type = types[i];

                if (ouExists(conn, ouName, description)) {
                    failedInserts.add(ouName); // Add failed OU to the list
                } else {
                    if (insertOU(conn, ouName, type, description)) {
                        successfulInserts.add(ouName); // Add successful OU to the list
                    } else {
                        failedInserts.add(ouName); // Add failed OU to the list
                    }
                }
            }
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("{\"successfulInserts\":" + successfulInserts + ", \"failedInserts\":" + failedInserts + "}");

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database connection error", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("{\"error\":\"Unable to connect to the database.\"}");
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Set the response content type
        response.setContentType("text/html");

        // Write the response
        response.getWriter().println("<html><body><h1>OU Data Inserted</h1></body></html>");
    }

    private boolean ouExists(Connection conn, String ouName, String description) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM act_dit WHERE name = ? AND description = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, ouName);
            checkStmt.setString(2, description);

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean insertOU(Connection conn, String ouName, String type, String description) throws SQLException {
        String insertSql = "INSERT INTO act_dit (type, name, description, isDeleted) VALUES (?, ?, ?, 'NO')";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, type);
            insertStmt.setString(2, ouName);
            insertStmt.setString(3, description);
            int rowsInserted = insertStmt.executeUpdate();
            return rowsInserted > 0;
        }
    }
}
