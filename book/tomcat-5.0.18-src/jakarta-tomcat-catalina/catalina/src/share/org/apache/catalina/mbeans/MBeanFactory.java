/*
 * $Header: /home/cvs/jakarta-tomcat-catalina/catalina/src/share/org/apache/catalina/mbeans/MBeanFactory.java,v 1.21 2004/01/08 05:32:25 amyroh Exp $
 * $Revision: 1.21 $
 * $Date: 2004/01/08 05:32:25 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2002 The Apache Software Foundation.  All rights
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

package org.apache.catalina.mbeans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Vector;

import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.DefaultContext;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Server;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.Service;
import org.apache.catalina.Valve;
import org.apache.catalina.authenticator.SingleSignOn;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardDefaultContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.logger.FileLogger;
import org.apache.catalina.logger.SystemErrLogger;
import org.apache.catalina.logger.SystemOutLogger;
import org.apache.catalina.realm.JDBCRealm;
import org.apache.catalina.realm.JNDIRealm;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.realm.UserDatabaseRealm;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.HostConfig;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.RemoteAddrValve;
import org.apache.catalina.valves.RemoteHostValve;
import org.apache.catalina.valves.RequestDumperValve;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.modeler.BaseModelMBean;
import org.apache.commons.modeler.ManagedBean;
import org.apache.commons.modeler.Registry;
import org.apache.coyote.tomcat5.CoyoteConnector;


/**
 * <p>A <strong>ModelMBean</strong> implementation for the
 * <code>org.apache.catalina.core.StandardServer</code> component.</p>
 *
 * @author Amy Roh
 * @version $Revision: 1.21 $ $Date: 2004/01/08 05:32:25 $
 */

public class MBeanFactory extends BaseModelMBean {

    /**
     * The <code>MBeanServer</code> for this application.
     */
    private static MBeanServer mserver = MBeanUtils.createServer();

    /**
     * The configuration information registry for our managed beans.
     */
    private static Registry registry = MBeanUtils.createRegistry();


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a <code>ModelMBean</code> with default
     * <code>ModelMBeanInfo</code> information.
     *
     * @exception MBeanException if the initializer of an object
     *  throws an exception
     * @exception RuntimeOperationsException if an IllegalArgumentException
     *  occurs
     */
    public MBeanFactory()
        throws MBeanException, RuntimeOperationsException {

        super();

    }


    // ------------------------------------------------------------- Attributes




    // ------------------------------------------------------------- Operations


    /**
     * Return the managed bean definition for the specified bean type
     *
     * @param type MBean type
     */
    public String findObjectName(String type) {

        if (type.equals("org.apache.catalina.core.StandardContext")) {
            return "StandardContext";
        } else if (type.equals("org.apache.catalina.core.StandardDefaultContext")) {
            return "DefaultContext";
        } else if (type.equals("org.apache.catalina.core.StandardEngine")) {
            return "Engine";
        } else if (type.equals("org.apache.catalina.core.StandardHost")) {
            return "Host";
        } else {
            return null;
        }

    }


    /**
     * Little convenience method to remove redundant code
     * when retrieving the path string
     *
     * @param t path string
     * @return empty string if t==null || t.equals("/")
     */
    private final String getPathStr(String t) {
        if (t == null || t.equals("/")) {
            return "";
        }
        return t;
    }

    
   /**
     * Get Parent DefaultContext to add its child component 
     * from parent's ObjectName
     */
    private DefaultContext getDefaultContext(ObjectName pname) 
        throws Exception {
        
        String type = pname.getKeyProperty("type");
        if (type.equals("DefaultContext")) {
            Service service = getService(pname);
            StandardEngine engine = (StandardEngine) service.getContainer();
            String hostName = pname.getKeyProperty("host");
            if (hostName!=null) {
                StandardHost host = 
                    (StandardHost) engine.findChild(hostName);
                return host.getDefaultContext();
            } else {
                return engine.getDefaultContext();
            }
        }
        return null;
    }
    

