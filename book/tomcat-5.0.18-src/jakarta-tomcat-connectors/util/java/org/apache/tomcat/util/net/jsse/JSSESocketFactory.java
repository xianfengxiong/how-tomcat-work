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
package org.apache.tomcat.util.net.jsse;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyStore;
import java.util.Vector;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

/*
  1. Make the JSSE's jars available, either as an installed
     extension (copy them into jre/lib/ext) or by adding
     them to the Tomcat classpath.
  2. keytool -genkey -alias tomcat -keyalg RSA
     Use "changeit" as password ( this is the default we use )
 */

/**
 * SSL server socket factory. It _requires_ a valid RSA key and
 * JSSE. 
 *
 * @author Harish Prabandham
 * @author Costin Manolache
 * @author Stefan Freyr Stefansson
 * @author EKR -- renamed to JSSESocketFactory
 */
public abstract class JSSESocketFactory
    extends org.apache.tomcat.util.net.ServerSocketFactory
{
    // defaults
    static String defaultProtocol = "TLS";
    static String defaultAlgorithm = "SunX509";
    static boolean defaultClientAuth = false;
    static String defaultKeystoreType = "JKS";
    private static final String defaultKeystoreFile
        = System.getProperty("user.home") + "/.keystore";
    private static final String defaultKeyPass = "changeit";
    static org.apache.commons.logging.Log log =
        org.apache.commons.logging.LogFactory.getLog(JSSESocketFactory.class);

    protected boolean initialized;
    protected boolean clientAuth = false;
    protected SSLServerSocketFactory sslProxy = null;
    protected String[] enabledCiphers;
   

    public JSSESocketFactory () {
    }

    public ServerSocket createSocket (int port)
        throws IOException
    {
        if (!initialized) init();
        ServerSocket socket = sslProxy.createServerSocket(port);
        initServerSocket(socket);
        return socket;
    }
    
    public ServerSocket createSocket (int port, int backlog)
        throws IOException
    {
        if (!initialized) init();
        ServerSocket socket = sslProxy.createServerSocket(port, backlog);
        initServerSocket(socket);
        return socket;
    }
    
    public ServerSocket createSocket (int port, int backlog,
                                      InetAddress ifAddress)
        throws IOException
    {   
        if (!initialized) init();
        ServerSocket socket = sslProxy.createServerSocket(port, backlog,
                                                          ifAddress);
        initServerSocket(socket);
        return socket;
    }
    
    public Socket acceptSocket(ServerSocket socket)
        throws IOException
    {
        SSLSocket asock = null;
        try {
             asock = (SSLSocket)socket.accept();
             asock.setNeedClientAuth(clientAuth);
        } catch (SSLException e){
          throw new SocketException("SSL handshake error" + e.toString());
        }
        return asock;
    }

    public void handshake(Socket sock) throws IOException {
        ((SSLSocket)sock).startHandshake();
    }

    /*
     * Determines the SSL cipher suites to be enabled.
     *
     * @param requestedCiphers Comma-separated list of requested ciphers
     * @param supportedCiphers Array of supported ciphers
     *
     * @return Array of SSL cipher suites to be enabled, or null if none of the
     * requested ciphers are supported
     */
    protected String[] getEnabledCiphers(String requestedCiphers,
                                         String[] supportedCiphers) {

        String[] enabledCiphers = null;

        if (requestedCiphers != null) {
            Vector vec = null;
            String cipher = requestedCiphers;
            int index = requestedCiphers.indexOf(',');
            if (index != -1) {
                int fromIndex = 0;
                while (index != -1) {
                    cipher = requestedCiphers.substring(fromIndex, index).trim();
                    if (cipher.length() > 0) {
                        /*
                         * Check to see if the requested cipher is among the
                         * supported ciphers, i.e., may be enabled
                         */
                        for (int i=0; supportedCiphers != null
                                     && i<supportedCiphers.length; i++) {
                            if (supportedCiphers[i].equals(cipher)) {
                                if (vec == null) {
                                    vec = new Vector();
                                }
                                vec.addElement(cipher);
                                break;
                            }
                        }
                    }
                    fromIndex = index+1;
                    index = requestedCiphers.indexOf(',', fromIndex);
                } // while
                cipher = requestedCiphers.substring(fromIndex);
            }

            if (cipher != null) {
                cipher = cipher.trim();
                if (cipher.length() > 0) {
                    /*
                     * Check to see if the requested cipher is among the
                     * supported ciphers, i.e., may be enabled
                     */
                    for (int i=0; supportedCiphers != null
                                 && i<supportedCiphers.length; i++) {
                        if (supportedCiphers[i].equals(cipher)) {
                            if (vec == null) {
                                vec = new Vector();
                            }
                            vec.addElement(cipher);
                            break;
                        }
                    }
                }
            }           

            if (vec != null) {
                enabledCiphers = new String[vec.size()];
                vec.copyInto(enabledCiphers);
            }
        }

        return enabledCiphers;
    }
     
    /*
     * Gets the SSL server's keystore password.
     */
    protected String getKeystorePassword() {
        String keyPass = (String)attributes.get("keypass");
        if (keyPass == null) {
            keyPass = defaultKeyPass;
        }
        String keystorePass = (String)attributes.get("keystorePass");
        if (keystorePass == null) {
            keystorePass = keyPass;
        }
        return keystorePass;
    }

    /*
     * Gets the SSL server's keystore.
     */
    protected KeyStore getKeystore(String type, String pass)
            throws IOException {

        String keystoreFile = (String)attributes.get("keystore");
        if (keystoreFile == null)
            keystoreFile = defaultKeystoreFile;

        return getStore(type, keystoreFile, pass);
    }

    /*
     * Gets the SSL server's truststore.
     */
    protected KeyStore getTrustStore(String keystoreType) throws IOException {
        KeyStore trustStore = null;

        String trustStoreFile = (String)attributes.get("truststoreFile");
        if(trustStoreFile == null) {
            trustStoreFile = System.getProperty("javax.net.ssl.trustStore");
        }
        if(log.isDebugEnabled()) {
            log.debug("Truststore = " + trustStoreFile);
        }
        String trustStorePassword = (String)attributes.get("truststorePass");
        if( trustStorePassword == null) {
            trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
        }
        if( trustStorePassword == null ) {
            trustStorePassword = getKeystorePassword();
        }
        if(log.isDebugEnabled()) {
            log.debug("TrustPass = " + trustStorePassword);
        }
        String truststoreType = (String)attributes.get("truststoreType");
        if(truststoreType == null) {
            truststoreType = keystoreType;
        }
        if(log.isDebugEnabled()) {
            log.debug("trustType = " + truststoreType);
        }
        if (trustStoreFile != null && trustStorePassword != null){
            trustStore = getStore(truststoreType, trustStoreFile,
                                  trustStorePassword);
        }

        return trustStore;
    }

    /*
     * Gets the key- or truststore with the specified type, path, and password.
     */
    private KeyStore getStore(String type, String path, String pass)
            throws IOException {

        KeyStore ks = null;
        InputStream istream = null;
        try {
            ks = KeyStore.getInstance(type);
            istream = new FileInputStream(path);
            ks.load(istream, pass.toCharArray());
            istream.close();
            istream = null;
        } catch (FileNotFoundException fnfe) {
            throw fnfe;
        } catch (IOException ioe) {
            throw ioe;      
        } catch(Exception ex) {
            ex.printStackTrace();
            throw new IOException("Exception trying to load keystore " +
                                  path + ": " + ex.getMessage() );
        } finally {
            if (istream != null) {
                try {
                    istream.close();
                } catch (IOException ioe) {
                    // Do nothing
                }
            }
        }

        return ks;
    }

    /**
     * Reads the keystore and initializes the SSL socket factory.
     *
     * Place holder method to initialize the KeyStore, etc.
     */
    abstract void init() throws IOException ;

    /*
     * Determines the SSL protocol variants to be enabled.
     *
     * @param socket The socket to get supported list from.
     * @param requestedProtocols Comma-separated list of requested SSL
     * protocol variants
     *
     * @return Array of SSL protocol variants to be enabled, or null if none of
     * the requested protocol variants are supported
     */
    abstract protected String[] getEnabledProtocols(SSLServerSocket socket,
                                                    String requestedProtocols);

    /**
     * Set the SSL protocol variants to be enabled.
     * @param socket the SSLServerSocket.
     * @param protocols the protocols to use.
     */
    abstract protected void setEnabledProtocols(SSLServerSocket socket, 
                                            String [] protocols);

    /**
     * Configures the given SSL server socket with the requested cipher suites,
     * protocol versions, and need for client authentication
     */
    private void initServerSocket(ServerSocket ssocket) {

        SSLServerSocket socket = (SSLServerSocket) ssocket;

        if (attributes.get("ciphers") != null) {
            socket.setEnabledCipherSuites(enabledCiphers);
        }

        String requestedProtocols = (String) attributes.get("protocols");
        setEnabledProtocols(socket, getEnabledProtocols(socket, 
                                                         requestedProtocols));

        // we don't know if client auth is needed -
        // after parsing the request we may re-handshake
        socket.setNeedClientAuth(clientAuth);
    }

}
