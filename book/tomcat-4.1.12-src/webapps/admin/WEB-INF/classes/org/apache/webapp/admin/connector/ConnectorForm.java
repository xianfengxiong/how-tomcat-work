/*
 * $Header: /home/cvs/jakarta-tomcat-4.0/webapps/admin/WEB-INF/classes/org/apache/webapp/admin/connector/ConnectorForm.java,v 1.11 2002/05/01 02:37:22 manveen Exp $
 * $Revision: 1.11 $
 * $Date: 2002/05/01 02:37:22 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Struts", and "Apache Software
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

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import java.net.InetAddress;
import java.util.List;

/**
 * Form bean for the connector page.
 *
 * @author Manveen Kaur
 * @version $Revision: 1.11 $ $Date: 2002/05/01 02:37:22 $
 */

public final class ConnectorForm extends ActionForm {
    
    // ----------------------------------------------------- Instance Variables
    
     /**
     * The administrative action represented by this form.
     */
    private String adminAction = "Edit";

    /**
     * The object name of the Connector this bean refers to.
     */
    private String objectName = null;
    
    /**
     * The name of the service this connector belongs to.
     */
    private String serviceName = null;
   
    /**
     * The text for the scheme.
     */
    private String scheme = null;

    /**
     * The text for the connector type. 
     * Specifies if it is a CoyoteConnector or AJP13Connector etc.
     */
    private String connectorType = null;    
    
     /**
     * The text for the node label.
     */
    private String nodeLabel = null;
    
    
    /**
     * The text for the accept Count.
     */
    private String acceptCountText = null;
    
    /**
     * The text for the Connection Time Out.
     */
    private String connTimeOutText = null;
    
    
    /**
     * The text for the debug level.
     */
    private String debugLvl = "0";
    
    /**
     * The text for the buffer size.
     */
    private String bufferSizeText = null;
    
    /**
     * The value of enable Lookups.
     */
    private String enableLookups = "false";
    
    /**
     * The text for the address.
     */
    private String address = null;
    
    /**
     * The text for the minProcessors.
     */
    private String minProcessorsText = null;
    
    /**
     * The text for the max Processors.
     */
    private String maxProcessorsText = null;
    
    
    /**
     * The text for the port.
     */
    private String portText = null;
    
    /**
     * The text for the redirect port.
     */
    private String redirectPortText = null;
    
    /**
     * The text for the proxyName.
     */
    private String proxyName = null;
    
    /**
     * The text for the proxy Port Number.
     */
    private String proxyPortText = null;
    
    
    /**
     * The text for the connectorName.
     */
    private String connectorName = null;
        
    /**
     * Whether client authentication is supported.
     */
    private String clientAuthentication = "false";
        
    /**
     * The keyStore Filename.
     */
    private String keyStoreFileName = null;
        
    /**
     * The keyStore Password.
     */
    private String keyStorePassword = null;

    /**
     * Set of valid values for debug level.
     */
    private List debugLvlVals = null;
    
    /*
     * Represent boolean (true, false) values for enableLookups etc.
     */    
    private List booleanVals = null;

    /*
     * Represent supported connector types.
     */    
    private List connectorTypeVals = null;

    // ------------------------------------------------------------- Properties
    
   /**
     * Return the administrative action represented by this form.
     */
    public String getAdminAction() {

        return this.adminAction;

    }


    /**
     * Set the administrative action represented by this form.
     */
    public void setAdminAction(String adminAction) {

        this.adminAction = adminAction;

    }

    /**
     * Return the object name of the Connector this bean refers to.
     */
    public String getObjectName() {

        return this.objectName;

    }


    /**
     * Set the object name of the Connector this bean refers to.
     */
    public void setObjectName(String objectName) {

        this.objectName = objectName;

    }
    
      /**
     * Return the object name of the service this connector belongs to.
     */
    public String getServiceName() {

        return this.serviceName;

    }


    /**
     * Set the object name of the Service this connector belongs to.
     */
    public void setServiceName(String serviceName) {

        this.serviceName = serviceName;

    }
    
    /**
     * Return the Scheme.
     */
    public String getScheme() {
        
        return this.scheme;
        
    }
    
    /**
     * Set the Scheme.
     */
    public void setScheme(String scheme) {
        
        this.scheme = scheme;
        
    }
    
    /**
     * Return the Connector type.
     */
    public String getConnectorType() {
        
        return this.connectorType;
        
    }
    
    /**
     * Set the Connector type.
     */
    public void setConnectorType(String connectorType) {
        
        this.connectorType = connectorType;
        
    }
    
    /**
     * Return the label of the node that was clicked.
     */
    public String getNodeLabel() {
        
        return this.nodeLabel;
        
    }
    
    /**
     * Set the node label.
     */
    public void setNodeLabel(String nodeLabel) {
        
        this.nodeLabel = nodeLabel;
        
    }
    
    /**
     * Return the acceptCountText.
     */
    public String getAcceptCountText() {
        
        return this.acceptCountText;
        
    }
    
    
    /**
     * Set the acceptCountText.
     */
    