    /**
     * Get Parent ContainerBase to add its child component 
     * from parent's ObjectName
     */
    private ContainerBase getParentContainerFromParent(ObjectName pname) 
        throws Exception {
        
        String type = pname.getKeyProperty("type");
        String j2eeType = pname.getKeyProperty("j2eeType");
        Service service = getService(pname);
        StandardEngine engine = (StandardEngine) service.getContainer();
        if ((j2eeType!=null) && (j2eeType.equals("WebModule"))) {
            String name = pname.getKeyProperty("name");
            name = name.substring(2);
            int i = name.indexOf("/");
            String hostName = name.substring(0,i);
            String path = name.substring(i);
            Host host = (Host) engine.findChild(hostName);
            String pathStr = getPathStr(path);
            StandardContext context = (StandardContext)host.findChild(pathStr);
            return context;
        } else if (type != null) {
            if (type.equals("Engine")) {
                return engine;
            } else if (type.equals("Host")) {
                String hostName = pname.getKeyProperty("host");
                StandardHost host = (StandardHost) engine.findChild(hostName);
                return host;
            }
        }
        return null;
        
    }


    /**
     * Get Parent ContainerBase to add its child component 
     * from child component's ObjectName  as a String
     */    
    private ContainerBase getParentContainerFromChild(ObjectName oname) 
        throws Exception {
        
        String hostName = oname.getKeyProperty("host");
        String path = oname.getKeyProperty("path");
        Service service = getService(oname);
        StandardEngine engine = (StandardEngine) service.getContainer();
        if (hostName == null) {             
            // child's container is Engine
            return engine;
        } else if (path == null) {      
            // child's container is Host
            StandardHost host = (StandardHost) engine.findChild(hostName);
            return host;
        } else {                
            // child's container is Context
            StandardHost host = (StandardHost) engine.findChild(hostName);
            path = getPathStr(path);
            StandardContext context = (StandardContext) host.findChild(path);
            return context;
        }
    }

    
    private Service getService(ObjectName oname) throws Exception {
    
        String domain = oname.getDomain();
        Server server = ServerFactory.getServer();
        Service[] services = server.findServices();
        StandardService service = null;
        for (int i = 0; i < services.length; i++) {
            service = (StandardService) services[i];
            if (domain.equals(service.getObjectName().getDomain())) {
                break;
            }
        }
        if (!service.getObjectName().getDomain().equals(domain)) {
            throw new Exception("Service with the domain is not found");
        }        
        return service;

    }
    
    
    /**
     * Create a new AccessLoggerValve.
     *
     * @param parent MBean Name of the associated parent component
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createAccessLoggerValve(String parent)
        throws Exception {

        ObjectName pname = new ObjectName(parent);
        // Create a new AccessLogValve instance
        AccessLogValve accessLogger = new AccessLogValve();
        ContainerBase containerBase = getParentContainerFromParent(pname);
        // Add the new instance to its parent component
        containerBase.addValve(accessLogger);
        ObjectName oname = accessLogger.getObjectName();
        return (oname.toString());

    }
        

    /**
     * Create a new AjpConnector
     *
     * @param parent MBean Name of the associated parent component
     * @param address The IP address on which to bind
     * @param port TCP port number to listen on
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createAjpConnector(String parent, String address, int port)
        throws Exception {

        Object retobj = null;
        try {
            // Create a new CoyoteConnector instance for AJP
            // use reflection to avoid j-t-c compile-time circular dependencies
            Class cls = Class.forName("org.apache.coyote.tomcat5.CoyoteConnector");
            Constructor ct = cls.getConstructor(null);
            retobj = ct.newInstance(null);
            Class partypes1 [] = new Class[1];
            // Set address
            String str = new String();
            if ((address!=null) && (address.length()>0)) {
                partypes1[0] = str.getClass();
                Method meth1 = cls.getMethod("setAddress", partypes1);
                Object arglist1[] = new Object[1];
                arglist1[0] = address;
                meth1.invoke(retobj, arglist1);
            }
            // Set port number
            Class partypes2 [] = new Class[1];
            partypes2[0] = Integer.TYPE;
            Method meth2 = cls.getMethod("setPort", partypes2);
            Object arglist2[] = new Object[1];
            arglist2[0] = new Integer(port);
            meth2.invoke(retobj, arglist2);
            // set protocolHandlerClassName for AJP
            Class partypes3 [] = new Class[1];
            partypes3[0] = str.getClass();
            Method meth3 = cls.getMethod("setProtocolHandlerClassName", partypes3);
            Object arglist3[] = new Object[1];
            arglist3[0] = new String("org.apache.jk.server.JkCoyoteHandler");
            meth3.invoke(retobj, arglist3);

            // Add the new instance to its parent component
            ObjectName pname = new ObjectName(parent);
            Service service = getService(pname);
            service.addConnector((Connector)retobj);
            Method getObjectName = cls.getMethod("getObjectName", null);
            
            // Return the corresponding MBean name
            //ObjectName coname = (ObjectName)getObjectName.invoke(retobj, null);
            ObjectName coname = 
                MBeanUtils.createObjectName(pname.getDomain(), (Connector)retobj);
            return (coname.toString());
        
        } catch (Exception e) {
            throw new MBeanException(e);
        }

    }
    

    /**
     * Create a new DefaultContext.
     *
     * @param parent MBean Name of the associated parent component
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createDefaultContext(String parent)
        throws Exception {
        // XXX FIXME
        // Create a new StandardDefaultContext instance
        StandardDefaultContext context = new StandardDefaultContext();

        // Add the new instance to its parent component
        ObjectName pname = new ObjectName(parent);
        Service service = getService(pname);       
        Engine engine = (Engine) service.getContainer();
        String hostName = pname.getKeyProperty("host");
        if (hostName == null) { //if DefaultContext is nested in Engine
            context.setParent(engine);
            engine.addDefaultContext(context);
        } else {                // if DefaultContext is nested in Host
            Host host = (Host) engine.findChild(hostName);
            context.setParent(host);
            host.addDefaultContext(context);
        }

        // Return the corresponding MBean name
        ManagedBean managed = registry.findManagedBean("DefaultContext");
        ObjectName oname =
            MBeanUtils.createObjectName(managed.getDomain(), context);
        return (oname.toString());

    }


    /**
     * Create a new FileLogger.
     *
     * @param parent MBean Name of the associated parent component
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createFileLogger(String parent)
        throws Exception {

        // Create a new FileLogger instance
        FileLogger fileLogger = new FileLogger();

        // Add the new instance to its parent component
        ObjectName pname = new ObjectName(parent);
        ContainerBase containerBase = getParentContainerFromParent(pname);
        // Add the new instance to its parent component
        containerBase.setLogger(fileLogger);
        // Return the corresponding MBean name
        ObjectName oname = fileLogger.getObjectName();
        return (oname.toString());

    }


    /**
     * Create a new HttpConnector
     *
     * @param parent MBean Name of the associated parent component
     * @param address The IP address on which to bind
     * @param port TCP port number to listen on
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createHttpConnector(String parent, String address, int port)
        throws Exception {

        Object retobj = null;
        try {
            // Create a new CoyoteConnector instance
            // use reflection to avoid j-t-c compile-time circular dependencies
            Class cls = Class.forName("org.apache.coyote.tomcat5.CoyoteConnector");
            Constructor ct = cls.getConstructor(null);
            retobj = ct.newInstance(null);
            Class partypes1 [] = new Class[1];
            // Set address
            String str = new String();
            if ((address!=null) && (address.length()>0)) {
                partypes1[0] = str.getClass();
                Method meth1 = cls.getMethod("setAddress", partypes1);
                Object arglist1[] = new Object[1];
                arglist1[0] = address;
                meth1.invoke(retobj, arglist1);
            }
            // Set port number
            Class partypes2 [] = new Class[1];
            partypes2[0] = Integer.TYPE;
            Method meth2 = cls.getMethod("setPort", partypes2);
            Object arglist2[] = new Object[1];
            arglist2[0] = new Integer(port);
            meth2.invoke(retobj, arglist2);
            // Add the new instance to its parent component
            ObjectName pname = new ObjectName(parent);
            Service service = getService(pname);
            service.addConnector((Connector)retobj);
            Method getObjectName = cls.getMethod("getObjectName", null);
            
            // Return the corresponding MBean name
            ObjectName coname = (ObjectName)getObjectName.invoke(retobj, null);
            //ObjectName coname = 
            //    MBeanUtils.createObjectName(pname.getDomain(), (Connector)retobj);
            return (coname.toString());
        } catch (Exception e) {
            throw new MBeanException(e);
        }

    }


    /**
     * Create a new HttpsConnector
     *
     * @param parent MBean Name of the associated parent component
     * @param address The IP address on which to bind
     * @param port TCP port number to listen on
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createHttpsConnector(String parent, String address, int port)
        throws Exception {

        Object retobj = null;
        // Create a new CoyoteConnector instance
        // use reflection to avoid j-t-c compile-time circular dependencies
        Class cls = Class.forName("org.apache.coyote.tomcat5.CoyoteConnector");
        try {
            Constructor ct = cls.getConstructor(null);
            retobj = ct.newInstance(null);
            Class partypes1 [] = new Class[1];
            // Set address
            String str = new String();
            if ((address!=null) && (address.length()>0)) {
                partypes1[0] = str.getClass();
                Method meth1 = cls.getMethod("setAddress", partypes1);
                Object arglist1[] = new Object[1];
                arglist1[0] = address;
                meth1.invoke(retobj, arglist1);
            }
            // Set port number
            Class partypes2 [] = new Class[1];
            partypes2[0] = Integer.TYPE;
            Method meth2 = cls.getMethod("setPort", partypes2);
            Object arglist2[] = new Object[1];
            arglist2[0] = new Integer(port);
            meth2.invoke(retobj, arglist2);
            // Set scheme
            Class partypes3 [] = new Class[1];
            partypes3[0] = str.getClass();
            Method meth3 = cls.getMethod("setScheme", partypes3);
            Object arglist3[] = new Object[1];
            arglist3[0] = new String("https");
            meth3.invoke(retobj, arglist3);
            // Set secure
            Class partypes4 [] = new Class[1];
            partypes4[0] = Boolean.TYPE;
            Method meth4 = cls.getMethod("setSecure", partypes4);
            Object arglist4[] = new Object[1];
            arglist4[0] = new Boolean(true);
            meth4.invoke(retobj, arglist4);
            // Set factory
            Class serverSocketFactoryCls =
                Class.forName("org.apache.catalina.net.ServerSocketFactory");
            Class coyoteServerSocketFactoryCls =
                Class.forName("org.apache.coyote.tomcat5.CoyoteServerSocketFactory");
            Constructor factoryConst =
                            coyoteServerSocketFactoryCls.getConstructor(null);
            Object factoryObj = factoryConst.newInstance(null);
            Class partypes5 [] = new Class[1];
            partypes5[0] = serverSocketFactoryCls;
            Method meth5 = cls.getMethod("setFactory", partypes5);
            Object arglist5[] = new Object[1];
            arglist5[0] = factoryObj;
            meth5.invoke(retobj, arglist5);
        } catch (Exception e) {
            throw new MBeanException(e);
        }

        try {
            // Add the new instance to its parent component
            ObjectName pname = new ObjectName(parent);
            Service service = getService(pname);
            service.addConnector((Connector)retobj);
            Method getObjectName = cls.getMethod("getObjectName", null);
            
            // Return the corresponding MBean name
            //ObjectName coname = (ObjectName)getObjectName.invoke(retobj, null);
            ObjectName coname = 
                MBeanUtils.createObjectName(pname.getDomain(), (Connector)retobj);
            return (coname.toString());
        } catch (Exception e) {
            // FIXME
            // disply error message
            // the user needs to use keytool to configure SSL first
            // addConnector will fail otherwise
            return null;
        }

    }


    /**
     * Create a new JDBC Realm.
     *
     * @param parent MBean Name of the associated parent component
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createJDBCRealm(String parent, String driverName, 
    	String connectionName, String connectionPassword, String connectionURL)
        throws Exception {

        // Create a new JDBCRealm instance
        JDBCRealm realm = new JDBCRealm();
	realm.setDriverName(driverName);
	realm.setConnectionName(connectionName);
	realm.setConnectionPassword(connectionPassword);
	realm.setConnectionURL(connectionURL);

        // Add the new instance to its parent component
        ObjectName pname = new ObjectName(parent);
        ContainerBase containerBase = getParentContainerFromParent(pname);
        // Add the new instance to its parent component
        containerBase.setRealm(realm);
        // Return the corresponding MBean name
        ObjectName oname = realm.getObjectName();
        // FIXME getObjectName() returns null
        //ObjectName oname = 
        //    MBeanUtils.createObjectName(pname.getDomain(), realm);
        if (oname != null) {
            return (oname.toString());
        } else {
            return null;
        }   

    }


    /**
     * Create a new JNDI Realm.
     *
     * @param parent MBean Name of the associated parent component
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createJNDIRealm(String parent)
        throws Exception {

         // Create a new JNDIRealm instance
        JNDIRealm realm = new JNDIRealm();

        // Add the new instance to its parent component
        ObjectName pname = new ObjectName(parent);
        ContainerBase containerBase = getParentContainerFromParent(pname);
        // Add the new instance to its parent component
        containerBase.setRealm(realm);
        // Return the corresponding MBean name
        ObjectName oname = realm.getObjectName();
        // FIXME getObjectName() returns null
        //ObjectName oname = 
        //    MBeanUtils.createObjectName(pname.getDomain(), realm);
        if (oname != null) {
            return (oname.toString());
        } else {
            return null;
        }   


    }


    /**
     * Create a new Memory Realm.
     *
     * @param parent MBean Name of the associated parent component
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createMemoryRealm(String parent)
        throws Exception {

         // Create a new MemoryRealm instance
        MemoryRealm realm = new MemoryRealm();

        // Add the new instance to its parent component
        ObjectName pname = new ObjectName(parent);
        ContainerBase containerBase = getParentContainerFromParent(pname);
        // Add the new instance to its parent component
        containerBase.setRealm(realm);
        // Return the corresponding MBean name
        //ObjectName oname = realm.getObjectName();
        // FIXME getObjectName() returns null
        ObjectName oname = 
            MBeanUtils.createObjectName(pname.getDomain(), realm);
        if (oname != null) {
            return (oname.toString());
        } else {
            return null;
        }   

    }


    /**
     * Create a new Remote Address Filter Valve.
     *
     * @param parent MBean Name of the associated parent component
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createRemoteAddrValve(String parent)
        throws Exception {

        // Create a new RemoteAddrValve instance
        RemoteAddrValve valve = new RemoteAddrValve();

        // Add the new instance to its parent component
        ObjectName pname = new ObjectName(parent);
        ContainerBase containerBase = getParentContainerFromParent(pname);
        containerBase.addValve(valve);
        ObjectName oname = valve.getObjectName();
        return (oname.toString());

    }


     /**
     * Create a new Remote Host Filter Valve.
     *
     * @param parent MBean Name of the associated parent component
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createRemoteHostValve(String parent)
        throws Exception {

        // Create a new RemoteHostValve instance
        RemoteHostValve valve = new RemoteHostValve();

        // Add the new instance to its parent component
        ObjectName pname = new ObjectName(parent);
        ContainerBase containerBase = getParentContainerFromParent(pname);
        containerBase.addValve(valve);
        ObjectName oname = valve.getObjectName();
        return (oname.toString());
        
    }


    /**
     * Create a new Request Dumper Valve.
     *
     * @param parent MBean Name of the associated parent component
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createRequestDumperValve(String parent)
        throws Exception {

        // Create a new RequestDumperValve instance
        RequestDumperValve valve = new RequestDumperValve();

        // Add the new instance to its parent component
        ObjectName pname = new ObjectName(parent);
        ContainerBase containerBase = getParentContainerFromParent(pname);
        containerBase.addValve(valve);
        ObjectName oname = valve.getObjectName();
        return (oname.toString());

    }


    /**
     * Create a new Single Sign On Valve.
     *
     * @param parent MBean Name of the associated parent component
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createSingleSignOn(String parent)
        throws Exception {

        // Create a new SingleSignOn instance
        SingleSignOn valve = new SingleSignOn();

        // Add the new instance to its parent component
        ObjectName pname = new ObjectName(parent);
        ContainerBase containerBase = getParentContainerFromParent(pname);
        containerBase.addValve(valve);
        ObjectName oname = valve.getObjectName();
        return (oname.toString());

    }


   /**
     * Create a new StandardContext.
     *
     * @param parent MBean Name of the associated parent component
     * @param path The context path for this Context
     * @param docBase Document base directory (or WAR) for this Context
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createStandardContext(String parent, String path,
                                        String docBase)
        throws Exception {

        // Create a new StandardContext instance
        StandardContext context = new StandardContext();
        path = getPathStr(path);
        context.setPath(path);
        context.setDocBase(docBase);
        ContextConfig contextConfig = new ContextConfig();
        context.addLifecycleListener(contextConfig);

        // Add the new instance to its parent component
        ObjectName pname = new ObjectName(parent);
        Service service = getService(pname);
        Engine engine = (Engine) service.getContainer();
        Host host = (Host) engine.findChild(pname.getKeyProperty("host"));
        host.addChild(context);

        // Return the corresponding MBean name
        ObjectName oname = 
            MBeanUtils.createObjectName(pname.getDomain(), context);
        return (oname.toString());

    }


   /**
     * Create a new StandardEngine.
     *
     * @param parent MBean Name of the associated parent component
     * @param name Unique name of this Engine
     * @param defaultHost Default hostname of this Engine
     *
     * @exception Exception if an MBean cannot be created or registered
     */
// replaced by createStandardEngineService to create with service
//     public String createStandardEngine(String parent, String name,
//                                       String defaultHost)
//        throws Exception {

