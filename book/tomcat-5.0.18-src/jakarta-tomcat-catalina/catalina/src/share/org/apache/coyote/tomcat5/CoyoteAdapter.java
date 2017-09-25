/* * $Header: /home/cvs/jakarta-tomcat-catalina/catalina/src/share/org/apache/coyote/tomcat5/CoyoteAdapter.java,v 1.16 2004/01/10 20:39:50 remm Exp $
 * $Revision: 1.16 $
 * $Date: 2004/01/10 20:39:50 $
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
 * [Additional notices, if required by prior licensing conditions]
 *
 */


package org.apache.coyote.tomcat5;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Wrapper;
import org.apache.catalina.util.StringManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.coyote.ActionCode;
import org.apache.coyote.Adapter;
import org.apache.coyote.Request;
import org.apache.coyote.Response;
import org.apache.tomcat.util.buf.B2CConverter;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.CharChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.Cookies;
import org.apache.tomcat.util.http.ServerCookie;


/**
 * Implementation of a request processor which delegates the processing to a
 * Coyote processor.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.16 $ $Date: 2004/01/10 20:39:50 $
 */

final class CoyoteAdapter
    implements Adapter 
 {
    private static Log log = LogFactory.getLog(CoyoteAdapter.class);

    // -------------------------------------------------------------- Constants


    public static final int ADAPTER_NOTES = 1;


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new CoyoteProcessor associated with the specified connector.
     *
     * @param connector CoyoteConnector that owns this processor
     * @param id Identifier of this CoyoteProcessor (unique per connector)
     */
    public CoyoteAdapter(CoyoteConnector connector) {

        super();
        this.connector = connector;
        this.debug = connector.getDebug();

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The CoyoteConnector with which this processor is associated.
     */
    private CoyoteConnector connector = null;


    /**
     * The debugging detail level for this component.
     */
    private int debug = 0;


    /**
     * The match string for identifying a session ID parameter.
     */
    private static final String match =
        ";" + Globals.SESSION_PARAMETER_NAME + "=";


    /**
     * The match string for identifying a session ID parameter.
     */
    private static final char[] SESSION_ID = match.toCharArray();


    /**
     * The string manager for this package.
     */
    protected StringManager sm =
        StringManager.getManager(Constants.Package);


    // -------------------------------------------------------- Adapter Methods


    /**
     * Service method.
     */
    public void service(Request req, Response res)
        throws Exception {

        CoyoteRequest request = (CoyoteRequest) req.getNote(ADAPTER_NOTES);
        CoyoteResponse response = (CoyoteResponse) res.getNote(ADAPTER_NOTES);

        if (request == null) {

            // Create objects
            request = (CoyoteRequest) connector.createRequest();
            request.setCoyoteRequest(req);
            response = (CoyoteResponse) connector.createResponse();
            response.setCoyoteResponse(res);

            // Link objects
            request.setResponse(response);
            response.setRequest(request);

            // Set as notes
            req.setNote(ADAPTER_NOTES, request);
            res.setNote(ADAPTER_NOTES, response);

            // Set query string encoding
            req.getParameters().setQueryStringEncoding
                (connector.getURIEncoding());

        }

        if (connector.isXpoweredBy()) {
            response.addHeader("X-Powered-By", "Servlet/2.4");
        }

        try {

            // Parse and set Catalina and configuration specific 
            // request parameters
            if ( postParseRequest(req, request, res, response) ) {
                // Calling the container
                connector.getContainer().invoke(request, response);
            }

            response.finishResponse();
            req.action( ActionCode.ACTION_POST_REQUEST , null);

        } catch (IOException e) {
            ;
        } catch (Throwable t) {
            log.error(sm.getString("coyoteAdapter.service"), t);
        } finally {
            // Recycle the wrapper request and response
            request.recycle();
            response.recycle();
        }

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Parse additional request parameters.
     */
    protected boolean postParseRequest(Request req, CoyoteRequest request,
                                       Response res, CoyoteResponse response)
        throws Exception {
        // XXX the processor needs to set a correct scheme and port prior to this point, 
        // in ajp13 protocols dont make sense to get the port from the connector..
        // XXX the processor may have set a correct scheme and port prior to this point, 
        // in ajp13 protocols dont make sense to get the port from the connector...
        // otherwise, use connector configuration
        if (! req.scheme().isNull()) {
            // use processor specified scheme to determine secure state
            request.setSecure(req.scheme().equals("https"));
        } else {
            // use connector scheme and secure configuration, (defaults to
            // "http" and false respectively)
            req.scheme().setString(connector.getScheme());
            request.setSecure(connector.getSecure());
        }

        // Filter trace method
        if (!connector.getAllowTrace() 
            && req.method().equalsIgnoreCase("TRACE")) {
            res.setStatus(403);
            res.setMessage("TRACE method is not allowed");
            return false;
        }

        // FIXME: the code below doesnt belongs to here, 
        // this is only have sense 
        // in Http11, not in ajp13..
        // At this point the Host header has been processed.
        // Override if the proxyPort/proxyHost are set 
        String proxyName = connector.getProxyName();
        int proxyPort = connector.getProxyPort();
        if (proxyPort != 0) {
            req.setServerPort(proxyPort);
        }
        if (proxyName != null) {
            req.serverName().setString(proxyName);
        }

        // URI decoding
        MessageBytes decodedURI = req.decodedURI();
        decodedURI.duplicate(req.requestURI());
        try {
          req.getURLDecoder().convert(decodedURI, false);
        } catch (IOException ioe) {
          res.setStatus(400);
          res.setMessage("Invalid URI");
          throw ioe;
        }

        // Normalize decoded URI
        if (!normalize(req.decodedURI())) {
            res.setStatus(400);
            res.setMessage("Invalid URI");
            return false;
        }

        // Set the remote principal
        String principal = req.getRemoteUser().toString();
        if (principal != null) {
            request.setUserPrincipal(new CoyotePrincipal(principal));
        }

        // URI character decoding
        convertURI(decodedURI, request);

        // Parse session Id
        parseSessionId(req, request);

        // Remove any remaining parameters (other than session id, which has
        // already been removed in parseSessionId()) from the URI, so they
        // won't be considered by the mapping algorithm.
        CharChunk uriCC = decodedURI.getCharChunk();
        int semicolon = uriCC.indexOf(';');
        if (semicolon > 0) {
            decodedURI.setChars
                (uriCC.getBuffer(), uriCC.getStart(), semicolon);
        }

        // Request mapping.
        connector.getMapper().map(req.serverName(), decodedURI, 
                                  request.getMappingData());
        request.setContext((Context) request.getMappingData().context);
        request.setWrapper((Wrapper) request.getMappingData().wrapper);

        // Possible redirect
        MessageBytes redirectPathMB = request.getMappingData().redirectPath;
        if (!redirectPathMB.isNull()) {
            String redirectPath = redirectPathMB.toString();
            String query = request.getQueryString();
            if (query != null) {
                // This is not optimal, but as this is not very common, it
                // shouldn't matter
                redirectPath = redirectPath + "?" + query;
            }
            response.sendRedirect(redirectPath);
            return false;
        }

        // Parse cookies
        parseCookies(req, request);
        return true;
    }


    /**
     * Parse session id in URL.
     */
    protected void parseSessionId(Request req, CoyoteRequest request) {

        CharChunk uriCC = req.decodedURI().getCharChunk();
        int semicolon = uriCC.indexOf(match, 0, match.length(), 0);

        if (semicolon > 0) {

            // Parse session ID, and extract it from the decoded request URI
            int start = uriCC.getStart();
            int end = uriCC.getEnd();

            int sessionIdStart = start + semicolon + match.length();
            int semicolon2 = uriCC.indexOf(';', sessionIdStart);
            if (semicolon2 >= 0) {
                request.setRequestedSessionId
                    (new String(uriCC.getBuffer(), sessionIdStart, 
                                semicolon2 - semicolon - match.length()));
            } else {
                request.setRequestedSessionId
                    (new String(uriCC.getBuffer(), sessionIdStart, 
                                end - sessionIdStart));
            }
            request.setRequestedSessionURL(true);

            // Extract session ID from request URI
            ByteChunk uriBC = req.requestURI().getByteChunk();
            start = uriBC.getStart();
            end = uriBC.getEnd();
            semicolon = uriBC.indexOf(match, 0, match.length(), 0);

            if (semicolon > 0) {
                sessionIdStart = start + semicolon;
                semicolon2 = uriCC.indexOf
                    (';', start + semicolon + match.length());
                uriBC.setEnd(start + semicolon);
                byte[] buf = uriBC.getBuffer();
                if (semicolon2 >= 0) {
                    for (int i = 0; i < end - start - semicolon2; i++) {
                        buf[start + semicolon + i] 
                            = buf[start + i + semicolon2];
                    }
                    uriBC.setBytes(buf, start, semicolon 
                                   + (end - start - semicolon2));
                }
            }

        } else {
            request.setRequestedSessionId(null);
            request.setRequestedSessionURL(false);
        }

    }


    /**
     * Parse cookies.
     */
    protected void parseCookies(Request req, CoyoteRequest request) {

        Cookies serverCookies = req.getCookies();
        int count = serverCookies.getCookieCount();
        if (count <= 0)
            return;

        Cookie[] cookies = new Cookie[count];

        int idx=0;
        for (int i = 0; i < count; i++) {
            ServerCookie scookie = serverCookies.getCookie(i);
            if (scookie.getName().equals(Globals.SESSION_COOKIE_NAME)) {
                // Override anything requested in the URL
                if (!request.isRequestedSessionIdFromCookie()) {
                    // Accept only the first session id cookie
                    request.setRequestedSessionId
                        (scookie.getValue().toString());
                    request.setRequestedSessionCookie(true);
                    request.setRequestedSessionURL(false);
                    if (log.isDebugEnabled())
                        log.debug(" Requested cookie session id is " +
                            ((HttpServletRequest) request.getRequest())
                            .getRequestedSessionId());
                }
            }
            try {
                Cookie cookie = new Cookie(scookie.getName().toString(),
                                           scookie.getValue().toString());
                cookie.setPath(scookie.getPath().toString());
                cookie.setVersion(scookie.getVersion());
                String domain = scookie.getDomain().toString();
                if (domain != null) {
                    cookie.setDomain(scookie.getDomain().toString());
                }
                cookies[idx++] = cookie;
            } catch(Exception ex) {
                log.error("Bad Cookie Name: " + scookie.getName() + 
                    " /Value: " + scookie.getValue(),ex);
            }
        }
        if( idx < count ) {
            Cookie [] ncookies = new Cookie[idx];
            System.arraycopy(cookies, 0, ncookies, 0, idx);
            cookies = ncookies;
        }

        request.setCookies(cookies);

    }


    /**
     * Character conversion of the URI.
     */
    protected void convertURI(MessageBytes uri, CoyoteRequest request) 
        throws Exception {

        ByteChunk bc = uri.getByteChunk();
        CharChunk cc = uri.getCharChunk();
        cc.allocate(bc.getLength(), -1);

        String enc = connector.getURIEncoding();
        if (enc != null) {
            B2CConverter conv = request.getURIConverter();
            try {
                if (conv == null) {
                    conv = new B2CConverter(enc);
                    request.setURIConverter(conv);
                } else {
                    conv.recycle();
                }
            } catch (IOException e) {
                // Ignore
                log.error("Invalid URI encoding; using HTTP default");
                connector.setURIEncoding(null);
            }
            if (conv != null) {
                try {
                    conv.convert(bc, cc);
                    uri.setChars(cc.getBuffer(), cc.getStart(), 
                                 cc.getLength());
                    return;
                } catch (IOException e) {
                    log.error("Invalid URI character encoding; trying ascii");
                    cc.recycle();
                }
            }
        }

        // Default encoding: fast conversion
        byte[] bbuf = bc.getBuffer();
        char[] cbuf = cc.getBuffer();
        int start = bc.getStart();
        for (int i = 0; i < bc.getLength(); i++) {
            cbuf[i] = (char) (bbuf[i + start] & 0xff);
        }
        uri.setChars(cbuf, 0, bc.getLength());

    }


    /**
     * Normalize URI.
     * <p>
     * This method normalizes "\", "//", "/./" and "/../". This method will
     * return false when trying to go above the root, or if the URI contains
     * a null byte.
     * 
     * @param uriMB URI to be normalized
     */
    public static boolean normalize(MessageBytes uriMB) {

        ByteChunk uriBC = uriMB.getByteChunk();
        byte[] b = uriBC.getBytes();
        int start = uriBC.getStart();
        int end = uriBC.getEnd();

        // URL * is acceptable
        if ((end - start == 1) && b[start] == (byte) '*')
          return true;

        int pos = 0;
        int index = 0;

        // Replace '\' with '/'
        // Check for null byte
        for (pos = start; pos < end; pos++) {
            if (b[pos] == (byte) '\\')
                b[pos] = (byte) '/';
            if (b[pos] == (byte) 0)
                return false;
        }

        // The URL must start with '/'
        if (b[start] != (byte) '/') {
            return false;
        }

        // Replace "//" with "/"
        for (pos = start; pos < (end - 1); pos++) {
            if (b[pos] == (byte) '/') {
                while ((pos + 1 < end) && (b[pos + 1] == (byte) '/')) {
                    copyBytes(b, pos, pos + 1, end - pos - 1);
                    end--;
                }
            }
        }

        // If the URI ends with "/." or "/..", then we append an extra "/"
        // Note: It is possible to extend the URI by 1 without any side effect
        // as the next character is a non-significant WS.
        if (((end - start) > 2) && (b[end - 1] == (byte) '.')) {
            if ((b[end - 2] == (byte) '/') 
                || ((b[end - 2] == (byte) '.') 
                    && (b[end - 3] == (byte) '/'))) {
                b[end] = (byte) '/';
                end++;
            }
        }

        uriBC.setEnd(end);

        index = 0;

        // Resolve occurrences of "/./" in the normalized path
        while (true) {
            index = uriBC.indexOf("/./", 0, 3, index);
            if (index < 0)
                break;
            copyBytes(b, start + index, start + index + 2, 
                      end - start - index - 2);
            end = end - 2;
            uriBC.setEnd(end);
        }

        index = 0;

        // Resolve occurrences of "/../" in the normalized path
        while (true) {
            index = uriBC.indexOf("/../", 0, 4, index);
            if (index < 0)
                break;
            // Prevent from going outside our context
            if (index == 0)
                return false;
            int index2 = -1;
            for (pos = start + index - 1; (pos >= 0) && (index2 < 0); pos --) {
                if (b[pos] == (byte) '/') {
                    index2 = pos;
                }
            }
            copyBytes(b, start + index2, start + index + 3,
                      end - start - index - 3);
            end = end + index2 - index - 3;
            uriBC.setEnd(end);
            index = index2;
        }

        uriBC.setBytes(b, start, end);

        return true;

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Copy an array of bytes to a different position. Used during 
     * normalization.
     */
    protected static void copyBytes(byte[] b, int dest, int src, int len) {
        for (int pos = 0; pos < len; pos++) {
            b[pos + dest] = b[pos + src];
        }
    }


    /**
     * Log a message on the Logger associated with our Container (if any)
     *
     * @param message Message to be logged
     */
    protected void log(String message) {
        log.info( message );
    }


    /**
     * Log a message on the Logger associated with our Container (if any)
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    protected void log(String message, Throwable throwable) {
        log.error( message, throwable);
    }


}