    public void setAcceptCountText(String acceptCountText) {
        
        this.acceptCountText = acceptCountText;
        
    }
    
    /**
     * Return the connTimeOutText.
     */
    public String getConnTimeOutText() {
        
        return this.connTimeOutText;
        
    }
    
    /**
     * Set the connTimeOutText.
     */
    
    public void setConnTimeOutText(String connTimeOutText) {
        
        this.connTimeOutText = connTimeOutText;
        
    }
       
    /**
     * Return the bufferSizeText.
     */
    public String getBufferSizeText() {
        
        return this.bufferSizeText;
        
    }
    
    /**
     * Set the bufferSizeText.
     */
    
    public void setBufferSizeText(String bufferSizeText) {
        
        this.bufferSizeText = bufferSizeText;
        
    }
    
    /**
     * Return the address.
     */
    public String getAddress() {
        
        return this.address;
        
    }
    
    /**
     * Set the connTimeOutText.
     */
    
    public void setAddress(String address) {
        
        this.address = address;
        
    }
    
    
    /**
     * Return the proxy Name.
     */
    public String getProxyName() {
        
        return this.proxyName;
        
    }
    
    /**
     * Set the proxy Name.
     */
    
    public void setProxyName(String proxyName) {
        
        this.proxyName = proxyName;
        
    }
    
    /**
     * Return the proxy Port NumberText.
     */
    public String getProxyPortText() {
        
        return this.proxyPortText;
        
    }
    
    /**
     * Set the proxy Port NumberText.
     */
    
    public void setProxyPortText(String proxyPortText) {
        
        this.proxyPortText = proxyPortText;
        
    }

   /**
     * Return the true/false value of client authentication.
     */
    public String getClientAuthentication() {

        return this.clientAuthentication;

    }


    /**
     * Set whether client authentication is supported or not.
     */
    public void setClientAuthentication(String clientAuthentication) {

        this.clientAuthentication = clientAuthentication;

    }

    /**
     * Return the object name of the service this connector belongs to.
     */
    public String getKeyStoreFileName() {

        return this.keyStoreFileName;

    }


    /**
     * Set the object name of the Service this connector belongs to.
     */
    public void setKeyStoreFileName(String keyStoreFileName) {

        this.keyStoreFileName = keyStoreFileName;

    }

          /**
     * Return the object name of the service this connector belongs to.
     */
    public String getKeyStorePassword() {

        return this.keyStorePassword;

    }


    /**
     * Set the object name of the Service this connector belongs to.
     */
    public void setKeyStorePassword(String keyStorePassword) {

        this.keyStorePassword = keyStorePassword;

    }

    /**
     * Return the debugVals.
     */
    public List getDebugLvlVals() {
        
        return this.debugLvlVals;
        
    }
    
    /**
     * Set the debugVals.
     */
    public void setDebugLvlVals(List debugLvlVals) {
        
        this.debugLvlVals = debugLvlVals;
        
    }
    
    
    /**
     * Return the Debug Level Text.
     */
    
    public String getDebugLvl() {
        
        return this.debugLvl;
        
    }
    
    /**
     * Set the Debug Level Text.
     */
    public void setDebugLvl(String debugLvl) {
        
        this.debugLvl = debugLvl;
        
    }
    
    
    /**
     * Return the Enable lookup Text.
     */
    
    public String getEnableLookups() {
        
        return this.enableLookups;
        
    }
    
    /**
     * Set the Enable Lookup Text.
     */
    public void setEnableLookups(String enableLookups) {
        
        this.enableLookups = enableLookups;
        
    }
    
    /**
     * Return the booleanVals.
     */
    public List getBooleanVals() {
        
        return this.booleanVals;
        
    }
    
    /**
     * Set the debugVals.
     */
    public void setBooleanVals(List booleanVals) {
        
        this.booleanVals = booleanVals;
        
    }
    
    /**
     * Return the min Processors Text.
     */
    public String getMinProcessorsText() {
        
        return this.minProcessorsText;
        
    }
    
    /**
     * Set the minProcessors Text.
     */
    public void setMinProcessorsText(String minProcessorsText) {
        
        this.minProcessorsText = minProcessorsText;
        
    }
    
    /**
     * Return the max processors Text.
     */
    public String getMaxProcessorsText() {
        
        return this.maxProcessorsText;
        
    }
    
    /**
     * Set the Max Processors Text.
     */
    public void setMaxProcessorsText(String maxProcessorsText) {
        
        this.maxProcessorsText = maxProcessorsText;
        
    }
    
    /**
     * Return the port text.
     */
    public String getPortText() {
        
        return this.portText;
        
    }
    
    /**
     * Set the port Text.
     */
    public void setPortText(String portText) {
        
        this.portText = portText;
        
    }
    
    
    /**
     * Return the port.
     */
    public String getRedirectPortText() {
        
        return this.redirectPortText;
        
    }
    
