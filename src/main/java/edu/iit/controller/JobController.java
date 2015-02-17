/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iit.controller;

import edu.iit.doa.DOA;
import edu.iit.model.User_Jobs;
import edu.iit.s3bucket.S3Bucket;
import edu.iit.scheduler.Scheduler;
import edu.iit.sqs.SendQueue;
import edu.iit.walrus.Walrus;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

/**
 *
 * @author supramo
 */
@MultipartConfig()
public class JobController extends HttpServlet {

    
    
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
            out.println("<title>Servlet Controller</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet Controller at " + request.getContextPath() + "</h1>");
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
        Walrus walrus = new Walrus();
        PrintWriter out = response.getWriter();
        System.out.println("from getr" + request.getServletPath());
        String path = request.getRequestURI().substring(request.getContextPath().length());
        System.out.println("path is" + path);
        HttpSession session = request.getSession();
        switch (path) {

            case "/app/index":
                System.out.println("awesome sai" + walrus.getObjects("sat-hadoop").toString());
                session.setAttribute("datasets", walrus.getObjects("sat-hadoop"));
                response.sendRedirect(request.getContextPath() + "/index.jsp");
                break;
            default:
                out.write("Page not found");
                break;
            
        }

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
        Walrus walrus = new Walrus();
        PrintWriter out = response.getWriter();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        System.out.println("path is" + path);
        System.out.println("req path is" + request.getServletPath());
        HttpSession session = request.getSession();
        switch (path) {
            case "/app/uploadfile":
                List filepaths = new ArrayList();
                // constructs path of the directory to save uploaded file
                walrus.createBucket("sat-hadoop");
                for (Part part : request.getParts()) {

                    String fileName = extractFileName(part);
                    if (fileName.isEmpty()) {
                        continue;
                    }


                    File file = new File("/tmp/"+fileName); // Or File#createTempFile() if you want to autogenerate an unique name.

                    try (InputStream input = part.getInputStream()) {
                        Files.copy(input, file.toPath()); // How to obtain part is answered in http://stackoverflow.com/a/2424824
                        walrus.putObject("sat-hadoop", file.getAbsolutePath());
                        //filepaths.add(file.toPath());
                    }
                }

                //walrus.createBucket("sat-hadoop");
                //for (int i=0 ; i<filepaths.size();i++)
                  //  walrus.putObject("sat-hadoop", filepaths.get(i).toString());
                session.setAttribute("message", "Upload has been done successfully!");
                request.getSession().setAttribute("datasets", walrus.getObjects("sat-hadoop"));
                response.sendRedirect(request.getContextPath() + "/index.jsp");
                break;
            case "/app/submitjob":
                String message = "Your Job is submitted, you will be emailed once completed";
                String nodes = request.getParameter("optionnode");
                String jobname = request.getParameter("optionjob");
                String dataset = request.getParameter("datasets");
                session.setAttribute("message", message);

                //s3output.createBucket();
                DOA doa = new DOA();

                //  Adding job to the database
                User_Jobs userjob = new User_Jobs();
                userjob.setInputurl(dataset);
                userjob.setOutputurl("");
                userjob.setUserid("sai");
                userjob.setJobstatus("INITIAL");
                userjob.setNodes(nodes);
                userjob.setJobname(jobname);
                String randomId = UUID.randomUUID().toString();
                userjob.setJobid(randomId);
                System.out.println(userjob.toString());
                doa.addJob(userjob);
                SendQueue sendqueue = new SendQueue();
                sendqueue.sendMessage(randomId);
                
                request.getSession().setAttribute("datasets", walrus.getObjects("sat-hadoop"));
                //request.setAttribute("message", "Your Job is submitted, you will be emailed once completed");
                //out.write("Sai is awesome");
                response.sendRedirect(request.getContextPath() + "/index.jsp");
                //request.getRequestDispatcher("/index.jsp").forward(request, response);
                break;
                
            case "/app/deletefile":
                String filename = request.getParameter("filetodelete");
                walrus.delObject("sat-hadoop", filename);
                session.setAttribute("message", "successfully deleted");
                session.setAttribute("datasets", walrus.getObjects("sat-hadoop"));
                response.sendRedirect(request.getContextPath() + "/index.jsp");
                break;
            default:
                out.write("Page not found");
                break;

        }
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

    private String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] items = contentDisp.split(";");
        for (String s : items) {
            if (s.trim().startsWith("filename")) {
                return s.substring(s.indexOf("=") + 2, s.length() - 1);
            }
        }
        return "";
    }
}
