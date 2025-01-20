package com.example.act_dir.fech_client;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class FetchUserData extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String displayName = request.getParameter("displayName");
        System.out.println("Received request for user: " + displayName); // Log received displayName

        response.setContentType("application/json");
        String userDetails = fetchUserDetails(displayName);
        response.getWriter().write(userDetails);
    }
    private String fetchUserDetails(String displayName) {
        String result = "{}";
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("/home/ram-pt7749/Music/prom/agent/fetch/userFetch", displayName);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                String fullOutput = output.toString();
                System.out.println("Fetched user details: " + fullOutput); // Log fetched details
                // Extract JSON part from the output
                int jsonStartIndex = fullOutput.indexOf("{");
                if (jsonStartIndex != -1) {
                    result = fullOutput.substring(jsonStartIndex);
                }
            } else {
                System.err.println("Error fetching user details. Exit code: " + exitCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.print(result);
        return result;
    }
}