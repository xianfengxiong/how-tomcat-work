/*
 * $Header: /home/cvs/jakarta-tomcat-jasper/jasper2/src/share/org/apache/jasper/JspC.java,v 1.63 2003/12/20 23:04:23 remm Exp $
 * $Revision: 1.63 $
 * $Date: 2003/12/20 23:04:23 $
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

package org.apache.jasper;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jasper.compiler.Compiler;
import org.apache.jasper.compiler.JspConfig;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.apache.jasper.compiler.Localizer;
import org.apache.jasper.compiler.TagPluginManager;
import org.apache.jasper.compiler.TldLocationsCache;
import org.apache.jasper.servlet.JspCServletContext;
import org.apache.tools.ant.AntClassLoader;

/**
 * Shell for the jspc compiler.  Handles all options associated with the
 * command line and creates compilation contexts which it then compiles
 * according to the specified options.
 *
 * This version can process files from a _single_ webapp at once, i.e.
 * a single docbase can be specified.
 *
 * It can be used as an Ant task using:
 * <pre>
 *   &lt;taskdef classname="org.apache.jasper.JspC" name="jasper2" &gt;
 *      &lt;classpath&gt;
 *          &lt;pathelement location="${java.home}/../lib/tools.jar"/&gt;
 *          &lt;fileset dir="${ENV.CATALINA_HOME}/server/lib"&gt;
 *              &lt;include name="*.jar"/&gt;
 *          &lt;/fileset&gt;
 *          &lt;fileset dir="${ENV.CATALINA_HOME}/common/lib"&gt;
 *              &lt;include name="*.jar"/&gt;
 *          &lt;/fileset&gt;
 *          &lt;path refid="myjars"/&gt;
 *       &lt;/classpath&gt;
 *  &lt;/taskdef&gt;
 *
 *  &lt;jasper2 verbose="0"
 *           package="my.package"
 *           uriroot="${webapps.dir}/${webapp.name}"
 *           webXmlFragment="${build.dir}/generated_web.xml"
 *           outputDir="${webapp.dir}/${webapp.name}/WEB-INF/src/my/package" /&gt;
 * </pre>
 *
 * @author Danno Ferrin
 * @author Pierre Delisle
 * @author Costin Manolache
 */
public class JspC implements Options {

    public static final String DEFAULT_IE_CLASS_ID =
            "clsid:8AD9C840-044E-11D1-B3E9-00805F499D93";

    // Logger
    private static Log log = LogFactory.getLog(JspC.class);

    private static final String SWITCH_VERBOSE = "-v";
    private static final String SWITCH_HELP = "-help";
    private static final String SWITCH_QUIET = "-q";
    private static final String SWITCH_OUTPUT_DIR = "-d";
    private static final String SWITCH_IE_CLASS_ID = "-ieplugin";
    private static final String SWITCH_PACKAGE_NAME = "-p";
    private static final String SWITCH_CLASS_NAME = "-c";
    private static final String SWITCH_FULL_STOP = "--";
    private static final String SWITCH_COMPILE = "-compile";
    private static final String SWITCH_URI_BASE = "-uribase";
    private static final String SWITCH_URI_ROOT = "-uriroot";
    private static final String SWITCH_FILE_WEBAPP = "-webapp";
    private static final String SWITCH_WEBAPP_INC = "-webinc";
    private static final String SWITCH_WEBAPP_XML = "-webxml";
    private static final String SWITCH_MAPPED = "-mapped";
    private static final String SWITCH_XPOWERED_BY = "-xpoweredBy";
    private static final String SWITCH_CLASSPATH = "-classpath";
    private static final String SWITCH_DIE = "-die";
    private static final String SHOW_SUCCESS ="-s";
    private static final String LIST_ERRORS = "-l";
    private static final int NO_WEBXML = 0;
    private static final int INC_WEBXML = 10;
    private static final int ALL_WEBXML = 20;
    private static final int DEFAULT_DIE_LEVEL = 1;
    private static final int NO_DIE_LEVEL = 0;

    private static final String[] insertBefore = 
    { "</web-app>", "<servlet-mapping>", "<session-config>", 
      "<mime-mapping>", "<welcome-file-list>", "<error-page>", "<taglib>",
      "<resource-env-ref>", "<resource-ref>", "<security-constraint>",
      "<login-config>", "<security-role>", "<env-entry>", "<ejb-ref>", 
      "<ejb-local-ref>" };

