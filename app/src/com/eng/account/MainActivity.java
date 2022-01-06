/*
 * Copyright (c) 2012-present, salesforce.com, inc.
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
package com.eng.account;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.mobilesync.app.MobileSyncSDKManager;
import com.salesforce.androidsdk.mobilesync.manager.SyncManager;
import com.salesforce.androidsdk.mobilesync.target.SyncTarget;
import com.salesforce.androidsdk.rest.ApiVersionStrings;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.smartstore.app.SmartStoreSDKManager;
import com.salesforce.androidsdk.smartstore.store.IndexSpec;
import com.salesforce.androidsdk.smartstore.store.QuerySpec;
import com.salesforce.androidsdk.smartstore.store.SmartStore;
import com.salesforce.androidsdk.ui.SalesforceActivity;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Main activity
 */
public class MainActivity extends SalesforceActivity {


    private RestClient client;
    private ArrayAdapter<String> listAdapter;

	private SmartStore smartStore;
	final IndexSpec[] ACCOUNTS_INDEX_SPEC = {
			new IndexSpec("Id", SmartStore.Type.string)
	};

	public MainActivity() {
		smartStore = SmartStoreSDKManager.getInstance().getSmartStore();
		smartStore.registerSoup("AccountSoup", ACCOUNTS_INDEX_SPEC);
	}

