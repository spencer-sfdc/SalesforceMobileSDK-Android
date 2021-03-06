/*
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.androidsdk.store;

import net.sqlcipher.database.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.os.SystemClock;

import com.salesforce.androidsdk.smartstore.store.AlterSoupLongOperation;
import com.salesforce.androidsdk.smartstore.store.AlterSoupLongOperation.AlterSoupStep;
import com.salesforce.androidsdk.smartstore.store.DBHelper;
import com.salesforce.androidsdk.smartstore.store.IndexSpec;
import com.salesforce.androidsdk.smartstore.store.LongOperation;
import com.salesforce.androidsdk.smartstore.store.QuerySpec;
import com.salesforce.androidsdk.smartstore.store.QuerySpec.Order;
import com.salesforce.androidsdk.smartstore.store.SmartSqlHelper.SmartSqlException;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.smartstore.store.SmartStore.Type;

/**
 * Abstract super class for plain and encrypted smart store tests
 *
 */
public abstract class AbstractSmartStoreTest extends SmartStoreTestCase {

	private static final String TEST_SOUP = "test_soup";
	private static final String OTHER_TEST_SOUP = "other_test_soup";
	private static final String THIRD_TEST_SOUP = "third_test_soup";
	private static final String FOURTH_TEST_SOUP = "fourth_test_soup";

	@Override
	public void setUp() throws Exception {
		super.setUp();
		assertFalse("Table for test_soup should not exist", hasTable("TABLE_1"));
		assertFalse("Soup test_soup should not exist", store.hasSoup(TEST_SOUP));
		store.registerSoup(TEST_SOUP, new IndexSpec[] {new IndexSpec("key", Type.string)});
		assertEquals("Table for test_soup was expected to be called TABLE_1", "TABLE_1", getSoupTableName(TEST_SOUP));
		assertTrue("Table for test_soup should now exist", hasTable("TABLE_1"));
		assertTrue("Soup test_soup should now exist", store.hasSoup(TEST_SOUP));
	}
	
	protected abstract SQLiteDatabase getWritableDatabase();

	/**
	 * Testing method with paths to top level string/integer/array/map as well as edge cases (null object/null or empty path)
	 * @throws JSONException 
	 */
	public void testProjectTopLevel() throws JSONException {
		JSONObject json = new JSONObject("{'a':'va', 'b':2, 'c':[0,1,2], 'd': {'d1':'vd1', 'd2':'vd2', 'd3':[1,2], 'd4':{'e':5}}}");

		// Null object
		assertNull("Should have been null", SmartStore.project(null, "path"));
		
		// Root
		assertSameJSON("Should have returned whole object", json, SmartStore.project(json, null));
		assertSameJSON("Should have returned whole object", json, SmartStore.project(json, ""));
		
		// Top-level elements
		assertEquals("Wrong value for key a", "va", SmartStore.project(json, "a"));
		assertEquals("Wrong value for key b", 2, SmartStore.project(json, "b"));
		assertSameJSON("Wrong value for key c", new JSONArray("[0,1,2]"), SmartStore.project(json, "c"));
		assertSameJSON("Wrong value for key d", new JSONObject("{'d1':'vd1','d2':'vd2','d3':[1,2],'d4':{'e':5}}"), (JSONObject) SmartStore.project(json, "d"));
	}

	/**
	 * Testing method with paths to non-top level string/integer/array/map
	 * @throws JSONException 
	 */
	public void testProjectNested() throws JSONException {
		JSONObject json = new JSONObject("{'a':'va', 'b':2, 'c':[0,1,2], 'd': {'d1':'vd1', 'd2':'vd2', 'd3':[1,2], 'd4':{'e':5}}}");
		
		// Nested elements
		assertEquals("Wrong value for key d.d1", "vd1", SmartStore.project(json, "d.d1"));
		assertEquals("Wrong value for key d.d2", "vd2", SmartStore.project(json, "d.d2"));
		assertSameJSON("Wrong value for key d.d3", new JSONArray("[1,2]"), SmartStore.project(json, "d.d3"));
		assertSameJSON("Wrong value for key d.d4", new JSONObject("{'e':5}"), SmartStore.project(json, "d.d4"));
		assertEquals("Wrong value for key d.d4.e", 5, SmartStore.project(json, "d.d4.e"));
	}

	/**
	 * Check that the meta data table (soup index map) has been created
	 */
	public void testMetaDataTableCreated() {
		assertTrue("Table soup_index_map not found", hasTable("soup_index_map"));
	}

	/**
	 * Test register/drop soup
	 */
	public void testRegisterDropSoup() {
		// Before
		assertNull("getSoupTableName should have returned null", getSoupTableName(THIRD_TEST_SOUP));
		assertFalse("Soup third_test_soup should not exist", store.hasSoup(THIRD_TEST_SOUP));
		
		// Register
		store.registerSoup(THIRD_TEST_SOUP, new IndexSpec[] {new IndexSpec("key", Type.string), new IndexSpec("value", Type.string)});
		String soupTableName = getSoupTableName(THIRD_TEST_SOUP);
		assertEquals("getSoupTableName should have returned TABLE_2", "TABLE_2", soupTableName);
		assertTrue("Table for soup third_test_soup does exist", hasTable(soupTableName));
		assertTrue("Register soup call failed", store.hasSoup(THIRD_TEST_SOUP));
		
		// Drop
		store.dropSoup(THIRD_TEST_SOUP);
		
		// After
		assertFalse("Soup third_test_soup should no longer exist", store.hasSoup(THIRD_TEST_SOUP));
		assertNull("getSoupTableName should have returned null", getSoupTableName(THIRD_TEST_SOUP));
		assertFalse("Table for soup third_test_soup does exist", hasTable(soupTableName));
	}

	/**
	 * Testing getAllSoupNames: register a new soup and then drop it and call getAllSoupNames before and after
	 */
	public void testGetAllSoupNames() {
		// Before
		assertEquals("One soup name expected", 1, store.getAllSoupNames().size());
		assertTrue(TEST_SOUP + " should have been returned by getAllSoupNames", store.getAllSoupNames().contains(TEST_SOUP));

		// Register another soup
		store.registerSoup(THIRD_TEST_SOUP, new IndexSpec[] {new IndexSpec("key", Type.string), new IndexSpec("value", Type.string)});
		assertEquals("Two soup names expected", 2, store.getAllSoupNames().size());
		assertTrue(TEST_SOUP + " should have been returned by getAllSoupNames", store.getAllSoupNames().contains(TEST_SOUP));
		assertTrue(THIRD_TEST_SOUP + " should have been returned by getAllSoupNames", store.getAllSoupNames().contains(THIRD_TEST_SOUP));

		// Drop the latest soup
		store.dropSoup(THIRD_TEST_SOUP);
		assertEquals("One soup name expected", 1, store.getAllSoupNames().size());
		assertTrue(TEST_SOUP + " should have been returned by getAllSoupNames", store.getAllSoupNames().contains(TEST_SOUP));
	}
	
	/**
	 * Testing dropAllSoups: register a couple of soups then drop them all
	 */
	public void testDropAllSoups() {
		// Register another soup
		assertEquals("One soup name expected", 1, store.getAllSoupNames().size());
		store.registerSoup(THIRD_TEST_SOUP, new IndexSpec[] {new IndexSpec("key", Type.string), new IndexSpec("value", Type.string)});
		assertEquals("Two soup names expected", 2, store.getAllSoupNames().size());

		// Drop all
		store.dropAllSoups();
		assertEquals("No soup name expected", 0, store.getAllSoupNames().size());
		assertFalse("Soup " + THIRD_TEST_SOUP + " should no longer exist", store.hasSoup(THIRD_TEST_SOUP));
		assertFalse("Soup " + TEST_SOUP + " should no longer exist", store.hasSoup(TEST_SOUP));
	}
	
	
	/**
	 * Testing create: create a single element with a single index pointing to a top level attribute
	 * @throws JSONException 
	 */
	public void testCreateOne() throws JSONException {
		JSONObject soupElt = new JSONObject("{'key':'ka', 'value':'va'}");
		JSONObject soupEltCreated = store.create(TEST_SOUP, soupElt);
		
		// Check DB
		Cursor c = null;
		try {
			String soupTableName = getSoupTableName(TEST_SOUP);
			c = DBHelper.getInstance(db).query(db, soupTableName, null, null, null, null);
			assertTrue("Expected a soup element", c.moveToFirst());
			assertEquals("Expected one soup element only", 1, c.getCount());
			assertEquals("Wrong id", idOf(soupEltCreated), c.getLong(c.getColumnIndex("id")));
			assertEquals("Wrong created date", soupEltCreated.getLong(SmartStore.SOUP_LAST_MODIFIED_DATE), c.getLong(c.getColumnIndex("lastModified")));
			assertEquals("Wrong value in index column", "ka", c.getString(c.getColumnIndex(soupTableName + "_0")));
			assertSameJSON("Wrong value in soup column", soupEltCreated, new JSONObject(c.getString(c.getColumnIndex("soup"))));
			assertEquals("Created date and last modified date should be equal", c.getLong(c.getColumnIndex("created")),  c.getLong(c.getColumnIndex("lastModified")));
		}
		finally {
			safeClose(c);
		}
	}

