/*
 * $Header: /home/cvs/jakarta-tomcat-catalina/webapps/admin/WEB-INF/classes/org/apache/webapp/admin/resources/ResourceUtils.java,v 1.7 2003/03/26 08:05:19 amyroh Exp $
 * $Revision: 1.7 $
 * $Date: 2003/03/26 08:05:19 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 3. The end-resource documentation included with the redistribution, if
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
 *    permission of the Apache Datasource.
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


package org.apache.webapp.admin.resources;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ObjectInstance;

/**
 * <p>Shared utility methods for the resource administration module.</p>
 *
 * @author Manveen Kaur
 * @author Amy Roh
 * @version $Revision: 1.7 $ $Date: 2003/03/26 08:05:19 $
 * @since 4.1
 */

public class ResourceUtils {

    public final static String ENVIRONMENT_TYPE = ":type=Environment";
    public final static String RESOURCE_TYPE = ":type=Resource";
    public final static String RESOURCELINK_TYPE = ":type=ResourceLink";
    public final static String NAMINGRESOURCES_TYPE = ":type=NamingResources";
    public final static String GLOBAL_TYPE = ",resourcetype=Global";
    public final static String CONTEXT_TYPE = ",resourcetype=Context";
    public final static String SERVICE_DEFAULTCONTEXT_TYPE = 
                                                ",resourcetype=ServiceDefaultContext";
    public final static String HOST_DEFAULTCONTEXT_TYPE = 
                                                ",resourcetype=HostDefaultContext";                                                
    
    // resource class names
    public final static String USERDB_CLASS = "org.apache.catalina.UserDatabase";
    public final static String DATASOURCE_CLASS = "javax.sql.DataSource";
    public final static String MAILSESSION_CLASS = "javax.mail.Session";

    // --------------------------------------------------------- Public Methods


    public static ObjectName getNamingResourceObjectName(String domain,
            String resourcetype, String path, String host) throws Exception {

        ObjectName oname = null;
        if ((resourcetype==null) || (domain==null)) {
            return null;
        }
        // Construct the MBean Name for the naming source
        if (resourcetype.equals("Global")) {
            oname = new ObjectName(domain + NAMINGRESOURCES_TYPE +
                                GLOBAL_TYPE);
        } else if (resourcetype.equals("Context")) {
            oname = new ObjectName(domain + NAMINGRESOURCES_TYPE +
                                CONTEXT_TYPE + 
                                ",path=" + path + ",host=" + host);
        } else if (resourcetype.equals("DefaultContext")) {
            if (host.length() > 0) {
                oname = new ObjectName(domain + NAMINGRESOURCES_TYPE +
                                        HOST_DEFAULTCONTEXT_TYPE + 
                                        ",host=" + host);
            } else {
                oname = new ObjectName(domain + NAMINGRESOURCES_TYPE +
                                        SERVICE_DEFAULTCONTEXT_TYPE);            
            }
        }
        
        return oname;
        
    }
    
    /**
     * Construct and return a ResourcesForm identifying all currently defined
     * resources in the specified resource database.
     *
     * @param mserver MBeanServer to be consulted
     *
     * @exception Exception if an error occurs
     */
    public static EnvEntriesForm getEnvEntriesForm(MBeanServer mserver, 
        String resourcetype, String path, String host, String domain) 
        throws Exception {
                           
        ObjectName ename = null;
        StringBuffer sb = null;        
        if (resourcetype!=null) {
            if (resourcetype.equals("Global")) {
                ename = new ObjectName( domain + ENVIRONMENT_TYPE + 
                                        GLOBAL_TYPE + ",*");
            } else if (resourcetype.equals("Context")) {
                ename = new ObjectName ( domain + ENVIRONMENT_TYPE + 
                                        CONTEXT_TYPE + ",path=" + path + 
                                        ",host=" + host + ",*");
            } else if (resourcetype.equals("DefaultContext")) {
                if (host.length() > 0) {
                    ename = new ObjectName( domain + ENVIRONMENT_TYPE + 
                        HOST_DEFAULTCONTEXT_TYPE + ",host=" + host + ",*");
                } else {
                    ename = new ObjectName( domain + ENVIRONMENT_TYPE + 
                        SERVICE_DEFAULTCONTEXT_TYPE + ",*");
                }
            }
        }
        
        Iterator iterator = (mserver.queryMBeans(ename, null).iterator());
        
        ArrayList results = new ArrayList();        
        while (iterator.hasNext()) {
            ObjectInstance instance = (ObjectInstance) iterator.next(); 
            results.add(instance.getObjectName().toString());
        }

        Collections.sort(results);        
        
        EnvEntriesForm envEntriesForm = new EnvEntriesForm();
        envEntriesForm.setEnvEntries((String[]) 
                        results.toArray(new String[results.size()]));
        
        if (resourcetype != null) {
            envEntriesForm.setResourcetype(resourcetype);
        } else {
            envEntriesForm.setResourcetype("");
        }
         if (path != null) {
            envEntriesForm.setPath(path);
        } else {
            envEntriesForm.setPath("");
        }        
        if (host != null) {
            envEntriesForm.setHost(host);
        } else {
            envEntriesForm.setHost("");
        }          
        if (domain != null) {
            envEntriesForm.setDomain(domain);
        } else {
            envEntriesForm.setDomain("");
        }          
        
        return (envEntriesForm);

    }

