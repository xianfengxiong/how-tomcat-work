/*
 * $Header: /home/cvs/jakarta-tomcat-4.0/webapps/admin/WEB-INF/classes/org/apache/webapp/admin/realm/JNDIRealmForm.java,v 1.10 2002/08/16 00:30:45 amyroh Exp $
 * $Revision: 1.10 $
 * $Date: 2002/08/16 00:30:45 $
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

package org.apache.webapp.admin.realm;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import java.net.InetAddress;
import java.util.List;

import org.apache.webapp.admin.ApplicationServlet;
import org.apache.webapp.admin.LabelValueBean;

/**
 * Form bean for the JNDI realm page.
 *
 * @author Manveen Kaur
 * @version $Revision: 1.10 $ $Date: 2002/08/16 00:30:45 $
 */

public final class JNDIRealmForm extends RealmForm {

    // ----------------------------------------------------- Instance Variables

    /**
     * The text for the connection user name.
     */
    private String connectionName = null;

    /**
     * The text for the connection Password.
     */
    private String connectionPassword = null;

    /**
     * The text for the connection URL.
     */
    private String connectionURL = null;

    /**
     * The text for the context Factory.
     */
    private String contextFactory = null;

    /**
     * The text for the digest algorithm.
     */
    private String digest = null;

    /**
     * The text for the role Base.
     */
    private String roleBase = null;

    /**
     * The text for the role name.
     */
    private String roleName = null;

    /**
     * The text for the role Pattern.
     */
    private String rolePattern = null;

    /**
     * Should we search the entire subtree for matching roles?
     */
    private String roleSubtree = "false";

    /**
     * The text for the user Base.
     */
    private String userBase = null;

    /**
     * The text for the user Password.
     */
    private String userPassword = null;

    /**
     * The text for the user Pattern.
     */
    private String userPattern = null;

    /**
     * The text for the user role name.
     */
    private String userRoleName = null;

    /**
     * The text for the user Search.
     */
    private String userSearch = null;

    /**
     * Should we search the entire subtree for matching users?
     */
    private String userSubtree = "false";

    /**
     * Set of valid values for search subtrees(true/false).
     */
    private List searchVals = null;


    // ------------------------------------------------------------- Properties

    /**
     * Return the search Vals.
     */
    public List getSearchVals() {

        return this.searchVals;

    }

    /**
     * Set the search Vals.
     */
    public void setSearchVals(List searchVals) {

        this.searchVals = searchVals;

    }

    /**
     * Return the roleSubtree boolean Text.
     */
    public String getRoleSubtree() {

        return this.roleSubtree;

    }

    /**
     * Set the roleSubtree Text.
     */
    public void setRoleSubtree(String roleSubtree) {

        this.roleSubtree = roleSubtree;

    }

    /**
     * Return the userSubtree boolean Text.
     */
    public String getUserSubtree() {

        return this.userSubtree;

    }

    /**
     * Set the userSubtree Text.
     */
    public void setUserSubtree(String userSubtree) {

        this.userSubtree = userSubtree;

    }

    /**
     * Return the digest.
     */
    public String getDigest() {

        return this.digest;

    }

    /**
     * Set the digest.
     */
    public void setDigest(String digest) {

        this.digest = digest;

    }

    /**
     * Return the roleBase .
     */
    public String getRoleBase() {

        return this.roleBase ;

    }

    /**
     * Set the roleBase .
     */
    public void setRoleBase(String roleBase ) {

        this.roleBase  = roleBase ;

    }

    /**
     * Return the role name.
     */
    public String getRoleName() {

        return this.roleName ;

    }

    /**
     * Set the role name Attribute .
     */
    public void setRoleName(String roleName) {

        this.roleName  = roleName ;

    }

    /**
     * Return the userBase.
     */
    public String getUserBase() {

        return this.userBase ;

    }

    /**
     * Set the userBase.
     */
    public void setUserBase(String userBase ) {

        this.userBase  = userBase ;

    }

    /**
     * Return the user role name.
     */
    public String getUserRoleName() {

        return this.userRoleName ;

    }

    /**
     * Set the user role name Attribute .
     */
    public void setUserRoleName(String userRoleName) {

        this.userRoleName  = userRoleName ;

    }

    /**
     * Return the role Pattern
     */
    public String getRolePattern() {

        return this.rolePattern ;

    }

    /**
     * Set the role Pattern.
     */
    public void setRolePattern(String rolePattern ) {

        this.rolePattern  = rolePattern ;

    }

    /**
     * Return the user Password .
     */
    public String getUserPassword() {

        return this.userPassword ;

    }

    /**
     * Set the user Password .
     */
    public void setUserPassword(String userPassword ) {

        this.userPassword  = userPassword ;

    }


    /**
     * Return the user Pattern .
     */
    public String getUserPattern() {

        return this.userPattern  ;

    }

    /**
     * Set the user user Pattern  .
     */
    public void setUserPattern(String userPattern) {

        this.userPattern   = userPattern  ;

    }

    /**
     * Return the user Search.
     */
    public String getUserSearch() {

        return this.userSearch;

    }

    /**
     * Set the user user Search.
     */
    public void setUserSearch(String userSearch) {

        this.userSearch  = userSearch;

    }

    /**
     * Return the connection name.
     */
    public String getConnectionName() {

        return this.connectionName;

    }

    /**
     * Set the connectionName.
     */
    public void setConnectionName(String connectionName) {

        this.connectionName = connectionName;

    }


