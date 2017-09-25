/*
 * JDBCStore.java
 * $Header: /home/cvs/jakarta-tomcat-catalina/catalina/src/share/org/apache/catalina/session/JDBCStore.java,v 1.7 2004/01/09 11:35:08 remm Exp $
 * $Revision: 1.7 $
 * $Date: 2004/01/09 11:35:08 $
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

package org.apache.catalina.session;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.apache.catalina.Session;
import org.apache.catalina.Store;
import org.apache.catalina.util.CustomObjectInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Implementation of the <code>Store</code> interface that stores
 * serialized session objects in a database.  Sessions that are
 * saved are still subject to being expired based on inactivity.
 *
 * @author Bip Thelin
 * @version $Revision: 1.7 $, $Date: 2004/01/09 11:35:08 $
 */

public class JDBCStore
        extends StoreBase implements Store {

    /**
     * The descriptive information about this implementation.
     */
    protected static String info = "JDBCStore/1.0";

    /**
     * Context name associated with this Store
     */
    private String name = null;

    /**
     * Name to register for this Store, used for logging.
     */
    protected static String storeName = "JDBCStore";

    /**
     * Name to register for the background thread.
     */
    protected String threadName = "JDBCStore";

    /**
     * The connection username to use when trying to connect to the database.
     */
    protected String connectionName = null;


    /**
     * The connection URL to use when trying to connect to the database.
     */
    protected String connectionPassword = null;

    /**
     * Connection string to use when connecting to the DB.
     */
    protected String connectionURL = null;

    /**
     * The database connection.
     */
    private Connection dbConnection = null;

    /**
     * Instance of the JDBC Driver class we use as a connection factory.
     */
    protected Driver driver = null;

    /**
     * Driver to use.
     */
    protected String driverName = null;

    // ------------------------------------------------------------- Table & cols

    /**
     * Table to use.
     */
    protected String sessionTable = "tomcat$sessions";

    /**
     * Column to use for /Engine/Host/Context name
     */
    protected String sessionAppCol = "app";

    /**
     * Id column to use.
     */
    protected String sessionIdCol = "id";

    /**
     * Data column to use.
     */
    protected String sessionDataCol = "data";

    /**
     * Is Valid column to use.
     */
    protected String sessionValidCol = "valid";

    /**
     * Max Inactive column to use.
     */
    protected String sessionMaxInactiveCol = "maxinactive";

    /**
     * Last Accessed column to use.
     */
    protected String sessionLastAccessedCol = "lastaccess";

    // ------------------------------------------------------------- SQL Variables

    /**
     * Variable to hold the <code>getSize()</code> prepared statement.
     */
    protected PreparedStatement preparedSizeSql = null;

    /**
     * Variable to hold the <code>keys()</code> prepared statement.
     */
    protected PreparedStatement preparedKeysSql = null;

    /**
     * Variable to hold the <code>save()</code> prepared statement.
     */
    protected PreparedStatement preparedSaveSql = null;

    /**
     * Variable to hold the <code>clear()</code> prepared statement.
     */
    protected PreparedStatement preparedClearSql = null;

    /**
     * Variable to hold the <code>remove()</code> prepared statement.
     */
    protected PreparedStatement preparedRemoveSql = null;

    /**
     * Variable to hold the <code>load()</code> prepared statement.
     */
    protected PreparedStatement preparedLoadSql = null;

    // ------------------------------------------------------------- Properties

    /**
     * Return the info for this Store.
     */
    public String getInfo() {
        return (info);
    }

    /**
     * Return the name for this instance (built from container name)
     */
    public String getName() {
        if (name == null) {
            Container container = manager.getContainer();
            String contextName = container.getName();
            String hostName = "";
            String engineName = "";

            if (container.getParent() != null) {
                Container host = container.getParent();
                hostName = host.getName();
                if (host.getParent() != null) {
                    engineName = host.getParent().getName();
                }
            }
            name = "/" + engineName + "/" + hostName + contextName;
        }
        return name;
    }

    /**
     * Return the thread name for this Store.
     */
    public String getThreadName() {
        return (threadName);
    }

    /**
     * Return the name for this Store, used for logging.
     */
    public String getStoreName() {
        return (storeName);
    }

    /**
     * Set the driver for this Store.
     *
     * @param driverName The new driver
     */
    public void setDriverName(String driverName) {
        String oldDriverName = this.driverName;
        this.driverName = driverName;
        support.firePropertyChange("driverName",
                oldDriverName,
                this.driverName);
        this.driverName = driverName;
    }

    /**
     * Return the driver for this Store.
     */
    public String getDriverName() {
        return (this.driverName);
    }

    /**
     * Return the username to use to connect to the database.
     *
     */
    public String getConnectionName() {
        return connectionName;
    }

    /**
     * Set the username to use to connect to the database.
     *
     * @param connectionName Username
     */
    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    /**
     * Return the password to use to connect to the database.
     *
     */
    public String getConnectionPassword() {
        return connectionPassword;
    }

    /**
     * Set the password to use to connect to the database.
     *
     * @param connectionPassword User password
     */
    public void setConnectionPassword(String connectionPassword) {
        this.connectionPassword = connectionPassword;
    }

    /**
     * Set the Connection URL for this Store.
     *
     * @param connectionURL The new Connection URL
     */
    public void setConnectionURL(String connectionURL) {
        String oldConnString = this.connectionURL;
        this.connectionURL = connectionURL;
        support.firePropertyChange("connectionURL",
                oldConnString,
                this.connectionURL);
    }

    /**
     * Return the Connection URL for this Store.
     */
    public String getConnectionURL() {
        return (this.connectionURL);
    }

    /**
     * Set the table for this Store.
     *
     * @param sessionTable The new table
     */
    public void setSessionTable(String sessionTable) {
        String oldSessionTable = this.sessionTable;
        this.sessionTable = sessionTable;
        support.firePropertyChange("sessionTable",
                oldSessionTable,
                this.sessionTable);
    }

    /**
     * Return the table for this Store.
     */
    public String getSessionTable() {
        return (this.sessionTable);
    }

    /**
     * Set the App column for the table.
     *
     * @param sessionAppCol the column name
     */
    public void setSessionAppCol(String sessionAppCol) {
        String oldSessionAppCol = this.sessionAppCol;
        this.sessionAppCol = sessionAppCol;
        support.firePropertyChange("sessionAppCol",
                oldSessionAppCol,
                this.sessionAppCol);
    }

    /**
     * Return the web application name column for the table.
     */
    public String getSessionAppCol() {
        return (this.sessionAppCol);
    }

    /**
     * Set the Id column for the table.
     *
     * @param sessionIdCol the column name
     */
    public void setSessionIdCol(String sessionIdCol) {
        String oldSessionIdCol = this.sessionIdCol;
        this.sessionIdCol = sessionIdCol;
        support.firePropertyChange("sessionIdCol",
                oldSessionIdCol,
                this.sessionIdCol);
    }

    /**
     * Return the Id column for the table.
     */
    public String getSessionIdCol() {
        return (this.sessionIdCol);
    }

    /**
     * Set the Data column for the table
     *
     * @param sessionDataCol the column name
     */
    public void setSessionDataCol(String sessionDataCol) {
        String oldSessionDataCol = this.sessionDataCol;
        this.sessionDataCol = sessionDataCol;
        support.firePropertyChange("sessionDataCol",
                oldSessionDataCol,
                this.sessionDataCol);
    }

    /**
     * Return the data column for the table
     */
    public String getSessionDataCol() {
        return (this.sessionDataCol);
    }

    /**
     * Set the Is Valid column for the table
     *
     * @param sessionValidCol The column name
     */
    public void setSessionValidCol(String sessionValidCol) {
        String oldSessionValidCol = this.sessionValidCol;
        this.sessionValidCol = sessionValidCol;
        support.firePropertyChange("sessionValidCol",
                oldSessionValidCol,
                this.sessionValidCol);
    }

    /**
     * Return the Is Valid column
     */
    public String getSessionValidCol() {
        return (this.sessionValidCol);
    }

    /**
     * Set the Max Inactive column for the table
     *
     * @param sessionMaxInactiveCol The column name
     */
    public void setSessionMaxInactiveCol(String sessionMaxInactiveCol) {
        String oldSessionMaxInactiveCol = this.sessionMaxInactiveCol;
        this.sessionMaxInactiveCol = sessionMaxInactiveCol;
        support.firePropertyChange("sessionMaxInactiveCol",
                oldSessionMaxInactiveCol,
                this.sessionMaxInactiveCol);
    }

    /**
     * Return the Max Inactive column
     */
    public String getSessionMaxInactiveCol() {
        return (this.sessionMaxInactiveCol);
    }

    /**
     * Set the Last Accessed column for the table
     *
     * @param sessionLastAccessedCol The column name
     */
    public void setSessionLastAccessedCol(String sessionLastAccessedCol) {
        String oldSessionLastAccessedCol = this.sessionLastAccessedCol;
        this.sessionLastAccessedCol = sessionLastAccessedCol;
        support.firePropertyChange("sessionLastAccessedCol",
                oldSessionLastAccessedCol,
                this.sessionLastAccessedCol);
    }

    /**
     * Return the Last Accessed column
     */
    public String getSessionLastAccessedCol() {
        return (this.sessionLastAccessedCol);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Return an array containing the session identifiers of all Sessions
     * currently saved in this Store.  If there are no such Sessions, a
     * zero-length array is returned.
     *
     * @exception IOException if an input/output error occurred
     */
    public String[] keys() throws IOException {
        String keysSql =
                "SELECT " + sessionIdCol + " FROM " + sessionTable +
                " WHERE " + sessionAppCol + " = ?";
        ResultSet rst = null;
        String keys[] = null;
        synchronized (this) {
            int numberOfTries = 2;
            while (numberOfTries > 0) {

                Connection _conn = getConnection();
                if (_conn == null) {
                    return (new String[0]);
                }
                try {
                    if (preparedKeysSql == null) {
                        preparedKeysSql = _conn.prepareStatement(keysSql);
                    }

                    preparedKeysSql.setString(1, getName());
                    rst = preparedKeysSql.executeQuery();
                    ArrayList tmpkeys = new ArrayList();
                    if (rst != null) {
                        while (rst.next()) {
                            tmpkeys.add(rst.getString(1));
                        }
                    }
                    keys = (String[]) tmpkeys.toArray(new String[tmpkeys.size()]);
                } catch (SQLException e) {
                    log(sm.getString(getStoreName() + ".SQLException", e));
                    // Close the connection so that it gets reopened next time
                    if (dbConnection != null)
                        close(dbConnection);
                } finally {
                    try {
                        if (rst != null) {
                            rst.close();
                        }
                    } catch (SQLException e) {
                        ;
                    }

                    release(_conn);
                }
                numberOfTries--;
            }
        }

        return (keys);
    }

    /**
     * Return an integer containing a count of all Sessions
     * currently saved in this Store.  If there are no Sessions,
     * <code>0</code> is returned.
     *
     * @exception IOException if an input/output error occurred
     */
    public int getSize() throws IOException {
        int size = 0;
        String sizeSql =
                "SELECT COUNT(" + sessionIdCol + ") FROM " + sessionTable +
                " WHERE " + sessionAppCol + " = ?";
        ResultSet rst = null;

        synchronized (this) {
            int numberOfTries = 2;
            while (numberOfTries > 0) {
                Connection _conn = getConnection();

                if (_conn == null) {
                    return (size);
                }

                try {
                    if (preparedSizeSql == null) {
                        preparedSizeSql = _conn.prepareStatement(sizeSql);
                    }

                    preparedSizeSql.setString(1, getName());
                    rst = preparedSizeSql.executeQuery();
                    if (rst.next()) {
                        size = rst.getInt(1);
                    }
                } catch (SQLException e) {
                    log(sm.getString(getStoreName() + ".SQLException", e));
                    if (dbConnection != null)
                        close(dbConnection);
                } finally {
                    try {
                        if (rst != null)
                            rst.close();
                    } catch (SQLException e) {
                        ;
                    }

                    release(_conn);
                }
                numberOfTries--;
            }
        }
        return (size);
    }

    /**
     * Load the Session associated with the id <code>id</code>.
     * If no such session is found <code>null</code> is returned.
     *
     * @param id a value of type <code>String</code>
     * @return the stored <code>Session</code>
     * @exception ClassNotFoundException if an error occurs
     * @exception IOException if an input/output error occurred
     */
    public Session load(String id)
            throws ClassNotFoundException, IOException {
        ResultSet rst = null;
        StandardSession _session = null;
        Loader loader = null;
        ClassLoader classLoader = null;
        ObjectInputStream ois = null;
        BufferedInputStream bis = null;
        Container container = manager.getContainer();
        String loadSql =
                "SELECT " + sessionIdCol + ", " + sessionDataCol + " FROM " +
                sessionTable + " WHERE " + sessionIdCol + " = ? AND " +
                sessionAppCol + " = ?";

        synchronized (this) {
            int numberOfTries = 2;
            while (numberOfTries > 0) {
                Connection _conn = getConnection();
                if (_conn == null) {
                    return (null);
                }

                try {
                    if (preparedLoadSql == null) {
                        preparedLoadSql = _conn.prepareStatement(loadSql);
                    }

                    preparedLoadSql.setString(1, id);
                    preparedLoadSql.setString(2, getName());
                    rst = preparedLoadSql.executeQuery();
                    if (rst.next()) {
                        bis = new BufferedInputStream(rst.getBinaryStream(2));

                        if (container != null) {
                            loader = container.getLoader();
                        }
                        if (loader != null) {
                            classLoader = loader.getClassLoader();
                        }
                        if (classLoader != null) {
                            ois = new CustomObjectInputStream(bis,
                                    classLoader);
                        } else {
                            ois = new ObjectInputStream(bis);
                        }

                        if (debug > 0) {
                            log(sm.getString(getStoreName() + ".loading",
                                    id, sessionTable));
                        }

                        _session = (StandardSession) manager.createEmptySession();
                        _session.readObjectData(ois);
                        _session.setManager(manager);

                    } else if (debug > 0) {
                        log(getStoreName() + ": No persisted data object found");
                    }
                } catch (SQLException e) {
                    log(sm.getString(getStoreName() + ".SQLException", e));
                    if (dbConnection != null)
                        close(dbConnection);
                } finally {
                    try {
                        if (rst != null) {
                            rst.close();
                        }
                    } catch (SQLException e) {
                        ;
                    }
                    if (ois != null) {
                        try {
                            ois.close();
                        } catch (IOException e) {
                            ;
                        }
                    }
                    release(_conn);
                }
                numberOfTries--;
            }
        }

        return (_session);
    }

    /**
     * Remove the Session with the specified session identifier from
     * this Store, if present.  If no such Session is present, this method
     * takes no action.
     *
     * @param id Session identifier of the Session to be removed
     *
     * @exception IOException if an input/output error occurs
     */
    public void remove(String id) throws IOException {
        String removeSql =
                "DELETE FROM " + sessionTable + " WHERE " + sessionIdCol +
                " = ?  AND " + sessionAppCol + " = ?";

        synchronized (this) {
            int numberOfTries = 2;
            while (numberOfTries > 0) {
                Connection _conn = getConnection();

                if (_conn == null) {
                    return;
                }

                try {
                    if (preparedRemoveSql == null) {
                        preparedRemoveSql = _conn.prepareStatement(removeSql);
                    }

                    preparedRemoveSql.setString(1, id);
                    preparedRemoveSql.setString(2, getName());
                    preparedRemoveSql.execute();
                } catch (SQLException e) {
                    log(sm.getString(getStoreName() + ".SQLException", e));
                    if (dbConnection != null)
                        close(dbConnection);
                } finally {
                    release(_conn);
                }
                numberOfTries--;
            }
        }

        if (debug > 0) {
            log(sm.getString(getStoreName() + ".removing", id, sessionTable));
        }
    }

    /**
     * Remove all of the Sessions in this Store.
     *
     * @exception IOException if an input/output error occurs
     */
    public void clear() throws IOException {
        String clearSql =
                "DELETE FROM " + sessionTable + " WHERE " + sessionAppCol + " = ?";

        synchronized (this) {
            int numberOfTries = 2;
            while (numberOfTries > 0) {
                Connection _conn = getConnection();
                if (_conn == null) {
                    return;
                }

                try {
                    if (preparedClearSql == null) {
                        preparedClearSql = _conn.prepareStatement(clearSql);
                    }

                    preparedClearSql.setString(1, getName());
                    preparedClearSql.execute();
                } catch (SQLException e) {
                    log(sm.getString(getStoreName() + ".SQLException", e));
                    if (dbConnection != null)
                        close(dbConnection);
                } finally {
                    release(_conn);
                }
                numberOfTries--;
            }
        }
    }

    /**
     * Save a session to the Store.
     *
     * @param session the session to be stored
     * @exception IOException if an input/output error occurs
     */
    public void save(Session session) throws IOException {
        String saveSql =
                "INSERT INTO " + sessionTable + " (" + sessionIdCol + ", " +
                sessionAppCol + ", " +
                sessionDataCol + ", " +
                sessionValidCol + ", " +
                sessionMaxInactiveCol + ", " +
                sessionLastAccessedCol + ") VALUES (?, ?, ?, ?, ?, ?)";
        ObjectOutputStream oos = null;
        ByteArrayOutputStream bos = null;
        ByteArrayInputStream bis = null;
        InputStream in = null;

        synchronized (this) {
            int numberOfTries = 2;
            while (numberOfTries > 0) {
                Connection _conn = getConnection();
                if (_conn == null) {
                    return;
                }

                // If sessions already exist in DB, remove and insert again.
                // TODO:
                // * Check if ID exists in database and if so use UPDATE.
                remove(session.getId());

                try {
                    bos = new ByteArrayOutputStream();
                    oos = new ObjectOutputStream(new BufferedOutputStream(bos));

                    ((StandardSession) session).writeObjectData(oos);
                    oos.close();
                    oos = null;
                    byte[] obs = bos.toByteArray();
                    int size = obs.length;
                    bis = new ByteArrayInputStream(obs, 0, size);
                    in = new BufferedInputStream(bis, size);

                    if (preparedSaveSql == null) {
                        preparedSaveSql = _conn.prepareStatement(saveSql);
                    }

                    preparedSaveSql.setString(1, session.getId());
                    preparedSaveSql.setString(2, getName());
                    preparedSaveSql.setBinaryStream(3, in, size);
                    preparedSaveSql.setString(4, session.isValid() ? "1" : "0");
                    preparedSaveSql.setInt(5, session.getMaxInactiveInterval());
                    preparedSaveSql.setLong(6, session.getLastAccessedTime());
                    preparedSaveSql.execute();
                } catch (SQLException e) {
                    log(sm.getString(getStoreName() + ".SQLException", e));
                    if (dbConnection != null)
                        close(dbConnection);
                } catch (IOException e) {
                    ;
                } finally {
                    if (oos != null) {
                        oos.close();
                    }
                    if (bis != null) {
                        bis.close();
                    }
                    if (in != null) {
                        in.close();
                    }

                    release(_conn);
                }
                numberOfTries--;
            }
        }

        if (debug > 0) {
            log(sm.getString(getStoreName() + ".saving",
                    session.getId(), sessionTable));
        }
    }

    // --------------------------------------------------------- Protected Methods

    /**
     * Check the connection associated with this store, if it's
     * <code>null</code> or closed try to reopen it.
     * Returns <code>null</code> if the connection could not be established.
     *
     * @return <code>Connection</code> if the connection suceeded
     */
    protected Connection getConnection() {
        try {
            if (dbConnection == null || dbConnection.isClosed()) {
                log(sm.getString(getStoreName() + ".checkConnectionDBClosed"));
                open();
                if (dbConnection == null || dbConnection.isClosed()) {
                    log(sm.getString(getStoreName() + ".checkConnectionDBReOpenFail"));
                }
            }
        } catch (SQLException ex) {
            log(sm.getString(getStoreName() + ".checkConnectionSQLException",
                    ex.toString()));
        }

        return dbConnection;
    }

    /**
     * Open (if necessary) and return a database connection for use by
     * this Realm.
     *
     * @exception SQLException if a database error occurs
     */
    protected Connection open() throws SQLException {

        // Do nothing if there is a database connection already open
        if (dbConnection != null)
            return (dbConnection);

        // Instantiate our database driver if necessary
        if (driver == null) {
            try {
                Class clazz = Class.forName(driverName);
                driver = (Driver) clazz.newInstance();
            } catch (ClassNotFoundException ex) {
                log(sm.getString(getStoreName() + ".checkConnectionClassNotFoundException",
                        ex.toString()));
            } catch (InstantiationException ex) {
                log(sm.getString(getStoreName() + ".checkConnectionClassNotFoundException",
                        ex.toString()));
            } catch (IllegalAccessException ex) {
                log(sm.getString(getStoreName() + ".checkConnectionClassNotFoundException",
                        ex.toString()));
            }
        }

        // Open a new connection
        Properties props = new Properties();
        if (connectionName != null)
            props.put("user", connectionName);
        if (connectionPassword != null)
            props.put("password", connectionPassword);
        dbConnection = driver.connect(connectionURL, props);
        dbConnection.setAutoCommit(true);
        return (dbConnection);

    }

    /**
     * Close the specified database connection.
     *
     * @param dbConnection The connection to be closed
     */
    protected void close(Connection dbConnection) {

        // Do nothing if the database connection is already closed
        if (dbConnection == null)
            return;

        // Close our prepared statements (if any)
        try {
            preparedSizeSql.close();
        } catch (Throwable f) {
            ;
        }
        this.preparedSizeSql = null;

        try {
            preparedKeysSql.close();
        } catch (Throwable f) {
            ;
        }
        this.preparedKeysSql = null;

        try {
            preparedSaveSql.close();
        } catch (Throwable f) {
            ;
        }
        this.preparedSaveSql = null;

        try {
            preparedClearSql.close();
        } catch (Throwable f) {
            ;
        }
        this.preparedClearSql = null;

        try {
            preparedRemoveSql.close();
        } catch (Throwable f) {
            ;
        }
        this.preparedRemoveSql = null;

        try {
            preparedLoadSql.close();
        } catch (Throwable f) {
            ;
        }
        this.preparedLoadSql = null;

        // Close this database connection, and log any errors
        try {
            dbConnection.close();
        } catch (SQLException e) {
            log(sm.getString(getStoreName() + ".close", e.toString())); // Just log it here
        } finally {
            this.dbConnection = null;
        }

    }

    /**
     * Release the connection, not needed here since the
     * connection is not associated with a connection pool.
     *
     * @param conn The connection to be released
     */
    protected void release(Connection conn) {
        ;
    }

    /**
     * Called once when this Store is first started.
     */
    public void start() throws LifecycleException {
        super.start();

        // Open connection to the database
        this.dbConnection = getConnection();
    }

    /**
     * Gracefully terminate everything associated with our db.
     * Called once when this Store is stoping.
     *
     */
    public void stop() throws LifecycleException {
        super.stop();

        // Close and release everything associated with our db.
        if (dbConnection != null) {
            try {
                dbConnection.commit();
            } catch (SQLException e) {
                ;
            }
            close(dbConnection);
        }
    }
}
