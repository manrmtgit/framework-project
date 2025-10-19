package com.manoa.core;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * FrontServlet : url interceptor
 */
public class FrontServlet extends HttpServlet {

    private Map<String, String> urlMappings;

    @Override
    public void init() throws ServletException {
        super.init();
        urlMappings = new HashMap<>();
        initializeUrlMappings();
    }

    private void initializeUrlMappings() {
        urlMappings.put("/hello", "/WEB-INF/views/hello.jsp");
        urlMappings.put("/home", "/WEB-INF/views/home.jsp");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestURI.substring(contextPath.length());

        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Framework MVC - " + path + "</title>");
            out.println("<style>");
            out.println("body { font-family: Arial, sans-serif; margin: 40px; }");
            out.println(".container { max-width: 800px; margin: 0 auto; }");
            out.println(".url-info { background: #f5f5f5; padding: 20px; border-radius: 5px; }");
            out.println(".mapping { color: #007bff; }");
            out.println("</style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<div class='container'>");
            out.println("<h1>Framework MVC - FrontServlet</h1>");

            if (urlMappings.containsKey(path)) {
                request.getRequestDispatcher(urlMappings.get(path)).forward(request, response);
            } else {
                out.println("<div class='url-info'>");
                out.println("<h2>URL non mappée: " + path + "</h2>");
                out.println("<p><strong>Information:</strong> Cette URL n'est pas définie dans les mappings du FrontServlet</p>");
                out.println("<p class='mapping'>✗ URL non mappée - Affichage de l'URL demandée</p>");
                out.println("</div>");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}