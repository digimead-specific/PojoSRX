/**
 * Copyright 2011 Karl Pauls karlpauls@gmail.com
 * Copyright 2013 Alexey Aksenov ezh@ezh.msk.ru
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

import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.Version;

import de.kalpatec.pojosr.framework.felix.framework.ServiceRegistry;
import de.kalpatec.pojosr.framework.felix.framework.util.EventDispatcher;

/**
 * Specialized version of PojoSRBundle that acts as System Bundle.
 */
public class PojoSRSystemBundle extends PojoSRBundle {

	public PojoSRSystemBundle(Revision revision, Map<String, String> manifest, Version version, String location, ServiceRegistry reg, EventDispatcher dispatcher, String activatorClass, long id, String symbolicName, Map<Long, Bundle> bundles, ClassLoader loader) {
		super(revision, manifest, version, location, reg, dispatcher, activatorClass, id, symbolicName, bundles, loader);
	}

	@Override
	public synchronized void start() throws BundleException {

		if (m_state != Bundle.RESOLVED) {
			return;
		}

		m_dispatcher.startDispatching();
		m_state = Bundle.STARTING;

		m_dispatcher.fireBundleEvent(new BundleEvent(BundleEvent.STARTING, this));
		m_context = new PojoSRBundleContext(this, m_reg, m_dispatcher, m_bundles);

		int i = 0;
		for (Bundle b : m_bundles.values()) {
			i++;
			try {
				if (b != this) {
					b.start();
				}
			} catch (Throwable t) {
				System.out.println("Unable to start bundle: " + i);
				t.printStackTrace();
			}
		}

		m_state = Bundle.ACTIVE;
		m_dispatcher.fireBundleEvent(new BundleEvent(BundleEvent.STARTED, this));
		m_dispatcher.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED, this, null));

		super.start();
	}

	@Override
	public synchronized void stop() throws BundleException {

		if ((m_state == Bundle.STOPPING) || m_state == Bundle.RESOLVED) {
			return;
		} else if (m_state != Bundle.ACTIVE) {
			throw new BundleException("Can't stop pojosr because it is not ACTIVE");
		}

		final Bundle systemBundle = this;

		Runnable r = new Runnable() {
			@Override
			public void run() {
				m_dispatcher.fireBundleEvent(new BundleEvent(BundleEvent.STOPPING, systemBundle));
				for (Bundle b : m_bundles.values()) {
					try {
						if (b != systemBundle) {
							b.stop();
						}
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}

				m_dispatcher.fireBundleEvent(new BundleEvent(BundleEvent.STOPPED, systemBundle));
				m_state = Bundle.RESOLVED;
				m_dispatcher.stopDispatching();
			}
		};

		m_state = Bundle.STOPPING;

		if ("true".equalsIgnoreCase(System.getProperty("de.kalpatec.pojosr.framework.events.sync"))) {
			r.run();
		} else {
			new Thread(r).start();
		}
	}
}