        // Create a new StandardEngine instance
//       StandardEngine engine = new StandardEngine();
//        engine.setName(name);
//        engine.setDefaultHost(defaultHost);

        // Add the new instance to its parent component
//        ObjectName pname = new ObjectName(parent);
//        Server server = ServerFactory.getServer();
//        Service service = server.findService(name);
//        service.setContainer(engine);

        // Return the corresponding MBean name
        //ManagedBean managed = registry.findManagedBean("StandardEngine");
//        ObjectName oname =
//            MBeanUtils.createObjectName(name, engine);
//        return (oname.toString());

//    }


    public Vector createStandardEngineService(String parent, 
            String engineName, String defaultHost, String serviceName)
        throws Exception {

        // Create a new StandardService instance
        StandardService service = new StandardService();
        service.setName(serviceName);
        // Create a new StandardEngine instance
        StandardEngine engine = new StandardEngine();
        engine.setName(engineName);
        engine.setDefaultHost(defaultHost);
        // Need to set engine before adding it to server in order to set domain
        service.setContainer(engine);
        // Add the new instance to its parent component
        Server server = ServerFactory.getServer();
        server.addService(service);
        Vector onames = new Vector();
        // FIXME service & engine.getObjectName
        //ObjectName oname = engine.getObjectName();
        ObjectName oname = 
            MBeanUtils.createObjectName(engineName, engine);
        onames.add(0, oname);
        //oname = service.getObjectName();
        oname = 
            MBeanUtils.createObjectName(engineName, service);
        onames.add(1, oname);
        return (onames);

    }


