/*
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


package org.apache.catalina.core;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Stack;
import java.util.TreeMap;

import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.InstanceListener;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Server;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.Wrapper;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.deploy.ContextEjb;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextLocalEjb;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.ContextResourceLink;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.MessageDestination;
import org.apache.catalina.deploy.MessageDestinationRef;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.deploy.ResourceParams;
import org.apache.catalina.deploy.SecurityCollection;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.mbeans.MBeanUtils;
import org.apache.catalina.session.PersistentManagerBase;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.TldConfig;
import org.apache.catalina.util.CharsetMapper;
import org.apache.catalina.util.ExtensionValidator;
import org.apache.catalina.util.RequestUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.modeler.ManagedBean;
import org.apache.commons.modeler.Registry;
import org.apache.naming.ContextBindings;
import org.apache.naming.resources.BaseDirContext;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.apache.naming.resources.FileDirContext;
import org.apache.naming.resources.ProxyDirContext;
import org.apache.naming.resources.WARDirContext;

/**
 * Standard implementation of the <b>Context</b> interface.  Each
 * child container must be a Wrapper implementation to process the
 * requests directed to a particular servlet.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.108 $ $Date: 2004/01/13 17:01:48 $
 */

public class StandardContext
    extends ContainerBase
    implements Context, Serializable
{
    private transient Log log = LogFactory.getLog(StandardContext.class);


    // ----------------------------------------------------------- Constructors


    /**
     * Create a new StandardContext component with the default basic Valve.
     */
    public StandardContext() {

        super();
        pipeline.setBasic(new StandardContextValve());
        namingResources.setContainer(this);
        broadcaster = new NotificationBroadcasterSupport();

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The alternate deployment descriptor name.
     */
    private String altDDName = null;


    /**
     * Associated host name.
     */
    private String hostName;


    /**
     * The set of application listener class names configured for this
     * application, in the order they were encountered in the web.xml file.
     */
    private String applicationListeners[] = new String[0];


    /**
     * The set of instantiated application event listener objects</code>.
     */
    private transient Object applicationEventListenersObjects[] = 
        new Object[0];


    /**
     * The set of instantiated application lifecycle listener objects</code>.
     */
    private transient Object applicationLifecycleListenersObjects[] = 
        new Object[0];


    /**
     * The set of application parameters defined for this application.
     */
    private ApplicationParameter applicationParameters[] =
        new ApplicationParameter[0];


    /**
     * The application available flag for this Context.
     */
    private boolean available = false;
    
    /**
     * The broadcaster that sends j2ee notifications. 
     */
    private NotificationBroadcasterSupport broadcaster = null;
    
    /**
     * The Locale to character set mapper for this application.
     */
    private transient CharsetMapper charsetMapper = null;


    /**
     * The Java class name of the CharsetMapper class to be created.
     */
    private String charsetMapperClass =
      "org.apache.catalina.util.CharsetMapper";


    /**
     * The path to a file to save this Context information.
     */
    private String configFile = null;


    /**
     * The "correctly configured" flag for this Context.
     */
    private boolean configured = false;


    /**
     * The security constraints for this web application.
     */
    private SecurityConstraint constraints[] = new SecurityConstraint[0];


    /**
     * The ServletContext implementation associated with this Context.
     */
    private transient ApplicationContext context = null;


    /**
     * Compiler classpath to use.
     */
    private String compilerClasspath = null;


    /**
     * Should we attempt to use cookies for session id communication?
     */
    private boolean cookies = true;


    /**
     * Should we allow the <code>ServletContext.getContext()</code> method
     * to access the context of other web applications in this server?
     */
    private boolean crossContext = false;


    /**
     * The "follow standard delegation model" flag that will be used to
     * configure our ClassLoader.
     */
    private boolean delegate = false;


     /**
     * The display name of this web application.
     */
    private String displayName = null;


    /** 
     * Override the default web xml location. ContextConfig is not configurable
     * so the setter is not used.
     */
    private String defaultWebXml;


    /**
     * The distributable flag for this web application.
     */
    private boolean distributable = false;


    /**
     * The document root for this web application.
     */
    private String docBase = null;


    /**
     * The exception pages for this web application, keyed by fully qualified
     * class name of the Java exception.
     */
    private HashMap exceptionPages = new HashMap();


    /**
     * The set of filter configurations (and associated filter instances) we
     * have initialized, keyed by filter name.
     */
    private HashMap filterConfigs = new HashMap();


    /**
     * The set of filter definitions for this application, keyed by
     * filter name.
     */
    private HashMap filterDefs = new HashMap();


    /**
     * The set of filter mappings for this application, in the order
     * they were defined in the deployment descriptor.
     */
    private FilterMap filterMaps[] = new FilterMap[0];


    /**
     * The descriptive information string for this implementation.
     */
    private static final String info =
        "org.apache.catalina.core.StandardContext/1.0";


    /**
     * The set of classnames of InstanceListeners that will be added
     * to each newly created Wrapper by <code>createWrapper()</code>.
     */
    private String instanceListeners[] = new String[0];


    /**
     * The login configuration descriptor for this web application.
     */
    private LoginConfig loginConfig = null;


    /**
     * The mapper associated with this context.
     */
    private org.apache.tomcat.util.http.mapper.Mapper mapper = 
        new org.apache.tomcat.util.http.mapper.Mapper();


    /**
     * The naming context listener for this web application.
     */
    private transient NamingContextListener namingContextListener = null;


    /**
     * The naming resources for this web application.
     */
    private NamingResources namingResources = new NamingResources();


    /**
     * The message destinations for this web application.
     */
    private HashMap messageDestinations = new HashMap();


    /**
     * The MIME mappings for this web application, keyed by extension.
     */
    private HashMap mimeMappings = new HashMap();


     /**
      * Special case: error page for status 200.
      */
     private ErrorPage okErrorPage = null;


    /**
     * The context initialization parameters for this web application,
     * keyed by name.
     */
    private HashMap parameters = new HashMap();


    /**
     * The request processing pause flag (while reloading occurs)
     */
    private boolean paused = false;


    /**
     * The public identifier of the DTD for the web application deployment
     * descriptor version we are currently parsing.  This is used to support
     * relaxed validation rules when processing version 2.2 web.xml files.
     */
    private String publicId = null;


    /**
     * The reloadable flag for this web application.
     */
    private boolean reloadable = false;


    /**
     * Unpack WAR property.
     */
    private boolean unpackWAR = true;


    /**
     * The DefaultContext override flag for this web application.
     */
    private boolean override = false;


    /**
     * The privileged flag for this web application.
     */
    private boolean privileged = false;


    /**
     * Should the next call to <code>addWelcomeFile()</code> cause replacement
     * of any existing welcome files?  This will be set before processing the
     * web application's deployment descriptor, so that application specified
     * choices <strong>replace</strong>, rather than append to, those defined
     * in the global descriptor.
     */
    private boolean replaceWelcomeFiles = false;


    /**
     * The security role mappings for this application, keyed by role
     * name (as used within the application).
     */
    private HashMap roleMappings = new HashMap();


    /**
     * The security roles for this application, keyed by role name.
     */
    private String securityRoles[] = new String[0];


    /**
     * The servlet mappings for this web application, keyed by
     * matching pattern.
     */
    private HashMap servletMappings = new HashMap();


    /**
     * The session timeout (in minutes) for this web application.
     */
    private int sessionTimeout = 30;

    /**
     * The notification sequence number.
     */
    private long sequenceNumber = 0;
    
    /**
     * The status code error pages for this web application, keyed by
     * HTTP status code (as an Integer).
     */
    private HashMap statusPages = new HashMap();


    /**
     * Set flag to true to cause the system.out and system.err to be redirected
     * to the logger when executing a servlet.
     */
    private boolean swallowOutput = false;


    /**
     * The JSP tag libraries for this web application, keyed by URI
     */
    private HashMap taglibs = new HashMap();


    /**
     * The welcome files for this application.
     */
    private String welcomeFiles[] = new String[0];


    /**
     * The set of classnames of LifecycleListeners that will be added
     * to each newly created Wrapper by <code>createWrapper()</code>.
     */
    private String wrapperLifecycles[] = new String[0];


    /**
     * The set of classnames of ContainerListeners that will be added
     * to each newly created Wrapper by <code>createWrapper()</code>.
     */
    private String wrapperListeners[] = new String[0];


    /**
     * The pathname to the work directory for this context (relative to
     * the server's home if not absolute).
     */
    private String workDir = null;


    /**
     * Java class name of the Wrapper class implementation we use.
     */
    private String wrapperClass = "org.apache.catalina.core.StandardWrapper";


    /**
     * JNDI use flag.
     */
    private boolean useNaming = true;


    /**
     * Filesystem based flag.
     */
    private boolean filesystemBased = false;


    /**
     * Name of the associated naming context.
     */
    private String namingContextName = null;


    /**
     * Frequency of the session expiration, and related manager operations.
     * Manager operations will be done once for the specified amount of
     * backgrondProcess calls (ie, the lower the amount, the most often the
     * checks will occur).
     */
    private int managerChecksFrequency = 6;


    /**
     * Iteration count for background processing.
     */
    private int count = 0;


    /**
     * Caching allowed flag.
     */
    private boolean cachingAllowed = true;


    /**
     * Case sensitivity.
     */
    protected boolean caseSensitive = true;


    /**
     * Allow linking.
     */
    protected boolean allowLinking = false;


    /**
     * Cache max size in KB.
     */
    protected int cacheMaxSize = 10240; // 10 MB


    /**
     * Cache TTL in ms.
     */
    protected int cacheTTL = 5000;


    private boolean lazy=true;

    /**
     * Non proxied resources.
     */
    private transient DirContext webappResources = null;

    private ArrayList wrappers=new ArrayList();

    private long startupTime;
    private long startTime;
    private long tldScanTime;

    /** Name of the engine. If null, the domain is used.
     */ 
    private String engineName = null;
    private String j2EEApplication="none";
    private String j2EEServer="none";

    // ----------------------------------------------------- Context Properties

    public void setName( String name ) {
        super.setName( name );
    }


    /**
     * Is caching allowed ?
     */
    public boolean isCachingAllowed() {
        return cachingAllowed;
    }


    /**
     * Set caching allowed flag.
     */
    public void setCachingAllowed(boolean cachingAllowed) {
        this.cachingAllowed = cachingAllowed;
    }


    /**
     * Set case sensitivity.
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }


    /**
     * Is case sensitive ?
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }


    /**
     * Set allow linking.
     */
    public void setAllowLinking(boolean allowLinking) {
        this.allowLinking = allowLinking;
    }


    /**
     * Is linking allowed.
     */
    public boolean isAllowLinking() {
        return allowLinking;
    }


    /**
     * Set cache TTL.
     */
    public void setCacheTTL(int cacheTTL) {
        this.cacheTTL = cacheTTL;
    }


    /**
     * Get cache TTL.
     */
    public int getCacheTTL() {
        return cacheTTL;
    }


    /**
     * Return the maximum size of the cache in KB.
     */
    public int getCacheMaxSize() {
        return cacheMaxSize;
    }


    /**
     * Set the maximum size of the cache in KB.
     */
    public void setCacheMaxSize(int cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }


    /**
     * Return the "follow standard delegation model" flag used to configure
     * our ClassLoader.
     */
    public boolean getDelegate() {

        return (this.delegate);

    }


    /**
     * Set the "follow standard delegation model" flag used to configure
     * our ClassLoader.
     *
     * @param delegate The new flag
     */
    public void setDelegate(boolean delegate) {

        boolean oldDelegate = this.delegate;
        this.delegate = delegate;
        support.firePropertyChange("delegate", new Boolean(oldDelegate),
                                   new Boolean(this.delegate));

    }


    /**
     * Returns true if the internal naming support is used.
     */
    public boolean isUseNaming() {

        return (useNaming);

    }


    /**
     * Enables or disables naming.
     */
    public void setUseNaming(boolean useNaming) {
        this.useNaming = useNaming;
    }


    /**
     * Returns true if the resources associated with this context are
     * filesystem based.
     */
    public boolean isFilesystemBased() {

        return (filesystemBased);

    }


    /**
     * Return the set of initialized application event listener objects,
     * in the order they were specified in the web application deployment
     * descriptor, for this application.
     *
     * @exception IllegalStateException if this method is called before
     *  this application has started, or after it has been stopped
     */
    public Object[] getApplicationEventListeners() {
        return (applicationEventListenersObjects);
    }


    /**
     * Store the set of initialized application event listener objects,
     * in the order they were specified in the web application deployment
     * descriptor, for this application.
     *
     * @param listeners The set of instantiated listener objects.
     */
    public void setApplicationEventListeners(Object listeners[]) {
        applicationEventListenersObjects = listeners;
    }


    /**
     * Return the set of initialized application lifecycle listener objects,
     * in the order they were specified in the web application deployment
     * descriptor, for this application.
     *
     * @exception IllegalStateException if this method is called before
     *  this application has started, or after it has been stopped
     */
    public Object[] getApplicationLifecycleListeners() {
        return (applicationLifecycleListenersObjects);
    }


    /**
     * Store the set of initialized application lifecycle listener objects,
     * in the order they were specified in the web application deployment
     * descriptor, for this application.
     *
     * @param listeners The set of instantiated listener objects.
     */
    public void setApplicationLifecycleListeners(Object listeners[]) {
        applicationLifecycleListenersObjects = listeners;
    }


    /**
     * Return the application available flag for this Context.
     */
    public boolean getAvailable() {

        return (this.available);

    }


    /**
     * Set the application available flag for this Context.
     *
     * @param available The new application available flag
     */
    public void setAvailable(boolean available) {

        boolean oldAvailable = this.available;
        this.available = available;
        support.firePropertyChange("available",
                                   new Boolean(oldAvailable),
                                   new Boolean(this.available));

    }


    /**
     * Return the Locale to character set mapper for this Context.
     */
    public CharsetMapper getCharsetMapper() {

        // Create a mapper the first time it is requested
        if (this.charsetMapper == null) {
            try {
                Class clazz = Class.forName(charsetMapperClass);
                this.charsetMapper =
                  (CharsetMapper) clazz.newInstance();
            } catch (Throwable t) {
                this.charsetMapper = new CharsetMapper();
            }
        }

        return (this.charsetMapper);

    }


    /**
     * Set the Locale to character set mapper for this Context.
     *
     * @param mapper The new mapper
     */
    public void setCharsetMapper(CharsetMapper mapper) {

        CharsetMapper oldCharsetMapper = this.charsetMapper;
        this.charsetMapper = mapper;
        if( mapper != null )
            this.charsetMapperClass= mapper.getClass().getName();
        support.firePropertyChange("charsetMapper", oldCharsetMapper,
                                   this.charsetMapper);

    }

    /**
     * Return the path to a file to save this Context information.
     */
    public String getConfigFile() {

        return (this.configFile);

    }


    /**
     * Set the path to a file to save this Context information.
     *
     * @param configFile The path to a file to save this Context information.
     */
    public void setConfigFile(String configFile) {

        this.configFile = configFile;
    }


    /**
     * Return the "correctly configured" flag for this Context.
     */
    public boolean getConfigured() {

        return (this.configured);

    }


    /**
     * Set the "correctly configured" flag for this Context.  This can be
     * set to false by startup listeners that detect a fatal configuration
     * error to avoid the application from being made available.
     *
     * @param configured The new correctly configured flag
     */
    public void setConfigured(boolean configured) {

        boolean oldConfigured = this.configured;
        this.configured = configured;
        support.firePropertyChange("configured",
                                   new Boolean(oldConfigured),
                                   new Boolean(this.configured));

    }


    /**
     * Return the "use cookies for session ids" flag.
     */
    public boolean getCookies() {

        return (this.cookies);

    }


    /**
     * Set the "use cookies for session ids" flag.
     *
     * @param cookies The new flag
     */
    public void setCookies(boolean cookies) {

        boolean oldCookies = this.cookies;
        this.cookies = cookies;
        support.firePropertyChange("cookies",
                                   new Boolean(oldCookies),
                                   new Boolean(this.cookies));

    }


    /**
     * Return the "allow crossing servlet contexts" flag.
     */
    public boolean getCrossContext() {

        return (this.crossContext);

    }


    /**
     * Set the "allow crossing servlet contexts" flag.
     *
     * @param crossContext The new cross contexts flag
     */
    public void setCrossContext(boolean crossContext) {

        boolean oldCrossContext = this.crossContext;
        this.crossContext = crossContext;
        support.firePropertyChange("crossContext",
                                   new Boolean(oldCrossContext),
                                   new Boolean(this.crossContext));

    }

    public String getDefaultWebXml() {
        return defaultWebXml;
    }

    /** Set the location of the default web xml that will be used.
     * If not absolute, it'll be made relative to the engine's base dir
     * ( which defaults to catalina.base system property ).
     *
     * XXX If a file is not found - we can attempt a getResource()
     *
     * @param defaultWebXml
     */
    public void setDefaultWebXml(String defaultWebXml) {
        this.defaultWebXml = defaultWebXml;
    }

    public long getStartupTime() {
        return startupTime;
    }

    public void setStartupTime(long startupTime) {
        this.startupTime = startupTime;
    }

    public long getTldScanTime() {
        return tldScanTime;
    }

    public void setTldScanTime(long tldScanTime) {
        this.tldScanTime = tldScanTime;
    }

    /**
     * Return the display name of this web application.
     */
    public String getDisplayName() {

        return (this.displayName);

    }


    /**
     * Return the alternate Deployment Descriptor name.
     */
    public String getAltDDName(){
        return altDDName;
    }


    /**
     * Set an alternate Deployment Descriptor name.
     */
    public void setAltDDName(String altDDName) {
        this.altDDName = altDDName;
        if (context != null) {
            context.setAttribute(Globals.ALT_DD_ATTR,altDDName);
        }
    }


    /**
     * Return the compiler classpath.
     */
    public String getCompilerClasspath(){
        return compilerClasspath;
    }


    /**
     * Set the compiler classpath.
     */
    public void setCompilerClasspath(String compilerClasspath) {
        this.compilerClasspath = compilerClasspath;
    }


    /**
     * Set the display name of this web application.
     *
     * @param displayName The new display name
     */
    public void setDisplayName(String displayName) {

        String oldDisplayName = this.displayName;
        this.displayName = displayName;
        support.firePropertyChange("displayName", oldDisplayName,
                                   this.displayName);
    }


    /**
     * Return the distributable flag for this web application.
     */
    public boolean getDistributable() {

        return (this.distributable);

    }

    /**
     * Set the distributable flag for this web application.
     *
     * @param distributable The new distributable flag
     */
    public void setDistributable(boolean distributable) {
        boolean oldDistributable = this.distributable;
        this.distributable = distributable;
        support.firePropertyChange("distributable",
                                   new Boolean(oldDistributable),
                                   new Boolean(this.distributable));
    }


    /**
     * Return the document root for this Context.  This can be an absolute
     * pathname, a relative pathname, or a URL.
     */
    public String getDocBase() {

        return (this.docBase);

    }


    /**
     * Set the document root for this Context.  This can be an absolute
     * pathname, a relative pathname, or a URL.
     *
     * @param docBase The new document root
     */
    public void setDocBase(String docBase) {

        this.docBase = docBase;

    }

    // experimental
    public boolean isLazy() {
        return lazy;
    }

    public void setLazy(boolean lazy) {
        this.lazy = lazy;
    }


    /**
     * Return the frequency of manager checks.
     */
    public int getManagerChecksFrequency() {

        return (this.managerChecksFrequency);

    }


    /**
     * Set the manager checks frequency.
     *
     * @param managerChecksFrequency the new manager checks frequency
     */
    public void setManagerChecksFrequency(int managerChecksFrequency) {

        if (managerChecksFrequency <= 0) {
            return;
        }

        int oldManagerChecksFrequency = this.managerChecksFrequency;
        this.managerChecksFrequency = managerChecksFrequency;
        support.firePropertyChange("managerChecksFrequency",
                                   new Integer(oldManagerChecksFrequency),
                                   new Integer(this.managerChecksFrequency));

    }


    /**
     * Return descriptive information about this Container implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {

        return (info);

    }

    public String getEngineName() {
        if( engineName != null ) return engineName;
        return domain;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public String getJ2EEApplication() {
        return j2EEApplication;
    }

    public void setJ2EEApplication(String j2EEApplication) {
        this.j2EEApplication = j2EEApplication;
    }

    public String getJ2EEServer() {
        return j2EEServer;
    }

    public void setJ2EEServer(String j2EEServer) {
        this.j2EEServer = j2EEServer;
    }


    /**
     * Set the Loader with which this Context is associated.
     *
     * @param loader The newly associated loader
     */
    public synchronized void setLoader(Loader loader) {

        super.setLoader(loader);

    }


    /**
     * Return the login configuration descriptor for this web application.
     */
    public LoginConfig getLoginConfig() {

        return (this.loginConfig);

    }


    /**
     * Set the login configuration descriptor for this web application.
     *
     * @param config The new login configuration
     */
    public void setLoginConfig(LoginConfig config) {

        // Validate the incoming property value
        if (config == null)
            throw new IllegalArgumentException
                (sm.getString("standardContext.loginConfig.required"));
        String loginPage = config.getLoginPage();
        if ((loginPage != null) && !loginPage.startsWith("/")) {
            if (isServlet22()) {
                log.debug(sm.getString("standardContext.loginConfig.loginWarning",
                                 loginPage));
                config.setLoginPage("/" + loginPage);
            } else {
                throw new IllegalArgumentException
                    (sm.getString("standardContext.loginConfig.loginPage",
                                  loginPage));
            }
        }
        String errorPage = config.getErrorPage();
        if ((errorPage != null) && !errorPage.startsWith("/")) {
            if (isServlet22()) {
                log.debug(sm.getString("standardContext.loginConfig.errorWarning",
                                 errorPage));
                config.setErrorPage("/" + errorPage);
            } else {
                throw new IllegalArgumentException
                    (sm.getString("standardContext.loginConfig.errorPage",
                                  errorPage));
            }
        }

        // Process the property setting change
        LoginConfig oldLoginConfig = this.loginConfig;
        this.loginConfig = config;
        support.firePropertyChange("loginConfig",
                                   oldLoginConfig, this.loginConfig);

    }


    /**
     * Get the mapper associated with the context.
     */
    public org.apache.tomcat.util.http.mapper.Mapper getMapper() {
        return (mapper);
    }


    /**
     * Return the naming resources associated with this web application.
     */
    public NamingResources getNamingResources() {

        return (this.namingResources);

    }


    /**
     * Set the naming resources for this web application.
     *
     * @param namingResources The new naming resources
     */
    public void setNamingResources(NamingResources namingResources) {

        // Process the property setting change
        NamingResources oldNamingResources = this.namingResources;
        this.namingResources = namingResources;
        support.firePropertyChange("namingResources",
                                   oldNamingResources, this.namingResources);

    }


    /**
     * Return the context path for this Context.
     */
    public String getPath() {

        return (getName());

    }

    
    /**
     * Set the context path for this Context.
     * <p>
     * <b>IMPLEMENTATION NOTE</b>:  The context path is used as the "name" of
     * a Context, because it must be unique.
     *
     * @param path The new context path
     */
    public void setPath(String path) {
        // XXX Use host in name
        setName(RequestUtil.URLDecode(path));

    }


    /**
     * Return the public identifier of the deployment descriptor DTD that is
     * currently being parsed.
     */
    public String getPublicId() {

        return (this.publicId);

    }


    /**
     * Set the public identifier of the deployment descriptor DTD that is
     * currently being parsed.
     *
     * @param publicId The public identifier
     */
    public void setPublicId(String publicId) {

        if (log.isDebugEnabled())
            log.debug("Setting deployment descriptor public ID to '" +
                publicId + "'");

        String oldPublicId = this.publicId;
        this.publicId = publicId;
        support.firePropertyChange("publicId", oldPublicId, publicId);

    }


    /**
     * Return the reloadable flag for this web application.
     */
    public boolean getReloadable() {

        return (this.reloadable);

    }


    /**
     * Return the DefaultContext override flag for this web application.
     */
    public boolean getOverride() {

        return (this.override);

    }


    /**
     * Return the privileged flag for this web application.
     */
    public boolean getPrivileged() {

        return (this.privileged);

    }


    /**
     * Set the privileged flag for this web application.
     *
     * @param privileged The new privileged flag
     */
    public void setPrivileged(boolean privileged) {

        boolean oldPrivileged = this.privileged;
        this.privileged = privileged;
        support.firePropertyChange("privileged",
                                   new Boolean(oldPrivileged),
                                   new Boolean(this.privileged));

    }


    /**
     * Set the reloadable flag for this web application.
     *
     * @param reloadable The new reloadable flag
     */
    public void setReloadable(boolean reloadable) {

        boolean oldReloadable = this.reloadable;
        this.reloadable = reloadable;
        support.firePropertyChange("reloadable",
                                   new Boolean(oldReloadable),
                                   new Boolean(this.reloadable));

    }


    /**
     * Set the DefaultContext override flag for this web application.
     *
     * @param override The new override flag
     */
    public void setOverride(boolean override) {

        boolean oldOverride = this.override;
        this.override = override;
        support.firePropertyChange("override",
                                   new Boolean(oldOverride),
                                   new Boolean(this.override));

    }


    /**
     * Return the "replace welcome files" property.
     */
    public boolean isReplaceWelcomeFiles() {

        return (this.replaceWelcomeFiles);

    }


    /**
     * Set the "replace welcome files" property.
     *
     * @param replaceWelcomeFiles The new property value
     */
    public void setReplaceWelcomeFiles(boolean replaceWelcomeFiles) {

        boolean oldReplaceWelcomeFiles = this.replaceWelcomeFiles;
        this.replaceWelcomeFiles = replaceWelcomeFiles;
        support.firePropertyChange("replaceWelcomeFiles",
                                   new Boolean(oldReplaceWelcomeFiles),
                                   new Boolean(this.replaceWelcomeFiles));

    }


    /**
     * Return the servlet context for which this Context is a facade.
     */
    public ServletContext getServletContext() {

        if (context == null) {
            context = new ApplicationContext(getBasePath(), this);
            if (altDDName != null)
                context.setAttribute(Globals.ALT_DD_ATTR,altDDName);
        }
        return (context.getFacade());

    }


    /**
     * Return the default session timeout (in minutes) for this
     * web application.
     */
    public int getSessionTimeout() {

        return (this.sessionTimeout);

    }


    /**
     * Set the default session timeout (in minutes) for this
     * web application.
     *
     * @param timeout The new default session timeout
     */
    public void setSessionTimeout(int timeout) {

        int oldSessionTimeout = this.sessionTimeout;
    /*
     * SRV.13.4 ("Deployment Descriptor"):
     * If the timeout is 0 or less, the container ensures the default
     * behaviour of sessions is never to time out.
     */
        this.sessionTimeout = (timeout == 0) ? -1 : timeout;
        support.firePropertyChange("sessionTimeout",
                                   new Integer(oldSessionTimeout),
                                   new Integer(this.sessionTimeout));

    }


    /**
     * Return the value of the swallowOutput flag.
     */
    public boolean getSwallowOutput() {

        return (this.swallowOutput);

    }


    /**
     * Set the value of the swallowOutput flag. If set to true, the system.out
     * and system.err will be redirected to the logger during a servlet
     * execution.
     *
     * @param swallowOuptut The new value
     */
    public void setSwallowOutput(boolean swallowOutput) {

        boolean oldSwallowOutput = this.swallowOutput;
        this.swallowOutput = swallowOutput;
        support.firePropertyChange("swallowOutput",
                                   new Boolean(oldSwallowOutput),
                                   new Boolean(this.swallowOutput));

    }


    /**
     * Unpack WAR flag accessor.
     */
    public boolean getUnpackWAR() {

        return (unpackWAR);

    }


    /**
     * Unpack WAR flag mutator.
     */
    public void setUnpackWAR(boolean unpackWAR) {

        this.unpackWAR = unpackWAR;

    }

    /**
     * Return the Java class name of the Wrapper implementation used
     * for servlets registered in this Context.
     */
    public String getWrapperClass() {

        return (this.wrapperClass);

    }


    /**
     * Set the Java class name of the Wrapper implementation used
     * for servlets registered in this Context.
     *
     * @param wrapperClass The new wrapper class
     */
    public void setWrapperClass(String wrapperClass) {

        this.wrapperClass = wrapperClass;

    }


    /**
     * Set the resources DirContext object with which this Container is
     * associated.
     *
     * @param resources The newly associated DirContext
     */
    public synchronized void setResources(DirContext resources) {

        if (started) {
            throw new IllegalStateException
                (sm.getString("standardContext.resources.started"));
        }

        DirContext oldResources = this.webappResources;
        if (oldResources == resources)
            return;

        if (resources instanceof BaseDirContext) {
            ((BaseDirContext) resources).setCached(isCachingAllowed());
            ((BaseDirContext) resources).setCacheTTL(getCacheTTL());
            ((BaseDirContext) resources).setCacheMaxSize(getCacheMaxSize());
        }
        if (resources instanceof FileDirContext) {
            filesystemBased = true;
            ((FileDirContext) resources).setCaseSensitive(isCaseSensitive());
            ((FileDirContext) resources).setAllowLinking(isAllowLinking());
        }
        this.webappResources = resources;

        // The proxied resources will be refreshed on start
        this.resources = null;

        support.firePropertyChange("resources", oldResources,
                                   this.webappResources);

    }


    // ------------------------------------------------------ Public Properties


    /**
     * Return the Locale to character set mapper class for this Context.
     */
    public String getCharsetMapperClass() {

        return (this.charsetMapperClass);

    }


    /**
     * Set the Locale to character set mapper class for this Context.
     *
     * @param mapper The new mapper class
     */
    public void setCharsetMapperClass(String mapper) {

        String oldCharsetMapperClass = this.charsetMapperClass;
        this.charsetMapperClass = mapper;
        support.firePropertyChange("charsetMapperClass",
                                   oldCharsetMapperClass,
                                   this.charsetMapperClass);

    }


    /** Get the absolute path to the work dir.
     *  To avoid duplication.
     * 
     * @return
     */ 
    public String getWorkPath() {
        File workDir = new File(getWorkDir());
        if (!workDir.isAbsolute()) {
            File catalinaHome = engineBase();
            String catalinaHomePath = null;
            try {
                catalinaHomePath = catalinaHome.getCanonicalPath();
                workDir = new File(catalinaHomePath,
                        getWorkDir());
            } catch (IOException e) {
            }
        }
        return workDir.getAbsolutePath();
    }
    
    /**
     * Return the work directory for this Context.
     */
    public String getWorkDir() {

        return (this.workDir);

    }


    /**
     * Set the work directory for this Context.
     *
     * @param workDir The new work directory
     */
    public void setWorkDir(String workDir) {

        this.workDir = workDir;

        if (started)
            postWorkDirectory();

    }


    // -------------------------------------------------------- Context Methods


    /**
     * Add a new Listener class name to the set of Listeners
     * configured for this application.
     *
     * @param listener Java class name of a listener class
     */
    public void addApplicationListener(String listener) {

        synchronized (applicationListeners) {
            String results[] =new String[applicationListeners.length + 1];
            for (int i = 0; i < applicationListeners.length; i++) {
                if (listener.equals(applicationListeners[i]))
                    return;
                results[i] = applicationListeners[i];
            }
            results[applicationListeners.length] = listener;
            applicationListeners = results;
        }
        fireContainerEvent("addApplicationListener", listener);

        // FIXME - add instance if already started?

    }


    /**
     * Add a new application parameter for this application.
     *
     * @param parameter The new application parameter
     */
    public void addApplicationParameter(ApplicationParameter parameter) {

        synchronized (applicationParameters) {
            String newName = parameter.getName();
            for (int i = 0; i < applicationParameters.length; i++) {
                if (newName.equals(applicationParameters[i].getName()) &&
                    !applicationParameters[i].getOverride())
                    return;
            }
            ApplicationParameter results[] =
                new ApplicationParameter[applicationParameters.length + 1];
            System.arraycopy(applicationParameters, 0, results, 0,
                             applicationParameters.length);
            results[applicationParameters.length] = parameter;
            applicationParameters = results;
        }
        fireContainerEvent("addApplicationParameter", parameter);

    }


    /**
     * Add a child Container, only if the proposed child is an implementation
     * of Wrapper.
     *
     * @param child Child container to be added
     *
     * @exception IllegalArgumentException if the proposed container is
     *  not an implementation of Wrapper
     */
    public void addChild(Container child) {

        if (!(child instanceof Wrapper))
            throw new IllegalArgumentException
                (sm.getString("standardContext.notWrapper"));
        Wrapper wrapper = (Wrapper) child;
        String jspFile = wrapper.getJspFile();
        if ((jspFile != null) && !jspFile.startsWith("/")) {
            if (isServlet22()) {
                log.debug(sm.getString("standardContext.wrapper.warning", 
                                       jspFile));
                wrapper.setJspFile("/" + jspFile);
            } else {
                throw new IllegalArgumentException
                    (sm.getString("standardContext.wrapper.error", jspFile));
            }
        }

        super.addChild(child);

    }


    /**
     * Add a security constraint to the set for this web application.
     */
    public void addConstraint(SecurityConstraint constraint) {

        // Validate the proposed constraint
        SecurityCollection collections[] = constraint.findCollections();
        for (int i = 0; i < collections.length; i++) {
            String patterns[] = collections[i].findPatterns();
            for (int j = 0; j < patterns.length; j++) {
                patterns[j] = adjustURLPattern(patterns[j]);
                if (!validateURLPattern(patterns[j]))
                    throw new IllegalArgumentException
                        (sm.getString
                         ("standardContext.securityConstraint.pattern",
                          patterns[j]));
            }
        }

        // Add this constraint to the set for our web application
        synchronized (constraints) {
            SecurityConstraint results[] =
                new SecurityConstraint[constraints.length + 1];
            for (int i = 0; i < constraints.length; i++)
                results[i] = constraints[i];
            results[constraints.length] = constraint;
            constraints = results;
        }

    }



    /**
     * Add an EJB resource reference for this web application.
     *
     * @param ejb New EJB resource reference
     */
    public void addEjb(ContextEjb ejb) {

        namingResources.addEjb(ejb);
        fireContainerEvent("addEjb", ejb.getName());

    }


    /**
     * Add an environment entry for this web application.
     *
     * @param environment New environment entry
     */
    public void addEnvironment(ContextEnvironment environment) {

        ContextEnvironment env = findEnvironment(environment.getName());
        if ((env != null) && !env.getOverride())
            return;
        namingResources.addEnvironment(environment);
        fireContainerEvent("addEnvironment", environment.getName());

    }


    /**
     * Add resource parameters for this web application.
     *
     * @param resourceParameters New resource parameters
     */
    public void addResourceParams(ResourceParams resourceParameters) {

        namingResources.addResourceParams(resourceParameters);
        fireContainerEvent("addResourceParams", resourceParameters.getName());

    }


    /**
     * Add an error page for the specified error or Java exception.
     *
     * @param errorPage The error page definition to be added
     */
    public void addErrorPage(ErrorPage errorPage) {
        // Validate the input parameters
        if (errorPage == null)
            throw new IllegalArgumentException
                (sm.getString("standardContext.errorPage.required"));
        String location = errorPage.getLocation();
        if ((location != null) && !location.startsWith("/")) {
            if (isServlet22()) {
                log.debug(sm.getString("standardContext.errorPage.warning",
                                 location));
                errorPage.setLocation("/" + location);
            } else {
                throw new IllegalArgumentException
                    (sm.getString("standardContext.errorPage.error",
                                  location));
            }
        }

        // Add the specified error page to our internal collections
        String exceptionType = errorPage.getExceptionType();
        if (exceptionType != null) {
            synchronized (exceptionPages) {
                exceptionPages.put(exceptionType, errorPage);
            }
        } else {
            synchronized (statusPages) {
                if (errorPage.getErrorCode() == 200) {
                    this.okErrorPage = errorPage;
                }
                statusPages.put(new Integer(errorPage.getErrorCode()),
                                errorPage);
            }
        }
        fireContainerEvent("addErrorPage", errorPage);

    }


    /**
     * Add a filter definition to this Context.
     *
     * @param filterDef The filter definition to be added
     */
    public void addFilterDef(FilterDef filterDef) {

        synchronized (filterDefs) {
            filterDefs.put(filterDef.getFilterName(), filterDef);
        }
        fireContainerEvent("addFilterDef", filterDef);

    }


    /**
     * Add a filter mapping to this Context.
     *
     * @param filterMap The filter mapping to be added
     *
     * @exception IllegalArgumentException if the specified filter name
     *  does not match an existing filter definition, or the filter mapping
     *  is malformed
     */
    public void addFilterMap(FilterMap filterMap) {

        // Validate the proposed filter mapping
        String filterName = filterMap.getFilterName();
        String servletName = filterMap.getServletName();
        String urlPattern = filterMap.getURLPattern();
        if (findFilterDef(filterName) == null)
            throw new IllegalArgumentException
                (sm.getString("standardContext.filterMap.name", filterName));
        if ((servletName == null) && (urlPattern == null))
            throw new IllegalArgumentException
                (sm.getString("standardContext.filterMap.either"));
        if ((servletName != null) && (urlPattern != null))
            throw new IllegalArgumentException
                (sm.getString("standardContext.filterMap.either"));
        // Because filter-pattern is new in 2.3, no need to adjust
        // for 2.2 backwards compatibility
        if ((urlPattern != null) && !validateURLPattern(urlPattern))
            throw new IllegalArgumentException
                (sm.getString("standardContext.filterMap.pattern",
                              urlPattern));

        // Add this filter mapping to our registered set
        synchronized (filterMaps) {
            FilterMap results[] =new FilterMap[filterMaps.length + 1];
            System.arraycopy(filterMaps, 0, results, 0, filterMaps.length);
            results[filterMaps.length] = filterMap;
            filterMaps = results;
        }
        fireContainerEvent("addFilterMap", filterMap);

    }


    /**
     * Add the classname of an InstanceListener to be added to each
     * Wrapper appended to this Context.
     *
     * @param listener Java class name of an InstanceListener class
     */
    public void addInstanceListener(String listener) {

        synchronized (instanceListeners) {
            String results[] =new String[instanceListeners.length + 1];
            for (int i = 0; i < instanceListeners.length; i++)
                results[i] = instanceListeners[i];
            results[instanceListeners.length] = listener;
            instanceListeners = results;
        }
        fireContainerEvent("addInstanceListener", listener);

    }

    /**
     * Add the given URL pattern as a jsp-property-group.  This maps
     * resources that match the given pattern so they will be passed
     * to the JSP container.  Though there are other elements in the
     * property group, we only care about the URL pattern here.  The
     * JSP container will parse the rest.
     *
     * @param pattern URL pattern to be mapped
     */
    public void addJspMapping(String pattern) {
        String servletName = findServletMapping("*.jsp");
        if (servletName == null) {
            servletName = "jsp";
        }

        if( findChild(servletName) != null) {
            addServletMapping(pattern, servletName);
        } else {
            log.debug("Skiping " + pattern + " , no servlet " + servletName);
        }
    }


    /**
     * Add a Locale Encoding Mapping (see Sec 5.4 of Servlet spec 2.4)
     *
     * @param locale locale to map an encoding for
     * @param encoding encoding to be used for a give locale
     */
    public void addLocaleEncodingMappingParameter(String locale, String encoding){
        getCharsetMapper().addCharsetMappingFromDeploymentDescriptor(locale, encoding);
    }


    /**
     * Add a local EJB resource reference for this web application.
     *
     * @param ejb New EJB resource reference
     */
    public void addLocalEjb(ContextLocalEjb ejb) {

        namingResources.addLocalEjb(ejb);
        fireContainerEvent("addLocalEjb", ejb.getName());

    }


    /**
     * Add a message destination for this web application.
     *
     * @param md New message destination
     */
    public void addMessageDestination(MessageDestination md) {

        synchronized (messageDestinations) {
            messageDestinations.put(md.getName(), md);
        }
        fireContainerEvent("addMessageDestination", md.getName());

    }


    /**
     * Add a message destination reference for this web application.
     *
     * @param mdr New message destination reference
     */
    public void addMessageDestinationRef
        (MessageDestinationRef mdr) {

        namingResources.addMessageDestinationRef(mdr);
        fireContainerEvent("addMessageDestinationRef", mdr.getName());

    }


    /**
     * Add a new MIME mapping, replacing any existing mapping for
     * the specified extension.
     *
     * @param extension Filename extension being mapped
     * @param mimeType Corresponding MIME type
     */
    public void addMimeMapping(String extension, String mimeType) {

        synchronized (mimeMappings) {
            mimeMappings.put(extension, mimeType);
        }
        fireContainerEvent("addMimeMapping", extension);

    }


    /**
     * Add a new context initialization parameter.
     *
     * @param name Name of the new parameter
     * @param value Value of the new  parameter
     *
     * @exception IllegalArgumentException if the name or value is missing,
     *  or if this context initialization parameter has already been
     *  registered
     */
    public void addParameter(String name, String value) {
        // Validate the proposed context initialization parameter
        if ((name == null) || (value == null))
            throw new IllegalArgumentException
                (sm.getString("standardContext.parameter.required"));
        if (parameters.get(name) != null)
            throw new IllegalArgumentException
                (sm.getString("standardContext.parameter.duplicate", name));

        // Add this parameter to our defined set
        synchronized (parameters) {
            parameters.put(name, value);
        }
        fireContainerEvent("addParameter", name);

    }


    /**
     * Add a resource reference for this web application.
     *
     * @param resource New resource reference
     */
    public void addResource(ContextResource resource) {

        namingResources.addResource(resource);
        fireContainerEvent("addResource", resource.getName());

    }


    /**
     * Add a resource environment reference for this web application.
     *
     * @param name The resource environment reference name
     * @param type The resource environment reference type
     */
    public void addResourceEnvRef(String name, String type) {

        namingResources.addResourceEnvRef(name, type);
        fireContainerEvent("addResourceEnvRef", name);

    }


    /**
     * Add a resource link for this web application.
     *
     * @param resourceLink New resource link
     */
    public void addResourceLink(ContextResourceLink resourceLink) {

        namingResources.addResourceLink(resourceLink);
        fireContainerEvent("addResourceLink", resourceLink.getName());

    }


    /**
     * Add a security role reference for this web application.
     *
     * @param role Security role used in the application
     * @param link Actual security role to check for
     */
    public void addRoleMapping(String role, String link) {

        synchronized (roleMappings) {
            roleMappings.put(role, link);
        }
        fireContainerEvent("addRoleMapping", role);

    }


    /**
     * Add a new security role for this web application.
     *
     * @param role New security role
     */
    public void addSecurityRole(String role) {

        synchronized (securityRoles) {
            String results[] =new String[securityRoles.length + 1];
            for (int i = 0; i < securityRoles.length; i++)
                results[i] = securityRoles[i];
            results[securityRoles.length] = role;
            securityRoles = results;
        }
        fireContainerEvent("addSecurityRole", role);

    }


    /**
     * Add a new servlet mapping, replacing any existing mapping for
     * the specified pattern.
     *
     * @param pattern URL pattern to be mapped
     * @param name Name of the corresponding servlet to execute
     *
     * @exception IllegalArgumentException if the specified servlet name
     *  is not known to this Context
     */
    public void addServletMapping(String pattern, String name) {

        // Validate the proposed mapping
        if (findChild(name) == null)
            throw new IllegalArgumentException
                (sm.getString("standardContext.servletMap.name", name));
        pattern = adjustURLPattern(RequestUtil.URLDecode(pattern));
        if (!validateURLPattern(pattern))
            throw new IllegalArgumentException
                (sm.getString("standardContext.servletMap.pattern", pattern));

        // Add this mapping to our registered set
        synchronized (servletMappings) {
            String name2 = (String) servletMappings.get(pattern);
            if (name2 != null) {
                // Don't allow more than one servlet on the same pattern
                Wrapper wrapper = (Wrapper) findChild(name2);
                wrapper.removeMapping(pattern);
                mapper.removeWrapper(pattern);
            }
            servletMappings.put(pattern, name);
        }
        Wrapper wrapper = (Wrapper) findChild(name);
        wrapper.addMapping(pattern);

        // Update context mapper
        mapper.addWrapper(pattern, wrapper);

        fireContainerEvent("addServletMapping", pattern);

    }


    /**
     * Add a JSP tag library for the specified URI.
     *
     * @param uri URI, relative to the web.xml file, of this tag library
     * @param location Location of the tag library descriptor
     */
    public void addTaglib(String uri, String location) {

        synchronized (taglibs) {
            taglibs.put(uri, location);
        }
        fireContainerEvent("addTaglib", uri);

    }


    /**
     * Add a new welcome file to the set recognized by this Context.
     *
     * @param name New welcome file name
     */
    public void addWelcomeFile(String name) {

        synchronized (welcomeFiles) {
            // Welcome files from the application deployment descriptor
            // completely replace those from the default conf/web.xml file
            if (replaceWelcomeFiles) {
                welcomeFiles = new String[0];
                setReplaceWelcomeFiles(false);
            }
            String results[] =new String[welcomeFiles.length + 1];
            for (int i = 0; i < welcomeFiles.length; i++)
                results[i] = welcomeFiles[i];
            results[welcomeFiles.length] = name;
            welcomeFiles = results;
        }
        postWelcomeFiles();
        fireContainerEvent("addWelcomeFile", name);

    }


    /**
     * Add the classname of a LifecycleListener to be added to each
     * Wrapper appended to this Context.
     *
     * @param listener Java class name of a LifecycleListener class
     */
    public void addWrapperLifecycle(String listener) {

        synchronized (wrapperLifecycles) {
            String results[] =new String[wrapperLifecycles.length + 1];
            for (int i = 0; i < wrapperLifecycles.length; i++)
                results[i] = wrapperLifecycles[i];
            results[wrapperLifecycles.length] = listener;
            wrapperLifecycles = results;
        }
        fireContainerEvent("addWrapperLifecycle", listener);

    }


    /**
     * Add the classname of a ContainerListener to be added to each
     * Wrapper appended to this Context.
     *
     * @param listener Java class name of a ContainerListener class
     */
    public void addWrapperListener(String listener) {

        synchronized (wrapperListeners) {
            String results[] =new String[wrapperListeners.length + 1];
            for (int i = 0; i < wrapperListeners.length; i++)
                results[i] = wrapperListeners[i];
            results[wrapperListeners.length] = listener;
            wrapperListeners = results;
        }
        fireContainerEvent("addWrapperListener", listener);

    }


    /**
     * Factory method to create and return a new Wrapper instance, of
     * the Java implementation class appropriate for this Context
     * implementation.  The constructor of the instantiated Wrapper
     * will have been called, but no properties will have been set.
     */
    public Wrapper createWrapper() {
        //log.info( "Create wrapper" );
        Wrapper wrapper = new StandardWrapper();
        wrappers.add(wrapper);

        synchronized (instanceListeners) {
            for (int i = 0; i < instanceListeners.length; i++) {
                try {
                    Class clazz = Class.forName(instanceListeners[i]);
                    InstanceListener listener =
                      (InstanceListener) clazz.newInstance();
                    wrapper.addInstanceListener(listener);
                } catch (Throwable t) {
                    log.error("createWrapper", t);
                    return (null);
                }
            }
        }

        synchronized (wrapperLifecycles) {
            for (int i = 0; i < wrapperLifecycles.length; i++) {
                try {
                    Class clazz = Class.forName(wrapperLifecycles[i]);
                    LifecycleListener listener =
                      (LifecycleListener) clazz.newInstance();
                    if (wrapper instanceof Lifecycle)
                        ((Lifecycle) wrapper).addLifecycleListener(listener);
                } catch (Throwable t) {
                    log.error("createWrapper", t);
                    return (null);
                }
            }
        }

        synchronized (wrapperListeners) {
            for (int i = 0; i < wrapperListeners.length; i++) {
                try {
                    Class clazz = Class.forName(wrapperListeners[i]);
                    ContainerListener listener =
                      (ContainerListener) clazz.newInstance();
                    wrapper.addContainerListener(listener);
                } catch (Throwable t) {
                    log.error("createWrapper", t);
                    return (null);
                }
            }
        }

        return (wrapper);

    }


    /**
     * Return the set of application listener class names configured
     * for this application.
     */
    public String[] findApplicationListeners() {

        return (applicationListeners);

    }


    /**
     * Return the set of application parameters for this application.
     */
    public ApplicationParameter[] findApplicationParameters() {

        return (applicationParameters);

    }


    /**
     * Return the security constraints for this web application.
     * If there are none, a zero-length array is returned.
     */
    public SecurityConstraint[] findConstraints() {

        return (constraints);

    }


    /**
     * Return the EJB resource reference with the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired EJB resource reference
     */
    public ContextEjb findEjb(String name) {

        return namingResources.findEjb(name);

    }


    /**
     * Return the defined EJB resource references for this application.
     * If there are none, a zero-length array is returned.
     */
    public ContextEjb[] findEjbs() {

        return namingResources.findEjbs();

    }


    /**
     * Return the environment entry with the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired environment entry
     */
    public ContextEnvironment findEnvironment(String name) {

        return namingResources.findEnvironment(name);

    }


    /**
     * Return the set of defined environment entries for this web
     * application.  If none have been defined, a zero-length array
     * is returned.
     */
    public ContextEnvironment[] findEnvironments() {

        return namingResources.findEnvironments();

    }


    /**
     * Return the error page entry for the specified HTTP error code,
     * if any; otherwise return <code>null</code>.
     *
     * @param errorCode Error code to look up
     */
    public ErrorPage findErrorPage(int errorCode) {
        if (errorCode == 200) {
            return (okErrorPage);
        } else {
            return ((ErrorPage) statusPages.get(new Integer(errorCode)));
        }

    }


    /**
     * Return the error page entry for the specified Java exception type,
     * if any; otherwise return <code>null</code>.
     *
     * @param exceptionType Exception type to look up
     */
    public ErrorPage findErrorPage(String exceptionType) {

        synchronized (exceptionPages) {
            return ((ErrorPage) exceptionPages.get(exceptionType));
        }

    }


    /**
     * Return the set of defined error pages for all specified error codes
     * and exception types.
     */
    public ErrorPage[] findErrorPages() {

        synchronized(exceptionPages) {
            synchronized(statusPages) {
                ErrorPage results1[] = new ErrorPage[exceptionPages.size()];
                results1 =
                    (ErrorPage[]) exceptionPages.values().toArray(results1);
                ErrorPage results2[] = new ErrorPage[statusPages.size()];
                results2 =
                    (ErrorPage[]) statusPages.values().toArray(results2);
                ErrorPage results[] =
                    new ErrorPage[results1.length + results2.length];
                for (int i = 0; i < results1.length; i++)
                    results[i] = results1[i];
                for (int i = results1.length; i < results.length; i++)
                    results[i] = results2[i - results1.length];
                return (results);
            }
        }

    }


    /**
     * Return the filter definition for the specified filter name, if any;
     * otherwise return <code>null</code>.
     *
     * @param filterName Filter name to look up
     */
    public FilterDef findFilterDef(String filterName) {

        synchronized (filterDefs) {
            return ((FilterDef) filterDefs.get(filterName));
        }

    }


    /**
     * Return the set of defined filters for this Context.
     */
    public FilterDef[] findFilterDefs() {

        synchronized (filterDefs) {
            FilterDef results[] = new FilterDef[filterDefs.size()];
            return ((FilterDef[]) filterDefs.values().toArray(results));
        }

    }


    /**
     * Return the set of filter mappings for this Context.
     */
    public FilterMap[] findFilterMaps() {

        return (filterMaps);

    }


    /**
     * Return the set of InstanceListener classes that will be added to
     * newly created Wrappers automatically.
     */
    public String[] findInstanceListeners() {

        return (instanceListeners);

    }


    /**
     * Return the local EJB resource reference with the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired EJB resource reference
     */
    public ContextLocalEjb findLocalEjb(String name) {

        return namingResources.findLocalEjb(name);

    }


    /**
     * Return the defined local EJB resource references for this application.
     * If there are none, a zero-length array is returned.
     */
    public ContextLocalEjb[] findLocalEjbs() {

        return namingResources.findLocalEjbs();

    }


    /**
     * FIXME: Fooling introspection ...
     */
    public Context findMappingObject() {
        return (Context) getMappingObject();
    }
    
    
    /**
     * Return the message destination with the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired message destination
     */
    public MessageDestination findMessageDestination(String name) {

        synchronized (messageDestinations) {
            return ((MessageDestination) messageDestinations.get(name));
        }

    }


    /**
     * Return the set of defined message destinations for this web
     * application.  If none have been defined, a zero-length array
     * is returned.
     */
    public MessageDestination[] findMessageDestinations() {

        synchronized (messageDestinations) {
            MessageDestination results[] =
                new MessageDestination[messageDestinations.size()];
            return ((MessageDestination[])
                    messageDestinations.values().toArray(results));
        }

    }


    /**
     * Return the message destination ref with the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Name of the desired message destination ref
     */
    public MessageDestinationRef
        findMessageDestinationRef(String name) {

        return namingResources.findMessageDestinationRef(name);

    }


    /**
     * Return the set of defined message destination refs for this web
     * application.  If none have been defined, a zero-length array
     * is returned.
     */
    public MessageDestinationRef[]
        findMessageDestinationRefs() {

        return namingResources.findMessageDestinationRefs();

    }


    /**
     * Return the MIME type to which the specified extension is mapped,
     * if any; otherwise return <code>null</code>.
     *
     * @param extension Extension to map to a MIME type
     */
    public String findMimeMapping(String extension) {

        return ((String) mimeMappings.get(extension));

    }


    /**
     * Return the extensions for which MIME mappings are defined.  If there
     * are none, a zero-length array is returned.
     */
    public String[] findMimeMappings() {

        synchronized (mimeMappings) {
            String results[] = new String[mimeMappings.size()];
            return
                ((String[]) mimeMappings.keySet().toArray(results));
        }

    }


    /**
     * Return the value for the specified context initialization
     * parameter name, if any; otherwise return <code>null</code>.
     *
     * @param name Name of the parameter to return
     */
    public String findParameter(String name) {

        synchronized (parameters) {
            return ((String) parameters.get(name));
        }

    }


    /**
     * Return the names of all defined context initialization parameters
     * for this Context.  If no parameters are defined, a zero-length
     * array is returned.
     */
    public String[] findParameters() {

        synchronized (parameters) {
            String results[] = new String[parameters.size()];
            return ((String[]) parameters.keySet().toArray(results));
        }

    }


    /**
     * Return the resource reference with the specified name, if any;
     * otherwise return <code>null</code>.
     *
     * @param name Name of the desired resource reference
     */
    public ContextResource findResource(String name) {

        return namingResources.findResource(name);

    }


    /**
     * Return the resource environment reference type for the specified
     * name, if any; otherwise return <code>null</code>.
     *
     * @param name Name of the desired resource environment reference
     */
    public String findResourceEnvRef(String name) {

        return namingResources.findResourceEnvRef(name);

    }


    /**
     * Return the set of resource environment reference names for this
     * web application.  If none have been specified, a zero-length
     * array is returned.
     */
    public String[] findResourceEnvRefs() {

        return namingResources.findResourceEnvRefs();

    }


    /**
     * Return the resource link with the specified name, if any;
     * otherwise return <code>null</code>.
     *
     * @param name Name of the desired resource link
     */
    public ContextResourceLink findResourceLink(String name) {

        return namingResources.findResourceLink(name);

    }


    /**
     * Return the defined resource links for this application.  If
     * none have been defined, a zero-length array is returned.
     */
    public ContextResourceLink[] findResourceLinks() {

        return namingResources.findResourceLinks();

    }


    /**
     * Return the defined resource references for this application.  If
     * none have been defined, a zero-length array is returned.
     */
    public ContextResource[] findResources() {

        return namingResources.findResources();

    }


    /**
     * For the given security role (as used by an application), return the
     * corresponding role name (as defined by the underlying Realm) if there
     * is one.  Otherwise, return the specified role unchanged.
     *
     * @param role Security role to map
     */
    public String findRoleMapping(String role) {

        String realRole = null;
        synchronized (roleMappings) {
            realRole = (String) roleMappings.get(role);
        }
        if (realRole != null)
            return (realRole);
        else
            return (role);

    }


    /**
     * Return <code>true</code> if the specified security role is defined
     * for this application; otherwise return <code>false</code>.
     *
     * @param role Security role to verify
     */
    public boolean findSecurityRole(String role) {

        synchronized (securityRoles) {
            for (int i = 0; i < securityRoles.length; i++) {
                if (role.equals(securityRoles[i]))
                    return (true);
            }
        }
        return (false);

    }


    /**
     * Return the security roles defined for this application.  If none
     * have been defined, a zero-length array is returned.
     */
    public String[] findSecurityRoles() {

        return (securityRoles);

    }


    /**
     * Return the servlet name mapped by the specified pattern (if any);
     * otherwise return <code>null</code>.
     *
     * @param pattern Pattern for which a mapping is requested
     */
    public String findServletMapping(String pattern) {

        synchronized (servletMappings) {
            return ((String) servletMappings.get(pattern));
        }

    }


    /**
     * Return the patterns of all defined servlet mappings for this
     * Context.  If no mappings are defined, a zero-length array is returned.
     */
    public String[] findServletMappings() {

        synchronized (servletMappings) {
            String results[] = new String[servletMappings.size()];
            return
               ((String[]) servletMappings.keySet().toArray(results));
        }

    }


    /**
     * Return the context-relative URI of the error page for the specified
     * HTTP status code, if any; otherwise return <code>null</code>.
     *
     * @param status HTTP status code to look up
     */
    public String findStatusPage(int status) {

        return ((String) statusPages.get(new Integer(status)));

    }


    /**
     * Return the set of HTTP status codes for which error pages have
     * been specified.  If none are specified, a zero-length array
     * is returned.
     */
    public int[] findStatusPages() {

        synchronized (statusPages) {
            int results[] = new int[statusPages.size()];
            Iterator elements = statusPages.keySet().iterator();
            int i = 0;
            while (elements.hasNext())
                results[i++] = ((Integer) elements.next()).intValue();
            return (results);
        }

    }


    /**
     * Return the tag library descriptor location for the specified taglib
     * URI, if any; otherwise, return <code>null</code>.
     *
     * @param uri URI, relative to the web.xml file
     */
    public String findTaglib(String uri) {

        synchronized (taglibs) {
            return ((String) taglibs.get(uri));
        }

    }


    /**
     * Return the URIs of all tag libraries for which a tag library
     * descriptor location has been specified.  If none are specified,
     * a zero-length array is returned.
     */
    public String[] findTaglibs() {

        synchronized (taglibs) {
            String results[] = new String[taglibs.size()];
            return ((String[]) taglibs.keySet().toArray(results));
        }

    }


    /**
     * Return <code>true</code> if the specified welcome file is defined
     * for this Context; otherwise return <code>false</code>.
     *
     * @param name Welcome file to verify
     */
    public boolean findWelcomeFile(String name) {

        synchronized (welcomeFiles) {
            for (int i = 0; i < welcomeFiles.length; i++) {
                if (name.equals(welcomeFiles[i]))
                    return (true);
            }
        }
        return (false);

    }


    /**
     * Return the set of welcome files defined for this Context.  If none are
     * defined, a zero-length array is returned.
     */
    public String[] findWelcomeFiles() {

        return (welcomeFiles);

    }


    /**
     * Return the set of LifecycleListener classes that will be added to
     * newly created Wrappers automatically.
     */
    public String[] findWrapperLifecycles() {

        return (wrapperLifecycles);

    }


    /**
     * Return the set of ContainerListener classes that will be added to
     * newly created Wrappers automatically.
     */
    public String[] findWrapperListeners() {

        return (wrapperListeners);

    }


    /**
     * Reload this web application, if reloading is supported.
     * <p>
     * <b>IMPLEMENTATION NOTE</b>:  This method is designed to deal with
     * reloads required by changes to classes in the underlying repositories
     * of our class loader.  It does not handle changes to the web application
     * deployment descriptor.  If that has occurred, you should stop this
     * Context and create (and start) a new Context instance instead.
     *
     * @exception IllegalStateException if the <code>reloadable</code>
     *  property is set to <code>false</code>.
     */
    public synchronized void reload() {

        // Validate our current component state
        if (!started)
            throw new IllegalStateException
                (sm.getString("containerBase.notStarted", logName()));

        // Make sure reloading is enabled
        //      if (!reloadable)
        //          throw new IllegalStateException
        //              (sm.getString("standardContext.notReloadable"));
        log.info(sm.getString("standardContext.reloadingStarted"));

        // Stop accepting requests temporarily
        setPaused(true);

        try {
            stop();
        } catch (LifecycleException e) {
            log.error(sm.getString("standardContext.stoppingContext"), e);
        }

        try {
            start();
        } catch (LifecycleException e) {
            log.error(sm.getString("standardContext.startingContext"), e);
        }

        setPaused(false);

    }


    /**
     * Remove the specified application listener class from the set of
     * listeners for this application.
     *
     * @param listener Java class name of the listener to be removed
     */
    public void removeApplicationListener(String listener) {

        synchronized (applicationListeners) {

            // Make sure this welcome file is currently present
            int n = -1;
            for (int i = 0; i < applicationListeners.length; i++) {
                if (applicationListeners[i].equals(listener)) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified constraint
            int j = 0;
            String results[] = new String[applicationListeners.length - 1];
            for (int i = 0; i < applicationListeners.length; i++) {
                if (i != n)
                    results[j++] = applicationListeners[i];
            }
            applicationListeners = results;

        }

        // Inform interested listeners
        fireContainerEvent("removeApplicationListener", listener);

        // FIXME - behavior if already started?

    }


    /**
     * Remove the application parameter with the specified name from
     * the set for this application.
     *
     * @param name Name of the application parameter to remove
     */
    public void removeApplicationParameter(String name) {

        synchronized (applicationParameters) {

            // Make sure this parameter is currently present
            int n = -1;
            for (int i = 0; i < applicationParameters.length; i++) {
                if (name.equals(applicationParameters[i].getName())) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified parameter
            int j = 0;
            ApplicationParameter results[] =
                new ApplicationParameter[applicationParameters.length - 1];
            for (int i = 0; i < applicationParameters.length; i++) {
                if (i != n)
                    results[j++] = applicationParameters[i];
            }
            applicationParameters = results;

        }

        // Inform interested listeners
        fireContainerEvent("removeApplicationParameter", name);

    }


    /**
     * Add a child Container, only if the proposed child is an implementation
     * of Wrapper.
     *
     * @param child Child container to be added
     *
     * @exception IllegalArgumentException if the proposed container is
     *  not an implementation of Wrapper
     */
    public void removeChild(Container child) {

        if (!(child instanceof Wrapper))
            throw new IllegalArgumentException
                (sm.getString("standardContext.notWrapper"));

        super.removeChild(child);

    }


    /**
     * Remove the specified security constraint from this web application.
     *
     * @param constraint Constraint to be removed
     */
    public void removeConstraint(SecurityConstraint constraint) {

        synchronized (constraints) {

            // Make sure this constraint is currently present
            int n = -1;
            for (int i = 0; i < constraints.length; i++) {
                if (constraints[i].equals(constraint)) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified constraint
            int j = 0;
            SecurityConstraint results[] =
                new SecurityConstraint[constraints.length - 1];
            for (int i = 0; i < constraints.length; i++) {
                if (i != n)
                    results[j++] = constraints[i];
            }
            constraints = results;

        }

        // Inform interested listeners
        fireContainerEvent("removeConstraint", constraint);

    }


    /**
     * Remove any EJB resource reference with the specified name.
     *
     * @param name Name of the EJB resource reference to remove
     */
    public void removeEjb(String name) {

        namingResources.removeEjb(name);
        fireContainerEvent("removeEjb", name);

    }


    /**
     * Remove any environment entry with the specified name.
     *
     * @param name Name of the environment entry to remove
     */
    public void removeEnvironment(String name) {
        if (namingResources == null) {
            return;
        }
        ContextEnvironment env = namingResources.findEnvironment(name);
        if (env == null) {
            throw new IllegalArgumentException
                ("Invalid environment name '" + name + "'");
        }

        namingResources.removeEnvironment(name);
        fireContainerEvent("removeEnvironment", name);

    }


    /**
     * Remove the error page for the specified error code or
     * Java language exception, if it exists; otherwise, no action is taken.
     *
     * @param errorPage The error page definition to be removed
     */
    public void removeErrorPage(ErrorPage errorPage) {

        String exceptionType = errorPage.getExceptionType();
        if (exceptionType != null) {
            synchronized (exceptionPages) {
                exceptionPages.remove(exceptionType);
            }
        } else {
            synchronized (statusPages) {
                if (errorPage.getErrorCode() == 200) {
                    this.okErrorPage = null;
                }
                statusPages.remove(new Integer(errorPage.getErrorCode()));
            }
        }
        fireContainerEvent("removeErrorPage", errorPage);

    }


    /**
     * Remove the specified filter definition from this Context, if it exists;
     * otherwise, no action is taken.
     *
     * @param filterDef Filter definition to be removed
     */
    public void removeFilterDef(FilterDef filterDef) {

        synchronized (filterDefs) {
            filterDefs.remove(filterDef.getFilterName());
        }
        fireContainerEvent("removeFilterDef", filterDef);

    }


    /**
     * Remove a filter mapping from this Context.
     *
     * @param filterMap The filter mapping to be removed
     */
    public void removeFilterMap(FilterMap filterMap) {

        synchronized (filterMaps) {

            // Make sure this filter mapping is currently present
            int n = -1;
            for (int i = 0; i < filterMaps.length; i++) {
                if (filterMaps[i] == filterMap) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified filter mapping
            FilterMap results[] = new FilterMap[filterMaps.length - 1];
            System.arraycopy(filterMaps, 0, results, 0, n);
            System.arraycopy(filterMaps, n + 1, results, n,
                             (filterMaps.length - 1) - n);
            filterMaps = results;

        }

        // Inform interested listeners
        fireContainerEvent("removeFilterMap", filterMap);

    }


    /**
     * Remove a class name from the set of InstanceListener classes that
     * will be added to newly created Wrappers.
     *
     * @param listener Class name of an InstanceListener class to be removed
     */
    public void removeInstanceListener(String listener) {

        synchronized (instanceListeners) {

            // Make sure this welcome file is currently present
            int n = -1;
            for (int i = 0; i < instanceListeners.length; i++) {
                if (instanceListeners[i].equals(listener)) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified constraint
            int j = 0;
            String results[] = new String[instanceListeners.length - 1];
            for (int i = 0; i < instanceListeners.length; i++) {
                if (i != n)
                    results[j++] = instanceListeners[i];
            }
            instanceListeners = results;

        }

        // Inform interested listeners
        fireContainerEvent("removeInstanceListener", listener);

    }


    /**
     * Remove any local EJB resource reference with the specified name.
     *
     * @param name Name of the EJB resource reference to remove
     */
    public void removeLocalEjb(String name) {

        namingResources.removeLocalEjb(name);
        fireContainerEvent("removeLocalEjb", name);

    }


    /**
     * Remove any message destination with the specified name.
     *
     * @param name Name of the message destination to remove
     */
    public void removeMessageDestination(String name) {

        synchronized (messageDestinations) {
            messageDestinations.remove(name);
        }
        fireContainerEvent("removeMessageDestination", name);

    }


    /**
     * Remove any message destination ref with the specified name.
     *
     * @param name Name of the message destination ref to remove
     */
    public void removeMessageDestinationRef(String name) {

        namingResources.removeMessageDestinationRef(name);
        fireContainerEvent("removeMessageDestinationRef", name);

    }


    /**
     * Remove the MIME mapping for the specified extension, if it exists;
     * otherwise, no action is taken.
     *
     * @param extension Extension to remove the mapping for
     */
    public void removeMimeMapping(String extension) {

        synchronized (mimeMappings) {
            mimeMappings.remove(extension);
        }
        fireContainerEvent("removeMimeMapping", extension);

    }


    /**
     * Remove the context initialization parameter with the specified
     * name, if it exists; otherwise, no action is taken.
     *
     * @param name Name of the parameter to remove
     */
    public void removeParameter(String name) {

        synchronized (parameters) {
            parameters.remove(name);
        }
        fireContainerEvent("removeParameter", name);

    }


    /**
     * Remove any resource reference with the specified name.
     *
     * @param name Name of the resource reference to remove
     */
    public void removeResource(String name) {
        name = URLDecoder.decode(name);
        if (namingResources == null) {
            return;
        }
        ContextResource resource = namingResources.findResource(name);
        if (resource == null) {
            throw new IllegalArgumentException
                ("Invalid resource name '" + name + "'");
        }

        namingResources.removeResource(name);
        fireContainerEvent("removeResource", name);

    }


    /**
     * Remove any resource environment reference with the specified name.
     *
     * @param name Name of the resource environment reference to remove
     */
    public void removeResourceEnvRef(String name) {

        namingResources.removeResourceEnvRef(name);
        fireContainerEvent("removeResourceEnvRef", name);

    }


    /**
     * Remove any resource link with the specified name.
     *
     * @param name Name of the resource link to remove
     */
    public void removeResourceLink(String name) {
        name = URLDecoder.decode(name);
        if (namingResources == null) {
            return;
        }
        ContextResourceLink resource = namingResources.findResourceLink(name);
        if (resource == null) {
            throw new IllegalArgumentException
                ("Invalid resource name '" + name + "'");
        }

        namingResources.removeResourceLink(name);
        fireContainerEvent("removeResourceLink", name);

    }


    /**
     * Remove any security role reference for the specified name
     *
     * @param role Security role (as used in the application) to remove
     */
    public void removeRoleMapping(String role) {

        synchronized (roleMappings) {
            roleMappings.remove(role);
        }
        fireContainerEvent("removeRoleMapping", role);

    }


    /**
     * Remove any security role with the specified name.
     *
     * @param role Security role to remove
     */
    public void removeSecurityRole(String role) {

        synchronized (securityRoles) {

            // Make sure this security role is currently present
            int n = -1;
            for (int i = 0; i < securityRoles.length; i++) {
                if (role.equals(securityRoles[i])) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified security role
            int j = 0;
            String results[] = new String[securityRoles.length - 1];
            for (int i = 0; i < securityRoles.length; i++) {
                if (i != n)
                    results[j++] = securityRoles[i];
            }
            securityRoles = results;

        }

        // Inform interested listeners
        fireContainerEvent("removeSecurityRole", role);

    }


    /**
     * Remove any servlet mapping for the specified pattern, if it exists;
     * otherwise, no action is taken.
     *
     * @param pattern URL pattern of the mapping to remove
     */
    public void removeServletMapping(String pattern) {

        String name = null;
        synchronized (servletMappings) {
            name = (String) servletMappings.remove(pattern);
        }
        Wrapper wrapper = (Wrapper) findChild(name);
        if( wrapper != null ) {
            wrapper.removeMapping(pattern);
        }
        mapper.removeWrapper(pattern);
        fireContainerEvent("removeServletMapping", pattern);

    }


    /**
     * Remove the tag library location forthe specified tag library URI.
     *
     * @param uri URI, relative to the web.xml file
     */
    public void removeTaglib(String uri) {

        synchronized (taglibs) {
            taglibs.remove(uri);
        }
        fireContainerEvent("removeTaglib", uri);
    }


    /**
     * Remove the specified welcome file name from the list recognized
     * by this Context.
     *
     * @param name Name of the welcome file to be removed
     */
    public void removeWelcomeFile(String name) {

        synchronized (welcomeFiles) {

            // Make sure this welcome file is currently present
            int n = -1;
            for (int i = 0; i < welcomeFiles.length; i++) {
                if (welcomeFiles[i].equals(name)) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified constraint
            int j = 0;
            String results[] = new String[welcomeFiles.length - 1];
            for (int i = 0; i < welcomeFiles.length; i++) {
                if (i != n)
                    results[j++] = welcomeFiles[i];
            }
            welcomeFiles = results;

        }

        // Inform interested listeners
        postWelcomeFiles();
        fireContainerEvent("removeWelcomeFile", name);

    }


    /**
     * Remove a class name from the set of LifecycleListener classes that
     * will be added to newly created Wrappers.
     *
     * @param listener Class name of a LifecycleListener class to be removed
     */
    public void removeWrapperLifecycle(String listener) {


        synchronized (wrapperLifecycles) {

            // Make sure this welcome file is currently present
            int n = -1;
            for (int i = 0; i < wrapperLifecycles.length; i++) {
                if (wrapperLifecycles[i].equals(listener)) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified constraint
            int j = 0;
            String results[] = new String[wrapperLifecycles.length - 1];
            for (int i = 0; i < wrapperLifecycles.length; i++) {
                if (i != n)
                    results[j++] = wrapperLifecycles[i];
            }
            wrapperLifecycles = results;

        }

        // Inform interested listeners
        fireContainerEvent("removeWrapperLifecycle", listener);

    }


    /**
     * Remove a class name from the set of ContainerListener classes that
     * will be added to newly created Wrappers.
     *
     * @param listener Class name of a ContainerListener class to be removed
     */
    public void removeWrapperListener(String listener) {


        synchronized (wrapperListeners) {

            // Make sure this welcome file is currently present
            int n = -1;
            for (int i = 0; i < wrapperListeners.length; i++) {
                if (wrapperListeners[i].equals(listener)) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified constraint
            int j = 0;
            String results[] = new String[wrapperListeners.length - 1];
            for (int i = 0; i < wrapperListeners.length; i++) {
                if (i != n)
                    results[j++] = wrapperListeners[i];
            }
            wrapperListeners = results;

        }

        // Inform interested listeners
        fireContainerEvent("removeWrapperListener", listener);

    }


    // --------------------------------------------------------- Public Methods


    /**
     * Configure and initialize the set of filters for this Context.
     * Return <code>true</code> if all filter initialization completed
     * successfully, or <code>false</code> otherwise.
     */
    public boolean filterStart() {

        if (log.isDebugEnabled())
            log.debug("Starting filters");
        // Instantiate and record a FilterConfig for each defined filter
        boolean ok = true;
        synchronized (filterConfigs) {
            filterConfigs.clear();
            Iterator names = filterDefs.keySet().iterator();
            while (names.hasNext()) {
                String name = (String) names.next();
                if (log.isDebugEnabled())
                    log.debug(" Starting filter '" + name + "'");
                ApplicationFilterConfig filterConfig = null;
                try {
                    filterConfig = new ApplicationFilterConfig
                      (this, (FilterDef) filterDefs.get(name));
                    filterConfigs.put(name, filterConfig);
                } catch (Throwable t) {
                    getServletContext().log
                        (sm.getString("standardContext.filterStart", name), t);
                    ok = false;
                }
            }
        }

        return (ok);

    }


    /**
     * Finalize and release the set of filters for this Context.
     * Return <code>true</code> if all filter finalization completed
     * successfully, or <code>false</code> otherwise.
     */
    public boolean filterStop() {

        if (log.isDebugEnabled())
            log.debug("Stopping filters");

        // Release all Filter and FilterConfig instances
        synchronized (filterConfigs) {
            Iterator names = filterConfigs.keySet().iterator();
            while (names.hasNext()) {
                String name = (String) names.next();
                if (log.isDebugEnabled())
                    log.debug(" Stopping filter '" + name + "'");
                ApplicationFilterConfig filterConfig =
                  (ApplicationFilterConfig) filterConfigs.get(name);
                filterConfig.release();
            }
            filterConfigs.clear();
        }
        return (true);

    }


    /**
     * Find and return the initialized <code>FilterConfig</code> for the
     * specified filter name, if any; otherwise return <code>null</code>.
     *
     * @param name Name of the desired filter
     */
    public FilterConfig findFilterConfig(String name) {

        return ((FilterConfig) filterConfigs.get(name));

    }


    /**
     * Configure the set of instantiated application event listeners
     * for this Context.  Return <code>true</code> if all listeners wre
     * initialized successfully, or <code>false</code> otherwise.
     */
    public boolean listenerStart() {

        if (log.isDebugEnabled())
            log.debug("Configuring application event listeners");

        // Instantiate the required listeners
        ClassLoader loader = getLoader().getClassLoader();
        String listeners[] = findApplicationListeners();
        Object results[] = new Object[listeners.length];
        boolean ok = true;
        for (int i = 0; i < results.length; i++) {
            if (log.isDebugEnabled())
                log.debug(" Configuring event listener class '" +
                    listeners[i] + "'");
            try {
                Class clazz = loader.loadClass(listeners[i]);
                results[i] = clazz.newInstance();
            } catch (Throwable t) {
                getServletContext().log
                    (sm.getString("standardContext.applicationListener",
                                  listeners[i]), t);
                ok = false;
            }
        }
        if (!ok) {
            log.error(sm.getString("standardContext.applicationSkipped"));
            return (false);
        }

        // Sort listeners in two arrays
        ArrayList eventListeners = new ArrayList();
        ArrayList lifecycleListeners = new ArrayList();
        for (int i = 0; i < results.length; i++) {
            if ((results[i] instanceof ServletContextAttributeListener)
                || (results[i] instanceof ServletRequestAttributeListener)
                || (results[i] instanceof ServletRequestListener)
                || (results[i] instanceof HttpSessionAttributeListener)) {
                eventListeners.add(results[i]);
            }
            if ((results[i] instanceof ServletContextListener)
                || (results[i] instanceof HttpSessionListener)) {
                lifecycleListeners.add(results[i]);
            }
        }

        setApplicationEventListeners(eventListeners.toArray());
        setApplicationLifecycleListeners(lifecycleListeners.toArray());

        // Send application start events

        if (log.isDebugEnabled())
            log.debug("Sending application start events");

        Object instances[] = getApplicationLifecycleListeners();
        if (instances == null)
            return (ok);
        ServletContextEvent event =
          new ServletContextEvent(getServletContext());
        for (int i = 0; i < instances.length; i++) {
            if (instances[i] == null)
                continue;
            if (!(instances[i] instanceof ServletContextListener))
                continue;
            ServletContextListener listener =
                (ServletContextListener) instances[i];
            try {
                fireContainerEvent("beforeContextInitialized", listener);
                listener.contextInitialized(event);
                fireContainerEvent("afterContextInitialized", listener);
            } catch (Throwable t) {
                fireContainerEvent("afterContextInitialized", listener);
                getServletContext().log
                    (sm.getString("standardContext.listenerStart",
                                  instances[i].getClass().getName()), t);
                ok = false;
            }
        }
        return (ok);

    }


    /**
     * Send an application stop event to all interested listeners.
     * Return <code>true</code> if all events were sent successfully,
     * or <code>false</code> otherwise.
     */
    public boolean listenerStop() {

        if (log.isDebugEnabled())
            log.debug("Sending application stop events");

        boolean ok = true;
        Object listeners[] = getApplicationLifecycleListeners();
        if (listeners == null)
            return (ok);
        ServletContextEvent event =
          new ServletContextEvent(getServletContext());
        for (int i = 0; i < listeners.length; i++) {
            int j = (listeners.length - 1) - i;
            if (listeners[j] == null)
                continue;
            if (!(listeners[j] instanceof ServletContextListener))
                continue;
            ServletContextListener listener =
                (ServletContextListener) listeners[j];
            try {
                fireContainerEvent("beforeContextDestroyed", listener);
                listener.contextDestroyed(event);
                fireContainerEvent("beforeContextDestroyed", listener);
            } catch (Throwable t) {
                fireContainerEvent("beforeContextDestroyed", listener);
                getServletContext().log
                    (sm.getString("standardContext.listenerStop",
                                  listeners[j].getClass().getName()), t);
                ok = false;
            }
        }
        setApplicationEventListeners(null);
        setApplicationLifecycleListeners(null);
        return (ok);

    }


    /**
     * Allocate resources, including proxy.
     * Return <code>true</code> if initialization was successfull,
     * or <code>false</code> otherwise.
     */
    public boolean resourcesStart() {

        boolean ok = true;

        Hashtable env = new Hashtable();
        if (getParent() != null)
            env.put(ProxyDirContext.HOST, getParent().getName());
        env.put(ProxyDirContext.CONTEXT, getName());

        try {
            ProxyDirContext proxyDirContext =
                new ProxyDirContext(env, webappResources);
            if (webappResources instanceof FileDirContext) {
                filesystemBased = true;
                ((FileDirContext) webappResources).setCaseSensitive
                    (isCaseSensitive());
                ((FileDirContext) webappResources).setAllowLinking
                    (isAllowLinking());
            }
            if (webappResources instanceof BaseDirContext) {
                ((BaseDirContext) webappResources).setDocBase(getBasePath());
                ((BaseDirContext) webappResources).setCached
                    (isCachingAllowed());
                ((BaseDirContext) webappResources).setCacheTTL(getCacheTTL());
                ((BaseDirContext) webappResources).setCacheMaxSize
                    (getCacheMaxSize());
                ((BaseDirContext) webappResources).allocate();
            }
            // Register the cache in JMX
            if (isCachingAllowed()) {
                ObjectName resourcesName = 
                    new ObjectName(this.getDomain() + ":type=Cache,host=" 
                                   + getHostname() + ",path=" 
                                   + (("".equals(getPath()))?"/":getPath()));
                Registry.getRegistry().registerComponent
                    (proxyDirContext.getCache(), resourcesName, null);
            }
            this.resources = proxyDirContext;
        } catch (Throwable t) {
            log.error(sm.getString("standardContext.resourcesStart"), t);
            ok = false;
        }

        return (ok);

    }


    /**
     * Deallocate resources and destroy proxy.
     */
    public boolean resourcesStop() {

        boolean ok = true;

        try {
            if (resources != null) {
                if (resources instanceof Lifecycle) {
                    ((Lifecycle) resources).stop();
                }
                if (webappResources instanceof BaseDirContext) {
                    ((BaseDirContext) webappResources).release();
                }
                // Unregister the cache in JMX
                if (isCachingAllowed()) {
                    ObjectName resourcesName = 
                        new ObjectName(this.getDomain()
                                       + ":type=Cache,host=" 
                                       + getHostname() + ",path=" 
                                       + (("".equals(getPath()))?"/"
                                          :getPath()));
                    Registry.getRegistry().unregisterComponent(resourcesName);
                }
            }
        } catch (Throwable t) {
            log.error(sm.getString("standardContext.resourcesStop"), t);
            ok = false;
        }

        this.resources = null;

        return (ok);

    }


    /**
     * Load and initialize all servlets marked "load on startup" in the
     * web application deployment descriptor.
     *
     * @param children Array of wrappers for all currently defined
     *  servlets (including those not declared load on startup)
     */
    public void loadOnStartup(Container children[]) {

        // Collect "load on startup" servlets that need to be initialized
        TreeMap map = new TreeMap();
        for (int i = 0; i < children.length; i++) {
            Wrapper wrapper = (Wrapper) children[i];
            int loadOnStartup = wrapper.getLoadOnStartup();
            if (loadOnStartup < 0)
                continue;
            if (loadOnStartup == 0)     // Arbitrarily put them last
                loadOnStartup = Integer.MAX_VALUE;
            Integer key = new Integer(loadOnStartup);
            ArrayList list = (ArrayList) map.get(key);
            if (list == null) {
                list = new ArrayList();
                map.put(key, list);
            }
            list.add(wrapper);
        }

        // Load the collected "load on startup" servlets
        Iterator keys = map.keySet().iterator();
        while (keys.hasNext()) {
            Integer key = (Integer) keys.next();
            ArrayList list = (ArrayList) map.get(key);
            Iterator wrappers = list.iterator();
            while (wrappers.hasNext()) {
                Wrapper wrapper = (Wrapper) wrappers.next();
                try {
                    wrapper.load();
                } catch (ServletException e) {
                    getServletContext().log
                        (sm.getString("standardWrapper.loadException",
                                      getName()), e);
                    // NOTE: load errors (including a servlet that throws
                    // UnavailableException from tht init() method) are NOT
                    // fatal to application startup
                }
            }
        }

    }


    /**
     * Start this Context component.
     *
     * @exception LifecycleException if a startup error occurs
     */
    public synchronized void start() throws LifecycleException {
        //if (lazy ) return;
        if (started) {
            log.info(sm.getString("containerBase.alreadyStarted", logName()));
            return;
        }
        if( !initialized ) { 
            try {
                init();
            } catch( Exception ex ) {
                throw new LifecycleException("Error initializaing ", ex);
            }
        }
        
        String logName = "tomcat." + getParent().getName() + "." +
            ("".equals(getName()) ? "ROOT" : getName()) + ".Context";
        log = org.apache.commons.logging.LogFactory.getLog(logName);

        log.debug("Starting " + logName);

        // Set JMX object name for proper pipeline registration
        preRegisterJMX();

        if ((oname != null) && 
            (Registry.getRegistry().getMBeanServer().isRegistered(oname))) {
            // As things depend on the JMX registration, the context
            // must be reregistered again once properly initialized
            Registry.getRegistry().unregisterComponent(oname);
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);

        setAvailable(false);
        setConfigured(false);
        boolean ok = true;

        // Set config file name
        File configBase = getConfigBase();
        if (configBase != null) {
            if (getConfigFile() == null) {
                File file = new File(configBase, getDefaultConfigFile());
                setConfigFile(file.getPath());
                // If the docbase is outside the appBase, we should save our
                // config
                try {
                    File appBaseFile = new File(getAppBase());
                    if (!appBaseFile.isAbsolute()) {
                        appBaseFile = new File(engineBase(), getAppBase());
                    }
                    String appBase = appBaseFile.getCanonicalPath();
                    String basePath = 
                        (new File(getBasePath())).getCanonicalPath();
                    if (!basePath.startsWith(appBase)) {
                        Server server = ServerFactory.getServer();
                        ((StandardServer) server).storeContext(this);
                    }
                } catch (Exception e) {
                    log.warn("Error storing config file", e);
                }
            } else {
                try {
                    String canConfigFile = 
                        (new File(getConfigFile())).getCanonicalPath();
                    if (!canConfigFile.startsWith
                        (configBase.getCanonicalPath())) {
                        File file = 
                            new File(configBase, getDefaultConfigFile());
                        if (copy(new File(canConfigFile), file)) {
                            setConfigFile(file.getPath());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error setting config file", e);
                }
            }
        }

        // Install DefaultContext configuration
        if (!getOverride()) {
            Container host = getParent();
            if (host instanceof StandardHost) {
                ((StandardHost)host).installDefaultContext(this);
                Container engine = host.getParent();
                if( engine instanceof StandardEngine ) {
                    ((StandardEngine)engine).installDefaultContext(this);
                }
            }
        }

        // Add missing components as necessary
        if (webappResources == null) {   // (1) Required by Loader
            if (log.isDebugEnabled())
                log.debug("Configuring default Resources");
            try {
                if ((docBase != null) && (docBase.endsWith(".war")))
                    setResources(new WARDirContext());
                else
                    setResources(new FileDirContext());
            } catch (IllegalArgumentException e) {
                log.error("Error initializing resources: " + e.getMessage());
                ok = false;
            }
        }
        if (ok) {
            if (!resourcesStart()) {
                log.error( "Error in resourceStart()");
                ok = false;
            }
        }

        // Look for a realm - that may have been configured earlier. 
        // If the realm is added after context - it'll set itself.
        if( realm == null ) {
            ObjectName realmName=null;
            try {
                realmName=new ObjectName( getEngineName() + ":type=Host,host=" + 
                        getHostname() + ",path=" + getPath());
                if( mserver.isRegistered(realmName ) ) {
                    mserver.invoke(realmName, "init", 
                            new Object[] {},
                            new String[] {}
                    );            
                }
            } catch( Throwable t ) {
                log.debug("No realm for this host " + realmName);
            }
        }
        
        if (getLoader() == null) {
            ClassLoader parent = null;
            if (getPrivileged()) {
                if (log.isDebugEnabled())
                    log.debug("Configuring privileged default Loader");
                parent = this.getClass().getClassLoader();
            } else {
                if (log.isDebugEnabled())
                    log.debug("Configuring non-privileged default Loader");
                parent = getParentClassLoader();
            }
            WebappLoader webappLoader = new WebappLoader(parent);
            webappLoader.setDelegate(getDelegate());
            setLoader(webappLoader);
        }

        // Initialize character set mapper
        getCharsetMapper();

        // Post work directory
        postWorkDirectory();

        // Validate required extensions
        boolean dependencyCheck = true;
        try {
            ExtensionValidator validator = ExtensionValidator.getInstance();
            dependencyCheck = validator.validateApplication(getResources(),
                                                            this);
        } catch (IOException ioe) {
            log.error("Error in dependencyCheck", ioe);
            dependencyCheck = false;
        }

        if (!dependencyCheck) {
            // do not make application available if depency check fails
            ok = false;
        }

        // Reading the "catalina.useNaming" environment variable
        String useNamingProperty = System.getProperty("catalina.useNaming");
        if ((useNamingProperty != null)
            && (useNamingProperty.equals("false"))) {
            useNaming = false;
        }

        if (ok && isUseNaming()) {
            if (namingContextListener == null) {
                namingContextListener = new NamingContextListener();
                namingContextListener.setDebug(getDebug());
                namingContextListener.setName(getNamingContextName());
                addLifecycleListener(namingContextListener);
            }
        }

        // Binding thread
        ClassLoader oldCCL = bindThread();

        // Standard container startup
        if (log.isDebugEnabled())
            log.debug("Processing standard container startup");

        if (ok) {

            boolean mainOk = false;
            try {

                started = true;

                // Start our subordinate components, if any
                if ((loader != null) && (loader instanceof Lifecycle))
                    ((Lifecycle) loader).start();
                if ((logger != null) && (logger instanceof Lifecycle))
                    ((Lifecycle) logger).start();

                // Unbinding thread
                unbindThread(oldCCL);

                // Binding thread
                oldCCL = bindThread();

                if ((cluster != null) && (cluster instanceof Lifecycle))
                    ((Lifecycle) cluster).start();
                if ((realm != null) && (realm instanceof Lifecycle))
                    ((Lifecycle) realm).start();
                if ((resources != null) && (resources instanceof Lifecycle))
                    ((Lifecycle) resources).start();

                // Start our child containers, if any
                Container children[] = findChildren();
                for (int i = 0; i < children.length; i++) {
                    if (children[i] instanceof Lifecycle)
                        ((Lifecycle) children[i]).start();
                }

                // Start the Valves in our pipeline (including the basic),
                // if any
                if (pipeline instanceof Lifecycle)
                    ((Lifecycle) pipeline).start();

                // Read tldListeners. XXX Option to disable
                TldConfig tldConfig = new TldConfig();
                tldConfig.setContext(this);
                tldConfig.setXmlValidation
                    (((StandardHost) getParent()).getXmlValidation());
                tldConfig.setXmlNamespaceAware
                    (((StandardHost) getParent()).getXmlNamespaceAware());
                try {
                    tldConfig.execute();
                } catch (Exception ex) {
                    log.error("Error reading tld listeners " 
                              + ex.toString(), ex);
                    //ok=false;
                }

                // Notify our interested LifecycleListeners
                lifecycle.fireLifecycleEvent(START_EVENT, null);

                // Start manager
                if ((manager != null) && (manager instanceof Lifecycle)) {
                    ((Lifecycle) getManager()).start();
                }

                // Start ContainerBackgroundProcessor thread
                super.threadStart();

                mainOk = true;

            } finally {
                // Unbinding thread
                unbindThread(oldCCL);
                if (!mainOk) {
                    // An exception occurred
                    // Register with JMX anyway, to allow management
                    registerJMX();
                }
            }

        }
        if (!getConfigured()) {
            log.error( "Error getConfigured");
            ok = false;
        }

        // We put the resources into the servlet context
        if (ok)
            getServletContext().setAttribute
                (Globals.RESOURCES_ATTR, getResources());

        // Binding thread
        oldCCL = bindThread();

        // Create context attributes that will be required
        if (ok) {
            if (log.isDebugEnabled())
                log.debug("Posting standard context attributes");
            postWelcomeFiles();
        }

        // Configure and call application event listeners and filters
        if (ok) {
            if (!listenerStart()) {
                log.error( "Error listenerStart");
                ok = false;
            }
        }
        if (ok) {
            if (!filterStart()) {
                log.error( "Error filterStart");
                ok = false;
            }
        }

        // Load and initialize all "load on startup" servlets
        if (ok) {
            loadOnStartup(findChildren());
        }

        // Unbinding thread
        unbindThread(oldCCL);

        // Initialize associated mapper
        mapper.setContext(getPath(), welcomeFiles, resources);

        // Set available status depending upon startup success
        if (ok) {
            if (log.isDebugEnabled())
                log.debug("Starting completed");
            setAvailable(true);
        } else {
            log.error(sm.getString("standardContext.startFailed"));
            try {
                stop();
            } catch (Throwable t) {
                log.error(sm.getString("standardContext.startCleanup"), t);
            }
            setAvailable(false);
        }

        // JMX registration
        registerJMX();

        if (ok) {
            // Notify our interested LifecycleListeners
            lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);
        }

        startTime=System.currentTimeMillis();
        
        // Send j2ee.state.running notification 
        if (ok && (this.getObjectName() != null)) {
            Notification notification = 
                new Notification("j2ee.state.running", this.getObjectName(), 
                                sequenceNumber++);
            broadcaster.sendNotification(notification);
        }

        // Close all JARs right away to avoid always opening a peak number 
        // of files on startup
        if (getLoader() instanceof WebappLoader) {
            ((WebappLoader) getLoader()).closeJARs(true);
        }

        // Reinitializing if something went wrong
        if (!ok && started) {
            stop();
        }

        //cacheContext();
    }
    
    private void cacheContext() {
        try {
            File workDir=new File( getWorkPath() );
            
            File ctxSer=new File( workDir, "_tomcat_context.ser");
            FileOutputStream fos=new FileOutputStream( ctxSer );
            ObjectOutputStream oos=new ObjectOutputStream( fos );
            oos.writeObject(this);
            oos.close();
            fos.close();
        } catch( Throwable t ) {
            log.info("Error saving context.ser ", t);
        }
    }

    /**
     * Stop this Context component. Experimental, please ignore.
     *
     * @exception LifecycleException if a shutdown error occurs
     */
    public synchronized void stopNew() throws LifecycleException {
        // Mark this application as unavailable while we shut down
        setAvailable(false);

        // Binding thread
        ClassLoader oldCCL = bindThread();

        try {
            // Stop our filters
            filterStop();
            
            // Finalize our character set mapper
            setCharsetMapper(null);
            
            // Stop our application listeners
            listenerStop();
            
            // Stop resources
            resourcesStop();
            
            super.stop();
        } finally {
            
            // Unbinding thread
            unbindThread(oldCCL);
            
        }
        
        // Reset application context
        context = null;
        
        wrappers = new ArrayList();
    }

    /**
     * Stop this Context component.
     *
     * @exception LifecycleException if a shutdown error occurs
     */
    public synchronized void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException
                (sm.getString("containerBase.notStarted", logName()));

        if (log.isDebugEnabled())
            log.debug("Stopping");

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);
        
        // Send j2ee.state.stopping notification 
        if (this.getObjectName() != null) {
            Notification notification = 
                new Notification("j2ee.state.stopping", this.getObjectName(), 
                                sequenceNumber++);
            broadcaster.sendNotification(notification);
        }
        
        // Mark this application as unavailable while we shut down
        setAvailable(false);

        // Binding thread
        ClassLoader oldCCL = bindThread();

        // Stop our filters
        filterStop();

        // Stop our application listeners
        listenerStop();

        // Finalize our character set mapper
        setCharsetMapper(null);

        // Stop ContainerBackgroundProcessor thread
        super.threadStop();

        if ((manager != null) && (manager instanceof Lifecycle)) {
            ((Lifecycle) manager).stop();
        }

        // Normal container shutdown processing
        if (log.isDebugEnabled())
            log.debug("Processing standard container shutdown");
        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        try {

            // Stop the Valves in our pipeline (including the basic), if any
            if (pipeline instanceof Lifecycle) {
                ((Lifecycle) pipeline).stop();
            }

            // Stop our child containers, if any
            Container[] children = findChildren();
            for (int i = 0; i < children.length; i++) {
                if (children[i] instanceof Lifecycle)
                    ((Lifecycle) children[i]).stop();
            }

            // Stop resources
            resourcesStop();

            if ((realm != null) && (realm instanceof Lifecycle)) {
                ((Lifecycle) realm).stop();
            }
            if ((cluster != null) && (cluster instanceof Lifecycle)) {
                ((Lifecycle) cluster).stop();
            }
            if ((logger != null) && (logger instanceof Lifecycle)) {
                ((Lifecycle) logger).stop();
            }
            if ((loader != null) && (loader instanceof Lifecycle)) {
                ((Lifecycle) loader).stop();
            }

        } finally {

            // Unbinding thread
            unbindThread(oldCCL);

        }

        // Send j2ee.state.stopped notification 
        if (this.getObjectName() != null) {
            Notification notification = 
                new Notification("j2ee.state.stopped", this.getObjectName(), 
                                sequenceNumber++);
            broadcaster.sendNotification(notification);
        }
        
        // Reset application context
        context = null;

        wrappers = new ArrayList();

        // This object will no longer be visible or used. 
        try {
            resetContext();
        } catch( Exception ex ) {
            log.error( "Error reseting context " + this + " " + ex, ex );
        }
        
        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);

        if (log.isDebugEnabled())
            log.debug("Stopping complete");

    }

    /** Destroy needs to clean up the context completely.
     * 
     * The problem is that undoing all the config in start() and restoring 
     * a 'fresh' state is impossible. After stop()/destroy()/init()/start()
     * we should have the same state as if a fresh start was done - i.e
     * read modified web.xml, etc. This can only be done by completely 
     * removing the context object and remapping a new one, or by cleaning
     * up everything.
     * 
     * XXX Should this be done in stop() ?
     * 
     */ 
    public void destroy() throws Exception {
        if( oname != null ) { 
            // Send j2ee.object.deleted notification 
            Notification notification = 
                new Notification("j2ee.object.deleted", this.getObjectName(), 
                                sequenceNumber++);
            broadcaster.sendNotification(notification);
        } 
        super.destroy();
    }
    
    private void resetContext() throws Exception, MBeanRegistrationException {
        // Restore the original state ( pre reading web.xml in start )
        // If you extend this - override this method and make sure to clean up
        children=new HashMap();
        log.debug("resetContext " + oname + " " + mserver);
    }

    /**
     * Return a String representation of this component.
     */
    public String toString() {

        StringBuffer sb = new StringBuffer();
        if (getParent() != null) {
            sb.append(getParent().toString());
            sb.append(".");
        }
        sb.append("StandardContext[");
        sb.append(getName());
        sb.append("]");
        return (sb.toString());

    }


    /**
     * Execute a periodic task, such as reloading, etc. This method will be
     * invoked inside the classloading context of this container. Unexpected
     * throwables will be caught and logged.
     */
    public void backgroundProcess() {

        if (!started)
            return;

        count = (count + 1) % managerChecksFrequency;

        if ((getManager() != null) && (count == 0)) {
            try {
                getManager().backgroundProcess();
            } catch ( Exception x ) {
                log.warn("Unable to perform background process on manager",x);
            }
        }

        if (getLoader() != null) {
            if (reloadable && (getLoader().modified())) {
                try {
                    Thread.currentThread().setContextClassLoader
                        (StandardContext.class.getClassLoader());
                    reload();
                } finally {
                    if (getLoader() != null) {
                        Thread.currentThread().setContextClassLoader
                            (getLoader().getClassLoader());
                    }
                }
            }
            if (getLoader() instanceof WebappLoader) {
                ((WebappLoader) getLoader()).closeJARs(false);
            }
        }

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Adjust the URL pattern to begin with a leading slash, if appropriate
     * (i.e. we are running a servlet 2.2 application).  Otherwise, return
     * the specified URL pattern unchanged.
     *
     * @param urlPattern The URL pattern to be adjusted (if needed)
     *  and returned
     */
    protected String adjustURLPattern(String urlPattern) {

        if (urlPattern == null)
            return (urlPattern);
        if (urlPattern.startsWith("/") || urlPattern.startsWith("*."))
            return (urlPattern);
        if (!isServlet22())
            return (urlPattern);
        log.debug(sm.getString("standardContext.urlPattern.patternWarning",
                         urlPattern));
        return ("/" + urlPattern);

    }


    /**
     * Are we processing a version 2.2 deployment descriptor?
     */
    protected boolean isServlet22() {

        if (this.publicId == null)
            return (false);
        if (this.publicId.equals
            (org.apache.catalina.startup.Constants.WebDtdPublicId_22))
            return (true);
        else
            return (false);

    }


    /**
     * Return a File object representing the base directory for the
     * entire servlet container (i.e. the Engine container if present).
     */
    protected File engineBase() {
        String base=System.getProperty("catalina.base");
        if( base == null ) {
            StandardEngine eng=(StandardEngine)this.getParent().getParent();
            base=eng.getBaseDir();
        }
        return (new File(base));
    }


    // -------------------------------------------------------- Private Methods


    /**
     * Bind current thread, both for CL purposes and for JNDI ENC support
     * during : startup, shutdown and realoading of the context.
     *
     * @return the previous context class loader
     */
    private ClassLoader bindThread() {

        ClassLoader oldContextClassLoader =
            Thread.currentThread().getContextClassLoader();

        if (getResources() == null)
            return oldContextClassLoader;

        Thread.currentThread().setContextClassLoader
            (getLoader().getClassLoader());

        DirContextURLStreamHandler.bind(getResources());

        if (isUseNaming()) {
            try {
                ContextBindings.bindThread(this, this);
            } catch (NamingException e) {
                // Silent catch, as this is a normal case during the early
                // startup stages
            }
        }

        return oldContextClassLoader;

    }


    /**
     * Unbind thread.
     */
    private void unbindThread(ClassLoader oldContextClassLoader) {

        Thread.currentThread().setContextClassLoader(oldContextClassLoader);

        oldContextClassLoader = null;

        if (isUseNaming()) {
            ContextBindings.unbindThread(this, this);
        }

        DirContextURLStreamHandler.unbind();

    }



    /**
     * Get base path.
     */
    private String getBasePath() {
        String docBase = null;
        Container container = this;
        while (container != null) {
            if (container instanceof Host)
                break;
            container = container.getParent();
        }
        File file = new File(getDocBase());
        if (!file.isAbsolute()) {
            if (container == null) {
                docBase = (new File(engineBase(), getDocBase())).getPath();
            } else {
                // Use the "appBase" property of this container
                String appBase = ((Host) container).getAppBase();
                file = new File(appBase);
                if (!file.isAbsolute())
                    file = new File(engineBase(), appBase);
                docBase = (new File(file, getDocBase())).getPath();
            }
        } else {
            docBase = file.getPath();
        }
        return docBase;
    }


    /**
     * Get app base.
     */
    private String getAppBase() {
        String appBase = null;
        Container container = this;
        while (container != null) {
            if (container instanceof Host)
                break;
            container = container.getParent();
        }
        if (container != null) {
            appBase = ((Host) container).getAppBase();
        }
        return appBase;
    }


    /**
     * Get config base.
     */
    private File getConfigBase() {
        File configBase = 
            new File(System.getProperty("catalina.base"), "conf");
        if (!configBase.exists()) {
            return null;
        }
        Container container = this;
        Container host = null;
        Container engine = null;
        while (container != null) {
            if (container instanceof Host)
                host = container;
            if (container instanceof Engine)
                engine = container;
            container = container.getParent();
        }
        if (engine != null) {
            configBase = new File(configBase, engine.getName());
        }
        if (host != null) {
            configBase = new File(configBase, host.getName());
        }
        configBase.mkdirs();
        return configBase;
    }


    /**
     * Given a context path, get the config file name.
     */
    protected String getDefaultConfigFile() {
        String basename = null;
        String path = getPath();
        if (path.equals("")) {
            basename = "ROOT";
        } else {
            basename = path.substring(1).replace('/', '_');
        }
        return (basename + ".xml");
    }


    /**
     * Copy a file.
     */
    private boolean copy(File src, File dest) {
        FileInputStream is = null;
        FileOutputStream os = null;
        try {
            is = new FileInputStream(src);
            os = new FileOutputStream(dest);
            byte[] buf = new byte[4096];
            while (true) {
                int len = is.read(buf);
                if (len < 0)
                    break;
                os.write(buf, 0, len);
            }
            is.close();
            os.close();
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception e) {
                // Ignore
            }
            try {
                if (os != null) {
                    os.close();
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return true;
    }


    /**
     * Get naming context full name.
     */
    private String getNamingContextName() {
    if (namingContextName == null) {
        Container parent = getParent();
        if (parent == null) {
        namingContextName = getName();
        } else {
        Stack stk = new Stack();
        StringBuffer buff = new StringBuffer();
        while (parent != null) {
            stk.push(parent.getName());
            parent = parent.getParent();
        }
        while (!stk.empty()) {
            buff.append("/" + stk.pop());
        }
        buff.append(getName());
        namingContextName = buff.toString();
        }
    }
    return namingContextName;
    }


    /**
     * Return the request processing paused flag for this Context.
     */
    public boolean getPaused() {

        return (this.paused);

    }


    /**
     * Post a copy of our web application resources as a servlet context
     * attribute.
     */
    private void postResources() {

        getServletContext().setAttribute
            (Globals.RESOURCES_ATTR, getResources());

    }


    /**
     * Post a copy of our current list of welcome files as a servlet context
     * attribute, so that the default servlet can find them.
     */
    private void postWelcomeFiles() {

        getServletContext().setAttribute("org.apache.catalina.WELCOME_FILES",
                                         welcomeFiles);

    }

    public String getHostname() {
        Container parentHost = getParent();
        if (parentHost != null) {
            hostName = parentHost.getName();
        }
        if ((hostName == null) || (hostName.length() < 1))
            hostName = "_";
        return hostName;
    }

    /**
     * Set the appropriate context attribute for our work directory.
     */
    private void postWorkDirectory() {

        // Acquire (or calculate) the work directory path
        String workDir = getWorkDir();
        if (workDir == null) {

            // Retrieve our parent (normally a host) name
            String hostName = null;
            String engineName = null;
            String hostWorkDir = null;
            Container parentHost = getParent();
            if (parentHost != null) {
                hostName = parentHost.getName();
                if (parentHost instanceof StandardHost) {
                    hostWorkDir = ((StandardHost)parentHost).getWorkDir();
                }
                Container parentEngine = parentHost.getParent();
                if (parentEngine != null) {
                   engineName = parentEngine.getName();
                }
            }
            if ((hostName == null) || (hostName.length() < 1))
                hostName = "_";
            if ((engineName == null) || (engineName.length() < 1))
                engineName = "_";

            String temp = getPath();
            if (temp.startsWith("/"))
                temp = temp.substring(1);
            temp = temp.replace('/', '_');
            temp = temp.replace('\\', '_');
            if (temp.length() < 1)
                temp = "_";
            if (hostWorkDir != null ) {
                workDir = hostWorkDir + File.separator + temp;
            } else {
                workDir = "work" + File.separator + engineName +
                    File.separator + hostName + File.separator + temp;
            }
            setWorkDir(workDir);
        }

        // Create this directory if necessary
        File dir = new File(workDir);
        if (!dir.isAbsolute()) {
            File catalinaHome = engineBase();
            String catalinaHomePath = null;
            try {
                catalinaHomePath = catalinaHome.getCanonicalPath();
                dir = new File(catalinaHomePath, workDir);
            } catch (IOException e) {
            }
        }
        dir.mkdirs();

        // Set the appropriate servlet context attribute
        getServletContext().setAttribute(Globals.WORK_DIR_ATTR, dir);
        if (getServletContext() instanceof ApplicationContext)
            ((ApplicationContext) getServletContext()).setAttributeReadOnly
                (Globals.WORK_DIR_ATTR);

    }


    /**
     * Set the request processing paused flag for this Context.
     *
     * @param paused The new request processing paused flag
     */
    private void setPaused(boolean paused) {

        this.paused = paused;

    }


    /**
     * Validate the syntax of a proposed <code>&lt;url-pattern&gt;</code>
     * for conformance with specification requirements.
     *
     * @param urlPattern URL pattern to be validated
     */
    private boolean validateURLPattern(String urlPattern) {

        if (urlPattern == null)
            return (false);
        if (urlPattern.startsWith("*.")) {
            if (urlPattern.indexOf('/') < 0)
                return (true);
            else
                return (false);
        }
        if ( (urlPattern.startsWith("/")) &&
                (urlPattern.indexOf("*.") < 0))
            return (true);
        else
            return (false);

    }


    // -------------------- JMX methods  --------------------

    /**
     * Return the MBean Names of the set of defined environment entries for
     * this web application
     */
    public String[] getEnvironments() {
        ContextEnvironment[] envs = getNamingResources().findEnvironments();
        ArrayList results = new ArrayList();
        for (int i = 0; i < envs.length; i++) {
            try {
                ObjectName oname =
                    MBeanUtils.createObjectName(this.getEngineName(), envs[i]);
                results.add(oname.toString());
            } catch (MalformedObjectNameException e) {
                throw new IllegalArgumentException
                    ("Cannot create object name for environment " + envs[i]);
            }
        }
        return ((String[]) results.toArray(new String[results.size()]));

    }


    /**
     * Return the MBean Names of all the defined resource references for this
     * application.
     */
    public String[] getResourceNames() {

        ContextResource[] resources = getNamingResources().findResources();
        ArrayList results = new ArrayList();
        for (int i = 0; i < resources.length; i++) {
            try {
                ObjectName oname =
                    MBeanUtils.createObjectName(this.getEngineName(), resources[i]);
                results.add(oname.toString());
            } catch (MalformedObjectNameException e) {
                throw new IllegalArgumentException
                    ("Cannot create object name for resource " + resources[i]);
            }
        }
        return ((String[]) results.toArray(new String[results.size()]));

    }


    /**
     * Return the MBean Names of all the defined resource links for this
     * application
     */
    public String[] getResourceLinks() {

        ContextResourceLink[] links = getNamingResources().findResourceLinks();
        ArrayList results = new ArrayList();
        for (int i = 0; i < links.length; i++) {
            try {
                ObjectName oname =
                    MBeanUtils.createObjectName(this.getEngineName(), links[i]);
                results.add(oname.toString());
            } catch (MalformedObjectNameException e) {
                throw new IllegalArgumentException
                    ("Cannot create object name for resource " + links[i]);
            }
        }
        return ((String[]) results.toArray(new String[results.size()]));

    }

    // ------------------------------------------------------------- Operations


    /**
     * Add an environment entry for this web application.
     *
     * @param envName New environment entry name
     */
    public String addEnvironment(String envName, String type)
        throws MalformedObjectNameException {

        NamingResources nresources = getNamingResources();
        if (nresources == null) {
            return null;
        }
        ContextEnvironment env = nresources.findEnvironment(envName);
        if (env != null) {
            throw new IllegalArgumentException
                ("Invalid environment name - already exists '" + envName + "'");
        }
        env = new ContextEnvironment();
        env.setName(envName);
        env.setType(type);
        nresources.addEnvironment(env);

        // Return the corresponding MBean name
        ManagedBean managed = Registry.getRegistry().findManagedBean("ContextEnvironment");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), env);
        return (oname.toString());

    }


    /**
     * Add a resource reference for this web application.
     *
     * @param resourceName New resource reference name
     */
    public String addResource(String resourceName, String type)
        throws MalformedObjectNameException {

        NamingResources nresources = getNamingResources();
        if (nresources == null) {
            return null;
        }
        ContextResource resource = nresources.findResource(resourceName);
        if (resource != null) {
            throw new IllegalArgumentException
                ("Invalid resource name - already exists'" + resourceName + "'");
        }
        resource = new ContextResource();
        resource.setName(resourceName);
        resource.setType(type);
        nresources.addResource(resource);

        // Return the corresponding MBean name
        ManagedBean managed = Registry.getRegistry().findManagedBean("ContextResource");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), resource);
        return (oname.toString());
    }


    /**
     * Add a resource link for this web application.
     *
     * @param resourceLinkName New resource link name
     */
    public String addResourceLink(String resourceLinkName, String global,
                String name, String type) throws MalformedObjectNameException {

        NamingResources nresources = getNamingResources();
        if (nresources == null) {
            return null;
        }
        ContextResourceLink resourceLink =
                                nresources.findResourceLink(resourceLinkName);
        if (resourceLink != null) {
            throw new IllegalArgumentException
                ("Invalid resource link name - already exists'" +
                                                        resourceLinkName + "'");
        }
        resourceLink = new ContextResourceLink();
        resourceLink.setGlobal(global);
        resourceLink.setName(resourceLinkName);
        resourceLink.setType(type);
        nresources.addResourceLink(resourceLink);

        // Return the corresponding MBean name
        ManagedBean managed = Registry.getRegistry().findManagedBean("ContextResourceLink");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), resourceLink);
        return (oname.toString());
    }
    
    
    /** JSR77 deploymentDescriptor attribute
     *
     * @return string deployment descriptor 
     */
    public String getDeploymentDescriptor() {
    
        InputStream stream = null;
        ServletContext servletContext = getServletContext();
        if (servletContext != null) {
            stream = servletContext.getResourceAsStream(
                org.apache.catalina.startup.Constants.ApplicationWebXml);
        }
        if (stream == null) {
            return "";
        }
        BufferedReader br = new BufferedReader(
                                new InputStreamReader(stream));
        StringBuffer sb = new StringBuffer();
        String strRead = "";
        try {
            while (strRead != null) {
                sb.append(strRead);
                strRead = br.readLine();
            }
        } catch (IOException e) {
            return "";
        }

        return sb.toString(); 
    
    }
    
    
    /** JSR77 servlets attribute
     *
     * @return list of all servlets ( we know about )
     */
    public String[] getServlets() {
        int size=wrappers.size();
        String result[]=new String[size];
        for( int i=0; i< size; i++ ) {
            result[i]=((StandardWrapper)wrappers.get(i)).getObjectName();
        }
        return result;
    }
    

    public ObjectName createObjectName(String hostDomain, ObjectName parentName)
            throws MalformedObjectNameException
    {
        String onameStr;
        StandardHost hst=(StandardHost)getParent();
        
        String pathName=getName();
        String hostName=getParent().getName();
        String name= "//" + ((hostName==null)? "DEFAULT" : hostName) +
                (("".equals(pathName))?"/":pathName );

        String suffix=",J2EEApplication=" +
                getJ2EEApplication() + ",J2EEServer=" +
                getJ2EEServer();

        onameStr="j2eeType=WebModule,name=" + name + suffix;
        if( log.isDebugEnabled())
            log.debug("Registering " + onameStr + " for " + oname);
        
        // default case - no domain explictely set.
        if( getDomain() == null ) domain=hst.getDomain();

        ObjectName oname=new ObjectName(getDomain() + ":" + onameStr);
        return oname;        
    }    
    
    private void preRegisterJMX() {
        try {
            StandardHost host = (StandardHost) getParent();
            if ((oname == null) 
                || (oname.getKeyProperty("j2eeType") == null)) {
                oname = createObjectName(host.getDomain(), host.getJmxName());
                controller = oname;
            }
        } catch(Exception ex) {
            log.info("Error registering ctx with jmx " + this + " " +
                     oname + " " + ex.toString(), ex );
        }
    }

    private void registerJMX() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Checking for " + oname );
            }
            if(! Registry.getRegistry().getMBeanServer().isRegistered(oname)) {
                controller = oname;
                Registry.getRegistry().registerComponent(this, oname, null);
                
                // Send j2ee.object.created notification 
                if (this.getObjectName() != null) {
                    Notification notification = new Notification(
                                                        "j2ee.object.created", 
                                                        this.getObjectName(), 
                                                        sequenceNumber++);
                    broadcaster.sendNotification(notification);
                }
            }
            for (Iterator it = wrappers.iterator(); it.hasNext() ; ) {
                StandardWrapper wrapper=(StandardWrapper)it.next();
                // XXX prevent duplicated registration
                wrapper.registerJMX( this );
            }
        } catch (Exception ex) {
            log.info("Error registering wrapper with jmx " + this + " " +
                    oname + " " + ex.toString(), ex );
        }
    }

    /** There are 2 cases:
     *   1.The context is created and registered by internal APIS
     *   2. The context is created by JMX, and it'll self-register.
     *
     * @param server
     * @param name
     * @return
     * @throws Exception
     */

    public ObjectName preRegister(MBeanServer server,
                                  ObjectName name)
            throws Exception
    {
        if( oname != null ) {
            //log.info( "Already registered " + oname + " " + name);
            // Temporary - /admin uses the old names
            return name;
        }
        ObjectName result=super.preRegister(server,name);
        return name;
    }

    public void preDeregister() throws Exception {
        if( started ) {
            try {
                stop();
            } catch( Exception ex ) {
                log.error( "error stopping ", ex);
            }
        }
    }

    public void init() throws Exception {

        if( this.getParent() == null ) {
            ObjectName parentName=getParentName();
            
            if( ! mserver.isRegistered(parentName)) {
                log.debug("No host, creating one " + parentName);
                StandardHost host=new StandardHost();
                host.setName(hostName);
                host.setAutoDeploy(false);
                Registry.getRegistry().registerComponent(host, parentName, null);
                mserver.invoke(parentName, "init", new Object[] {}, new String[] {} );
            }
            ContextConfig config = new ContextConfig();
            this.addLifecycleListener(config);

            log.debug( "AddChild " + parentName + " " + this);
            try {
                mserver.invoke(parentName, "addChild", new Object[] { this },
                               new String[] {"org.apache.catalina.Container"});
            } catch (Exception e) {
                destroy();
                throw e;
            }
        }
        super.init();
        
        // Send j2ee.state.starting notification 
        if (this.getObjectName() != null) {
            Notification notification = new Notification("j2ee.state.starting", 
                                                        this.getObjectName(), 
                                                        sequenceNumber++);
            broadcaster.sendNotification(notification);
        }
        
    }

    public ObjectName getParentName() throws MalformedObjectNameException {
        // "Life" update
        String path=oname.getKeyProperty("name");
        if( path == null ) {
            log.error( "No name attribute " +name );
            return null;
        }
        if( ! path.startsWith( "//")) {
            log.error("Invalid name " + name);
        }
        path=path.substring(2);
        int delim=path.indexOf( "/" );
        hostName="localhost"; // Should be default...
        if( delim > 0 ) {
            hostName=path.substring(0, delim);
            path = path.substring(delim);
            if (path.equals("/")) {
                this.setName("");
            } else {
                this.setName(path);
            }
        } else {
            log.debug("Setting path " +  path );
            this.setName( path );
        }
        // XXX The service and domain should be the same.
        String parentDomain=getEngineName();
        if( parentDomain == null ) parentDomain=domain;
        ObjectName parentName=new ObjectName( parentDomain + ":" +
                "type=Host,host=" + hostName);
        return parentName;
    }
    
    public void create() throws Exception{
        init();
    }

    // ------------------------------------------------------------- Attributes


    /**
     * Return the naming resources associated with this web application.
     */
    public javax.naming.directory.DirContext getStaticResources() {

        return getResources();

    }


    /**
     * Return the naming resources associated with this web application.
     * FIXME: Fooling introspection ... 
     */
    public javax.naming.directory.DirContext findStaticResources() {

        return getResources();

    }


    /**
     * Return the naming resources associated with this web application.
     */
    public String[] getWelcomeFiles() {

        return findWelcomeFiles();

    }



    /** Support for "stateManageable" JSR77 
     * 
     */
    public boolean isStateManageable() {
        return true;
    }
    
    public void startRecursive() throws LifecycleException {
        // nothing to start recursive, the servlets will be started by load-on-startup
        start();
    }
    
    public int getState() {
        if( started ) {
            return 1; // RUNNING
        }
        if( initialized ) {
            return 0; // starting ? 
        }
        if( ! available ) { 
            return 4; //FAILED
        }
        // 2 - STOPPING
        return 3; // STOPPED
    }
    
    /**
     * The J2EE Server ObjectName this module is deployed on.
     */     
    private String server = null;
    
    /**
     * The Java virtual machines on which this module is running.
     */       
    private String[] javaVMs = null;
    
    public String getServer() {
        return server;
    }
        
    public String setServer(String server) {
        return this.server=server;
    }
        
    public String[] getJavaVMs() {
        return javaVMs;
    }
        
    public String[] setJavaVMs(String[] javaVMs) {
        return this.javaVMs = javaVMs;
    }
    
    public long getStartTime() {
        return startupTime;
    }
    
    public boolean isEventProvider() {
        return false;
    }
    
    public boolean isStatisticsProvider() {
        return false;
    }
    
}
