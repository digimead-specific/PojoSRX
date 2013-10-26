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

import org.osgi.framework.Bundle;
import org.osgi.service.startlevel.StartLevel;

/**
 * Implementations of the StartLevel service needed by many third party bundles
 */
// TODO replace with package org.osgi.framework.startlevel
public class PojoSRStartLevelService implements StartLevel {
	@Override
	public void setStartLevel(int startlevel) {
		// empty
	}

	@Override
	public void setInitialBundleStartLevel(int startlevel) {
		// empty
	}

	@Override
	public void setBundleStartLevel(Bundle bundle, int startlevel) {
		// empty
	}

	@Override
	public boolean isBundlePersistentlyStarted(Bundle bundle) {
		return true;
	}

	@Override
	public boolean isBundleActivationPolicyUsed(Bundle bundle) {
		return false;
	}

	@Override
	public int getStartLevel() {
		return 1;
	}

	@Override
	public int getInitialBundleStartLevel() {
		return 1;
	}

	@Override
	public int getBundleStartLevel(Bundle bundle) {
		return 1;
	}
}