    /**
     * Create a new StandardHost.
     *
     * @param parent MBean Name of the associated parent component
     * @param name Unique name of this Host
     * @param appBase Application base directory name
     * @param autoDeploy Should we auto deploy?
     * @param deployXML Should we deploy Context XML config files property?
     * @param liveDeploy Should we live deploy?     
     * @param unpackWARs Should we unpack WARs when auto deploying?
     * @param xmlNamespaceAware Should we turn on/off XML namespace awareness?
     * @param xmlValidation Should we turn on/off XML validation?        
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createStandardHost(String parent, String name,
                                     String appBase, boolean autoDeploy,
                                     boolean deployXML, boolean liveDeploy,                                      
                                     boolean unpackWARs,
                                     boolean xmlNamespaceAware,
                                     boolean xmlValidation)
        throws Exception {

        // Create a new StandardHost instance
        StandardHost host = new StandardHost();
        host.setName(name);
        host.setAppBase(appBase);
        host.setAutoDeploy(autoDeploy);
        host.setDeployXML(deployXML);
        host.setLiveDeploy(liveDeploy);
        host.setUnpackWARs(unpackWARs);
        host.setXmlNamespaceAware(xmlNamespaceAware);
        host.setXmlValidation(xmlValidation);
	
        // add HostConfig for active reloading
        HostConfig hostConfig = new HostConfig();
        host.addLifecycleListener(hostConfig);

        // Add the new instance to its parent component
        ObjectName pname = new ObjectName(parent);
        Service service = getService(pname);
        Engine engine = (Engine) service.getContainer();
        engine.addChild(host);

        // Return the corresponding MBean name
        return (host.getObjectName().toString());

    }


    /**
     * Create a new StandardManager.
     *
     * @param parent MBean Name of the associated parent component
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createStandardManager(String parent)
        throws Exception {

        // Create a new StandardManager instance
        StandardManager manager = new StandardManager();

        // Add the new instance to its parent component
        ObjectName pname = new ObjectName(parent);
        ContainerBase containerBase = getParentContainerFromParent(pname);
        if (containerBase != null) {
            containerBase.setManager(manager);
        } 
        ObjectName oname = manager.getObjectName();
        if (oname != null) {
            return (oname.toString());
        } else {
            return null;
        }
        
    }


    /**
     * Create a new StandardService.
     *
     * @param parent MBean Name of the associated parent component
     * @param name Unique name of this StandardService
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createStandardService(String parent, String name, String domain)
        throws Exception {

        // Create a new StandardService instance
        StandardService service = new StandardService();
        service.setName(name);

        // Add the new instance to its parent component
        Server server = ServerFactory.getServer();
        server.addService(service);

        // Return the corresponding MBean name
        return (service.getObjectName().toString());

    }



    /**
     * Create a new System Error Logger.
     *
     * @param parent MBean Name of the associated parent component
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createSystemErrLogger(String parent)
        throws Exception {

        // Create a new SystemErrLogger instance
        SystemErrLogger logger = new SystemErrLogger();

        // Add the new instance to its parent component
        ObjectName pname = new ObjectName(parent);
        ContainerBase containerBase = getParentContainerFromParent(pname);
        containerBase.setLogger(logger);
        ObjectName oname = logger.getObjectName();
        return (oname.toString());

    }


    /**
     * Create a new System Output Logger.
     *
     * @param parent MBean Name of the associated parent component
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createSystemOutLogger(String parent)
        throws Exception {

        // Create a new SystemOutLogger instance
        SystemOutLogger logger = new SystemOutLogger();

        // Add the new instance to its parent component
        ObjectName pname = new ObjectName(parent);
        ContainerBase containerBase = getParentContainerFromParent(pname);
        containerBase.setLogger(logger);
        ObjectName oname = logger.getObjectName();
        return (oname.toString());
        
    }


    /**
     * Create a new  UserDatabaseRealm.
     *
     * @param parent MBean Name of the associated parent component
     * @param resourceName Global JNDI resource name of the associated
     *  UserDatabase
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createUserDatabaseRealm(String parent, String resourceName)
        throws Exception {

         // Create a new UserDatabaseRealm instance
        UserDatabaseRealm realm = new UserDatabaseRealm();
        realm.setResourceName(resourceName);
        
        // Add the new instance to its parent component
        ObjectName pname = new ObjectName(parent);
        ContainerBase containerBase = getParentContainerFromParent(pname);
        // Add the new instance to its parent component
        containerBase.setRealm(realm);
        // Return the corresponding MBean name
        ObjectName oname = realm.getObjectName();
        // FIXME getObjectName() returns null
        //ObjectName oname = 
        //    MBeanUtils.createObjectName(pname.getDomain(), realm);
        if (oname != null) {
            return (oname.toString());
        } else {
            return null;
        }   

    }


    /**
     * Create a new Web Application Loader.
     *
     * @param parent MBean Name of the associated parent component
     *
     * @exception Exception if an MBean cannot be created or registered
     */
    public String createWebappLoader(String parent)
        throws Exception {

        // Create a new WebappLoader instance
        WebappLoader loader = new WebappLoader();

        // Add the new instance to its parent component
        ObjectName pname = new ObjectName(parent);
        ContainerBase containerBase = getParentContainerFromParent(pname);
        if (containerBase != null) {
            containerBase.setLoader(loader);
        } 
        // FIXME add Loader.getObjectName
        //ObjectName oname = loader.getObjectName();
        ObjectName oname = 
            MBeanUtils.createObjectName(pname.getDomain(), loader);
        return (oname.toString());
        
    }