    private static int die; 
    private String classPath = null;
    private URLClassLoader loader = null;
    private boolean trimSpaces = false;
    private boolean xpoweredBy;
    private boolean mappedFile = false;
    private File scratchDir;
    private String ieClassId = DEFAULT_IE_CLASS_ID;
    private String targetPackage;
    private String targetClassName;
    private String uriBase;
    private String uriRoot;
    private int dieLevel;
    private boolean helpNeeded = false;
    private boolean compile = false;
    private String compiler = null;
    private boolean classDebugInfo = true;
    private Vector extensions;
    private Vector pages = new Vector();

    /**
     * The java file encoding.  Default
     * is UTF-8.  Added per bugzilla 19622.
     */
    private String javaEncoding = "UTF-8";

    // Generation of web.xml fragments
    private String webxmlFile;
    private int webxmlLevel;
    private boolean addWebXmlMappings = false;

    private Writer mapout;
    private CharArrayWriter servletout;
    private CharArrayWriter mappingout;

    private JspCServletContext context;

    // Maintain a dummy JspRuntimeContext for compiling tag files
    private JspRuntimeContext rctxt;

    /**
     * Cache for the TLD locations
     */
    private TldLocationsCache tldLocationsCache = null;

    private JspConfig jspConfig = null;
    private TagPluginManager tagPluginManager = null;

    private boolean verbose = false;
    private boolean listErrors = false;
    private boolean showSuccess = false;
    private int argPos;
    private boolean fullstop = false;
    private String args[];

    public static void main(String arg[]) {
        if (arg.length == 0) {
           System.out.println(Localizer.getMessage("jspc.usage"));
        } else {
            try {
                JspC jspc = new JspC();
                jspc.setArgs(arg);
                if (jspc.helpNeeded) {
                    System.out.println(Localizer.getMessage("jspc.usage"));
                } else {
                    jspc.execute();
                }
            } catch (JasperException je) {
                System.err.print("error:");
                je.printStackTrace();
                //System.err.println(je.getMessage());
                if (die != NO_DIE_LEVEL) {
                    System.exit(die);
                }
            }
        }
    }

    public void setArgs(String[] arg) throws JasperException {
        args = arg;
        String tok;

        dieLevel = NO_DIE_LEVEL;
        die = dieLevel;

        while ((tok = nextArg()) != null) {
            if (tok.equals(SWITCH_VERBOSE)) {
                verbose = true;
                showSuccess = true;
                listErrors = true;
            } else if (tok.equals(SWITCH_OUTPUT_DIR)) {
                tok = nextArg();
                setOutputDir( tok );
            } else if (tok.equals(SWITCH_PACKAGE_NAME)) {
                targetPackage = nextArg();
            } else if (tok.equals(SWITCH_COMPILE)) {
                compile=true;
            } else if (tok.equals(SWITCH_CLASS_NAME)) {
                targetClassName = nextArg();
            } else if (tok.equals(SWITCH_URI_BASE)) {
                uriBase=nextArg();
            } else if (tok.equals(SWITCH_URI_ROOT)) {
                setUriroot( nextArg());
            } else if (tok.equals(SWITCH_FILE_WEBAPP)) {
                setUriroot( nextArg());
            } else if ( tok.equals( SHOW_SUCCESS ) ) {
                showSuccess = true;
            } else if ( tok.equals( LIST_ERRORS ) ) {
                listErrors = true;
            } else if (tok.equals(SWITCH_WEBAPP_INC)) {
                webxmlFile = nextArg();
                if (webxmlFile != null) {
                    webxmlLevel = INC_WEBXML;
                }
            } else if (tok.equals(SWITCH_WEBAPP_XML)) {
                webxmlFile = nextArg();
                if (webxmlFile != null) {
                    webxmlLevel = ALL_WEBXML;
                }
            } else if (tok.equals(SWITCH_MAPPED)) {
                mappedFile = true;
            } else if (tok.equals(SWITCH_XPOWERED_BY)) {
                xpoweredBy = true;
            } else if (tok.equals(SWITCH_CLASSPATH)) {
                setClassPath(nextArg());
            } else if (tok.startsWith(SWITCH_DIE)) {
                try {
                    dieLevel = Integer.parseInt(
                        tok.substring(SWITCH_DIE.length()));
                } catch (NumberFormatException nfe) {
                    dieLevel = DEFAULT_DIE_LEVEL;
                }
                die = dieLevel;
            } else if (tok.equals(SWITCH_HELP)) {
                helpNeeded = true;
            } else {
                if (tok.startsWith("-")) {
                    throw new JasperException("Unrecognized option: " + tok +
                        ".  Use -help for help.");
                }
                if (!fullstop) {
                    argPos--;
                }
                // Start treating the rest as JSP Pages
                break;
            }
        }

        // Add all extra arguments to the list of files
        while( true ) {
            String file = nextFile();
            if( file==null ) break;
            pages.addElement( file );
        }
    }

