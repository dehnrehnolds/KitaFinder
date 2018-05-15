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

    // Specify the columns we need.

    public FavAdapter(Context context, ClickListener listener)
    {
        super(null);
        viewNr = 0;
        mContext = context;
        mListener = listener;

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
    protected void onBindViewHolder(OverviewViewHolder holder, Cursor cursor)
    {

        viewNr++;
        //Bind Name
        String kitaName = cursor.getString(IJ_COL_NAME);
        //Kita names ar far toooo loooong
        if (kitaName.length()<24 && kitaName.length()>0) holder.name.setText(kitaName);
        else if (kitaName.length()>=24) holder.name.setText(kitaName.substring(0,21)+"...");
        else holder.name.setText("kein Kitaname gefunden...");

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
        //make Boolean from String
        boolean isFav = false;
        if (isFavString != mContext.getString(R.string.status_not_fav))
            isFav = true;
        else isFav = false;

        //Bind logo to ImageButton-View depending on the value of freshly queried "isFav"
        // for given Item (KitaEntry._ID+ " = ?", favId)
        holder.favorit.setTag(kitaId);
        holder.item.setTag(kitaId);
        if (isFav){
            holder.favorit.setImageResource(R.drawable.ic_favorite_black_24dp);
        } else holder.favorit.setImageResource(R.drawable.ic_favorite_border_black_24dp);

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

            favorit = (ImageButton) itemView.findViewById(R.id.kita_fav);
            mail = itemView.findViewById(R.id.kita_mail);
            waiting = itemView.findViewById(R.id.kita_wait);
            declined = itemView.findViewById(R.id.kita_declined);
            approved = itemView.findViewById(R.id.kita_approved);

            item = itemView.findViewById(R.id.fav_list_item);

            item.setOnClickListener(this);
            favorit.setOnClickListener(this);
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

                // if clicked-on view == Fav-Button
                if (view.getId() == favorit.getId()) {

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

                        favorit.setBackgroundResource(R.drawable.rounded_corner_fav_inactive);
                        mail.setBackgroundResource(R.drawable.rounded_corner_fav_inactive);
                        waiting.setBackgroundResource(R.drawable.rounded_corner_fav_inactive);
                        declined.setBackgroundResource(R.drawable.rounded_corner_fav_inactive);
                        approved.setBackgroundResource(R.drawable.rounded_corner_fav_inactive);

                        favorit.setPadding(4,4,4,4);
                        mail.setPadding(4,4,4,4);
                        waiting.setPadding(4,4,4,4);
                        declined.setPadding(4,4,4,4);
                        approved.setPadding(4,4,4,4);

                        isFav = false;

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
                        favorit.setBackgroundResource(R.drawable.rounded_corner_fav_active);
                        favorit.setPadding(8,8,8,8);
                    }
                    listenerRef.get().onPositionClicked(getAdapterPosition(), kitaId);

                //if the clicked-on view == Mail Sent Status
                } else if (view.getId() == mail.getId()) {
                    Log.d(TAG, "MailButton clicked.");
                    favorit.setBackgroundResource(R.drawable.rounded_corner_fav_active);
                    mail.setBackgroundResource(R.drawable.rounded_corner_fav_active);
                    waiting.setBackgroundResource(R.drawable.rounded_corner_fav_inactive);
                    declined.setBackgroundResource(R.drawable.rounded_corner_fav_inactive);
                    approved.setBackgroundResource(R.drawable.rounded_corner_fav_inactive);

                    favorit.setPadding(4, 4, 4, 4);
                    mail.setPadding(8, 8, 8, 8);
                    waiting.setPadding(4, 4, 4, 4);
                    declined.setPadding(4, 4, 4, 4);
                    approved.setPadding(4, 4, 4, 4);

                    makeToast(view.getContext(), "Mail an Kita gesendet, warten auf Antwort.");

                //if the clicked-on view == Waiting List Status
                } else if (view.getId() == waiting.getId()){
                    Log.d(TAG, "MailButton clicked.");
                    favorit.setBackgroundResource(R.drawable.rounded_corner_fav_active);
                    mail.setBackgroundResource(R.drawable.rounded_corner_fav_active);
                    waiting.setBackgroundResource(R.drawable.rounded_corner_fav_active);
                    declined.setBackgroundResource(R.drawable.rounded_corner_fav_inactive);
                    approved.setBackgroundResource(R.drawable.rounded_corner_fav_inactive);

                    favorit.setPadding(4,4,4,4);
                    mail.setPadding(4,4,4,4);
                    waiting.setPadding(8,8,8,8);
                    declined.setPadding(4,4,4,4);
                    approved.setPadding(4,4,4,4);

                    makeToast(view.getContext(), "Auf der Warteliste. " +
                            "Reminder alle 3 Monate senden!");

                //if the clicked-on view == Declined Status
                } else if (view.getId() == declined.getId()){
                    Log.d(TAG, "MailButton clicked.");
                    favorit.setBackgroundResource(R.drawable.rounded_corner_fav_declined);
                    mail.setBackgroundResource(R.drawable.rounded_corner_fav_declined);
                    waiting.setBackgroundResource(R.drawable.rounded_corner_fav_declined);
                    declined.setBackgroundResource(R.drawable.rounded_corner_fav_declined);
                    approved.setBackgroundResource(R.drawable.rounded_corner_fav_declined);

                    favorit.setPadding(4,4,4,4);
                    mail.setPadding(4,4,4,4);
                    waiting.setPadding(4,4,4,4);
                    declined.setPadding(8,8,8,8);
                    approved.setPadding(4,4,4,4);

                    makeToast(view.getContext(), "Die Kita hat abgesagt. " +
                            "Lassen Sie sich für das folgende Jahr auf die Warteliste setzen!");

                //if the clicked-on view == APPROVED STATUS
                } else if (view.getId() == approved.getId()){
                    Log.d(TAG, "MailButton clicked.");
                    favorit.setBackgroundResource(R.drawable.rounded_corner_fav_active);
                    mail.setBackgroundResource(R.drawable.rounded_corner_fav_active);
                    waiting.setBackgroundResource(R.drawable.rounded_corner_fav_active);
                    declined.setBackgroundResource(R.drawable.rounded_corner_fav_active);
                    approved.setBackgroundResource(R.drawable.rounded_corner_fav_active);

                    favorit.setPadding(4,4,4,4);
                    mail.setPadding(4,4,4,4);
                    waiting.setPadding(4,4,4,4);
                    declined.setPadding(8,8,8,8);
                    approved.setPadding(4,4,4,4);

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

    private static void makeToast(Context context, final String text) {
        Toast addressFoundToast = Toast.makeText(context,
                text,
                Toast.LENGTH_SHORT
        );
        addressFoundToast.show();
    }

}