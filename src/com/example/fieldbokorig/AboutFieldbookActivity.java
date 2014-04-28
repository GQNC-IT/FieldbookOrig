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

import java.io.IOException;
import java.io.InputStream;

import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.example.fieldbokorig.R;

public class AboutFieldbookActivity extends SherlockActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.aboutfieldbookactivity); // just a very simple free-flowing text
		try {
//			InputStream is = getAssets().open("about_fieldbook.html");
			InputStream is = getAssets().open("about_fieldbook.txt");
			int size = is.available();	// get size first

			// Read the entire asset into a local byte buffer.
			byte[] buffer = new byte[size];	// create buffer enough for size
			is.read(buffer);	// buffer now contains the entire text
			is.close();	// close the InputStream

			// Convert the buffer into a string.
			String text = new String(buffer);

			// Finally stick the string into the text view.
			TextView tv = (TextView) findViewById(R.id.text);
			
//			tv.setText(Html.fromHtml(text));
			tv.setText(text);
		} catch (IOException e) {
			Toast.makeText(this, "about_fieldbook.txt not found in assets directory.", Toast.LENGTH_LONG).show();
		}
	}

}
