/*
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
 */

package org.apache.jk.ant;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.jk.ant.compilers.CcCompiler;
import org.apache.jk.ant.compilers.CompilerAdapter;
import org.apache.jk.ant.compilers.GcjCompiler;
import org.apache.jk.ant.compilers.GcjLinker;
import org.apache.jk.ant.compilers.LibtoolCompiler;
import org.apache.jk.ant.compilers.LibtoolLinker;
import org.apache.jk.ant.compilers.LinkerAdapter;
import org.apache.jk.ant.compilers.MsvcCompiler;
import org.apache.jk.ant.compilers.MsvcLinker;
import org.apache.jk.ant.compilers.MwccCompiler;
import org.apache.jk.ant.compilers.MwldLinker;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PatternSet;

/** Global properties

    Same idea as in javac, some user .properties will set the local preferences,
    including machine-specific. If one is not specified we'll guess it. The
    build file will be clean.

    TODO: can we get configure to generate such a file ? 

    build.native.cc=gcc
    # Path to libtool ( used as a backup )
    build.native.libtool=
    # Platform-specific flags for compilation.
    build.native.extra_cflags=
*/
/* XXX add a optional "compiler" attribute 
    to not guess the compiler based on the executable name 
    present in a global property.

*/


/**
 * Task to generate a .so file, similar with ( or using ) libtool.
 * I hate libtool, so long term I would like to replace most of it
 * with decent java code. Short term it'll just use libtool and
 * hide some of the ugliness.
 * 
 * arguments:
 * <ul>
 * <li>source
 * </ul>
 *
 * <p>
 *
 * @author Costin Manolache
 * @author Mike Anderson
 * @author Ignacio J. Ortega
 */
public class SoTask extends Task {
    protected String apxs;
    // or FileSet ?
    protected Vector src; //[FileSet]
    protected PatternSet includes;
    protected Path depends;
    protected Path libs;
    protected String module;
    protected String soFile;
    protected String soExt = ".so";
    protected String cflags;
    protected File buildDir;
    protected int debug;

    protected boolean optG=true;
    protected boolean optWgcc=true;
    protected boolean optimize=false;
    protected boolean profile=false;
    protected Vector defines    = new Vector();
    protected Vector imports    = new Vector();     // used by the NetWare, win32 linkers
    protected Vector exports    = new Vector();     // used by the NetWare, win32 linkers
    protected Vector modules    = new Vector();     // used by the NetWare linker
    protected Vector linkOpts   = new Vector();     // used by the NetWare, win32 linkers
    protected Vector altSoFiles = new Vector();     // used by the NetWare linker
    protected Vector resources  = new Vector();     // used by the win32 linker

    // Computed fields 
    //    protected Vector compileList; // [Source]
    protected Vector srcList=new Vector();
    protected CompilerAdapter compiler;
    //    protected GlobPatternMapper co_mapper;
    
    public SoTask() {};

    // Hack to allow individual compilers/linkers to work
    // as regular Tasks, independnetly.
    public void duplicateTo(SoTask so) {
        // This will act as a proxy for the child task 
        so.project=project;
        so.target=target;
        so.location=location;
        so.taskName=taskName;
        so.taskType=taskType;
        
        so.apxs=apxs;
        so.src=src;
        so.includes=includes;
        so.depends=depends;
        so.libs=libs;
        so.module=module;
        so.soFile=soFile;
        so.soExt=soExt;
        so.cflags=cflags;
        so.buildDir=buildDir;
        so.debug=debug;
        so.optG=optG;
        so.optWgcc=optWgcc;
        so.optimize=optimize;
        so.profile=profile;
        so.defines=defines;
        so.imports=imports;
        so.exports=exports;
        so.resources=resources;
        so.modules=modules;
        so.linkOpts=linkOpts;
        so.srcList=srcList;
        //      so.compileList=compileList;
        so.compiler=compiler;
        //      so.co_mapper=co_mapper;
        so.altSoFiles=altSoFiles;
    }

    /**  @deprecated use setTarget
     */
    public void setSoFile(String s ) {
        soFile=s;
    }

    /** Add debug information
     */
    public void setDebug(boolean b) {
        optG=b;
    }

    /** Add debug information
     */
    public void setOptimize(boolean b) {
        optimize=b;
    }

    /** Add profiling information
     */
    public void setProfile(boolean b) {
        profile=b;
    }

    /** Gcc warnings
     */
    public void setGccWarn(boolean b) {
        optWgcc=b;
    }

