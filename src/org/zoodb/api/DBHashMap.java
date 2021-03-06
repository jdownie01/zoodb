/*
 * Copyright 2009-2020 Tilmann Zaeschke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zoodb.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.zoodb.api.impl.ZooPC;

/**
 * Warning: This class does not track changes made to the valueSet(), entrySet() or keySet(). 
 * 
 * @author Tilmann Zaeschke
 *
 * @param <K> The key type
 * @param <V> The value type
 */
public class DBHashMap<K, V> extends ZooPC implements Map<K, V>, DBCollection {

	private transient HashMap<K, V> t;
	
	public DBHashMap() {
		t = new HashMap<K, V>();
	}
	
	public DBHashMap(int size) {
		t = new HashMap<K, V>(size);
	}
	
	@Override
	public void clear() {
		zooActivateWrite();
		t.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		zooActivateRead();
		return t.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		zooActivateRead();
		return t.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		zooActivateRead();
		return t.entrySet();
	}

	@Override
	public V get(Object key) {
		zooActivateRead();
		return t.get(key);
	}

	@Override
	public boolean isEmpty() {
		zooActivateRead();
		return t.isEmpty();
	}

	@Override
	public Set<K> keySet() {
		zooActivateRead();
		return t.keySet();
	}

	@Override
	public V put(K key, V value) {
		zooActivateWrite();
		return t.put(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		zooActivateWrite();
		t.putAll(m);
	}

	@Override
	public V remove(Object key) {
		zooActivateWrite();
		return t.remove(key);
	}

	@Override
	public int size() {
		zooActivateRead();
		return t.size();
	}

	@Override
	public Collection<V> values() {
		zooActivateRead();
		return t.values();
	}

	public void setBatchSize(int i) {
		System.out.println("STUB: DBHashtable.setBatchSize()");
	}

	public void resize(int size) {
		// TODO
	}
	
	@Override
	public int hashCode() {
        int h = 0;
        for (K k: keySet()) {
        	if (k != null && !(k instanceof DBCollection)) {
        		h += k.hashCode();
        	}
        }
        for (V v: values()) {
        	if (v != null && !(v instanceof DBCollection)) {
        		h += v.hashCode();
        	}
        }
        return h;
        //TODO: For some reason the following fails some tests.... (014/015)
//		return (int) (jdoZooGetOid()*10000) | size();  
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof DBHashMap)) {
			return false;
		}
		DBHashMap<?, ?> m = (DBHashMap<?, ?>) obj;
		if (size() != m.size() || jdoZooGetOid() != m.jdoZooGetOid()) {
			return false;
		}
        try {
            Iterator<Entry<K,V>> i = entrySet().iterator();
            while (i.hasNext()) {
                Entry<K,V> e = i.next();
                K key = e.getKey();
                V value = e.getValue();
                if (value == null) {
                    if (!(m.get(key)==null && m.containsKey(key)))
                        return false;
                } else {
                	Object v2 = m.get(key);
                	if (value != v2 && !value.equals(v2))
                        return false;
                }
            }
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
//		for (Map.Entry<K, V> e: entrySet()) {
//			Object v2 = o.get(e.getKey());
//			if ((e.getValue() == null && v2 != null) || !e.getValue().equals(v2)) {
//				return false;
//			}
//		}
		return true;
	}
}