    /**
     * Construct and return a DataSourcesForm identifying all currently defined
     * datasources in the specified resource database.
     *
     * @param mserver MBeanServer to be consulted
     *
     * @exception Exception if an error occurs
     */
    public static DataSourcesForm getDataSourcesForm(MBeanServer mserver, 
        String resourcetype, String path, String host, String domain) 
        throws Exception {
                            
        ObjectName rname = null;
        if (resourcetype!=null) {
            if (resourcetype.equals("Global")) {
                rname = new ObjectName( domain + RESOURCE_TYPE + GLOBAL_TYPE + 
                                        ",class=" + DATASOURCE_CLASS + ",*");
            } else if (resourcetype.equals("Context")) {
                rname = new ObjectName ( domain + RESOURCE_TYPE + CONTEXT_TYPE + 
                    ",path=" + path + ",host=" + host + ",class=" + 
                    DATASOURCE_CLASS + ",*");
            } else if (resourcetype.equals("DefaultContext")) {
                if (host.length() > 0) {
                    rname = new ObjectName( domain + RESOURCE_TYPE + 
                        HOST_DEFAULTCONTEXT_TYPE + ",host=" + host + 
                        ",class=" + DATASOURCE_CLASS + ",*");
                } else {
                    rname = new ObjectName( domain + RESOURCE_TYPE + 
                        SERVICE_DEFAULTCONTEXT_TYPE +  
                        ",class=" + DATASOURCE_CLASS + ",*");
                }
            }
        }
        Iterator iterator = (mserver.queryMBeans(rname, null).iterator());
        
        ArrayList results = new ArrayList();        
        while (iterator.hasNext()) {
            
            ObjectInstance instance = (ObjectInstance) iterator.next(); 
            results.add(instance.getObjectName().toString());
        }

        Collections.sort(results);        
        DataSourcesForm dataSourcesForm = new DataSourcesForm();
        dataSourcesForm.setDataSources((String[]) 
                        results.toArray(new String[results.size()]));        
        
        if (resourcetype != null) {
            dataSourcesForm.setResourcetype(resourcetype);
        } else {
            dataSourcesForm.setResourcetype("");
        }
         if (path != null) {
            dataSourcesForm.setPath(path);
        } else {
            dataSourcesForm.setPath("");
        }        
        if (host != null) {
            dataSourcesForm.setHost(host);
        } else {
            dataSourcesForm.setHost("");
        }        
        if (domain != null) {
            dataSourcesForm.setDomain(domain);
        } else {
            dataSourcesForm.setDomain("");
        }          
        return (dataSourcesForm);

    }
    
    /**
     * Construct and return a MailSessionsForm identifying all currently defined
     * mailsessions in the specified resource database.
     *
     * @param mserver MBeanServer to be consulted
     *
     * @exception Exception if an error occurs
     */
    public static MailSessionsForm getMailSessionsForm(MBeanServer mserver, 
        String resourcetype, String path, String host, String domain) 
        throws Exception {
                            
        ObjectName rname = null;
        if (resourcetype!=null) {
            if (resourcetype.equals("Global")) {
                rname = new ObjectName( domain + RESOURCE_TYPE + GLOBAL_TYPE + 
                                        ",class=" + MAILSESSION_CLASS + ",*");
            } else if (resourcetype.equals("Context")) {
                rname = new ObjectName (domain + RESOURCE_TYPE + CONTEXT_TYPE + 
                    ",path=" + path + ",host=" + host + ",class=" + 
                    MAILSESSION_CLASS + ",*");
            } else if (resourcetype.equals("DefaultContext")) {
                if (host.length() > 0) {
                    rname = new ObjectName(domain + RESOURCE_TYPE + 
                        HOST_DEFAULTCONTEXT_TYPE + ",host=" + host + 
                        ",class=" + MAILSESSION_CLASS + ",*");
                } else {
                    rname = new ObjectName(domain + RESOURCE_TYPE + 
                        SERVICE_DEFAULTCONTEXT_TYPE +
                        ",class=" + MAILSESSION_CLASS + ",*");
                }
            }
        }
       
        Iterator iterator = (mserver.queryMBeans(rname, null).iterator());
        
        ArrayList results = new ArrayList();        
        while (iterator.hasNext()) {
            
            ObjectInstance instance = (ObjectInstance) iterator.next(); 
            results.add(instance.getObjectName().toString());
        }

        Collections.sort(results);        
        MailSessionsForm mailSessionsForm = new MailSessionsForm();
        mailSessionsForm.setMailSessions((String[]) 
                        results.toArray(new String[results.size()]));        
        
        if (resourcetype != null) {
            mailSessionsForm.setResourcetype(resourcetype);
        } else {
            mailSessionsForm.setResourcetype("");
        }
         if (path != null) {
            mailSessionsForm.setPath(path);
        } else {
            mailSessionsForm.setPath("");
        }        
        if (host != null) {
            mailSessionsForm.setHost(host);
        } else {
            mailSessionsForm.setHost("");
        }        
        if (domain != null) {
            mailSessionsForm.setDomain(domain);
        } else {
            mailSessionsForm.setDomain("");
        }          
        
        return (mailSessionsForm);

    }
    
