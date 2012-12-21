/*
 * Copyright 2011 Karl Pauls karlpauls@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.kalpatec.pojosr.framework;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;
import org.osgi.service.startlevel.StartLevel;

import de.kalpatec.pojosr.framework.felix.framework.ServiceRegistry;
import de.kalpatec.pojosr.framework.felix.framework.util.EventDispatcher;
import de.kalpatec.pojosr.framework.launch.BundleDescriptor;
import de.kalpatec.pojosr.framework.launch.ClasspathScanner;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistry;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistryFactory;

/**
 * Pojo Service Registry concrete implementation.
 */
public class PojoSR implements PojoServiceRegistry {

    private final BundleContext m_context;

    /** The {@link ServiceRegistry} responsible for keeping track of all registered services. */
    private final ServiceRegistry m_reg = new ServiceRegistry(
            new ServiceRegistry.ServiceRegistryCallbacks() {
                @Override
                public void serviceChanged(ServiceEvent event, Dictionary oldProps) {
                    m_dispatcher.fireServiceEvent(event, oldProps, null);
                }
            });

    private final EventDispatcher m_dispatcher = new EventDispatcher( m_reg );
    private final Map<Long, Bundle> m_bundles = new HashMap<Long, Bundle>();
    private final Map<String, Bundle> m_symbolicNameToBundle = new HashMap<String, Bundle>();

    //---- Inner classes ------------------------------------------------------

    /**
     * Specialized version of PojoSRBundle that acts as System Bundle.
     */
    private static class SystemBundle extends PojoSRBundle {

        public SystemBundle(Revision revision, Map<String, String> manifest, Version version, String location,
                ServiceRegistry reg, EventDispatcher dispatcher, String activatorClass, long id,
                String symbolicName, Map<Long, Bundle> bundles, ClassLoader loader) {

            super( revision, manifest, version, location, reg, dispatcher, activatorClass, id, symbolicName,
                    bundles, loader );
        }

        @Override
        public synchronized void start() throws BundleException {

            if ( m_state != Bundle.RESOLVED ) {
                return;
            }

            m_dispatcher.startDispatching();
            m_state = Bundle.STARTING;

            m_dispatcher.fireBundleEvent( new BundleEvent( BundleEvent.STARTING, this ) );
            m_context = new PojoSRBundleContext( this, m_reg, m_dispatcher, m_bundles );

            int i = 0;
            for ( Bundle b : m_bundles.values() ) {
                i++;
                try {
                    if ( b != this ) {
                        b.start();
                    }
                } catch ( Throwable t ) {
                    System.out.println( "Unable to start bundle: " + i );
                    t.printStackTrace();
                }
            }

            m_state = Bundle.ACTIVE;
            m_dispatcher.fireBundleEvent( new BundleEvent( BundleEvent.STARTED, this ) );
            m_dispatcher.fireFrameworkEvent( new FrameworkEvent( FrameworkEvent.STARTED, this, null ) );

            super.start();
        }

        @Override
        public synchronized void stop() throws BundleException {

            if ( (m_state == Bundle.STOPPING) || m_state == Bundle.RESOLVED ) {
                return;
            } else if ( m_state != Bundle.ACTIVE ) {
                throw new BundleException( "Can't stop pojosr because it is not ACTIVE" );
            }

            final Bundle systemBundle = this;

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    m_dispatcher.fireBundleEvent( new BundleEvent( BundleEvent.STOPPING, systemBundle ) );
                    for ( Bundle b : m_bundles.values() ) {
                        try {
                            if ( b != systemBundle ) {
                                b.stop();
                            }
                        } catch ( Throwable t ) {
                            t.printStackTrace();
                        }
                    }
                    
                    m_dispatcher.fireBundleEvent( new BundleEvent( BundleEvent.STOPPED, systemBundle ) );
                    m_state = Bundle.RESOLVED;
                    m_dispatcher.stopDispatching();
                }
            };

            m_state = Bundle.STOPPING;

