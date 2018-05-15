package com.example.robert.kitafinder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.robert.kitafinder.data.DetailTrigger;
import com.example.robert.kitafinder.data.KitaContract;
import com.example.robert.kitafinder.data.KitaProvider;


import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.example.robert.kitafinder.data.Constants.IJ_COL_AUFNAHMEA;
import static com.example.robert.kitafinder.data.Constants.IJ_COL_DIST;
import static com.example.robert.kitafinder.data.Constants.IJ_COL_FREMDSP;
import static com.example.robert.kitafinder.data.Constants.IJ_COL_KITAID;
import static com.example.robert.kitafinder.data.Constants.IJ_COL_NAME;
import static com.example.robert.kitafinder.data.Constants.IJ_COL_ÖFFNUNGSZ;


/**
 * Created by Robert on 23.10.2017.
 */


public class ListAdapter extends RecyclerViewCursorAdapter<ListAdapter.OverviewViewHolder>
{
    public interface ClickListener {

        void onPositionClicked(int position, int kitaId);

        void onLongClicked(int position, int kitaId);
    }

    private static final String TAG = ListAdapter.class.getSimpleName();
    private final Context mContext;
    private final ClickListener mListener;
    private final float mSearchRadius;
    private int viewNr;

    // Specify the columns we need.

