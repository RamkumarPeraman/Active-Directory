package com.example.act_dir.recover;

import com.example.act_dir.db.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
public class RecoverComputerServlet extends HttpServlet {
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setCORSHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setCORSHeaders(response);
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        String recoverAccountName = request.getParameter("recoverAccountName");
        String recoverTimeCreated = request.getParameter("recoverTimeCreated");

        System.out.println(recoverAccountName +"------------"+recoverTimeCreated);

        if (recoverAccountName == null || recoverTimeCreated == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Missing parameters\"}");
            return;
        }
        try (Connection conn = DBConnection.getConnection()) {
//            String query = "SELECT OldValue, changedOn, AccountName, Organization FROM logs WHERE AccountName = ? AND TimeCreated = ?";
            String query = "select l1.changedOn, l1.OldValue , l1.AccountName, l1.Organization from logs l1 where l1.TimeCreated=(select MIN(l2.TimeCreated) from logs l2 where l2.changedOn = l1.changedOn and l2.TimeCreated >= ? and l2.accountName = l1.accountName) and l1.changedOn in ('description', 'mail', 'givenName') and l1.accountName = ? order by l1.changedOn;" ;
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, recoverTimeCreated);
                stmt.setString(2, recoverAccountName);
                try (ResultSet rs = stmt.executeQuery()) {
                    ArrayList<String> oldValues = new ArrayList<>();
                    ArrayList<String> changedOns = new ArrayList<>();
                    ArrayList<String> accountNames = new ArrayList<>();
                    ArrayList<String> organizations = new ArrayList<>();
                    while(rs.next()){
                        oldValues.add(rs.getString("OldValue"));
                        changedOns.add(rs.getString("changedOn"));
                        accountNames.add(rs.getString("AccountName"));
                        organizations.add(rs.getString("Organization"));
                    }

                    if(oldValues.isEmpty()){
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("{\"error\": \"No matching records found\"}");
                        return;
                    }
                    JSONArray jsonArray = new JSONArray();
                    for (int i = 0; i < oldValues.size(); i++)
                    {
                        String oldValue = oldValues.get(i);
                        String changedOn = changedOns.get(i);
                        String accountName = accountNames.get(i);
                        String organization = organizations.get(i);
                        String filePath;
                        String executablePath;
                        if("computer".equalsIgnoreCase(organization)) {
                            filePath = "/home/ram-pt7749/Music/prom/agent/recover/computerRecover";
                            executablePath = "/home/ram-pt7749/Music/prom/agent/recover/computerRecover";
                        }
                        else{
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            response.getWriter().write("{\"error\": \"Invalid organization type. This service is only for computers.\"}");
                            return;
                        }
                        try(FileWriter writer = new FileWriter(filePath, true)) {
                            writer.write("OldValue: " + oldValue + "\n");
                            writer.write("ChangedOn: " + changedOn + "\n");
                            writer.write("AccountName: " + accountName + "\n");
                            writer.write("Organization: " + organization + "\n");
                            writer.write("\n");
                        }
                        System.out.println(oldValue + "--" + changedOn + "--" + accountName + "--" + organization);
                        String[] cmd = {executablePath, accountName, changedOn, oldValue};
                        Process process = Runtime.getRuntime().exec(cmd);
                        StringBuilder output = new StringBuilder();
                        try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                output.append(line).append("\n");
                            }
                        }
                        int exitCode = process.waitFor();
                        if(exitCode != 0) {
                            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            response.getWriter().write("{\"error\": \"C++ program execution failed\"}");
                            return;
                        }
                        JSONObject jsonResponse = new JSONObject();
                        jsonResponse.put("status", "success");
                        jsonResponse.put("message", "Data successfully recovered and written to " + filePath);
                        jsonResponse.put("cppOutput", output.toString());
                        jsonArray.add(jsonResponse);
                    }
                    response.setContentType("application/json");
                    PrintWriter out = response.getWriter();
                    out.println(jsonArray.toString());
                }
            }
        }
        catch(SQLException | InterruptedException e) {
            throw new ServletException("Database error", e);
        }
    }
    private void setCORSHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}