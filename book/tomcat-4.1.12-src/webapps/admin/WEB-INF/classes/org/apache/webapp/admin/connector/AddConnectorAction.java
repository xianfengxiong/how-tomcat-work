/*
 * $Header: /home/cvs/jakarta-tomcat-4.0/webapps/admin/WEB-INF/classes/org/apache/webapp/admin/connector/AddConnectorAction.java,v 1.6 2002/04/30 18:10:47 manveen Exp $
 * $Revision: 1.6 $
 * $Date: 2002/04/30 18:10:47 $
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

package org.apache.webapp.admin.connector;

import java.io.IOException;
import java.net.URLEncoder;
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
import org.apache.webapp.admin.LabelValueBean;
import org.apache.webapp.admin.Lists;

/**
 * The <code>Action</code> that sets up <em>Add Connector</em> transactions.
 *
 * @author Manveen Kaur
 * @version $Revision: 1.6 $ $Date: 2002/04/30 18:10:47 $
 */

public class AddConnectorAction extends Action {
    
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
        
        // the service Name is needed to retrieve the engine mBean to
        // which the new connector mBean will be added.
        String serviceName = request.getParameter("serviceName");
        
        // Fill in the form values for display and editing
        ConnectorForm connectorFm = new ConnectorForm();
        session.setAttribute("connectorForm", connectorFm);
        connectorFm.setAdminAction("Create");
        connectorFm.setObjectName("");
        connectorFm.setConnectorName("");
        String type = request.getParameter("type");
        if (type == null)
            type = "HTTP";    // default type is HTTP
        connectorFm.setConnectorType(type);
        connectorFm.setDebugLvl("0");
        connectorFm.setServiceName(serviceName);
        if ("HTTPS".equalsIgnoreCase(type)) {
            connectorFm.setScheme("https");
        } else {
            connectorFm.setScheme("http");       
        }
        connectorFm.setAcceptCountText("10");
        connectorFm.setConnTimeOutText("60000");
        connectorFm.setBufferSizeText("2048");
        connectorFm.setEnableLookups("true");
        connectorFm.setAddress("");
        connectorFm.setPortText("");
        connectorFm.setRedirectPortText("-1");
        connectorFm.setMinProcessorsText("5");
        connectorFm.setMaxProcessorsText("20");

        //supported only by HTTPS
        connectorFm.setClientAuthentication("false");
        connectorFm.setKeyStoreFileName("");
        connectorFm.setKeyStorePassword("");
                       
        // supported only by Coyote connectors
        connectorFm.setProxyName("");
        connectorFm.setProxyPortText("0");        
        
        connectorFm.setDebugLvlVals(Lists.getDebugLevels());
        connectorFm.setBooleanVals(Lists.getBooleanValues());                
        
        String schemeTypes[]= new String[3];
        schemeTypes[0] = "HTTP";
        schemeTypes[1] = "HTTPS";                
        schemeTypes[2] = "AJP";
        
        ArrayList types = new ArrayList();    
        // the first element in the select list should be the type selected
        types.add(new LabelValueBean(type,
                "/admin/AddConnector.do?serviceName=" + URLEncoder.encode(serviceName) 
                + "&type=" + type));        
         for (int i=0; i< schemeTypes.length; i++) {
            if (!type.equalsIgnoreCase(schemeTypes[i])) {
                types.add(new LabelValueBean(schemeTypes[i],
                "/admin/AddConnector.do?serviceName=" + URLEncoder.encode(serviceName)
                + "&type=" + schemeTypes[i]));        
            }
        }
        connectorFm.setConnectorTypeVals(types);
        
        // Forward to the connector display page
        return (mapping.findForward("Connector"));
        
    }        
}
