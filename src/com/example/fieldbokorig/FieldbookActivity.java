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

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.example.fieldbokorig.R;

public class FieldbookActivity extends SherlockActivity{
	public static int THEME = com.actionbarsherlock.R.style.Sherlock___Theme;	// Don't delete!
	static final int DIALOG_REMOVE_ID = 0;	// Confirmation to remove the selected fieldbooks
    ActionMode mMode;	// for the contextual ActionMode
	Boolean isDisplayedActionMode;	// signals that the ActionMode is up already

	// Define the controls and configures the ModelBean for the ListView adapter
	ListView listViewStudies;
	List<ModelBean> model;
	
	FieldbookActivityActionMode actionMode;	// handle to the FieldbookActivityActionMode inner class

	// Define a class-scoped Uri ArrayList to hold the ShareIntent parameter of the ShareActionProvider
	ArrayList<Uri> uriList;
	
	// Create a dialog to allow the user to confirm removal of the selected fieldbooks
	@Override
	protected Dialog onCreateDialog(int id) {
		// TODO: Create the dialogs here..
//		Dialog dialog = null;
		switch (id) {
		case DIALOG_REMOVE_ID:
			// Create out AlterDialog
			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Remove the selected fieldbooks?");
			builder.setCancelable(true);
			builder.setPositiveButton("Yes", new OkOnClickListener());
			builder.setNegativeButton("No", new CancelOnClickListener());
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
		}
		return super.onCreateDialog(id);
	}

