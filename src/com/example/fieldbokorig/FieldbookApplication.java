/*
 * Copyright 2012 Fieldbook authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *       
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.fieldbokorig;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

public class FieldbookApplication extends Application {
	// Define the constants here
	public static final String MOUNT_SD_CARD = Environment.getExternalStorageDirectory().getPath();
	public static final String INSTALL_DIRECTORY = MOUNT_SD_CARD + "/fieldbook"; //install directory
	public static final String FIELDBOOK_DIRECTORY = INSTALL_DIRECTORY + "/files";	//contains the fieldbook files
	public static final String CONFIG_DIRECTORY = INSTALL_DIRECTORY + "/config";	//contains config files
	public static final String ARCHIVE_DIRECTORY = FIELDBOOK_DIRECTORY + "/archives";	//contains "removed" fieldbooks
	public static final String NOTES_DIRECTORY = FIELDBOOK_DIRECTORY + "/notes";	//contains typewritten observations
	public static final String PHOTOS_DIRECTORY = FIELDBOOK_DIRECTORY + "/photos";	//contains photographic observations
	public static final String RECYCLE_BIN_DIRECTORY = ARCHIVE_DIRECTORY + "/recycle_bin";	//contains "deleted" fieldbooks
	public static final String EMAIL_DIRECTORY = MOUNT_SD_CARD + "/download";	//not sure if this is standard location
	public static final String BLUETOOTH_DIRECTORY = MOUNT_SD_CARD + "/bluetooth"; 	//not sure if this is standard location
	
	// Will store the common preferences shared across activities
	SharedPreferences prefs;	

	@Override
	public void onCreate() {
		super.onCreate();

		// TODO Auto-generated method stub
		prefs = PreferenceManager.getDefaultSharedPreferences(this); // just prepare this package variable
		
		// Usage for prefs object is as follows:
		String userId = prefs.getString("userId", "");	// the keys must be defined elsewhere or here.
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();

		// TODO Auto-generated method stub
	}

	@Override
	public void onTerminate() {
		super.onTerminate();

		// TODO Auto-generated method stub
	}

}
