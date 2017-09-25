/*
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2001 The Apache Software Foundation.  All rights
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


package org.apache.catalina.core;


import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.directory.DirContext;
import javax.servlet.ServletException;

import org.apache.catalina.Cluster;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerEvent;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Logger;
import org.apache.catalina.Manager;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.logger.LoggerBase;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.apache.commons.modeler.Registry;
import org.apache.naming.resources.ProxyDirContext;


/**
 * Abstract implementation of the <b>Container</b> interface, providing common
 * functionality required by nearly every implementation.  Classes extending
 * this base class must implement <code>getInfo()</code>, and may implement
 * a replacement for <code>invoke()</code>.
 * <p>
 * All subclasses of this abstract base class will include support for a
 * Pipeline object that defines the processing to be performed for each request
 * received by the <code>invoke()</code> method of this class, utilizing the
 * "Chain of Responsibility" design pattern.  A subclass should encapsulate its
 * own processing functionality as a <code>Valve</code>, and configure this
 * Valve into the pipeline by calling <code>setBasic()</code>.
 * <p>
 * This implementation fires property change events, per the JavaBeans design
 * pattern, for changes in singleton properties.  In addition, it fires the
 * following <code>ContainerEvent</code> events to listeners who register
 * themselves with <code>addContainerListener()</code>:
 * <table border=1>
 *   <tr>
 *     <th>Type</th>
 *     <th>Data</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td align=center><code>addChild</code></td>
 *     <td align=center><code>Container</code></td>
 *     <td>Child container added to this Container.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>addValve</code></td>
 *     <td align=center><code>Valve</code></td>
 *     <td>Valve added to this Container.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>removeChild</code></td>
 *     <td align=center><code>Container</code></td>
 *     <td>Child container removed from this Container.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>removeValve</code></td>
 *     <td align=center><code>Valve</code></td>
 *     <td>Valve removed from this Container.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>start</code></td>
 *     <td align=center><code>null</code></td>
 *     <td>Container was started.</td>
 *   </tr>
 *   <tr>
 *     <td align=center><code>stop</code></td>
 *     <td align=center><code>null</code></td>
 *     <td>Container was stopped.</td>
 *   </tr>
 * </table>
 * Subclasses that fire additional events should document them in the
 * class comments of the implementation class.
 *
 * @author Craig R. McClanahan
 */