	private final class CancelOnClickListener implements DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int which) {
			Toast.makeText(getApplicationContext(), "Good.", Toast.LENGTH_SHORT).show();
		}
	}

	private final class OkOnClickListener implements DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int which) {
			FieldbookActivity.this.actionMode.removeFieldbooks();
		}
	}
	
	// Show the overflow menuitem at the ActionBar.
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		// Use the resource-defined strings to make sure that the onOptionsItemSelected will work!
		menu.add(R.string.menuitem_FieldbookActivity_new).setIcon(R.drawable.content_new_light)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		menu.add(R.string.menuitem_FieldbookActivity_retrieve).setIcon(R.drawable.av_replay)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		menu.add(R.string.menuitem_FieldbookActivity_refresh).setIcon(android.R.drawable.ic_popup_sync)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		menu.add(R.string.menuitem_FieldbookActivity_about).setIcon(R.drawable.action_about_hdpi)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		return true;
    } // Use this to show the restore archives button..

	// Pairs with the onCreateOptionsMenu to handle the event when an menu item is selected by the user
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getTitle() == getResources().getString(R.string.menuitem_FieldbookActivity_new) ) {
			// TODO: Code for creating a new fieldbook file
		} 
		
		else if (item.getTitle() == getResources().getString(R.string.menuitem_FieldbookActivity_retrieve)) {
			startActivity(new Intent(this, RetrieveFieldbookActivity.class));
		} 
		
		else if (item.getTitle() == getResources().getString(R.string.menuitem_FieldbookActivity_refresh)) {
			new PrepareArrayAdapter().execute(FieldbookApplication.FIELDBOOK_DIRECTORY);
		} 
		
		else if (item.getTitle() == getResources().getString(R.string.menuitem_FieldbookActivity_about)) {
			startActivity(new Intent(this, AboutFieldbookActivity.class));
		}
		return true;
	}

	
	// This is the activity that always gets called first..
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fieldbook);	
		
		// Check the filesystem...
		new CheckFilesystem().execute(FieldbookApplication.RECYCLE_BIN_DIRECTORY);	// entire path will be created.

		listViewStudies = (ListView) findViewById(R.id.listViewStudies);
		// Prepare the ArrayAdapter in a non-UI thread
		new PrepareArrayAdapter().execute(FieldbookApplication.FIELDBOOK_DIRECTORY);
		
		uriList = new ArrayList<Uri>();	// Initialize the uriList
		isDisplayedActionMode = false;	// ActionMode initially off -- of course.
	} //onCreate

	
	
	// This is called whenever this Activity gets restarted coming from another activity
	@Override
	protected void onRestart() {
		super.onRestart();

		// Refresh the contents of the ListView
//		new PrepareArrayAdapter().execute(FieldbookApplication.FIELDBOOK_DIRECTORY);
	}

	
	
	// Takes the complete directory path to create... i.e., /mnt/sdcard/fieldbook/files/archives
	// Always check the Fieldbook's filesystem at startup. microSD cards may have been replaced
	private final class CheckFilesystem extends AsyncTask<String, String, String>{

		@Override
		protected String doInBackground(String... params) {
			StringBuilder sb = new StringBuilder();
			for(String param : params) {
				try {
					if (new File(param).mkdirs()) sb.append(param);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			return sb.toString();
		}
		
		
		@Override
		protected void onPostExecute(String result) {
			// inform the user that the filesystem is created
			if (result.length() != 0) {
				Toast.makeText(FieldbookActivity.this, "Fieldbook filesystem ready.", Toast.LENGTH_SHORT).show();
			}
		}

	}
	
	
	/**
	 *  This will prepare the array adapter in the background and then associate it to the ListView.
	 This ensures that the UI will not block when the number of files in the assigned directory
	 for the fieldbook files is unusually large.
	 The first parameter(of the generic class) is the input to the doInBackground function. The
	 2nd parameter goes to the progress indicator, and the 3rd parameter goes to the result.
	
	 There is a very nice tutorial at [32:48] in 08_18 video by marakana. 
	 The way to access resources in the Activity from this internal asynchronous task is by using a
	 call similar to this:
	 		FieldbookActivity.this.getString(R.string.theStringHere);
	 * @author RAnacleto
	 *
	 */
	private final class PrepareArrayAdapter extends AsyncTask<String, String, List<ModelBean>> {

		// This is called to do the work inside this separate thread.
		@Override
		protected List<ModelBean> doInBackground(String... pathToFieldbook) {
			// Define the return object
			List<ModelBean> model = new ArrayList<ModelBean>();

			// Read the contents of the mount path: i.e. "MOUNT_SD_CARD + mntPath"
			File folder = new File(pathToFieldbook[0]);	// just get the first parameter in the input String array

			if (folder.exists()) {
				// Loop through all entries of the folder array and put each file into the ArrayList
				String md5 = null;
				for (final File fileEntry : folder.listFiles()) {
					if (!fileEntry.isDirectory()) {
						// Compute the md5 of the file to serve as version control
						try {
							FileInputStream fis = new FileInputStream(new File(pathToFieldbook[0] + "/" + fileEntry.getName()));
							md5 = new String(Hex.encodeHex(DigestUtils.md5(fis)));
						} catch (Exception e) {
							md5 = "";
						} 

						//add new list entry into the ArrayList<ModelBean>
						model.add(new ModelBean(fileEntry.getName(),
							DateUtils.getRelativeTimeSpanString(FieldbookActivity.this.getApplicationContext(),
								fileEntry.lastModified()).toString(), 
								md5));
					}
				}

				//Sort the model
				Collections.sort(model, new Comparator<ModelBean>() {
					@Override
					public int compare(ModelBean lhs, ModelBean rhs) {
						return lhs.getFieldBookName().compareToIgnoreCase(rhs.getFieldBookName());
					}
				});
			}
			return model;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
		}

		// This will be called when the thread is done with its work.
		@Override
		protected void onPostExecute(List<ModelBean> result) {
			super.onPostExecute(result);

			// Instantiate a new ArrayAdapter.
			ArrayAdapter<ModelBean> adapter = new MultiSelectAdapter(FieldbookActivity.this, result);

			// Associate the listview with an adapter
			FieldbookActivity.this.listViewStudies.setAdapter(adapter);
			FieldbookActivity.this.listViewStudies.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
					CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkBoxFieldbook);
					ModelBean element = (ModelBean) checkbox.getTag();

					// Show the laste modified date and time of the clicked file.
					Toast.makeText(FieldbookActivity.this.getApplicationContext(), 
							(new SimpleDateFormat("dd MMM yyyy hh:mm:ss")).format(new Date((new File(FieldbookApplication.FIELDBOOK_DIRECTORY + "/" + element.getFieldBookName())).lastModified())),
							Toast.LENGTH_SHORT).show();
					
				}
			});
			
			FieldbookActivity.this.listViewStudies.setOnItemLongClickListener(new OnItemLongClickListener() {
				// TODO: Create the logic here to open the sqlite file when it is clicked directly..
				// This is where the Fieldbook file gets opened that subsequently leads to the Observations
				// activity..
				// The other listener is for the Checkbox configured in each View (row) within the listview.
				// Please find it at the MultiSelectAdapter class definition, particularly in the 
				// getView() method definition.

				@Override
				public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
					// Open the fieldbook and proceed to data collection
					CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkBoxFieldbook);
					ModelBean element = (ModelBean) checkbox.getTag();
					Toast.makeText(FieldbookActivity.this.getApplicationContext(), "fieldbook LONGCLICK", Toast.LENGTH_SHORT).show();
					return true; // prevent the onItemClick from firing!
				}
			});

			// This informs the user how many field books are present in the 'files' directory 
			Toast.makeText(FieldbookActivity.this.getApplicationContext(),
					String.valueOf(result.size()) + " fieldbook" +
					(result.size()==1?" ":"s ") + "found.", Toast.LENGTH_SHORT).show();
		}		
	} // end of the asynchronous task

	private final class FieldbookActivityActionMode implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.add(R.string.menuitem_FieldbookActivity_remove) 
                .setIcon(R.drawable.content_remove)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        	// Add the ShareActionProvider here. This is a multiple-attachment ShareActionProvider which
            // means that the attachments is dependent on whatever is selected by the user from among
            // the ArrayList presented in the ListView. Therefore, the "Uri"s configured in the ShareIntent
            // should get updated when the checkbox of each ListItem view gets clicked. Updating could mean
            // removing the item from the Uri array (if it was unchecked) or adding it to the Uri array
            // if the checkbox is checked. It's sort-of complicated... but... that's how it is..
            //
            // IMPORTANT: The ShareIntent component of the ShareActionProvider needs to be updated every time
            // the CheckBox in the ListView view gets checked/unchecked.
            getSupportMenuInflater().inflate(R.menu.share, menu);
			ShareActionProvider actionProvider = (ShareActionProvider) menu.findItem(R.id.share).getActionProvider();
            actionProvider.setShareHistoryFileName(null);	// ensures that the ShareHistory is not saved.

            return true;
        }
        
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        
        // This "listens" for the click event on the ActionMode buttons **except** the ShareActionProvider
        // button.
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        	// IMPORTANT! Let the user confirm first. Although "Remove" is technically reversible, it would
        	// be better if the user gives the final signal to the program to remove the selected items.
        	// Panic-relief.
        	showDialog(DIALOG_REMOVE_ID); //show and update the isOkToRemove variable
        	FieldbookActivity.this.actionMode = this; // pass the entire object 
        	// ^ POTENTIAL PITFALL: I wonder what will happen if the FieldbookActivity is "paused" 
        	//		(a.k.a. put in the background.
        	
            return true;
        }
        
        
        public void removeFieldbooks() {
			// Get a handle of the ArrayList
			List<ModelBean> list = ((MultiSelectAdapter) FieldbookActivity.this.listViewStudies
					.getAdapter()).getList();

			// Traverse the ArrayList and then perform the logic based on the
			// clicked MenuItem
			String filename;
			int count = 0;
			for (ModelBean bean : list) { // Use this instead of the
											// ArrayList<Uri> -- better!
				if (bean.isSelected()) {
					// Leave outside to ensure that the bean gets unselected
					// when traversed.
					bean.setSelected(false);

					filename = bean.getFieldBookName(); // pair this with the
														// install directory..

					// move the fieldbook from files directory to archives.
					File file = new File(
							FieldbookApplication.FIELDBOOK_DIRECTORY + "/"
									+ filename); // source
					File dir = new File(
							FieldbookApplication.ARCHIVE_DIRECTORY); // destination

					if ((new File(FieldbookApplication.ARCHIVE_DIRECTORY
							+ "/" + filename)).exists()) {
						// APPEND timestamp to filename
						if (file.renameTo(new File(dir, file.getName()
								+ "."
								+ (new SimpleDateFormat("yyyyMMddHHmmss"))
										.format(new Date()))))
							count++;
					} else {
						// move the file in the detination directory
						if (file.renameTo(new File(dir, file.getName())))
							count++;
					}
				} // This also decrements the intOnCount property
			} // end of For loop

			// Display how many files were processed (affected).
			Toast.makeText(
					FieldbookActivity.this.getApplicationContext(),
					String.valueOf(count) + " fieldbook"
							+ (count == 1 ? " " : "s ") + "removed.",
					Toast.LENGTH_SHORT).show();

			// Refresh the ListAdapter after deleting the files..
			// This updates the ArrayList<ModelBean> adapter of the ListView
			new PrepareArrayAdapter().execute(FieldbookApplication.FIELDBOOK_DIRECTORY);
			// ListView is reset to the top of the list.

			// This mode.finish() will take care of unchecking all checkboxes.
			FieldbookActivity.this.mMode.finish(); // removes ActionMode and clear the selection from the ArrayList<Uri>
        } //removeFieldbooks

        
        
        // This function gets called always when the ActionBar is destroyed. So, might as well put in the
        // logic here to take care of unchecking the checkboxes of the listitems.
        @Override
        public void onDestroyActionMode(ActionMode mode) {
        	// Get a handle of the ArrayList component of the ListView
        	List<ModelBean> list = ((MultiSelectAdapter) FieldbookActivity.this.listViewStudies.getAdapter()).getList();
        	
        	// Make sure that everything is unchecked when the ActionBar is removed.
        	for(ModelBean bean : list) { if (bean.isSelected()) { bean.setSelected(false); } }
        	((MultiSelectAdapter) FieldbookActivity.this.listViewStudies.getAdapter()).notifyDataSetChanged();
        	FieldbookActivity.this.isDisplayedActionMode = false;	// Raise the flag...
        	FieldbookActivity.this.uriList = null;	// ... and purge the entire list. Is this called twice?
        }
        
    } // FieldbookActivityActionMode

	
	// Use this private class to hold state values for each checkbox
	private final static class ModelBean {
	 	private String fieldBookName;	//prefix this with the index number in the list to prevent name clashes in case of duplicates
	 	private String fieldBookDate;
	 	private String fieldBookVersion;
		private boolean selected;
		static int intOnCount;

		//instantiate a new ModelBean
	 	public ModelBean(String fieldBookName, String fieldBookDate, String fieldBookVersion) {
			this.fieldBookName = fieldBookName;
			this.fieldBookDate= fieldBookDate;
			this.fieldBookVersion = fieldBookVersion;
			selected = false;
			intOnCount=0;
		}

		public String getFieldBookName() {
			return fieldBookName;
		}

		public void setFieldBookName(String fieldBookName) {
			this.fieldBookName = fieldBookName;
		}

		public String getFieldBookDate() {
			return fieldBookDate;
		}

		public void setFieldBookDate(String fieldBookDate) {
			this.fieldBookDate = fieldBookDate;
		}

		public String getFieldBookVersion() {
			return fieldBookVersion;
		}

		public void setFieldBookVersion(String fieldBookVersion) {
			this.fieldBookVersion = fieldBookVersion;
		}

	 	public boolean isSelected() {
			return selected;
		}

	 	public void setSelected(boolean selected) {
			this.selected = selected;
			if (selected) intOnCount++;
			else intOnCount--;
		}
	}

	private class MultiSelectAdapter extends ArrayAdapter<ModelBean> {
	 	private final List<ModelBean> list;
		private final Activity context;

	 	public MultiSelectAdapter(Activity context, List<ModelBean> list) {
			super(context, R.layout.fieldbookrow2, list);
			this.context = context;
			this.list = list;
		}

	 	// use this to hold the elements of the row layout
	 	private class ViewHolder {
			protected TextView textViewFieldbookDate;
			protected CheckBox checkBoxFieldbookName;
			protected TextView textViewFieldbookVersion;
			protected TextView textViewFieldbookName;
		}

	 	
	 	// This is a *major* method in the Activity. The ActionMode bar is either shown or hidden depending on
	 	// the number of checked/unchecked CheckBoxes. It is also here that the contents of the ShareIntent
	 	// gets updated. The ShareIntent contains only those items in the ListView that are checked. When the
	 	// ShareActionProvider gets finally invoked, the contents of the ShareIntent (represented as an 
	 	// ArrayList<Uri> gets attached to the email (in case Gmail is selected) or sent OTA (on-the-air) via
	 	// Bluetooth (in case that's the sharing mode chosen by the user).
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = null;
			if (convertView == null) {
				LayoutInflater inflator = context.getLayoutInflater();
				view = inflator.inflate(R.layout.fieldbookrow2, null);

				//map the row layout elements to the ViewHolder object
				final ViewHolder viewHolder = new ViewHolder();
				viewHolder.textViewFieldbookDate = (TextView) view.findViewById(R.id.textViewStudyDate);
				viewHolder.textViewFieldbookVersion = (TextView) view.findViewById(R.id.textViewStudyVersion);
				viewHolder.textViewFieldbookName = (TextView) view.findViewById(R.id.textViewStudyName);
				viewHolder.checkBoxFieldbookName = (CheckBox) view.findViewById(R.id.checkBoxFieldbook);

				// Setup a click listener to catch toggle changes. This is a major listener that
				// affects even the contents of the ShareActionProvider. The other listener is configured
				// when the row (a.k.a. customized "View") of the ListView gets clicked. You can find it
				// in the PrepareAdapter inner class, particularly in the onPostExecute() method
				// definition.
				viewHolder.checkBoxFieldbookName.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkBoxFieldbook);
						ModelBean element = (ModelBean) viewHolder.checkBoxFieldbookName.getTag();
						element.setSelected(checkbox.isChecked());

						// Determine whether to show or hide the ActionMode
						if (ModelBean.intOnCount == 0) {
							FieldbookActivity.this.mMode.finish(); // Close the ActionBar
							FieldbookActivity.this.isDisplayedActionMode = false;
							FieldbookActivity.this.uriList = null; // make sure all elements purged!
							// ^ wouldn't this cause the garbage collector to kick in???
							//   might be a problem when the user again decides to choose some fieldbooks.
						} else if(!FieldbookActivity.this.isDisplayedActionMode) {
							FieldbookActivity.this.mMode = startActionMode(new FieldbookActivityActionMode());
							FieldbookActivity.this.isDisplayedActionMode = true; //raise the flag
						}
						
						// Update the attachments of the ShareIintent
						if (FieldbookActivity.this.mMode != null) {
							if (FieldbookActivity.this.uriList == null) uriList = new ArrayList<Uri>(); // initialize the class variable
							
							// Implement the logic here. It's surprisingly easy!!
							if (checkbox.isChecked()) {
								// Add it to the uriList
								uriList.add(Uri.fromFile(new File(FieldbookApplication.FIELDBOOK_DIRECTORY + "/" + 
																	element.getFieldBookName())));
							} else {
								// Remove it from the uriList
								uriList.remove(Uri.fromFile(new File(FieldbookApplication.FIELDBOOK_DIRECTORY + "/" + 
										element.getFieldBookName())));
							}
							
				        	Intent shareIntent = new Intent(); // create a new Intent object 
				        	shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE); // of this type
				        	shareIntent.setType("*/*");	// to allow sending of binary files.
				        	// ^ Note: MIME type of "*/*" will only match activities that handle generic data streams
				        	//   GMail and Bluetooth are two of those activities... so this is OK.
				        	shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList);	// attach here

				        	// Grab a handle of the actionProvider to be able to modify its ShareIntent component.
							ShareActionProvider actionProvider = (ShareActionProvider) FieldbookActivity.this.mMode.getMenu().findItem(R.id.share).getActionProvider();
							actionProvider.setShareIntent(shareIntent);
						} 
						// Postcondition: The ShareIntent of the actionProvider is now updated

					} // end of onClick method of the OnClickListener
				});

				//This is the inflated view modified by the inflater object above.
				view.setTag(viewHolder); // stores the viewHolder object in the view
				viewHolder.checkBoxFieldbookName.setTag(list.get(position));	//so that the listbox will be uniquely identified
			} else {
				view = convertView;
				((ViewHolder) view.getTag()).checkBoxFieldbookName.setTag(list.get(position));
			}

			// This is the portion that actually updates the screen. Without this, the internal elements/contents
			// of the adapter will not be reflected on the screen.
			ViewHolder holder = (ViewHolder) view.getTag();	// extracts the object stored in the view.
			holder.textViewFieldbookName.setText(list.get(position).getFieldBookName());
			holder.textViewFieldbookDate.setText(list.get(position).getFieldBookDate());
			holder.textViewFieldbookVersion.setText(list.get(position).getFieldBookVersion());
			holder.checkBoxFieldbookName.setChecked(list.get(position).isSelected());

			return view;
		}

		public List<ModelBean> getList() {
			return list;
		}
		
		
	}
}
