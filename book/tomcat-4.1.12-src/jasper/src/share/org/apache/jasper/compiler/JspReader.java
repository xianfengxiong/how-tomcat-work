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

import java.io.*;
import java.util.*;
import org.apache.jasper.Constants;
import org.apache.jasper.JasperException;
import org.apache.jasper.JspCompilationContext;
import org.apache.jasper.logging.*;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * JspReader is an input buffer for the JSP parser. It should allow
 * unlimited lookahead and pushback. It also has a bunch of parsing
 * utility methods for understanding htmlesque thingies.
 *
 * @author Anil K. Vijendran
 * @author Anselm Baird-Smith
 * @author Harish Prabandham
 * @author Rajiv Mordani
 * @author Mandar Raje
 * @author Danno Ferrin
 */

public class JspReader {

    protected Mark current  = null;
    String master = null;

    Vector sourceFiles = new Vector();
    int currFileId = 0;
    int size = 0;
    
    private JspCompilationContext context;
    private ErrorDispatcher err;

    /*
     * Set to true when using the JspReader on a single file where we read up
     * to the end and reset to the beginning many times.
     * (as in ParserCtl.figureOutJspDocument().
     */
    boolean singleFile = false;

    Logger.Helper loghelper = new Logger.Helper("JASPER_LOG", "JspReader");
    
    public String getFile(int fileid) {
	return (String) sourceFiles.elementAt(fileid);
    }

    /**
     * Register a new source file.
     * This method is used to implement file inclusion. Each included file
     * gets a uniq identifier (which is the index in the array of source files).
     * @return The index of the now registered file.
     */
    protected int registerSourceFile(String file) {
        if (sourceFiles.contains(file))
            return -1;
	sourceFiles.addElement(file);
	this.size++;
	return sourceFiles.size() - 1;
    }
    

    /**
     * Unregister the source file.
     * This method is used to implement file inclusion. Each included file
     * gets a uniq identifier (which is the index in the array of source
     * files).
     * @return The index of the now registered file.
     */
    protected int unregisterSourceFile(String file) {
        if (!sourceFiles.contains(file))
            return -1;
	sourceFiles.removeElement(file);
	this.size--;
	return sourceFiles.size() - 1;
    }

    private void pushFile2(String file, String encoding, 
			   InputStreamReader reader) 
	        throws JasperException, FileNotFoundException {

	// Register the file
	String longName = file;

	int fileid = registerSourceFile(longName);

        if (fileid == -1) {
            err.jspError("jsp.error.file.already.registered", file);
	}

	currFileId = fileid;

	try {
	    CharArrayWriter caw = new CharArrayWriter();
	    char buf[] = new char[1024];
	    for (int i = 0 ; (i = reader.read(buf)) != -1 ;)
		caw.write(buf, 0, i);
	    caw.close();
	    if (current == null) {
		current = new Mark(this, caw.toCharArray(), fileid, 
				   getFile(fileid), master, encoding);
	    } else {
		current.pushStream(caw.toCharArray(), fileid, getFile(fileid),
				   longName, encoding);
	    }
	} catch (Throwable ex) {
	    loghelper.log("Exception parsing file ", ex);
	    // Pop state being constructed:
	    popFile();
	    err.jspError("jsp.error.file.cannot.read", "ze file");
	} finally {
	    if (reader != null) {
		try { reader.close(); } catch (Exception any) {}
	    }
	}
    }

    public boolean popFile() throws JasperException {
	// Is stack created ? (will happen if the Jsp file we'r looking at is
	// missing.
	if (current == null) 
		return false;

	// Restore parser state:
	//size--;
	if (currFileId < 0) {
	    err.jspError("jsp.error.no.more.content");
	}
	
	String fName = getFile(currFileId);
	currFileId = unregisterSourceFile(fName);
	if (currFileId < -1) {
	    err.jspError("jsp.error.file.not.registered", fName);
	}

	boolean result = current.popStream();
	if (result)
	    master = current.baseDir;
	return (result);
    }
	
    protected JspReader(JspCompilationContext ctx,
			String file,
			String encoding,
			InputStreamReader reader,
			ErrorDispatcher err) 
	        throws JasperException, FileNotFoundException {
        this.context = ctx;
	this.err = err;
	pushFile2(file, encoding, reader);
    }