	/**
	 * Inserts accounts into the accounts soup.
	 *
	 * @param accounts Accounts.
	 */
	public void insertAccounts(JSONArray accounts) {
		if (accounts != null) {
			for (int i = 0; i < accounts.length(); i++) {
				try {
					accounts.getJSONObject(i).put(SyncTarget.LOCAL, true);
					accounts.getJSONObject(i).put(SyncTarget.LOCALLY_UPDATED, true);
					accounts.getJSONObject(i).put("Name", accounts.getJSONObject(i).get("Name") + " Synced");
					smartStore.upsert(
							"AccountSoup", accounts.getJSONObject(i));
				} catch (JSONException exc) {
					Log.e("exception",
							"Error occurred while attempting "
									+ "to insert account. Please verify "
									+ "validity of JSON data set.");
				}
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Setup theme
		boolean isDarkTheme = MobileSyncSDKManager.getInstance().isDarkTheme();
		setTheme(isDarkTheme ? R.style.SalesforceSDK_Dark : R.style.SalesforceSDK);
		MobileSyncSDKManager.getInstance().setViewNavigationVisibility(this);

		// Setup view
		setContentView(R.layout.main);
	}
	
	@Override 
	public void onResume() {
		// Hide everything until we are logged in
		findViewById(R.id.root).setVisibility(View.INVISIBLE);

		// Create list adapter
		listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
		((ListView) findViewById(R.id.contacts_list)).setAdapter(listAdapter);				
		
		super.onResume();
	}		
	
	@Override
	public void onResume(RestClient client) {
        // Keeping reference to rest client
        this.client = client; 

		// Show everything
		findViewById(R.id.root).setVisibility(View.VISIBLE);
	}

	/**
	 * Called when "Logout" button is clicked. 
	 * 
	 * @param v
	 */
	public void onLogoutClick(View v) {
		SalesforceSDKManager.getInstance().logout(this);
	}
	
	/**
	 * Called when "Clear" button is clicked. 
	 * 
	 * @param v
	 */
	public void onClearClick(View v) {
		listAdapter.clear();
	}	

	/**
	 * Called when "Fetch Contacts" button is clicked.
	 *
	 * @param v
	 * @throws UnsupportedEncodingException 
	 */
	public void onFetchContactsClick(View v) throws UnsupportedEncodingException {
        sendRequest("SELECT Name FROM Contact ORDER BY Name");
	}

	/**
	 * Called when "Fetch Contacts" button is clicked.
	 *
	 * @param v
	 * @throws UnsupportedEncodingException
	 */
	public void onFetchContactsJoao(View v) throws UnsupportedEncodingException {
		sendRequest("SELECT Name FROM Contact WHERE FirstName = 'Joao' ORDER BY Name");
	}

	public void showOfflineAccounts(View v) throws UnsupportedEncodingException {
		try {
			//SyncManager.getInstance().createSyncUp(null,null,"AccountSoup", "MySync1");
			listAdapter.clear();
			QuerySpec querySpec = QuerySpec.buildAllQuerySpec(
					"AccountSoup",
					"Name",
					null,
					20
			);
					//QuerySpec.buildSmartQuerySpec("select {AccountSoup:Name} from {AccountSoup}", 0);
			JSONArray records = smartStore.query(querySpec,0);
			Log.d("soup1" , records.toString());
			for (int i = 0; i < records.length(); i++) {
				listAdapter.add(records.getJSONObject(i).getString("Name"));
			}
		}catch(Exception e){
			Toast.makeText(MainActivity.this,
					MainActivity.this.getString(R.string.sf__generic_error, e.toString()),
					Toast.LENGTH_LONG).show();
		}

	}

	public void onUpdateContactsClick(View v) throws UnsupportedEncodingException {
		HashMap<String, Object> fields = new HashMap<String, Object>();
		final int min = 10;
		final int max = 80;
		final int random = new Random().nextInt((max - min) + 1) + min;
		fields.put("LastName", "Silva " + random);
		fields.put("FirstName", "Joao");
		fields.put("Email", "joao.silva"+random+"@gmail.com");
		RestRequest restRequest = RestRequest.getRequestForCreate(ApiVersionStrings.getVersionNumber(this), "Contact", fields);

		client.sendAsync(restRequest, new AsyncRequestCallback() {
			@Override
			public void onSuccess(RestRequest request, final RestResponse result) {
				result.consumeQuietly(); // consume before going back to main thread
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						try {
							Log.d("response", result.asString());
							onFetchContactsJoao(v);
						} catch (Exception e) {
							onError(e);
						}
					}
				});
			}

			@Override
			public void onError(final Exception exception) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(MainActivity.this,
								MainActivity.this.getString(R.string.sf__generic_error, exception.toString()),
								Toast.LENGTH_LONG).show();
					}
				});
			}
		});
	}

	/**
	 * Called when "Fetch Accounts" button is clicked
	 * 
	 * @param v
	 * @throws UnsupportedEncodingException 
	 */
	public void onFetchAccountsClick(View v) throws UnsupportedEncodingException {
		final RestRequest restRequest =
				RestRequest.getRequestForQuery(
						getString(R.string.api_version),
						"SELECT Name, Id, OwnerId FROM Account", 20);
		client.sendAsync(restRequest, new AsyncRequestCallback() {
			@Override
			public void onSuccess(RestRequest request,
								  RestResponse result) {
				// Consume before going back to main thread
				// Not required if you don't do main (UI) thread tasks here
				result.consumeQuietly();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// Network component doesn’t report app layer status.
						// Use the Mobile SDK RestResponse.isSuccess() method to check
						// whether the REST request itself succeeded.
						if (result.isSuccess()) {
							try {
								Log.d("response", result.asString());
								final JSONArray records =
										result.asJSONObject().getJSONArray("records");
								insertAccounts(records);
							} catch (Exception e) {
								onError(e);
							} finally {
								Toast.makeText(MainActivity.this,
										"Records ready for offline access.",
										Toast.LENGTH_SHORT).show();
							}
						}
					}
				});
			}

			@Override
			public void onError(Exception e) {
				// You might want to log the error
				// or show it to the user
			}
		});
	}	
	
	private void sendRequest(String soql) throws UnsupportedEncodingException {
		RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);

		client.sendAsync(restRequest, new AsyncRequestCallback() {
			@Override
			public void onSuccess(RestRequest request, final RestResponse result) {
				result.consumeQuietly(); // consume before going back to main thread
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						try {
							listAdapter.clear();
							JSONArray records = result.asJSONObject().getJSONArray("records");
							for (int i = 0; i < records.length(); i++) {
								listAdapter.add(records.getJSONObject(i).getString("Name"));
							}
						} catch (Exception e) {
							onError(e);
						}
					}
				});
			}
			
			@Override
			public void onError(final Exception exception) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(MainActivity.this,
								MainActivity.this.getString(R.string.sf__generic_error, exception.toString()),
								Toast.LENGTH_LONG).show();
					}
				});
			}
		});
	}
}
