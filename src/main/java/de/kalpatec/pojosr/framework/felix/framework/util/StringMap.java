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
package de.kalpatec.pojosr.framework.felix.framework.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Simple utility class that creates a map for string-based keys. This map can
 * be set to use case-sensitive or case-insensitive comparison when searching
 * for the key. Any keys put into this map will be converted to a
 * <tt>String</tt> using the <tt>toString()</tt> method, since it is only
 * intended to compare strings.
 *
 */
public class StringMap implements Map {

	private TreeMap m_map;

	public StringMap() {
		this(true);
	}

	public StringMap(boolean caseSensitive) {
		m_map = new TreeMap(new StringComparator(caseSensitive));
	}

	public StringMap(Map map, boolean caseSensitive) {
		this(caseSensitive);
		putAll(map);
	}

	public boolean isCaseSensitive() {
		return ((StringComparator) m_map.comparator()).isCaseSensitive();
	}

	public void setCaseSensitive(boolean b) {
		if (isCaseSensitive() != b) {
			TreeMap map = new TreeMap(new StringComparator(b));
			map.putAll(m_map);
			m_map = map;
		}
	}

	@Override
	public int size() {
		return m_map.size();
	}

	@Override
	public boolean isEmpty() {
		return m_map.isEmpty();
	}

	@Override
	public boolean containsKey(Object arg0) {
		return m_map.containsKey(arg0);
	}

	@Override
	public boolean containsValue(Object arg0) {
		return m_map.containsValue(arg0);
	}

	@Override
	public Object get(Object arg0) {
		return m_map.get(arg0);
	}

	@Override
	public Object put(Object key, Object value) {
		return m_map.put(key.toString(), value);
	}

	@Override
	public void putAll(Map map) {
		for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public Object remove(Object arg0) {
		return m_map.remove(arg0);
	}

	@Override
	public void clear() {
		m_map.clear();
	}

	@Override
	public Set keySet() {
		return m_map.keySet();
	}

	@Override
	public Collection values() {
		return m_map.values();
	}

	@Override
	public Set entrySet() {
		return m_map.entrySet();
	}

	@Override
	public String toString() {
		return m_map.toString();
	}
}