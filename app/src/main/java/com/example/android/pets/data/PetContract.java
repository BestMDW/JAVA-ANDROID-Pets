package com.example.android.pets.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * API Contract for the Pets app.
 */
public final class PetContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private PetContract() {}

    /** {@link String} Constant of Content Authority */
    public static final String CONTENT_AUTHORITY = "com.example.android.pets";

    /** Make {@link Uri} object of Content Authority URI */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /** Possible path (appended to base content URI for possible URI's). */
    public static final String PATH_PETS = "pets";

    /**
     * Inner class that defines constant values for the pets database table.
     */
    public static class PetEntry implements BaseColumns {
        /** The content URI to access the pet data in the provider. */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PETS);

        /** The MIME type of the {@link #CONTENT_URI} for a list of pets. */
        public static final String CONTENT_LIST_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PETS;

        /** The MIME type of the {@link #CONTENT_URI} for a single pet. */
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PETS;

        /** Table name in SQLite */
        public static final String TABLE_NAME = "Pets";

        /** Columns names in Pets table */
        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_PET_NAME = "name";
        public static final String COLUMN_PET_BREED = "breed";
        public static final String COLUMN_PET_GENDER = "gender";
        public static final String COLUMN_PET_WEIGHT = "weight";

        /** Possible states for gender */
        public static final int GENDER_UNKNOWN = 0;
        public static final int GENDER_MALE = 1;
        public static final int GENDER_FEMALE = 2;

        /** Return true if gender is unknown, male or female. False otherwise. */
        public static boolean isValidGender(int gender) {
            if (gender != GENDER_UNKNOWN && gender != GENDER_MALE && gender != GENDER_FEMALE) { return false; }
            return true;
        }
    }
}
