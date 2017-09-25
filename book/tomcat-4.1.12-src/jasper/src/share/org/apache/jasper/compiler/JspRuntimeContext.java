
/*
 * $Header: /home/cvs/jakarta-tomcat-jasper/jasper2/src/share/org/apache/jasper/compiler/JspRuntimeContext.java,v 1.4.2.2 2002/09/20 23:04:12 glenn Exp $
 * $Revision: 1.4.2.2 $
 * $Date: 2002/09/20 23:04:12 $
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
 */ 
package org.apache.jasper.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.Policy;
import java.security.PermissionCollection;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.jsp.JspFactory;

import org.apache.jasper.JasperException;
import org.apache.jasper.Constants;
import org.apache.jasper.JspCompilationContext;
import org.apache.jasper.Options;
import org.apache.jasper.logging.Logger;
import org.apache.jasper.runtime.JspFactoryImpl;
import org.apache.jasper.servlet.JspServletWrapper;

/**
 * Class for tracking JSP compile time file dependencies when the
 * &lt;%@include file="..."%&gt; directive is used.
 *
 * A background thread periodically checks the files a JSP page
 * is dependent upon.  If a dpendent file changes the JSP page
 * which included it is recompiled.
 *
 * Only used if a web application context is a directory.
 *
 * @author Glenn L. Nielsen
 * @version $Revision: 1.4.2.2 $
 */
public final class JspRuntimeContext implements Runnable {

    /**
     * Preload classes required at runtime by a JSP servlet so that
     * we don't get a defineClassInPackage security exception.
     */
    static {
        JspFactoryImpl factory = new JspFactoryImpl();
        if( System.getSecurityManager() != null ) {
            String basePackage = "org.apache.jasper.";
            try {
                factory.getClass().getClassLoader().loadClass( basePackage +
                    "runtime.JspFactoryImpl$PrivilegedGetPageContext");
                factory.getClass().getClassLoader().loadClass( basePackage +
                    "runtime.JspFactoryImpl$PrivilegedReleasePageContext");
                factory.getClass().getClassLoader().loadClass( basePackage +
                    "runtime.JspRuntimeLibrary");
                factory.getClass().getClassLoader().loadClass( basePackage +
                    "runtime.JspRuntimeLibrary$PrivilegedIntrospectHelper");
                factory.getClass().getClassLoader().loadClass( basePackage +
                    "runtime.ServletResponseWrapperInclude");
                factory.getClass().getClassLoader().loadClass( basePackage +
                    "runtime.TagHandlerPool");
                factory.getClass().getClassLoader().loadClass( basePackage +
                    "servlet.JspServletWrapper");
            } catch (ClassNotFoundException ex) {
                System.out.println(
                    "Jasper JspRuntimeContext preload of class failed: " +
                    ex.getMessage());
            }
        }
        JspFactory.setDefaultFactory(factory);
    }

    // ----------------------------------------------------------- Constructors

