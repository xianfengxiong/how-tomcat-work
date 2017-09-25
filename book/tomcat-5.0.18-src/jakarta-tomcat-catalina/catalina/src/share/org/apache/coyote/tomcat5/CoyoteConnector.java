/*
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


package org.apache.coyote.tomcat5;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.catalina.Connector;
import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Service;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.net.ServerSocketFactory;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.modeler.Registry;
import org.apache.coyote.Adapter;
import org.apache.coyote.ProtocolHandler;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.http.mapper.Mapper;


/**
 * Implementation of a Coyote connector for Tomcat 5.x.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.35 $ $Date: 2004/01/10 20:39:50 $
 */


public class CoyoteConnector
    implements Connector, Lifecycle, MBeanRegistration
{
    private static Log log = LogFactory.getLog(CoyoteConnector.class);


    // ----------------------------------------------------- Instance Variables


    /**
     * Holder for our configured properties.
     */
    private HashMap properties = new HashMap();

    /**
     * The <code>Service</code> we are associated with (if any).
     */
    private Service service = null;


    /**
     * The accept count for this Connector.
     */
    private int acceptCount = 10;


    /**
     * The IP address on which to bind, if any.  If <code>null</code>, all
     * addresses on the server will be bound.
     */
    private String address = null;


    /**
     * Do we allow TRACE ?
     */
    private boolean allowTrace = false;


    /**
     * The input buffer size we should create on input streams.
     */
    private int bufferSize = 2048;


    /**
     * The Container used for processing requests received by this Connector.
     */
    protected Container container = null;


    /**
     * Compression value.
     */
    private String compression = "off";


    /**
     * The debugging detail level for this component.
     */
    private int debug = 0;


    /**
     * The "enable DNS lookups" flag for this Connector.
     */
    private boolean enableLookups = false;


    /**
     * The server socket factory for this component.
     */
    private ServerSocketFactory factory = null;


    /*
     * Is generation of X-Powered-By response header enabled/disabled?
     */
    private boolean xpoweredBy;


    /**
     * Descriptive information about this Connector implementation.
     */
    private static final String info =
        "org.apache.coyote.tomcat5.CoyoteConnector/2.0";


    /**
     * The lifecycle event support for this component.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * The minimum number of processors to start at initialization time.
     */
    protected int minProcessors = 5;


    /**
     * The maximum number of processors allowed, or <0 for unlimited.
     */
    private int maxProcessors = 20;


    /**
     * Linger value on the incoming connection.
     * Note : a value inferior to 0 means no linger.
     */
    private int connectionLinger = Constants.DEFAULT_CONNECTION_LINGER;


    /**
     * Timeout value on the incoming connection.
     * Note : a value of 0 means no timeout.
     */
    private int connectionTimeout = Constants.DEFAULT_CONNECTION_TIMEOUT;


    /**
     * Timeout value on the incoming connection during request processing.
     * Note : a value of 0 means no timeout.
     */
    private int connectionUploadTimeout = 
        Constants.DEFAULT_CONNECTION_UPLOAD_TIMEOUT;


    /**
     * Timeout value on the server socket.
     * Note : a value of 0 means no timeout.
     */
    private int serverSocketTimeout = Constants.DEFAULT_SERVER_SOCKET_TIMEOUT;


    /**
     * The port number on which we listen for requests.
     */
    private int port = 0;


    /**
     * The server name to which we should pretend requests to this Connector
     * were directed.  This is useful when operating Tomcat behind a proxy
     * server, so that redirects get constructed accurately.  If not specified,
     * the server name included in the <code>Host</code> header is used.
     */
    private String proxyName = null;


    /**
     * The server port to which we should pretent requests to this Connector
     * were directed.  This is useful when operating Tomcat behind a proxy
     * server, so that redirects get constructed accurately.  If not specified,
     * the port number specified by the <code>port</code> property is used.
     */
    private int proxyPort = 0;


    /**
     * The redirect port for non-SSL to SSL redirects.
     */
    private int redirectPort = 443;


    /**
     * The request scheme that will be set on all requests received
     * through this connector.
     */
    private String scheme = "http";


    /**
     * The secure connection flag that will be set on all requests received
     * through this connector.
     */
    private boolean secure = false;


    /** For jk, do tomcat authentication if true, trust server if false 
     */ 
    private boolean tomcatAuthentication = true;


    /**
     * The string manager for this package.
     */
    private StringManager sm =
        StringManager.getManager(Constants.Package);


    /**
     * Flag to disable setting a seperate time-out for uploads.
     * If <code>true</code>, then the <code>timeout</code> parameter is
     * ignored.  If <code>false</code>, then the <code>timeout</code>
     * parameter is used to control uploads.
     */
    private boolean disableUploadTimeout = false;


    /**
     * Maximum number of Keep-Alive requests to honor per connection.
     */
    private int maxKeepAliveRequests = 100;


    /**
     * Maximum size of a POST which will be automatically parsed by the 
     * container. 2MB by default.
     */
    private int maxPostSize = 2 * 1024 * 1024;


    /**
     * Has this component been initialized yet?
     */
    private boolean initialized = false;


    /**
     * Has this component been started yet?
     */
    private boolean started = false;


    /**
     * The shutdown signal to our background thread
     */
    private boolean stopped = false;


    /**
     * The background thread.
     */
    private Thread thread = null;


    /**
     * Use TCP no delay ?
     */
    private boolean tcpNoDelay = true;


    /**
     * Coyote Protocol handler class name.
     * Defaults to the Coyote HTTP/1.1 protocolHandler.
     */
    private String protocolHandlerClassName =
        "org.apache.coyote.http11.Http11Protocol";


    /**
     * Coyote protocol handler.
     */
    private ProtocolHandler protocolHandler = null;


    /**
     * Coyote adapter.
     */
    private Adapter adapter = null;


     /**
      * Mapper.
      */
     private Mapper mapper = new Mapper();


     /**
      * Mapper listener.
      */
     private MapperListener mapperListener = new MapperListener(mapper);


     /**
      * URI encoding.
      */
     private String URIEncoding = null;


     /**
      * URI encoding as body.
      */
     private boolean useBodyEncodingForURI = false;


    // ------------------------------------------------------------- Properties


    /**
     * Return a configured property.
     */
    public Object getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Set a configured property.
     */
    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }

    /** 
     * remove a configured property.
     */
    public void removeProperty(String name) {
        properties.remove(name);
    }

    /**
     * Return the <code>Service</code> with which we are associated (if any).
     */
    public Service getService() {

        return (this.service);

    }


    /**
     * Set the <code>Service</code> with which we are associated (if any).
     *
     * @param service The service that owns this Engine
     */
    public void setService(Service service) {

        this.service = service;
        setProperty("service", service);

    }


    /**
     * Get the value of compression.
     */
    public String getCompression() {

        return (compression);

    }


    /**
     * Set the value of compression.
     *
     * @param compression The new compression value, which can be "on", "off"
     * or "force"
     */
    public void setCompression(String compression) {

        this.compression = compression;
        setProperty("compression", compression);

    }


    /**
     * Return the connection linger for this Connector.
     */
    public int getConnectionLinger() {

        return (connectionLinger);

    }


    /**
     * Set the connection linger for this Connector.
     *
     * @param count The new connection linge
     */
    public void setConnectionLinger(int connectionLinger) {

        this.connectionLinger = connectionLinger;
        setProperty("soLinger", String.valueOf(connectionLinger));

    }


    /**
     * Return the connection timeout for this Connector.
     */
    public int getConnectionTimeout() {

        return (connectionTimeout);

    }


    /**
     * Set the connection timeout for this Connector.
     *
     * @param count The new connection timeout
     */
    public void setConnectionTimeout(int connectionTimeout) {

        this.connectionTimeout = connectionTimeout;
        setProperty("soTimeout", String.valueOf(connectionTimeout));

    }


    /**
     * Return the connection upload timeout for this Connector.
     */
    public int getConnectionUploadTimeout() {

        return (connectionUploadTimeout);

    }


    /**
     * Set the connection upload timeout for this Connector.
     *
     * @param connectionUploadTimeout The new connection upload timeout
     */
    public void setConnectionUploadTimeout(int connectionUploadTimeout) {

        this.connectionUploadTimeout = connectionUploadTimeout;
        setProperty("timeout", String.valueOf(connectionUploadTimeout));

    }


    /**
     * Return the server socket timeout for this Connector.
     */
    public int getServerSocketTimeout() {

        return (serverSocketTimeout);

    }


    /**
     * Set the server socket timeout for this Connector.
     *
     * @param connectionUploadTimeout The new server socket timeout
     */
    public void setServerSocketTimeout(int serverSocketTimeout) {

        this.serverSocketTimeout = serverSocketTimeout;
        setProperty("serverSoTimeout", String.valueOf(serverSocketTimeout));

    }


    /**
     * Return the accept count for this Connector.
     */
    public int getAcceptCount() {

        return (acceptCount);

    }


    /**
     * Set the accept count for this Connector.
     *
     * @param count The new accept count
     */
    public void setAcceptCount(int count) {

        this.acceptCount = count;
        setProperty("backlog", String.valueOf(count));

    }


    /**
     * Return the bind IP address for this Connector.
     */
    public String getAddress() {

        return (this.address);

    }


    /**
     * Set the bind IP address for this Connector.
     *
     * @param address The bind IP address
     */
    public void setAddress(String address) {

        this.address = address;
        setProperty("address", address);

    }


    /**
     * True if the TRACE method is allowed.  Default value is "false".
     */
    public boolean getAllowTrace() {

        return (this.allowTrace);

    }


    /**
     * Set the allowTrace flag, to disable or enable the TRACE HTTP method.
     *
     * @param allowTrace The new allowTrace flag
     */
    public void setAllowTrace(boolean allowTrace) {

        this.allowTrace = allowTrace;
        setProperty("allowTrace", String.valueOf(allowTrace));

    }

    /**
     * Is this connector available for processing requests?
     */
    public boolean isAvailable() {

        return (started);

    }


    /**
     * Return the input buffer size for this Connector.
     */
    public int getBufferSize() {

        return (this.bufferSize);

    }


    /**
     * Set the input buffer size for this Connector.
     *
     * @param bufferSize The new input buffer size.
     */
    public void setBufferSize(int bufferSize) {

        this.bufferSize = bufferSize;
        setProperty("bufferSize", String.valueOf(bufferSize));

    }


    /**
     * Return the Container used for processing requests received by this
     * Connector.
     */
    public Container getContainer() {
        if( container==null ) {
            // Lazy - maybe it was added later
            findContainer();     
        }
        return (container);

    }


    /**
     * Set the Container used for processing requests received by this
     * Connector.
     *
     * @param container The new Container to use
     */
    public void setContainer(Container container) {

        this.container = container;

    }


    /**
     * Return the debugging detail level for this component.
     */
    public int getDebug() {

        return (debug);

    }


    /**
     * Set the debugging detail level for this component.
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {

        this.debug = debug;

    }


    /**
     * Return the "enable DNS lookups" flag.
     */
    public boolean getEnableLookups() {

        return (this.enableLookups);

    }


    /**
     * Set the "enable DNS lookups" flag.
     *
     * @param enableLookups The new "enable DNS lookups" flag value
     */
    public void setEnableLookups(boolean enableLookups) {

        this.enableLookups = enableLookups;
        setProperty("enableLookups", String.valueOf(enableLookups));

    }


    /**
     * Return the server socket factory used by this Container.
     */
    public ServerSocketFactory getFactory() {

        return (this.factory);

    }


    /**
     * Set the server socket factory used by this Container.
     *
     * @param factory The new server socket factory
     */
    public void setFactory(ServerSocketFactory factory) {

        this.factory = factory;

    }


    /**
     * Return descriptive information about this Connector implementation.
     */
    public String getInfo() {

        return (info);

    }


     /**
      * Return the mapper.
      */
     public Mapper getMapper() {

         return (mapper);

     }


    /**
     * Return the minimum number of processors to start at initialization.
     */
    public int getMinProcessors() {

        return (minProcessors);

    }


    /**
     * Set the minimum number of processors to start at initialization.
     *
     * @param minProcessors The new minimum processors
     */
    public void setMinProcessors(int minProcessors) {

        this.minProcessors = minProcessors;
        setProperty("minProcessors", String.valueOf(minProcessors));

    }


    /**
     * Return the maximum number of processors allowed, or <0 for unlimited.
     */
    public int getMaxProcessors() {

        return (maxProcessors);

    }


    /**
     * Set the maximum number of processors allowed, or <0 for unlimited.
     *
     * @param maxProcessors The new maximum processors
     */
    public void setMaxProcessors(int maxProcessors) {

        this.maxProcessors = maxProcessors;
        setProperty("maxThreads", String.valueOf(maxProcessors));

    }


    /**
     * Return the maximum size of a POST which will be automatically
     * parsed by the container.
     */
    public int getMaxPostSize() {

        return (maxPostSize);

    }


    /**
     * Set the maximum size of a POST which will be automatically
     * parsed by the container.
     *
     * @param maxPostSize The new maximum size in bytes of a POST which will 
     * be automatically parsed by the container
     */
    public void setMaxPostSize(int maxPostSize) {

        this.maxPostSize = maxPostSize;
        setProperty("maxPostSize", String.valueOf(maxPostSize));
    }


    /**
     * Return the port number on which we listen for requests.
     */
    public int getPort() {

        return (this.port);

    }


    /**
     * Set the port number on which we listen for requests.
     *
     * @param port The new port number
     */
    public void setPort(int port) {

        this.port = port;
        setProperty("port", String.valueOf(port));

    }


    /**
     * Return the Coyote protocol handler in use.
     */
    public String getProtocol() {

        if ("org.apache.coyote.http11.Http11Protocol".equals
            (getProtocolHandlerClassName())) {
            return "HTTP/1.1";
        } else if ("org.apache.jk.server.JkCoyoteHandler".equals
                   (getProtocolHandlerClassName())) {
            return "AJP/1.3";
        }
        return null;

    }


    /**
     * Set the Coyote protocol which will be used by the connector.
     *
     * @param protocol The Coyote protocol name
     */
    public void setProtocol(String protocol) {

        if (protocol.equals("HTTP/1.1")) {
            setProtocolHandlerClassName
                ("org.apache.coyote.http11.Http11Protocol");
        } else if (protocol.equals("AJP/1.3")) {
            setProtocolHandlerClassName
                ("org.apache.jk.server.JkCoyoteHandler");
        } else {
            setProtocolHandlerClassName(null);
        }

    }


    /**
     * Return the class name of the Coyote protocol handler in use.
     */
    public String getProtocolHandlerClassName() {

        return (this.protocolHandlerClassName);

    }


    /**
     * Set the class name of the Coyote protocol handler which will be used
     * by the connector.
     *
     * @param protocolHandlerClassName The new class name
     */
    public void setProtocolHandlerClassName(String protocolHandlerClassName) {

        this.protocolHandlerClassName = protocolHandlerClassName;

    }


    /**
     * Return the protocol handler associated with the connector.
     */
    public ProtocolHandler getProtocolHandler() {

        return (this.protocolHandler);

    }


    /**
     * Return the proxy server name for this Connector.
     */
    public String getProxyName() {

        return (this.proxyName);

    }


    /**
     * Set the proxy server name for this Connector.
     *
     * @param proxyName The new proxy server name
     */
    public void setProxyName(String proxyName) {

        if(proxyName != null && proxyName.length() > 0) {
            this.proxyName = proxyName;
            setProperty("proxyName", proxyName);
        } else {
            this.proxyName = null;
            removeProperty("proxyName");
        }

    }


    /**
     * Return the proxy server port for this Connector.
     */
    public int getProxyPort() {

        return (this.proxyPort);

    }


    /**
     * Set the proxy server port for this Connector.
     *
     * @param proxyPort The new proxy server port
     */
    public void setProxyPort(int proxyPort) {

        this.proxyPort = proxyPort;
        setProperty("proxyPort", String.valueOf(proxyPort));

    }


    /**
     * Return the port number to which a request should be redirected if
     * it comes in on a non-SSL port and is subject to a security constraint
     * with a transport guarantee that requires SSL.
     */
    public int getRedirectPort() {

        return (this.redirectPort);

    }


    /**
     * Set the redirect port number.
     *
     * @param redirectPort The redirect port number (non-SSL to SSL)
     */
    public void setRedirectPort(int redirectPort) {

        this.redirectPort = redirectPort;
        setProperty("redirectPort", String.valueOf(redirectPort));

    }

    /**
     * Return the flag that specifies upload time-out behavior.
     */
    public boolean getDisableUploadTimeout() {
        return disableUploadTimeout;
    }

    /**
     * Set the flag to specify upload time-out behavior.
     *
     * @param isDisabled If <code>true</code>, then the <code>timeout</code>
     * parameter is ignored.  If <code>false</code>, then the
     * <code>timeout</code> parameter is used to control uploads.
     */
    public void setDisableUploadTimeout( boolean isDisabled ) {
        disableUploadTimeout = isDisabled;
        setProperty("disableUploadTimeout", String.valueOf(isDisabled));
    }


    /**
     * Return the Keep-Alive policy for the connection.
     */
    public boolean getKeepAlive() {
        return ((maxKeepAliveRequests != 0) && (maxKeepAliveRequests != 1));
    }

    /**
     * Set the keep-alive policy for this connection.
     */
    public void setKeepAlive(boolean keepAlive) {
        if (!keepAlive) {
            setMaxKeepAliveRequests(1);
        }
    }

    /**
     * Return the maximum number of Keep-Alive requests to honor 
     * per connection.
     */
    public int getMaxKeepAliveRequests() {
        return maxKeepAliveRequests;
    }

    /**
     * Set the maximum number of Keep-Alive requests to honor per connection.
     */
    public void setMaxKeepAliveRequests(int mkar) {
        maxKeepAliveRequests = mkar;
        setProperty("maxKeepAliveRequests", String.valueOf(mkar));
    }

    /**
     * Return the scheme that will be assigned to requests received
     * through this connector.  Default value is "http".
     */
    public String getScheme() {

        return (this.scheme);

    }


    /**
     * Set the scheme that will be assigned to requests received through
     * this connector.
     *
     * @param scheme The new scheme
     */
    public void setScheme(String scheme) {

        this.scheme = scheme;
        setProperty("scheme", scheme);

    }


    /**
     * Return the secure connection flag that will be assigned to requests
     * received through this connector.  Default value is "false".
     */
    public boolean getSecure() {

        return (this.secure);

    }


    /**
     * Set the secure connection flag that will be assigned to requests
     * received through this connector.
     *
     * @param secure The new secure connection flag
     */
    public void setSecure(boolean secure) {

        this.secure = secure;
        setProperty("secure", String.valueOf(secure));

    }

    public boolean getTomcatAuthentication() {
        return tomcatAuthentication;
    }

    public void setTomcatAuthentication(boolean tomcatAuthentication) {
        this.tomcatAuthentication = tomcatAuthentication;
        setProperty("tomcatAuthentication", String.valueOf(tomcatAuthentication));
    }
    

    /**
     * Return the TCP no delay flag value.
     */
    public boolean getTcpNoDelay() {

        return (this.tcpNoDelay);

    }


    /**
     * Set the TCP no delay flag which will be set on the socket after
     * accepting a connection.
     *
     * @param tcpNoDelay The new TCP no delay flag
     */
    public void setTcpNoDelay(boolean tcpNoDelay) {

        this.tcpNoDelay = tcpNoDelay;
        setProperty("tcpNoDelay", String.valueOf(tcpNoDelay));

    }


     /**
      * Return the character encoding to be used for the URI.
      */
     public String getURIEncoding() {

         return (this.URIEncoding);

     }


     /**
      * Set the URI encoding to be used for the URI.
      *
      * @param URIEncoding The new URI character encoding.
      */
     public void setURIEncoding(String URIEncoding) {

         this.URIEncoding = URIEncoding;
         setProperty("uRIEncoding", URIEncoding);

     }


     /**
      * Return the true if the entity body encoding should be used for the URI.
      */
     public boolean getUseBodyEncodingForURI() {

         return (this.useBodyEncodingForURI);

     }


     /**
      * Set if the entity body encoding should be used for the URI.
      *
      * @param useBodyEncodingForURI The new value for the flag.
      */
     public void setUseBodyEncodingForURI(boolean useBodyEncodingForURI) {

         this.useBodyEncodingForURI = useBodyEncodingForURI;
         setProperty
             ("useBodyEncodingForURI", String.valueOf(useBodyEncodingForURI));

     }


    /**
     * Indicates whether the generation of an X-Powered-By response header for
     * servlet-generated responses is enabled or disabled for this Connector.
     *
     * @return true if generation of X-Powered-By response header is enabled,
     * false otherwise
     */
    public boolean isXpoweredBy() {
        return xpoweredBy;
    }


    /**
     * Enables or disables the generation of an X-Powered-By header (with value
     * Servlet/2.4) for all servlet-generated responses returned by this
     * Connector.
     *
     * @param xpoweredBy true if generation of X-Powered-By response header is
     * to be enabled, false otherwise
     */
    public void setXpoweredBy(boolean xpoweredBy) {
        this.xpoweredBy = xpoweredBy;
        setProperty("xpoweredBy", String.valueOf(xpoweredBy));
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Create (or allocate) and return a Request object suitable for
     * specifying the contents of a Request to the responsible Container.
     */
    public Request createRequest() {

        CoyoteRequest request = new CoyoteRequest();
        request.setConnector(this);
        return (request);

    }


    /**
     * Create (or allocate) and return a Response object suitable for
     * receiving the contents of a Response from the responsible Container.
     */
    public Response createResponse() {

        CoyoteResponse response = new CoyoteResponse();
        response.setConnector(this);
        return (response);

    }


    // -------------------------------------------------------- Private Methods


    /**
     * Log a message on the Logger associated with our Container (if any).
     *
     * @param message Message to be logged
     */
    private void log(String message) {

        Logger logger = container.getLogger();
        String localName = "CoyoteConnector";
        if (logger != null)
            logger.log(localName + " " + message);
        else
            System.out.println(localName + " " + message);

    }


    /**
     * Log a message on the Logger associated with our Container (if any).
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    private void log(String message, Throwable throwable) {

        Logger logger = container.getLogger();
        String localName = "CoyoteConnector";
        if (logger != null)
            logger.log(localName + " " + message, throwable);
        else {
            System.out.println(localName + " " + message);
            throwable.printStackTrace(System.out);
        }

    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {

        lifecycle.addLifecycleListener(listener);

    }


    /**
     * Get the lifecycle listeners associated with this lifecycle. If this
     * Lifecycle has no listeners registered, a zero-length array is returned.
     */
    public LifecycleListener[] findLifecycleListeners() {

        return null;//lifecycle.findLifecycleListeners();

    }


    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener The listener to add
     */
    public void removeLifecycleListener(LifecycleListener listener) {

        lifecycle.removeLifecycleListener(listener);

    }

    /**
     * Initialize this connector (create ServerSocket here!)
     */
    public void initialize()
        throws LifecycleException
    {
        if (initialized) {
            log.info(sm.getString("coyoteConnector.alreadyInitialized"));
            return;
        }

        this.initialized = true;

        if( oname == null && (container instanceof StandardEngine)) {
            try {
                // we are loaded directly, via API - and no name was given to us
                StandardEngine cb=(StandardEngine)container;
                String encodedAddr = null;
                if (getAddress() != null) {
                    encodedAddr = URLEncoder.encode(getAddress());
                }
                String addSuffix=(getAddress()==null) ?"": ",address=" + encodedAddr;
                oname=new ObjectName(cb.getName() + ":type=Connector,port="+
                        getPort() + addSuffix);
                Registry.getRegistry().registerComponent(this, oname, null);
                controller=oname;
            } catch (Exception e) {
                log.error( "Error registering connector ", e);
            }
            log.debug("Creating name for connector " + oname);
        }

        // Initializa adapter
        adapter = new CoyoteAdapter(this);

        // Instantiate protocol handler
        try {
            Class clazz = Class.forName(protocolHandlerClassName);
            protocolHandler = (ProtocolHandler) clazz.newInstance();
        } catch (Exception e) {
            throw new LifecycleException
                (sm.getString
                 ("coyoteConnector.protocolHandlerInstantiationFailed", e));
        }
        protocolHandler.setAdapter(adapter);

        IntrospectionUtils.setProperty(protocolHandler, "jkHome",
                                       System.getProperty("catalina.base"));

        // Configure secure socket factory
        // XXX For backwards compatibility only.
        if (factory instanceof CoyoteServerSocketFactory) {
            IntrospectionUtils.setProperty(protocolHandler, "secure",
                                           "" + true);
            CoyoteServerSocketFactory ssf =
                (CoyoteServerSocketFactory) factory;
            IntrospectionUtils.setProperty(protocolHandler, "algorithm",
                                           ssf.getAlgorithm());
            if (ssf.getClientAuth()) {
                IntrospectionUtils.setProperty(protocolHandler, "clientauth",
                                               "" + ssf.getClientAuth());
            }
            IntrospectionUtils.setProperty(protocolHandler, "keystore",
                                           ssf.getKeystoreFile());
            IntrospectionUtils.setProperty(protocolHandler, "randomfile",
                                           ssf.getRandomFile());
            IntrospectionUtils.setProperty(protocolHandler, "rootfile",
                                           ssf.getRootFile());

            IntrospectionUtils.setProperty(protocolHandler, "keypass",
                                           ssf.getKeystorePass());
            IntrospectionUtils.setProperty(protocolHandler, "keytype",
                                           ssf.getKeystoreType());
            IntrospectionUtils.setProperty(protocolHandler, "protocol",
                                           ssf.getProtocol());
            IntrospectionUtils.setProperty(protocolHandler, "protocols",
                                           ssf.getProtocols());
            IntrospectionUtils.setProperty(protocolHandler,
                                           "sSLImplementation",
                                           ssf.getSSLImplementation());
            IntrospectionUtils.setProperty(protocolHandler, "ciphers",
                                           ssf.getCiphers());
            IntrospectionUtils.setProperty(protocolHandler, "keyAlias",
                                           ssf.getKeyAlias());
        } else {
            IntrospectionUtils.setProperty(protocolHandler, "secure",
                                           "" + secure);
        }

        /* Set the configured properties.  This only sets the ones that were
         * explicitly configured.  Default values are the responsibility of
         * the protocolHandler.
         */
        Iterator keys = properties.keySet().iterator();
        while( keys.hasNext() ) {
            String name = (String)keys.next();
            String value = properties.get(name).toString();
	    String trnName = translateAttributeName(name);
            IntrospectionUtils.setProperty(protocolHandler, trnName, value);
        }
        

        try {
            protocolHandler.init();
        } catch (Exception e) {
            throw new LifecycleException
                (sm.getString
                 ("coyoteConnector.protocolHandlerInitializationFailed", e));
        }
    }

    /*
     * Translate the attribute name from the legacy Factory names to their
     * internal protocol names.
     */
    private String translateAttributeName(String name) {
	if ("clientAuth".equals(name)) {
	    return "clientauth";
	} else if ("keystoreFile".equals(name)) {
	    return "keystore";
	} else if ("randomFile".equals(name)) {
	    return "randomfile";
	} else if ("rootFile".equals(name)) {
	    return "rootfile";
	} else if ("keystorePass".equals(name)) {
	    return "keypass";
	} else if ("keystoreType".equals(name)) {
	    return "keytype";
	} else if ("sslProtocol".equals(name)) {
	    return "protocol";
	} else if ("sslProtocols".equals(name)) {
	    return "protocols";
	}
	return name;
    }


    /**
     * Begin processing requests via this Connector.
     *
     * @exception LifecycleException if a fatal startup error occurs
     */
    public void start() throws LifecycleException {
        if( !initialized )
            initialize();

        // Validate and update our current state
        if (started) {
            log.info(sm.getString("coyoteConnector.alreadyStarted"));
            return;
        }
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        // We can't register earlier - the JMX registration of this happens
        // in Server.start callback
        if ( this.oname != null ) {
            // We are registred - register the adapter as well.
            try {
                Registry.getRegistry().registerComponent
                    (protocolHandler, this.domain, "protocolHandler",
                     "type=protocolHandler,className="
                     + protocolHandlerClassName);
            } catch (Exception ex) {
                log.error(sm.getString
                          ("coyoteConnector.protocolRegistrationFailed"), ex);
            }
        } else {
            log.info(sm.getString
                     ("coyoteConnector.cannotRegisterProtocol"));
        }

        try {
            protocolHandler.start();
        } catch (Exception e) {
            throw new LifecycleException
                (sm.getString
                 ("coyoteConnector.protocolHandlerStartFailed", e));
        }

        if( this.domain != null ) {
            mapperListener.setDomain( domain );
            //mapperListener.setEngine( service.getContainer().getName() );
            mapperListener.init();
            try {
                Registry.getRegistry().registerComponent
                        (mapper, this.domain, "Mapper",
                                "type=Mapper");
            } catch (Exception ex) {
                log.error(sm.getString
                        ("coyoteConnector.protocolRegistrationFailed"), ex);
            }
        }
    }


    /**
     * Terminate processing requests via this Connector.
     *
     * @exception LifecycleException if a fatal shutdown error occurs
     */
    public void stop() throws LifecycleException {

        // Validate and update our current state
        if (!started) {
            log.error(sm.getString("coyoteConnector.notStarted"));
            return;

        }
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        try {
            Registry.getRegistry().unregisterComponent(new ObjectName(domain,"type", "Mapper"));
            Registry.getRegistry().unregisterComponent(new ObjectName(domain
                    + ":type=protocolHandler,className="
                    + protocolHandlerClassName));
        } catch (MalformedObjectNameException e) {
            log.info( "Error unregistering mapper ", e);
        }
        try {
            protocolHandler.destroy();
        } catch (Exception e) {
            throw new LifecycleException
                (sm.getString
                 ("coyoteConnector.protocolHandlerDestroyFailed", e));
        }

    }

    // -------------------- Management methods --------------------

    
    public boolean getClientAuth() {
        boolean ret = false;

        String prop = (String) getProperty("clientauth");
        if (prop != null) {
            ret = Boolean.valueOf(prop).booleanValue();
        } else {	
            ServerSocketFactory factory = this.getFactory();
            if (factory instanceof CoyoteServerSocketFactory) {
                ret = ((CoyoteServerSocketFactory)factory).getClientAuth();
            }
        }

        return ret;
    }

    public void setClientAuth(boolean clientAuth) {
        setProperty("clientauth", String.valueOf(clientAuth));
        ServerSocketFactory factory = this.getFactory();
        if (factory instanceof CoyoteServerSocketFactory) {
            ((CoyoteServerSocketFactory)factory).setClientAuth(clientAuth);
        }
    }


    public String getKeystoreFile() {
        String ret = (String) getProperty("keystore");
        if (ret == null) {
            ServerSocketFactory factory = this.getFactory();
            if (factory instanceof CoyoteServerSocketFactory) {
                ret = ((CoyoteServerSocketFactory)factory).getKeystoreFile();
            }
        }

        return ret;
    }

    public void setKeystoreFile(String keystoreFile) {
        setProperty("keystore", keystoreFile);
        ServerSocketFactory factory = this.getFactory();
        if (factory instanceof CoyoteServerSocketFactory) {
            ((CoyoteServerSocketFactory)factory).setKeystoreFile(keystoreFile);
        }
    }

    /**
     * Return keystorePass
     */
    public String getKeystorePass() {
        String ret = (String) getProperty("keypass");
        if (ret == null) {
            ServerSocketFactory factory = getFactory();
            if (factory instanceof CoyoteServerSocketFactory ) {
                return ((CoyoteServerSocketFactory)factory).getKeystorePass();
            }
        }

        return ret;
    }

    /**
     * Set keystorePass
     */
    public void setKeystorePass(String keystorePass) {
        setProperty("keypass", keystorePass);
        ServerSocketFactory factory = getFactory();
        if( factory instanceof CoyoteServerSocketFactory ) {
            ((CoyoteServerSocketFactory)factory).setKeystorePass(keystorePass);
        }
    }
    
    /**
     * Gets the list of SSL cipher suites that are to be enabled
     *
     * @return Comma-separated list of SSL cipher suites, or null if all
     * cipher suites supported by the underlying SSL implementation are being
     * enabled
     */
    public String getCiphers() {
        String ret = (String) getProperty("ciphers");
        if (ret == null) {
            ServerSocketFactory factory = getFactory();
            if (factory instanceof CoyoteServerSocketFactory) {
                ret = ((CoyoteServerSocketFactory)factory).getCiphers();
            }
        }

        return ret;
    }

    /**
     * Sets the SSL cipher suites that are to be enabled.
     *
     * Only those SSL cipher suites that are actually supported by
     * the underlying SSL implementation will be enabled.
     *
     * @param ciphers Comma-separated list of SSL cipher suites
     */
    public void setCiphers(String ciphers) {
        setProperty("ciphers", ciphers);
        ServerSocketFactory factory = getFactory();
        if (factory instanceof CoyoteServerSocketFactory) {
            ((CoyoteServerSocketFactory)factory).setCiphers(ciphers);
        }
    }

    /**
     * Gets the alias name of the keypair and supporting certificate chain
     * used by this Connector to authenticate itself to SSL clients.
     *
     * @return The alias name of the keypair and supporting certificate chain
     */
    public String getKeyAlias() {
        String ret = (String) getProperty("keyAlias");
        if (ret == null) {
            ServerSocketFactory factory = getFactory();
            if (factory instanceof CoyoteServerSocketFactory) {
                ret = ((CoyoteServerSocketFactory)factory).getKeyAlias();
            }
        }

        return ret;
    }

    /**
     * Sets the alias name of the keypair and supporting certificate chain
     * used by this Connector to authenticate itself to SSL clients.
     *
     * @param alias The alias name of the keypair and supporting certificate
     * chain
     */
    public void setKeyAlias(String alias) {
        setProperty("keyAlias", alias);
        ServerSocketFactory factory = getFactory();
        if (factory instanceof CoyoteServerSocketFactory) {
            ((CoyoteServerSocketFactory)factory).setKeyAlias(alias);
        }
    }

    /**
     * Gets the SSL protocol variant to be used.
     *
     * @return SSL protocol variant
     */
    public String getSslProtocol() {
        String ret = (String) getProperty("sslProtocol");
        if (ret == null) {
            ServerSocketFactory factory = getFactory();
            if (factory instanceof CoyoteServerSocketFactory) {
                ret = ((CoyoteServerSocketFactory)factory).getProtocol();
            }
        }

        return ret;
    }

    /**
     * Sets the SSL protocol variant to be used.
     *
     * @param sslProtocol SSL protocol variant
     */
    public void setSslProtocol(String sslProtocol) {
        setProperty("sslProtocol", sslProtocol);
        ServerSocketFactory factory = getFactory();
        if (factory instanceof CoyoteServerSocketFactory) {
            ((CoyoteServerSocketFactory)factory).setProtocol(sslProtocol);
        }
    }

    /**
     * Gets the SSL protocol variants to be enabled.
     *
     * @return Comma-separated list of SSL protocol variants
     */
    public String getSslProtocols() {
        String ret = (String) getProperty("sslProtocols");
        if (ret == null) {
            ServerSocketFactory factory = getFactory();
            if (factory instanceof CoyoteServerSocketFactory) {
                ret = ((CoyoteServerSocketFactory)factory).getProtocols();
            }
        }

        return ret;
    }

    /**
     * Sets the SSL protocol variants to be enabled.
     *
     * @param sslProtocols Comma-separated list of SSL protocol variants
     */
    public void setSslProtocols(String sslProtocols) {
        setProperty("sslProtocols", sslProtocols);
        ServerSocketFactory factory = getFactory();
        if (factory instanceof CoyoteServerSocketFactory) {
            ((CoyoteServerSocketFactory)factory).setProtocols(sslProtocols);
        }
    }


    // -------------------- JMX registration  --------------------
    protected String domain;
    protected ObjectName oname;
    protected MBeanServer mserver;
    ObjectName controller;

    public ObjectName getController() {
        return controller;
    }

    public void setController(ObjectName controller) {
        this.controller = controller;
    }

    public ObjectName getObjectName() {
        return oname;
    }

    public String getDomain() {
        return domain;
    }

    public ObjectName preRegister(MBeanServer server,
                                  ObjectName name) throws Exception {
        oname=name;
        mserver=server;
        domain=name.getDomain();
        return name;
    }

    public void postRegister(Boolean registrationDone) {
    }

    public void preDeregister() throws Exception {
    }

    public void postDeregister() {
        try {
            if( started ) {
                stop();
            }
        } catch( Throwable t ) {
            log.error( "Unregistering - can't stop", t);
        }
    }
    
    private void findContainer() {
        try {
            // Register to the service
            ObjectName parentName=new ObjectName( domain + ":" +
                    "type=Service");
            
            log.debug("Adding to " + parentName );
            if( mserver.isRegistered(parentName )) {
                mserver.invoke(parentName, "addConnector", new Object[] { this },
                        new String[] {"org.apache.catalina.Connector"});
                // As a side effect we'll get the container field set
                // Also initialize will be called
                //return;
            }
            // XXX Go directly to the Engine
            // initialize(); - is called by addConnector
            ObjectName engName=new ObjectName( domain + ":" + "type=Engine");
            if( mserver.isRegistered(engName )) {
                Object obj=mserver.getAttribute(engName, "managedResource");
                log.debug("Found engine " + obj + " " + obj.getClass());
                container=(Container)obj;
                
                // Internal initialize - we now have the Engine
                initialize();
                
                log.debug("Initialized");
                // As a side effect we'll get the container field set
                // Also initialize will be called
                return;
            }
        } catch( Exception ex ) {
            log.error( "Error finding container " + ex);
        }
    }

    public void init() throws Exception {

        if( this.getService() != null ) {
            log.debug( "Already configured" );
            return;
        }
        if( container==null ) {
            findContainer();
        }
    }

    public void destroy() throws Exception {
        if( oname!=null && controller==oname ) {
            log.debug("Unregister itself " + oname );
            Registry.getRegistry().unregisterComponent(oname);
        }
        if( getService() == null)
            return;
        getService().removeConnector(this);
    }

}