    public boolean getKeepGenerated() {
        // isn't this why we are running jspc?
        return true;
    }

    public boolean getTrimSpaces() {
        return trimSpaces;
    }

    public void setTrimSpaces(boolean ts) {
        this.trimSpaces = ts;
    }

    public boolean isPoolingEnabled() {
        return true;
    }

    public boolean isXpoweredBy() {
        return xpoweredBy;
    }

    public int getTagPoolSize() {
	return Constants.MAX_POOL_SIZE;
    }

    /**
     * Are we supporting HTML mapped servlets?
     */
    public boolean getMappedFile() {
	return mappedFile;
    }

    // Off-line compiler, no need for security manager
    public Object getProtectionDomain() {
	return null;
    }

    public boolean getSendErrorToClient() {
        // implied send to System.err
        return true;
    }

    public void setClassDebugInfo( boolean b ) {
        classDebugInfo=b;
    }

    public boolean getClassDebugInfo() {
        // compile with debug info
        return classDebugInfo;
    }

    /**
     * Background compilation check intervals in seconds
     */
    public int getCheckInterval() {
        return 300;
    }

    /**
     * Is Jasper being used in development mode?
     */
    public boolean getDevelopment() {
        return false;
    }

    /**
     * JSP reloading check ?
     */
    public boolean getReloading() {
        return true;
    }

    /**
     * Is the generation of SMAP info for JSR45 debuggin suppressed?
     */
    public boolean isSmapSuppressed() {
	return true;
    }

   /**
     * Should SMAP info for JSR45 debugging be dumped to a file?
     */
    public boolean isSmapDumped() {
	return false;
    }

    /**
     * Are Text strings to be generated as char arrays?
     */
    public boolean genStringAsCharArray() {
        return false;
    }

    public String getIeClassId() {
        return ieClassId;
    }

    public File getScratchDir() {
        return scratchDir;
    }

    public Class getJspCompilerPlugin() {
       // we don't compile, so this is meanlingless
        return null;
    }

    public String getJspCompilerPath() {
       // we don't compile, so this is meanlingless
        return null;
    }

    /**
     * Compiler to use.
     */
    public String getCompiler() {
        return compiler;
    }

    public void setCompiler(String c) {
        compiler=c;
    }

    public TldLocationsCache getTldLocationsCache() {
	return tldLocationsCache;
    }

    /**
     * Returns the encoding to use for
     * java files.  The default is UTF-8.
     *
     * @return String The encoding
     */
    public String getJavaEncoding() {
	return javaEncoding;
    }

    /**
     * Sets the encoding to use for
     * java files.
     *
     * @param encodingName The name, e.g. "UTF-8" 
     */
    public void setJavaEncoding(String encodingName) {
      javaEncoding = encodingName;
    }

    public boolean getFork() {
        return false;
    }

    public String getClassPath() {
        if( classPath != null )
            return classPath;
        return System.getProperty("java.class.path");
    }

    public void setClassPath(String s) {
        classPath=s;
    }

    /**
     * Base dir for the webapp. Used to generate class names and resolve
     * includes
     */
    public void setUriroot( String s ) {
        if( s==null ) {
            uriRoot=s;
            return;
        }
        try {
            uriRoot=new File( s ).getCanonicalPath();
        } catch( Exception ex ) {
            uriRoot=s;
        }
    }

    public void setCompile( boolean b ) {
        compile=b;
    }

    public void setValidateXml( boolean b ) {
        org.apache.jasper.xmlparser.ParserUtils.validating=b;
    }

    public void setListErrors( boolean b ) {
        listErrors = b;
    }

