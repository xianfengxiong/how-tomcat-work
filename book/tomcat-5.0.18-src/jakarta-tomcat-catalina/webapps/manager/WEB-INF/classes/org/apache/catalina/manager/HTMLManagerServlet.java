/*
* $Header: /home/cvs/jakarta-tomcat-catalina/webapps/manager/WEB-INF/classes/org/apache/catalina/manager/HTMLManagerServlet.java,v 1.9 2003/11/03 22:01:38 remm Exp $
* $Revision: 1.9 $
* $Date: 2003/11/03 22:01:38 $
*
* ====================================================================
*
* The Apache Software License, Version 1.1
*
* Copyright (c) 1999 The Apache Software Foundation.  All rights
* reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in
*    the documentation and/or other materials provided with the
*    distribution.
*
* 3. The end-user documentation included with the redistribution, if
*    any, must include the following acknowlegement:
*       "This product includes software developed by the
*        Apache Software Foundation (http://www.apache.org/)."
*    Alternately, this acknowlegement may appear in the software itself,
*    if and wherever such third-party acknowlegements normally appear.
*
* 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
*    Foundation" must not be used to endorse or promote products derived
*    from this software without prior written permission. For written
*    permission, please contact apache@apache.org.
*
* 5. Products derived from this software may not be called "Apache"
*    nor may "Apache" appear in their names without prior written
*    permission of the Apache Group.
*
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
* SUCH DAMAGE.
* ====================================================================
*
* This software consists of voluntary contributions made by many
* individuals on behalf of the Apache Software Foundation.  For more
* information on the Apache Software Foundation, please see
* <http://www.apache.org/>.
*
* [Additional notices, if required by prior licensing conditions]
*
*/


package org.apache.catalina.manager;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.util.ServerInfo;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileUploadException;

/**
* Servlet that enables remote management of the web applications deployed
* within the same virtual host as this web application is.  Normally, this
* functionality will be protected by a security constraint in the web
* application deployment descriptor.  However, this requirement can be
* relaxed during testing.
* <p>
* The difference between the <code>ManagerServlet</code> and this
* Servlet is that this Servlet prints out a HTML interface which
* makes it easier to administrate.
* <p>
* However if you use a software that parses the output of
* <code>ManagerServlet</code you won't be able to upgrade
* to this Servlet since the output are not in the
* same format ar from <code>ManagerServlet</code>
*
* @author Bip Thelin
* @author Malcolm Edgar
* @author Glenn L. Nielsen
* @version $Revision: 1.9 $, $Date: 2003/11/03 22:01:38 $
* @see ManagerServlet
*/

public final class HTMLManagerServlet extends ManagerServlet {

    // --------------------------------------------------------- Public Methods

    /**
     * Process a GET request for the specified resource.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet-specified error occurs
     */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException {

        // Identify the request parameters that we need
        String command = request.getPathInfo();

        String path = request.getParameter("path");
        String deployPath = request.getParameter("deployPath");
        String deployConfig = request.getParameter("deployConfig");
        String deployWar = request.getParameter("deployWar");

        // Prepare our output writer to generate the response message
        response.setContentType("text/html; charset=" + Constants.CHARSET);

        String message = "";
        // Process the requested command
        if (command == null || command.equals("/")) {
        } else if (command.equals("/deploy")) {
            message = deployInternal(deployConfig, deployPath, deployWar);
        } else if (command.equals("/list")) {
        } else if (command.equals("/reload")) {
            message = reload(path);
        } else if (command.equals("/undeploy")) {
            message = undeploy(path);
        } else if (command.equals("/sessions")) {
            message = sessions(path);
        } else if (command.equals("/start")) {
            message = start(path);
        } else if (command.equals("/stop")) {
            message = stop(path);
        } else {
            message =
                sm.getString("managerServlet.unknownCommand", command);
        }

        list(request, response, message);
    }

