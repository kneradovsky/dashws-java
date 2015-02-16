/*
 * Copyright 2015 SBT-Neradovskiy-KL.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.kn.dashws.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kn.dashws.ws.WSUpstreamConnection;

/**
 *
 * @author kneradovsky
 */
@WebServlet(
    asyncSupported = true, 
    urlPatterns = { 
        "/upstreams/*", 
    }, 
    loadOnStartup = 1,
    initParams = { 
        //@WebInitParam(name = "upstream1", value = "ws://10.68.195.104:4041/websocket/connection", description = ""),
        //@WebInitParam(name = "subscribe1", value = "id1,id2,id3", description = "")
    })
public class Upstreams extends HttpServlet {
    
    List<WSUpstreamConnection> upstreams;
    Logger logger = LoggerFactory.getLogger(Upstreams.class);
    
    public Upstreams() {
        upstreams=new LinkedList<>();
    }
    /**
    * @see Servlet#init(ServletConfig)
    */
    @Override
    public void init(ServletConfig config) throws ServletException {
        ServletContext ctx=config.getServletContext();
        Enumeration<String> parNames = ctx.getInitParameterNames();
        while(parNames.hasMoreElements()) {
            String parname=parNames.nextElement();
            if(!parname.startsWith("upstream")) continue;
            if(parname.length()<9) {
                logger.debug("Unnamed upstream");
                continue;
            }
            String upstreamName=parname.substring(8);
            WSUpstreamConnection conn=new WSUpstreamConnection(upstreamName,config.getInitParameter(parname),config.getInitParameter("subscribe"+upstreamName));
            conn.connect();
            upstreams.add(conn);
        }
    }
    
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet Upstreams</title>");            
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet Upstreams at " + request.getContextPath() + "</h1>");
            for(WSUpstreamConnection conn : upstreams) {
                out.println("<div class=\"upstream\"><div class=\"upstreamurl\">"+conn.getURL()+"</div>");
                out.println("<div><div class=\"upstreamsubs\">"+conn.getSubscriptions()+"</div></div>");
            }
            out.println("</body>");
            out.println("</html>");
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
