/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

package org.apache.naming.ant;

import javax.naming.InitialContext;

import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Task;


/**
 *  Dynamic properties from a JNDI context. Use ${jndi:NAME} syntax.
 *  You may need to use <jndiEnv> to set up jndi properties and drivers,
 *  and eventually different context-specific tasks.
 *
 * @author Costin Manolache
 */
public class JndiProperties extends Task {
    public static String PREFIX="jndi:";
    private static org.apache.commons.logging.Log log=
        org.apache.commons.logging.LogFactory.getLog( JndiProperties.class );
    private JndiHelper helper=new JndiHelper();

    public JndiProperties() {
        initNaming();
    }

    static boolean initialized=false;
    static void initNaming() {
        if( initialized ) return;
        initialized=true;
        Thread.currentThread().setContextClassLoader( JndiProperties.class.getClassLoader() );
//         System.setProperty( "java.naming.factory.initial", "org.apache.naming.memory.MemoryInitialContextFactory" );
    }
    
    class JndiHelper extends PropertyHelper {
        public boolean setPropertyHook( String ns, String name, Object v, boolean inh,
                                        boolean user, boolean isNew)
        {
            if( ! name.startsWith(PREFIX) ) {
                // pass to next
                return super.setPropertyHook(ns, name, v, inh, user, isNew);
            }
            name=name.substring( PREFIX.length() );

            // XXX later

            return true;
        }

        public Object getPropertyHook( String ns, String name , boolean user) {
            if( ! name.startsWith(PREFIX) ) {
                // pass to next
                return super.getPropertyHook(ns, name, user);
            }

            Object o=null;
            name=name.substring( PREFIX.length() );
            try {
                InitialContext ic=new InitialContext();
                // XXX lookup attribute in DirContext ?
                o=ic.lookup( name );
                if( log.isDebugEnabled() ) log.debug( "getProperty jndi: " + name +  " " + o);
            } catch( Exception ex ) {
                log.error("getProperty " + name , ex);
                o=null;
            }
            return o;
        }

    }


    public void execute() {
        PropertyHelper phelper=PropertyHelper.getPropertyHelper( project );
        helper.setProject( project );
        helper.setNext( phelper.getNext() );
        phelper.setNext( helper );

        project.addReference( "jndiProperties", this );
    }
    
}