    /**
     * Process a POST request for the specified resource.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet-specified error occurs
     */
    public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException {

        // Identify the request parameters that we need
        String command = request.getPathInfo();

        if (command == null || !command.equals("/upload")) {
            doGet(request,response);
            return;
        }

        // Prepare our output writer to generate the response message
        response.setContentType("text/html; charset=" + Constants.CHARSET);

        String message = "";

        // Create a new file upload handler
        DiskFileUpload upload = new DiskFileUpload();

        // Get the tempdir
        File tempdir = (File) getServletContext().getAttribute
            ("javax.servlet.context.tempdir");
        // Set upload parameters
        upload.setSizeMax(-1);
        upload.setRepositoryPath(tempdir.getCanonicalPath());
    
        // Parse the request
        String basename = null;
        File appBaseDir = null;
        String war = null;
        FileItem warUpload = null;
        try {
            List items = upload.parseRequest(request);
        
            // Process the uploaded fields
            Iterator iter = items.iterator();
            while (iter.hasNext()) {
                FileItem item = (FileItem) iter.next();
        
                if (!item.isFormField()) {
                    if (item.getFieldName().equals("deployWar") &&
                        warUpload == null) {
                        warUpload = item;
                    } else {
                        item.delete();
                    }
                }
            }
            while (true) {
                if (warUpload == null) {
                    message = sm.getString
                        ("htmlManagerServlet.deployUploadNoFile");
                    break;
                }
                war = warUpload.getName();
                if (!war.toLowerCase().endsWith(".war")) {
                    message = sm.getString
                        ("htmlManagerServlet.deployUploadNotWar",war);
                    break;
                }
                // Get the filename if uploaded name includes a path
                if (war.lastIndexOf('\\') >= 0) {
                    war = war.substring(war.lastIndexOf('\\') + 1);
                }
                if (war.lastIndexOf('/') >= 0) {
                    war = war.substring(war.lastIndexOf('/') + 1);
                }
                // Identify the appBase of the owning Host of this Context
                // (if any)
                String appBase = null;
                appBase = ((Host) context.getParent()).getAppBase();
                appBaseDir = new File(appBase);
                if (!appBaseDir.isAbsolute()) {
                    appBaseDir = new File(System.getProperty("catalina.base"),
                                          appBase);
                }
                basename = war.substring(0, war.indexOf(".war"));
                File file = new File(appBaseDir, war);
                if (file.exists()) {
                    message = sm.getString
                        ("htmlManagerServlet.deployUploadWarExists",war);
                    break;
                }
                warUpload.write(file);
                try {
                    URL url = file.toURL();
                    war = url.toString();
                    war = "jar:" + war + "!/";
                } catch(MalformedURLException e) {
                    file.delete();
                    throw e;
                }
                break;
            }
        } catch(Exception e) {
            message = sm.getString
                ("htmlManagerServlet.deployUploadFail", e.getMessage());
            log(message, e);
        } finally {
            if (warUpload != null) {
                warUpload.delete();
            }
            warUpload = null;
        }

        // Extract the nested context deployment file (if any)
        File localWar = new File(appBaseDir, basename + ".war");
        File localXml = new File(configBase, basename + ".xml");
        try {
            extractXml(localWar, localXml);
        } catch (IOException e) {
            log("managerServlet.extract[" + localWar + "]", e);
            return;
        }
        String config = null;
        try {
            if (localXml.exists()) {
                URL url = localXml.toURL();
                config = url.toString();
            }
        } catch (MalformedURLException e) {
            throw e;
        }

        // If there were no errors, deploy the WAR
        if (message.length() == 0) {
            message = deployInternal(config, null, war);
        }

        list(request, response, message);
    }

    /**
     * Deploy an application for the specified path from the specified
     * web application archive.
     *
     * @param config URL of the context configuration file to be deployed
     * @param path Context path of the application to be deployed
     * @param war URL of the web application archive to be deployed
     * @return message String
     */
    protected String deployInternal(String config, String path, String war) {

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        super.deploy(printWriter, config, path, war, false);

        return stringWriter.toString();
    }