    public boolean hasMoreInput() throws JasperException {
	if (current.cursor >= current.stream.length) {
            if (singleFile) return false; 
	    while (popFile()) {
		if (current.cursor < current.stream.length) return true;
	    }
	    return false;
	}
	return true;
    }
    
    public int nextChar() throws JasperException {
	if (!hasMoreInput())
	    return -1;
	
	int ch = current.stream[current.cursor];

	current.cursor++;
	
	if (ch == '\n') {
	    current.line++;
	    current.col = 0;
	} else {
	    current.col++;
	}
	return ch;
    }

    /**
     * Gets Content until the next potential JSP element.  Because all elements
     * begin with a '&lt;' we can just move until we see the next one.
     */
    char[] nextContent() {
        int cur_cursor = current.cursor;
	int len = current.stream.length;
 	char ch;

	if (peekChar() == '\n') {
	    current.line++;
	    current.col = 0;
	}
	else current.col++;
	
	// pure obsfuscated genius!
        while ((++current.cursor < len) && 
	    ((ch = current.stream[current.cursor]) != '<')) {

	    if (ch == '\n') {
		current.line++;
		current.col = 0;
	    } else {
  		current.col++;
	    }
	}

	len = current.cursor - cur_cursor;
	char[] content = new char[len];
	System.arraycopy(current.stream, cur_cursor, content, 0, len);
	
	return content;
    }

    char[] getText(Mark start, Mark stop) throws JasperException {
	Mark oldstart = mark();
	reset(start);
	CharArrayWriter caw = new CharArrayWriter();
	while (!stop.equals(mark()))
	    caw.write(nextChar());
	caw.close();
	reset(oldstart);
	return caw.toCharArray();
    }

    public int peekChar() {
	return current.stream[current.cursor];
    }

    public Mark mark() {
	return new Mark(current);
    }

    public void reset(Mark mark) {
	current = new Mark(mark);
    }

    public boolean matchesIgnoreCase(String string) throws JasperException {
	Mark mark = mark();
	int ch = 0;
	int i = 0;
	do {
	    ch = nextChar();
	    if (Character.toLowerCase((char) ch) != string.charAt(i++)) {
		reset(mark);
		return false;
	    }
	} while (i < string.length());
	reset(mark);
	return true;
    }

    /**
     * search the stream for a match to a string
     * @param string The string to match
     * @return <stront>true</strong> is one is found, the current position
     *         in stream is positioned after the search string, <strong>
     *	       false</strong> otherwise, position in stream unchanged.
     */
    public boolean matches(String string) throws JasperException {
	Mark mark = mark();
	int ch = 0;
	int i = 0;
	do {
	    ch = nextChar();
	    if (((char) ch) != string.charAt(i++)) {
		reset(mark);
		return false;
	    }
	} while (i < string.length());
	return true;
    }

    public boolean matchesETag(String tagName) throws JasperException {
	Mark mark = mark();

	if (!matches("</" + tagName))
	    return false;
	skipSpaces();
	if (nextChar() == '>')
	    return true;

	reset(mark);
	return false;
    }
    
    public void advance(int n) throws JasperException {
	while (--n >= 0)
	    nextChar();
    }

    public int skipSpaces() throws JasperException {
	int i = 0;
	while (isSpace()) {
	    i++;
	    nextChar();
	}
	return i;
    }

    /**
     * Skip until the given string is matched in the stream.
     * When returned, the context is positioned past the end of the match.
     * @param s The String to match.
     * @return A non-null <code>Mark</code> instance (positioned immediately
     *         before the search string) if found, <strong>null</strong>
     *         otherwise.
     */
    public Mark skipUntil(String limit) throws JasperException {
	Mark ret = null;
	int limlen = limit.length();
	int ch;
	
    skip:
	for (ret = mark(), ch = nextChar() ; ch != -1 ;
	         ret = mark(), ch = nextChar()) {	    
	    if (ch == limit.charAt(0)) {
                Mark restart = mark();
		for (int i = 1 ; i < limlen ; i++) {
		    if (peekChar() == limit.charAt(i))
			nextChar();
		    else {
                        reset(restart);
			continue skip;
                    }
		}
		return ret;
	    }
	}
	return null;
    }
    
