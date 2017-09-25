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

package org.apache.coyote.tomcat4;

import java.io.IOException;
import java.io.Writer;
import java.util.Hashtable;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.coyote.ActionCode;
import org.apache.coyote.Response;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.C2BConverter;
import org.apache.tomcat.util.buf.CharChunk;


/**
 * The buffer used by Tomcat response. This is a derivative of the Tomcat 3.3
 * OutputBuffer, with the removal of some of the state handling (which in 
 * Coyote is mostly the Processor's responsability).
 *
 * @author Costin Manolache
 * @author Remy Maucherat
 */
public class OutputBuffer extends Writer
    implements ByteChunk.ByteOutputChannel, CharChunk.CharOutputChannel {


    // -------------------------------------------------------------- Constants


    public static final String DEFAULT_ENCODING = 
        org.apache.coyote.Constants.DEFAULT_CHARACTER_ENCODING;
    public static final int DEFAULT_BUFFER_SIZE = 8*1024;
    static final int debug = 0;


    // The buffer can be used for byte[] and char[] writing
    // ( this is needed to support ServletOutputStream and for
    // efficient implementations of templating systems )
    public final int INITIAL_STATE = 0;
    public final int CHAR_STATE = 1;
    public final int BYTE_STATE = 2;


    // ----------------------------------------------------- Instance Variables


    /**
     * The byte buffer.
     */
    private ByteChunk bb;


    /**
     * The chunk buffer.
     */
    private CharChunk cb;


    /**
     * State of the output buffer.
     */
    private int state = 0;


    /**
     * Number of bytes written.
     */
    private int bytesWritten = 0;


    /**
     * Number of chars written.
     */
    private int charsWritten = 0;


    /**
     * Flag which indicates if the output buffer is closed.
     */
    private boolean closed = false;


    /**
     * Do a flush on the next operation.
     */
    private boolean doFlush = false;


    /**
     * Byte chunk used to output bytes.
     */
    private ByteChunk outputChunk = new ByteChunk();


    /**
     * Encoding to use.
     */
    private String enc;


    /**
     * Encoder is set.
     */
    private boolean gotEnc = false;


    /**
     * List of encoders.
     */
    protected Hashtable encoders = new Hashtable();


    /**
     * Current char to byte converter.
     */
    protected C2BConverter conv;


    /**
     * Associated Coyote response.
     */
    private Response coyoteResponse;


    /**
     * Suspended flag. All output bytes will be swallowed if this is true.
     */
    private boolean suspended = false;


    // ----------------------------------------------------------- Constructors


    /**
     * Default constructor. Allocate the buffer with the default buffer size.
     */
    public OutputBuffer() {

        this(DEFAULT_BUFFER_SIZE);

    }


    /**
     * Alternate constructor which allows specifying the initial buffer size.
     * 
     * @param size Buffer size to use
     */
    public OutputBuffer(int size) {

        bb = new ByteChunk(size);
        bb.setLimit(size);
        bb.setByteOutputChannel(this);
        cb = new CharChunk(size);
        cb.setCharOutputChannel(this);
        cb.setLimit(size);

    }


    // ------------------------------------------------------------- Properties


    /**
     * Associated Coyote response.
     * 
     * @param coyoteResponse Associated Coyote response
     */
    public void setResponse(Response coyoteResponse) {
	this.coyoteResponse = coyoteResponse;
    }


    /**
     * Get associated Coyote response.
     * 
     * @return the associated Coyote response
     */
    public Response getResponse() {
        return this.coyoteResponse;
    }


    /**
     * Is the response output suspended ?
     * 
     * @return suspended flag value
     */
    public boolean isSuspended() {
        return this.suspended;
    }


    /**
     * Set the suspended flag.
     * 
     * @param suspended New suspended flag value
     */
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Recycle the output buffer.
     */
    public void recycle() {

	if (debug > 0)
            log("recycle()");

	state = INITIAL_STATE;
	bytesWritten = 0;
	charsWritten = 0;

        cb.recycle();
        bb.recycle(); 
        closed = false;
        suspended = false;

        if (conv!= null) {
            conv.recycle();
        }

        gotEnc = false;
        enc = null;

    }


    /**
     * Close the output buffer. This tries to calculate the response size if 
     * the response has not been committed yet.
     * 
     * @throws IOException An underlying IOException occurred
     */
    public void close()
        throws IOException {

        if (closed)
            return;
        if (suspended)
            return;

        if ((!coyoteResponse.isCommitted()) 
            && (coyoteResponse.getContentLength() == -1)) {
            // Flushing the char buffer
            if (state == CHAR_STATE) {
                cb.flushBuffer();
                state = BYTE_STATE;
            }
            // If this didn't cause a commit of the response, the final content
            // length can be calculated
            if (!coyoteResponse.isCommitted()) {
                coyoteResponse.setContentLength(bb.getLength());
            }
        }

        doFlush(false);
        closed = true;

        coyoteResponse.finish();

    }


    /**
     * Flush bytes or chars contained in the buffer.
     * 
     * @throws IOException An underlying IOException occurred
     */
    public void flush()
        throws IOException {
        doFlush(true);
    }


    /**
     * Flush bytes or chars contained in the buffer.
     * 
     * @throws IOException An underlying IOException occurred
     */
    protected void doFlush(boolean realFlush)
        throws IOException {

        if (suspended)
            return;

        doFlush = true;
        if (state == CHAR_STATE) {
            cb.flushBuffer();
            bb.flushBuffer();
            state = BYTE_STATE;
        } else if (state == BYTE_STATE) {
            bb.flushBuffer();
        } else if (state == INITIAL_STATE)
            realWriteBytes(null, 0, 0);       // nothing written yet
        doFlush = false;

        if (realFlush) {
            coyoteResponse.action(ActionCode.ACTION_CLIENT_FLUSH, 
                                  coyoteResponse);
            // If some exception occurred earlier, or if some IOE occurred
            // here, notify the servlet with an IOE
            if (coyoteResponse.isExceptionPresent()) {
                throw new ClientAbortException
                    (coyoteResponse.getErrorException());
            }
        }

    }


    // ------------------------------------------------- Bytes Handling Methods


    /** 
     * Sends the buffer data to the client output, checking the
     * state of Response and calling the right interceptors.
     * 
     * @param buf Byte buffer to be written to the response
     * @param off Offset
     * @param cnt Length
     * 
     * @throws IOException An underlying IOException occurred
     */
    public void realWriteBytes(byte buf[], int off, int cnt)
	throws IOException {

        if (debug > 2)
            log("realWrite(b, " + off + ", " + cnt + ") " + coyoteResponse);

        if (closed)
            return;
        if (coyoteResponse == null)
            return;

        // If we really have something to write
        if (cnt > 0) {
            // real write to the adapter
            outputChunk.setBytes(buf, off, cnt);
            try {
                coyoteResponse.doWrite(outputChunk);
            } catch (IOException e) {
                // An IOException on a write is almost always due to
                // the remote client aborting the request.  Wrap this
                // so that it can be handled better by the error dispatcher.
                throw new ClientAbortException(e);
            }
        }

    }


    public void write(byte b[], int off, int len) throws IOException {

        if (suspended)
            return;

        if (state == CHAR_STATE)
            cb.flushBuffer();
        state = BYTE_STATE;
        writeBytes(b, off, len);

    }


    private void writeBytes(byte b[], int off, int len) 
        throws IOException {

        if (closed)
            return;
        if (debug > 0)
            log("write(b,off,len)");

        bb.append(b, off, len);
        bytesWritten += len;

        // if called from within flush(), then immediately flush
        // remaining bytes
        if (doFlush) {
            bb.flushBuffer();
        }

    }


    // XXX Char or byte ?
    public void writeByte(int b)
        throws IOException {

        if (suspended)
            return;

        if (state == CHAR_STATE)
            cb.flushBuffer();
        state = BYTE_STATE;

        if (debug > 0)
            log("write(b)");

        bb.append( (byte)b );
        bytesWritten++;

    }


    // ------------------------------------------------- Chars Handling Methods


    public void write(int c)
        throws IOException {

        if (suspended)
            return;

        state = CHAR_STATE;

        if (debug > 0)
            log("writeChar(b)");

        cb.append((char) c);
        charsWritten++;

    }


    public void write(char c[])
        throws IOException {

        if (suspended)
            return;

        write(c, 0, c.length);

    }


    public void write(char c[], int off, int len)
        throws IOException {

        if (suspended)
            return;

        state = CHAR_STATE;

        if (debug > 0)
            log("write(c,off,len)" + cb.getLength() + " " + cb.getLimit());

        cb.append(c, off, len);
        charsWritten += len;

    }


    public void write(StringBuffer sb)
        throws IOException {

        if (suspended)
            return;

        state = CHAR_STATE;

        if (debug > 1)
            log("write(s,off,len)");

        int len = sb.length();
        charsWritten += len;
        cb.append(sb);

    }


    /**
     * Append a string to the buffer
     */
    public void write(String s, int off, int len)
        throws IOException {

        if (suspended)
            return;

        state=CHAR_STATE;

        if (debug > 1)
            log("write(s,off,len)");

        charsWritten += len;
        if (s==null)
            s="null";
        cb.append( s, off, len );

    }


    public void write(String s)
        throws IOException {

        if (suspended)
            return;

        state = CHAR_STATE;
        if (s==null)
            s="null";
        write(s, 0, s.length());

    } 


    public void flushChars()
        throws IOException {

        if (debug > 0)
            log("flushChars() " + cb.getLength());

        cb.flushBuffer();
        state = BYTE_STATE;

    }


    public boolean flushCharsNeeded() {
        return state == CHAR_STATE;
    }


    public void setEncoding(String s) {
        enc = s;
    }


    public void realWriteChars(char c[], int off, int len) 
        throws IOException {

        if (debug > 0)
            log("realWrite(c,o,l) " + cb.getOffset() + " " + len);

        if (!gotEnc)
            setConverter();

        if (debug > 0)
            log("encoder:  " + conv + " " + gotEnc);

        conv.convert(c, off, len);
        conv.flushBuffer();	// ???

    }


    protected void setConverter() {

        if (coyoteResponse != null)
            enc = coyoteResponse.getCharacterEncoding();

        if (debug > 0)
            log("Got encoding: " + enc);

        gotEnc = true;
        if (enc == null)
            enc = DEFAULT_ENCODING;
        conv = (C2BConverter) encoders.get(enc);
        if (conv == null) {
            try {
                conv = new C2BConverter(bb, enc);
                encoders.put(enc, conv);
            } catch (IOException e) {
                conv = (C2BConverter) encoders.get(DEFAULT_ENCODING);
                if (conv == null) {
                    try {
                        conv = new C2BConverter(bb, DEFAULT_ENCODING);
                        encoders.put(DEFAULT_ENCODING, conv);
                    } catch (IOException ex) {
                        // Ignore
                    }
                }
            }
        }
    }

    
    // --------------------  BufferedOutputStream compatibility


    /**
     * Real write - this buffer will be sent to the client
     */
    public void flushBytes()
        throws IOException {

        if (debug > 0)
            log("flushBytes() " + bb.getLength());
        bb.flushBuffer();

    }


    public int getBytesWritten() {
        return bytesWritten;
    }


    public int getCharsWritten() {
        return charsWritten;
    }


    public int getContentWritten() {
        return bytesWritten + charsWritten;
    }


    /** 
     * True if this buffer hasn't been used ( since recycle() ) -
     * i.e. no chars or bytes have been added to the buffer.  
     */
    public boolean isNew() {
        return (bytesWritten == 0) && (charsWritten == 0);
    }


    public void setBufferSize(int size) {
        if (size > bb.getLimit()) {// ??????
	    bb.setLimit(size);
	}
    }


    public void reset() {

        //count=0;
        bb.recycle();
        bytesWritten = 0;
        cb.recycle();
        charsWritten = 0;
        gotEnc = false;
        enc = null;

    }


    public int getBufferSize() {
	return bb.getLimit();
    }



    protected void log( String s ) {
	System.out.println("OutputBuffer: " + s);
    }


}
