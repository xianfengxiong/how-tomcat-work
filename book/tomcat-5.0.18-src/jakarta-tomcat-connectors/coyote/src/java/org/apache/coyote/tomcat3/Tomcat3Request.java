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


package org.apache.coyote.tomcat3;

import java.io.IOException;

import org.apache.coyote.ActionCode;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.MessageBytes;

/** The Request to connect with Coyote.
 *  This class handles the I/O requirements and transferring the request
 *  line and Mime headers between Coyote and Tomcat.
 * 
 *  @author Bill Barker
 *  @author Costin Manolache
 */
public class Tomcat3Request extends org.apache.tomcat.core.Request {

    org.apache.coyote.Request coyoteRequest=null;

    // For SSL attributes we need to call an ActionHook to get
    // info from the protocol handler.
    //    SSLSupport sslSupport=null;

    ByteChunk  readChunk = new ByteChunk(8096);
    int  pos=-1;
    int  end=-1;
    byte [] readBuffer = null;


    public Tomcat3Request() {
        super();
        remoteAddrMB.recycle();
        remoteHostMB.recycle();
    }

    public void recycle() {
	super.recycle();
	if( coyoteRequest != null) coyoteRequest.recycle();

        remoteAddrMB.recycle();
        remoteHostMB.recycle();
	readChunk.recycle();

	readBuffer=null;
	pos=-1;
	end=-1;
    }

    public org.apache.coyote.Request getCoyoteRequest() {
        return coyoteRequest;
    }
    
    /** Attach the Coyote Request to this Request.
     *  This is currently set pre-request to allow copying the request
     *  attributes to the Tomcat attributes.
     */
    public void setCoyoteRequest(org.apache.coyote.Request cReq) {
        coyoteRequest=cReq;

        // The CoyoteRequest/Tomcat3Request are bound togheter, they
        // don't change. That means we can use the same field ( which
        // doesn't change as well.
        schemeMB = coyoteRequest.scheme();
        methodMB = coyoteRequest.method();
        uriMB = coyoteRequest.requestURI();
        queryMB = coyoteRequest.query();
        protoMB = coyoteRequest.protocol();

	headers  = coyoteRequest.getMimeHeaders();
	scookies.setHeaders(headers);
	params.setHeaders(headers);
        params.setQuery( queryMB );
        
        remoteAddrMB = coyoteRequest.remoteAddr();
	remoteHostMB = coyoteRequest.remoteHost();
	serverNameMB = coyoteRequest.serverName();

        
    }
    
    /** Read a single character from the request body.
     */
    public int doRead() throws IOException {
	if( available == 0 ) 
	    return -1;
	// #3745
	// if available == -1: unknown length, we'll read until end of stream.
	if( available!= -1 )
	    available--;
	if(pos >= end) {
	    if(readBytes() < 0)
		return -1;
	}
	return readBuffer[pos++] & 0xFF;
    }

    /** Read a chunk from the request body.
     */
    public int doRead(byte[] b, int off, int len) throws IOException {
	if( available == 0 )
	    return -1;
	// if available == -1: unknown length, we'll read until end of stream.
	if(pos >= end) {
	    if(readBytes() <= 0) 
		return -1;
	}
	int rd = -1;
	if((end - pos) > len) {
	    rd = len;
	} else {
	    rd = end - pos;
	}

        System.arraycopy(readBuffer, pos, b, off, rd);
	pos += rd;
	if( available!= -1 )
	    available -= rd;

	return rd;
    }
    
    /**
     * Read bytes to the read chunk buffer.
     */
    protected int readBytes()
        throws IOException {

        int result = coyoteRequest.doRead(readChunk);
        if (result > 0) {
            readBuffer = readChunk.getBytes();
            end = readChunk.getEnd();
            pos = readChunk.getStart();
        } else if( result < 0 ) {
            throw new IOException( "Read bytes failed " + result );
        }
        return result;

    }

    // -------------------- override special methods

    public MessageBytes remoteAddr() {
	if( remoteAddrMB.isNull() ) {
	    coyoteRequest.action( ActionCode.ACTION_REQ_HOST_ADDR_ATTRIBUTE, coyoteRequest );
	}
	return remoteAddrMB;
    }

    public MessageBytes remoteHost() {
	if( remoteHostMB.isNull() ) {
	    coyoteRequest.action( ActionCode.ACTION_REQ_HOST_ATTRIBUTE, coyoteRequest );
	}
	return remoteHostMB;
    }

    public String getLocalHost() {
	return localHost;
    }

    public MessageBytes serverName(){
        // That's set by protocol in advance, it's needed for mapping anyway,
        // no need to do lazy eval.
        return coyoteRequest.serverName();
    }

    public int getServerPort(){
        return coyoteRequest.getServerPort();
    }
    
    public void setServerPort(int i ) {
	coyoteRequest.setServerPort( i );
    }


    public  void setRemoteUser( String s ) {
	super.setRemoteUser(s);
	coyoteRequest.getRemoteUser().setString(s);
    }

    public String getRemoteUser() {
	String s=coyoteRequest.getRemoteUser().toString();
	if( s == null )
	    s=super.getRemoteUser();
	return s;
    }

    public String getAuthType() {
	return coyoteRequest.getAuthType().toString();
    }
    
    public void setAuthType(String s ) {
	coyoteRequest.getAuthType().setString(s);
    }

    public String getJvmRoute() {
	return coyoteRequest.instanceId().toString();
    }
    
    public void setJvmRoute(String s ) {
	coyoteRequest.instanceId().setString(s);
    }

    public boolean isSecure() {
	return "https".equalsIgnoreCase( coyoteRequest.scheme().toString());
    }
}
