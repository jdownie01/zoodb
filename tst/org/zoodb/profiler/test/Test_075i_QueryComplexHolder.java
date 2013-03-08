/*
 * Copyright 2009-2012 Tilmann Z�schke. All rights reserved.
 * 
 * This file is part of ZooDB.
 * 
 * ZooDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ZooDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ZooDB.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * See the README and COPYING files for further information. 
 */
package org.zoodb.profiler.test;

import javax.jdo.PersistenceManager;

import org.junit.Before;
import org.junit.Test;
import org.zoodb.jdo.api.ZooClass;
import org.zoodb.jdo.api.ZooSchema;
import org.zoodb.profiler.test.data.ComplexHolder2;
import org.zoodb.profiler.test.util.TestTools;
import org.zoodb.test.Test_075_QueryComplexHolder;


/**
 * Perform query tests using one indexed attribute.
 * 
 * @author Tilmann Z�schke
 */
public class Test_075i_QueryComplexHolder extends Test_075_QueryComplexHolder {

	@Before
	public void createIndex() {
		PersistenceManager pm = TestTools.openPM();
		pm.currentTransaction().begin();
		ZooClass s = ZooSchema.locateClass(pm, ComplexHolder2.class);
		if (!s.locateField("i2").hasIndex()) {
			s.locateField("i2").createIndex(false);
		}
		pm.currentTransaction().commit();
		TestTools.closePM();
	}
	
	/**
	 * This failed with DataStreamCorrupted exception if makeDirty() was not called when setting
	 * attributes. In that case, the update() failed for most objects.
	 * It is unclear how this corrupted the database.
	 * -> Temporary fix: do not set objects clean in ClientSessionCache.postCommit().
	 * 
	 * The problem was that the _usedClasses in the DataSerializer were reused for objects on the 
	 * written in the same stream (same page). Randomly accessing these objects meant that the
	 * required used classes were not available, because they were only store with the first object.
	 */
	@Test
	@Override
	public void test() {
//		run(1, 1, 4);

		run(1, 50, 4);
		run(2, 50, 4);
		run(3, 50, 4);
//		run(5, 500, 6);
//		run(6, 500, 6);
//		run(7, 500, 6);
	}	
}
