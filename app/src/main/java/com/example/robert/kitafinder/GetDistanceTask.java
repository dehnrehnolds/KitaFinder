package com.example.robert.kitafinder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.example.robert.kitafinder.data.DistancesCalculatedTrigger;
import com.example.robert.kitafinder.data.KitaContract;
import com.example.robert.kitafinder.data.KitaProvider;
import com.example.robert.kitafinder.data.RefreshTrigger;

import org.greenrobot.eventbus.EventBus;

import static com.example.robert.kitafinder.data.Constants.COL_LOCID;
import static com.example.robert.kitafinder.data.Constants.COL_LONG;
import static com.example.robert.kitafinder.data.Constants.COL_LAT;


/**
 * Created by Robert on 10.12.2017.
 */

public class GetDistanceTask extends AsyncTask<Void,Integer,Integer> {

    private ProgressDialog mProgressDialog;
    private Location mSearchAddress;
    protected Cursor mCursor;
    Activity activity;
    private static final String TAG = GetDistanceTask.class.getSimpleName();

    public GetDistanceTask(Activity activity){
        this.activity = activity;
        mProgressDialog = new ProgressDialog(activity);
        mSearchAddress = activity.getIntent().getParcelableExtra("address");
    }

    @Override
    protected Integer doInBackground(Void...voids) {
        int rowsUpdated = 0;
        int max = 0;
        final Uri locationUri = KitaContract.LocationEntry.CONTENT_URI;
        String selection = KitaProvider.sLocationKitaSelection;
        String[] args = new String[] {"3"};
        //All entries, unsorted
        mCursor = activity.getContentResolver()
                .query(locationUri, null, selection, args, null);
        if (mCursor != null)
            max = mCursor.getCount();
        mCursor.moveToFirst();
            do{
                double kitaLat = mCursor.getDouble(COL_LAT);
                double kitaLong = mCursor.getDouble(COL_LONG);
                int id = mCursor.getInt(COL_LOCID);
                Log.d(TAG, "KitaID: "+id);

                //make a Location-object from Kita's Lat & Long
                Location kitaAddress = new Location("kitaName");
                kitaAddress.setLatitude(kitaLat);
                kitaAddress.setLongitude(kitaLong);
                //calculate distance
                float distance = kitaAddress.distanceTo(mSearchAddress);
                ContentValues contentValues = new ContentValues();
                contentValues.put(KitaContract.LocationEntry.COLUMN_DIST, distance);

                //add the distance to the kita entry in the database
                int rowUpdated = activity.getContentResolver().update(
                        locationUri,
                        contentValues,
                        KitaContract.LocationEntry._ID + " = ?",
                        new String[] {String.valueOf(id)});
                rowsUpdated += rowUpdated;
                publishProgress(rowsUpdated, mCursor.getCount());
            }while (mCursor.moveToNext());
        mCursor.close();


        return rowsUpdated;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // instantiate ProgressDialog
        mProgressDialog.setMessage("Suche Kitas in der Nähe...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setMax(progress[1]);
        mProgressDialog.setProgress(progress[0]);
        super.onProgressUpdate(progress);
    }

    @Override
    protected void onPostExecute(Integer rowsUpdated) {
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        if (rowsUpdated > 0) {
            Toast.makeText(activity, rowsUpdated + " Kitas gefunden", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(activity, "Keine Kitas gefunden", Toast.LENGTH_LONG).show();
        }

        EventBus.getDefault().post(new DistancesCalculatedTrigger());
        super.onPostExecute(rowsUpdated);
    }
}