	/**
	 * Testing create: create multiple elements with multiple indices not just pointing to top level attributes 
	 * @throws JSONException 
	 */
	public void testCreateMultiple() throws JSONException {
		assertFalse("Soup other_test_soup should not exist", store.hasSoup(OTHER_TEST_SOUP));
		store.registerSoup(OTHER_TEST_SOUP, new IndexSpec[] {new IndexSpec("lastName", Type.string), new IndexSpec("address.city", Type.string)});
		assertTrue("Register soup call failed", store.hasSoup(OTHER_TEST_SOUP));

		JSONObject soupElt1 = new JSONObject("{'lastName':'Doe', 'address':{'city':'San Francisco','street':'1 market'}}");
		JSONObject soupElt2 = new JSONObject("{'lastName':'Jackson', 'address':{'city':'Los Angeles','street':'100 mission'}}");
		JSONObject soupElt3 = new JSONObject("{'lastName':'Watson', 'address':{'city':'London','street':'50 market'}}");

		JSONObject soupElt1Created = store.create(OTHER_TEST_SOUP, soupElt1);
		JSONObject soupElt2Created = store.create(OTHER_TEST_SOUP, soupElt2);
		JSONObject soupElt3Created = store.create(OTHER_TEST_SOUP, soupElt3);

		// Check DB
		Cursor c = null;
		try {
			String soupTableName = getSoupTableName(OTHER_TEST_SOUP);
			assertEquals("Table for other_test_soup was expected to be called TABLE_2", "TABLE_2", soupTableName);
			assertTrue("Table for other_test_soup should now exist", hasTable("TABLE_2"));
			
			c = DBHelper.getInstance(db).query(db, soupTableName, null, "id ASC", null, null);
			assertTrue("Expected a soup element", c.moveToFirst());
			assertEquals("Expected three soup elements", 3, c.getCount());
			
			assertEquals("Wrong id", idOf(soupElt1Created), c.getLong(c.getColumnIndex("id")));
			assertEquals("Wrong created date", soupElt1Created.getLong(SmartStore.SOUP_LAST_MODIFIED_DATE), c.getLong(c.getColumnIndex("lastModified")));
			assertEquals("Wrong value in index column", "Doe", c.getString(c.getColumnIndex(soupTableName + "_0")));
			assertEquals("Wrong value in index column", "San Francisco", c.getString(c.getColumnIndex(soupTableName + "_1")));
			assertSameJSON("Wrong value in soup column", soupElt1Created, new JSONObject(c.getString(c.getColumnIndex("soup"))));
			
			c.moveToNext();
			assertEquals("Wrong id", idOf(soupElt2Created), c.getLong(c.getColumnIndex("id")));
			assertEquals("Wrong created date", soupElt2Created.getLong(SmartStore.SOUP_LAST_MODIFIED_DATE), c.getLong(c.getColumnIndex("lastModified")));
			assertEquals("Wrong value in index column", "Jackson", c.getString(c.getColumnIndex(soupTableName + "_0")));
			assertEquals("Wrong value in index column", "Los Angeles", c.getString(c.getColumnIndex(soupTableName + "_1")));
			assertSameJSON("Wrong value in soup column", soupElt2Created, new JSONObject(c.getString(c.getColumnIndex("soup"))));

			c.moveToNext();
			assertEquals("Wrong id", idOf(soupElt3Created), c.getLong(c.getColumnIndex("id")));
			assertEquals("Wrong created date", soupElt3Created.getLong(SmartStore.SOUP_LAST_MODIFIED_DATE), c.getLong(c.getColumnIndex("lastModified")));
			assertEquals("Wrong value in index column", "Watson", c.getString(c.getColumnIndex(soupTableName + "_0")));
			assertEquals("Wrong value in index column", "London", c.getString(c.getColumnIndex(soupTableName + "_1")));
			assertSameJSON("Wrong value in soup column", soupElt3Created, new JSONObject(c.getString(c.getColumnIndex("soup"))));
			
		}
		finally {
			safeClose(c);
		}
	}

