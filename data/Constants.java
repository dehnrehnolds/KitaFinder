package com.example.robert.kitafinder.data;

/**
 * Created by Robert on 19.10.2017.
 */

public class Constants {

    public static final int SUCCESS_RESULT = 0;
    public static final int FAILURE_RESULT = 1;
    public static final String PACKAGE_NAME =
            "com.google.android.gms.location.sample.locationaddress";
    public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
    public static final String RESULT_DATA_KEY = PACKAGE_NAME +
            ".RESULT_DATA_KEY";
    public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME +
            ".LOCATION_DATA_EXTRA";

    public static final String[] KITA_LIST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            KitaContract.KitaEntry._ID,
            KitaContract.KitaEntry.COLUMN_NAME,
            KitaContract.KitaEntry.COLUMN_EINRICHTUNG,
            KitaContract.KitaEntry.COLUMN_TRÄGER,
            KitaContract.KitaEntry.COLUMN_ADRESSE,
            KitaContract.KitaEntry.COLUMN_ORT,
            KitaContract.KitaEntry.COLUMN_TELEFON,
            KitaContract.KitaEntry.COLUMN_EMAIL,
            KitaContract.KitaEntry.COLUMN_WEB,
            KitaContract.KitaEntry.COLUMN_ÖFFNET,
            KitaContract.KitaEntry.COLUMN_SCHLIEßT,
            KitaContract.KitaEntry.COLUMN_ÖFFNUNGSD,
            KitaContract.KitaEntry.COLUMN_ÖFFNUNGSZ,
            KitaContract.KitaEntry.COLUMN_AUFNAHMEA,
            KitaContract.KitaEntry.COLUMN_FREMDSP,
            KitaContract.KitaEntry.COLUMN_FAV,
    };

    // These indices are tied to KITA_LIST_COLUMNS.  If KITA_LIST_COLUMNS changes, these
    // must change.
    public static final int COL_KITAID = 0;
    public static final int COL_NAME = 1;
    public static final int COL_EINRICHTUNG = 2;
    public static final int COL_TRÄGER = 3;
    public static final int COL_ADRESSE = 4;
    public static final int COL_ORT = 5;
    public static final int COL_TELEFON = 6;
    public static final int COL_EMAIL = 7;
    public static final int COL_WEB = 8;
    public static final int COL_ÖFFNET = 9;
    public static final int COL_SCHLIEßT = 10;
    public static final int COL_ÖFFNUNGSD = 11;
    public static final int COL_ÖFFNUNGSZ = 12;
    public static final int COL_AUFNAHMEA = 13;
    public static final int COL_FREMDSP = 14;
    public static final int COL_FAV = 15;

    public static final String[] LOCATIONS_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            KitaContract.LocationEntry._ID,
            KitaContract.LocationEntry.COLUMN_TYPE,
            KitaContract.LocationEntry.COLUMN_LAT,
            KitaContract.LocationEntry.COLUMN_LONG,
            KitaContract.LocationEntry.COLUMN_DIST,
            KitaContract.LocationEntry.COLUMN_MAPST,
            KitaContract.LocationEntry.COLUMN_FK_KITA_ID
    };

    public static final int COL_LOCID = 0;
    public static final int COL_TYPE = 1;
    public static final int COL_LAT = 2;
    public static final int COL_LONG = 3;
    public static final int COL_DIST = 4;
    public static final int COL_MAPST = 5;
    public static final int COL_FK_KITA_ID = 6;

}