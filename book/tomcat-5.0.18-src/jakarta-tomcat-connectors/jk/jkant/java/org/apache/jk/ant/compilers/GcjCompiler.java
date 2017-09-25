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

/**
 *  Compile using Gcj. This is ( even more ) experimental.
 * 
 * @author Costin Manolache
 */
public class GcjCompiler extends CcCompiler {
    
    public GcjCompiler() {
	super();
	co_mapper.setFrom("*.java");
	co_mapper.setTo("*.o");
    }

    public String[] getTargetFiles( Source src ) {
        File srcFile = src.getFile();
        String name=srcFile.getName();
        if( name.endsWith( ".java" ) ) {
            return co_mapper.mapFileName( name );
        } else {
            return new String[]
            { name + ".o" };
        }
    }

    
    /** Compile using libtool.
     */
    public void compileSingleFile(Source sourceObj) throws BuildException {
	File f=sourceObj.getFile();
	String source=f.toString();
	Commandline cmd = new Commandline();

	cmd.setExecutable( "gcj" );

	cmd.createArgument().setValue("-c" );
	
	if( optG ) {
            //	    cmd.createArgument().setValue("-g" );
            cmd.createArgument().setValue("-ggdb3" );
            //            cmd.createArgument().setValue("-save-temps" );
	    //  cmd.createArgument().setValue("-Wall");
	}
	addOptimize( cmd );
	addExtraFlags( cmd );
	cmd.createArgument().setValue("-fPIC" );
	addIncludes( cmd );
        String targetDir=sourceObj.getPackage();
	try {
	    File f1=new File( buildDir, targetDir );
	    f1.mkdirs();
            cmd.createArgument().setValue( "-o" );
            String targetO[]=getTargetFiles( sourceObj );
            if( targetO==null ) {
                log("no target for " + sourceObj.getFile() );
                return;
            }
            File ff=new File( f1, targetO[0]);
            cmd.createArgument().setValue( ff.toString() );
	} catch( Exception ex ) {
	    ex.printStackTrace();
	}
	
        if( ! source.endsWith(".java") ) {
            cmd.createArgument().setValue("-R" );
            cmd.createArgument().setValue( targetDir + "/" + f.getName() );
            //System.out.println("XXX resource " + targetDir + "/" + f.getName() );
        } else {
            //            cmd.createArgument().setValue("-fno-bounds-check" );
        }
        cmd.createArgument().setValue( source );
	project.log( "Compiling " + source);

	if( debug > 0 )
	    project.log( "Command: " + cmd ); 
	int result=execute( cmd );
	if( result!=0 ) {
	    displayError( result, source, cmd );
	}
	closeStreamHandler();
    }

    protected void addIncludes(Commandline cmd) {
	String [] includeList = ( includes==null ) ?
	    new String[] {} : includes.getIncludePatterns(project); 
	for( int i=0; i<includeList.length; i++ ) {
	    cmd.createArgument().setValue("-I" + includeList[i] );
	}
    }


}

