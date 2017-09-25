/*
 * $Header: /home/cvs/jakarta-tomcat-jasper/jasper2/src/share/org/apache/jasper/compiler/DefaultErrorHandler.java,v 1.8 2003/10/22 23:52:55 kinman Exp $
 * $Revision: 1.8 $
 * $Date: 2003/10/22 23:52:55 $
 *
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
 */ 
package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;

/**
 * Default implementation of ErrorHandler interface.
 *
 * @author Jan Luehe
 */
class DefaultErrorHandler implements ErrorHandler {

    /*
     * Processes the given JSP parse error.
     *
     * @param fname Name of the JSP file in which the parse error occurred
     * @param line Parse error line number
     * @param column Parse error column number
     * @param errMsg Parse error message
     * @param exception Parse exception
     */
    public void jspError(String fname, int line, int column, String errMsg,
			 Exception ex) throws JasperException {
	throw new JasperException(fname + "(" + line + "," + column + ")"
				  + " " + errMsg, ex);
    }

    /*
     * Processes the given JSP parse error.
     *
     * @param errMsg Parse error message
     * @param exception Parse exception
     */
    public void jspError(String errMsg, Exception ex) throws JasperException {
	throw new JasperException(errMsg, ex);
    }

    /*
     * Processes the given javac compilation errors.
     *
     * @param details Array of JavacErrorDetail instances corresponding to the
     * compilation errors
     */
    public void javacError(JavacErrorDetail[] details) throws JasperException {

	Object[] args = null;
        StringBuffer buf = new StringBuffer();
	
        if ((details.length == 1) 
            && (details[0].getJavaLineNumber() == -1)) {
            // Special case: No Java compiler found
            buf.append(Localizer.getMessage("jsp.error.nojavac"));
            buf.append('\n');
        } else {
            for (int i=0; i < details.length; i++) {
                args = new Object[] {
                    new Integer(details[i].getJspBeginLineNumber()), 
                    details[i].getJspFileName()
                };
                buf.append(Localizer.getMessage("jsp.error.single.line.number",
                                                args));
                buf.append(Localizer.getMessage("jsp.error.corresponding.servlet"));
                buf.append(details[i].getErrorMessage());
                buf.append('\n');
            }
        }

	throw new JasperException(Localizer.getMessage("jsp.error.unable.compile")
				  + buf);
    }
}