    /**
     * Render a HTML list of the currently active Contexts in our virtual host,
     * and memory and server status information.
     *
     * @param writer Writer to render to
     * @param message a message to display
     */
    public void list(HttpServletRequest request,
                     HttpServletResponse response,
                     String message) throws IOException {

        if (debug >= 1)
            log("list: Listing contexts for virtual host '" +
                deployer.getName() + "'");

        PrintWriter writer = response.getWriter();

        // HTML Header Section
        writer.print(Constants.HTML_HEADER_SECTION);

        // Body Header Section
        Object[] args = new Object[2];
        args[0] = request.getContextPath();
        args[1] = sm.getString("htmlManagerServlet.title");
        writer.print(MessageFormat.format
                     (Constants.BODY_HEADER_SECTION, args));

        // Message Section
        args = new Object[3];
        args[0] = sm.getString("htmlManagerServlet.messageLabel");
        args[1] = (message == null || message.length() == 0) ? "OK" : message;
        writer.print(MessageFormat.format(Constants.MESSAGE_SECTION, args));

        // Manager Section
        args = new Object[9];
        args[0] = sm.getString("htmlManagerServlet.manager");
        args[1] = response.encodeURL(request.getContextPath() + "/html/list");
        args[2] = sm.getString("htmlManagerServlet.list");
        args[3] = response.encodeURL
            (request.getContextPath() + "/" +
             sm.getString("htmlManagerServlet.helpHtmlManagerFile"));
        args[4] = sm.getString("htmlManagerServlet.helpHtmlManager");
        args[5] = response.encodeURL
            (request.getContextPath() + "/" +
             sm.getString("htmlManagerServlet.helpManagerFile"));
        args[6] = sm.getString("htmlManagerServlet.helpManager");
        args[7] = response.encodeURL
            (request.getContextPath() + "/status");
        args[8] = sm.getString("statusServlet.title");
        writer.print(MessageFormat.format(Constants.MANAGER_SECTION, args));

        // Apps Header Section
        args = new Object[6];
        args[0] = sm.getString("htmlManagerServlet.appsTitle");
        args[1] = sm.getString("htmlManagerServlet.appsPath");
        args[2] = sm.getString("htmlManagerServlet.appsName");
        args[3] = sm.getString("htmlManagerServlet.appsAvailable");
        args[4] = sm.getString("htmlManagerServlet.appsSessions");
        args[5] = sm.getString("htmlManagerServlet.appsTasks");
        writer.print(MessageFormat.format(APPS_HEADER_SECTION, args));

        // Apps Row Section
        // Create sorted map of deployed applications context paths.
        String contextPaths[] = deployer.findDeployedApps();

        TreeMap sortedContextPathsMap = new TreeMap();

        for (int i = 0; i < contextPaths.length; i++) {
            String displayPath = contextPaths[i];
            sortedContextPathsMap.put(displayPath, contextPaths[i]);
        }

        String appsStart = sm.getString("htmlManagerServlet.appsStart");
        String appsStop = sm.getString("htmlManagerServlet.appsStop");
        String appsReload = sm.getString("htmlManagerServlet.appsReload");
        String appsUndeploy = sm.getString("htmlManagerServlet.appsUndeploy");

        Iterator iterator = sortedContextPathsMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String displayPath = (String) entry.getKey();
            String contextPath = (String) entry.getKey();
            Context context = deployer.findDeployedApp(contextPath);
            if (displayPath.equals("")) {
                displayPath = "/";
            }

            if (context != null ) {
                args = new Object[5];
                args[0] = displayPath;
                args[1] = context.getDisplayName();
                if (args[1] == null) {
                    args[1] = "&nbsp;";
                }
                args[2] = new Boolean(context.getAvailable());
                args[3] = response.encodeURL
                    (request.getContextPath() +
                     "/html/sessions?path=" + displayPath);
                if (context.getManager() != null) {
                    args[4] = new Integer
                        (context.getManager().findSessions().length);
                } else {
                    args[4] = new Integer(0);
                }
                writer.print
                    (MessageFormat.format(APPS_ROW_DETAILS_SECTION, args));

                args = new Object[8];
                args[0] = response.encodeURL
                    (request.getContextPath() +
                     "/html/start?path=" + displayPath);
                args[1] = appsStart;
                args[2] = response.encodeURL
                    (request.getContextPath() +
                     "/html/stop?path=" + displayPath);
                args[3] = appsStop;
                args[4] = response.encodeURL
                    (request.getContextPath() +
                     "/html/reload?path=" + displayPath);
                args[5] = appsReload;
                args[6] = response.encodeURL
                    (request.getContextPath() +
                     "/html/undeploy?path=" + displayPath);
                args[7] = appsUndeploy;
                if (context.getPath().equals(this.context.getPath())) {
                    writer.print(MessageFormat.format(
                        MANAGER_APP_ROW_BUTTON_SECTION, args));
                } else if (context.getAvailable()) {
                    writer.print(MessageFormat.format(
                        STARTED_APPS_ROW_BUTTON_SECTION, args));
                } else {
                    writer.print(MessageFormat.format(
                        STOPPED_APPS_ROW_BUTTON_SECTION, args));
                }

            }
        }

        // Deploy Section
        args = new Object[7];
        args[0] = sm.getString("htmlManagerServlet.deployTitle");
        args[1] = sm.getString("htmlManagerServlet.deployServer");
        args[2] = response.encodeURL(request.getContextPath() + "/html/deploy");
        args[3] = sm.getString("htmlManagerServlet.deployPath");
        args[4] = sm.getString("htmlManagerServlet.deployConfig");
        args[5] = sm.getString("htmlManagerServlet.deployWar");
        args[6] = sm.getString("htmlManagerServlet.deployButton");
        writer.print(MessageFormat.format(DEPLOY_SECTION, args));