    /**
     * Skip until the given string is matched in the stream, but ignoring
     * chars initially escaped by a '\'.
     * When returned, the context is positioned past the end of the match.
     * @param s The String to match.
     * @return A non-null <code>Mark</code> instance (positioned immediately
     *         before the search string) if found, <strong>null</strong>
     *         otherwise.
     */
    public Mark skipUntilIgnoreEsc(String limit) throws JasperException {
        Mark ret = null;
        int limlen = limit.length();
        int ch;
        int prev = 'x'; // Doesn't matter

    skip:
        for (ret = mark(), ch = nextChar() ; ch != -1 ;
                 ret = mark(), prev = ch, ch = nextChar()) {
            if (ch == limit.charAt(0) && prev != '\\') {
                for (int i = 1 ; i < limlen ; i++) {
                    if (peekChar() == limit.charAt(i))
                        nextChar();
                    else
                        continue skip;
                }
                return ret;
            }
        }
        return null;
    }

    /**
     * Skip until the given end tag is matched in the stream.
     * When returned, the context is positioned past the end of the tag.
     * @param tag The name of the tag whose ETag (</tag>) to match.
     * @return A non-null <code>Mark</code> instance (positioned immediately
     *	       before the ETag) if found, <strong>null</strong> otherwise.
     */
    public Mark skipUntilETag(String tag) throws JasperException {
	Mark ret = skipUntil("</" + tag);
	if (ret != null) {
	    skipSpaces();
	    if (nextChar() != '>')
		ret = null;
	}
	return ret;
    }

    final boolean isSpace() {
	return peekChar() <= ' ';
    }

    /**
     * Parse a space delimited token.
     * If quoted the token will consume all characters up to a matching quote,
     * otherwise, it consumes up to the first delimiter character.
     * @param quoted If <strong>true</strong> accept quoted strings.
     */
    public String parseToken(boolean quoted) throws JasperException {
	StringBuffer stringBuffer = new StringBuffer();
	skipSpaces();
	stringBuffer.setLength(0);
	
	int ch = peekChar();
	
	if (quoted) {
	    if (ch == '"' || ch == '\'') {

		char endQuote = ch == '"' ? '"' : '\'';
		// Consume the open quote: 
		ch = nextChar();
		for (ch = nextChar(); ch != -1 && ch != endQuote;
		         ch = nextChar()) {
		    if (ch == '\\') 
			ch = nextChar();
		    stringBuffer.append((char) ch);
		}
		// Check end of quote, skip closing quote:
		if (ch == -1) {
		    err.jspError(mark(), "jsp.error.quotes.unterminated");
		}
	    } else {
		err.jspError(mark(), "jsp.error.attr.quoted");
	    }
	} else {
	    if (!isDelimiter()) {
		// Read value until delimiter is found:
		do {
		    ch = nextChar();
		    // Take care of the quoting here.
		    if (ch == '\\') {
			if (peekChar() == '"' || peekChar() == '\'' ||
			       peekChar() == '>' || peekChar() == '%')
			    ch = nextChar();
		    }
		    stringBuffer.append((char) ch);
		} while (!isDelimiter());
	    }
	}

	return stringBuffer.toString();
    }

    /**
     * Parse utils - Is current character a token delimiter ?
     * Delimiters are currently defined to be =, &gt;, &lt;, ", and ' or any
     * any space character as defined by <code>isSpace</code>.
     * @return A boolean.
     */
    private boolean isDelimiter() throws JasperException {
	if (! isSpace()) {
	    int ch = peekChar();
	    // Look for a single-char work delimiter:
	    if (ch == '=' || ch == '>' || ch == '"' || ch == '\''
		    || ch == '/') {
		return true;
	    }
	    // Look for an end-of-comment or end-of-tag:		
	    if (ch == '-') {
		Mark mark = mark();
		if (((ch = nextChar()) == '>')
		        || ((ch == '-') && (nextChar() == '>'))) {
		    reset(mark);
		    return true;
		} else {
		    reset(mark);
		    return false;
		}
	    }
	    return false;
	} else {
	    return true;
	}
    }

    public void setSingleFile(boolean val) {
        singleFile = val;
    }
}

