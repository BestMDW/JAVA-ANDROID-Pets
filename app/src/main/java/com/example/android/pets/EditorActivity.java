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
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.pets.data.PetContract.PetEntry;

/**
 * Allows user to create a new pet or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    /** Constant variable for the name of log tags in this activity. */
    private final String LOG_TAG = EditorActivity.class.getSimpleName();

    /** Identifies a particular Loader is being used in this component */
    private static final int PET_LOADER = 0;

    /** Constant variable for checking if activity is in insert mode (for adding new pet). */
    private static final int INSERT_MODE = 0;
    /** Constant variable for checking if activity is in edit mode (for editing exists pet). */
    private static final int EDIT_MODE = 1;
    /** Initial variable which contains in which mode is running activity. */
    private static int mCurrentMode;

    /** EditText field to enter the pet's name */
    private EditText mNameEditText;

    /** EditText field to enter the pet's breed */
    private EditText mBreedEditText;

    /** EditText field to enter the pet's weight */
    private EditText mWeightEditText;

    /** EditText field to enter the pet's gender */
    private Spinner mGenderSpinner;

    /**
     * Gender of the pet. The possible values are:
     * 0 for unknown gender, 1 for male, 2 for female.
     */
    private int mGender = PetEntry.GENDER_UNKNOWN;

    /** URI passed by intent */
    private Uri mCurrentPetUri;

    /** Initial variable for checking if any changes has been made on the form. */
    private boolean mPetHasChanged = false;

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mPetHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Get URI from intent.
        mCurrentPetUri = getIntent().getData();

        // If the intent DOES NOT contain a pet content URI, then we know that we are
        // creating new pet.
        if (mCurrentPetUri == null) {
            // This is a new pet, so change the app bar to say "Add a Pet".
            setTitle(R.string.editor_activity_title_new_pet);
            // Set current mode as insert mode
            mCurrentMode = INSERT_MODE;
            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing pet, so change the app bar to say "Edit Pet".
            setTitle(R.string.editor_activity_title_edit_pet);
            Log.i(LOG_TAG, "URI passed by intent: " + mCurrentPetUri);
            // Set current mode as edit mode
            mCurrentMode = EDIT_MODE;

            // Initialize the CursorLoader.
            getLoaderManager().initLoader(PET_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_pet_name);
        mBreedEditText = (EditText) findViewById(R.id.edit_pet_breed);
        mWeightEditText = (EditText) findViewById(R.id.edit_pet_weight);
        mGenderSpinner = (Spinner) findViewById(R.id.spinner_gender);

        // Setup gender spinner
        setupSpinner();

        // Set onTouchListener on data fields from the form.
        mNameEditText.setOnTouchListener(mTouchListener);
        mBreedEditText.setOnTouchListener(mTouchListener);
        mWeightEditText.setOnTouchListener(mTouchListener);
        mGenderSpinner.setOnTouchListener(mTouchListener);
    }

    private void savePet()
    {
        // Read and convert necessary data from user input
        String name = mNameEditText.getText().toString().trim();
        String breed = mBreedEditText.getText().toString().trim();
        String weightString = mWeightEditText.getText().toString().trim();

        // Do nothing when all fields are empty.
        if (mCurrentPetUri == null &&
                TextUtils.isEmpty(name) && TextUtils.isEmpty(breed) &&
                TextUtils.isEmpty(weightString) && mGender == PetEntry.GENDER_UNKNOWN) { return; }

        // If the weight is not provided by the user, don't try to parse the string into an
        // integer value. Use 0 by default
        int weight = 0;
        if (!TextUtils.isEmpty(weightString)) {
            weight = Integer.parseInt(weightString);
        }

        // Set data into {@link ContentValues} for database query
        ContentValues data = new ContentValues();
        data.put(PetEntry.COLUMN_PET_NAME, name);
        data.put(PetEntry.COLUMN_PET_BREED, breed);
        data.put(PetEntry.COLUMN_PET_GENDER, mGender);
        data.put(PetEntry.COLUMN_PET_WEIGHT, weight);

        // Check in which mode currently running activity.
        switch (mCurrentMode) {
            case INSERT_MODE:
                // Insert data into the database
                Uri lastPet = getContentResolver().insert(PetEntry.CONTENT_URI, data);

                // If new pet has been added show {@link Toast} message with his ID,
                // otherwise show {@link Toast} with error message.
                if (lastPet != null) {
                    Toast.makeText(this, R.string.editor_insert_pet_successful, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.editor_insert_pet_failed, Toast.LENGTH_SHORT).show();
                }
                break;
            case EDIT_MODE:
                // Update data in the database
                int petsUpdated = getContentResolver().update(mCurrentPetUri, data, null, null);

                // If new pet has been added show {@link Toast} message with his ID,
                // otherwise show {@link Toast} with error message.
                if (petsUpdated != 0) {
                    Toast.makeText(this, R.string.editor_update_pet_successful, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.editor_update_pet_failed, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /**
     * Setup the dropdown spinner that allows the user to select the gender of the pet.
     */
    private void setupSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mGenderSpinner.setAdapter(genderSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mGenderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.gender_male))) {
                        mGender = PetEntry.GENDER_MALE; // Male
                    } else if (selection.equals(getString(R.string.gender_female))) {
                        mGender = PetEntry.GENDER_FEMALE; // Female
                    } else {
                        mGender = PetEntry.GENDER_UNKNOWN; // Unknown
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mGender = PetEntry.GENDER_UNKNOWN; // Unknown
            }
        });
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        //Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User clicked the "Delete" button, so delete the pet.
                deletePet();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
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
     * Perform the deletion of the pet in the database.
     */
    private void deletePet() {
        if (mCurrentPetUri == null) {
            return;
        }

        // Use ContentResolver for delete pet from the database.
        // Catch number of rows which were deleted.
        int rowsAffected = getContentResolver().delete(mCurrentPetUri, null, null);

        // Show a toast message depending on whether or not the delete was successful.
        if (rowsAffected != 0) {
            // If rows were deleted, the delete was successful and we can display a toast.
            Toast.makeText(this, R.string.editor_delete_pet_successful, Toast.LENGTH_SHORT).show();
            // Close the activity
            finish();
        } else {
            // Otherwise, then there was an error with the delete.
            Toast.makeText(this, R.string.editor_delete_pet_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        // If the pet hasn't changed, continue with handling back button press.
        if (!mPetHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User clicked "Discard" button, close the current activity.
                finish();
            }
        };

        // Show dialog that there are unsaved changes.
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new pet, hide the "Delete" menu item.
        if (mCurrentMode == INSERT_MODE) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Insert pet to the database
                savePet();
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mPetHasChanged) {
                    NavUtils.navigateUpFromSameTask(this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirm that changes
                // should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User clicked "Discard" button, navigate to parent activity.
                        NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    }
                };

                // Show a dialog that notifies the user they have unsaved changes.
                showUnsavedChangesDialog(discardButtonClickListener);
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
                        PetEntry.COLUMN_PET_BREED,
                        PetEntry.COLUMN_PET_GENDER,
                        PetEntry.COLUMN_PET_WEIGHT
                };

                // Return a new cursor loader.
                return new CursorLoader(this,   // Parent activity context
                        mCurrentPetUri,                   // Provider context URI to query
                        projection,             // Columns to include in the resulting Cursor
                        null,                   // No selection clause
                        null,                   // No selection arguments
                        null                    // Default sort order
                );
            default:
                // An invalid was passed in.
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (data.moveToFirst()) {
            // Find the columns of pet attributes that we're interested in
            int nameColumnIndex = data.getColumnIndex(PetEntry.COLUMN_PET_NAME);
            int breedColumnIndex = data.getColumnIndex(PetEntry.COLUMN_PET_BREED);
            int genderColumnIndex = data.getColumnIndex(PetEntry.COLUMN_PET_GENDER);
            int weightColumnIndex = data.getColumnIndex(PetEntry.COLUMN_PET_WEIGHT);

            // Extract out the value from the Cursor for the given column index
            String name = data.getString(nameColumnIndex);
            String breed = data.getString(breedColumnIndex);
            int gender = data.getInt(genderColumnIndex);
            int weight = data.getInt(weightColumnIndex);

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mBreedEditText.setText(breed);
            mWeightEditText.setText(Integer.toString(weight));

            // Gender is a dropdown spinner, so map the constant value from the database
            // into one of the dropdown options (0 is Unknown, 1 is Male, 2 is Female).
            // Then call setSelection() so that option is displayed on screen as the current selection.
            switch (gender) {
                case PetEntry.GENDER_MALE:
                    mGenderSpinner.setSelection(1);
                    break;
                case PetEntry.GENDER_FEMALE:
                    mGenderSpinner.setSelection(2);
                    break;
                default:
                    mGenderSpinner.setSelection(0);
                    break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Clear all data fields
        mNameEditText.setText(null);
        mBreedEditText.setText(null);
        mGenderSpinner.setSelection(PetEntry.GENDER_UNKNOWN);
        mWeightEditText.setText(null);
    }
}