    public void setOutputDir( String s ) {
        if( s!= null ) {
            scratchDir = new File(s).getAbsoluteFile();
        } else {
            scratchDir=null;
        }
    }

    public void setPackage( String p ) {
        targetPackage=p;
    }

    /**
     * Class name of the generated file ( without package ).
     * Can only be used if a single file is converted.
     * XXX Do we need this feature ?
     */
    public void setClassName( String p ) {
        targetClassName=p;
    }

    /**
     * File where we generate a web.xml fragment with the class definitions.
     */
    public void setWebXmlFragment( String s ) {
        webxmlFile=s;
        webxmlLevel=INC_WEBXML;
    }

    /**
     * File where we generate a complete web.xml with the class definitions.
     */
    public void setWebXml( String s ) {
        webxmlFile=s;
        webxmlLevel=ALL_WEBXML;
    }

    public void setAddWebXmlMappings(boolean b) {
        addWebXmlMappings = b;
    }

    /**
     * Obtain JSP configuration informantion specified in web.xml.
     */
    public JspConfig getJspConfig() {
	return jspConfig;
    }

    public TagPluginManager getTagPluginManager() {
        return tagPluginManager;
    }

    public void generateWebMapping( String file, JspCompilationContext clctxt )
        throws IOException
    {
        String className = clctxt.getServletClassName();
        String packageName = clctxt.getServletPackageName();

        String thisServletName;
        if  ("".equals(packageName)) {
            thisServletName = className;
        } else {
            thisServletName = packageName + '.' + className;
        }

        if (servletout != null) {
            servletout.write("\n    <servlet>\n        <servlet-name>");
            servletout.write(thisServletName);
            servletout.write("</servlet-name>\n        <servlet-class>");
            servletout.write(thisServletName);
            servletout.write("</servlet-class>\n    </servlet>\n");
        }
        if (mappingout != null) {
            mappingout.write("\n    <servlet-mapping>\n        <servlet-name>");
            mappingout.write(thisServletName);
            mappingout.write("</servlet-name>\n        <url-pattern>");
            mappingout.write(file.replace('\\', '/'));
            mappingout.write("</url-pattern>\n    </servlet-mapping>\n");

        }
    }

    /**
     * Include the generated web.xml inside the webapp's web.xml.
     */
    protected void mergeIntoWebXml() throws IOException {

        File webappBase = new File(uriRoot);
        File webXml = new File(webappBase, "WEB-INF/web.xml");
        File webXml2 = new File(webappBase, "WEB-INF/web2.xml");
        String insertStartMarker = 
            Localizer.getMessage("jspc.webinc.insertStart");
        String insertEndMarker = 
            Localizer.getMessage("jspc.webinc.insertEnd");

        BufferedReader reader = new BufferedReader(new FileReader(webXml));
        BufferedReader fragmentReader = 
            new BufferedReader(new FileReader(webxmlFile));
        PrintWriter writer = new PrintWriter(new FileWriter(webXml2));

        // Insert the <servlet> and <servlet-mapping> declarations
        int pos = -1;
        String line = null;
        while (true) {
            line = reader.readLine();
            if (line == null) {
                break;
            }
            // Skip anything previously generated by JSPC
            if (line.indexOf(insertStartMarker) >= 0) {
                while (true) {
                    line = reader.readLine();
                    if (line == null) {
                        return;
                    }
                    if (line.indexOf(insertEndMarker) >= 0) {
                        line = reader.readLine();
                        if (line == null) {
                            return;
                        }
                        break;
                    }
                }
            }
            for (int i = 0; i < insertBefore.length; i++) {
                pos = line.indexOf(insertBefore[i]);
                if (pos >= 0)
                    break;
            }
            if (pos >= 0) {
                writer.println(line.substring(0, pos));
                break;
            } else {
                writer.println(line);
            }
        }

        writer.println(insertStartMarker);
        while (true) {
            String line2 = fragmentReader.readLine();
            if (line2 == null) {
                writer.println();
                break;
            }
            writer.println(line2);
        }
        writer.println(insertEndMarker);
        writer.println();

        for (int i = 0; i < pos; i++) {
            writer.print(" ");
        }
        writer.println(line.substring(pos));

        while (true) {
            line = reader.readLine();
            if (line == null) {
                break;
            }
            writer.println(line);
        }
        writer.close();

        reader.close();
        fragmentReader.close();

        FileInputStream fis = new FileInputStream(webXml2);
        FileOutputStream fos = new FileOutputStream(webXml);

        byte buf[] = new byte[512];
        while (true) {
            int n = fis.read(buf);
            if (n < 0) {
                break;
            }
            fos.write(buf, 0, n);
        }

        fis.close();
        fos.close();

        webXml2.delete();
        (new File(webxmlFile)).delete();

    }