    /**
     * Return the connection password.
     */
    public String getConnectionPassword() {

        return this.connectionPassword;

    }

    /**
     * Set the connection password.
     */
    public void setConnectionPassword(String connectionPassword) {

        this.connectionPassword = connectionPassword;

    }


    /**
     * Return the connection URL.
     */
    public String getConnectionURL() {

        return this.connectionURL;

    }

    /**
     * Set the connectionURL.
     */
    public void setConnectionURL(String connectionURL) {

        this.connectionURL = connectionURL;

    }

    /**
     * Return the context Factory .
     */
    public String getContextFactory() {

        return this.contextFactory ;

    }

    /**
     * Set the context Factory .
     */
    public void setContextFactory(String contextFactory ) {

        this.contextFactory  = contextFactory ;

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
        this.roleSubtree="false";
        this.userSubtree="false";

        this.digest = null;
        this.roleName = null;
        this.userRoleName = null;

        this.connectionName = null;
        this.connectionPassword = null;
        this.connectionURL = null;

        this.rolePattern = null;
        this.roleBase = null;
        this.userBase = null;
        this.userPassword = null;
        this.userPattern = null;
        this.userSearch = null;
        this.contextFactory = null;
    }

    /**
     * Render this object as a String.
     */
    public String toString() {

        StringBuffer sb = new StringBuffer("UserDatabaseRealmForm[adminAction=");
        sb.append(getAdminAction());
        sb.append(",debugLvl=");
        sb.append(getDebugLvl());
        sb.append(",userSubtree=");
        sb.append(userSubtree);
        sb.append(",roleSubtree=");
        sb.append(roleSubtree);
        sb.append(",digest=");
        sb.append(digest);
        sb.append("',userRoleName='");
        sb.append(userRoleName);
        sb.append("',roleName='");
        sb.append(roleName);
        sb.append("',connectionName=");
        sb.append(connectionName);
        sb.append(",connectionPassword=");
        sb.append(connectionPassword);
        sb.append("',connectionURL='");
        sb.append(connectionURL);
        sb.append("',rolePattern=");
        sb.append(rolePattern);
        sb.append(",roleBase=");
        sb.append(roleBase);
        sb.append("',userPassword='");
        sb.append(userPassword);
        sb.append(",userBase=");
        sb.append(userBase);
        sb.append("',userPattern=");
        sb.append(userPattern);
        sb.append("',userSearch=");
        sb.append(userSearch);
        sb.append(",contextFactory=");
        sb.append(contextFactory);
        sb.append("',objectName='");
        sb.append(getObjectName());
        sb.append("',realmType=");
        sb.append(getRealmType());
        sb.append("]");
        return (sb.toString());

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

    public ActionErrors validate(ActionMapping mapping,
    HttpServletRequest request) {

        ActionErrors errors = new ActionErrors();

        String submit = request.getParameter("submit");

        // front end validation when save is clicked.
        if (submit != null) {
            // the following fields are required.

            if ((connectionURL == null) || (connectionURL.length() < 1)) {
                errors.add("connectionURL",
                new ActionError("error.connURL.required"));
            }

            // Either userPattern or userSearch should be specified not both
            boolean isUserPatternSpecified = false;
            boolean isUserSearchSpecified = false;
            if ((userPattern != null) && (userPattern.length() > 0)) {
                isUserPatternSpecified = true;
            }

            if ((userSearch != null) && (userSearch.length() > 0)) {
                isUserSearchSpecified = true;
            }

            if (isUserPatternSpecified && isUserSearchSpecified) {
                errors.add("userPattern" ,
                new ActionError("error.userPattern.userSearch.defined"));
            }

            /*if ((digest == null) || (digest.length() < 1)) {
                errors.add("digest",
                new ActionError("error.digest.required"));
            } */

            /*if ((roleName == null) || (roleName.length() < 1)) {
                errors.add("roleName",
                new ActionError("error.roleName.required"));
            }

            if ((userRoleName == null) || (userRoleName.length() < 1)) {
                errors.add("userRoleName",
                new ActionError("error.userRoleName.required"));
            }

            if ((rolePattern == null) || (rolePattern.length() < 1)) {
                errors.add("rolePattern",
                new ActionError("error.rolePattern.required"));
            }

            if ((roleBase == null) || (roleBase.length() < 1)) {
                errors.add("roleBase",
                new ActionError("error.roleBase.required"));
            }

            if ((userBase == null) || (userBase.length() < 1)) {
                errors.add("userBase",
                new ActionError("error.userBase.required"));
            }

            if ((userPassword == null) || (userPassword.length() < 1)) {
                errors.add("userPassword",
                new ActionError("error.userPassword.required"));
            }

            if ((userPattern == null) || (userPattern.length() < 1)) {
                errors.add("userPattern",
                new ActionError("error.userPattern.required"));
            }

            if ((userSearch == null) || (userSearch.length() < 1)) {
                errors.add("userSearch",
                new ActionError("error.userSearch.required"));
            }

            if ((connectionName == null) || (connectionName.length() < 1)) {
                errors.add("connectionName",
                new ActionError("error.connName.required"));
            }

            if ((connectionPassword == null) || (connectionPassword.length() < 1)) {
                errors.add("connectionPassword",
                new ActionError("error.connPassword.required"));
            }

            if ((contextFactory == null) || (contextFactory.length() < 1)) {
                errors.add("contextFactory",
                new ActionError("error.contextFactory.required"));
            } */
        }

        return errors;
    }
}