	/**
	 * Testing update: create multiple soup elements and update one of them, check them all
	 * @throws JSONException 
	 */
	public void testUpdate() throws JSONException {
		JSONObject soupElt1 = new JSONObject("{'key':'ka1', 'value':'va1'}");
		JSONObject soupElt2 = new JSONObject("{'key':'ka2', 'value':'va2'}");
		JSONObject soupElt3 = new JSONObject("{'key':'ka3', 'value':'va3'}");
		
		JSONObject soupElt1Created = store.create(TEST_SOUP, soupElt1);
		JSONObject soupElt2Created = store.create(TEST_SOUP, soupElt2);
		JSONObject soupElt3Created = store.create(TEST_SOUP, soupElt3);
		
		SystemClock.sleep(10); // to get a different last modified date
		JSONObject soupElt2ForUpdate = new JSONObject("{'key':'ka2u', 'value':'va2u'}");
		JSONObject soupElt2Updated = store.update(TEST_SOUP, soupElt2ForUpdate, idOf(soupElt2Created));
		
		JSONObject soupElt1Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt1Created)).getJSONObject(0);
		JSONObject soupElt2Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt2Created)).getJSONObject(0);
		JSONObject soupElt3Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt3Created)).getJSONObject(0);

		assertSameJSON("Retrieve mismatch", soupElt1Created, soupElt1Retrieved);
		assertSameJSON("Retrieve mismatch", soupElt2Updated, soupElt2Retrieved);
		assertSameJSON("Retrieve mismatch", soupElt3Created, soupElt3Retrieved);

		// Check DB
		Cursor c = null;
		try {
			String soupTableName = getSoupTableName(TEST_SOUP);			
			c = DBHelper.getInstance(db).query(db, soupTableName, null, "id ASC", null, null);
			assertTrue("Expected a soup element", c.moveToFirst());
			assertEquals("Expected three soup elements", 3, c.getCount());
			
			assertEquals("Wrong id", idOf(soupElt1Created), c.getLong(c.getColumnIndex("id")));
			assertEquals("Wrong created date", soupElt1Created.getLong(SmartStore.SOUP_LAST_MODIFIED_DATE), c.getLong(c.getColumnIndex("lastModified")));
			assertEquals("Created date and last modified date should be equal", c.getLong(c.getColumnIndex("created")),  c.getLong(c.getColumnIndex("lastModified")));				
			
			c.moveToNext();
			assertEquals("Wrong id", idOf(soupElt2Created), c.getLong(c.getColumnIndex("id")));
			assertEquals("Wrong created date", soupElt2Updated.getLong(SmartStore.SOUP_LAST_MODIFIED_DATE), c.getLong(c.getColumnIndex("lastModified")));
			assertTrue("Last modified date should be more recent than created date", c.getLong(c.getColumnIndex("created")) < c.getLong(c.getColumnIndex("lastModified")));				

			c.moveToNext();
			assertEquals("Wrong id", idOf(soupElt3Created), c.getLong(c.getColumnIndex("id")));
			assertEquals("Wrong created date", soupElt3Created.getLong(SmartStore.SOUP_LAST_MODIFIED_DATE), c.getLong(c.getColumnIndex("lastModified")));
			assertEquals("Created date and last modified date should be equal", c.getLong(c.getColumnIndex("created")),  c.getLong(c.getColumnIndex("lastModified")));				
		}
		finally {
			safeClose(c);
		}
	}
	
	/**
	 * Testing upsert: upsert multiple soup elements and re-upsert one of them, check them all
	 * @throws JSONException
	 */
	public void testUpsert() throws JSONException {
		JSONObject soupElt1 = new JSONObject("{'key':'ka1', 'value':'va1'}");
		JSONObject soupElt2 = new JSONObject("{'key':'ka2', 'value':'va2'}");
		JSONObject soupElt3 = new JSONObject("{'key':'ka3', 'value':'va3'}");
		
		JSONObject soupElt1Upserted = store.upsert(TEST_SOUP, soupElt1);
		JSONObject soupElt2Upserted = store.upsert(TEST_SOUP, soupElt2);
		JSONObject soupElt3Upserted = store.upsert(TEST_SOUP, soupElt3);

		SystemClock.sleep(10); // to get a different last modified date
		JSONObject soupElt2ForUpdate = new JSONObject("{'key':'ka2u', 'value':'va2u', '_soupEntryId': " + idOf(soupElt2Upserted) + "}");
		JSONObject soupElt2Updated = store.upsert(TEST_SOUP, soupElt2ForUpdate);
		
		JSONObject soupElt1Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt1Upserted)).getJSONObject(0);
		JSONObject soupElt2Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt2Upserted)).getJSONObject(0);
		JSONObject soupElt3Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt3Upserted)).getJSONObject(0);

		assertSameJSON("Retrieve mismatch", soupElt1Upserted, soupElt1Retrieved);
		assertSameJSON("Retrieve mismatch", soupElt2Updated, soupElt2Retrieved);
		assertSameJSON("Retrieve mismatch", soupElt3Upserted, soupElt3Retrieved);
		
		// Check DB
		Cursor c = null;
		try {
			String soupTableName = getSoupTableName(TEST_SOUP);			
			c = DBHelper.getInstance(db).query(db, soupTableName, null, "id ASC", null, null);
			assertTrue("Expected a soup element", c.moveToFirst());
			assertEquals("Expected three soup elements", 3, c.getCount());
			
			assertEquals("Wrong id", idOf(soupElt1Upserted), c.getLong(c.getColumnIndex("id")));
			assertEquals("Wrong created date", soupElt1Upserted.getLong(SmartStore.SOUP_LAST_MODIFIED_DATE), c.getLong(c.getColumnIndex("lastModified")));
			assertEquals("Created date and last modified date should be equal", c.getLong(c.getColumnIndex("created")),  c.getLong(c.getColumnIndex("lastModified")));				
			
			c.moveToNext();
			assertEquals("Wrong id", idOf(soupElt2Upserted), c.getLong(c.getColumnIndex("id")));
			assertEquals("Wrong created date", soupElt2Updated.getLong(SmartStore.SOUP_LAST_MODIFIED_DATE), c.getLong(c.getColumnIndex("lastModified")));
			assertTrue("Last modified date should be more recent than created date", c.getLong(c.getColumnIndex("created")) < c.getLong(c.getColumnIndex("lastModified")));				

			c.moveToNext();
			assertEquals("Wrong id", idOf(soupElt3Upserted), c.getLong(c.getColumnIndex("id")));
			assertEquals("Wrong created date", soupElt3Upserted.getLong(SmartStore.SOUP_LAST_MODIFIED_DATE), c.getLong(c.getColumnIndex("lastModified")));
			assertEquals("Created date and last modified date should be equal", c.getLong(c.getColumnIndex("created")),  c.getLong(c.getColumnIndex("lastModified")));				
		}
		finally {
			safeClose(c);
		}
	}

	/**
	 * Testing upsert with external id: upsert multiple soup elements and re-upsert one of them, check them all
	 * @throws JSONException
	 */
	public void testUpsertWithExternalId() throws JSONException {
		JSONObject soupElt1 = new JSONObject("{'key':'ka1', 'value':'va1'}");
		JSONObject soupElt2 = new JSONObject("{'key':'ka2', 'value':'va2'}");
		JSONObject soupElt3 = new JSONObject("{'key':'ka3', 'value':'va3'}");
		
		JSONObject soupElt1Upserted = store.upsert(TEST_SOUP, soupElt1, "key");
		JSONObject soupElt2Upserted = store.upsert(TEST_SOUP, soupElt2, "key");
		JSONObject soupElt3Upserted = store.upsert(TEST_SOUP, soupElt3, "key");

		SystemClock.sleep(10); // to get a different last modified date
		JSONObject soupElt2ForUpdate = new JSONObject("{'key':'ka2', 'value':'va2u'}");
		JSONObject soupElt2Updated = store.upsert(TEST_SOUP, soupElt2ForUpdate, "key");
		
		JSONObject soupElt1Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt1Upserted)).getJSONObject(0);
		JSONObject soupElt2Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt2Upserted)).getJSONObject(0);
		JSONObject soupElt3Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt3Upserted)).getJSONObject(0);

		assertSameJSON("Retrieve mismatch", soupElt1Upserted, soupElt1Retrieved);
		assertSameJSON("Retrieve mismatch", soupElt2Updated, soupElt2Retrieved);
		assertSameJSON("Retrieve mismatch", soupElt3Upserted, soupElt3Retrieved);

		// Check DB
		Cursor c = null;
		try {
			String soupTableName = getSoupTableName(TEST_SOUP);			
			c = DBHelper.getInstance(db).query(db, soupTableName, null, "id ASC", null, null);
			assertTrue("Expected a soup element", c.moveToFirst());
			assertEquals("Expected three soup elements", 3, c.getCount());
			
			assertEquals("Wrong id", idOf(soupElt1Upserted), c.getLong(c.getColumnIndex("id")));
			assertEquals("Wrong created date", soupElt1Upserted.getLong(SmartStore.SOUP_LAST_MODIFIED_DATE), c.getLong(c.getColumnIndex("lastModified")));
			assertEquals("Created date and last modified date should be equal", c.getLong(c.getColumnIndex("created")),  c.getLong(c.getColumnIndex("lastModified")));				
			
			c.moveToNext();
			assertEquals("Wrong id", idOf(soupElt2Upserted), c.getLong(c.getColumnIndex("id")));
			assertEquals("Wrong created date", soupElt2Updated.getLong(SmartStore.SOUP_LAST_MODIFIED_DATE), c.getLong(c.getColumnIndex("lastModified")));
			assertTrue("Last modified date should be more recent than created date", c.getLong(c.getColumnIndex("created")) < c.getLong(c.getColumnIndex("lastModified")));				

			c.moveToNext();
			assertEquals("Wrong id", idOf(soupElt3Upserted), c.getLong(c.getColumnIndex("id")));
			assertEquals("Wrong created date", soupElt3Upserted.getLong(SmartStore.SOUP_LAST_MODIFIED_DATE), c.getLong(c.getColumnIndex("lastModified")));
			assertEquals("Created date and last modified date should be equal", c.getLong(c.getColumnIndex("created")),  c.getLong(c.getColumnIndex("lastModified")));				
		}
		finally {
			safeClose(c);
		}
	}
	
	
	/**
	 * Testing upsert passing a non-indexed path for the external id (should fail)
	 * @throws JSONException
	 */
	public void testUpsertWithNonIndexedExternalId() throws JSONException {
		JSONObject soupElt = new JSONObject("{'key':'ka1', 'value':'va1'}");
		
		try {
			store.upsert(TEST_SOUP, soupElt, "value");
			fail("Exception was expected: value is not an indexed field");
		}
		catch (RuntimeException e) {
			assertTrue("Wrong exception", e.getMessage().contains("does not have an index"));
		}
	}

	/**
	 * Testing upsert with an external id that is not unique in the soup
	 * @throws JSONException
	 */
	public void testUpsertWithNonUniqueExternalId() throws JSONException {
		JSONObject soupElt1 = new JSONObject("{'key':'ka', 'value':'va1'}");
		JSONObject soupElt2 = new JSONObject("{'key':'ka', 'value':'va2'}");
		JSONObject soupElt3 = new JSONObject("{'key':'ka', 'value':'va3'}");
		
		JSONObject soupElt1Upserted = store.upsert(TEST_SOUP, soupElt1);
		JSONObject soupElt2Upserted = store.upsert(TEST_SOUP, soupElt2);

		JSONObject soupElt1Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt1Upserted)).getJSONObject(0);
		JSONObject soupElt2Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt2Upserted)).getJSONObject(0);

		assertSameJSON("Retrieve mismatch", soupElt1Upserted, soupElt1Retrieved);
		assertSameJSON("Retrieve mismatch", soupElt2Upserted, soupElt2Retrieved);
		
		try {
			store.upsert(TEST_SOUP, soupElt3, "key");
			fail("Exception was expected: key is not unique in the soup");
		}
		catch (RuntimeException e) {
			assertTrue("Wrong exception", e.getMessage().contains("are more than one soup elements"));
		}
	}
	
	/**
	 * Testing retrieve: create multiple soup elements and retrieves them back
	 * @throws JSONException 
	 */
	public void testRetrieve() throws JSONException {
		JSONObject soupElt1 = new JSONObject("{'key':'ka1', 'value':'va1'}");
		JSONObject soupElt2 = new JSONObject("{'key':'ka2', 'value':'va2'}");
		JSONObject soupElt3 = new JSONObject("{'key':'ka3', 'value':'va3'}");
		
		JSONObject soupElt1Created = store.create(TEST_SOUP, soupElt1);
		JSONObject soupElt2Created = store.create(TEST_SOUP, soupElt2);
		JSONObject soupElt3Created = store.create(TEST_SOUP, soupElt3);
		
		JSONObject soupElt1Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt1Created)).getJSONObject(0);
		JSONObject soupElt2Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt2Created)).getJSONObject(0);
		JSONObject soupElt3Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt3Created)).getJSONObject(0);

		assertSameJSON("Retrieve mismatch", soupElt1Created, soupElt1Retrieved);
		assertSameJSON("Retrieve mismatch", soupElt2Created, soupElt2Retrieved);
		assertSameJSON("Retrieve mismatch", soupElt3Created, soupElt3Retrieved);
	}

	/**
	 * Testing delete: create a soup element, deletes and check database directly that it is in fact gone
	 * @throws JSONException 
	 */
	public void testDelete() throws JSONException {
		JSONObject soupElt1 = new JSONObject("{'key':'ka1', 'value':'va1'}");
		JSONObject soupElt2 = new JSONObject("{'key':'ka2', 'value':'va2'}");
		JSONObject soupElt3 = new JSONObject("{'key':'ka3', 'value':'va3'}");
		
		JSONObject soupElt1Created = store.create(TEST_SOUP, soupElt1);
		JSONObject soupElt2Created = store.create(TEST_SOUP, soupElt2);
		JSONObject soupElt3Created = store.create(TEST_SOUP, soupElt3);
		
		store.delete(TEST_SOUP, idOf(soupElt2Created));

		JSONObject soupElt1Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt1Created)).getJSONObject(0);
		JSONArray soupElt2Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt2Created));
		JSONObject soupElt3Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt3Created)).getJSONObject(0);

		assertSameJSON("Retrieve mismatch", soupElt1Created, soupElt1Retrieved);
		assertEquals("Should be empty", 0, soupElt2Retrieved.length());
		assertSameJSON("Retrieve mismatch", soupElt3Created, soupElt3Retrieved);
		
		// Check DB
		Cursor c = null;
		try {
			String soupTableName = getSoupTableName(TEST_SOUP);
			c = DBHelper.getInstance(db).query(db, soupTableName, null, "id ASC", null, null);
			assertTrue("Expected a soup element", c.moveToFirst());
			assertEquals("Expected three soup elements", 2, c.getCount());
			
			assertEquals("Wrong id", idOf(soupElt1Created), c.getLong(c.getColumnIndex("id")));
			
			c.moveToNext();
			assertEquals("Wrong id", idOf(soupElt3Created), c.getLong(c.getColumnIndex("id")));
		}
		finally {
			safeClose(c);
		}
	}
	
	/**
	 * Testing clear soup: create soup elements, clear soup and check database directly that there are in fact gone
	 * @throws JSONException 
	 */
	public void testClearSoup() throws JSONException {
		JSONObject soupElt1 = new JSONObject("{'key':'ka1', 'value':'va1'}");
		JSONObject soupElt2 = new JSONObject("{'key':'ka2', 'value':'va2'}");
		JSONObject soupElt3 = new JSONObject("{'key':'ka3', 'value':'va3'}");
		
		JSONObject soupElt1Created = store.create(TEST_SOUP, soupElt1);
		JSONObject soupElt2Created = store.create(TEST_SOUP, soupElt2);
		JSONObject soupElt3Created = store.create(TEST_SOUP, soupElt3);
		
		store.clearSoup(TEST_SOUP);

		JSONArray soupElt1Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt1Created));
		JSONArray soupElt2Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt2Created));
		JSONArray soupElt3Retrieved = store.retrieve(TEST_SOUP, idOf(soupElt3Created));

		assertEquals("Should be empty", 0, soupElt1Retrieved.length());
		assertEquals("Should be empty", 0, soupElt2Retrieved.length());
		assertEquals("Should be empty", 0, soupElt3Retrieved.length());

		// Check DB
		Cursor c = null;
		try {
			String soupTableName = getSoupTableName(TEST_SOUP);
			c = DBHelper.getInstance(db).query(db, soupTableName, null, "id ASC", null, null);
			assertFalse("Expected no soup element", c.moveToFirst());
		}
		finally {
			safeClose(c);
		}
	}
	
	/**
	 * Test query when looking for all elements
	 * @throws JSONException 
	 */
	public void testAllQuery() throws JSONException {
		JSONObject soupElt1 = new JSONObject("{'key':'ka1', 'value':'va1', 'otherValue':'ova1'}");
		JSONObject soupElt2 = new JSONObject("{'key':'ka2', 'value':'va2', 'otherValue':'ova2'}");
		JSONObject soupElt3 = new JSONObject("{'key':'ka3', 'value':'va3', 'otherValue':'ova3'}");
		
		JSONObject soupElt1Created = store.create(TEST_SOUP, soupElt1);
		JSONObject soupElt2Created = store.create(TEST_SOUP, soupElt2);
		JSONObject soupElt3Created = store.create(TEST_SOUP, soupElt3);

		// Query all - small page
		JSONArray result = store.query(QuerySpec.buildAllQuerySpec(TEST_SOUP, "key", Order.ascending, 2), 0);
		assertEquals("Two elements expected", 2, result.length());
		assertSameJSON("Wrong result for query", soupElt1Created, result.getJSONObject(0));
		assertSameJSON("Wrong result for query", soupElt2Created, result.getJSONObject(1));

		// Query all - next small page
		result = store.query(QuerySpec.buildAllQuerySpec(TEST_SOUP, "key", Order.ascending, 2), 1);
		assertEquals("One element expected", 1, result.length());
		assertSameJSON("Wrong result for query", soupElt3Created, result.getJSONObject(0));

		// Query all - large page
		result = store.query(QuerySpec.buildAllQuerySpec(TEST_SOUP, "key", Order.ascending, 10), 0);
		assertEquals("Three elements expected", 3, result.length());
		assertSameJSON("Wrong result for query", soupElt1Created, result.getJSONObject(0));
		assertSameJSON("Wrong result for query", soupElt2Created, result.getJSONObject(1));
		assertSameJSON("Wrong result for query", soupElt3Created, result.getJSONObject(2));
	
	}
	
	/**
	 * Test query when looking for a specific element
	 * @throws JSONException 
	 */
	public void testMatchQuery() throws JSONException {
		JSONObject soupElt1 = new JSONObject("{'key':'ka1', 'value':'va1', 'otherValue':'ova1'}");
		JSONObject soupElt2 = new JSONObject("{'key':'ka2', 'value':'va2', 'otherValue':'ova2'}");
		JSONObject soupElt3 = new JSONObject("{'key':'ka3', 'value':'va3', 'otherValue':'ova3'}");
		
		store.create(TEST_SOUP, soupElt1);
		JSONObject soupElt2Created= store.create(TEST_SOUP, soupElt2);
		store.create(TEST_SOUP, soupElt3);

		// Exact match
		JSONArray result = store.query(QuerySpec.buildExactQuerySpec(TEST_SOUP, "key", "ka2", 10), 0);
		assertEquals("One result expected", 1, result.length());
		assertSameJSON("Wrong result for query", soupElt2Created, result.getJSONObject(0));

	}

	/**
	 * Query test looking for a range of elements (with ascending or descending ordering)
	 * @throws JSONException 
	 */
	public void testRangeQuery() throws JSONException {
		JSONObject soupElt1 = new JSONObject("{'key':'ka1', 'value':'va1', 'otherValue':'ova1'}");
		JSONObject soupElt2 = new JSONObject("{'key':'ka2', 'value':'va2', 'otherValue':'ova2'}");
		JSONObject soupElt3 = new JSONObject("{'key':'ka3', 'value':'va3', 'otherValue':'ova3'}");
		
		/*JSONObjectsoupElt1Created = */store.create(TEST_SOUP, soupElt1);
		JSONObject soupElt2Created = store.create(TEST_SOUP, soupElt2);
		JSONObject soupElt3Created = store.create(TEST_SOUP, soupElt3);

		// Range query
		JSONArray result = store.query(QuerySpec.buildRangeQuerySpec(TEST_SOUP, "key", "ka2", "ka3", Order.ascending, 10), 0);
		assertEquals("Two results expected", 2, result.length());
		assertSameJSON("Wrong result for query", soupElt2Created, result.getJSONObject(0));
		assertSameJSON("Wrong result for query", soupElt3Created, result.getJSONObject(1));

		// Range query - descending order
		result = store.query(QuerySpec.buildRangeQuerySpec(TEST_SOUP, "key", "ka2", "ka3", Order.descending, 10), 0);
		assertEquals("Two results expected", 2, result.length());
		assertSameJSON("Wrong result for query", soupElt3Created, result.getJSONObject(0));
		assertSameJSON("Wrong result for query", soupElt2Created, result.getJSONObject(1));
	}

	/**
	 * Query test looking using like (with ascending or descending ordering)
	 * @throws JSONException 
	 */
	public void testLikeQuery() throws JSONException {
		JSONObject soupElt1 = new JSONObject("{'key':'abcd', 'value':'va1', 'otherValue':'ova1'}");
		JSONObject soupElt2 = new JSONObject("{'key':'bbcd', 'value':'va2', 'otherValue':'ova2'}");
		JSONObject soupElt3 = new JSONObject("{'key':'abcc', 'value':'va3', 'otherValue':'ova3'}");
		JSONObject soupElt4 = new JSONObject("{'key':'defg', 'value':'va4', 'otherValue':'ova3'}");
		
		JSONObject soupElt1Created = store.create(TEST_SOUP, soupElt1);
		JSONObject soupElt2Created = store.create(TEST_SOUP, soupElt2);
		JSONObject soupElt3Created = store.create(TEST_SOUP, soupElt3);
		/*JSONObject soupElt4Created = */ store.create(TEST_SOUP, soupElt4);

		// Like query (starts with)
		JSONArray result = store.query(QuerySpec.buildLikeQuerySpec(TEST_SOUP, "key", "abc%", Order.ascending, 10), 0);
		assertEquals("Two results expected", 2, result.length());
		assertSameJSON("Wrong result for query", soupElt3Created, result.getJSONObject(0));
		assertSameJSON("Wrong result for query", soupElt1Created, result.getJSONObject(1));

		// Like query (ends with)
		result = store.query(QuerySpec.buildLikeQuerySpec(TEST_SOUP, "key", "%bcd", Order.ascending, 10), 0);
		assertEquals("Two results expected", 2, result.length());
		assertSameJSON("Wrong result for query", soupElt1Created, result.getJSONObject(0));
		assertSameJSON("Wrong result for query", soupElt2Created, result.getJSONObject(1));

		// Like query (starts with) - descending order
		result = store.query(QuerySpec.buildLikeQuerySpec(TEST_SOUP, "key", "abc%", Order.descending, 10), 0);
		assertEquals("Two results expected", 2, result.length());
		assertSameJSON("Wrong result for query", soupElt1Created, result.getJSONObject(0));
		assertSameJSON("Wrong result for query", soupElt3Created, result.getJSONObject(1));

		// Like query (ends with) - descending order
		result = store.query(QuerySpec.buildLikeQuerySpec(TEST_SOUP, "key", "%bcd", Order.descending, 10), 0);
		assertEquals("Two results expected", 2, result.length());
		assertSameJSON("Wrong result for query", soupElt2Created, result.getJSONObject(0));
		assertSameJSON("Wrong result for query", soupElt1Created, result.getJSONObject(1));

		// Like query (contains)
		result = store.query(QuerySpec.buildLikeQuerySpec(TEST_SOUP, "key", "%bc%", Order.ascending, 10), 0);
		assertEquals("Three results expected", 3, result.length());
		assertSameJSON("Wrong result for query", soupElt3Created, result.getJSONObject(0));
		assertSameJSON("Wrong result for query", soupElt1Created, result.getJSONObject(1));
		assertSameJSON("Wrong result for query", soupElt2Created, result.getJSONObject(2));

		// Like query (contains) - descending order
		result = store.query(QuerySpec.buildLikeQuerySpec(TEST_SOUP, "key", "%bc%", Order.descending, 10), 0);
		assertEquals("Three results expected", 3, result.length());
		assertSameJSON("Wrong result for query", soupElt2Created, result.getJSONObject(0));
		assertSameJSON("Wrong result for query", soupElt1Created, result.getJSONObject(1));
		assertSameJSON("Wrong result for query", soupElt3Created, result.getJSONObject(2));
	}
	
	/**
	 * Test upsert soup element with null value in indexed field
	 * @throws JSONException 
	 */
	public void testUpsertWithNullInIndexedField() throws JSONException {
		// Before
		assertFalse("Soup third_test_soup should not exist", store.hasSoup(THIRD_TEST_SOUP));
		
		// Register
		store.registerSoup(THIRD_TEST_SOUP, new IndexSpec[] {new IndexSpec("key", Type.string), new IndexSpec("value", Type.string)});
		assertTrue("Register soup call failed", store.hasSoup(THIRD_TEST_SOUP));

		// Upsert
		JSONObject soupElt1 = new JSONObject("{'key':'ka', 'value':null}");
		JSONObject soupElt1Upserted = store.upsert(THIRD_TEST_SOUP, soupElt1);
		
		// Check
		JSONObject soupElt1Retrieved = store.retrieve(THIRD_TEST_SOUP, idOf(soupElt1Upserted)).getJSONObject(0);		
		assertSameJSON("Retrieve mismatch", soupElt1Upserted, soupElt1Retrieved);
	}

	/**
	 * Test to verify an aggregate query on floating point values.
	 *
	 * @throws JSONException
	 */
	public void testAggregateQueryOnIndexedField() throws JSONException {
		final JSONObject soupElt1 = new JSONObject("{'amount':10.2}");
		final JSONObject soupElt2 = new JSONObject("{'amount':9.9}");
		final IndexSpec[] indexSpecs = { new IndexSpec("amount", Type.floating) };
		store.registerSoup(FOURTH_TEST_SOUP, indexSpecs);
		assertTrue("Soup " + FOURTH_TEST_SOUP + " should have been created", store.hasSoup(FOURTH_TEST_SOUP));
		store.upsert(FOURTH_TEST_SOUP, soupElt1);
		store.upsert(FOURTH_TEST_SOUP, soupElt2);
		final String smartSql = "SELECT SUM({" + FOURTH_TEST_SOUP + ":amount}) FROM {" + FOURTH_TEST_SOUP + "}";
		final QuerySpec querySpec = QuerySpec.buildSmartQuerySpec(smartSql, 1);
		final JSONArray result = store.query(querySpec, 0);
		assertNotNull("Result should not be null", result);
		assertEquals("One result expected", 1, result.length());
		assertEquals("Incorrect result received", 20.1, result.getJSONArray(0).getDouble(0));
		store.dropSoup(FOURTH_TEST_SOUP);
		assertFalse("Soup " + FOURTH_TEST_SOUP + " should have been deleted", store.hasSoup(FOURTH_TEST_SOUP));
	}
	
	/**
	 * Test to verify proper indexing of integer and longs
	 */
	public void testIntegerIndexedField() throws JSONException {
		store.registerSoup(FOURTH_TEST_SOUP, new IndexSpec[] { new IndexSpec("amount", Type.integer) });
		tryNumber(Type.integer, Integer.MIN_VALUE, Integer.MIN_VALUE);
		tryNumber(Type.integer, Integer.MAX_VALUE, Integer.MAX_VALUE);
		tryNumber(Type.integer, Long.MIN_VALUE, Long.MIN_VALUE);
		tryNumber(Type.integer, Long.MIN_VALUE, Long.MIN_VALUE);
		tryNumber(Type.integer, Double.MIN_VALUE, (long) Double.MIN_VALUE);
		tryNumber(Type.integer, Double.MAX_VALUE, (long) Double.MAX_VALUE);
	}

	/**
	 * Test to verify proper indexing of doubles
	 */
	public void testFloatingIndexedField() throws JSONException {
		store.registerSoup(FOURTH_TEST_SOUP, new IndexSpec[] { new IndexSpec("amount", Type.floating) });
		tryNumber(Type.floating, Integer.MIN_VALUE, (double) Integer.MIN_VALUE);
		tryNumber(Type.floating, Integer.MAX_VALUE, (double) Integer.MAX_VALUE);
		tryNumber(Type.floating, Long.MIN_VALUE, (double) Long.MIN_VALUE);
		tryNumber(Type.floating, Long.MIN_VALUE, (double) Long.MIN_VALUE);
		tryNumber(Type.floating, Double.MIN_VALUE, Double.MIN_VALUE);
		tryNumber(Type.floating, Double.MAX_VALUE, Double.MAX_VALUE);
	}

	/**
	 * Helper method for testIntegerIndexedField and testFloatingIndexedField
	 * Insert soup element with number and check db 
	 * @param fieldType
	 * @param valuesIn
	 * @param valuesOut
	 * @throws JSONException 
	 */
	private void tryNumber(Type fieldType, Number valueIn, Number valueOut) throws JSONException {
		JSONObject elt = new JSONObject();
		elt.put("amount", valueIn);
		Long id = store.upsert(FOURTH_TEST_SOUP, elt).getLong(SmartStore.SOUP_ENTRY_ID);
		
		
		Cursor c = null;
		try {
			String soupTableName = getSoupTableName(FOURTH_TEST_SOUP);
			String amountColumnName = store.getSoupIndexSpecs(FOURTH_TEST_SOUP)[0].columnName;
			c = DBHelper.getInstance(db).query(db, soupTableName, new String[] { amountColumnName }, null, null, "id = " + id);
			assertTrue("Expected a soup element", c.moveToFirst());
			assertEquals("Expected one soup element", 1, c.getCount());
			if (fieldType == Type.integer)
				assertEquals("Not the value expected", valueOut.longValue(), c.getLong(0));
			else if (fieldType == Type.floating)
				assertEquals("Not the value expected", valueOut.doubleValue(), c.getDouble(0)); 
		}
		finally {
			safeClose(c);
		}
	}
	
	/**
	 * Test using smart sql to retrieve integer indexed fields
	 */
	public void testIntegerIndexedFieldWithSmartSql() throws JSONException {
		store.registerSoup(FOURTH_TEST_SOUP, new IndexSpec[] { new IndexSpec("amount", Type.integer) });
		tryNumberWithSmartSql(Type.integer, Integer.MIN_VALUE, Integer.MIN_VALUE);
		tryNumberWithSmartSql(Type.integer, Integer.MAX_VALUE, Integer.MAX_VALUE);
		tryNumberWithSmartSql(Type.integer, Long.MIN_VALUE, Long.MIN_VALUE);
		tryNumberWithSmartSql(Type.integer, Long.MIN_VALUE, Long.MIN_VALUE);
		tryNumberWithSmartSql(Type.integer, Double.MIN_VALUE, (long) Double.MIN_VALUE);
		tryNumberWithSmartSql(Type.integer, Double.MAX_VALUE, (long) Double.MAX_VALUE);
	}

	/**
	 * Test using smart sql to retrieve indexed fields holding doubles
	 * NB smart sql will return a long when querying a double field that contains a long
	 */
	public void testFloatingIndexedFieldWithSmartSql() throws JSONException {
		store.registerSoup(FOURTH_TEST_SOUP, new IndexSpec[] { new IndexSpec("amount", Type.floating) });
		tryNumberWithSmartSql(Type.floating, Integer.MIN_VALUE, Integer.MIN_VALUE);
		tryNumberWithSmartSql(Type.floating, Integer.MAX_VALUE, Integer.MAX_VALUE);
		tryNumberWithSmartSql(Type.floating, Long.MIN_VALUE, Long.MIN_VALUE);
		tryNumberWithSmartSql(Type.floating, Long.MIN_VALUE, Long.MIN_VALUE);
		tryNumberWithSmartSql(Type.floating, Double.MIN_VALUE, Double.MIN_VALUE);
		tryNumberWithSmartSql(Type.floating, Double.MAX_VALUE, Double.MAX_VALUE);
	}

	/**
	 * Helper method for testIntegerIndexedFieldWithSmartSql and testFloatingIndexedFieldWithSmartSql
	 * Insert soup element with number and retrieve it back using smartsql
	 * @param fieldType
	 * @param valuesIn
	 * @param valuesOut
	 * @throws JSONException 
	 */
	private void tryNumberWithSmartSql(Type fieldType, Number valueIn, Number valueOut) throws JSONException {
		String smartSql = "SELECT {" + FOURTH_TEST_SOUP + ":amount} FROM {" + FOURTH_TEST_SOUP + "} WHERE {" + FOURTH_TEST_SOUP + ":_soupEntryId} = ";
		JSONObject elt = new JSONObject();
		elt.put("amount", valueIn);
		Long id = store.upsert(FOURTH_TEST_SOUP, elt).getLong(SmartStore.SOUP_ENTRY_ID);
		
		Number actualValueOut = (Number) store.query(QuerySpec.buildSmartQuerySpec(smartSql + id, 1), 0).getJSONArray(0).get(0);
		if (fieldType == Type.integer)
			assertEquals("Not the value expected", valueOut.longValue(), actualValueOut.longValue());
		else if (fieldType == Type.floating)
			assertEquals("Not the value expected", valueOut.doubleValue(), actualValueOut.doubleValue()); 
	}

	/**
	 * Test for getDatabaseSize
	 * 
	 * @throws JSONException
	 */
	public void testGetDatabaseSize() throws JSONException {
		int initialSize = store.getDatabaseSize();
		for (int i=0; i<100; i++) {
			JSONObject soupElt = new JSONObject("{'key':'abcd" + i + "', 'value':'va" + i + "', 'otherValue':'ova" + i + "'}");
			store.create(TEST_SOUP, soupElt);
		}
		assertTrue("Database should be larger now", store.getDatabaseSize() > initialSize);
	}
	
	/**
	 * Test for getSoupIndexSpecs
	 * 
	 * @throws JSONException
	 */
	public void testGetSoupIndexSpecs() throws JSONException {
		IndexSpec[] indexSpecs = new IndexSpec[] {new IndexSpec("lastName", Type.string), new IndexSpec("address.city", Type.string)};
		
		assertFalse("Soup other_test_soup should not exist", store.hasSoup(OTHER_TEST_SOUP));
		store.registerSoup(OTHER_TEST_SOUP, indexSpecs);
		assertTrue("Register soup call failed", store.hasSoup(OTHER_TEST_SOUP));

		checkIndexSpecs(indexSpecs);
	}
	
	/**
	 * Test for alterSoup with reIndexData = false
	 * 
	 * @throws JSONException
	 */
	public void testAlterSoupNoReIndexing() throws JSONException {
		alterSoupHelper(false);
	}
	
	/**
	 * Test for alterSoup with reIndexData = true
	 * 
	 * @throws JSONException
	 */
	public void testAlterSoupWithReIndexing() throws JSONException {
		alterSoupHelper(true);
	}

	/**
	 * Test for alterSoup with column type change
	 * 
	 * throws JSONException
	 */
	public void testAlterSoupTypeChange() throws JSONException {
		IndexSpec[] indexSpecs = new IndexSpec[] {new IndexSpec("name", Type.string), new IndexSpec("population", Type.string)};
		
		assertFalse("Soup other_test_soup should not exist", store.hasSoup(OTHER_TEST_SOUP));
		store.registerSoup(OTHER_TEST_SOUP, indexSpecs);
		assertTrue("Register soup call failed", store.hasSoup(OTHER_TEST_SOUP));

		JSONObject soupElt1 = new JSONObject("{'name': 'San Francisco', 'population': 825863}");
		JSONObject soupElt2 = new JSONObject("{'name': 'Paris', 'population': 2234105}");

		store.create(OTHER_TEST_SOUP, soupElt1);
		store.create(OTHER_TEST_SOUP, soupElt2);

		// Query all sorted by population ascending - we should get Paris first because we indexed population as a string
		JSONArray results = store.query(QuerySpec.buildAllQuerySpec(OTHER_TEST_SOUP, "population", Order.ascending, 2), 0);
		assertEquals("Paris should be first", "Paris", results.getJSONObject(0).get("name"));
		assertEquals("San Francisco should be second", "San Francisco", results.getJSONObject(1).get("name"));

		// Alter soup - index population as integer
		IndexSpec[] indexSpecsNew = new IndexSpec[] {new IndexSpec("name", Type.string), new IndexSpec("population", Type.integer)};
		store.alterSoup(OTHER_TEST_SOUP, indexSpecsNew, true);

		// Query all sorted by population ascending - we should get San Francisco first because we indexed population as an integer
		JSONArray results2 = store.query(QuerySpec.buildAllQuerySpec(OTHER_TEST_SOUP, "population", Order.ascending, 2), 0);
		assertEquals("San Francisco should be first", "San Francisco", results2.getJSONObject(0).get("name"));
		assertEquals("Paris should be first", "Paris", results2.getJSONObject(1).get("name"));
	}
	
	/**
	 * Helper method for alter soup tests
	 * @param reIndexData
	 * @throws JSONException
	 */
	private void alterSoupHelper(boolean reIndexData) throws JSONException {
		IndexSpec[] indexSpecs = new IndexSpec[] {new IndexSpec("lastName", Type.string), new IndexSpec("address.city", Type.string)};
		
		assertFalse("Soup other_test_soup should not exist", store.hasSoup(OTHER_TEST_SOUP));
		store.registerSoup(OTHER_TEST_SOUP, indexSpecs);
		assertTrue("Register soup call failed", store.hasSoup(OTHER_TEST_SOUP));

		// Populate soup
		JSONObject soupElt1Created = store.create(OTHER_TEST_SOUP, new JSONObject("{'lastName':'Doe', 'address':{'city':'San Francisco','street':'1 market'}}"));
		JSONObject soupElt2Created = store.create(OTHER_TEST_SOUP, new JSONObject("{'lastName':'Jackson', 'address':{'city':'Los Angeles','street':'100 mission'}}"));
		JSONObject soupElt3Created = store.create(OTHER_TEST_SOUP, new JSONObject("{'lastName':'Watson', 'address':{'city':'London','street':'50 market'}}"));

		// Alter soup
		IndexSpec[] indexSpecsNew = new IndexSpec[] {new IndexSpec("lastName", Type.string), new IndexSpec("address.street", Type.string)};
		store.alterSoup(OTHER_TEST_SOUP, indexSpecsNew, reIndexData);
		
		// Check index specs
		checkIndexSpecs(indexSpecsNew);
		
		// Check DB
		Cursor c = null;
		try {
			String soupTableName = getSoupTableName(OTHER_TEST_SOUP);
			assertEquals("Table for other_test_soup was expected to be called TABLE_2", "TABLE_2", soupTableName);
			assertTrue("Table for other_test_soup should now exist", hasTable("TABLE_2"));
			
			c = DBHelper.getInstance(db).query(db, soupTableName, null, "id ASC", null, null);
			assertTrue("Expected a soup element", c.moveToFirst());
			assertEquals("Expected three soup elements", 3, c.getCount());
			
			assertEquals("Wrong id", idOf(soupElt1Created), c.getLong(c.getColumnIndex("id")));
			assertEquals("Wrong created date", soupElt1Created.getLong(SmartStore.SOUP_LAST_MODIFIED_DATE), c.getLong(c.getColumnIndex("lastModified")));
			assertEquals("Wrong value in index column", "Doe", c.getString(c.getColumnIndex(soupTableName + "_0")));
			if (reIndexData)
				assertEquals("Wrong value in index column", "1 market", c.getString(c.getColumnIndex(soupTableName + "_1")));
			else
				assertNull("Wrong value in index column", c.getString(c.getColumnIndex(soupTableName + "_1"))); 
			assertSameJSON("Wrong value in soup column", soupElt1Created, new JSONObject(c.getString(c.getColumnIndex("soup"))));
			
			c.moveToNext();
			assertEquals("Wrong id", idOf(soupElt2Created), c.getLong(c.getColumnIndex("id")));
			assertEquals("Wrong created date", soupElt2Created.getLong(SmartStore.SOUP_LAST_MODIFIED_DATE), c.getLong(c.getColumnIndex("lastModified")));
			assertEquals("Wrong value in index column", "Jackson", c.getString(c.getColumnIndex(soupTableName + "_0")));
			if (reIndexData)
				assertEquals("Wrong value in index column", "100 mission", c.getString(c.getColumnIndex(soupTableName + "_1")));
			else
				assertNull("Wrong value in index column", c.getString(c.getColumnIndex(soupTableName + "_1"))); 
			assertSameJSON("Wrong value in soup column", soupElt2Created, new JSONObject(c.getString(c.getColumnIndex("soup"))));
		
			c.moveToNext();
			assertEquals("Wrong id", idOf(soupElt3Created), c.getLong(c.getColumnIndex("id")));
			assertEquals("Wrong created date", soupElt3Created.getLong(SmartStore.SOUP_LAST_MODIFIED_DATE), c.getLong(c.getColumnIndex("lastModified")));
			assertEquals("Wrong value in index column", "Watson", c.getString(c.getColumnIndex(soupTableName + "_0")));
			if (reIndexData)
				assertEquals("Wrong value in index column", "50 market", c.getString(c.getColumnIndex(soupTableName + "_1")));
			else
				assertNull("Wrong value in index column", c.getString(c.getColumnIndex(soupTableName + "_1"))); 
			assertSameJSON("Wrong value in soup column", soupElt3Created, new JSONObject(c.getString(c.getColumnIndex("soup"))));
			
		}
		finally {
			safeClose(c);
		}
	}

	/**
	 * Helper method
	 * @param indexSpecs
	 */
	private void checkIndexSpecs(IndexSpec[] indexSpecs) {
		// Check index specs
		IndexSpec[] indexSpecsReturned = store.getSoupIndexSpecs(OTHER_TEST_SOUP);
		assertEquals("Should have the same number of index specs", indexSpecs.length, indexSpecsReturned.length);
		for (int i = 0; i<indexSpecs.length; i++) {
			assertEquals("Wrong index spec path", indexSpecs[i].path, indexSpecsReturned[i].path);
			assertEquals("Wrong index spec type", indexSpecs[i].type, indexSpecsReturned[i].type);
			assertEquals("Wrong index spec column", "TABLE_2_" + i, indexSpecsReturned[i].columnName);			
		}
	}


	/**
	 * Test reIndexSoup
	 * @throws JSONException
	 */
	public void testReIndexSoup() throws JSONException {
		IndexSpec[] indexSpecs = new IndexSpec[] {new IndexSpec("lastName", Type.string)};
		
		assertFalse("Soup other_test_soup should not exist", store.hasSoup(OTHER_TEST_SOUP));
		store.registerSoup(OTHER_TEST_SOUP, indexSpecs);
		assertTrue("Register soup call failed", store.hasSoup(OTHER_TEST_SOUP));

		JSONObject soupElt1 = new JSONObject("{'lastName':'Doe', 'address':{'city':'San Francisco','street':'1 market'}}");
		
		store.create(OTHER_TEST_SOUP, soupElt1);

		// Find by last name
		assertRowCount(1, "lastName", "Doe");

		// Find by city - error expected - field is not yet indexed
		try {
			assertRowCount(1, "address.city", "San Francisco");
			fail("Expected smart sql exception");
		}
		catch (SmartSqlException e) {
			// as expected
		}

		// Alter soup - add city + street
		IndexSpec[] indexSpecsNew = new IndexSpec[] {new IndexSpec("lastName", Type.string), new IndexSpec("address.city", Type.string), new IndexSpec("address.street", Type.string)};
		store.alterSoup(OTHER_TEST_SOUP, indexSpecsNew, false);

		// Find by city - no rows expected (we have not re-indexed yet)
		assertRowCount(0, "address.city", "San Francisco");

		// Re-index city
		store.reIndexSoup(OTHER_TEST_SOUP, new String[] {"address.city"}, true);
		
		// Find by city
		assertRowCount(1, "address.city", "San Francisco");

		// Find by street - no rows expected (we have not re-indexed yet)
		assertRowCount(0, "address.street", "1 market");

		// Re-index street
		store.reIndexSoup(OTHER_TEST_SOUP, new String[] {"address.street"}, true);
		
		// Find by street
		assertRowCount(1, "address.street", "1 market");
	}
	
	/**
	 * Helper function for testReIndexSoup: count rows where field has value
	 * @param expectedCount
	 * @param field
	 * @param value
	 * @throws JSONException
	 */
	private void assertRowCount(int expectedCount, String field, String value) throws JSONException {
		String smartSql = "SELECT count(*) FROM {" + OTHER_TEST_SOUP + "} WHERE {" + OTHER_TEST_SOUP + ":" + field + "} = '" + value + "'";
		int actualCount = store.query(QuerySpec.buildSmartQuerySpec(smartSql, 1), 0).getJSONArray(0).getInt(0);
		assertEquals("Should have found " + expectedCount + " rows", expectedCount, actualCount);
	}

	/**
	 * Test alter soup interrupted and resumed after step RENAME_OLD_SOUP_TABLE
	 * @throws JSONException
	 */
	public void testAlterSoupResumeAfterRenameOldSoupTable() throws JSONException {
		tryAlterSoupInterruptResume(AlterSoupStep.RENAME_OLD_SOUP_TABLE);
	}

	/**
	 * Test alter soup interrupted and resumed after step DROP_OLD_INDEXES
	 * @throws JSONException
	 */
	public void testAlterSoupResumeAfterDropOldIndexed() throws JSONException {
		tryAlterSoupInterruptResume(AlterSoupStep.DROP_OLD_INDEXES);
	}

	/**
	 * Test alter soup interrupted and resumed after step REGISTER_SOUP_USING_TABLE_NAME
	 * @throws JSONException
	 */
	public void testAlterSoupResumeAfterRegisterSoupUsingTableName() throws JSONException {
		tryAlterSoupInterruptResume(AlterSoupStep.REGISTER_SOUP_USING_TABLE_NAME);
	}

	/**
	 * Test alter soup interrupted and resumed after step COPY_TABLE
	 * @throws JSONException
	 */
	public void testAlterSoupResumeAfterCopyTable() throws JSONException {
		tryAlterSoupInterruptResume(AlterSoupStep.COPY_TABLE);
	}

	/**
	 * Test alter soup interrupted and resumed after step RE_INDEX_SOUP 
	 * @throws JSONException
	 */
	public void testAlterSoupResumeAfterReIndexSoup() throws JSONException {
		tryAlterSoupInterruptResume(AlterSoupStep.RE_INDEX_SOUP);
	}

	/**
	 * Test alter soup interrupted and resumed after step DROP_OLD_TABLE 
	 * @throws JSONException
	 */
	public void testAlterSoupResumeAfterDropOldTable() throws JSONException {
		tryAlterSoupInterruptResume(AlterSoupStep.DROP_OLD_TABLE);
	}
	
	
	/**
	 * Helper for testAlterSoupInterruptResume
	 * @throws JSONException
	 */
	private void tryAlterSoupInterruptResume(AlterSoupStep toStep) throws JSONException {
		assertFalse("Soup other_test_soup should not exist", store.hasSoup(OTHER_TEST_SOUP));
		IndexSpec[] indexSpecs = new IndexSpec[] {new IndexSpec("lastName", Type.string)};
		store.registerSoup(OTHER_TEST_SOUP, indexSpecs);
		IndexSpec[] oldIndexSpecs = store.getSoupIndexSpecs(OTHER_TEST_SOUP); // with column names
		String soupTableName = getSoupTableName(OTHER_TEST_SOUP);
		assertTrue("Register soup call failed", store.hasSoup(OTHER_TEST_SOUP));

		// Populate soup
		JSONObject soupElt1Created = store.create(OTHER_TEST_SOUP, new JSONObject("{'lastName':'Doe', 'address':{'city':'San Francisco','street':'1 market'}}"));
		JSONObject soupElt2Created = store.create(OTHER_TEST_SOUP, new JSONObject("{'lastName':'Jackson', 'address':{'city':'Los Angeles','street':'100 mission'}}"));
		
		// Partial alter - up to toStep included
		IndexSpec[] indexSpecsNew = new IndexSpec[] {new IndexSpec("lastName", Type.string), new IndexSpec("address.city", Type.string), new IndexSpec("address.street", Type.string)};
		AlterSoupLongOperation operation = new AlterSoupLongOperation(store, OTHER_TEST_SOUP, indexSpecsNew, true);
		operation.run(toStep);

		// Validate long_operations_status table
		LongOperation[] operations = store.getLongOperations();
		int expectedCount = (toStep == AlterSoupStep.LAST ? 0 : 1);
		assertEquals("Wrong number of long operations found", expectedCount, operations.length);
		
		if (operations.length > 0) {
			// Check details
			JSONObject actualDetails = operations[0].getDetails();
			assertEquals("Wrong soup name", OTHER_TEST_SOUP, actualDetails.getString("soupName"));
			assertEquals("Wrong soup table name", soupTableName, actualDetails.getString("soupTableName"));
			assertSameJSON("Wrong old indexes", IndexSpec.toJSON(oldIndexSpecs), actualDetails.getJSONArray("oldIndexSpecs"));
			// new index specs in details might or might not have column names based on step so not comparing with assertSameJSON however checkIndexSpecs below should catch any discrepancies
			assertEquals("Wrong re-index data", true, actualDetails.getBoolean("reIndexData"));
			
			// Check last step completed
			assertEquals("Wrong step", toStep, ((AlterSoupLongOperation) operations[0]).getLastStepCompleted());
			
			// Simulate restart (clear cache and call resumeLongOperations)
			DBHelper.getInstance(db).clearMemoryCache();
			store.resumeLongOperations();
			
			// Check index specs
			checkIndexSpecs(indexSpecsNew);
			
			// Check DB
			Cursor c = null;
			try {
				assertEquals("Table for other_test_soup was expected to be called TABLE_2", "TABLE_2", soupTableName);
				assertTrue("Table for other_test_soup should now exist", hasTable("TABLE_2"));
				
				c = DBHelper.getInstance(db).query(db, soupTableName, null, "id ASC", null, null);
				assertTrue("Expected a soup element", c.moveToFirst());
				assertEquals("Expected three soup elements", 2, c.getCount());
				
				assertEquals("Wrong id", idOf(soupElt1Created), c.getLong(c.getColumnIndex("id")));
				assertEquals("Wrong created date", soupElt1Created.getLong(SmartStore.SOUP_LAST_MODIFIED_DATE), c.getLong(c.getColumnIndex("lastModified")));
				assertEquals("Wrong value in index column", "Doe", c.getString(c.getColumnIndex(soupTableName + "_0")));
				assertEquals("Wrong value in index column", "San Francisco", c.getString(c.getColumnIndex(soupTableName + "_1")));
				assertEquals("Wrong value in index column", "1 market", c.getString(c.getColumnIndex(soupTableName + "_2")));
				assertSameJSON("Wrong value in soup column", soupElt1Created, new JSONObject(c.getString(c.getColumnIndex("soup"))));
				
				c.moveToNext();
				assertEquals("Wrong id", idOf(soupElt2Created), c.getLong(c.getColumnIndex("id")));
				assertEquals("Wrong created date", soupElt2Created.getLong(SmartStore.SOUP_LAST_MODIFIED_DATE), c.getLong(c.getColumnIndex("lastModified")));
				assertEquals("Wrong value in index column", "Jackson", c.getString(c.getColumnIndex(soupTableName + "_0")));
				assertEquals("Wrong value in index column", "Los Angeles", c.getString(c.getColumnIndex(soupTableName + "_1")));
				assertEquals("Wrong value in index column", "100 mission", c.getString(c.getColumnIndex(soupTableName + "_2")));
				assertSameJSON("Wrong value in soup column", soupElt2Created, new JSONObject(c.getString(c.getColumnIndex("soup"))));
			}
			finally {
				safeClose(c);
			}
		}
	}
}
