package com.example.robert.kitafinder.data;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.robert.kitafinder.R;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Created by Robert on 19.10.2017.
 */

public class KitaProvider extends ContentProvider {
    private static final String TAG = KitaProvider.class.getSimpleName();
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private KitaDbHelper mOpenHelper;

    static final int KITA = 100;
    static final int KITA_WITH_FILTER = 101;
    static final int KITA_WITH_LOCATION = 102;
    static final int LOCATION = 103;

    private static final SQLiteQueryBuilder sKitaQueryBuilder;

    static{
        sKitaQueryBuilder = new SQLiteQueryBuilder();
        sKitaQueryBuilder.setTables(KitaContract.KitaEntry.TABLE_NAME);
    }

    // ------  selections for filter queries ------

    //aufnahmealter <= ?
    public static final String sAufnahmealterSelection =
            KitaContract.KitaEntry.COLUMN_AUFNAHMEA + " <= ? ";

    //opening in the morning <= ?
    public static final String sÖffnetSelection =
            KitaContract.KitaEntry.COLUMN_ÖFFNET + " <= ? ";

    //closing in the evening >= ?
    public static final String sSchließtSelection =
            KitaContract.KitaEntry.COLUMN_SCHLIEßT + " >= ? ";

    //opening hours during the day >= ?
    public static final String sOpeningHoursSelection =
            KitaContract.KitaEntry.COLUMN_ÖFFNUNGSD + " >= ? ";

    //fremdsprache = ?
    public static final String sFremdspracheSelection =
            KitaContract.KitaEntry.COLUMN_FREMDSP + " LIKE ? ";

    //distanz = ?
    public static final String sDistanzSelection =
            KitaContract.LocationEntry.COLUMN_DIST + " <= ? ";

    public static final String sLocationVisibleSelection =
            KitaContract.LocationEntry.COLUMN_MAPST + " = ? OR " +
                    KitaContract.LocationEntry.COLUMN_MAPST + " = ?";

    public static final String sLocationKitaSelection =
            //KitaContract.LocationEntry.TABLE_NAME + "." +
                    KitaContract.LocationEntry.COLUMN_MAPST + " <= ?";

    public static final String sLocationOptionSelection =
            KitaContract.LocationEntry.COLUMN_MAPST + " = ?";

    private static final SQLiteQueryBuilder sKitaByLocationSettingQueryBuilder;

    // ------  inner join of Kitaliste with locations  ---------

    static{
        sKitaByLocationSettingQueryBuilder = new SQLiteQueryBuilder();

        //This is an inner join which looks like
        //kitaliste INNER JOIN locations ON kitaliste.locationId = locations._id
        sKitaByLocationSettingQueryBuilder.setTables(
                KitaContract.LocationEntry.TABLE_NAME + " INNER JOIN " +
                        KitaContract.KitaEntry.TABLE_NAME +
                        " ON " + KitaContract.LocationEntry.TABLE_NAME +
                        "." + KitaContract.LocationEntry.COLUMN_FK_KITA_ID +
                        " = " + KitaContract.KitaEntry.TABLE_NAME +
                        "." + KitaContract.KitaEntry._ID);

        Log.d(TAG, "Inner join: " + sKitaByLocationSettingQueryBuilder.toString() );
    }


