/*
 * $Header: /home/cvs/jakarta-tomcat-catalina/catalina/src/share/org/apache/catalina/util/ExtensionValidator.java,v 1.8 2003/09/02 21:22:06 remm Exp $
 * $Revision: 1.8 $
 * $Date: 2003/09/02 21:22:06 $
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

package org.apache.catalina.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.naming.Binding;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.apache.catalina.core.StandardContext;
import org.apache.naming.resources.Resource;


/**
 * Ensures that all extension dependies are resolved for a WEB application
 * are met. This class builds a master list of extensions available to an
 * applicaiton and then validates those extensions.
 *
 * See http://java.sun.com/j2se/1.4/docs/guide/extensions/spec.html for
 * a detailed explanation of the extension mechanism in Java.
 *
 * @author Greg Murray
 * @author Justyna Horwat
 * @version $Revision: 1.8 $ $Date: 2003/09/02 21:22:06 $
 *
 */
public final class ExtensionValidator {

    private static org.apache.commons.logging.Log log=
        org.apache.commons.logging.LogFactory.getLog(ExtensionValidator.class);

    /**
     * The string resources for this package.
     */
    private static StringManager sm =
        StringManager.getManager("org.apache.catalina.util");
    
    private static ExtensionValidator validator = null;
    private static HashMap containerAvailableExtensions = null;
    private static ArrayList containerManifestResources = null;
    private static ResourceBundle messages = null;


    // ----------------------------------------------------------- Constructors


    /**
     *  Access to this class can only be made through the factory method
     *  getInstance()
     *
     *  This private constructor loads the container level extensions that are
     *  available to all web applications. This method scans all extension 
     *  directories available via the "java.ext.dirs" System property. 
     *
     *  The System Class-Path is also scanned for jar files that may contain 
     *  available extensions. The system extensions are loaded only the 
     *  first time an instance of the ExtensionValidator is created.
     */
    private ExtensionValidator() throws IOException {

        // check for container level optional packages
        String systemClasspath = System.getProperty("java.class.path");

        StringTokenizer strTok = new StringTokenizer(systemClasspath, 
                                                     File.pathSeparator);

        // build a list of jar files in the classpath
        while (strTok.hasMoreTokens()) {
            String classpathItem = strTok.nextToken();
            if (classpathItem.toLowerCase().endsWith(".jar")) {
                File item = new File(classpathItem);
                if (item.exists()) {
                    addSystemResource(item);
                }
            }
        }

        // get the files in the extensions directory
        String extensionsDir = System.getProperty("java.ext.dirs");
        if (extensionsDir != null) {
            StringTokenizer extensionsTok
                = new StringTokenizer(extensionsDir, File.pathSeparator);
            while (extensionsTok.hasMoreTokens()) {
                File targetDir = new File(extensionsTok.nextToken());
                if (!targetDir.exists() || !targetDir.isDirectory()) {
                    continue;
                }
                File[] files = targetDir.listFiles();
                for (int i = 0; i < files.length; i++) {
                    if (files[i].getName().toLowerCase().endsWith(".jar")) {
                        addSystemResource(files[i]);
                    }
                }
            }
        }
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Runtime validation of a Web Applicaiton.
     *
     * This method uses JNDI to look up the resources located under a 
     * <code>DirContext</code>. It locates Web Application MANIFEST.MF 
     * file in the /META-INF/ directory of the application and all 
     * MANIFEST.MF files in each JAR file located in the WEB-INF/lib 
     * directory and creates an <code>ArrayList</code> of 
     * <code>ManifestResorce<code> objects. These objects are then passed 
     * to the validateManifestResources method for validation.
     *
     * @param dirContext The JNDI root of the Web Application
     * @param context The context from which the Logger and path to the
     *                application
     *
     * @return true if all required extensions satisfied
     */
    public static synchronized boolean validateApplication(
                                           DirContext dirContext, 
                                           StandardContext context)
                    throws IOException {

        String appName = context.getPath();
        ArrayList appManifestResources = new ArrayList();
        ManifestResource appManifestResource = null;
        // If the application context is null it does not exist and 
        // therefore is not valid
        if (dirContext == null) return false;
        // Find the Manifest for the Web Applicaiton
        InputStream inputStream = null;
        try {
            NamingEnumeration wne = dirContext.listBindings("/META-INF/");
            Binding binding = (Binding) wne.nextElement();
            if (binding.getName().toUpperCase().equals("MANIFEST.MF")) {
                Resource resource = (Resource)dirContext.lookup
                                    ("/META-INF/" + binding.getName());
                inputStream = resource.streamContent();
                Manifest manifest = new Manifest(inputStream);
                inputStream.close();
                inputStream = null;
                ManifestResource mre = new ManifestResource
                    (sm.getString("extensionValidator.web-application-manifest"),
                    manifest, ManifestResource.WAR);
                appManifestResources.add(mre);
            } 
        } catch (NamingException nex) {
            // Application does not contain a MANIFEST.MF file
        } catch (NoSuchElementException nse) {
            // Application does not contain a MANIFEST.MF file
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable t) {
                    // Ignore
                }
            }
        }