    /**
     * Remove an existing Connector.
     *
     * @param name MBean Name of the comonent to remove
     *
     * @param serviceName Service name of the connector to remove
     *
     * @exception Exception if a component cannot be removed
     */
    public void removeConnector(String name) throws Exception {

        // Acquire a reference to the component to be removed
        ObjectName oname = new ObjectName(name);
        Server server = ServerFactory.getServer();
        Service service = getService(oname);
        String port = oname.getKeyProperty("port");
        //String address = oname.getKeyProperty("address");

        Connector conns[] = (Connector[]) service.findConnectors();

        for (int i = 0; i < conns.length; i++) {
            Class cls = conns[i].getClass();
            Method getAddrMeth = cls.getMethod("getAddress", null);
            Object addrObj = getAddrMeth.invoke(conns[i], null);
            String connAddress = null;
            if (addrObj != null) {
                connAddress = addrObj.toString();
            }
            Method getPortMeth = cls.getMethod("getPort", null);
            Object portObj = getPortMeth.invoke(conns[i], null);
            String connPort = new String();
            if (portObj != null) {
                connPort = portObj.toString();
            }
            // if (((address.equals("null")) &&
            if ((connAddress==null) && port.equals(connPort)) {
                service.removeConnector(conns[i]);
                ((CoyoteConnector)conns[i]).destroy();
                break;
            }
            // } else if (address.equals(connAddress))
            if (port.equals(connPort)) {
                // Remove this component from its parent component
                service.removeConnector(conns[i]);
                ((CoyoteConnector)conns[i]).destroy();
                break;
            }
        }

    }


