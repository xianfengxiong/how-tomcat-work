/*
 * The Apache Software License, Version 1.1
 *
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
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xerces" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
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
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.jasper.xmlparser;

import java.io.IOException;
import java.util.jar.JarFile;

import org.apache.jasper.JasperException;
import org.apache.jasper.JspCompilationContext;
import org.apache.jasper.compiler.ErrorDispatcher;


public class XMLEncodingDetector {

    /**
     * Autodetects the encoding of the XML document supplied by the given
     * input stream.
     *
     * Encoding autodetection is done according to the XML 1.0 specification,
     * Appendix F.1: Detection Without External Encoding Information.
     *
     * @param in The input stream to read
     * @param err The error dispatcher
     *
     * @return Two-element array, where the first element (of type
     * java.lang.String) contains the name of the autodetected encoding, and
     * the second element (of type java.lang.Boolean) specifies whether the 
     * encoding was specified by the encoding attribute of an XML declaration
     * (prolog).
     */
    public static Object[] getEncoding(String fname, JarFile jarFile,
                                       JspCompilationContext ctxt,
                                       ErrorDispatcher err)
            throws IOException, JasperException
    {
        XMLEncodingDetector detector=null;
        try {
            Class.forName("org.apache.xerces.util.SymbolTable");
            Class detectorClass=Class.forName("org.apache.jasper.xmlparser.XercesEncodingDetector");
            detector = (XMLEncodingDetector) detectorClass.newInstance();
        } catch(Exception ex ) {
            detector = new XMLEncodingDetector();
        }
        return detector.getEncodingMethod(fname, jarFile, ctxt, err);
    }

    public Object[] getEncodingMethod(String fname, JarFile jarFile,
				      JspCompilationContext ctxt,
				      ErrorDispatcher err)
	throws IOException, JasperException
    {
        Object result[] = new Object[] { "UTF8", new Boolean(false) };
        return result;
    }
}


