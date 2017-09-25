/*
 * $Header: /home/cvs/jakarta-tomcat-4.0/webapps/admin/WEB-INF/classes/org/apache/webapp/admin/defaultcontext/EditDefaultContextAction.java,v 1.2 2002/09/19 22:56:05 amyroh Exp $
 * $Revision: 1.2 $
 * $Date: 2002/09/19 22:56:05 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
 */

package org.apache.webapp.admin.defaultcontext;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.JMException;

import org.apache.webapp.admin.ApplicationServlet;
import org.apache.webapp.admin.LabelValueBean;
import org.apache.webapp.admin.Lists;
import org.apache.webapp.admin.TomcatTreeBuilder;

/**
 * The <code>Action</code> that sets up <em>Edit DefaultContext</em> transactions.
 *
 * @author Amy Roh
 * @version $Revision: 1.2 $ $Date: 2002/09/19 22:56:05 $
 */

public class EditDefaultContextAction extends Action {
    

    /**
     * The MBeanServer we will be interacting with.
     */
    private MBeanServer mBServer = null;
    

    /**
     * The MessageResources we will be retrieving messages from.
     */
    private MessageResources resources = null;
    

    // --------------------------------------------------------- Public Methods
    
    /**
     * Process the specified HTTP request, and create the corresponding HTTP
     * response (or forward to another web component that will create it).
     * Return an <code>ActionForward</code> instance describing where and how
     * control should be forwarded, or <code>null</code> if the response has
     * already been completed.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param actionForm The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet exception occurs
     */
    public ActionForward perform(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws IOException, ServletException {
        
        // Acquire the resources that we need
        HttpSession session = request.getSession();
        Locale locale = (Locale) session.getAttribute(Action.LOCALE_KEY);
        if (resources == null) {
            resources = getServlet().getResources();
        }
        
        // Acquire a reference to the MBeanServer containing our MBeans
        try {
            mBServer = ((ApplicationServlet) getServlet()).getServer();
        } catch (Throwable t) {
            throw new ServletException
            ("Cannot acquire MBeanServer reference", t);
        }
        
        // Set up the object names of the MBeans we are manipulating
        // DefaultContext mBean
        ObjectName cname = null;
        // Loader mBean
        ObjectName lname = null;
        // Manager mBean 
        ObjectName mname = null;
        
        StringBuffer sb = null;
        try {
            cname = new ObjectName(request.getParameter("select"));
        } catch (Exception e) {
            String message =
                resources.getMessage("error.contextName.bad",
                                     request.getParameter("select"));
            getServlet().log(message);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
            return (null);
        }
        
        // Get the corresponding loader
        try {
            sb = new StringBuffer(cname.getDomain());
            sb.append(":type=DefaultLoader");
            String host = cname.getKeyProperty("host");
            if (host != null) {
                sb.append(",host=");
                sb.append(host);
            }
            sb.append(",service=");
            sb.append(cname.getKeyProperty("service"));
            lname = new ObjectName(sb.toString());
        } catch (Exception e) {
            String message =
                resources.getMessage("error.managerName.bad",
                                 sb.toString());
            getServlet().log(message);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
            return (null);
        }

        // Session manager properties
        // Get the corresponding Session Manager mBean
        try {
            sb = new StringBuffer(cname.getDomain());
            sb.append(":type=DefaultManager");
            String host = cname.getKeyProperty("host");
            if (host != null) {
                sb.append(",host=");
                sb.append(host);
            }
            sb.append(",service=");
            sb.append(cname.getKeyProperty("service"));
            mname = new ObjectName(sb.toString());
        } catch (Exception e) {
            String message =
                resources.getMessage("error.managerName.bad",
                                 sb.toString());
            getServlet().log(message);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
            return (null);
        }

        // Fill in the form values for display and editing
        DefaultContextForm defaultContextFm = new DefaultContextForm();
        session.setAttribute("defaultContextForm", defaultContextFm);
        defaultContextFm.setAdminAction("Edit");
        defaultContextFm.setObjectName(cname.toString());
        defaultContextFm.setLoaderObjectName(lname.toString());
        defaultContextFm.setManagerObjectName(mname.toString());
        sb = new StringBuffer("DefaultContext");
        defaultContextFm.setNodeLabel(sb.toString());
        defaultContextFm.setDebugLvlVals(Lists.getDebugLevels());
        defaultContextFm.setBooleanVals(Lists.getBooleanValues());
       
        String attribute = null;
        try {

            // Copy scalar properties
            attribute = "cookies";
            defaultContextFm.setCookies
                (((Boolean) mBServer.getAttribute(cname, attribute)).toString());
            attribute = "crossContext";
            defaultContextFm.setCrossContext
                (((Boolean) mBServer.getAttribute(cname, attribute)).toString());
            attribute = "useNaming";
            defaultContextFm.setUseNaming
                (((Boolean) mBServer.getAttribute(cname, attribute)).toString());
            attribute = "reloadable";
            defaultContextFm.setReloadable
                (((Boolean) mBServer.getAttribute(cname, attribute)).toString());
            attribute = "swallowOutput";
            defaultContextFm.setSwallowOutput
                (((Boolean) mBServer.getAttribute(cname, attribute)).toString());

            // loader properties
            attribute = "debug";
            defaultContextFm.setLdrDebugLvl
                (((Integer) mBServer.getAttribute(lname, attribute)).toString());
            attribute = "checkInterval";
            defaultContextFm.setLdrCheckInterval
                (((Integer) mBServer.getAttribute(lname, attribute)).toString());
            attribute = "reloadable";
            defaultContextFm.setLdrReloadable
                (((Boolean) mBServer.getAttribute(lname, attribute)).toString());

            // manager properties
            attribute = "debug";
            defaultContextFm.setMgrDebugLvl
                (((Integer) mBServer.getAttribute(mname, attribute)).toString());
            attribute = "entropy";
            defaultContextFm.setMgrSessionIDInit
                ((String) mBServer.getAttribute(mname, attribute));
            attribute = "maxActiveSessions";
            defaultContextFm.setMgrMaxSessions
                (((Integer) mBServer.getAttribute(mname, attribute)).toString());
            attribute = "checkInterval";
            defaultContextFm.setMgrCheckInterval
                (((Integer) mBServer.getAttribute(mname, attribute)).toString());

        } catch (Throwable t) {
            getServlet().log
                (resources.getMessage(locale, "users.error.attribute.get",
                                      attribute), t);
            response.sendError
                (HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                 resources.getMessage(locale, "users.error.attribute.get",
                                      attribute));
            return (null);
        }
        
        // Forward to the default context display page
        return (mapping.findForward("DefaultContext"));
        
    }


}
