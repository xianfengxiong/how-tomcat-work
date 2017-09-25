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

package org.apache.jk.ant.compilers;

import java.io.File;

import org.apache.jk.ant.Source;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.util.GlobPatternMapper;

/**
 *  Compile using Gcc.
 *
 * @author Costin Manolache
 */
public class CcCompiler extends CompilerAdapter {
    GlobPatternMapper co_mapper=new GlobPatternMapper();

    public CcCompiler() {
	super();
	co_mapper.setFrom("*.c");
	co_mapper.setTo("*.o");
    }

    public String[] getTargetFiles( Source src ) {
        File srcFile = src.getFile();
        String name=srcFile.getName();
        
        return co_mapper.mapFileName( name );
    }
    
    String cc;
    
    /** Compile  using 'standard' gcc flags. This assume a 'current' gcc on
     *  a 'normal' platform - no need for libtool
     */
    public void compileSingleFile(Source sourceObj) throws BuildException {
	File f=sourceObj.getFile();
	String source=f.toString();
	Commandline cmd = new Commandline();

	cc=project.getProperty("build.native.cc");
	if(cc==null) cc="cc";
	
	cmd.setExecutable( cc );

	cmd.createArgument().setValue( "-c" );

	addIncludes(cmd);
	addExtraFlags( cmd );
	addDebug(cmd);
	addDefines( cmd );
	addOptimize( cmd );
	addProfile( cmd );

	cmd.createArgument().setValue( source );

	project.log( "Compiling " + source);

	int result=execute( cmd );
        displayError( result, source, cmd );
	closeStreamHandler();
    }
    protected void addDebug(Commandline cmd) {
	if( optG ) {
	    cmd.createArgument().setValue("-g" );
        }

        if( optWgcc ) {
	    if( ! "HP-UX".equalsIgnoreCase( System.getProperty( "os.name" )) ) {
                // HP-UX uses -W for some other things
                cmd.createArgument().setValue("-W");
            }

            if( cc!= null && cc.indexOf( "gcc" ) >= 0 ) {
                //cmd.createArgument().setValue("-Wall");
                cmd.createArgument().setValue("-Wimplicit");
                cmd.createArgument().setValue("-Wreturn-type");
                cmd.createArgument().setValue("-Wcomment");
                cmd.createArgument().setValue("-Wformat");
                cmd.createArgument().setValue("-Wchar-subscripts");
                cmd.createArgument().setValue("-O");
                cmd.createArgument().setValue("-Wuninitialized");
                
                // Non -Wall
                // 	    cmd.createArgument().setValue("-Wtraditional");
                // 	    cmd.createArgument().setValue("-Wredundant-decls");
                cmd.createArgument().setValue("-Wmissing-declarations");
                cmd.createArgument().setValue("-Wmissing-prototypes");
                //	    cmd.createArgument().setValue("-Wconversions");
                cmd.createArgument().setValue("-Wcast-align");
                // 	    cmd.createArgument().setValue("-pedantic" );
            }
	}
    }
    protected void addOptimize( Commandline cmd ) {
	if( optimize )
	    cmd.createArgument().setValue("-O3" );
    }

    protected void addProfile( Commandline cmd ) {
	if( profile ) {
	    cmd.createArgument().setValue("-pg" );
	    // bb.in 
	    // cmd.createArgument().setValue("-ax" );
	}
    }


}