        // Locate the Manifests for all bundled JARs
        NamingEnumeration ne = null;
        try {
            if (dirContext != null) {
                ne = dirContext.listBindings("WEB-INF/lib/");
            }
            while ((ne != null) && ne.hasMoreElements()) {
                Binding binding = (Binding)ne.nextElement();
                if (!binding.getName().toLowerCase().endsWith(".jar")) {
                    continue;
                }
                Resource resource = (Resource)dirContext.lookup
                                        ("/WEB-INF/lib/" + binding.getName());
                Manifest jmanifest = getManifest(resource.streamContent());
                if (jmanifest != null) {
                    ManifestResource mre = new ManifestResource(
                                                binding.getName(),
                                                jmanifest, 
                                                ManifestResource.APPLICATION);
                    appManifestResources.add(mre);
                }
            }
        } catch (NamingException nex) {
            // Jump out of the check for this application because it 
            // has no resources
        }

        return validateManifestResources(appName, appManifestResources);
    }
    
    /**
     * Return an instance of the ExtensionValidator. 
     * The ExtensionValidator is a singleton.
     */
    public static ExtensionValidator getInstance() throws IOException {
        if (validator == null) {
            validator = new ExtensionValidator();
        }
        return validator;
    }
    
    // -------------------------------------------------------- Private Methods

    /**
     * Validates a <code>ArrayList</code> of <code>ManifestResource</code> 
     * objects. This method requires an application name (which is the 
     * context root of the application at runtime).  
     *
     * <code>false</false> is returned if the extension dependencies
     * represented by any given <code>ManifestResource</code> objects 
     * is not met.
     *
     * This method should also provide static validation of a Web Applicaiton 
     * if provided with the necessary parameters.
     *
     * @param appName The name of the Application that will appear in the 
     *                error messages
     * @param resources A list of <code>ManifestResource</code> objects 
     *                  to be validated.
     *
     * @return true if manifest resource file requirements are met
     */
    private static boolean validateManifestResources(String appName, 
                                                     ArrayList resources) {
        boolean passes = true;
        int failureCount = 0;        
        HashMap availableExtensions = null;

        Iterator it = resources.iterator();
        while (it.hasNext()) {
            ManifestResource mre = (ManifestResource)it.next();
            ArrayList requiredList = mre.getRequiredExtensions();
            if (requiredList == null) {
                continue;
            }

            // build the list of available extensions if necessary
            if (availableExtensions == null) {
                availableExtensions = buildAvailableExtensionsMap(resources);
            }

            // load the container level resource map if it has not been built
            // yet
            if (containerAvailableExtensions == null) {
                containerAvailableExtensions
                    = buildAvailableExtensionsMap(containerManifestResources);
            }

            // iterate through the list of required extensions
            Iterator rit = requiredList.iterator();
            while (rit.hasNext()) {
                Extension requiredExt = (Extension)rit.next();
                String extId = requiredExt.getUniqueId();
                // check the applicaion itself for the extension
                if (availableExtensions != null
                                && availableExtensions.containsKey(extId)) {
                   Extension targetExt = (Extension)
                       availableExtensions.get(extId);
                   if (targetExt.isCompatibleWith(requiredExt)) {
                       requiredExt.setFulfilled(true);
                   }
                // check the container level list for the extension
                } else if (containerAvailableExtensions != null
                        && containerAvailableExtensions.containsKey(extId)) {
                   Extension targetExt = (Extension)
                       containerAvailableExtensions.get(extId);
                   if (targetExt.isCompatibleWith(requiredExt)) {
                       requiredExt.setFulfilled(true);
                   }
                } else {
                    // Failure
                    log.info(sm.getString(
                        "extensionValidator.extension-not-found-error",
                        appName, mre.getResourceName(),
                        requiredExt.getExtensionName()));
                    passes = false;
                    failureCount++;
                }
            }
        }

        if (!passes) {
            log.info(sm.getString(
                     "extensionValidator.extension-validation-error", appName,
                     failureCount + ""));
        }

        return passes;
    }
    
   /* 
    * Build this list of available extensions so that we do not have to 
    * re-build this list every time we iterate through the list of required 
    * extensions. All available extensions in all of the 
    * <code>MainfestResource</code> objects will be added to a 
    * <code>HashMap</code> which is returned on the first dependency list
    * processing pass. 
    *
    * The key is the name + implementation version.
    *
    * NOTE: A list is built only if there is a dependency that needs 
    * to be checked (performance optimization).
    *
    * @param resources A list of <code>ManifestResource</code> objects
    *
    * @return HashMap Map of available extensions
    */
    private static HashMap buildAvailableExtensionsMap(ArrayList resources) {

        HashMap availableMap = null;

        Iterator it = resources.iterator();
        while (it.hasNext()) {
            ManifestResource mre = (ManifestResource)it.next();
            HashMap map = mre.getAvailableExtensions();
            if (map != null) {
                Iterator values = map.values().iterator();
                while (values.hasNext()) {
                    Extension ext = (Extension) values.next();
                    if (availableMap == null) {
                        availableMap = new HashMap();
                        availableMap.put(ext.getUniqueId(), ext);
                    } else if (!availableMap.containsKey(ext.getUniqueId())) {
                        availableMap.put(ext.getUniqueId(), ext);
                    }
                }
            }
        }

        return availableMap;
    }
    
    /**
     * Return the Manifest from a jar file or war file
     *
     * @param inStream Input stream to a WAR or JAR file
     * @return The WAR's or JAR's manifest
     */
    private static Manifest getManifest(InputStream inStream)
            throws IOException {

        Manifest manifest = null;
        JarInputStream jin = null;

        try {
            jin = new JarInputStream(inStream);
            manifest = jin.getManifest();
            jin.close();
            jin = null;
        } finally {
            if (jin != null) {
                try {
                    jin.close();
                } catch (Throwable t) {
                    // Ignore
                }
            }
        }

        return manifest;
    }
    
    /*
     * Checks to see if the given system JAR file contains a MANIFEST, and adds
     * it to the container's manifest resources.
     *
     * @param jarFile The system JAR whose manifest to add
     */
    private static void addSystemResource(File jarFile) throws IOException {

        Manifest manifest = getManifest(new FileInputStream(jarFile));
        if (manifest != null)  {
            ManifestResource mre
                = new ManifestResource(jarFile.getAbsolutePath(),
                                       manifest,
                                       ManifestResource.SYSTEM);
            if (containerManifestResources == null) {
                containerManifestResources = new ArrayList();
            }
            containerManifestResources.add(mre);
        }
    }

}


