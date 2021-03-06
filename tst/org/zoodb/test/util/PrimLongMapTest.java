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
package org.zoodb.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zoodb.internal.util.PrimLongMap;

/**
 * Test harness for PrimLongTreeMap.
 *
 * @author  Tilmann Zaeschke
 */
public abstract class PrimLongMapTest {

    private static final long KEY1 = 1;
    private static final long KEY2 = 2;
    private static final long KEY3 = 3;
    private static final long KEY4 = 4;
    private static final long KEY5 = 5;
    private static final long KEY6 = 6;
    private static final String ELEMENT1 = "first one";
    private static final String ELEMENT2 = "second element";
    private static final String ELEMENT3 = "third element";
    private static final String ELEMENT4 = "fourth element";
    private static final String ELEMENT5 = "fifth element";
    private static final String ELEMENT6 = "sixth element";
    
    private PrimLongMap<String> map;
    
    protected abstract PrimLongMap<String> createMap();
    
    /**
     * Run before each test.
     * The setUp method tests the put method of PrimLongTreeMap.
     */
    @Before
    public void before() {
        //create a PrimLongTreeMap
    	map = createMap(); 
        map.put(KEY1, ELEMENT1);
        map.put(KEY2, ELEMENT2);
        map.put(KEY3, ELEMENT3);
    }

    /**
     * Run after each test.
     */
    @After
    public void after() {
        map.clear();
    }

    /**
     * Test the clear() and size() methods.
     */
    @Test
    public void testClearAndSize() {
        assertEquals("Check size 1", 3, map.size());
        map.clear();
        assertEquals("Check size 2", 0, map.size());
    }
    
    /**
     * Test the values method returns the correct collection.
     */
    @Test
    public void testIterator() {
    	HashSet<String> temp = new HashSet<String>();
    	temp.add(ELEMENT1);
    	temp.add(ELEMENT2);
    	temp.add(ELEMENT3);
        Iterator<String> i = map.values().iterator();
        while (i.hasNext()) {
        	assertTrue(temp.remove(i.next()));
        }
        assertTrue(temp.isEmpty());
        assertFalse("Check the number of remaining elements", i.hasNext());
    }

    /**
     * Test the remove method removes the correct values.
     */
    @Test
    public void testRemove() {
        map.remove(KEY2);
        assertEquals("Check element 1", ELEMENT1, map.get(KEY1));
        assertEquals("Check element 2", null, map.get(KEY2));
        assertEquals("Check element 3", ELEMENT3, map.get(KEY3));
        assertTrue("Check the number of values", map.size()==2);
    }

    /**
     * Test the contains method returns the correct results.
     */
    @Test
    public void testContains() {
        map.remove(KEY2);
        assertEquals("Check no element", true, map.containsKey(KEY1));
        assertEquals("Check no element", false, map.containsKey(KEY2));
        assertEquals("Check element 1", true, map.containsValue(ELEMENT1));
        assertEquals("Check element 2", false, map.containsValue(ELEMENT2));
        assertEquals("Check element 3", true, map.containsValue(ELEMENT3));
        //assertEquals("Check no element", false, map.containsValue(KEY1));
        assertEquals("Check no element", false, map.containsValue(ELEMENT4));
    }

    /**
     * Test the addAll methods add the correct values.
     */
    @Test
    public void testPutAll() {
        PrimLongMap<String> h = createMap();
        h.put(KEY3, ELEMENT3);
        h.put(KEY4, ELEMENT4);
        h.put(KEY5, ELEMENT5);
        h.put(KEY6, ELEMENT6);
        
        map.putAll(h);
        assertTrue("Check the number of values", map.size()==6);
        assertEquals("Check element 1", ELEMENT1, map.get(KEY1));
        assertEquals("Check element 2", ELEMENT2, map.get(KEY2));
        assertEquals("Check element 3", ELEMENT3, map.get(KEY3));
        assertEquals("Check element 4", ELEMENT4, map.get(KEY4));
        assertEquals("Check element 5", ELEMENT5, map.get(KEY5));
        assertEquals("Check element 6", ELEMENT6, map.get(KEY6));
    }

    /**
     * Test the keySet method returns the correct set.
     */
    @Test
    public void testKeySet() {
        SortedSet<Long> keys = new TreeSet<Long>(map.keySet());
        assertTrue("Check the number of keys", keys.size()==3);
        Long[] keyArray = keys.toArray(new Long[0]);
        assertEquals("Check key 1", KEY1, (long)keyArray[0]);
        assertEquals("Check key 2", KEY2, (long)keyArray[1]);
        assertEquals("Check key 3", KEY3, (long)keyArray[2]);
    }

    /**
     * Test the entrySet method returns the correct set.
     */
    @Test
    public void testEntrySet() {
        Set<? extends PrimLongMap.PrimLongEntry<String>> entries = map.entrySet();
        assertTrue("Check the number of entries", entries.size()==3);
        boolean b1 = false;
        boolean b2 = false;
        boolean b3 = false;
        for (Iterator<? extends PrimLongMap.PrimLongEntry<String>> i = 
            entries.iterator(); i.hasNext(); ) {
        	PrimLongMap.PrimLongEntry<String> me = i.next();
            if (me.getKey() == KEY1) {
                assertEquals("Check entry 1", ELEMENT1, me.getValue());
                b1 = true;
            } else if (me.getKey() == KEY2) {
                assertEquals("Check entry 2", ELEMENT2, me.getValue());
                b2 = true;
            } else if (me.getKey() == KEY3) {
                assertEquals("Check entry 3", ELEMENT3, me.getValue());
                b3 = true;
            } else {
                fail ("Unexpected key found.");
            }
        }
        assertEquals("Check occurences of entries", b1 && b2 && b3, true);
    }

    /**
     * Test the values method returns the correct collection.
     */
    @Test
    public void testValues() {
        Collection<String> values = map.values();
        assertTrue("Check the number of values", values.size()==3);
    	HashSet<String> temp = new HashSet<String>();
    	temp.add(ELEMENT1);
    	temp.add(ELEMENT2);
    	temp.add(ELEMENT3);
        Iterator<String> i = values.iterator();
        while (i.hasNext()) {
        	assertTrue(temp.remove(i.next()));
        }
        assertTrue(temp.isEmpty());
    }
    
    @Test
    public void testPutIfAbsent() {
        assertEquals(ELEMENT1, map.putIfAbsent(KEY1, ELEMENT2));
        assertEquals(ELEMENT1, map.get(KEY1));
        assertEquals(ELEMENT1, map.put(KEY1, ELEMENT2));
        assertEquals(ELEMENT2, map.get(KEY1));
    }
}
