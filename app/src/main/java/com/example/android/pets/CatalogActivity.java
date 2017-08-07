/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.example.android.pets;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.pets.data.PetContract.PetEntry;

/**
 * Displays list of pets that were entered and stored in the app.
 */
public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    /** Tag for the log messages */
    private static final String LOG_TAG = CatalogActivity.class.getSimpleName();

    /** Identifies a particular Loader being used in this component */
    private static final int PET_LOADER = 0;

    private PetCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Find ListView to populate
        ListView petListView = (ListView) findViewById(R.id.list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        petListView.setEmptyView(emptyView);

        // Setup an Adapter to create a list item for each row of pet data in the Cursor.
        // There is no pet data yet (until loader finishes) so pass in null for the Cursor.
        mCursorAdapter = new PetCursorAdapter(this, null);
        petListView.setAdapter(mCursorAdapter);

        // Setup listener that opens EditorActivity when ListView item is clicked.
        petListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                // Set the URI on the data field of the intent.
                intent.setData(ContentUris.withAppendedId(PetEntry.CONTENT_URI, id));
                // Launch the {@link EditorActivity} to display the data for the current pet.
                startActivity(intent);
            }
        });

        // Initializes the CursorLoader. The URL_LOADER is eventually passed to onCreateLoader().
        getLoaderManager().initLoader(PET_LOADER, null, this);
    }

    private void insertPet() {
        // Set {@link ContentValues} object with dummy data.
        ContentValues insert = new ContentValues();
        insert.put(PetEntry.COLUMN_PET_NAME, "Tommy");
        insert.put(PetEntry.COLUMN_PET_BREED, "Pitbull");
        insert.put(PetEntry.COLUMN_PET_GENDER, PetEntry.GENDER_MALE);
        insert.put(PetEntry.COLUMN_PET_WEIGHT, 45);

        // Call ContentResolver for insert dummy data.
        Uri lastPet = getContentResolver().insert(PetEntry.CONTENT_URI, insert);

        // If pet has been added show toast successful message, toast error message otherwise.
        if (lastPet != null) {
            Toast.makeText(this, R.string.editor_insert_pet_successful, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.editor_insert_pet_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_all_pets_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User clicked the "Delete" button, so delete all pets.
                deleteAllPets();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User clicked the "Cancel" button, so dismiss the dialog.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Helper method for delete all pets in the database.
     */
    private void deleteAllPets() {
        // Use ContentResolver for delete all pets from database
        int rowsDeleted = getContentResolver().delete(PetEntry.CONTENT_URI, null, null);
        // Show toast message depending on whether or not the delete was successful.
        if (rowsDeleted != 0) {
            // If rows were deleted, the delete was successful and we can display the toast.
            Toast.makeText(this, R.string.editor_delete_all_pets_successful, Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, then there was an error with the delete.
            Toast.makeText(this, R.string.editor_delete_all_pets_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                // Insert dummy data
                insertPet();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case PET_LOADER:
                // Define a projection that specifies the columns from the table we care about.
                String[] projection = {
                        PetEntry._ID,
                        PetEntry.COLUMN_PET_NAME,
                        PetEntry.COLUMN_PET_BREED
                };

                // Return a new CursorLoader
                return new CursorLoader(this,   // Parent activity context
                        PetEntry.CONTENT_URI,   // Provider context URI to query
                        projection,             // Columns to include in the resulting Cursor
                        null,                   // No selection clause
                        null,                   // No selection arguments
                        null                    // Default sort order
                );
            default:
                // An invalid id was passed in
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update {@link PetCursorAdapter} with this new cursor containing updated pet data.
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Clears out the adapter's reference to the Cursor.
        // This prevent memory leaks.
        mCursorAdapter.swapCursor(null);
    }
}