    private void processFile(String file)
        throws JasperException
    {
        try {
            // set up a scratch/output dir if none is provided
            if (scratchDir == null) {
                String temp = System.getProperty("java.io.tmpdir");
                if (temp == null) {
                    temp = "";
                }
                scratchDir = new File(new File(temp).getAbsolutePath());
            }

            String jspUri=file.replace('\\','/');
            JspCompilationContext clctxt = new JspCompilationContext
                ( jspUri, false,  this, context, null, rctxt );

            /* Override the defaults */
            if ((targetClassName != null) && (targetClassName.length() > 0)) {
                clctxt.setServletClassName(targetClassName);
                targetClassName = null;
            }
            if (targetPackage != null) {
                clctxt.setServletPackageName(targetPackage);
            }

            if( loader==null )
                initClassLoader( clctxt );

            clctxt.setClassLoader(loader);
            clctxt.setClassPath(classPath);

            Compiler clc = clctxt.createCompiler();

            // If compile is set, generate both .java and .class, if
            // .jsp file is newer than .class file;
            // Otherwise only generate .java, if .jsp file is newer than
            // the .java file
            if( clc.isOutDated(compile) ) {
                clc.compile(compile);
            }

            // Generate mapping
            generateWebMapping( file, clctxt );
            if ( showSuccess ) {
                log.info( "Built File: " + file );
            }

        } catch (JasperException je) {
            Throwable rootCause = je;
            while (rootCause instanceof JasperException
                    && ((JasperException) rootCause).getRootCause() != null) {
                rootCause = ((JasperException) rootCause).getRootCause();
            }
            log.error(Localizer.getMessage("jspc.error.generalException",
					   file),
		      rootCause);
            throw je;

        } catch (Exception e) {
	    if ((e instanceof FileNotFoundException) && log.isWarnEnabled()) {
		log.warn(Localizer.getMessage("jspc.error.fileDoesNotExist",
					      e.getMessage()));
	    }
            throw new JasperException(e);
        }
    }

    /**
     * Locate all jsp files in the webapp. Used if no explicit
     * jsps are specified.
     */
    public void scanFiles( File base ) throws JasperException {
        Stack dirs = new Stack();
        dirs.push(base);
        if (extensions == null) {
            extensions = new Vector();
            extensions.addElement("jsp");
            extensions.addElement("jspx");
        }
        while (!dirs.isEmpty()) {
            String s = dirs.pop().toString();
            //System.out.println("--" + s);
            File f = new File(s);
            if (f.exists() && f.isDirectory()) {
                String[] files = f.list();
                String ext;
                for (int i = 0; i < files.length; i++) {
                    File f2 = new File(s, files[i]);
                    //System.out.println(":" + f2.getPath());
                    if (f2.isDirectory()) {
                        dirs.push(f2.getPath());
                        //System.out.println("++" + f2.getPath());
                    } else {
                        String path = f2.getPath();
                        String uri = path.substring(uriRoot.length());
                        ext = files[i].substring(files[i].lastIndexOf('.')
						 + 1);
                        if (extensions.contains(ext) ||
                                jspConfig.isJspPage(uri)) {
                            //System.out.println(s + "?" + files[i]);
                            pages.addElement(path);
                        } else {
			    //System.out.println("not done:" + ext);
                        }
                    }
                }
            }
        }
    }

