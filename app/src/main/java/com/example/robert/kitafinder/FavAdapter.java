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
import android.widget.Toast;

import com.example.robert.kitafinder.data.DetailTrigger;
import com.example.robert.kitafinder.data.FavRefreshTrigger;
import com.example.robert.kitafinder.data.KitaContract;
import com.example.robert.kitafinder.data.KitaProvider;
import com.example.robert.kitafinder.data.ListRefreshTrigger;

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


public class FavAdapter extends RecyclerViewCursorAdapter<FavAdapter.OverviewViewHolder>
{
    public interface ClickListener {

        void onPositionClicked(int position, int kitaId);

        void onLongClicked(int position, int kitaId);
    }

    private static final String TAG = FavAdapter.class.getSimpleName();
    private final Context mContext;
    private final ClickListener mListener;
    private final float mSearchRadius;
    private int viewNr;
    static int mActivePaddingInPx;
    static int mInactivePaddingInPx;
    static OverviewViewHolder mHolder;

    // Specify the columns we need.

    public FavAdapter(Context context, ClickListener listener)
    {
        super(null);
        viewNr = 0;
        mContext = context;
        mListener = listener;

        // Calculation of pxs from dp
        // Get the screen's density scale
        final float scale = context.getResources().getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        mActivePaddingInPx = (int) (8 * scale + 0.5f);
        mInactivePaddingInPx = (int) (4 * scale + 0.5f);

        //open filter preferences
        SharedPreferences sharedPref = mContext.getSharedPreferences("filter", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        mSearchRadius = (float) sharedPref.getInt(mContext.getString(R.string.search_radius), -1);

        //initialise sortOrder, kitaUri and cursor for query within switch-statement
        String selection = "";
        String sortOrder = KitaContract.LocationEntry.COLUMN_DIST + " ASC";
        String[] args = new String[]{};
        Uri kitaByLocationUri;
        Cursor cursor = null;

        Log.d(TAG, "favourites");
        kitaByLocationUri = KitaContract.KitaEntry
                .buildKitaUriWithLocation(mContext.getString(R.string.location_option_all));
        selection = "NOT " + KitaContract.KitaEntry.COLUMN_FAV+ " = ?";
        args = new String[]{mContext.getString(R.string.status_not_fav)};

        // query all Kitas with given sort order
        cursor = mContext.getContentResolver()
                .query(kitaByLocationUri, null, selection, args, sortOrder);




        if (cursor != null) {
            Log.d(TAG, "Cursor.length:" + cursor.getCount());
            Log.d(TAG, "Cursor.getColumnCount:" + cursor.getColumnCount());
            swapCursor(cursor);
        }
        else Log.e(TAG, "(Cursor == null) after query");
    }

    @Override
    public OverviewViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_fav_item, parent, false);
        return new OverviewViewHolder(view, mListener);
    }

    @Override
    protected void onBindViewHolder(OverviewViewHolder holder, Cursor cursor) {

        viewNr++;
        //Bind Name
        String kitaName = cursor.getString(IJ_COL_NAME);
        //Kita names ar far toooo loooong
        if (kitaName.length() < 24 && kitaName.length() > 0) holder.name.setText(kitaName);
        else if (kitaName.length() >= 24) holder.name.setText(kitaName.substring(0, 21) + "...");
        else holder.name.setText("kein Kitaname gefunden...");

        //Bind Distanz
        float kitaDistanz = cursor.getFloat(IJ_COL_DIST);
        if (kitaDistanz >= 0.0) holder.distanz.setText(String.format(
                Locale.GERMAN,
                "%2.1f km",
                kitaDistanz / 1000
        ));
        else Log.e(TAG, "ERROR Distance= " + kitaDistanz);

        // Bind Fav-Logo (muss die Datenbank fresch callen, weil der cursor bei hin-und her scrollen
        // eventuell noch ncihts davon weiß)
        Uri kitaUri = KitaContract.KitaEntry.CONTENT_URI;
        int kitaId = cursor.getInt(IJ_COL_KITAID);


        Cursor freshCursor = holder.favorit.getContext().getContentResolver().query(
                kitaUri,
                new String[]{KitaContract.KitaEntry.COLUMN_FAV},
                KitaContract.KitaEntry._ID + " = ?",
                new String[]{String.valueOf(kitaId)},
                null
        );
        String isFavString = "";
        //Get string from freschly queried cursor
        if (freshCursor.moveToFirst()) isFavString = (freshCursor.getString(0));
        //make Boolean from String

        holder.favorit.setTag(kitaId);
        holder.item.setTag(kitaId);

        switch (isFavString) {
            case "notFav":
                setNotFavProperties(holder);
                break;
            case "fav":
                setFavProperties(holder);
                break;
            case "sent":
                setMailProperties(holder);
                break;
            case "waitingList":
                setWaitingProperties(holder);
                break;
            case "declined":
                setDeclinedProperties(holder);
                break;
            case "approved":
                setApprovedProperties(holder);
                break;
            default:
                Log.e(TAG, "no SWITCH statement met!");

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
        ImageButton mail;
        ImageButton waiting;
        ImageButton declined;
        ImageButton approved;
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

            favorit = itemView.findViewById(R.id.kita_fav);
            mail = itemView.findViewById(R.id.kita_mail);
            waiting = itemView.findViewById(R.id.kita_wait);
            declined = itemView.findViewById(R.id.kita_declined);
            approved = itemView.findViewById(R.id.kita_approved);

            item = itemView.findViewById(R.id.fav_list_item);

            item.setOnClickListener(this);
            favorit.setOnClickListener(this);
            mail.setOnClickListener(this);
            waiting.setOnClickListener(this);
            declined.setOnClickListener(this);
            approved.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

            // first check if the whole item has been clicked
            if (view.getId() == item.getId()) {
                Log.d(TAG, "item clicked: " + getAdapterPosition());
                listenerRef.get().onPositionClicked(getAdapterPosition(), -1);
                EventBus.getDefault().post(new DetailTrigger((int) item.getTag()));

            // if it's not the item, get isFav status of the Kita
            } else {

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
                if (!isFavString.equals(view.getContext().getString(R.string.status_not_fav)))
                    isFav = true;
                isFavCursor.close();

                // if clicked-on view == Fav-Button
                if (view.getId() == favorit.getId()) {

                    // In case anything happens on the FAV part, refresh the ListAdapter already,
                    // so it will be updated when looking at the list again later
                    EventBus.getDefault().post(new ListRefreshTrigger());
                    Log.d(TAG, "LIST REFRESH TRIGGER sent");


                    //if the Kita was favorite before clicking, unfav it...
                    if (isFav) {
                        ContentValues contentValuesUnFav = new ContentValues();
                        contentValuesUnFav.put(KitaContract.KitaEntry.COLUMN_FAV,
                                view.getContext().getString(R.string.status_not_fav));
                        int updatedRows = view.getContext().getContentResolver().update(
                                kitaUri,
                                contentValuesUnFav,
                                selection,
                                kitaIdArgs
                        );
                        Log.d(TAG,"Unfaved Kita-Id "+kitaId+ "and updated "+updatedRows+" rows");

                        setNotFavProperties(this);


                    //...otherwise make it favorite
                    } else {
                        ContentValues contentValuesFav = new ContentValues();
                        contentValuesFav.put(KitaContract.KitaEntry.COLUMN_FAV,
                                view.getContext().getString(R.string.status_fav));
                        int updatedRows = view.getContext().getContentResolver().update(
                                kitaUri,
                                contentValuesFav,
                                selection,
                                kitaIdArgs
                        );

                       setFavProperties(this);
                    }

                //if the clicked-on view == Mail Sent Status
                } else if (view.getId() == mail.getId()) {
                    Log.d(TAG, "MailButton clicked.");

                    if (!isFav) {
                        // In case the Kita was not faved, but now status "mail" is set, means that
                        // the Kita is also faved now. Refresh the ListAdapter already,
                        // so it will be updated when looking at the list again later
                        EventBus.getDefault().post(new ListRefreshTrigger());
                        Log.d(TAG, "LIST REFRESH TRIGGER sent");
                    }

                    setMailProperties(this);

                    // set Kita-status to mail-sent
                    ContentValues contentValuesFav = new ContentValues();
                    contentValuesFav.put(KitaContract.KitaEntry.COLUMN_FAV, 
                            view.getContext().getString(R.string.status_mail_sent));
                    int updatedRows = view.getContext().getContentResolver().update(
                            kitaUri,
                            contentValuesFav,
                            selection,
                            kitaIdArgs
                    );
                    makeToast(view.getContext(), "Mail an Kita gesendet, warten auf Antwort.");

                //if the clicked-on view == Waiting List Status
                } else if (view.getId() == waiting.getId()){
                    Log.d(TAG, "WitingListButton clicked.");

                    if (!isFav) {
                        // In case the Kita was not faved, but now status "waiting" is set, means that
                        // the Kita is also faved now. Refresh the ListAdapter already,
                        // so it will be updated when looking at the list again later
                        EventBus.getDefault().post(new ListRefreshTrigger());
                        Log.d(TAG, "LIST REFRESH TRIGGER sent");
                    }

                    setWaitingProperties(this);

                    // set Kita-status to waiting list
                    ContentValues contentValuesFav = new ContentValues();
                    contentValuesFav.put(KitaContract.KitaEntry.COLUMN_FAV,
                            view.getContext().getString(R.string.status_waiting_list));
                    int updatedRows = view.getContext().getContentResolver().update(
                            kitaUri,
                            contentValuesFav,
                            selection,
                            kitaIdArgs
                    );

                    makeToast(view.getContext(), "Auf der Warteliste. " +
                            "Reminder alle 3 Monate senden!");

                //if the clicked-on view == Declined Status
                } else if (view.getId() == declined.getId()){
                    Log.d(TAG, "DeclinedButton clicked.");

                    if (!isFav) {
                        // In case the Kita was not faved, but now status "declined" is set, means that
                        // the Kita is also faved now. Refresh the ListAdapter already,
                        // so it will be updated when looking at the list again later
                        EventBus.getDefault().post(new ListRefreshTrigger());
                        Log.d(TAG, "LIST REFRESH TRIGGER sent");
                    }


                    setDeclinedProperties(this);

                    // set Kita-status to declined
                    ContentValues contentValuesFav = new ContentValues();
                    contentValuesFav.put(KitaContract.KitaEntry.COLUMN_FAV,
                            view.getContext().getString(R.string.status_declined));
                    int updatedRows = view.getContext().getContentResolver().update(
                            kitaUri,
                            contentValuesFav,
                            selection,
                            kitaIdArgs
                    );

                    makeToast(view.getContext(), "Die Kita hat abgesagt. " +
                            "Lassen Sie sich für das folgende Jahr auf die Warteliste setzen!");

                //if the clicked-on view == APPROVED STATUS
                } else if (view.getId() == approved.getId()){
                    Log.d(TAG, "ApprovedButton clicked.");

                    if (!isFav) {
                        // In case the Kita was not faved, but now status "approved" is set, means that
                        // the Kita is also faved now. Refresh the ListAdapter already,
                        // so it will be updated when looking at the list again later
                        EventBus.getDefault().post(new ListRefreshTrigger());
                        Log.d(TAG, "LIST REFRESH TRIGGER sent");
                    }

                    setApprovedProperties(this);

                    // set Kita-status to approved
                    ContentValues contentValuesFav = new ContentValues();
                    contentValuesFav.put(KitaContract.KitaEntry.COLUMN_FAV,
                            view.getContext().getString(R.string.status_approved));
                    int updatedRows = view.getContext().getContentResolver().update(
                            kitaUri,
                            contentValuesFav,
                            selection,
                            kitaIdArgs
                    );
                    
                    makeToast(view.getContext(), "Die Kita hat zugesagt, Glückwunsch!" +
                            " Denken Sie an den Kita-Gutschein!");

                } else Log.e(TAG, "Dont know what has been clicked");
            }
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
            Log.d(TAG, "Builder.create().show();");
            builder.create().show();
            return true;
        }

    }

    private static void setNotFavProperties(OverviewViewHolder holder){
        holder.favorit.setImageResource(R.drawable.ic_favorite_border_black_24dp);

        holder.favorit.setBackgroundResource(R.drawable.rounded_corner_fav_grey);
        holder.mail.setBackgroundResource(R.drawable.rounded_corner_fav_grey);
        holder.waiting.setBackgroundResource(R.drawable.rounded_corner_fav_grey);
        holder.declined.setBackgroundResource(R.drawable.rounded_corner_fav_grey);
        holder.approved.setBackgroundResource(R.drawable.rounded_corner_fav_grey);

        holder.favorit.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.mail.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.waiting.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.declined.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.approved.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
    }

    private static void setFavProperties(OverviewViewHolder holder){

        holder.favorit.setImageResource(R.drawable.ic_favorite_black_24dp);

        holder.favorit.setBackgroundResource(R.drawable.rounded_corner_fav_active);
        holder.mail.setBackgroundResource(R.drawable.rounded_corner_fav_grey);
        holder.waiting.setBackgroundResource(R.drawable.rounded_corner_fav_grey);
        holder.declined.setBackgroundResource(R.drawable.rounded_corner_fav_grey);
        holder.approved.setBackgroundResource(R.drawable.rounded_corner_fav_grey);

        holder.favorit.setPadding(mActivePaddingInPx,mActivePaddingInPx,
                mActivePaddingInPx,mActivePaddingInPx);
        holder.mail.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.waiting.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.declined.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.approved.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
    }

    private static void setMailProperties(OverviewViewHolder holder){

        holder.favorit.setImageResource(R.drawable.ic_favorite_black_24dp);

        holder.favorit.setBackgroundResource(R.drawable.rounded_corner_fav_active);
        holder.mail.setBackgroundResource(R.drawable.rounded_corner_fav_active);
        holder.waiting.setBackgroundResource(R.drawable.rounded_corner_fav_grey);
        holder.declined.setBackgroundResource(R.drawable.rounded_corner_fav_grey);
        holder.approved.setBackgroundResource(R.drawable.rounded_corner_fav_grey);

        holder.favorit.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.mail.setPadding(mActivePaddingInPx,mActivePaddingInPx,
                mActivePaddingInPx,mActivePaddingInPx);
        holder.waiting.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.declined.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.approved.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
    }

    private static void setWaitingProperties(OverviewViewHolder holder){

        holder.favorit.setImageResource(R.drawable.ic_favorite_black_24dp);

        holder.favorit.setBackgroundResource(R.drawable.rounded_corner_fav_active);
        holder.mail.setBackgroundResource(R.drawable.rounded_corner_fav_active);
        holder.waiting.setBackgroundResource(R.drawable.rounded_corner_fav_active);
        holder.declined.setBackgroundResource(R.drawable.rounded_corner_fav_grey);
        holder.approved.setBackgroundResource(R.drawable.rounded_corner_fav_grey);

        holder.favorit.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.mail.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.waiting.setPadding(mActivePaddingInPx,mActivePaddingInPx,
                mActivePaddingInPx,mActivePaddingInPx);
        holder.declined.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.approved.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
    }

    private static void setDeclinedProperties(OverviewViewHolder holder){

        holder.favorit.setImageResource(R.drawable.ic_favorite_black_24dp);

        holder.favorit.setBackgroundResource(R.drawable.rounded_corner_fav_grey);
        holder.mail.setBackgroundResource(R.drawable.rounded_corner_fav_grey);
        holder.waiting.setBackgroundResource(R.drawable.rounded_corner_fav_grey);
        holder.declined.setBackgroundResource(R.drawable.rounded_corner_fav_declined);
        holder.approved.setBackgroundResource(R.drawable.rounded_corner_fav_grey);

        holder.favorit.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.mail.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.waiting.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.declined.setPadding(mActivePaddingInPx,mActivePaddingInPx,
                mActivePaddingInPx,mActivePaddingInPx);
        holder.approved.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
    }

    private static void setApprovedProperties(OverviewViewHolder holder){

        holder.favorit.setImageResource(R.drawable.ic_favorite_black_24dp);

        holder.favorit.setBackgroundResource(R.drawable.rounded_corner_fav_active);
        holder.mail.setBackgroundResource(R.drawable.rounded_corner_fav_active);
        holder.waiting.setBackgroundResource(R.drawable.rounded_corner_fav_active);
        holder.declined.setBackgroundResource(R.drawable.rounded_corner_fav_grey);
        holder.approved.setBackgroundResource(R.drawable.rounded_corner_fav_approved);

        holder.favorit.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.mail.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.waiting.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.declined.setPadding(mInactivePaddingInPx,mInactivePaddingInPx,
                mInactivePaddingInPx,mInactivePaddingInPx);
        holder.approved.setPadding(mActivePaddingInPx,mActivePaddingInPx,
                mActivePaddingInPx,mActivePaddingInPx);
    }

    private static void makeToast(Context context, final String text) {
        Toast addressFoundToast = Toast.makeText(context,
                text,
                Toast.LENGTH_SHORT
        );
        addressFoundToast.show();
    }

}