    /**
     * Remove an existing Context.
     *
     * @param name MBean Name of the comonent to remove
     *
     * @exception Exception if a component cannot be removed
     */
    public void removeContext(String contextName) throws Exception {

        // Acquire a reference to the component to be removed
        ObjectName oname = new ObjectName(contextName);
        String domain = oname.getDomain();
        StandardService service = (StandardService) getService(oname);
        if (!service.getObjectName().getDomain().equals(domain)) {
            throw new Exception("Service with the domain is not found");
        }        
        Engine engine = (Engine) service.getContainer();
        String name = oname.getKeyProperty("name");
        name = name.substring(2);
        int i = name.indexOf("/");
        String hostName = name.substring(0,i);
        String path = name.substring(i);
        Host host = (Host) engine.findChild(hostName);
        String pathStr = getPathStr(path);
        Context context = (Context) host.findChild(pathStr);
        // Remove this component from its parent component
        host.removeChild(context);

    }


    /**
     * Remove an existing Host.
     *
     * @param name MBean Name of the comonent to remove
     *
     * @exception Exception if a component cannot be removed
     */
    public void removeHost(String name) throws Exception {

        // Acquire a reference to the component to be removed
        ObjectName oname = new ObjectName(name);
        String hostName = oname.getKeyProperty("host");
        Service service = getService(oname);
        Engine engine = (Engine) service.getContainer();
        Host host = (Host) engine.findChild(hostName);

        // Remove this component from its parent component
        engine.removeChild(host);

    }