    /** Debug the <so> task
     */
    public void setTaskDebug(int i) {
        debug=i;
    }

    /** Add a -D option. Note that each define has
     *  an if/unless attribute
     */ 
    public void addDef(Def var ) {
        var.setProject( project );
        defines.addElement(var);
    }

    /**
     * Add an import file/symbol for NetWare or win32 platform
     *
     * 
     */
    public void addImport(JkData imp) {
        imp.setProject( project );
        imports.add(imp);
    }

    /**
     * Add an export file/symbol for NetWare or win32 platform
     *
     * 
     */
    public void addExport(JkData exp) {
        exp.setProject( project );
        exports.add(exp);
    }

    /**
     * Add an resource file on win32 platform
     *
     * 
     */
    public void addResource(JkData res) {
        res.setProject( project );
        resources.add(res);
    }

    /**
     * Add a link option for NetWare or win32 platform
     *
     * 
     */
    public void addLinkOpt(JkData option) {
        option.setProject( project );
        linkOpts.add(option);
    }

    /**
     * Add an NLMModule dependancy
     *
     * 
     */
    public void addNLMModule(JkData module) {
        module.setProject( project );
        modules.add(module);
    }

    /**
     * Add an alternate target since some platforms (NetWare) have file name
     * limitations.
     * 
     */
    public void addAltSoFile(JkData altSo) {
        altSo.setProject( project );
        altSoFiles.add(altSo);
    }

    /** Set the target for this compilation. Don't include any
     *  directory or suffix ( not sure about prefix - we may want
     *  to add lib automatically for unix, and nothing on win/etc ?  ).
     */
    public void setTarget(String s ) {
        soFile=s;
    }

    /** Set the extension for the target.  This will depend on the platform
     *  we are compiling for.
     */
    public void setExtension(String s ) {
        soExt=s;
    }

    /** Directory where intermediary objects will be
     *  generated
     */
    public void setBuildDir( File s ) {
        buildDir=s;
    }

    public void setCflags(String s ) {
        cflags=s;
    }
    
    /** Directory where the .so file will be generated
     */
    public void setSoDir( String s ) {
        
    }

    public void addJniConfig( JniConfig jniCfg ) {

    }

    public void addApacheConfig( ApacheConfig apacheCfg ) {

    }
    
    
    /**
     * Source files ( .c )
     *
     * @return a nested src element.
     */
    public void addSrc(FileSet fl) {
        if( src==null ) src=new Vector();
        src.addElement(fl);
    }

    /**
     * Include files
     */
    public PatternSet createIncludes() {
        includes=new PatternSet(); //Path(project);
        return includes;
    }

    /**
     * General dependencies. If any of the files is modified
     * ( i.e. is newer than the oldest .o ) we'll recompile everything.
     *
     * This can be used for headers ( until we add support for makedepend)
     * or any important file that could invalidate the build.
     * Another example is checking httpd or apxs ( if a new version
     * was installed, maybe some flags or symbols changed )
     */
    public Path createDepends() {
        depends=new Path(project);
        return depends;
    }

    /**
     * Libraries ( .a, .so or .dll ) files to link to.
     */
    public Path createLibs() {
        libs=new Path(project);
        return libs;
    }
    
    
    /**
     * The name of the target file.
     * (XXX including extension - this should be automatically added )
     */
    public void setModule(String modName) {
        this.module = modName;	// Should be this ?
    }

    // XXX Add specific code for Netware, Windows and platforms where libtool
    // is problematic

    // XXX Add specific code for Linux and platforms where things are
    // clean, libtool should be just a fallback.
    public void execute() throws BuildException {
        compiler=findCompilerAdapter();
        //      co_mapper=compiler.getOMapper();
        LinkerAdapter linker=findLinkerAdapter();

        if( soFile==null )
            throw new BuildException("No target ( " + soExt + " file )");
        if (src == null) 
            throw new BuildException("No source files");

        // XXX makedepend-type dependencies - how ??
        // We could generate a dummy Makefile and parse the content...
        findSourceFiles();

        // Copy all settings into compiler
        this.duplicateTo(compiler);
        compiler.compile( srcList );

        // XXX move this checking to linker
        File soTarget=new File( buildDir, soFile + soExt );
        if( compiler.getCompiledFiles().size() == 0 && soTarget.exists()) {
            // No dependency, no need to relink
            return;
        }

        this.duplicateTo(linker);
        linker.link(srcList);
    }