        args = new Object[4];
        args[0] = sm.getString("htmlManagerServlet.deployUpload");
        args[1] = response.encodeURL(request.getContextPath() + "/html/upload");
        args[2] = sm.getString("htmlManagerServlet.deployUploadFile");
        args[3] = sm.getString("htmlManagerServlet.deployButton");
        writer.print(MessageFormat.format(UPLOAD_SECTION, args));

        // Server Header Section
        args = new Object[7];
        args[0] = sm.getString("htmlManagerServlet.serverTitle");
        args[1] = sm.getString("htmlManagerServlet.serverVersion");
        args[2] = sm.getString("htmlManagerServlet.serverJVMVersion");
        args[3] = sm.getString("htmlManagerServlet.serverJVMVendor");
        args[4] = sm.getString("htmlManagerServlet.serverOSName");
        args[5] = sm.getString("htmlManagerServlet.serverOSVersion");
        args[6] = sm.getString("htmlManagerServlet.serverOSArch");
        writer.print(MessageFormat.format
                     (Constants.SERVER_HEADER_SECTION, args));

        // Server Row Section
        args = new Object[6];
        args[0] = ServerInfo.getServerInfo();
        args[1] = System.getProperty("java.runtime.version");
        args[2] = System.getProperty("java.vm.vendor");
        args[3] = System.getProperty("os.name");
        args[4] = System.getProperty("os.version");
        args[5] = System.getProperty("os.arch");
        writer.print(MessageFormat.format(Constants.SERVER_ROW_SECTION, args));

        // HTML Tail Section
        writer.print(Constants.HTML_TAIL_SECTION);

