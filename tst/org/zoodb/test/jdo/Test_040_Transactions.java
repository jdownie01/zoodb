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
package org.zoodb.test.jdo;

import static org.junit.Assert.fail;

import java.util.Properties;

import javax.jdo.JDOFatalUserException;
import javax.jdo.JDOHelper;
import javax.jdo.JDOUserException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zoodb.jdo.ZooJdoProperties;
import org.zoodb.test.testutil.TestTools;

public class Test_040_Transactions {

	private static final String DB_NAME = "TestDb";
	
	private PersistenceManager pm;
	private PersistenceManagerFactory pmf;
	
	@BeforeClass
	public static void setUp() {
		TestTools.createDb(DB_NAME);
	}

	@Test
	public void testTransaction() {
		Properties props = new ZooJdoProperties(DB_NAME);
		pmf = JDOHelper.getPersistenceManagerFactory(props);
		pm = pmf.getPersistenceManager();

		//test before begin()
		try {
			pm.currentTransaction().commit();
			fail();
		} catch (JDOUserException e) {
			//good
		}
		try {
			pm.currentTransaction().rollback();
			fail();
		} catch (JDOUserException e) {
			//good
		}

		//begin -> commit
		pm.currentTransaction().begin();
		try {
			pm.currentTransaction().begin();
			fail();
		} catch (JDOUserException e) {
			//good
		}

		pm.currentTransaction().commit();
		try {
			pm.currentTransaction().commit();
			fail();
		} catch (JDOUserException e) {
			//good
		}
		
		//begin -> rollback
		pm.currentTransaction().begin();
		pm.currentTransaction().rollback();
		try {
			pm.currentTransaction().rollback();
			fail();
		} catch (JDOUserException e) {
			//good
		}
		
		
		pm.currentTransaction().begin();
		try {
			pm.close();
			fail();
		} catch (JDOUserException e) {
			//good
		}
		try {
			pmf.close();
			fail();
		} catch (JDOUserException e) {
			//good
		}
		
		//close
		pm.currentTransaction().rollback();
		pm.close();
		try {
			pm.currentTransaction();//.begin();
			fail();
		} catch (JDOFatalUserException e) {
			//good
		}
		
		pmf.close();
	}
	
	@Test
	public void testClosedTransaction() {
		Properties props = new ZooJdoProperties(DB_NAME);
		pmf = JDOHelper.getPersistenceManagerFactory(props);
		pm = pmf.getPersistenceManager();

		pm.close();
		pmf.close();

		
		try {
			pm.currentTransaction();
			fail();
		} catch (JDOFatalUserException e) {
			//good
		}

		try {
			pm.makePersistent(new TestClass());
			fail();
		} catch (JDOFatalUserException e) {
			//good
		}
		
		//TODO
		System.out.println("TODO check others on closed PM");
	}

	
	@After
	public void afterTest() {
		if (pm != null && !pm.isClosed()) {
			if (pm.currentTransaction().isActive()) {
				pm.currentTransaction().rollback();
			}
			pm.close();
		}
		pm = null;
		if (pmf != null && !pmf.isClosed()) {
			pmf.close();
		}
		pmf = null;
	}
	
	@AfterClass
	public static void tearDown() {
		TestTools.removeDb(DB_NAME);
	}
}