    public void execute() throws JasperException {

        try {
	    if (uriRoot == null) {
		if( pages.size() == 0 ) {
		    throw new JasperException(
                        Localizer.getMessage("jsp.error.jspc.missingTarget"));
		}
		String firstJsp=(String)pages.elementAt( 0 );
                File firstJspF = new File( firstJsp );
                if (!firstJspF.exists()) {
                   throw new JasperException(
                       Localizer.getMessage("jspc.error.fileDoesNotExist",
                                            firstJsp));
                }
                locateUriRoot( firstJspF );
	    }

	    if( context==null )
		initServletContext();

	    // No explicit page, we'll process all .jsp in the webapp
	    if (pages.size() == 0) {
		scanFiles( new File( uriRoot ));
	    }

            if (uriRoot == null) {
		throw new JasperException(
                    Localizer.getMessage("jsp.error.jspc.no_uriroot"));
	    }
	    File uriRootF = new File(uriRoot);
	    if (!uriRootF.exists() || !uriRootF.isDirectory()) {
		throw new JasperException(
                    Localizer.getMessage("jsp.error.jspc.uriroot_not_dir"));
	    }

	    initWebXml();

	    Enumeration e = pages.elements();
	    while (e.hasMoreElements()) {
		String nextjsp = e.nextElement().toString();
		try {
		    File fjsp = new File(nextjsp);
		    if (!fjsp.exists()) {
			if (log.isWarnEnabled()) {
			    log.warn(Localizer.getMessage("jspc.error.fileDoesNotExist",
							  fjsp.toString()));
			}
			continue;
		    }
		    String s = fjsp.getCanonicalPath();
		    //System.out.println("**" + s);
		    if (s.startsWith(uriRoot)) {
			nextjsp = s.substring(uriRoot.length());
		    }
		} catch (IOException ioe) {
		    // if we got problems dont change the file name
		}

		if (nextjsp.startsWith("." + File.separatorChar)) {
		    nextjsp = nextjsp.substring(2);
		}

		processFile(nextjsp);
	    }

	    completeWebXml();
	    
            if (addWebXmlMappings) {
                mergeIntoWebXml();
            }

        } catch (IOException ioe) {
            throw new JasperException(ioe);

        } catch (JasperException je) {
            Throwable rootCause = je;
            while (rootCause instanceof JasperException
                    && ((JasperException) rootCause).getRootCause() != null) {
                rootCause = ((JasperException) rootCause).getRootCause();
            }
            rootCause.printStackTrace();
            throw je;
        }
    }

    // ==================== Private utility methods ====================

    private String nextArg() {
        if ((argPos >= args.length)
            || (fullstop = SWITCH_FULL_STOP.equals(args[argPos]))) {
            return null;
        } else {
            return args[argPos++];
        }
    }

    private String nextFile() {
        if (fullstop) argPos++;
        if (argPos >= args.length) {
            return null;
        } else {
            return args[argPos++];
        }
    }

    private void initWebXml() {
        try {
            if (webxmlLevel >= INC_WEBXML) {
                File fmapings = new File(webxmlFile);
                mapout = new FileWriter(fmapings);
                servletout = new CharArrayWriter();
                mappingout = new CharArrayWriter();
            } else {
                mapout = null;
                servletout = null;
                mappingout = null;
            }
            if (webxmlLevel >= ALL_WEBXML) {
                mapout.write(Localizer.getMessage("jspc.webxml.header"));
                mapout.flush();
            } else if ((webxmlLevel>= INC_WEBXML) && !addWebXmlMappings) {
                mapout.write(Localizer.getMessage("jspc.webinc.header"));
                mapout.flush();
            }
        } catch (IOException ioe) {
            mapout = null;
            servletout = null;
            mappingout = null;
        }
    }

    private void completeWebXml() {
        if (mapout != null) {
            try {
                servletout.writeTo(mapout);
                mappingout.writeTo(mapout);
                if (webxmlLevel >= ALL_WEBXML) {
                    mapout.write(Localizer.getMessage("jspc.webxml.footer"));
                } else if ((webxmlLevel >= INC_WEBXML) && !addWebXmlMappings) {
                    mapout.write(Localizer.getMessage("jspc.webinc.footer"));
                }
                mapout.close();
            } catch (IOException ioe) {
                // noting to do if it fails since we are done with it
            }
        }
    }

    private void initServletContext() {
        try {
            context =new JspCServletContext
                (new PrintWriter(System.out),
                 new URL("file:" + uriRoot.replace('\\','/') + '/'));
            tldLocationsCache = new
                TldLocationsCache(context, true);
        } catch (MalformedURLException me) {
            System.out.println("**" + me);
        }
        rctxt = new JspRuntimeContext(context, this);
        jspConfig = new JspConfig(context);
        tagPluginManager = new TagPluginManager(context);
    }

