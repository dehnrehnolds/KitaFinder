package com.example.robert.kitafinder.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.Time;
import android.util.Log;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;

/**
 * Created by Robert on 19.10.2017.
 */

public class KitaContract {

    public static final String TAG = KitaContract.class.getSimpleName();
    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "com.example.robert.kitafinder";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths (appended to base content URI for possible URI's)
    public static final String PATH_KITA = "kita";
    public static final String PATH_LOCATION = "location";
    public static final String LOCATION_OPTION = "location option";



    /* Inner class that defines the table contents of the kitalist table */
    public static final class KitaEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_KITA).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_KITA;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_KITA;

        public static final String TABLE_NAME = "kitaliste";

//        // Column with the primary key of kitaliste
//        public static final String COLUMN_KITA_ID = "_id";
        // Name of the Kita
        public static final String COLUMN_NAME = "name";
        // Kind of organisational structure of the Kita
        public static final String COLUMN_EINRICHTUNG = "einrichtung";

        // Company running the Kita
        public static final String COLUMN_TRÄGER = "träger";

        // Street and Nr of the property of the Kita
        public static final String COLUMN_ADRESSE = "adresse";

        //Zip-code and City of the Kita
        public static final String COLUMN_ORT = "ort";

        // Telefon number
        public static final String COLUMN_TELEFON = "telefon";

        // Email address
        public static final String COLUMN_EMAIL = "email";

        // Homepage
        public static final String COLUMN_WEB = "web";

        // Time when kita is opening in the morning in float (8:30 -> 8.5f)
        public static final String COLUMN_ÖFFNET = "öffnet";

        // Time when kita is closing in float (17:30 -> 17.5f)
        public static final String COLUMN_SCHLIEßT = "schließt";

        // Opening hours per day in float (
        public static final String COLUMN_ÖFFNUNGSD = "öffnungsdauer";

        // Opening hours in format "09:00-18:30"
        public static final String COLUMN_ÖFFNUNGSZ = "öffnungszeit";

        // Least age of Kids to be accepted in months
        public static final String COLUMN_AUFNAHMEA = "aufnahme";

        // foreign languages spoken in the Kita (format: deutsch-xxx, e.g.: deutsch-spanisch)
        public static final String COLUMN_FREMDSP = "fremdsprache";

        //set "true" if row is sset as favourite
        public static final String COLUMN_FAV = "favorit";

        public static Uri buildKitaUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        // locationOption can be "0" - invisible , "1" - light, "2" - bold or "3" - home
        public static Uri buildKitaUriWithLocation(String locationOption){
            return CONTENT_URI.buildUpon().appendPath(locationOption)
                    .build();
        }

        public static String getLocationOptionFromUri(Uri uri){
            return uri.getPathSegments().get(1);
        }


    }

    public static final class LocationEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATION).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;

        public static final String TABLE_NAME = "locations";

        // Column with the primary key of kitaliste
//        public static final String COLUMN_KITA_ID = "_id";

        // Type of localition; 3 = home, 0 = kita
        public static final String COLUMN_TYPE = "type";
        // Lat coord of the location
        public static final String COLUMN_LAT = "latitude";
        // Long coord of the location
        public static final String COLUMN_LONG = "longitude";
        // Distace to latest search address
        public static final String COLUMN_DIST = "distance";
        // The locactions status on the map; 0 = invsbl, 1 = light, 2 = bold, 3 = home
        public static final String COLUMN_MAPST = "mapstatus";
        // Foreign Key of the Kita, if home = -3
        public static final String COLUMN_FK_KITA_ID = "fk_kita_id";



        public static Uri buildLocationUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }


    }


}
