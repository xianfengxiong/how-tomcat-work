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
package org.apache.jk.core;

import java.util.Hashtable;
import javax.management.ObjectName;

/**
 * The controller object. It manages all other jk objects, acting as the root of
 * the jk object model.
 *
 * @author Gal Shachor
 * @author Henri Gomez [hgomez@apache.org]
 * @author Dan Milstein [danmil@shore.net]
 * @author Keith Wannamaker [Keith@Wannamaker.org]
 * @author Kevin Seguin
 * @author Costin Manolache
 */
public class WorkerEnv {

    Hashtable properties;

    public static final int ENDPOINT_NOTE=0;
    public static final int REQUEST_NOTE=1;
    public static final int SSL_CERT_NOTE=16;
    int noteId[]=new int[4];
    String noteName[][]=new String[4][];
    private Object notes[]=new Object[32];

    Hashtable handlersMap=new Hashtable();
    JkHandler handlersTable[]=new JkHandler[20];
    int handlerCount=0;
    
    // base dir for the jk webapp
    String home;
    int localId=0;
    
    public WorkerEnv() {
        for( int i=0; i<noteId.length; i++ ) {
            noteId[i]=7;
            noteName[i]=new String[20];
        }
    }

    public void setLocalId(int id) {
        localId=id;
    }
    
    public int getLocalId() {
        return localId;
    }
    
    public void setJkHome( String s ) {
        home=s;
    }

    public String getJkHome() {
        return home;
    }
    
    public final Object getNote(int i ) {
        return notes[i];
    }

    public final void setNote(int i, Object o ) {
        notes[i]=o;
    }

    public int getNoteId( int type, String name ) {
        for( int i=0; i<noteId[type]; i++ ) {
            if( name.equals( noteName[type][i] ))
                return i;
        }
        int id=noteId[type]++;
        noteName[type][id]=name;
        return id;
    }

    public void addHandler( String name, JkHandler w ) {
        w.setWorkerEnv( this );
        w.setName( name );
        handlersMap.put( name, w );
        if( handlerCount > handlersTable.length ) {
            JkHandler newT[]=new JkHandler[ 2 * handlersTable.length ];
            System.arraycopy( handlersTable, 0, newT, 0, handlersTable.length );
            handlersTable=newT;
        }
        handlersTable[handlerCount]=w;
        w.setId( handlerCount );
        handlerCount++;

        // Notify all other handlers of the new one
        // XXX Could be a Coyote action ?
        for( int i=0; i< handlerCount ; i++ ) {
            handlersTable[i].addHandlerCallback( w );
        }
    }

    public final JkHandler getHandler( String name ) {
        return (JkHandler)handlersMap.get(name);
    }

    public final JkHandler getHandler( int id ) {
        return handlersTable[id];
    }

    public final int getHandlerCount() {
        return handlerCount;
    }
    
    public ObjectName[] getHandlersObjectName() {
        
        ObjectName onames[]=new ObjectName[ handlerCount ];
        for( int i=0; i<handlerCount; i++ ) {
            onames[i]=handlersTable[i].getObjectName();
        }
        return onames;
    }

}
