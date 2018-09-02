package com.example.robert.kitafinder;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.Toast;

import com.example.robert.kitafinder.data.DistancesCalculatedTrigger;
import com.example.robert.kitafinder.data.KitaContract;
import com.example.robert.kitafinder.data.KitaProvider;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.util.ErrorDialogManager;

import static com.example.robert.kitafinder.data.Constants.COL_LOCID;
import static com.example.robert.kitafinder.data.Constants.COL_LONG;
import static com.example.robert.kitafinder.data.Constants.COL_LAT;


/**
 * Created by Robert on 10.12.2017.
 */

public class GetDistanceTask extends AsyncTask<Void,Integer,Integer> {

//    private Activity mActivity;
    private Location mSearchAddress;
    private FragmentManager mSfm;
    private ContentResolver mCr;
    private static final String TAG = GetDistanceTask.class.getSimpleName();
    private DistanceProgressDialogFragment mDf;
    private ProgressDialog mProgressDialog;

    public GetDistanceTask(FragmentManager sfm, ContentResolver cr, Location searchAddress){
        mSfm = sfm;
        mCr = cr;
        mSearchAddress = searchAddress;
    }

    @Override
    protected Integer doInBackground(Void...voids) {
        int rowsUpdated = 0;
        int max = 0;
        final Uri locationUri = KitaContract.LocationEntry.CONTENT_URI;
        String selection = KitaProvider.sLocationKitaSelection;
        String[] args = new String[] {"3"};
        //All entries, unsorted
        Cursor cursor = mCr.query(locationUri, null, selection, args, null);
        if (cursor != null) {
            max = cursor.getCount();
            cursor.moveToFirst();
        } else {
            Log.e(TAG, "Cursor == null !");
            return 0;
        }
            do{
                double kitaLat = cursor.getDouble(COL_LAT);
                double kitaLong = cursor.getDouble(COL_LONG);
                int id = cursor.getInt(COL_LOCID);

                //make a Location-object from Kita's Lat & Long
                Location kitaAddress = new Location("kitaName");
                kitaAddress.setLatitude(kitaLat);
                kitaAddress.setLongitude(kitaLong);
                //calculate distance
                float distance = kitaAddress.distanceTo(mSearchAddress);
                ContentValues contentValues = new ContentValues();
                contentValues.put(KitaContract.LocationEntry.COLUMN_DIST, distance);

                //add the distance to the kita entry in the database
                int rowUpdated =mCr.update(
                        locationUri,
                        contentValues,
                        KitaContract.LocationEntry._ID + " = ?",
                        new String[] {String.valueOf(id)});
                rowsUpdated += rowUpdated;
                publishProgress(rowsUpdated, max);
//                Log.d(TAG, "Distance calculated: " + id);
            }while (cursor.moveToNext());
        cursor.close();


        return rowsUpdated;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mDf = DistanceProgressDialogFragment.newInstance();
        mDf.show(mSfm, "dpdf_tag");
        mSfm.executePendingTransactions();

        if (mDf != null) {
//            Log.d(TAG, "progress[0]: " + progress[0]);
            mProgressDialog = (ProgressDialog) mDf.getDialog();
            Log.d(TAG, "mProgressDialog = ....");
            if (mProgressDialog != null){
                mProgressDialog.setProgressNumberFormat("%1d/%2d Kitas");
            } else Log.d(TAG, "mProgressDialog is NULL");
        } else Log.d(TAG, "Why would mDf still be NULL???");


//        Toast.makeText(mActivity, "Enfernungen werden aktualisiert...", Toast.LENGTH_LONG);
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        mProgressDialog.setMax(progress[1]);
        mProgressDialog.setProgress(progress[0]);
        super.onProgressUpdate(progress);
    }

    @Override
    protected void onPostExecute(Integer rowsUpdated) {
        if ( mDf != null) {
            Log.d(TAG, "dpdf.dismiss()");
            mDf.dismiss();
        }

//        if (rowsUpdated > 0) {
//            Toast.makeText(mActivity, rowsUpdated + " Kitas gefunden", Toast.LENGTH_LONG).show();
//        } else {
//            Toast.makeText(mActivity, "Keine Kitas gefunden", Toast.LENGTH_LONG).show();
//        }

//        Toast.makeText(mActivity, "Enfernungen aktualisiert!", Toast.LENGTH_LONG);
        EventBus.getDefault().post(new DistancesCalculatedTrigger());
        super.onPostExecute(rowsUpdated);
    }
}
