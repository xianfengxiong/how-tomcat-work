/*
 * $Header: /home/cvs/jakarta-tomcat-4.0/webapps/admin/WEB-INF/classes/org/apache/webapp/admin/resources/DataSourceForm.java,v 1.5 2002/06/20 22:53:16 amyroh Exp $
 * $Revision: 1.5 $
 * $Date: 2002/06/20 22:53:16 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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

package org.apache.webapp.admin.resources;

import java.util.List;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.webapp.admin.LabelValueBean;

/**
 * Form bean for the individual data source page.
 *
 * @author Manveen Kaur
 * @version $Revision: 1.5 $ $Date: 2002/06/20 22:53:16 $
 * @since 4.1
 */

public final class DataSourceForm extends BaseForm {


    // ----------------------------------------------------- Instance Variables


    // ------------------------------------------------------------- Properties


    /**
     * The url of the data source.
     */
    private String url = null;

    public String getUrl() {
        return (this.url);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * The JNDI name of the data source.
     */
    private String jndiName = null;

    public String getJndiName() {
        return (this.jndiName);
    }

    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }
    
    /**
     * The JDBC driver class of the data source.
     */
    private String driverClass = null;

    public String getDriverClass() {
        return (this.driverClass);
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    
    /**
     * The username of the databse corresponding to the data source.
     */
    private String username = null;

    public String getUsername() {
        return (this.username);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    
    /**
     * The password of the database corresponding to the data source.
     */
    private String password = null;

    public String getPassword() {
        return (this.password);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    
    /**
     * The max number of active sessions to the data source.
     */
    private String active = null;

    public String getActive() {
        return (this.active);
    }

    public void setActive(String active) {
        this.active = active;
    }

    /**
     * The max number of idle connections to the data source.
     */
    private String idle = null;

    public String getIdle() {
        return (this.idle);
    }

    public void setIdle(String idle) {
        this.idle = idle;
    }

    /**
     * The maximum wait for a connection to the data source.
     */
    private String wait = null;

    public String getWait() {
        return (this.wait);
    }

    public void setWait(String wait) {
        this.wait = wait;
    }

    /**
     * The resource type of this data source.
     */
    private String resourcetype = null;
    
    /**
     * Return the resource type of the data source this bean refers to.
     */
    public String getResourcetype() {
        return this.resourcetype;
    }

    /**
     * Set the resource type of the data source this bean refers to.
     */
    public void setResourcetype(String resourcetype) {
        this.resourcetype = resourcetype;
    }
       
    /**
     * The path of this data source.
     */
    private String path = null;
    
    /**
     * Return the path of the data source this bean refers to.
     */
    public String getPath() {
        return this.path;
    }

    /**
     * Set the path of the data source this bean refers to.
     */
    public void setPath(String path) {
        this.path = path;
    }
       
    /**
     * The host of this data source.
     */
    private String host = null;
    
    /**
     * Return the host of the data source this bean refers to.
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Set the host of the data source this bean refers to.
     */
    public void setHost(String host) {
        this.host = host;
    }    
    
       
    /**
     * The service of this data source.
     */
    private String service = null;
    
    /**
     * Return the service of the data source this bean refers to.
     */
    public String getService() {
        return this.service;
    }

    /**
     * Set the service of the data source this bean refers to.
     */
    public void setService(String service) {
        this.service = service;
    }
    
    /**
     * The validation query to the data source.
     */
    private String query = null;

    public String getQuery() {
        return (this.query);
    }

    public void setQuery(String query) {
        this.query = query;
    }
    
    /**
     * The type of the resource.
     */
    private String type = null;

    public String getType() {
        return (this.type);
    }

    public void setType(String type) {
        this.type = type;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Reset all properties to their default values.
     *
     * @param mapping The mapping used to select this instance
     * @param request The servlet request we are processing
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {

        super.reset(mapping, request);
        url = null;        
        jndiName = null;
        driverClass = null;
        username = null;
        password = null;
        type = null;
    
        active = null;
        idle = null;
        wait = null;
        query = null;
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
    
    private ActionErrors errors = null;
    
    public ActionErrors validate(ActionMapping mapping,
    HttpServletRequest request) {

        errors = new ActionErrors();

        String submit = request.getParameter("submit");

        if (submit != null) {

            // url is a required field
            if ((url == null) || (url.length() < 1)) {
                errors.add("url",
                           new ActionError("resources.error.url.required"));
            }

            // jndiName is a required field
            if (( jndiName == null) || (jndiName.length() < 1)) {
                errors.add("jndiName",
                           new ActionError("resources.error.jndiName.required"));
            }

            // driverClass is a required field
            if ((driverClass == null) || (driverClass.length() < 1)) {
                errors.add("driverClass",
                           new ActionError("resources.error.driverClass.required"));
            }
            
            // username is a required field
            if ((username == null) || (username.length() < 1)) {
                errors.add("username",
                           new ActionError("users.error.username.required"));
            }
            
            // commented out password can be an empty string
            // password is a required field
            //if ((password == null) || (password.length() < 1)) {
            //    errors.add("password",
            //               new ActionError("error.userPassword.required"));
            //
            
            // FIX ME -- need to do a range check
            numberCheck("active", active , false, 0, 10000);
            numberCheck("idle", idle , false, 0, 10000);
            numberCheck("wait", wait , false, 0, 10000);

            // Quotes not allowed in username
            if ((username != null) && (username.indexOf('"') >= 0)) {
                errors.add("username",
                           new ActionError("users.error.quotes"));
            }

            // Quotes not allowed in password
            if ((password != null) && (password.indexOf('"') > 0)) {
                errors.add("password",
                           new ActionError("users.error.quotes"));
            }
         }
        return (errors);
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
    
    private void numberCheck(String field, String numText, boolean rangeCheck,
                             int min, int max) {
        
        // Check for 'is required'
        if ((numText == null) || (numText.length() < 1)) {
            errors.add(field, new ActionError("resources.error."+field+".required"));
        } else {
            
            // check for 'must be a number' in the 'valid range'
            try {
                int num = Integer.parseInt(numText);
                // perform range check only if required
                if (rangeCheck) {
                    if ((num < min) || (num > max ))
                        errors.add( field,
                        new ActionError("resources.error."+ field +".range"));
                }
            } catch (NumberFormatException e) {
                errors.add(field,
                new ActionError("resources.integer.error"));
            }
        }
    }
    
}