    /**
     * Remove an existing Logger.
     *
     * @param name MBean Name of the comonent to remove
     *
     * @exception Exception if a component cannot be removed
     */
    public void removeLogger(String name) throws Exception {

        ObjectName oname = new ObjectName(name);
        // Acquire a reference to the component to be removed
        ContainerBase container = getParentContainerFromChild(oname);      
        container.setLogger(null);
    
    }

    
    /**
     * Remove an existing Loader.
     *
     * @param name MBean Name of the comonent to remove
     *
     * @exception Exception if a component cannot be removed
     */
    public void removeLoader(String name) throws Exception {

        ObjectName oname = new ObjectName(name);
        // Acquire a reference to the component to be removed
        ContainerBase container = getParentContainerFromChild(oname);    
        container.setLoader(null);
        
    }


    /**
     * Remove an existing Manager.
     *
     * @param name MBean Name of the comonent to remove
     *
     * @exception Exception if a component cannot be removed
     */
    public void removeManager(String name) throws Exception {

        ObjectName oname = new ObjectName(name);
        // Acquire a reference to the component to be removed
        ContainerBase container = getParentContainerFromChild(oname);    
        container.setManager(null);

    }


    /**
     * Remove an existing Realm.
     *
     * @param name MBean Name of the comonent to remove
     *
     * @exception Exception if a component cannot be removed
     */
    public void removeRealm(String name) throws Exception {

        ObjectName oname = new ObjectName(name);
        // Acquire a reference to the component to be removed
        ContainerBase container = getParentContainerFromChild(oname); 
        container.setRealm(null);
    }


    /**
     * Remove an existing Service.
     *
     * @param name MBean Name of the component to remove
     *
     * @exception Exception if a component cannot be removed
     */
    public void removeService(String name) throws Exception {

        // Acquire a reference to the component to be removed
        ObjectName oname = new ObjectName(name);
        String serviceName = oname.getKeyProperty("serviceName");
        Server server = ServerFactory.getServer();
        Service service = server.findService(serviceName);

        // Remove this component from its parent component
        server.removeService(service);

    }


    /**
     * Remove an existing Valve.
     *
     * @param name MBean Name of the comonent to remove
     *
     * @exception Exception if a component cannot be removed
     */
    public void removeValve(String name) throws Exception {

        // Acquire a reference to the component to be removed
        ObjectName oname = new ObjectName(name);
        ContainerBase container = getParentContainerFromChild(oname);
        String sequence = oname.getKeyProperty("seq");
        Valve[] valves = (Valve[])container.getValves();
        for (int i = 0; i < valves.length; i++) {
            ObjectName voname = ((ValveBase) valves[i]).getObjectName();
            if (voname.equals(oname)) {
                container.removeValve(valves[i]);
            }
        }
    }

}