    private void initClassLoader(JspCompilationContext clctxt)
	    throws IOException {

        classPath = getClassPath();

        ClassLoader jspcLoader = getClass().getClassLoader();
        if (jspcLoader instanceof AntClassLoader) {
            classPath += File.pathSeparator
                + ((AntClassLoader) jspcLoader).getClasspath();
        }

        // Turn the classPath into URLs
        ArrayList urls = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(classPath,
                                                        File.pathSeparator);
        while (tokenizer.hasMoreTokens()) {
            String path = tokenizer.nextToken();
            try {
                File libFile = new File(path);
                urls.add(libFile.toURL());
            } catch (IOException ioe) {
                // Failing a toCanonicalPath on a file that
                // exists() should be a JVM regression test,
                // therefore we have permission to freak uot
                throw new RuntimeException(ioe.toString());
            }
        }

        File webappBase = new File(uriRoot);
        if (webappBase.exists()) {
            File classes = new File(webappBase, "/WEB-INF/classes");
            try {
                if (classes.exists()) {
                    classPath = classPath + File.pathSeparator
                        + classes.getCanonicalPath();
                    urls.add(classes.getCanonicalFile().toURL());
                }
            } catch (IOException ioe) {
                // failing a toCanonicalPath on a file that
                // exists() should be a JVM regression test,
                // therefore we have permission to freak out
                throw new RuntimeException(ioe.toString());
            }
            File lib = new File(webappBase, "/WEB-INF/lib");
            if (lib.exists() && lib.isDirectory()) {
                String[] libs = lib.list();
                for (int i = 0; i < libs.length; i++) {
                    if( libs[i].length() <5 ) continue;
                    String ext=libs[i].substring( libs[i].length() - 4 );
                    if (! ".jar".equalsIgnoreCase(ext)) {
                        if (".tld".equalsIgnoreCase(ext)) {
                            log.warn("TLD files should not be placed in "
                                     + "/WEB-INF/lib");
                        }
                        continue;
                    }
                    try {
                        File libFile = new File(lib, libs[i]);
                        classPath = classPath + File.pathSeparator
                            + libFile.getCanonicalPath();
                        urls.add(libFile.getCanonicalFile().toURL());
                    } catch (IOException ioe) {
                        // failing a toCanonicalPath on a file that
                        // exists() should be a JVM regression test,
                        // therefore we have permission to freak out
                        throw new RuntimeException(ioe.toString());
                    }
                }
            }
        }

        // What is this ??
        urls.add(new File(clctxt.getRealPath("/")).getCanonicalFile().toURL());

        URL urlsA[]=new URL[urls.size()];
        urls.toArray(urlsA);
        loader = new URLClassLoader(urlsA, this.getClass().getClassLoader());
        Thread.currentThread().setContextClassLoader(loader);
    }

    /**
     * Find the WEB-INF dir by looking up in the directory tree.
     * This is used if no explicit docbase is set, but only files.
     * XXX Maybe we should require the docbase.
     */
    private void locateUriRoot( File f ) {
        String tUriBase = uriBase;
        if (tUriBase == null) {
            tUriBase = "/";
        }
        try {
            if (f.exists()) {
                f = new File(f.getCanonicalPath());
                while (f != null) {
                    File g = new File(f, "WEB-INF");
                    if (g.exists() && g.isDirectory()) {
                        uriRoot = f.getCanonicalPath();
                        uriBase = tUriBase;
			if (log.isInfoEnabled()) {
			    log.info(Localizer.getMessage("jspc.implicit.uriRoot",
							  uriRoot));
			}
                        break;
                    }
                    if (f.exists() && f.isDirectory()) {
                        tUriBase = "/" + f.getName() + "/" + tUriBase;
                    }

                    String fParent = f.getParent();
                    if (fParent == null) {
                        break;
                    } else {
                        f = new File(fParent);
                    }

                    // If there is no acceptible candidate, uriRoot will
                    // remain null to indicate to the CompilerContext to
                    // use the current working/user dir.
                }

                if (uriRoot != null) {
                    File froot = new File(uriRoot);
                    uriRoot = froot.getCanonicalPath();
                }
            }
        } catch (IOException ioe) {
            // since this is an optional default and a null value
            // for uriRoot has a non-error meaning, we can just
            // pass straight through
        }
    }
}