public abstract class ContainerBase
    implements Container, Lifecycle, Pipeline, MBeanRegistration, Serializable {

    private static org.apache.commons.logging.Log log=
        org.apache.commons.logging.LogFactory.getLog( ContainerBase.class );

    /**
     * Perform addChild with the permissions of this class.
     * addChild can be called with the XML parser on the stack,
     * this allows the XML parser to have fewer privileges than
     * Tomcat.
     */
    protected class PrivilegedAddChild
        implements PrivilegedAction {

        private Container child;

        PrivilegedAddChild(Container child) {
            this.child = child;
        }

        public Object run() {
            addChildInternal(child);
            return null;
        }

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The child Containers belonging to this Container, keyed by name.
     */
    protected HashMap children = new HashMap();


    /**
     * The debugging detail level for this component.
     */
    protected int debug = 0;


    /**
     * The processor delay for this component.
     */
    protected int backgroundProcessorDelay = -1;


    /**
     * The lifecycle event support for this component.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * The container event listeners for this Container.
     */
    protected ArrayList listeners = new ArrayList();


    /**
     * The Loader implementation with which this Container is associated.
     */
    protected Loader loader = null;


    /**
     * The Logger implementation with which this Container is associated.
     */
    protected Logger logger = null;


    /**
     * The Manager implementation with which this Container is associated.
     */
    protected Manager manager = null;


    /**
     * The cluster with which this Container is associated.
     */
    protected Cluster cluster = null;


    /**
     * The human-readable name of this Container.
     */
    protected String name = null;


    /**
     * The parent Container to which this Container is a child.
     */
    protected Container parent = null;


    /**
     * The parent class loader to be configured when we install a Loader.
     */
    protected ClassLoader parentClassLoader = null;


    /**
     * The Pipeline object with which this Container is associated.
     */
    protected Pipeline pipeline = new StandardPipeline(this);


    /**
     * The Realm with which this Container is associated.
     */
    protected Realm realm = null;


    /**
     * The resources DirContext object with which this Container is associated.
     */
    protected DirContext resources = null;


    /**
     * The string manager for this package.
     */
    protected static StringManager sm =
        StringManager.getManager(Constants.Package);


    /**
     * Has this component been started?
     */
    protected boolean started = false;

    protected boolean initialized=false;

    /**
     * The property change support for this component.
     */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);


    /**
     * The background thread.
     */
    private Thread thread = null;


    /**
     * The background thread completion semaphore.
     */
    private boolean threadDone = false;


    // ------------------------------------------------------------- Properties


    /**
     * Return the debugging detail level for this component.
     */
    public int getDebug() {

        return (this.debug);

    }


    /**
     * Set the debugging detail level for this component.
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {

        int oldDebug = this.debug;
        this.debug = debug;
        support.firePropertyChange("debug", new Integer(oldDebug),
                                   new Integer(this.debug));

    }


    /**
     * Get the delay between the invocation of the backgroundProcess method on
     * this container and its children. Child containers will not be invoked
     * if their delay value is not negative (which would mean they are using 
     * their own thread). Setting this to a positive value will cause 
     * a thread to be spawn. After waiting the specified amount of time, 
     * the thread will invoke the executePeriodic method on this container 
     * and all its children.
     */
    public int getBackgroundProcessorDelay() {
        return backgroundProcessorDelay;
    }


    /**
     * Set the delay between the invocation of the execute method on this
     * container and its children.
     * 
     * @param delay The delay in seconds between the invocation of 
     *              backgroundProcess methods
     */
    public void setBackgroundProcessorDelay(int delay) {
        backgroundProcessorDelay = delay;
    }


    /**
     * Return descriptive information about this Container implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {
        return this.getClass().getName();
    }


    /**
     * Return the Loader with which this Container is associated.  If there is
     * no associated Loader, return the Loader associated with our parent
     * Container (if any); otherwise, return <code>null</code>.
     */
    public Loader getLoader() {

        if (loader != null)
            return (loader);
        if (parent != null)
            return (parent.getLoader());
        return (null);

    }


    /**
     * Set the Loader with which this Container is associated.
     *
     * @param loader The newly associated loader
     */
    public synchronized void setLoader(Loader loader) {

        // Change components if necessary
        Loader oldLoader = this.loader;
        if (oldLoader == loader)
            return;
        this.loader = loader;

        // Stop the old component if necessary
        if (started && (oldLoader != null) &&
            (oldLoader instanceof Lifecycle)) {
            try {
                ((Lifecycle) oldLoader).stop();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setLoader: stop: ", e);
            }
        }

        // Start the new component if necessary
        if (loader != null)
            loader.setContainer(this);
        if (started && (loader != null) &&
            (loader instanceof Lifecycle)) {
            try {
                ((Lifecycle) loader).start();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setLoader: start: ", e);
            }
        }

        // Report this property change to interested listeners
        support.firePropertyChange("loader", oldLoader, this.loader);

    }


    /**
     * Return the Logger with which this Container is associated.  If there is
     * no associated Logger, return the Logger associated with our parent
     * Container (if any); otherwise return <code>null</code>.
     */
    public Logger getLogger() {

        if (logger != null)
            return (logger);
        if (parent != null)
            return (parent.getLogger());
        return (null);

    }


    /**
     * Set the Logger with which this Container is associated.
     *
     * @param logger The newly associated Logger
     */
    public synchronized void setLogger(Logger logger) {

        // Change components if necessary
        Logger oldLogger = this.logger;
        if (oldLogger == logger)
            return;
        this.logger = logger;

        // Stop the old component if necessary
        if (started && (oldLogger != null) &&
            (oldLogger instanceof Lifecycle)) {
            try {
                ((Lifecycle) oldLogger).stop();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setLogger: stop: ", e);
            }
        }

        
        // Start the new component if necessary
        if (logger != null)
            logger.setContainer(this);
        if (started && (logger != null) &&
            (logger instanceof Lifecycle)) {
            try {
                ((Lifecycle) logger).start();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setLogger: start: ", e);
            }
        }

        // Report this property change to interested listeners
        support.firePropertyChange("logger", oldLogger, this.logger);

    }


    /**
     * Return the Manager with which this Container is associated.  If there is
     * no associated Manager, return the Manager associated with our parent
     * Container (if any); otherwise return <code>null</code>.
     */
    public Manager getManager() {

        if (manager != null)
            return (manager);
        if (parent != null)
            return (parent.getManager());
        return (null);

    }


    /**
     * Set the Manager with which this Container is associated.
     *
     * @param manager The newly associated Manager
     */
    public synchronized void setManager(Manager manager) {

        // Change components if necessary
        Manager oldManager = this.manager;
        if (oldManager == manager)
            return;
        this.manager = manager;

        // Stop the old component if necessary
        if (started && (oldManager != null) &&
            (oldManager instanceof Lifecycle)) {
            try {
                ((Lifecycle) oldManager).stop();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setManager: stop: ", e);
            }
        }

        // Start the new component if necessary
        if (manager != null)
            manager.setContainer(this);
        if (started && (manager != null) &&
            (manager instanceof Lifecycle)) {
            try {
                ((Lifecycle) manager).start();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setManager: start: ", e);
            }
        }

        // Report this property change to interested listeners
        support.firePropertyChange("manager", oldManager, this.manager);

    }


    /**
     * Return an object which may be utilized for mapping to this component.
     */
    public Object getMappingObject() {
        return this;
    }


    /**
     * Return the Cluster with which this Container is associated.  If there is
     * no associated Cluster, return the Cluster associated with our parent
     * Container (if any); otherwise return <code>null</code>.
     */
    public Cluster getCluster() {
        if (cluster != null)
            return (cluster);

        if (parent != null)
            return (parent.getCluster());

        return (null);
    }


    /**
     * Set the Cluster with which this Container is associated.
     *
     * @param cluster The newly associated Cluster
     */
    public synchronized void setCluster(Cluster cluster) {
        // Change components if necessary
        Cluster oldCluster = this.cluster;
        if (oldCluster == cluster)
            return;
        this.cluster = cluster;

        // Stop the old component if necessary
        if (started && (oldCluster != null) &&
            (oldCluster instanceof Lifecycle)) {
            try {
                ((Lifecycle) oldCluster).stop();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setCluster: stop: ", e);
            }
        }

        // Start the new component if necessary
        if (cluster != null)
            cluster.setContainer(this);

        if (started && (cluster != null) &&
            (cluster instanceof Lifecycle)) {
            try {
                ((Lifecycle) cluster).start();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setCluster: start: ", e);
            }
        }

        // Report this property change to interested listeners
        support.firePropertyChange("cluster", oldCluster, this.cluster);
    }


    /**
     * Return a name string (suitable for use by humans) that describes this
     * Container.  Within the set of child containers belonging to a particular
     * parent, Container names must be unique.
     */
    public String getName() {

        return (name);

    }


    /**
     * Set a name string (suitable for use by humans) that describes this
     * Container.  Within the set of child containers belonging to a particular
     * parent, Container names must be unique.
     *
     * @param name New name of this container
     *
     * @exception IllegalStateException if this Container has already been
     *  added to the children of a parent Container (after which the name
     *  may not be changed)
     */
    public void setName(String name) {

        String oldName = this.name;
        this.name = name;
        support.firePropertyChange("name", oldName, this.name);
    }


    /**
     * Return the Container for which this Container is a child, if there is
     * one.  If there is no defined parent, return <code>null</code>.
     */
    public Container getParent() {

        return (parent);

    }


    /**
     * Set the parent Container to which this Container is being added as a
     * child.  This Container may refuse to become attached to the specified
     * Container by throwing an exception.
     *
     * @param container Container to which this Container is being added
     *  as a child
     *
     * @exception IllegalArgumentException if this Container refuses to become
     *  attached to the specified Container
     */
    public void setParent(Container container) {

        Container oldParent = this.parent;
        this.parent = container;
        support.firePropertyChange("parent", oldParent, this.parent);

    }


    /**
     * Return the parent class loader (if any) for this web application.
     * This call is meaningful only <strong>after</strong> a Loader has
     * been configured.
     */
    public ClassLoader getParentClassLoader() {
        if (parentClassLoader != null)
            return (parentClassLoader);
        if (parent != null) {
            return (parent.getParentClassLoader());
        }
        return (ClassLoader.getSystemClassLoader());

    }


    /**
     * Set the parent class loader (if any) for this web application.
     * This call is meaningful only <strong>before</strong> a Loader has
     * been configured, and the specified value (if non-null) should be
     * passed as an argument to the class loader constructor.
     *
     *
     * @param parent The new parent class loader
     */
    public void setParentClassLoader(ClassLoader parent) {
        ClassLoader oldParentClassLoader = this.parentClassLoader;
        this.parentClassLoader = parent;
        support.firePropertyChange("parentClassLoader", oldParentClassLoader,
                                   this.parentClassLoader);

    }


    /**
     * Return the Pipeline object that manages the Valves associated with
     * this Container.
     */
    public Pipeline getPipeline() {

        return (this.pipeline);

    }


    /**
     * Return the Realm with which this Container is associated.  If there is
     * no associated Realm, return the Realm associated with our parent
     * Container (if any); otherwise return <code>null</code>.
     */
    public Realm getRealm() {

        if (realm != null)
            return (realm);
        if (parent != null)
            return (parent.getRealm());
        return (null);

    }


    /**
     * Set the Realm with which this Container is associated.
     *
     * @param realm The newly associated Realm
     */
    public synchronized void setRealm(Realm realm) {

        // Change components if necessary
        Realm oldRealm = this.realm;
        if (oldRealm == realm)
            return;
        this.realm = realm;

        // Stop the old component if necessary
        if (started && (oldRealm != null) &&
            (oldRealm instanceof Lifecycle)) {
            try {
                ((Lifecycle) oldRealm).stop();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setRealm: stop: ", e);
            }
        }

        // Start the new component if necessary
        if (realm != null)
            realm.setContainer(this);
        if (started && (realm != null) &&
            (realm instanceof Lifecycle)) {
            try {
                ((Lifecycle) realm).start();
            } catch (LifecycleException e) {
                log.error("ContainerBase.setRealm: start: ", e);
            }
        }

        // Report this property change to interested listeners
        support.firePropertyChange("realm", oldRealm, this.realm);

    }


    /**
      * Return the resources DirContext object with which this Container is
      * associated.  If there is no associated resources object, return the
      * resources associated with our parent Container (if any); otherwise
      * return <code>null</code>.
     */
    public DirContext getResources() {
        if (resources != null)
            return (resources);
        if (parent != null)
            return (parent.getResources());
        return (null);

    }


    /**
     * Set the resources DirContext object with which this Container is
     * associated.
     *
     * @param resources The newly associated DirContext
     */
    public synchronized void setResources(DirContext resources) {
        // Called from StandardContext.setResources()
        //              <- StandardContext.start() 
        //              <- ContainerBase.addChildInternal() 

        // Change components if necessary
        DirContext oldResources = this.resources;
        if (oldResources == resources)
            return;
        Hashtable env = new Hashtable();
        if (getParent() != null)
            env.put(ProxyDirContext.HOST, getParent().getName());
        env.put(ProxyDirContext.CONTEXT, getName());
        this.resources = new ProxyDirContext(env, resources);
        // Report this property change to interested listeners
        support.firePropertyChange("resources", oldResources, this.resources);

    }


    // ------------------------------------------------------ Container Methods


    /**
     * Add a new child Container to those associated with this Container,
     * if supported.  Prior to adding this Container to the set of children,
     * the child's <code>setParent()</code> method must be called, with this
     * Container as an argument.  This method may thrown an
     * <code>IllegalArgumentException</code> if this Container chooses not
     * to be attached to the specified Container, in which case it is not added
     *
     * @param child New child Container to be added
     *
     * @exception IllegalArgumentException if this exception is thrown by
     *  the <code>setParent()</code> method of the child Container
     * @exception IllegalArgumentException if the new child does not have
     *  a name unique from that of existing children of this Container
     * @exception IllegalStateException if this Container does not support
     *  child Containers
     */
    public void addChild(Container child) {
        if (System.getSecurityManager() != null) {
            PrivilegedAction dp =
                new PrivilegedAddChild(child);
            AccessController.doPrivileged(dp);
        } else {
            addChildInternal(child);
        }
    }

    private void addChildInternal(Container child) {

        if( log.isDebugEnabled() )
            log.debug("Add child " + child + " " + this);
        synchronized(children) {
            if (children.get(child.getName()) != null)
                throw new IllegalArgumentException("addChild:  Child name '" +
                                                   child.getName() +
                                                   "' is not unique");
            child.setParent(this);  // May throw IAE
            if (started && (child instanceof Lifecycle)) {
                try {
                    ((Lifecycle) child).start();
                } catch (LifecycleException e) {
                    log.error("ContainerBase.addChild: start: ", e);
                    throw new IllegalStateException
                        ("ContainerBase.addChild: start: " + e);
                }
            }
            children.put(child.getName(), child);

            fireContainerEvent(ADD_CHILD_EVENT, child);
        }

    }


    /**
     * Add a container event listener to this component.
     *
     * @param listener The listener to add
     */
    public void addContainerListener(ContainerListener listener) {

        synchronized (listeners) {
            listeners.add(listener);
        }

    }


    /**
     * Add a property change listener to this component.
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {

        support.addPropertyChangeListener(listener);

    }


    /**
     * Return the child Container, associated with this Container, with
     * the specified name (if any); otherwise, return <code>null</code>
     *
     * @param name Name of the child Container to be retrieved
     */
    public Container findChild(String name) {

        if (name == null)
            return (null);
        synchronized (children) {       // Required by post-start changes
            return ((Container) children.get(name));
        }

    }


    /**
     * Return the set of children Containers associated with this Container.
     * If this Container has no children, a zero-length array is returned.
     */
    public Container[] findChildren() {

        synchronized (children) {
            Container results[] = new Container[children.size()];
            return ((Container[]) children.values().toArray(results));
        }

    }


    /**
     * Return the set of container listeners associated with this Container.
     * If this Container has no registered container listeners, a zero-length
     * array is returned.
     */
    public ContainerListener[] findContainerListeners() {

        synchronized (listeners) {
            ContainerListener[] results = 
                new ContainerListener[listeners.size()];
            return ((ContainerListener[]) listeners.toArray(results));
        }

    }


    /**
     * Process the specified Request, to produce the corresponding Response,
     * by invoking the first Valve in our pipeline (if any), or the basic
     * Valve otherwise.
     *
     * @param request Request to be processed
     * @param response Response to be produced
     *
     * @exception IllegalStateException if neither a pipeline or a basic
     *  Valve have been configured for this Container
     * @exception IOException if an input/output error occurred while
     *  processing
     * @exception ServletException if a ServletException was thrown
     *  while processing this request
     */
    public final void invoke(Request request, Response response)
        throws IOException, ServletException {

        pipeline.invoke(request, response);

    }


    /**
     * Remove an existing child Container from association with this parent
     * Container.
     *
     * @param child Existing child Container to be removed
     */
    public void removeChild(Container child) {

        synchronized(children) {
            if (children.get(child.getName()) == null)
                return;
            children.remove(child.getName());
        }
        
        if (started && (child instanceof Lifecycle)) {
            try {
                if( child instanceof ContainerBase ) {
                    if( ((ContainerBase)child).started ) {
                        ((Lifecycle) child).stop();
                    }
                } else {
                    ((Lifecycle) child).stop();
                }
            } catch (LifecycleException e) {
                log.error("ContainerBase.removeChild: stop: ", e);
            }
        }
        
        fireContainerEvent(REMOVE_CHILD_EVENT, child);
        
        // child.setParent(null);

    }


    /**
     * Remove a container event listener from this component.
     *
     * @param listener The listener to remove
     */
    public void removeContainerListener(ContainerListener listener) {

        synchronized (listeners) {
            listeners.remove(listener);
        }

    }


    /**
     * Remove a property change listener from this component.
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {

        support.removePropertyChangeListener(listener);

    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {

        lifecycle.addLifecycleListener(listener);

    }


    /**
     * Get the lifecycle listeners associated with this lifecycle. If this 
     * Lifecycle has no listeners registered, a zero-length array is returned.
     */
    public LifecycleListener[] findLifecycleListeners() {

        return lifecycle.findLifecycleListeners();

    }


    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener) {

        lifecycle.removeLifecycleListener(listener);

    }


    /**
     * Prepare for active use of the public methods of this Component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents it from being started
     */
    public synchronized void start() throws LifecycleException {

        // Validate and update our current component state
        if (started) {
            log.info(sm.getString("containerBase.alreadyStarted", logName()));
            return;
        }
        
        if( logger instanceof LoggerBase ) {
            LoggerBase lb=(LoggerBase)logger;
            if( lb.getObjectName()==null ) {
                ObjectName lname=lb.createObjectName();
                try {
                    Registry.getRegistry().registerComponent(lb, lname, null);
                } catch( Exception ex ) {
                    log.error( "Can't register logger " + lname, ex);
                }
            }
        }
        
        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);

        started = true;

        // Start our subordinate components, if any
        if ((loader != null) && (loader instanceof Lifecycle))
            ((Lifecycle) loader).start();
        if ((logger != null) && (logger instanceof Lifecycle))
            ((Lifecycle) logger).start();
        if ((manager != null) && (manager instanceof Lifecycle))
            ((Lifecycle) manager).start();
        if ((cluster != null) && (cluster instanceof Lifecycle))
            ((Lifecycle) cluster).start();
        if ((realm != null) && (realm instanceof Lifecycle))
            ((Lifecycle) realm).start();
        if ((resources != null) && (resources instanceof Lifecycle))
            ((Lifecycle) resources).start();

        // Start our child containers, if any
        Container children[] = findChildren();
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof Lifecycle)
                ((Lifecycle) children[i]).start();
        }

        // Start the Valves in our pipeline (including the basic), if any
        if (pipeline instanceof Lifecycle)
            ((Lifecycle) pipeline).start();

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(START_EVENT, null);

        // Start our thread
        threadStart();

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);

    }


    /**
     * Gracefully shut down active use of the public methods of this Component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public synchronized void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started) {
            log.info(sm.getString("containerBase.notStarted", logName()));
            return;
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);

        // Stop our thread
        threadStop();

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Stop the Valves in our pipeline (including the basic), if any
        if (pipeline instanceof Lifecycle) {
            ((Lifecycle) pipeline).stop();
        }

        // Stop our child containers, if any
        Container children[] = findChildren();
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof Lifecycle)
                ((Lifecycle) children[i]).stop();
        }
        // Remove children - so next start can work
        children = findChildren();
        for (int i = 0; i < children.length; i++) {
            removeChild(children[i]);
        }

        // Stop our subordinate components, if any
        if ((resources != null) && (resources instanceof Lifecycle)) {
            ((Lifecycle) resources).stop();
        }
        if ((realm != null) && (realm instanceof Lifecycle)) {
            ((Lifecycle) realm).stop();
        }
        if ((cluster != null) && (cluster instanceof Lifecycle)) {
            ((Lifecycle) cluster).stop();
        }
        if ((manager != null) && (manager instanceof Lifecycle)) {
            ((Lifecycle) manager).stop();
        }
        if ((logger != null) && (logger instanceof Lifecycle)) {
            ((Lifecycle) logger).stop();
        }
        if ((loader != null) && (loader instanceof Lifecycle)) {
            ((Lifecycle) loader).stop();
        }

        if( logger instanceof LoggerBase ) {
            LoggerBase lb=(LoggerBase)logger;
            if( lb.getObjectName()!=null ) {
                try {
                    Registry.getRegistry().unregisterComponent(lb.getObjectName());
                } catch( Exception ex ) {
                    log.error( "Can't unregister logger " + lb.getObjectName(), ex);
                }
            }
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);

    }

    /** Init method, part of the MBean lifecycle.
     *  If the container was added via JMX, it'll register itself with the 
     * parent, using the ObjectName conventions to locate the parent.
     * 
     *  If the container was added directly and it doesn't have an ObjectName,
     * it'll create a name and register itself with the JMX console. On destroy(), 
     * the object will unregister.
     * 
     * @throws Exception
     */ 
    public void init() throws Exception {

        if( this.getParent() == null ) {
            // "Life" update
            ObjectName parentName=getParentName();

            //log.info("Register " + parentName );
            if( parentName != null && 
                    mserver.isRegistered(parentName)) 
            {
                mserver.invoke(parentName, "addChild", new Object[] { this },
                        new String[] {"org.apache.catalina.Container"});
            }
        }
        initialized=true;
    }
    
    public ObjectName getParentName() throws MalformedObjectNameException {
        return null;
    }
    
    public void destroy() throws Exception {
        if( started ) {
            stop();
        }
        initialized=false;

        // unregister this component
        if ( oname != null ) {
            try {
                if( controller == oname ) {
                    Registry.getRegistry().unregisterComponent(oname);
                    log.debug("unregistering " + oname);
                }
            } catch( Throwable t ) {
                log.error("Error unregistering ", t );
            }
        }

        if (parent != null) {
            parent.removeChild(this);
        }

        // Stop our child containers, if any
        Container children[] = findChildren();
        for (int i = 0; i < children.length; i++) {
            removeChild(children[i]);
        }
                
    }

    // ------------------------------------------------------- Pipeline Methods


    /**
     * Add a new Valve to the end of the pipeline associated with this
     * Container.  Prior to adding the Valve, the Valve's
     * <code>setContainer</code> method must be called, with this Container
     * as an argument.  The method may throw an
     * <code>IllegalArgumentException</code> if this Valve chooses not to
     * be associated with this Container, or <code>IllegalStateException</code>
     * if it is already associated with a different Container.
     *
     * @param valve Valve to be added
     *
     * @exception IllegalArgumentException if this Container refused to
     *  accept the specified Valve
     * @exception IllegalArgumentException if the specifie Valve refuses to be
     *  associated with this Container
     * @exception IllegalStateException if the specified Valve is already
     *  associated with a different Container
     */
    public synchronized void addValve(Valve valve) {

        pipeline.addValve(valve);
        fireContainerEvent(ADD_VALVE_EVENT, valve);
    }

    public ObjectName[] getValveObjectNames() {
        return ((StandardPipeline)pipeline).getValveObjectNames();
    }
    
    /**
     * <p>Return the Valve instance that has been distinguished as the basic
     * Valve for this Pipeline (if any).
     */
    public Valve getBasic() {

        return (pipeline.getBasic());

    }


    /**
     * Return the set of Valves in the pipeline associated with this
     * Container, including the basic Valve (if any).  If there are no
     * such Valves, a zero-length array is returned.
     */
    public Valve[] getValves() {

        return (pipeline.getValves());

    }


    /**
     * Remove the specified Valve from the pipeline associated with this
     * Container, if it is found; otherwise, do nothing.
     *
     * @param valve Valve to be removed
     */
    public synchronized void removeValve(Valve valve) {

        pipeline.removeValve(valve);
        fireContainerEvent(REMOVE_VALVE_EVENT, valve);
    }


    /**
     * <p>Set the Valve instance that has been distinguished as the basic
     * Valve for this Pipeline (if any).  Prioer to setting the basic Valve,
     * the Valve's <code>setContainer()</code> will be called, if it
     * implements <code>Contained</code>, with the owning Container as an
     * argument.  The method may throw an <code>IllegalArgumentException</code>
     * if this Valve chooses not to be associated with this Container, or
     * <code>IllegalStateException</code> if it is already associated with
     * a different Container.</p>
     *
     * @param valve Valve to be distinguished as the basic Valve
     */
    public void setBasic(Valve valve) {

        pipeline.setBasic(valve);

    }


    /**
     * Execute a periodic task, such as reloading, etc. This method will be
     * invoked inside the classloading context of this container. Unexpected
     * throwables will be caught and logged.
     */
    public void backgroundProcess() {
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Notify all container event listeners that a particular event has
     * occurred for this Container.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @param type Event type
     * @param data Event data
     */
    public void fireContainerEvent(String type, Object data) {

        if (listeners.size() < 1)
            return;
        ContainerEvent event = new ContainerEvent(this, type, data);
        ContainerListener list[] = new ContainerListener[0];
        synchronized (listeners) {
            list = (ContainerListener[]) listeners.toArray(list);
        }
        for (int i = 0; i < list.length; i++)
            ((ContainerListener) list[i]).containerEvent(event);

    }


    /**
     * Log the specified message to our current Logger (if any).
     *
     * @param message Message to be logged
     */
    protected void log(String message) {

//         Logger logger = getLogger();
//         if (logger != null)
//             logger.log(logName() + ": " + message);
//         else
            log.info(message);
    }


    /**
     * Log the specified message and exception to our current Logger
     * (if any).
     *
     * @param message Message to be logged
     * @param throwable Related exception
     */
    protected void log(String message, Throwable throwable) {

        Logger logger = getLogger();
        if (logger != null)
            logger.log(logName() + ": " + message, throwable);
        else {
            log.error( message, throwable );
        }

    }


    /**
     * Return the abbreviated name of this container for logging messsages
     */
    protected String logName() {

        String className = this.getClass().getName();
        int period = className.lastIndexOf(".");
        if (period >= 0)
            className = className.substring(period + 1);
        return (className + "[" + getName() + "]");

    }

    // -------------------- JMX and Registration  --------------------
    protected String type;
    protected String domain;
    protected String suffix;
    protected ObjectName oname;
    protected ObjectName controller;
    protected transient MBeanServer mserver;

    public ObjectName getJmxName() {
        return oname;
    }
    
    public String getObjectName() {
        if (oname != null) {
            return oname.toString();
        } else return null;
    }

    public String getDomain() {
        if( domain==null ) {
            Container parent=this;
            while( parent != null &&
                    !( parent instanceof StandardEngine) ) {
                parent=parent.getParent();
            }
            if( parent instanceof StandardEngine ) {
                domain=((StandardEngine)parent).getDomain();
            } 
        }
        return domain;
    }

    public void setDomain(String domain) {
        this.domain=domain;
    }
    
    public String getType() {
        return type;
    }

    protected String getJSR77Suffix() {
        return suffix;
    }

    public ObjectName preRegister(MBeanServer server,
                                  ObjectName name) throws Exception {
        oname=name;
        mserver=server;
        if (name == null ){
            return null;
        }

        domain=name.getDomain();

        type=name.getKeyProperty("type");
        if( type==null ) {
            type=name.getKeyProperty("j2eeType");
        }

        String j2eeApp=name.getKeyProperty("J2EEApplication");
        String j2eeServer=name.getKeyProperty("J2EEServer");
        if( j2eeApp==null ) {
            j2eeApp="none";
        }
        if( j2eeServer==null ) {
            j2eeServer="none";
        }
        suffix=",J2EEApplication=" + j2eeApp + ",J2EEServer=" + j2eeServer;
        return name;
    }

    public void postRegister(Boolean registrationDone) {
    }

    public void preDeregister() throws Exception {
    }

    public void postDeregister() {
    }

    public ObjectName[] getChildren() {
        ObjectName result[]=new ObjectName[children.size()];
        Iterator it=children.values().iterator();
        int i=0;
        while( it.hasNext() ) {
            Object next=it.next();
            if( next instanceof ContainerBase ) {
                result[i++]=((ContainerBase)next).getJmxName();
            }
        }
        return result;
    }

    public ObjectName createObjectName(String domain, ObjectName parent)
        throws Exception
    {
        if( log.isDebugEnabled())
            log.debug("Create ObjectName " + domain + " " + parent );
        return null;
    }

    public String getContainerSuffix() {
        Container container=this;
        Container context=null;
        Container host=null;
        Container servlet=null;
        
        StringBuffer suffix=new StringBuffer();
        
        if( container instanceof StandardHost ) {
            host=container;
        } else if( container instanceof StandardContext ) {
            host=container.getParent();
            context=container;
        } else if( container instanceof StandardWrapper ) {
            context=container.getParent();
            host=context.getParent();
            servlet=container;
        }
        if( context!=null ) {
            String path=((StandardContext)context).getPath();
            suffix.append(",path=").append((path.equals("")) ? "/" : path);
        } 
        if( host!=null ) suffix.append(",host=").append( host.getName() );
        if( servlet != null ) {
            String name=container.getName();
            suffix.append(",servlet=");
            suffix.append((name=="") ? "/" : name);
        }
        return suffix.toString();
    }


    /**
     * Start the background thread that will periodically check for
     * session timeouts.
     */
    protected void threadStart() {

        if (thread != null)
            return;
        if (backgroundProcessorDelay <= 0)
            return;

        threadDone = false;
        String threadName = "ContainerBackgroundProcessor[" + toString() + "]";
        thread = new Thread(new ContainerBackgroundProcessor(), threadName);
        thread.setDaemon(true);
        thread.start();

    }


    /**
     * Stop the background thread that is periodically checking for
     * session timeouts.
     */
    protected void threadStop() {

        if (thread == null)
            return;

        threadDone = true;
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            ;
        }

        thread = null;

    }


    // -------------------------------------- ContainerExecuteDelay Inner Class


    /**
     * Private thread class to invoke the backgroundProcess method 
     * of this container and its children after a fixed delay.
     */
    protected class ContainerBackgroundProcessor implements Runnable {

        public void run() {
            while (!threadDone) {
                try {
                    Thread.sleep(backgroundProcessorDelay * 1000L);
                } catch (InterruptedException e) {
                    ;
                }
                if (!threadDone) {
                    Container parent = (Container) getMappingObject();
                    ClassLoader cl = 
                        Thread.currentThread().getContextClassLoader();
                    if (parent.getLoader() != null) {
                        cl = parent.getLoader().getClassLoader();
                    }
                    processChildren(parent, cl);
                }
            }
        }

        protected void processChildren(Container container, ClassLoader cl) {
            try {
                if (container.getLoader() != null) {
                    Thread.currentThread().setContextClassLoader
                        (container.getLoader().getClassLoader());
                }
                container.backgroundProcess();
            } catch (Throwable t) {
                log.error("Exception invoking periodic operation: ", t);
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
            Container[] children = container.findChildren();
            for (int i = 0; i < children.length; i++) {
                if (children[i].getBackgroundProcessorDelay() <= 0) {
                    processChildren(children[i], cl);
                }
            }
        }

    }


}