            if ( "true".equalsIgnoreCase( System.getProperty( "de.kalpatec.pojosr.framework.events.sync" ) ) ) {
                r.run();
            } else {
                new Thread( r ).start();
            }
        }
    }

    //---- Constructors -------------------------------------------------------

    /**
     * Create a new Pojo Service Registry.
     * @param config the configuration parameters of the new Pojo Service Registry
     * @throws Exception
     */
    public PojoSR(Map config) throws Exception {

        final Map<String, String> headers = new HashMap<String, String>();
        headers.put( Constants.BUNDLE_SYMBOLICNAME,    "de.kalpatec.pojosr.framework" );
        headers.put( Constants.BUNDLE_VERSION,         "0.2.1" );
        headers.put( Constants.BUNDLE_NAME,            "System Bundle" );
        headers.put( Constants.BUNDLE_MANIFESTVERSION, "2" );
        headers.put( Constants.BUNDLE_VENDOR,          "kalpatec" );

        final Bundle sb = new SystemBundle(
                new Revision() {
                    @Override public long getLastModified() {
                        return System.currentTimeMillis();
                    }
                    @Override public Enumeration getEntries() {
                        return new Properties().elements();
                    }
                    @Override public URL getEntry(String entryName) {
                        return getClass().getClassLoader().getResource( entryName );
                    }
                },
                headers,
                new Version( 0, 0, 1 ),
                "file:pojosr",                      // location
                m_reg,
                m_dispatcher,                       // event dispatcher
                null,                               // activator class
                0,                                  // id
                "de.kalpatec.pojosr.framework",     // symbolic name
                m_bundles,
                getClass().getClassLoader() );

        m_symbolicNameToBundle.put( sb.getSymbolicName(), sb );

        sb.start();

        // Register empty implementations of the StartLevel and PackageAdmin services
        // needed by many third party bundles

        // TODO replace with package org.osgi.framework.startlevel
        sb.getBundleContext().registerService( StartLevel.class.getName(),
                new StartLevel() {
                    @Override public void setStartLevel(int startlevel) {
                        // empty
                    }
                    @Override public void setInitialBundleStartLevel(int startlevel) {
                        // empty
                    }
                    @Override public void setBundleStartLevel(Bundle bundle, int startlevel) {
                        // empty
                    }
                    @Override public boolean isBundlePersistentlyStarted(Bundle bundle) {
                        return true;
                    }
                    @Override public boolean isBundleActivationPolicyUsed(Bundle bundle) {
                        return false;
                    }
                    @Override public int getStartLevel() {
                        return 1;
                    }
                    @Override public int getInitialBundleStartLevel() {
                        return 1;
                    }
                    @Override public int getBundleStartLevel(Bundle bundle) {
                        return 1;
                    }
                },
                null );

        // TODO replace with package org.osgi.framework.wiring
        sb.getBundleContext().registerService( PackageAdmin.class.getName(),
                new PackageAdmin() {
                    @Override public boolean resolveBundles(Bundle[] bundles) {
                        return true;
                    }
                    @Override public void refreshPackages(Bundle[] bundles) {
                        m_dispatcher.fireFrameworkEvent( new FrameworkEvent( FrameworkEvent.PACKAGES_REFRESHED, sb, null ) );
                    }
                    @Override public RequiredBundle[] getRequiredBundles(String symbolicName) {
                        return null;
                    }
                    @Override public Bundle[] getHosts(Bundle bundle) {
                        return null;
                    }
                    @Override public Bundle[] getFragments(Bundle bundle) {
                        return null;
                    }
                    @Override public ExportedPackage[] getExportedPackages(String name) {
                        return null;
                    }
                    @Override public ExportedPackage[] getExportedPackages(Bundle bundle) {
                        return null;
                    }
                    @Override public ExportedPackage getExportedPackage(String name) {
                        return null;
                    }
                    @Override public Bundle[] getBundles(String symbolicName, String versionRange) {
                        Bundle result = m_symbolicNameToBundle.get( (symbolicName != null) ? symbolicName.trim() : symbolicName );
                        if ( result != null ) {
                            return new Bundle[] { result };
                        }
                        return null;
                    }
                    @Override public int getBundleType(Bundle bundle) {
                        return 0;
                    }
                    @Override public Bundle getBundle(Class clazz) {
                        return m_context.getBundle();
                    }
                },
                null );

        m_context = sb.getBundleContext();

        // Start all specified bundles

        List<BundleDescriptor> bundles = (List<BundleDescriptor>) config.get( PojoServiceRegistryFactory.BUNDLE_DESCRIPTORS );

        if ( bundles != null ) {
            startBundles( bundles );
        }
    }

    //---- Main method --------------------------------------------------------

    /**
     * Main method that can be used to automatically launch bundles placed on the classpath.
     * The following optional arguments can be provided on the command line:
     * <dl>
     *  <dt>Filter</dt>
     *  <dd>A {@link Filter} expression used to filter bundles found on the classpath.
     *  <dt>Class</dt>
     *  <dd>The fully qualified name of a class whose main method will be invoked after
     *  having loaded and started all bundles on the classpath.</dd>
     * </dl>
     * @param args The program arguments.
     * @throws Exception If the arguments are invalid.
     */
    public static void main(String[] args) throws Exception {

        Filter filter = null;
        Class main = null;

        for ( int i = 0; (args != null) && (i < args.length) && (i < 2); i++ ) {
            try {
                filter = FrameworkUtil.createFilter( args[i] );
            } catch ( InvalidSyntaxException ie ) {
                try {
                    main = PojoSR.class.getClassLoader().loadClass( args[i] );
                } catch ( Exception ex ) {
                    throw new IllegalArgumentException( "Argument is neither a filter nor a class: " + args[i] );
                }
            }
        }

        // Find all bundles on the classpath, eventually filtering them
        Map config = new HashMap();
        config.put( PojoServiceRegistryFactory.BUNDLE_DESCRIPTORS, (filter != null)
                ? new ClasspathScanner().scanForBundles( filter.toString() )
                : new ClasspathScanner().scanForBundles() );

        // Trigger the creation of an instance of this class that will manage all bundles found on the classpath.
        // This is where the framework is started and all bundles are wired together and started.
        new PojoServiceRegistryFactoryImpl().newPojoServiceRegistry( config );

        // Optional last step: if a fully qualified class name was specified on the command line, invoke
        // its main() method.
        // FIXME what must be achieved here? remove the main class argument and invoke the new main()?
        if ( main != null ) {
            int count = 0;

            if ( filter != null ) {
                count++;
            }

            if ( main != null ) {
                count++;
            }

            String[] newArgs = args;
            if ( count > 0 ) {
                newArgs = new String[args.length - count];
                System.arraycopy( args, count, newArgs, 0, newArgs.length );
            }

            main.getMethod( "main", String[].class ).invoke( null, (Object) newArgs );
        }
    }

    //---- PojoServiceRegistry methods ----------------------------------------

    @Override
    public void startBundles(List<BundleDescriptor> bundles) throws Exception {

        for ( BundleDescriptor desc : bundles ) {

            URL u = new URL( desc.getUrl().toExternalForm() + "META-INF/MANIFEST.MF" );
            Revision rev;

            if ( u.toExternalForm().startsWith( "file:" ) ) {

                File root = new File( URLDecoder.decode( desc.getUrl().getFile(), "UTF-8" ) );
                u = root.toURL();
                rev = new DirRevision( root );

            } else {

                URLConnection uc = u.openConnection();

                if ( uc instanceof JarURLConnection ) {

                    final JarURLConnection juc = (JarURLConnection) uc;

                    String target = juc.getJarFileURL().toExternalForm();
                    String prefix = null;

                    if ( !("jar:" + target + "!/").equals( desc.getUrl().toExternalForm() ) ) {
                        prefix = desc.getUrl().toExternalForm().substring( ("jar:" + target + "!/").length() );
                    }

                    rev = new JarRevision( juc.getJarFile(), juc.getJarFileURL(), prefix, juc.getLastModified() );

                } else {

                    rev = new URLRevision( desc.getUrl(), desc.getUrl().openConnection().getLastModified() );
                }
            }

            Map<String, String> bundleHeaders = desc.getHeaders();
            Version osgiVersion = null;

            try {
                osgiVersion = Version.parseVersion( bundleHeaders.get( Constants.BUNDLE_VERSION ) );
            } catch ( Exception ex ) {
                ex.printStackTrace();
                osgiVersion = Version.emptyVersion;
            }

            String sym = bundleHeaders.get( Constants.BUNDLE_SYMBOLICNAME );
            if ( sym != null ) {
                int idx = sym.indexOf( ';' );
                if ( idx > 0 ) {
                    sym = sym.substring( 0, idx );
                }
                sym = sym.trim();
            }

            if ( (sym == null) || !m_symbolicNameToBundle.containsKey( sym ) ) {

                // TODO: framework - support multiple versions
                Bundle bundle = new PojoSRBundle( rev, bundleHeaders,
                        osgiVersion, desc.getUrl().toExternalForm(), m_reg,
                        m_dispatcher, bundleHeaders.get( Constants.BUNDLE_ACTIVATOR ),
                        m_bundles.size(), sym, m_bundles, desc.getClassLoader() );

                if ( sym != null ) {
                    m_symbolicNameToBundle.put( bundle.getSymbolicName(), bundle );
                }
            }
        }

        for ( long i = 1; i < m_bundles.size(); i++ ) {
            try {
                m_bundles.get( i ).start();
            } catch ( Throwable e ) {
                System.out.println( "Unable to start bundle: " + i );
                e.printStackTrace();
            }
        }
    }

    @Override
    public BundleContext getBundleContext() {
        return m_context;
    }

    @Override
    public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
        m_context.addServiceListener( listener, filter );
    }

    @Override
    public void addServiceListener(ServiceListener listener) {
        m_context.addServiceListener( listener );
    }

    @Override
    public void removeServiceListener(ServiceListener listener) {
        m_context.removeServiceListener( listener );
    }

    @Override
    public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties) {
        return m_context.registerService( clazzes, service, properties );
    }

    @Override
    public ServiceRegistration registerService(String clazz, Object service, Dictionary properties) {
        return m_context.registerService( clazz, service, properties );
    }

    @Override
    public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        return m_context.getServiceReferences( clazz, filter );
    }

    @Override
    public ServiceReference getServiceReference(String clazz) {
        return m_context.getServiceReference( clazz );
    }

    @Override
    public Object getService(ServiceReference reference) {
        return m_context.getService( reference );
    }

    @Override
    public boolean ungetService(ServiceReference reference) {
        return m_context.ungetService( reference );
    }
}
