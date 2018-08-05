package com.example.robert.kitafinder;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.example.robert.kitafinder.data.KitaContract;
import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by Robert on 20.10.2017.
 */

public class FetchDbFromCsv  extends AsyncTask<Void, Integer, Integer> {

    private static final String TAG = FetchDbFromCsv.class.getSimpleName();

    private Context mContext;
    private ProgressDialog mProgressDialog;

    public FetchDbFromCsv(Context context){
        mContext = context;
        mProgressDialog = new ProgressDialog(context);
    }

    protected Integer doInBackground(Void...voids) {
        int kitaRows = 0;
        CSVReader reader = null;
        ArrayList<ContentValues> cvVectorKita = new ArrayList<ContentValues>();
        ArrayList<ContentValues> cvVectorLoc = new ArrayList<ContentValues>();
        int kitasDeleted = mContext.getContentResolver().delete(
                KitaContract.KitaEntry.CONTENT_URI,
                null,
                null
        );
        int locationsDeleted = mContext.getContentResolver().delete(
                KitaContract.LocationEntry.CONTENT_URI,
                null,
                null
        );

        Log.d(TAG, "Locations deleted: " + locationsDeleted +
                "  ....  Kitas deleted: " + kitasDeleted);

        try{
            reader = new CSVReader(new InputStreamReader(mContext.getAssets()
                    .open("Kitaliste.csv")),';');
            String[] nextLine;
            int lineNr = 0;
            while ((nextLine = reader.readNext()) != null) {

                ContentValues contentVKita = new ContentValues();
                ContentValues contentVLoc = new ContentValues();
                lineNr++;
                // nextLine[] is an array of values from the line
                Log.d(TAG,"Row Nr: " +lineNr + "  Columns in this row: " +nextLine.length);
                Log.d(TAG,"Spalte0(ID): " +nextLine[0]+"  Spalte1(Name): " +nextLine[1]);

                contentVKita.put(KitaContract.KitaEntry._ID, lineNr);
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
                        mContext.getString(R.string.status_not_fav));

                contentVLoc.put(KitaContract.LocationEntry.COLUMN_TYPE, 0); //all csv-entries = kita
                contentVLoc.put(KitaContract.LocationEntry.COLUMN_LAT, Float.parseFloat(nextLine[6]));
                contentVLoc.put(KitaContract.LocationEntry.COLUMN_LONG, Float.parseFloat(nextLine[7]));
                contentVLoc.put(KitaContract.LocationEntry.COLUMN_DIST, "-1");
                contentVLoc.put(KitaContract.LocationEntry.COLUMN_MAPST, "0"); // all are invisible
                contentVLoc.put(KitaContract.LocationEntry.COLUMN_FK_KITA_ID, lineNr);
                cvVectorKita.add(contentVKita);
                cvVectorLoc.add(contentVLoc);

                publishProgress(lineNr, reader.getMultilineLimit());
            }

            if (cvVectorKita.isEmpty()) {
                Log.d(TAG, "cvList (kita) ist empty");
                return 0;
            }
            else {
                ContentValues[] cvArrayKita = new ContentValues[cvVectorKita.size()];
                Log.d(TAG,"cvVectorKita.size(): "+cvVectorKita.size());
                cvVectorKita.toArray(cvArrayKita);
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

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog.setMessage("Aktualisiere Kita-Datenbank...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    protected void onProgressUpdate(Integer... progress) {
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setMax(progress[1]);
        mProgressDialog.setProgress(progress[0]);
        super.onProgressUpdate(progress);
}

    protected void onPostExecute(Integer result) {
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        if (result > 0) {
            Toast.makeText(mContext, result + " Datenbankeinträge aktualisiert", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(mContext, "Datenbank nicht gefunden", Toast.LENGTH_LONG).show();
        }

        super.onPostExecute(result);
        SearchActivity.mInserted = result;
    }
}