    /**
     * Create a JspRuntimeContext for a web application context.
     *
     * Loads in any previously generated dependencies from file.
     *
     * @param ServletContext for web application
     */
    public JspRuntimeContext(ServletContext context, Options options) {

        this.context = context;
        this.options = options;

        // Get the parent class loader
        parentClassLoader =
            (URLClassLoader) Thread.currentThread().getContextClassLoader();
        if (parentClassLoader == null) {
            parentClassLoader =
                (URLClassLoader)this.getClass().getClassLoader();
        }
        if (parentClassLoader != null) {
            Constants.message("jsp.message.parent_class_loader_is",
                              new Object[] {
                                  parentClassLoader.toString()
                              }, Logger.DEBUG);
        } else {
            Constants.message("jsp.message.parent_class_loader_is",
                              new Object[] {
                                  "<none>"
                              }, Logger.DEBUG);
        }

        initSecurity();
        initClassPath();

        // If this web application context is running from a
        // directory, start the background compilation thread
        String appBase = context.getRealPath("/");         
        if (!options.getDevelopment()
                && appBase != null
                && options.getReloading() ) {
            if (appBase.endsWith(File.separator) ) {
                appBase = appBase.substring(0,appBase.length()-1);
            }
            String directory =
                appBase.substring(appBase.lastIndexOf(File.separator));
            threadName = threadName + "[" + directory + "]";
            threadStart();
        }                                            
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * This web applications ServletContext
     */
    private ServletContext context;
    private Options options;
    private URLClassLoader parentClassLoader;
    private PermissionCollection permissionCollection;
    private CodeSource codeSource;                    
    private String classpath;

    /**
     * Maps JSP pages to their JspServletWrapper's
     */
    private Map jsps = Collections.synchronizedMap( new HashMap());
 

    /**
     * The background thread.
     */
    private Thread thread = null;


    /**
     * The background thread completion semaphore.
     */
    private boolean threadDone = false;


    /**
     * Name to register for the background thread.
     */
    private String threadName = "JspRuntimeContext";

    // ------------------------------------------------------ Protected Methods

    /**
     * Add a new JspServletWrapper.
     *
     * @param String uri of JSP
     * @param JspServletWrapper for JSP
     */
    public void addWrapper(String jspUri, JspServletWrapper jsw) {
        jsps.remove(jspUri);
        jsps.put(jspUri,jsw);
    }

    /**
     * Get an already existing JspServletWrapper.
     *
     * @param String JSP URI
     * @return JspServletWrapper for JSP
     */
    public JspServletWrapper getWrapper(String jspUri) {
        return (JspServletWrapper) jsps.get(jspUri);
    }

    /**
     * Remove a  JspServletWrapper.
     *
     * @param String JSP URI of JspServletWrapper to remove
     */
    public void removeWrapper(String jspUri) {
        jsps.remove(jspUri);
    }

    /**
     * Get the SecurityManager Policy CodeSource for this web
     * applicaiton context.
     *
     * @return CodeSource for JSP
     */
    public CodeSource getCodeSource() {
        return codeSource;
    }

    /**
     * Get the parent URLClassLoader.
     *
     * @return URLClassLoader parent
     */
    public URLClassLoader getParentClassLoader() {
        return parentClassLoader;
    }

    /**
     * Get the SecurityManager PermissionCollection for this
     * web application context.
     *
     * @return PermissionCollection permissions
     */
    public PermissionCollection getPermissionCollection() {
        return permissionCollection;
    }

    /**
     * Process a "destory" event for this web application context.
     */                                                        
    public void destroy() {                                    

        threadStop();

        Iterator servlets = jsps.values().iterator();
        while (servlets.hasNext()) {
            ((JspServletWrapper) servlets.next()).destroy();
        }
    }

    // -------------------------------------------------------- Private Methods

    /**
     * Method used by background thread to check the JSP dependencies
     * registered with this class for JSP's.
     */
    private void checkCompile() {
        Iterator it = jsps.values().iterator();
        while (it.hasNext()) {
            JspServletWrapper jsw = (JspServletWrapper)it.next();
            JspCompilationContext ctxt = jsw.getJspEngineContext();
            // JspServletWrapper also synchronizes on this when
            // it detects it has to do a reload
            synchronized(jsw) {
                try {
                    ctxt.compile();
                } catch (FileNotFoundException ex) {
                    ctxt.incrementRemoved();
                } catch (Throwable t) {
                    jsw.getServletContext().log("Background compile failed",t);
                }
            }
        }
    }

    /**
     * The classpath that is passed off to the Java compiler.
     */
    public String getClassPath() {
        return classpath;
    }

    /**
     * Method used to initialize classpath for compiles.
     */
    private void initClassPath() {

        URL [] urls = parentClassLoader.getURLs();
        StringBuffer cpath = new StringBuffer();
        String sep = System.getProperty("path.separator");

        for(int i = 0; i < urls.length; i++) {
            // Tomcat 4 can use URL's other than file URL's,
            // a protocol other than file: will generate a
            // bad file system path, so only add file:
            // protocol URL's to the classpath.
            
            if( urls[i].getProtocol().equals("file") ) {
                cpath.append((String)urls[i].getFile()+sep);
            }
        }    

        String cp = (String) context.getAttribute(Constants.SERVLET_CLASSPATH);
        if (cp == null || cp.equals("")) {
            cp = options.getClassPath();
        }

        classpath = cpath.toString() + cp;
    }

    /**
     * Method used to initialize SecurityManager data.
     */
    private void initSecurity() {

        // Setup the PermissionCollection for this web app context
        // based on the permissions configured for the root of the
        // web app context directory, then add a file read permission
        // for that directory.
        Policy policy = Policy.getPolicy();
        if( policy != null ) {
            try {          
                // Get the permissions for the web app context
                String contextDir = context.getRealPath("/");
                if( contextDir == null ) {
                    contextDir = options.getScratchDir().toString();
                }
                URL url = new URL("file:" + contextDir);
                codeSource = new CodeSource(url,null);
                permissionCollection = policy.getPermissions(codeSource);

                // Create a file read permission for web app context directory
                if (contextDir.endsWith(File.separator)) {
                    contextDir = contextDir + "-";
                } else {
                    contextDir = contextDir + File.separator + "-";
                }

                // Create a file read permission for web app tempdir (work) directory
                String workDir = options.getScratchDir().toString();
                if (workDir.endsWith(File.separator)) {
                    workDir = workDir + "-";
                } else {
                    workDir = workDir + File.separator + "-";
                }
                permissionCollection.add(new FilePermission(workDir,"read"));

                // Allow the JSP to access org.apache.jasper.runtime.HttpJspBase
                permissionCollection.add( new RuntimePermission(
                    "accessClassInPackage.org.apache.jasper.runtime") );

                if (parentClassLoader instanceof URLClassLoader) {
                    URL [] urls = parentClassLoader.getURLs();
                    String jarUrl = null;
                    String jndiUrl = null;
                    for (int i=0; i<urls.length; i++) {
                        if (jndiUrl == null
                                && urls[i].toString().startsWith("jndi:") ) {
                            jndiUrl = urls[i].toString() + "-";
                        }
                        if (jarUrl == null
                                && urls[i].toString().startsWith("jar:jndi:")
                                ) {
                            jarUrl = urls[i].toString();
                            jarUrl = jarUrl.substring(0,jarUrl.length() - 2);
                            jarUrl = jarUrl.substring(0,
                                     jarUrl.lastIndexOf('/')) + "/-";
                        }
                    }
                    if (jarUrl != null) {
                        permissionCollection.add(
                                new FilePermission(jarUrl,"read"));
                        permissionCollection.add(
                                new FilePermission(jarUrl.substring(4),"read"));
                    }
                    if (jndiUrl != null)
                        permissionCollection.add(
                                new FilePermission(jndiUrl,"read") );
                }
            } catch(MalformedURLException mfe) {
            }
        }
    }


    // -------------------------------------------------------- Thread Support

    /**
     * Start the background thread that will periodically check for
     * changes to compile time included files in a JSP.
     *
     * @exception IllegalStateException if we should not be starting
     *  a background thread now
     */
    protected void threadStart() {

        // Has the background thread already been started?
        if (thread != null) {
            return;
        }

        // Start the background thread
        threadDone = false;
        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.start();

    }


    /**
     * Stop the background thread that is periodically checking for
     * changes to compile time included files in a JSP.
     */ 
    protected void threadStop() {

        if (thread == null) {
            return;
        }

        threadDone = true;
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            ;
        }
        
        thread = null;
        
    }

    /**
     * Sleep for the duration specified by the <code>checkInterval</code>
     * property.
     */ 
    protected void threadSleep() {
        
        try {
            Thread.sleep(options.getCheckInterval() * 1000L);
        } catch (InterruptedException e) {
            ;
        }
        
    }   
    
    
    // ------------------------------------------------------ Background Thread
        
        
    /**
     * The background thread that checks for changes to files
     * included by a JSP and flags that a recompile is required.
     */ 
    public void run() {
        
        // Loop until the termination semaphore is set
        while (!threadDone) {

            // Wait for our check interval
            threadSleep();

            // Check for included files which are newer than the
            // JSP which uses them.
            checkCompile();
        }
        
    }

}
