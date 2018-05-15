package com.example.robert.kitafinder.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Robert on 19.10.2017.
 */

public class KitaDbHelper extends SQLiteOpenHelper {

    private static final String TAG = KitaDbHelper.class.getSimpleName();
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 31;

    static final String DATABASE_NAME = "kita.db";

    public KitaDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Create a table to hold Kitas

        final String SQL_CREATE_KITA_TABLE = "CREATE TABLE " +
                KitaContract.KitaEntry.TABLE_NAME + " (" +

                KitaContract.KitaEntry._ID + " INTEGER PRIMARY KEY UNIQUE NOT NULL," +

                KitaContract.KitaEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                KitaContract.KitaEntry.COLUMN_EINRICHTUNG + " TEXT NOT NULL, " +
                KitaContract.KitaEntry.COLUMN_TRÄGER + " TEXT NOT NULL, " +
                KitaContract.KitaEntry.COLUMN_ADRESSE + " TEXT NOT NULL," +

                KitaContract.KitaEntry.COLUMN_ORT + " TEXT NOT NULL, " +
                KitaContract.KitaEntry.COLUMN_TELEFON + " TEXT NOT NULL, " +

                KitaContract.KitaEntry.COLUMN_EMAIL + " TEXT NOT NULL, " +
                KitaContract.KitaEntry.COLUMN_WEB + " TEXT NOT NULL, " +
                KitaContract.KitaEntry.COLUMN_ÖFFNET + " REAL NOT NULL, " +
                KitaContract.KitaEntry.COLUMN_SCHLIEßT + " REAL NOT NULL, " +
                KitaContract.KitaEntry.COLUMN_ÖFFNUNGSD + " REAL NOT NULL, " +
                KitaContract.KitaEntry.COLUMN_ÖFFNUNGSZ + " TEXT NOT NULL, " +
                KitaContract.KitaEntry.COLUMN_AUFNAHMEA + " INT NOT NULL, " +
                KitaContract.KitaEntry.COLUMN_FREMDSP + " TEXT NOT NULL, " +
                KitaContract.KitaEntry.COLUMN_FAV + " TEXT NOT NULL, " +

                "UNIQUE (" + KitaContract.KitaEntry.COLUMN_NAME + ", " +
                KitaContract.KitaEntry.COLUMN_EMAIL + ") ON CONFLICT REPLACE);";

        final String SQL_CREATE_LOCATION_TABLE = "CREATE TABLE " +
                KitaContract.LocationEntry.TABLE_NAME + " (" +

                KitaContract.LocationEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                KitaContract.LocationEntry.COLUMN_TYPE + " INTEGER NOT NULL, " +
                KitaContract.LocationEntry.COLUMN_LAT + " REAL NOT NULL, " +
                KitaContract.LocationEntry.COLUMN_LONG + " REAL NOT NULL, " +
                KitaContract.LocationEntry.COLUMN_DIST + " REAL NOT NULL, " +
                KitaContract.LocationEntry.COLUMN_MAPST + " INTEGER NOT NULL, " +
                KitaContract.LocationEntry.COLUMN_FK_KITA_ID + " INT NOT NULL, " +
                "FOREIGN KEY (" + KitaContract.LocationEntry.COLUMN_FK_KITA_ID +
                ") REFERENCES " + KitaContract.KitaEntry.TABLE_NAME + "(" +
                KitaContract.KitaEntry._ID + "));" ;


        sqLiteDatabase.execSQL(SQL_CREATE_KITA_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_LOCATION_TABLE);
        Log.d(TAG, "Tabelle erstellt");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + KitaContract.KitaEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + KitaContract.LocationEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
