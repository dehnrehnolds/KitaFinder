package com.example.robert.kitafinder;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.example.robert.kitafinder.data.KitaContract;
import com.opencsv.CSVReader;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Robert on 20.10.2017.
 */

public class FetchDbFromCsv  extends AsyncTask<Void, Integer, Integer> {

    private static final String TAG = FetchDbFromCsv.class.getSimpleName();

    private Context mContext;

    public FetchDbFromCsv(Context context){
        mContext = context;
    }

    protected Integer doInBackground(Void...voids) {
        int kitaRows = 0;
        CSVReader reader = null;
        ArrayList<ContentValues> cvVectorKita = new ArrayList<ContentValues>();
        ArrayList<ContentValues> cvVectorLoc = new ArrayList<ContentValues>();
        try{
            reader = new CSVReader(new InputStreamReader(mContext.getAssets().open("Kitaliste.csv")),';');
            String[] nextLine;
            int lineNr = 0;
            while ((nextLine = reader.readNext()) != null) {

                ContentValues contentVKita = new ContentValues();
                ContentValues contentVLoc = new ContentValues();
                lineNr++;
                // nextLine[] is an array of values from the line
                Log.d(TAG,"Row Nr: " +lineNr + "  Columns in this row: " +nextLine.length);
                Log.d(TAG,"Spalte0(ID): " +nextLine[0]+"  Spalte1(Name): " +nextLine[1]);

                contentVKita.put(KitaContract.KitaEntry.COLUMN_NAME, nextLine[1]);
                contentVKita.put(KitaContract.KitaEntry.COLUMN_EINRICHTUNG, nextLine[2]);
                contentVKita.put(KitaContract.KitaEntry.COLUMN_TRÄGER, nextLine[3]);
                contentVKita.put(KitaContract.KitaEntry.COLUMN_ADRESSE, nextLine[4]);
                contentVKita.put(KitaContract.KitaEntry.COLUMN_ORT, nextLine[5]);
                contentVKita.put(KitaContract.KitaEntry.COLUMN_TELEFON, nextLine[8]);
                contentVKita.put(KitaContract.KitaEntry.COLUMN_EMAIL, nextLine[9]);
                contentVKita.put(KitaContract.KitaEntry.COLUMN_WEB, nextLine[10]);
                contentVKita.put(KitaContract.KitaEntry.COLUMN_ÖFFNET, nextLine[11]);
                contentVKita.put(KitaContract.KitaEntry.COLUMN_SCHLIEßT, nextLine[12]);
                contentVKita.put(KitaContract.KitaEntry.COLUMN_ÖFFNUNGSD, nextLine[13]);
                contentVKita.put(KitaContract.KitaEntry.COLUMN_ÖFFNUNGSZ, nextLine[14]);
                contentVKita.put(KitaContract.KitaEntry.COLUMN_AUFNAHMEA, nextLine[15]);
                contentVKita.put(KitaContract.KitaEntry.COLUMN_FREMDSP, nextLine[16]);
                contentVKita.put(KitaContract.KitaEntry.COLUMN_FAV,
                        mContext.getString(R.sting.status_no_fav));

                contentVLoc.put(KitaContract.LocationEntry.COLUMN_TYPE, 3); //all csv-entries = kita
                contentVLoc.put(KitaContract.LocationEntry.COLUMN_LAT, Float.parseFloat(nextLine[6]));
                contentVLoc.put(KitaContract.LocationEntry.COLUMN_LONG, Float.parseFloat(nextLine[7]));
                contentVLoc.put(KitaContract.LocationEntry.COLUMN_DIST, "-1");
                contentVLoc.put(KitaContract.LocationEntry.COLUMN_MAPST, "0"); // all are invisible

                cvVectorKita.add(contentVKita);
                cvVectorLoc.add(contentVLoc);
            }

            if (cvVectorKita.isEmpty()) {
                Log.d(TAG, "cvList (kita) ist empty");
                return 0;
            }
            else {
                ContentValues[] cvArrayKita = new ContentValues[cvVectorKita.size()];
                Log.d(TAG,"cvVectorKita.size(): "+cvVectorKita.size());
                cvVectorKita.toArray(cvArrayKita);
                Log.d(TAG,"cvArray.length: " +cvArrayKita.length);
                kitaRows = mContext.getContentResolver().bulkInsert(KitaContract.KitaEntry.CONTENT_URI,
                        cvArrayKita);
            }

            if (cvVectorLoc.isEmpty()) {
                Log.d(TAG, "cvList (loc) ist empty");
                return 0;
            }
            else {
                ContentValues[] cvArrayLoc = new ContentValues[cvVectorLoc.size()];
                Log.d(TAG,"cvVectorLoc.size(): "+cvVectorLoc.size());
                cvVectorLoc.toArray(cvArrayLoc);
                Log.d(TAG,"cvArray.length: " +cvArrayLoc.length);
                return mContext.getContentResolver().bulkInsert(KitaContract.LocationEntry.CONTENT_URI,
                        cvArrayLoc) + kitaRows ;
            }




        }catch (IOException e){
            Log.e(TAG,e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }
        return 0;
    }

    protected void onProgressUpdate(Integer... progress) {

}

    protected void onPostExecute(Integer result) {
        SearchActivity.mInserted = result;
    }
}
