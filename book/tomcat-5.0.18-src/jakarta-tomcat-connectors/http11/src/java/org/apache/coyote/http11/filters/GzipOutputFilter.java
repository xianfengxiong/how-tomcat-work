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

package org.apache.coyote.http11.filters;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.tomcat.util.buf.ByteChunk;

import org.apache.coyote.OutputBuffer;
import org.apache.coyote.Response;
import org.apache.coyote.http11.OutputFilter;

/**
 * Gzip output filter.
 * 
 * @author Remy Maucherat
 */
public class GzipOutputFilter implements OutputFilter {


    // -------------------------------------------------------------- Constants


    protected static final String ENCODING_NAME = "gzip";
    protected static final ByteChunk ENCODING = new ByteChunk();


    // ----------------------------------------------------- Static Initializer


    static {
        ENCODING.setBytes(ENCODING_NAME.getBytes(), 0, ENCODING_NAME.length());
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * Next buffer in the pipeline.
     */
    protected OutputBuffer buffer;


    /**
     * Compression output stream.
     */
    protected GZIPOutputStream compressionStream = null;


    /**
     * Fake internal output stream.
     */
    protected OutputStream fakeOutputStream = new FakeOutputStream();


    // --------------------------------------------------- OutputBuffer Methods


    /**
     * Write some bytes.
     * 
     * @return number of bytes written by the filter
     */
    public int doWrite(ByteChunk chunk, Response res)
        throws IOException {
        if (compressionStream == null) {
            compressionStream = new GZIPOutputStream(fakeOutputStream);
        }
        compressionStream.write(chunk.getBytes(), chunk.getStart(), 
                                chunk.getLength());
        return chunk.getLength();
    }


    // --------------------------------------------------- OutputFilter Methods


    /**
     * Some filters need additional parameters from the response. All the 
     * necessary reading can occur in that method, as this method is called
     * after the response header processing is complete.
     */
    public void setResponse(Response response) {
    }


    /**
     * Set the next buffer in the filter pipeline.
     */
    public void setBuffer(OutputBuffer buffer) {
        this.buffer = buffer;
    }


    /**
     * End the current request. It is acceptable to write extra bytes using
     * buffer.doWrite during the execution of this method.
     */
    public long end()
        throws IOException {
        if (compressionStream == null) {
            compressionStream = new GZIPOutputStream(fakeOutputStream);
        }
        compressionStream.finish();
        return ((OutputFilter) buffer).end();
    }


    /**
     * Make the filter ready to process the next request.
     */
    public void recycle() {
        // Set compression stream to null
        compressionStream = null;
    }


    /**
     * Return the name of the associated encoding; Here, the value is 
     * "identity".
     */
    public ByteChunk getEncodingName() {
        return ENCODING;
    }


    // ------------------------------------------- FakeOutputStream Inner Class


    protected class FakeOutputStream
        extends OutputStream {
        protected ByteChunk outputChunk = new ByteChunk();
        protected byte[] singleByteBuffer = new byte[1];
        public void write(int b)
            throws IOException {
            // Shouldn't get used for good performance, but is needed for 
            // compatibility with Sun JDK 1.4.0
            singleByteBuffer[0] = (byte) (b & 0xff);
            outputChunk.setBytes(singleByteBuffer, 0, 1);
            buffer.doWrite(outputChunk, null);
        }
        public void write(byte[] b, int off, int len)
            throws IOException {
            outputChunk.setBytes(b, off, len);
            buffer.doWrite(outputChunk, null);
        }
        public void flush() throws IOException {}
        public void close() throws IOException {}
    }


}