        // Finish up the response
        writer.flush();
        writer.close();
    }

    /**
     * Reload the web application at the specified context path.
     *
     * @see ManagerServlet#reload(PrintWriter, String)
     *
     * @param path Context path of the application to be restarted
     * @return message String
     */
    protected String reload(String path) {

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        super.reload(printWriter, path);

        return stringWriter.toString();
    }

    /**
     * Undeploy the web application at the specified context path.
     *
     * @see ManagerServlet#undeploy(PrintWriter, String)
     *
     * @param path Context path of the application to be undeployd
     * @return message String
     */
    protected String undeploy(String path) {

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        super.undeploy(printWriter, path);

        return stringWriter.toString();
    }

    /**
     * Display session information and invoke list.
     *
     * @see ManagerServlet#sessions(PrintWriter, String)
     *
     * @param path Context path of the application to list session information
     * @return message String
     */
    public String sessions(String path) {

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        super.sessions(printWriter, path);

        return stringWriter.toString();
    }

    /**
     * Start the web application at the specified context path.
     *
     * @see ManagerServlet#start(PrintWriter, String)
     *
     * @param path Context path of the application to be started
     * @return message String
     */
    public String start(String path) {

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        super.start(printWriter, path);

        return stringWriter.toString();
    }

    /**
     * Stop the web application at the specified context path.
     *
     * @see ManagerServlet#stop(PrintWriter, String)
     *
     * @param path Context path of the application to be stopped
     * @return message String
     */
    protected String stop(String path) {

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        super.stop(printWriter, path);

        return stringWriter.toString();
    }

    // ------------------------------------------------------ Private Constants

    // These HTML sections are broken in relatively small sections, because of
    // limited number of subsitutions MessageFormat can process
    // (maximium of 10).

    private static final String APPS_HEADER_SECTION =
        "<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\">\n" +
        "<tr>\n" +
        " <td colspan=\"5\" class=\"title\">{0}</td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        " <td class=\"header-left\"><small>{1}</small></td>\n" +
        " <td class=\"header-left\"><small>{2}</small></td>\n" +
        " <td class=\"header-center\"><small>{3}</small></td>\n" +
        " <td class=\"header-center\"><small>{4}</small></td>\n" +
        " <td class=\"header-center\"><small>{5}</small></td>\n" +
        "</tr>\n";

    private static final String APPS_ROW_DETAILS_SECTION =
        "<tr>\n" +
        " <td class=\"row-left\"><small><a href=\"{0}\">{0}</a>" +
        "</small></td>\n" +
        " <td class=\"row-left\"><small>{1}</small></td>\n" +
        " <td class=\"row-center\"><small>{2}</small></td>\n" +
        " <td class=\"row-center\">" +
        "<small><a href=\"{3}\">{4}</a></small></td>\n";

    private static final String MANAGER_APP_ROW_BUTTON_SECTION =
        " <td class=\"row-left\">\n" +
        "  <small>\n" +
        "  &nbsp;{1}&nbsp;\n" +
        "  &nbsp;{3}&nbsp;\n" +
        "  &nbsp;{5}&nbsp;\n" +
        "  &nbsp;{7}&nbsp;\n" +
        "  </small>\n" +
        " </td>\n" +
        "</tr>\n";

    private static final String STARTED_APPS_ROW_BUTTON_SECTION =
        " <td class=\"row-left\">\n" +
        "  <small>\n" +
        "  &nbsp;{1}&nbsp;\n" +
        "  &nbsp;<a href=\"{2}\">{3}</a>&nbsp;\n" +
        "  &nbsp;<a href=\"{4}\">{5}</a>&nbsp;\n" +
        "  &nbsp;<a href=\"{6}\">{7}</a>&nbsp;\n" +
        "  </small>\n" +
        " </td>\n" +
        "</tr>\n";

    private static final String STOPPED_APPS_ROW_BUTTON_SECTION =
        " <td class=\"row-left\">\n" +
        "  <small>\n" +
        "  &nbsp;<a href=\"{0}\">{1}</a>&nbsp;\n" +
        "  &nbsp;{3}&nbsp;\n" +
        "  &nbsp;{5}&nbsp;\n" +
        "  &nbsp;<a href=\"{6}\">{7}</a>&nbsp;\n" +
        "  </small>\n" +
        " </td>\n" +
        "</tr>\n";

    private static final String DEPLOY_SECTION =
        "</table>\n" +
        "<br>\n" +
        "<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\">\n" +
        "<tr>\n" +
        " <td colspan=\"2\" class=\"title\">{0}</td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        " <td colspan=\"2\" class=\"header-left\"><small>{1}</small></td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        " <td colspan=\"2\">\n" +
        "<form method=\"get\" action=\"{2}\">\n" +
        "<table cellspacing=\"0\" cellpadding=\"3\">\n" +
        "<tr>\n" +
        " <td class=\"row-right\">\n" +
        "  <small>{3}</small>\n" +
        " </td>\n" +
        " <td class=\"row-left\">\n" +
        "  <input type=\"text\" name=\"deployPath\" size=\"20\">\n" +
        " </td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        " <td class=\"row-right\">\n" +
        "  <small>{4}</small>\n" +
        " </td>\n" +
        " <td class=\"row-left\">\n" +
        "  <input type=\"text\" name=\"deployConfig\" size=\"20\">\n" +
        " </td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        " <td class=\"row-right\">\n" +
        "  <small>{5}</small>\n" +
        " </td>\n" +
        " <td class=\"row-left\">\n" +
        "  <input type=\"text\" name=\"deployWar\" size=\"40\">\n" +
        " </td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        " <td class=\"row-right\">\n" +
        "  &nbsp;\n" +
        " </td>\n" +
        " <td class=\"row-left\">\n" +
        "  <input type=\"submit\" value=\"{6}\">\n" +
        " </td>\n" +
        "</tr>\n" +
        "</table>\n" +
        "</form>\n" +
        "</td>\n" +
        "</tr>\n";

    private static final String UPLOAD_SECTION =
        "<tr>\n" +
        " <td colspan=\"2\" class=\"header-left\"><small>{0}</small></td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        " <td colspan=\"2\">\n" +
        "<form action=\"{1}\" method=\"post\" " +
        "enctype=\"multipart/form-data\">\n" +
        "<table cellspacing=\"0\" cellpadding=\"3\">\n" +
        "<tr>\n" +
        " <td class=\"row-right\">\n" +
        "  <small>{2}</small>\n" +
        " </td>\n" +
        " <td class=\"row-left\">\n" +
        "  <input type=\"file\" name=\"deployWar\" size=\"40\">\n" +
        " </td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        " <td class=\"row-right\">\n" +
        "  &nbsp;\n" +
        " </td>\n" +
        " <td class=\"row-left\">\n" +
        "  <input type=\"submit\" value=\"{3}\">\n" +
        " </td>\n" +
        "</tr>\n" +
        "</table>\n" +
        "</form>\n" +
        "</table>\n" +
        "<br>\n" +
        "\n";

}
