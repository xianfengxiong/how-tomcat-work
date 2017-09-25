/*
 * $Header: /home/cvs/jakarta-tomcat-catalina/catalina/src/share/org/apache/catalina/startup/HostConfig.java,v 1.27 2004/01/13 16:57:16 remm Exp $
 * $Revision: 1.27 $
 * $Date: 2004/01/13 16:57:16 $
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


package org.apache.catalina.startup;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Deployer;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.util.StringManager;
import org.apache.naming.resources.ResourceAttributes;


/**
 * Startup event listener for a <b>Host</b> that configures the properties
 * of that Host, and the associated defined contexts.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.27 $ $Date: 2004/01/13 16:57:16 $
 */

public class HostConfig
    implements LifecycleListener {
    
    private static org.apache.commons.logging.Log log=
         org.apache.commons.logging.LogFactory.getLog( HostConfig.class );

    // ----------------------------------------------------- Instance Variables


    /**
     * App base.
     */
    private File appBase = null;


    /**
     * Config base.
     */
    private File configBase = null;


    /**
     * The Java class name of the Context configuration class we should use.
     */
    protected String configClass = "org.apache.catalina.startup.ContextConfig";


    /**
     * The Java class name of the Context implementation we should use.
     */
    protected String contextClass = "org.apache.catalina.core.StandardContext";


    /**
     * The debugging detail level for this component.
     */
    protected int debug = 0;


    /**
     * The names of applications that we have auto-deployed (to avoid
     * double deployment attempts).
     */
    protected ArrayList deployed = new ArrayList();


    /**
     * The Host we are associated with.
     */
    protected Host host = null;


    /**
     * The string resources for this package.
     */
    protected static final StringManager sm =
        StringManager.getManager(Constants.Package);


    /**
     * Should we deploy XML Context config files?
     */
    private boolean deployXML = false;


    /**
     * Should we unpack WAR files when auto-deploying applications in the
     * <code>appBase</code> directory?
     */
    private boolean unpackWARs = false;


    /**
     * Last modified dates of the web.xml files of the contexts, keyed by
     * context name.
     */
    private HashMap webXmlLastModified = new HashMap();


    /**
     * Last modified dates of the Context xml files of the contexts, keyed by
     * context name.
     */
    private HashMap contextXmlLastModified = new HashMap();


    /**
     * Last modified dates of the source WAR files, keyed by WAR name.
     */
    private HashMap warLastModified = new HashMap();


    /**
     * Attribute value used to turn on/off XML validation
     */
    private boolean xmlValidation = false;


    /**
     * Attribute value used to turn on/off XML namespace awarenes.
     */
    private boolean xmlNamespaceAware = false;


    // ------------------------------------------------------------- Properties


    /**
     * Return the Context configuration class name.
     */
    public String getConfigClass() {

        return (this.configClass);

    }


    /**
     * Set the Context configuration class name.
     *
     * @param configClass The new Context configuration class name.
     */
    public void setConfigClass(String configClass) {

        this.configClass = configClass;

    }


    /**
     * Return the Context implementation class name.
     */
    public String getContextClass() {

        return (this.contextClass);

    }


    /**
     * Set the Context implementation class name.
     *
     * @param contextClass The new Context implementation class name.
     */
    public void setContextClass(String contextClass) {

        this.contextClass = contextClass;

    }


    /**
     * Return the debugging detail level for this component.
     */
    public int getDebug() {

        return (this.debug);

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
     * Return the deploy XML config file flag for this component.
     */
    public boolean isDeployXML() {

        return (this.deployXML);

    }


    /**
     * Set the deploy XML config file flag for this component.
     *
     * @param deployXML The new deploy XML flag
     */
    public void setDeployXML(boolean deployXML) {

        this.deployXML= deployXML;

    }


    /**
     * Return the unpack WARs flag.
     */
    public boolean isUnpackWARs() {

        return (this.unpackWARs);

    }


    /**
     * Set the unpack WARs flag.
     *
     * @param unpackWARs The new unpack WARs flag
     */
    public void setUnpackWARs(boolean unpackWARs) {

        this.unpackWARs = unpackWARs;

    }
    
    
     /**
     * Set the validation feature of the XML parser used when
     * parsing xml instances.
     * @param xmlValidation true to enable xml instance validation
     */
    public void setXmlValidation(boolean xmlValidation){
        this.xmlValidation = xmlValidation;
    }

    /**
     * Get the server.xml <host> attribute's xmlValidation.
     * @return true if validation is enabled.
     *
     */
    public boolean getXmlValidation(){
        return xmlValidation;
    }

    /**
     * Get the server.xml <host> attribute's xmlNamespaceAware.
     * @return true if namespace awarenes is enabled.
     *
     */
    public boolean getXmlNamespaceAware(){
        return xmlNamespaceAware;
    }


    /**
     * Set the namespace aware feature of the XML parser used when
     * parsing xml instances.
     * @param xmlNamespaceAware true to enable namespace awareness
     */
    public void setXmlNamespaceAware(boolean xmlNamespaceAware){
        this.xmlNamespaceAware=xmlNamespaceAware;
    }    


    // --------------------------------------------------------- Public Methods


    /**
     * Process the START event for an associated Host.
     *
     * @param event The lifecycle event that has occurred
     */
    public void lifecycleEvent(LifecycleEvent event) {

        if (event.getType().equals("check"))
            check();

        // Identify the host we are associated with
        try {
            host = (Host) event.getLifecycle();
            if (host instanceof StandardHost) {
                int hostDebug = ((StandardHost) host).getDebug();
                if (hostDebug > this.debug) {
                    this.debug = hostDebug;
                }
                setDeployXML(((StandardHost) host).isDeployXML());
                setUnpackWARs(((StandardHost) host).isUnpackWARs());
                setXmlNamespaceAware(((StandardHost) host).getXmlNamespaceAware());
                setXmlValidation(((StandardHost) host).getXmlValidation());
            }
        } catch (ClassCastException e) {
            log.error(sm.getString("hostConfig.cce", event.getLifecycle()), e);
            return;
        }

        // Process the event that has occurred
        if (event.getType().equals(Lifecycle.START_EVENT))
            start();
        else if (event.getType().equals(Lifecycle.STOP_EVENT))
            stop();

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Return a File object representing the "application root" directory
     * for our associated Host.
     */
    protected File appBase() {

        if (appBase != null) {
            return appBase;
        }

        File file = new File(host.getAppBase());
        if (!file.isAbsolute())
            file = new File(System.getProperty("catalina.base"),
                            host.getAppBase());
        try {
            appBase = file.getCanonicalFile();
        } catch (IOException e) {
            appBase = file;
        }
        return (appBase);

    }


    /**
     * Return a File object representing the "configuration root" directory
     * for our associated Host.
     */
    protected File configBase() {

        if (configBase != null) {
            return configBase;
        }

        File file = new File(System.getProperty("catalina.base"), "conf");
        Container parent = host.getParent();
        if ((parent != null) && (parent instanceof Engine)) {
            file = new File(file, parent.getName());
        }
        file = new File(file, host.getName());
        try {
            configBase = file.getCanonicalFile();
        } catch (IOException e) {
            configBase = file;
        }
        return (configBase);

    }


    /**
     * Deploy applications for any directories or WAR files that are found
     * in our "application root" directory.
     */
    protected void deployApps() {

        if (!(host instanceof Deployer))
            return;

        // Initialize the deployer
        ((Deployer) host).findDeployedApps();

        File appBase = appBase();
        if (!appBase.exists() || !appBase.isDirectory())
            return;
        File configBase = configBase();
        if (configBase.exists() && configBase.isDirectory()) {
            String configFiles[] = configBase.list();
            deployDescriptors(configBase, configFiles);
        }

        String files[] = appBase.list();
        deployWARs(appBase, files);
        deployDirectories(appBase, files);

    }


    /**
     * Deploy XML context descriptors.
     */
    protected void deployDescriptors(File configBase, String[] files) {

        if (!deployXML)
           return;

        for (int i = 0; i < files.length; i++) {

            if (files[i].equalsIgnoreCase("META-INF"))
                continue;
            if (files[i].equalsIgnoreCase("WEB-INF"))
                continue;
            if (deployed.contains(files[i]))
                continue;
            File dir = new File(configBase, files[i]);
            if (files[i].toLowerCase().endsWith(".xml")) {

                deployed.add(files[i]);

                // Calculate the context path and make sure it is unique
                String file = files[i].substring(0, files[i].length() - 4);
                String contextPath = "/" + file.replace('_', '/');
                if (file.equals("ROOT")) {
                    contextPath = "";
                }

                // Assume this is a configuration descriptor and deploy it
                log.debug(sm.getString("hostConfig.deployDescriptor", files[i]));
                try {
                    if (host.findChild(contextPath) != null) {
                        if ((deployed.contains(file))
                            || (deployed.contains(file + ".war"))) {
                            // If this is a newly added context file and 
                            // it overrides a context with a simple path, 
                            // that was previously deployed by the auto
                            // deployer, undeploy the context
                            ((Deployer) host).remove(contextPath);
                        } else {
                            continue;
                        }
                    }
                    URL config =
                        new URL("file", null, dir.getCanonicalPath());
                    ((Deployer) host).install(config, null);
                } catch (Throwable t) {
                    log.error(sm.getString("hostConfig.deployDescriptor.error",
                                           files[i]), t);
                }

            }

        }

    }


    /**
     * Deploy WAR files.
     */
    protected void deployWARs(File appBase, String[] files) {

        for (int i = 0; i < files.length; i++) {

            if (files[i].equalsIgnoreCase("META-INF"))
                continue;
            if (files[i].equalsIgnoreCase("WEB-INF"))
                continue;
            if (deployed.contains(files[i]))
                continue;
            File dir = new File(appBase, files[i]);
            if (files[i].toLowerCase().endsWith(".war")) {

                deployed.add(files[i]);

                // Calculate the context path and make sure it is unique
                String contextPath = "/" + files[i];
                int period = contextPath.lastIndexOf(".");
                if (period >= 0)
                    contextPath = contextPath.substring(0, period);
                if (contextPath.equals("/ROOT"))
                    contextPath = "";
                if (host.findChild(contextPath) != null)
                    continue;

                // Checking for a nested /META-INF/context.xml
                JarFile jar = null;
                JarEntry entry = null;
                InputStream istream = null;
                BufferedOutputStream ostream = null;
                File xml = new File
                    (configBase, files[i].substring
                     (0, files[i].lastIndexOf(".")) + ".xml");
                if (!xml.exists()) {
                    try {
                        jar = new JarFile(dir);
                        entry = jar.getJarEntry("META-INF/context.xml");
                        if (entry != null) {
                            istream = jar.getInputStream(entry);
                            ostream =
                                new BufferedOutputStream
                                (new FileOutputStream(xml), 1024);
                            byte buffer[] = new byte[1024];
                            while (true) {
                                int n = istream.read(buffer);
                                if (n < 0) {
                                    break;
                                }
                                ostream.write(buffer, 0, n);
                            }
                            ostream.flush();
                            ostream.close();
                            ostream = null;
                            istream.close();
                            istream = null;
                            entry = null;
                            jar.close();
                            jar = null;
                            deployDescriptors(configBase(), configBase.list());
                            return;
                        }
                    } catch (Exception e) {
                        // Ignore and continue
                        if (ostream != null) {
                            try {
                                ostream.close();
                            } catch (Throwable t) {
                                ;
                            }
                            ostream = null;
                        }
                        if (istream != null) {
                            try {
                                istream.close();
                            } catch (Throwable t) {
                                ;
                            }
                            istream = null;
                        }
                        entry = null;
                        if (jar != null) {
                            try {
                                jar.close();
                            } catch (Throwable t) {
                                ;
                            }
                            jar = null;
                        }
                    }
                }

                if (isUnpackWARs()) {

                    // Expand and deploy this application as a directory
                    log.debug(sm.getString("hostConfig.expand", files[i]));
                    URL url = null;
                    String path = null;
                    try {
                        url = new URL("jar:file:" +
                                      dir.getCanonicalPath() + "!/");
                        path = ExpandWar.expand(host, url);
                    } catch (IOException e) {
                        // JAR decompression failure
                        log.warn(sm.getString
                                 ("hostConfig.expand.error", files[i]));
                        continue;
                    } catch (Throwable t) {
                        log.error(sm.getString
                                  ("hostConfig.expand.error", files[i]), t);
                        continue;
                    }
                    try {
                        if (path != null) {
                            url = new URL("file:" + path);
                            ((Deployer) host).install(contextPath, url);
                        }
                    } catch (Throwable t) {
                        log.error(sm.getString
                                  ("hostConfig.expand.error", files[i]), t);
                    }

                } else {

                    // Deploy the application in this WAR file
                    log.info(sm.getString("hostConfig.deployJar", files[i]));
                    try {
                        URL url = new URL("file", null,
                                          dir.getCanonicalPath());
                        url = new URL("jar:" + url.toString() + "!/");
                        ((Deployer) host).install(contextPath, url);
                    } catch (Throwable t) {
                        log.error(sm.getString("hostConfig.deployJar.error",
                                         files[i]), t);
                    }

                }

            }

        }

    }


    /**
     * Deploy directories.
     */
    protected void deployDirectories(File appBase, String[] files) {

        for (int i = 0; i < files.length; i++) {

            if (files[i].equalsIgnoreCase("META-INF"))
                continue;
            if (files[i].equalsIgnoreCase("WEB-INF"))
                continue;
            if (deployed.contains(files[i]))
                continue;
            File dir = new File(appBase, files[i]);
            if (dir.isDirectory()) {

                deployed.add(files[i]);

                // Make sure there is an application configuration directory
                // This is needed if the Context appBase is the same as the
                // web server document root to make sure only web applications
                // are deployed and not directories for web space.
                File webInf = new File(dir, "/WEB-INF");
                if (!webInf.exists() || !webInf.isDirectory() ||
                    !webInf.canRead())
                    continue;

                // Calculate the context path and make sure it is unique
                String contextPath = "/" + files[i];
                if (files[i].equals("ROOT"))
                    contextPath = "";
                if (host.findChild(contextPath) != null)
                    continue;

                // Deploy the application in this directory
                if( log.isDebugEnabled() ) 
                    log.debug(sm.getString("hostConfig.deployDir", files[i]));
                long t1=System.currentTimeMillis();
                try {
                    URL url = new URL("file", null, dir.getCanonicalPath());
                    ((Deployer) host).install(contextPath, url);
                } catch (Throwable t) {
                    log.error(sm.getString("hostConfig.deployDir.error", files[i]),
                        t);
                }
                long t2=System.currentTimeMillis();
                if( (t2-t1) > 200 )
                    log.debug("Deployed " + files[i] + " " + (t2-t1));
            }

        }

    }


    /**
     * Check deployment descriptors last modified date.
     */
    protected void checkContextLastModified() {

        if (!(host instanceof Deployer))
            return;

        Deployer deployer = (Deployer) host;

        String[] contextNames = deployer.findDeployedApps();

        for (int i = 0; i < contextNames.length; i++) {

            String contextName = contextNames[i];
            Context context = deployer.findDeployedApp(contextName);

            if (!(context instanceof Lifecycle))
                continue;

            try {
                DirContext resources = context.getResources();
                if (resources == null) {
                    // This can happen if there was an error initializing
                    // the context
                    continue;
                }
                ResourceAttributes webXmlAttributes = 
                    (ResourceAttributes) 
                    resources.getAttributes("/WEB-INF/web.xml");
                ResourceAttributes webInfAttributes = 
                    (ResourceAttributes) 
                    resources.getAttributes("/WEB-INF");
                long newLastModified = webXmlAttributes.getLastModified();
                long webInfLastModified = webInfAttributes.getLastModified();
                Long lastModified = (Long) webXmlLastModified.get(contextName);
                if (lastModified == null) {
                    webXmlLastModified.put
                        (contextName, new Long(newLastModified));
                } else {
                    if (lastModified.longValue() != newLastModified) {
                        if (newLastModified > (webInfLastModified + 5000)) {
                            webXmlLastModified.remove(contextName);
                            restartContext(context);
                        } else {
                            webXmlLastModified.put
                                (contextName, new Long(newLastModified));
                        }
                    }
                }
            } catch (NamingException e) {
                ; // Ignore
            }

            Long lastModified = (Long) contextXmlLastModified.get(contextName);
            String configBase = configBase().getPath();
            String configFileName = context.getConfigFile();
            if (configFileName != null) {
                File configFile = new File(configFileName);
                if (!configFile.isAbsolute()) {
                    configFile = new File(System.getProperty("catalina.base"),
                                          configFile.getPath());
                }
                long newLastModified = configFile.lastModified();
                if (lastModified == null) {
                    contextXmlLastModified.put
                        (contextName, new Long(newLastModified));
                } else {
                    if (lastModified.longValue() != newLastModified) {
                        contextXmlLastModified.remove(contextName);
                        String fileName = configFileName;
                        if (fileName.startsWith(configBase)) {
                            fileName = 
                                fileName.substring(configBase.length() + 1);
                            try {
                                deployed.remove(fileName);
                                if (host.findChild(contextName) != null) {
                                    ((Deployer) host).remove(contextName);
                                }
                            } catch (Throwable t) {
                                log.error(sm.getString
                                          ("hostConfig.undeployJar.error",
                                           fileName), t);
                            }
                            deployApps();
                        }
                    }
                }
            }

        }

        // Check for WAR modification
        if (isUnpackWARs()) {
            File appBase = appBase();
            if (!appBase.exists() || !appBase.isDirectory())
                return;
            String files[] = appBase.list();

            for (int i = 0; i < files.length; i++) {
                if (files[i].endsWith(".war")) {
                    File dir = new File(appBase, files[i]);
                    Long lastModified = (Long) warLastModified.get(files[i]);
                    long dirLastModified = dir.lastModified();
                    if (lastModified == null) {
                        warLastModified.put
                            (files[i], new Long(dir.lastModified()));
                    } else if (dirLastModified > lastModified.longValue()) {
                        // The WAR has been modified: redeploy
                        String expandedDir = files[i];
                        int period = expandedDir.lastIndexOf(".");
                        if (period >= 0)
                            expandedDir = expandedDir.substring(0, period);
                        File expanded = new File(appBase, expandedDir);
                        String contextPath = "/" + expandedDir;
                        if (contextPath.equals("/ROOT"))
                            contextPath = "";
                        if (dirLastModified > expanded.lastModified()) {
                            try {
                                // Undeploy current application
                                deployed.remove(files[i]);
                                deployed.remove(expandedDir + ".xml");
                                if (host.findChild(contextPath) != null) {
                                    ((Deployer) host).remove(contextPath, 
                                                             false);
                                    ExpandWar.deleteDir(expanded);
                                }
                            } catch (Throwable t) {
                                log.error(sm.getString
                                          ("hostConfig.undeployJar.error",
                                           files[i]), t);
                            }
                            deployApps();
                        }
                        // If deployment was successful, reset 
                        // the last modified values
                        if (host.findChild(contextPath) != null) {
                            webXmlLastModified.remove(contextPath);
                            warLastModified.put
                                (files[i], new Long(dir.lastModified()));
                        }
                    }
                }
            }
        }

    }


    protected boolean restartContext(Context context) {
        boolean result = true;
        log.info("restartContext(" + context.getName() + ")");

        if (context instanceof StandardContext) {
            try {
                StandardContext sctx = (StandardContext)context;
                sctx.reload();
            } catch (Exception e) {
                log.warn(sm.getString
                         ("hostConfig.context.restart", context.getName()), e);
                result = false;
            }
        } else {
            try {
                ((Lifecycle) context).stop();
            } catch (Exception e) {
                log.warn(sm.getString
                         ("hostConfig.context.restart", context.getName()), e);
            }
            // If the context was not started (for example an error 
            // in web.xml) we'll still get to try to start
            try {
                ((Lifecycle) context).start();
            } catch (Exception e) {
                log.warn(sm.getString
                         ("hostConfig.context.restart", context.getName()), e);
                result = false;
            }
        }

        return result;
    }


    /**
     * Expand the WAR file found at the specified URL into an unpacked
     * directory structure, and return the absolute pathname to the expanded
     * directory.
     *
     * @param war URL of the web application archive to be expanded
     *  (must start with "jar:")
     *
     * @exception IllegalArgumentException if this is not a "jar:" URL
     * @exception IOException if an input/output error was encountered
     *  during expansion
     */
    protected String expand(URL war) throws IOException {

        return ExpandWar.expand(host,war);
    }


    /**
     * Expand the specified input stream into the specified directory, creating
     * a file named from the specified relative path.
     *
     * @param input InputStream to be copied
     * @param docBase Document base directory into which we are expanding
     * @param name Relative pathname of the file to be created
     *
     * @exception IOException if an input/output error occurs
     */
    protected void expand(InputStream input, File docBase, String name)
        throws IOException {

        ExpandWar.expand(input,docBase,name);
    }


    /**
     * Log a message on the Logger associated with our Host (if any)
     *
     * @param message Message to be logged
     */
    protected void log(String message) {

        Logger logger = null;
        if (host != null)
            logger = host.getLogger();
        if (logger != null)
            logger.log("HostConfig[" + host.getName() + "]: " + message);
        else
            log.info(message);
    }


    /**
     * Log a message on the Logger associated with our Host (if any)
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    protected void log(String message, Throwable throwable) {

        Logger logger = null;
        if (host != null)
            logger = host.getLogger();
        if (logger != null)
            logger.log("HostConfig[" + host.getName() + "] "
                       + message, throwable);
        else {
            log.error( message, throwable );
        }

    }


    /**
     * Process a "start" event for this Host.
     */
    public void start() {

        if (log.isDebugEnabled())
            log.debug(sm.getString("hostConfig.start"));

        if (host.getDeployOnStartup()) {
            deployApps();
        } else {
            // Deploy descriptors anyway (it should be equivalent to being
            // part of server.xml)
            File configBase = configBase();
            if (configBase.exists() && configBase.isDirectory()) {
                String configFiles[] = configBase.list();
                deployDescriptors(configBase, configFiles);
            }
        }

    }


    /**
     * Process a "stop" event for this Host.
     */
    public void stop() {

        if (log.isDebugEnabled())
            log.debug(sm.getString("hostConfig.stop"));

        undeployApps();

        appBase = null;
        configBase = null;

    }


    /**
     * Undeploy all deployed applications.
     */
    protected void undeployApps() {

        if (!(host instanceof Deployer))
            return;
        if (log.isDebugEnabled())
            log.debug(sm.getString("hostConfig.undeploying"));

        String contextPaths[] = ((Deployer) host).findDeployedApps();
        for (int i = 0; i < contextPaths.length; i++) {
            if (log.isDebugEnabled())
                log.debug(sm.getString("hostConfig.undeploy", contextPaths[i]));
            try {
                ((Deployer) host).remove(contextPaths[i]);
            } catch (Throwable t) {
                log.error(sm.getString("hostConfig.undeploy.error",
                                 contextPaths[i]), t);
            }
        }

        webXmlLastModified.clear();
        deployed.clear();

    }


    /**
     * Deploy webapps.
     */
    protected void check() {

        if (host.getAutoDeploy()) {
            // Deploy apps if the Host allows auto deploying
            deployApps();
            // Check for web.xml modification
            checkContextLastModified();
        }

    }


}