    public ListAdapter(Context context, String filter, ClickListener listener)
    {
        super(null);
        viewNr = 0;
        mContext = context;
        mListener = listener;

        //open filter preferences
        SharedPreferences sharedPref = mContext.getSharedPreferences("filter", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        mSearchRadius = (float) sharedPref.getInt(mContext.getString(R.string.search_radius), -1);

        //get the filter settings for each filter
        float searchRadius = (float) sharedPref.getInt(mContext.getString(R.string.search_radius), -1);
        final int minAge = sharedPref.getInt(mContext.getString(R.string.minimum_age),-1);
        float morningTime = sharedPref.getFloat(mContext.getString(R.string.morning_time), -1.0f);
        float eveningTime = sharedPref.getFloat(mContext.getString(R.string.evening_time), -1.0f);
        float openingHours = sharedPref.getFloat(mContext.getString(R.string.opening_hours),-1.0f);
        String language = sharedPref.getString(mContext.getString(R.string.language), "disable");
        Log.d(TAG, "------------  Filter Settings (ListAdapter) --------------");
        Log.d(TAG,"    searchRadius: " + searchRadius);
        Log.d(TAG, "    minAge: " + minAge);
        Log.d(TAG,"    morningTime: " + morningTime);
        Log.d(TAG,"    eveningTime: " + eveningTime);
        Log.d(TAG,"    openingHours: " + openingHours);
        Log.d(TAG,"    language: " + language);

        //make array with all the values
        String[] filterValues = {
//                String.valueOf(1000*searchRadius),
                String.valueOf(minAge),
                String.valueOf(morningTime),
                String.valueOf(eveningTime),
                String.valueOf(openingHours),
                String.valueOf(3),   // 3 is home everything < 3 is Kita
                language};

        //make array with the selection phrases for each filter
        String[] filterSelcetion = {
//                KitaProvider.sDistanzSelection,
                KitaProvider.sAufnahmealterSelection,
                KitaProvider.sÖffnetSelection,
                KitaProvider.sSchließtSelection,
                KitaProvider.sOpeningHoursSelection,
                KitaProvider.sLocationKitaSelection,
                KitaProvider.sFremdspracheSelection};

        List<String> argsList = new ArrayList<String>();

        Log.d(TAG, "filterList.size(): " +filterValues.length);

        String selection = "";
        int index = 0;

        //running through the filters with number values (first4)
        while (index < filterValues.length-1){
            Log.d(TAG, "index: " +index);
            if(Float.parseFloat(filterValues[index]) >= 0){
                Log.d(TAG, String.format("Filter at index %d activ",index));
                if (selection.equals("")) selection = selection + filterSelcetion[index];
                else selection = selection + " AND " + filterSelcetion[index];
                argsList.add(filterValues[index]);
            }else Log.d(TAG, String.format("Filter at index %d nicht activ",index));
            index++;
        }
        Log.d(TAG, "filterValues[index]:" + filterValues[index]);
        if (filterValues[index].equals("disable"))
            Log.d(TAG, String.format("Filter at index %d not activ",index));
        else{
            Log.d(TAG, String.format("Filter at index %d activ", index));
            if (selection.equals("")) selection = selection + filterSelcetion[index];
            else selection = selection + " AND " + filterSelcetion[index];
            argsList.add(filterValues[index]);
        }

        //add maximum Radius
        argsList.add("5000");
        if (selection.equals("")) selection = KitaProvider.sDistanzSelection;
        else selection = selection + " AND " + KitaProvider.sDistanzSelection;

        //convert argsList (type List<String>) to args (type String[])
        String[] args = new String[argsList.size()];
        argsList.toArray(args);
        Log.d(TAG, "Length of args: " + args.length);
        for (String arg:args) Log.d(TAG, "arg: " + arg);

        //initialise sortOrder, kitaUri and cursor for query within switch-statement
        String sortOrder = KitaContract.LocationEntry.COLUMN_DIST + " ASC";
        Uri kitaByLocationUri;
        Cursor cursor = null;

        Log.d(TAG, "default");
        kitaByLocationUri = KitaContract.KitaEntry
                .buildKitaUriWithLocation(mContext.getString(R.string.location_option_all));

        //query all Kitas with given sort order
        cursor = mContext.getContentResolver()
                .query(kitaByLocationUri, null, selection, args, sortOrder);

        if (cursor != null) {
            Log.d(TAG, "Cursor.length:" + cursor.getCount());
            Log.d(TAG, "Cursor.getColumnCount:" + cursor.getColumnCount());
            swapCursor(cursor);
        }
        else Log.e(TAG, "(Cursor == null) after query");

//        //Test Output
//        if (kitaCursor != null){
//            kitaCursor.moveToFirst();
//            while (!kitaCursor.isAfterLast()) {
//                Log.d(TAG, "cursor move to First! Columns: " + kitaCursor.getColumnCount()
//                        + " String and der Stelle 1: " + kitaCursor.getString(1)
//                        + "Column count: " + kitaCursor.getColumnCount());
//                Log.d(TAG, "Kita_ID: " + kitaCursor.getLong(COL_KITAID));
//                kitaCursor.moveToNext();
//            }
//        } else Log.e(TAG, "NOT cursor.moveToFirst()!");
//
//        if (locCursor != null){
//            locCursor.moveToFirst();
//            while (!locCursor.isAfterLast()) {
//                Log.d(TAG, "cursor move to First! Columns: " + locCursor.getColumnCount()
//                        + " String and der Stelle 1: " + locCursor.getString(1)
//                        + "Column count: " + locCursor.getColumnCount());
//                Log.d(TAG, "Kita_Foreign_ID: " + locCursor.getLong(COL_FK_KITA_ID));
//                locCursor.moveToNext();
//            }
//        } else Log.e(TAG, "NOT cursor.moveToFirst()!");
    }

    @Override
    public OverviewViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_list_item, parent, false);
        return new OverviewViewHolder(view, mListener);
    }

    @Override
    protected void onBindViewHolder(OverviewViewHolder holder, Cursor cursor)
    {

        viewNr++;
        //Bind Name
        String kitaName = cursor.getString(IJ_COL_NAME);
        //Kita names ar far toooo loooong
        if (kitaName.length()<24 && kitaName.length()>0) holder.name.setText(kitaName);
        else if (kitaName.length()>=24) holder.name.setText(kitaName.substring(0,21)+"...");
        else holder.name.setText("kein Kitaname gefunden...");

        //Bind Öffnungszeiten
        String kitaÖffnungsz = cursor.getString(IJ_COL_ÖFFNUNGSZ);
        if (kitaÖffnungsz.length()>0) holder.öffnungsz.setText(kitaÖffnungsz+" Uhr");
        else holder.öffnungsz.setText("Öffnungszeit n.A.");

        //Bind Aufnahmealter
        String kitaAufnahmea = cursor.getString(IJ_COL_AUFNAHMEA);
        if (kitaAufnahmea.length()==0) holder.aufnahmea.setText("nicht bekannt");
        else holder.aufnahmea.setText("ab "+kitaAufnahmea+" Mon.");

        //Bind Sprachen
        String sprache = cursor.getString(IJ_COL_FREMDSP);
        //Log.d(TAG, "Fremdsprache: "+ sprache);
        //Log.d(TAG, "kitaID: " + cursor.getInt(COL_ID));
        //Log.d(TAG, "kitaName: " + cursor.getString(COL_NAME));
        if (sprache.equals("deutsch")) {
            holder.sprache.setText("DE");
            holder.sprache.setVisibility(View.GONE);
        } else if (sprache.length()>0){
            holder.sprache.setText(sprache.substring(0,3).toUpperCase());
        } else Log.e(TAG, "Language column empty for Kita: " + kitaName);


        //Bind Distanz
        float kitaDistanz = cursor.getFloat(IJ_COL_DIST);
        if (kitaDistanz >= 0.0) holder.distanz.setText(String.format(
                Locale.GERMAN,
                "%2.1f km",
                kitaDistanz/1000
        ));
        else Log.e(TAG,"ERROR Distance= " + kitaDistanz);

        // Bind Fav-Logo (muss die Datenbank fresch callen, weil der cursor bei hin-und her scrollen
        // eventuell noch ncihts davon weiß)
        Uri kitaUri = KitaContract.KitaEntry.CONTENT_URI;
        int kitaId = cursor.getInt(IJ_COL_KITAID);


        Cursor freshCursor = holder.favorit.getContext().getContentResolver().query(
                kitaUri,
                new String[]{KitaContract.KitaEntry.COLUMN_FAV},
                KitaContract.KitaEntry._ID+ " = ?",
                new String[] {String.valueOf(kitaId)},
                null
        );
        String isFavString = "";
        //Get string from freschly queried cursor
        if (freshCursor.moveToFirst()) isFavString = (freshCursor.getString(0));
        else Log.e(TAG, "favCursor is empty");
        //make Boolean from String
        boolean isFav = false;
        Log.d(TAG, "isFavString:" + isFavString + "!=" + mContext.getString(R.string.status_not_fav)+".");
        if (!isFavString.equals(mContext.getString(R.string.status_not_fav))) {
            isFav = true;
            Log.d(TAG, "isFav");
        } else {
            isFav = false;
            Log.d(TAG, "!isFav");
        }

        //Bind logo to ImageButton-View depending on the value of freshly queried "isFav"
        // for given Item (KitaEntry._ID+ " = ?", favId)
        holder.favorit.setTag(kitaId);
        holder.item.setTag(kitaId);
        if (isFav){
            holder.favorit.setImageResource(R.drawable.ic_favorite_black_24dp);
        } else holder.favorit.setImageResource(R.drawable.ic_favorite_border_black_24dp);

        float realSearchRadius = mSearchRadius;
        if (mSearchRadius == 0) realSearchRadius = 0.5f;
        //make texts grey if kitaDitanz > mSearchRadius
        if (kitaDistanz >= realSearchRadius*1000+50) {
            holder.distanz.setTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));
            holder.name.setTextColor(Color.GRAY);
            holder.öffnungsz.setTextColor(Color.GRAY);
            holder.aufnahmea.setTextColor(Color.GRAY);
            holder.sprache.setTextColor(Color.GRAY);
            holder.favorit.setAlpha(0.5f);
        //make them black again, if scrolled back up
        } else {
            holder.distanz.setTextColor(Color.BLACK);
            holder.name.setTextColor(Color.BLACK);
            holder.öffnungsz.setTextColor(Color.BLACK);
            holder.aufnahmea.setTextColor(Color.BLACK);
            holder.sprache.setTextColor(Color.BLACK);
            holder.favorit.setAlpha(1.0f);
        }

    }



    public static class OverviewViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener
    {
        TextView name;
        TextView öffnungsz;
        TextView aufnahmea;
        TextView sprache;
        TextView distanz;
        ImageButton favorit;
        View item;
        private WeakReference<ClickListener> listenerRef;

        OverviewViewHolder(View itemView, ClickListener listener)
        {
            super(itemView);
            listenerRef = new WeakReference<>(listener);
            name = (TextView) itemView.findViewById(R.id.kita_name);
            öffnungsz = (TextView) itemView.findViewById(R.id.kita_öffnungszeiten);
            aufnahmea = (TextView) itemView.findViewById(R.id.kita_aufnahmealter);
            sprache = (TextView) itemView.findViewById(R.id.kita_sprache);
            distanz = (TextView) itemView.findViewById(R.id.kita_distanz);
            favorit = (ImageButton) itemView.findViewById(R.id.kita_fav);
            item = itemView.findViewById(R.id.kita_list_item);

            item.setOnClickListener(this);
            favorit.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

            // if clicked-on view == ImageButton view
            if (view.getId() == favorit.getId()) {

                boolean isFav = false;
                String isFavString = "";

                //get kitaID from Fav-Button tag
                int kitaId = (Integer) favorit.getTag();

                //get a Cursor with COLUMN_FAV where kitaId == COLUMN_ID
                Uri kitaUri = KitaContract.KitaEntry.CONTENT_URI;
                String[] kitaIdArgs = new String[]{String.valueOf(kitaId)};
                String selection = KitaContract.KitaEntry._ID + " = ?";

                Cursor isFavCursor = view.getContext().getContentResolver().query(
                        kitaUri,
                        new String[]{KitaContract.KitaEntry.COLUMN_FAV},
                        KitaContract.KitaEntry._ID + " = ?",
                        kitaIdArgs,
                        null
                );

                //get boolean isFav from cursor
                if (isFavCursor.moveToFirst()) isFavString = (isFavCursor.getString(0));
                //make Boolean from String
                if (!isFavString.equals("notFav"))
                    isFav = true;
                else isFav = false;
                isFavCursor.close();

                //if the Kita was favorite before clicking, unfav it...
                if (isFav) {
                    ContentValues contentValuesUnFav = new ContentValues();
                    contentValuesUnFav.put(KitaContract.KitaEntry.COLUMN_FAV, "notFav");
                    int updatedRows = view.getContext().getContentResolver().update(
                            kitaUri,
                            contentValuesUnFav,
                            selection,
                            kitaIdArgs
                    );
                    Log.d(TAG,"Unfaved Kita-Id "+kitaId+ "and updated "+updatedRows+" rows");
                    favorit.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                //...otherwise make it favorite
                } else {
                    ContentValues contentValuesFav = new ContentValues();
                    contentValuesFav.put(KitaContract.KitaEntry.COLUMN_FAV, "fav");
                    int updatedRows = view.getContext().getContentResolver().update(
                            kitaUri,
                            contentValuesFav,
                            selection,
                            kitaIdArgs
                    );
                    favorit.setImageResource(R.drawable.ic_favorite_black_24dp);
                }
                listenerRef.get().onPositionClicked(getAdapterPosition(), kitaId);

            //if the clicked-on view is sth else, it's probably the whole list-item
            } else if (view.getId() == item.getId()){
                Log.d(TAG, "item clicked: " + getAdapterPosition());
                listenerRef.get().onPositionClicked(getAdapterPosition(),-1);
                EventBus.getDefault().post(new DetailTrigger((int) item.getTag()));
            } else Log.e(TAG, "Dont know what has been clicked");
        }
        @Override
        public boolean onLongClick(View view) {

            final AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setTitle("Hello Dialog")
                    .setMessage("LONG CLICK DIALOG WINDOW FOR ICON " + String.valueOf(getAdapterPosition()))
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });

            builder.create().show();
            return true;
        }
    }
}



