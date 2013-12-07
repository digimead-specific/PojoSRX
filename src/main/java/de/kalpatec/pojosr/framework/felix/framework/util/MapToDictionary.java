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

package de.kalpatec.pojosr.framework.felix.framework.util;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;

/**
 * This is a simple class that implements a <tt>Dictionary</tt> from a
 * <tt>Map</tt>. The resulting dictionary is immutable.
 *
 */
public class MapToDictionary extends Dictionary<String, String> {
	/**
	 * Map source.
	 *
	 */
	private Map<String, String> m_map = null;

	public MapToDictionary(Map<String, String> map) {
		if (map == null) {
			throw new IllegalArgumentException("Source map cannot be null.");
		}
		m_map = map;
	}

	@Override
	public Enumeration<String> elements() {
		return Collections.enumeration(m_map.values());
	}

	@Override
	public String get(Object key) {
		return m_map.get(key);
	}

	@Override
	public boolean isEmpty() {
		return m_map.isEmpty();
	}

	@Override
	public Enumeration<String> keys() {
		return Collections.enumeration(m_map.keySet());
	}

	@Override
	public String put(String key, String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return m_map.size();
	}

	@Override
	public String toString() {
		return m_map.toString();
	}
}
