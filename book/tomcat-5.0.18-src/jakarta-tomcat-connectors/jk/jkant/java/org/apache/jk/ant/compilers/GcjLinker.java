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
import java.util.Enumeration;
import java.util.Vector;

import org.apache.jk.ant.JkData;
import org.apache.jk.ant.SoTask;
import org.apache.jk.ant.Source;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.util.GlobPatternMapper;

/**
 * Link java using gcj.
 * 
 * @author Costin Manolache
 */
public class GcjLinker extends LinkerAdapter {
    SoTask so;
    protected static GlobPatternMapper co_mapper=new GlobPatternMapper();
    static {
	co_mapper.setFrom("*.java");
	co_mapper.setTo("*.o");
    }
    public GcjLinker() {
	so=this;
    }

    public String[] getTargetFiles( Source src ) {
        File srcFile = src.getFile();
        String name=srcFile.getName();
        if( name.endsWith( ".java" ) )
            return co_mapper.mapFileName( name );
        else
            return new String[]
            { name + ".o" };
    }


    
    public void setSoTask(SoTask so ) {
	this.so=so;
	so.duplicateTo( this );
    }

    public void execute() throws BuildException {
	findSourceFiles();
	link(this.srcList);
    }

    /** Link using libtool.
     */
    public boolean link(Vector srcList) throws BuildException {
//         link( srcList, false );
        link( srcList, true );
	return true;
    }
    public boolean link(Vector srcList, boolean shared) throws BuildException {
	Commandline cmd = new Commandline();

	cmd.setExecutable( "gcj" );

        if( shared )
            cmd.createArgument().setValue( "--shared" );
	cmd.createArgument().setValue( "-o" );
        if( shared )
            cmd.createArgument().setValue( soFile + ".so" );
        else
            cmd.createArgument().setValue( soFile + ".a" );

	project.log( "Linking " + buildDir + "/" + soFile + ".so");

        // write out any additional link options
        Enumeration opts = linkOpts.elements();
        while( opts.hasMoreElements() ) {
            JkData opt = (JkData) opts.nextElement();
            String option = opt.getValue();
            if( option == null ) continue;

            cmd.createArgument().setValue( option );
        }

	for( int i=0; i<srcList.size(); i++ ) {
	    Source source=(Source)srcList.elementAt(i);
	    File f1=new File(buildDir, source.getPackage());
	    File srcF=new File(f1, getTargetFiles(source)[0]);
	    cmd.createArgument().setValue( srcF.toString() );
	}
	
	int result=execute( cmd );
	if( result!=0 ) {
	    log("Link failed " + result );
	    log("Command:" + cmd.toString());
	    log("Output:" );
	    if( outputstream!=null ) 
		log( outputstream.toString());
	    log("StdErr:" );
	    if( errorstream!=null ) 
		log( errorstream.toString());
	    
	    throw new BuildException("Link failed " + soFile);
	}
	closeStreamHandler();

	return true;
    }
}