    /**
     * Set the Redirect Port Text.
     */
    public void setRedirectPortText(String redirectPortText) {
        
        this.redirectPortText = redirectPortText;
        
    }
    
    /**
     * Return the Service Name.
     */
    public String getConnectorName() {
        
        return this.connectorName;
        
    }
    
    /**
     * Set the Service Name.
     */
    public void setConnectorName(String connectorName) {
        
        this.connectorName = connectorName;
        
    }
    
    /**
     * Return the connectorTypeVals.
     */
    public List getConnectorTypeVals() {
        
        return this.connectorTypeVals;
        
    }
    
    /**
     * Set the connectorTypeVals.
     */
    public void setConnectorTypeVals(List connectorTypeVals) {
        
        this.connectorTypeVals = connectorTypeVals;
        
    }
    
    // --------------------------------------------------------- Public Methods
    
    /**
     * Reset all properties to their default values.
     *
     * @param mapping The mapping used to select this instance
     * @param request The servlet request we are processing
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
    
        this.objectName = null;
        this.connectorType = null;
        this.portText = null;
        this.acceptCountText = null;
        this.connTimeOutText = null;
        this.bufferSizeText = null;
        this.address = null;
        this.enableLookups = "false";
        this.minProcessorsText = null;
        this.maxProcessorsText = null;
        this.portText = null;
        this.redirectPortText = null;
        this.proxyName = null;
        this.proxyPortText = null;
        this.keyStoreFileName = null;
        this.keyStorePassword = null;        
        this.clientAuthentication = "false";
        
    }
    
    /**
     * Validate the properties that have been set from this HTTP request,
     * and return an <code>ActionErrors</code> object that encapsulates any
     * validation errors that have been found.  If no errors are found, return
     * <code>null</code> or an <code>ActionErrors</code> object with no
     * recorded error messages.
     *
     * @param mapping The mapping used to select this instance
     * @param request The servlet request we are processing
     */
    
    private ActionErrors errors;
    
    public ActionErrors validate(ActionMapping mapping,
    HttpServletRequest request) {
        
        errors = new ActionErrors();
        
        String submit = request.getParameter("submit");
        
        // front end validation when save is clicked.
        if (submit != null) {
            
            /* general */
            numberCheck("acceptCountText", acceptCountText, true, 0, 128);
            numberCheck("connTimeOutText", connTimeOutText, true, -1, 60000);
            numberCheck("bufferSizeText", bufferSizeText, true, 1, 8192);
  
            /* The IP address can also be null -- which means open the
             server socket on *all* IP addresses for this host */
            if ((address.length() > 0) && !address.equalsIgnoreCase(" ")) {
                try {
                    InetAddress.getByName(address);
                } catch (Exception e) {
                    errors.add("address", new ActionError("error.address.invalid"));
                }
            } else {
                address = " ";
            }
            
            /* ports */
            numberCheck("portNumber",  portText, true, 1, 65535);
            numberCheck("redirectPortText",  redirectPortText, true, -1, 65535);
            
            /* processors*/
            numberCheck("minProcessorsText",  minProcessorsText, true, 1, 512);
            try {
                // if min is a valid integer, then check that max >= min
                int min = Integer.parseInt(minProcessorsText);
                numberCheck("maxProcessorsText",  maxProcessorsText, true, min, 512);
            } catch (Exception e) {
                // check for the complete range
                numberCheck("maxProcessorsText",  maxProcessorsText, true, 1, 512);
            }
            
            // proxy                  
            if ((proxyName!= null) && (proxyName.length() > 0)) {
                try {
                    InetAddress.getByName(proxyName);
                } catch (Exception e) {
                    errors.add("proxyName", new ActionError("error.proxyName.invalid"));
                }
            }   
            
            // supported only by Coyote HTTP and HTTPS connectors
            if (!("AJP".equalsIgnoreCase(connectorType)))
                numberCheck("proxyPortText",  proxyPortText, true, 0, 65535);            
        }
        
        return errors;
    }
    
    /*
     * Helper method to check that it is a required number and
     * is a valid integer within the given range. (min, max).
     *
     * @param  field  The field name in the form for which this error occured.
     * @param  numText  The string representation of the number.
     * @param rangeCheck  Boolean value set to true of reange check should be performed.
     *
     * @param  min  The lower limit of the range
     * @param  max  The upper limit of the range
     *
     */
    
    public void numberCheck(String field, String numText, boolean rangeCheck,
    int min, int max) {
        
        /* Check for 'is required' */
        if ((numText == null) || (numText.length() < 1)) {
            errors.add(field, new ActionError("error."+field+".required"));
        } else {
            
        /*check for 'must be a number' in the 'valid range'*/
            try {
                int num = Integer.parseInt(numText);
                // perform range check only if required
                if (rangeCheck) {
                    if ((num < min) || (num > max ))
                        errors.add( field,
                        new ActionError("error."+ field +".range"));
                }
            } catch (NumberFormatException e) {
                errors.add(field,
                new ActionError("error."+ field + ".format"));
            }
        }
    }
    
}
