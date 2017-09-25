/*
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

package org.apache.jk.common;

import java.io.IOException;

import org.apache.jk.core.JkHandler;
import org.apache.jk.core.Msg;
import org.apache.jk.core.MsgContext;




/**
 * Dispatch based on the message type. ( XXX make it more generic,
 * now it's specific to ajp13 ).
 * 
 * @author Costin Manolache
 */
public class HandlerDispatch extends JkHandler
{
    private static org.apache.commons.logging.Log log=
        org.apache.commons.logging.LogFactory.getLog( HandlerDispatch.class );

    public HandlerDispatch() 
    {
    }

    public void init() {
    }

    JkHandler handlers[]=new JkHandler[MAX_HANDLERS];
    String handlerNames[]=new String[MAX_HANDLERS];
    
    static final int MAX_HANDLERS=32;    
    static final int RESERVED=16;  // reserved names, backward compat
    int currentId=RESERVED;

    public int registerMessageType( int id, String name, JkHandler h,
                                    String sig[] )
    {
        if( log.isDebugEnabled() )
            log.debug( "Register message " + id + " " + h.getName() +
                 " " + h.getClass().getName());
	if( id < 0 ) {
	    // try to find it by name
	    for( int i=0; i< handlerNames.length; i++ ) {
                if( handlerNames[i]==null ) continue;
                if( name.equals( handlerNames[i] ) )
                    return i;
            }
	    handlers[currentId]=h;
            handlerNames[currentId]=name;
	    currentId++;
	    return currentId;
	}
	handlers[id]=h;
        handlerNames[currentId]=name;
	return id;
    }

    
    // -------------------- Incoming message --------------------

    public int invoke(Msg msg, MsgContext ep ) 
        throws IOException
    {
        int type=msg.peekByte();
        ep.setType( type );
        
        if( type > handlers.length ||
            handlers[type]==null ) {
	    if( log.isDebugEnabled() )
                log.debug( "Invalid handler " + type );
	    return ERROR;
	}

        if( log.isDebugEnabled() )
            log.debug( "Received " + type + " " + handlers[type].getName());
        
	JkHandler handler=handlers[type];
        
        return handler.invoke( msg, ep );
    }

 }
