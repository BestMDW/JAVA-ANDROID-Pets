package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import com.example.android.pets.data.PetContract.PetEntry;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

/**
 * {@link ContentProvider} for Pets app.
 */
public class PetProvider extends ContentProvider {
    /** Tag for the log messages */
    private static final String LOG_TAG = PetProvider.class.getSimpleName();

    /** URI matcher code for content URI for the pets table. */
    private static final int PETS = 100;
    /** URI matcher code for content URI for a single pet in the pets table. */
    private static final int PET_ID = 101;

    /** {@link UriMatcher} object to match a content URI to a corresponding code. */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS, PETS);
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS + "/#", PET_ID);
    }

    /** Database helper object */
    public PetDbHelper mDbHelper;


    /**
     * Initialize the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {
        mDbHelper = new PetDbHelper(getContext());

        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Get readable database
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Create {@link Cursor} object for storing data from Pets database.
        Cursor cursor;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                    cursor = db.query(PetEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case PET_ID:
                    selection = PetEntry._ID + "=?";
                    selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                    cursor = db.query(PetEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Close connection with SQLite database.
        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                    return insertPet(uri, values);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a pet into the database with the given content values.
     * @return The new content URI for that specific row in the database.
     */
    private Uri insertPet(Uri uri, ContentValues values) {
        // Check if name is not empty.
        String nameString = values.getAsString(PetEntry.COLUMN_PET_NAME);
        if (nameString == null) {
            throw new IllegalArgumentException("Pet requires a name.");
        }

        // Check if gender value is valid
        Integer gender = values.getAsInteger(PetEntry.COLUMN_PET_GENDER);
        if (gender == null || !PetEntry.isValidGender(gender)) {
            throw new IllegalArgumentException("Gender is not valid.");
        }

        // Check if weight is not null
        Integer weight = values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
        if (weight == null) {
            throw new IllegalArgumentException("Pet requires a weight.");
        }

        // Get writable database for insert new pet.
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Insert pet into database
        long lastPetId = db.insert(PetEntry.TABLE_NAME, null, values);
        // If the lastPetId is -1, then the insertion failed. Log an error and return null.
        if (lastPetId == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the pet content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID appended to the end of it.
        return ContentUris.withAppendedId(PetEntry.CONTENT_URI, lastPetId);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        //Get writable database.
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Match the URI with possible options.
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        switch (match) {
            case PETS:
                // Delete all rows that match the selection and selection args.
                rowsDeleted = db.delete(PetEntry.TABLE_NAME, selection, selectionArgs);
                if (rowsDeleted != 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return rowsDeleted;
            case PET_ID:
                // Delete a single row given by the ID in the URI.
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = db.delete(PetEntry.TABLE_NAME, selection, selectionArgs);
                if (rowsDeleted != 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return rowsDeleted;
            default:
                throw new IllegalArgumentException("Delete is not supported for " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Match the URI with possible options.
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;
        switch (match) {
            case PETS:
                // Update pets and return number of updated rows.
                rowsUpdated = updatePet(values, selection, selectionArgs);
                if (rowsUpdated != 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return rowsUpdated;
            case PET_ID:
                // Extract out the ID from the URI, and select that row for update.
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                // Update pet and return number of updated rows.
                rowsUpdated = updatePet(values, selection, selectionArgs);
                if (rowsUpdated != 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                return rowsUpdated;
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updatePet(ContentValues values, String selection, String[] selectionArgs) {
        // If the {@link PetEntry#COLUMN_PET_NAME} key is present,
        // check that the name value is not null.
        if (values.containsKey(PetEntry.COLUMN_PET_NAME)) {
            String name = values.getAsString(PetEntry.COLUMN_PET_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Pet requires a name");
            }
        }

        // If the {@link PetEntry#COLUMN_PET_GENDER} key is present,
        // check that the gender value is valid.
        if (values.containsKey(PetEntry.COLUMN_PET_GENDER)) {
            Integer gender = values.getAsInteger(PetEntry.COLUMN_PET_GENDER);
            if (gender == null || !PetEntry.isValidGender(gender)) {
                throw new IllegalArgumentException("Pet requires valid gender");
            }
        }

        // If the {@link PetEntry#COLUMN_PET_WEIGHT} key is present,
        // check that the weight value is valid.
        if (values.containsKey(PetEntry.COLUMN_PET_WEIGHT)) {
            // Check that the weight is greater than or equal to 0 kg
            Integer weight = values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
            if (weight != null && weight < 0) {
                throw new IllegalArgumentException("Pet requires valid weight");
            }
        }

        // If there are no values to update, then don't try to update the database.
        if (values.size() == 0) {
            return 0;
        }

        // Open writable connection with database for update pets data.
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Update table and return number of updated rows.
        return  db.update(PetEntry.TABLE_NAME, values, selection, selectionArgs);
    }
}
