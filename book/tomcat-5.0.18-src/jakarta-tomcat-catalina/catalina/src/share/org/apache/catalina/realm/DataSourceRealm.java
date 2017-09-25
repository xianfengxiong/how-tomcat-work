/*
* $Header: /home/cvs/jakarta-tomcat-catalina/catalina/src/share/org/apache/catalina/realm/DataSourceRealm.java,v 1.3 2003/09/02 21:22:05 remm Exp $
* $Revision: 1.3 $
* $Date: 2003/09/02 21:22:05 $
*
* ====================================================================
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


package org.apache.catalina.realm;


import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.naming.Context;
import javax.sql.DataSource;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.util.StringManager;

/**
*
* Implmentation of <b>Realm</b> that works with any JDBC JNDI DataSource.
* See the JDBCRealm.howto for more details on how to set up the database and
* for configuration options.
*
* @author Glenn L. Nielsen
* @author Craig R. McClanahan
* @author Carson McDonald
* @author Ignacio Ortega
* @version $Revision: 1.3 $
*/

public class DataSourceRealm
    extends RealmBase {


    // ----------------------------------------------------- Instance Variables


    /**
     * The generated string for the roles PreparedStatement
     */
    private StringBuffer preparedRoles = null;


    /**
     * The generated string for the credentials PreparedStatement
     */
    private StringBuffer preparedCredentials = null;


    /**
     * The name of the JNDI JDBC DataSource
     */
    protected String dataSourceName = null;


    /**
     * Descriptive information about this Realm implementation.
     */
    protected static final String info =
        "org.apache.catalina.realm.DataSourceRealm/1.0";


    /**
     * Descriptive information about this Realm implementation.
     */
    protected static final String name = "DataSourceRealm";


    /**
     * The column in the user role table that names a role
     */
    protected String roleNameCol = null;


    /**
     * The string manager for this package.
     */
    protected static final StringManager sm =
        StringManager.getManager(Constants.Package);


    /**
     * The column in the user table that holds the user's credintials
     */
    protected String userCredCol = null;


    /**
     * The column in the user table that holds the user's name
     */
    protected String userNameCol = null;


    /**
     * The table that holds the relation between user's and roles
     */
    protected String userRoleTable = null;


    /**
     * The table that holds user data.
     */
    protected String userTable = null;


    // ------------------------------------------------------------- Properties


    /**
     * Return the name of the JNDI JDBC DataSource.
     *
     */
    public String getDataSourceName() {
        return dataSourceName;
    }

    /**
     * Set the name of the JNDI JDBC DataSource.
     *
     * @param dataSourceName the name of the JNDI JDBC DataSource
     */
    public void setDataSourceName( String dataSourceName) {
      this.dataSourceName = dataSourceName;
    }

    /**
     * Return the column in the user role table that names a role.
     *
     */
    public String getRoleNameCol() {
        return roleNameCol;
    }

    /**
     * Set the column in the user role table that names a role.
     *
     * @param roleNameCol The column name
     */
    public void setRoleNameCol( String roleNameCol ) {
        this.roleNameCol = roleNameCol;
    }

    /**
     * Return the column in the user table that holds the user's credentials.
     *
     */
    public String getUserCredCol() {
        return userCredCol;
    }

    /**
     * Set the column in the user table that holds the user's credentials.
     *
     * @param userCredCol The column name
     */
    public void setUserCredCol( String userCredCol ) {
       this.userCredCol = userCredCol;
    }

    /**
     * Return the column in the user table that holds the user's name.
     *
     */
    public String getUserNameCol() {
        return userNameCol;
    }

    /**
     * Set the column in the user table that holds the user's name.
     *
     * @param userNameCol The column name
     */
    public void setUserNameCol( String userNameCol ) {
       this.userNameCol = userNameCol;
    }

    /**
     * Return the table that holds the relation between user's and roles.
     *
     */
    public String getUserRoleTable() {
        return userRoleTable;
    }

    /**
     * Set the table that holds the relation between user's and roles.
     *
     * @param userRoleTable The table name
     */
    public void setUserRoleTable( String userRoleTable ) {
        this.userRoleTable = userRoleTable;
    }

    /**
     * Return the table that holds user data..
     *
     */
    public String getUserTable() {
        return userTable;
    }

    /**
     * Set the table that holds user data.
     *
     * @param userTable The table name
     */
    public void setUserTable( String userTable ) {
      this.userTable = userTable;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Return the Principal associated with the specified username and
     * credentials, if there is one; otherwise return <code>null</code>.
     *
     * If there are any errors with the JDBC connection, executing
     * the query or anything we return null (don't authenticate). This
     * event is also logged, and the connection will be closed so that
     * a subsequent request will automatically re-open it.
     *
     * @param username Username of the Principal to look up
     * @param credentials Password or other credentials to use in
     *  authenticating this username
     */
    public Principal authenticate(String username, String credentials) {

        Connection dbConnection = null;

        try {

            // Ensure that we have an open database connection
            dbConnection = open();
            if (dbConnection == null) {
                // If the db connection open fails, return "not authenticated"
                return null;
            }

            // Acquire a Principal object for this user
            Principal principal = authenticate(dbConnection,
                                               username, credentials);

            if( !dbConnection.getAutoCommit() ) {
                dbConnection.commit();             
            }

            // Release the database connection we just used
            close(dbConnection);
            dbConnection = null;

            // Return the Principal (if any)
            return (principal);

        } catch (SQLException e) {

            // Log the problem for posterity
            log(sm.getString("dataSourceRealm.exception"), e);

            // Close the connection so that it gets reopened next time
            if (dbConnection != null)
                close(dbConnection);

            // Return "not authenticated" for this request
            return (null);

        }

    }


    // -------------------------------------------------------- Package Methods


    // ------------------------------------------------------ Protected Methods


    /**
     * Return the Principal associated with the specified username and
     * credentials, if there is one; otherwise return <code>null</code>.
     *
     * @param dbConnection The database connection to be used
     * @param username Username of the Principal to look up
     * @param credentials Password or other credentials to use in
     *  authenticating this username
     *
     * @exception SQLException if a database error occurs
     */
    private Principal authenticate(Connection dbConnection,
                                               String username,
                                               String credentials)
        throws SQLException {

        ResultSet rs = null;
        PreparedStatement stmt = null;
        ArrayList list = null;

        try {
            // Look up the user's credentials
            String dbCredentials = null;
            stmt = credentials(dbConnection, username);
            rs = stmt.executeQuery();
            while (rs.next()) {
                dbCredentials = rs.getString(1).trim();
            }
            rs.close();
            rs = null;
            stmt.close();
            stmt = null;
            if (dbCredentials == null) {
                return (null);
            }
    
            // Validate the user's credentials
            boolean validated = false;
            if (hasMessageDigest()) {
                // Hex hashes should be compared case-insensitive
                validated = (digest(credentials).equalsIgnoreCase(dbCredentials));
            } else
                validated = (digest(credentials).equals(dbCredentials));
    
            if (validated) {
                if (debug >= 2)
                    log(sm.getString("dataSourceRealm.authenticateSuccess",
                                     username));
            } else {
                if (debug >= 2)
                    log(sm.getString("dataSourceRealm.authenticateFailure",
                                     username));
                return (null);
            }
    
            // Accumulate the user's roles
            list = new ArrayList();
            stmt = roles(dbConnection, username);
            rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(rs.getString(1).trim());
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        }

        // Create and return a suitable Principal for this user
        return (new GenericPrincipal(this, username, credentials, list));

    }


    /**
     * Close the specified database connection.
     *
     * @param dbConnection The connection to be closed
     */
    private void close(Connection dbConnection) {

        // Do nothing if the database connection is already closed
        if (dbConnection == null)
            return;

        // Close this database connection, and log any errors
        try {
            dbConnection.close();
        } catch (SQLException e) {
            log(sm.getString("dataSourceRealm.close"), e); // Just log it here
        }

    }


    /**
     * Open the specified database connection.
     *
     * @return Connection to the database
     */
    private Connection open() {

        try {
            StandardServer server = (StandardServer) ServerFactory.getServer();
            Context context = server.getGlobalNamingContext();
            DataSource dataSource = (DataSource)context.lookup(dataSourceName);
            return dataSource.getConnection();
        } catch (Exception e) {
            // Log the problem for posterity
            log(sm.getString("dataSourceRealm.exception"), e);
        }  
        return null;
    }


    /**
     * Return a PreparedStatement configured to perform the SELECT required
     * to retrieve user credentials for the specified username.
     *
     * @param dbConnection The database connection to be used
     * @param username Username for which credentials should be retrieved
     *
     * @exception SQLException if a database error occurs
     */
    private PreparedStatement credentials(Connection dbConnection,
                                            String username)
        throws SQLException {

        PreparedStatement credentials =
            dbConnection.prepareStatement(preparedCredentials.toString());

        credentials.setString(1, username);
        return (credentials);

    }


    /**
     * Return a short name for this Realm implementation.
     */
    protected String getName() {

        return (this.name);

    }


    /**
     * Return the password associated with the given principal's user name.
     */
    protected String getPassword(String username) {

        return (null);

    }


    /**
     * Return the Principal associated with the given user name.
     */
    protected Principal getPrincipal(String username) {

        return (null);

    }



    /**
     * Return a PreparedStatement configured to perform the SELECT required
     * to retrieve user roles for the specified username.
     *
     * @param dbConnection The database connection to be used
     * @param username Username for which roles should be retrieved
     *
     * @exception SQLException if a database error occurs
     */
    private PreparedStatement roles(Connection dbConnection, String username)
        throws SQLException {

        PreparedStatement roles = 
            dbConnection.prepareStatement(preparedRoles.toString());

        roles.setString(1, username);
        return (roles);

    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     *
     * Prepare for active use of the public methods of this Component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents it from being started
     */
    public void start() throws LifecycleException {

        // Create the roles PreparedStatement string
        preparedRoles = new StringBuffer("SELECT ");
        preparedRoles.append(roleNameCol);
        preparedRoles.append(" FROM ");
        preparedRoles.append(userRoleTable);
        preparedRoles.append(" WHERE ");
        preparedRoles.append(userNameCol);
        preparedRoles.append(" = ?");

        // Create the credentials PreparedStatement string
        preparedCredentials = new StringBuffer("SELECT ");
        preparedCredentials.append(userCredCol);
        preparedCredentials.append(" FROM ");
        preparedCredentials.append(userTable);
        preparedCredentials.append(" WHERE ");
        preparedCredentials.append(userNameCol);
        preparedCredentials.append(" = ?");

        // Perform normal superclass initialization
        super.start();

    }


    /**
     * Gracefully shut down active use of the public methods of this Component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop() throws LifecycleException {

        // Perform normal superclass finalization
        super.stop();

    }


}