    public CompilerAdapter findCompilerAdapter() {
        CompilerAdapter compilerAdapter;
        String cc;
        cc=project.getProperty("build.compiler.cc");
        if( cc!=null ) {
            if( "cc".equals( cc ) ) {
                compilerAdapter=new CcCompiler();
                compilerAdapter.setSoTask( this );
                return compilerAdapter;
            }
            if( "gcj".equals( cc ) ) {
                compilerAdapter=new GcjCompiler();
                compilerAdapter.setSoTask( this );
                return compilerAdapter;
            }
            if( cc.indexOf("mwccnlm") != -1 ) {
                compilerAdapter=new MwccCompiler();
                compilerAdapter.setSoTask( this );
                return compilerAdapter;
            }
            if( cc.indexOf("cl") != -1 ) {
                compilerAdapter=new MsvcCompiler();
                compilerAdapter.setSoTask( this );
                return compilerAdapter;
            }
        }
        
        compilerAdapter=new LibtoolCompiler(); 
        compilerAdapter.setSoTask( this );
        return compilerAdapter;
   }

    public LinkerAdapter findLinkerAdapter() {
        LinkerAdapter linkerAdapter;
        String ld=project.getProperty("build.compiler.ld");
        if( ld!=null ) {
            if( ld.indexOf("mwldnlm") != -1 ) {
                linkerAdapter=new MwldLinker();
                linkerAdapter.setSoTask( this );
                return linkerAdapter;
            }
            if( ld.indexOf("link") != -1 ) {
                linkerAdapter=new MsvcLinker();
                linkerAdapter.setSoTask( this );
                return linkerAdapter;
            }
            //      if( "ld".equals( cc ) ) {
            //          linkerAdapter=new LdLinker();
            //          linkerAdapter.setSoTask( this );
            //          return cc;
            //      }
        }

        String cc=project.getProperty("build.compiler.cc");
        if( "gcj".equals( cc ) ) {
            linkerAdapter=new GcjLinker();
            linkerAdapter.setSoTask( this );
            return linkerAdapter;
        }

        
        linkerAdapter=new LibtoolLinker(); 
        linkerAdapter.setSoTask( this );
        return linkerAdapter;
   }

    /** Find all source files declared with <src> elements
     */
    public void findSourceFiles() {
        if (buildDir == null) buildDir = project.getBaseDir();

        Enumeration e=src.elements();
        while( e.hasMoreElements() ) {
            FileSet fs=(FileSet)e.nextElement();
            DirectoryScanner ds=fs.getDirectoryScanner( project );
            String localList[]= ds.getIncludedFiles(); 
            if (localList.length == 0) 
                throw new BuildException("No source files ");
            for( int i=0; i<localList.length; i++ ) {
                srcList.addElement( new Source( fs.getDir(project), localList[i]));
            }
        }
    }
 
    /** If any file declared in <depend> element has changed, we'll do
        a full rebuild.
    */
    public boolean checkDepend(long oldestO, File oldestOFile) {
        if( depends==null )
            return false;
        String dependsA[]=depends.list(); 
        for( int i=0; i< dependsA.length; i++ ) {
            File f=new File( dependsA[i] );
            if( ! f.exists() ) {
                log("Depend not found " + f );
                return true;
            }
            if( f.lastModified() > oldestO ) {
                log( "Depend " + f + " newer than " + oldestOFile );
                return true;
            }
        }
        return false;
    }
    
    // ==================== Execution utils ==================== 

    protected ExecuteStreamHandler streamhandler = null;
    protected ByteArrayOutputStream outputstream = null;
    protected ByteArrayOutputStream errorstream = null;

    public int execute( Commandline cmd ) throws BuildException
    {
        createStreamHandler();
        Execute exe = new Execute(streamhandler, null);
        exe.setAntRun(project);

        exe.setWorkingDirectory(buildDir);

        exe.setCommandline(cmd.getCommandline());
        int result=0;
        try {
            result=exe.execute();
        } catch (IOException e) {
            throw new BuildException(e, location);
        } 
        return result;
    }

    public void createStreamHandler()  throws BuildException {
        //      try {
        outputstream= new ByteArrayOutputStream();
        errorstream = new ByteArrayOutputStream();
        
        streamhandler =
            new PumpStreamHandler(new PrintStream(outputstream),
                                  new PrintStream(errorstream));
        //      }  catch (IOException e) {
        //          throw new BuildException(e,location);
        //      }
    }

    public void closeStreamHandler() {
        try {
            if (outputstream != null) 
                outputstream.close();
            if (errorstream != null) 
                errorstream.close();
            outputstream=null;
            errorstream=null;
        } catch (IOException e) {}
    }
}