    private Cursor getKitaByLocationSetting(Uri uri, String[] projection, String selection,
                                            String[] selectionArgs, String sortOrder) {

        String locationOption = KitaContract.KitaEntry.getLocationOptionFromUri(uri);
        String[] newSelectionArgs;
        String[] kitaSelectionArgs = new String[] {"2"};

        // if locationOption == "all" do not use any selection
        if (locationOption.equals(getContext().getString(R.string.location_option_all))) {
            Log.d(TAG, "location_option_all");
            if (selection != null) {
                Log.d(TAG, "selection != null");
                if (selection.equals("")) {
                    Log.d(TAG, "selection.equals('')");
                    selection = sLocationKitaSelection;
                    newSelectionArgs = kitaSelectionArgs;
                } else {
                    Log.d(TAG, "selection has some text");
                    selection = selection + " AND " + sLocationKitaSelection;
                    newSelectionArgs = ArrayUtils.addAll(selectionArgs, kitaSelectionArgs);
                }
            }
            else {
                Log.d(TAG, "selection == null");
                selection = sLocationKitaSelection;
                newSelectionArgs = kitaSelectionArgs;
            }

            Log.d(TAG, "selection= " +selection);
            for (String args:newSelectionArgs){
                Log.d(TAG, "newSelectionArgs:" +args);
            }
            return sKitaByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                    projection,
                    selection,
                    newSelectionArgs,
                    null,
                    null,
                    sortOrder
            );
        }
        // else use the locationOption form the Uri (either "0" - invisible, "1" - light, "2" - bold or "3" - home) as a selection
        else {
            Log.d(TAG, "ELSE--");
            return sKitaByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                    projection,
                    sLocationOptionSelection,
                    new String[]{locationOption},
                    null,
                    null,
                    sortOrder
            );

        }
    }

    /*
        Students: Here is where you need to create the UriMatcher. This UriMatcher will
        match each URI to the WEATHER, WEATHER_WITH_LOCATION, WEATHER_WITH_LOCATION_AND_DATE,
        and LOCATION integer constants defined above.  You can test this by uncommenting the
        testUriMatcher test within TestUriMatcher.
     */
    static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = KitaContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, KitaContract.PATH_LOCATION, LOCATION);
        matcher.addURI(authority, KitaContract.PATH_KITA, KITA);
//        matcher.addURI(authority, KitaContract.PATH_KITA + "/*", KITA_WITH_FILTER);
        matcher.addURI(authority, KitaContract.PATH_KITA + "/*", KITA_WITH_LOCATION);

        return matcher;
    }

    //create a new WeatherDbHelper for later use
    @Override
    public boolean onCreate() {
        mOpenHelper = new KitaDbHelper(getContext());
        return true;
    }

    //code the getType function that uses the UriMatcher
    @Override
    public String getType(@NonNull Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            // Student: Uncomment and fill out these two cases
            case LOCATION:
                return KitaContract.KitaEntry.CONTENT_TYPE;
            case KITA:
                return KitaContract.KitaEntry.CONTENT_TYPE;
//            case KITA_WITH_FILTER:
//                return KitaContract.KitaEntry.CONTENT_TYPE;
            case KITA_WITH_LOCATION:
                return KitaContract.KitaEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "kita"
            case LOCATION: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        KitaContract.LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case KITA:
            {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        KitaContract.KitaEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "kita/*"
//            case KITA_WITH_FILTER: {
//                retCursor = getKitaByAufnahmealter(uri, projection, sortOrder);
//                break;
//            }
            // "kita/*"
            case KITA_WITH_LOCATION: {
                retCursor = getKitaByLocationSetting(uri, projection, selection, selectionArgs, sortOrder);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    /*
        Student: Add the ability to insert Locations to the implementation of this function.
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case KITA: {
                long _id = db.insert(KitaContract.KitaEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = KitaContract.KitaEntry.buildKitaUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case LOCATION: {
                long _id = db.insert(KitaContract.LocationEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = KitaContract.LocationEntry.buildLocationUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if (null == selection) selection = "1";
        switch (match) {
            case KITA: {
                rowsDeleted = db.delete(
                        KitaContract.KitaEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case LOCATION: {
                rowsDeleted = db.delete(
                        KitaContract.LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            default: rowsDeleted = 0;
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(
            @NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;
        switch (match){
            case KITA: {
                rowsUpdated = db.update(KitaContract.KitaEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            }
            case LOCATION: {
                rowsUpdated = db.update(KitaContract.LocationEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            }
            default:rowsUpdated = 0;
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case KITA: {
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(KitaContract.KitaEntry.TABLE_NAME,
                                null,
                                value
                        );
                        Log.d(TAG, "_id= " + _id);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            }
            case LOCATION: {
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(KitaContract.LocationEntry.TABLE_NAME,
                                null,
                                value
                        );
                        Log.d(TAG, "_id= " + _id);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            }
            default:
                return super.bulkInsert(uri, values);
        }
    }

    // You do not need to call this method. This is a method specifically to assist the testing
    // framework in running smoothly. You can read more at:
    // http://developer.android.com/reference/android/content/ContentProvider.html#shutdown()
    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }
}
