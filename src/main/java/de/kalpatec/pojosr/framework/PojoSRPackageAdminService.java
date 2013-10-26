/*
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

import de.kalpatec.pojosr.framework.felix.framework.util.EventDispatcher;

/**
 * Implementations of the PackageAdmin service needed by many third party
 * bundles
 */
// TODO replace with package org.osgi.framework.wiring
public class PojoSRPackageAdminService implements PackageAdmin {

	protected final EventDispatcher m_dispatcher;
	protected final BundleContext m_context;
	protected final Map<String, Bundle> m_symbolicNameToBundle;

	/**
	 * Create a new Pojo Service Registry.
	 *
	 * @param config
	 *            the configuration parameters of the new Pojo Service Registry
	 * @throws Exception
	 */
	public PojoSRPackageAdminService(EventDispatcher m_dispatcher, BundleContext m_context, Map<String, Bundle> m_symbolicNameToBundle) throws Exception {
		this.m_dispatcher = m_dispatcher;
		this.m_context = m_context;
		this.m_symbolicNameToBundle = m_symbolicNameToBundle;
	}

	@Override
	public boolean resolveBundles(Bundle[] bundles) {
		return true;
	}

	@Override
	public void refreshPackages(Bundle[] bundles) {
		m_dispatcher.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.PACKAGES_REFRESHED, m_context.getBundle(), null));
	}

	@Override
	public RequiredBundle[] getRequiredBundles(String symbolicName) {
		return null;
	}

	@Override
	public Bundle[] getHosts(Bundle bundle) {
		return null;
	}

	@Override
	public Bundle[] getFragments(Bundle bundle) {
		return null;
	}

	@Override
	public ExportedPackage[] getExportedPackages(String name) {
		return null;
	}

	@Override
	public ExportedPackage[] getExportedPackages(Bundle bundle) {
		return null;
	}

	@Override
	public ExportedPackage getExportedPackage(String name) {
		return null;
	}

	@Override
	public Bundle[] getBundles(String symbolicName, String versionRange) {
		Bundle result = m_symbolicNameToBundle.get((symbolicName != null) ? symbolicName.trim() : symbolicName);
		if (result != null) {
			return new Bundle[] { result };
		}
		return null;
	}

	@Override
	public int getBundleType(Bundle bundle) {
		return 0;
	}

	@Override
	public Bundle getBundle(Class clazz) {
		return m_context.getBundle();
	}
}