    /**
     * Construct and return a ResourceLinksForm identifying all currently defined
     * resourcelinks in the specified resource database.
     *
     * @param mserver MBeanServer to be consulted
     *
     * @exception Exception if an error occurs
     */
    public static ResourceLinksForm getResourceLinksForm(MBeanServer mserver, 
        String resourcetype, String path, String host, String domain) 
        throws Exception {

        ObjectName rname = null;
        if (resourcetype!=null) {
            if (resourcetype.equals("Global")) {
                rname = new ObjectName( domain + RESOURCELINK_TYPE + 
                                        GLOBAL_TYPE + ",*");
            } else if (resourcetype.equals("Context")) {
                rname = new ObjectName ( domain + RESOURCELINK_TYPE + 
                            CONTEXT_TYPE + ",path=" + path + 
                            ",host=" + host + ",*");
            } else if (resourcetype.equals("DefaultContext")) {
                if (host.length() > 0) {
                    rname = new ObjectName( domain + RESOURCELINK_TYPE + 
                                HOST_DEFAULTCONTEXT_TYPE + 
                                ",host=" + host + ",*");
                } else {
                    rname = new ObjectName( domain + RESOURCELINK_TYPE + 
                                SERVICE_DEFAULTCONTEXT_TYPE + ",*");
                }
            }
        }
       
        Iterator iterator = (mserver.queryMBeans(rname, null).iterator());
        
        ArrayList results = new ArrayList();        
        while (iterator.hasNext()) {
            ObjectInstance instance = (ObjectInstance) iterator.next(); 
            results.add(instance.getObjectName().toString());
        }

        Collections.sort(results);        
        ResourceLinksForm resourceLinksForm = new ResourceLinksForm();
        resourceLinksForm.setResourceLinks((String[]) 
                        results.toArray(new String[results.size()]));        
        
        if (resourcetype != null) {
            resourceLinksForm.setResourcetype(resourcetype);
        } else {
            resourceLinksForm.setResourcetype("");
        }
         if (path != null) {
            resourceLinksForm.setPath(path);
        } else {
            resourceLinksForm.setPath("");
        }        
        if (host != null) {
            resourceLinksForm.setHost(host);
        } else {
            resourceLinksForm.setHost("");
        }        
        if (domain != null) {
            resourceLinksForm.setDomain(domain);
        } else {
            resourceLinksForm.setDomain("");
        }          
        
        return (resourceLinksForm);
        
    }
    
    /**
     * Construct and return a UserDatabaseForm identifying all currently defined
     * user databases in the specified resource database.
     *
     * @param mserver MBeanServer to be consulted
     *
     * @exception Exception if an error occurs
     */
    public static UserDatabasesForm getUserDatabasesForm(MBeanServer mserver,String domain)
        throws Exception {
            
        ObjectName rname = new ObjectName( domain + RESOURCE_TYPE + GLOBAL_TYPE +
                            ",class=" + USERDB_CLASS + ",*");
        
        Iterator iterator = (mserver.queryMBeans(rname, null).iterator());
        
        ArrayList results = new ArrayList();        
        while (iterator.hasNext()) {
            ObjectInstance instance = (ObjectInstance) iterator.next(); 
            results.add(instance.getObjectName().toString());
        }

        Collections.sort(results);

        UserDatabasesForm userDatabasesForm = new UserDatabasesForm();
        userDatabasesForm.setUserDatabases((String[]) 
                        results.toArray(new String[results.size()]));  
        return (userDatabasesForm);

    }
    
}
