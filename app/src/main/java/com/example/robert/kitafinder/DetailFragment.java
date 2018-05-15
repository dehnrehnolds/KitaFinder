package com.example.robert.kitafinder;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.robert.kitafinder.data.Constants;
import com.example.robert.kitafinder.data.KitaContract;
import com.example.robert.kitafinder.data.KitaProvider;

import org.apache.commons.lang3.text.WordUtils;

/**
 * Created by Robert on 18.12.2017.
 */

public class DetailFragment extends Fragment {

    private static final String TAG = DetailFragment.class.getSimpleName();
    private static String mPhoneNo = "";
    private static String mEmail = "";
    private static Boolean mIsFav = false;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        int kitaID = -1;

        Bundle bundle = getArguments();
        if (bundle != null) kitaID = bundle.getInt("kitaId", -1);
        else Log.e(TAG, "bundle is null");

        Uri kitaUri = KitaContract.KitaEntry.CONTENT_URI;
        String selection = KitaContract.KitaEntry._ID + " = ?";
        String[] args = new String[]{String.valueOf(kitaID)};

        Cursor cursor = getContext().getContentResolver().query(kitaUri,
                null,
                selection,
                args,
                null);

        if (cursor!=null) cursor.moveToNext();

        bindViews(rootView, cursor);

        return rootView;
    }

    private void bindViews(View rootView, final Cursor cursor) {

        TextView name = rootView.findViewById(R.id.name_value);
        String kitaName = cursor.getString(Constants.COL_NAME);
        final int maxNameLength = 40;
        if (kitaName.length()>maxNameLength)
            while (kitaName.length()>maxNameLength){
            if (kitaName.indexOf(" ")>0) kitaName = kitaName.substring(0,kitaName.lastIndexOf(" "));
            else if (kitaName.indexOf(",")>0) kitaName = kitaName.substring(0,kitaName.lastIndexOf(","));
            else if (kitaName.indexOf("/")>0) kitaName = kitaName.substring(0,kitaName.lastIndexOf("/"));
            else break;
            }
        name.setText(kitaName);

        TextView open = rootView.findViewById(R.id.opening_hours_value);
        String openHour = cursor.getString(Constants.COL_ÖFFNUNGSZ);
        if (openHour.equals("")) open.setText("k.A.");
        else open.setText(openHour);

        TextView age = rootView.findViewById(R.id.min_age_value);
        String minAge = cursor.getString(Constants.COL_AUFNAHMEA);
        if (minAge.equals("")) age.setText("k.A.");
        else age.setText(minAge);

        TextView language = rootView.findViewById(R.id.language_value);
        String foreignLanguage =cursor.getString(Constants.COL_FREMDSP);
        if (foreignLanguage.equals("deutsch")) language.setText("keine Frendsprache");
        else {
            foreignLanguage = Character.toUpperCase(foreignLanguage.charAt(0)) + foreignLanguage.substring(1);
            language.setText(foreignLanguage + " (neben Deutsch)");
        }

        TextView phone = rootView.findViewById(R.id.phone_value);
        String phoneNo = cursor.getString(Constants.COL_TELEFON);
        if (phoneNo.equals("") || phoneNo.equals("030")) {
            Log.d(TAG,"k.A.");
            phone.setText("k.A.");
            mPhoneNo = "";
        } else if (!phoneNo.substring(0,3).equals("030")) {
            Log.d(TAG, "substring: " + phoneNo.substring(0,2));
            Log.d(TAG,"030");
            mPhoneNo = "030" + phoneNo;
            phone.setText(mPhoneNo);
        } else if (phoneNo.substring(0,6).equals("030030")){
            Log.d(TAG,"030030");
            mPhoneNo = phoneNo.substring(3);
            phone.setText(mPhoneNo);
            Log.d(TAG, "mPhoneNo: " + mPhoneNo);
        } else {
            Log.d(TAG,"phoneNo");
            mPhoneNo = phoneNo;
            phone.setText(phoneNo);
        }

        TextView mail = rootView.findViewById(R.id.mail_value);
        String mailAdd = cursor.getString(Constants.COL_EMAIL);
        if (mailAdd.equals("")){
            mail.setText("k.A.");
            mEmail = "";
        }
        else {
            mEmail = mailAdd;
            SpannableString mailSS = new SpannableString(mailAdd);
            mailSS.setSpan(new UnderlineSpan(), 0, mailSS.length(), 0);
            mail.setText(mailSS);
        }

        TextView web = rootView.findViewById(R.id.web_value);
        String webSite = cursor.getString(Constants.COL_WEB);
        if (webSite.equals("")) web.setText("k.A.");
        else web.setText(webSite);

        TextView street = rootView.findViewById(R.id.street_value);
        String streetAdd = cursor.getString(Constants.COL_ADRESSE);
        if (streetAdd.equals("")) street.setText("k.A.");
        else street.setText(streetAdd);

        TextView plz = rootView.findViewById(R.id.plz_value);
        String plzNo = cursor.getString(Constants.COL_ORT);
        if (plzNo.equals("")) plz.setText("k.A.");
        else plz.setText(plzNo.substring(0,plzNo.indexOf("(")));

        final FloatingActionButton favButton = getActivity().findViewById(R.id.favourite_button);

        if (isFav( cursor.getInt(Constants.COL_KITAID))) {
            Log.d(TAG, "bindView isFav");
            favButton.setImageResource(R.drawable.ic_favorite_white_24dp);
        } else {
            Log.d(TAG, "bindView !isFav");
            favButton.setImageResource(R.drawable.ic_favorite_border_white_24dp);
        }

        mail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mailAdd = cursor.getString(Constants.COL_EMAIL);
                composeEmail(mailAdd);
            }
        });

        getActivity().findViewById(R.id.mail_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mEmail.equals("")) composeEmail(mEmail);
                else makeToast(getString(R.string.no_mail_toast));
            }
        });

        getActivity().findViewById(R.id.button_call).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mPhoneNo.equals("")) callKita(mPhoneNo);
                else makeToast(getString(R.string.no_phone_toast));
            }
        });


        favButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int kitaId = cursor.getInt(Constants.COL_KITAID);
                Uri kitaUri = KitaContract.KitaEntry.CONTENT_URI;

                if (isFav(kitaId)) {
                    Log.d(TAG, "onClick mIsFav");
                    favButton.setImageResource(R.drawable.ic_favorite_border_white_24dp);
                    ContentValues contentValuesUnFav = new ContentValues();
                    contentValuesUnFav.put(KitaContract.KitaEntry.COLUMN_FAV,
                            getString(R.string.status_not_fav));
                    int updatedRows = getContext().getContentResolver().update(
                            kitaUri,
                            contentValuesUnFav,
                            KitaContract.KitaEntry._ID+ " = ?",
                            new String[] {String.valueOf(kitaId)}
                    );
                    Log.d(TAG,"Unfaved Kita-Id "+kitaId+ "and updated "+updatedRows+" rows");
                } else {
                    Log.d(TAG, "onClick !mIsFav");
                    favButton.setImageResource(R.drawable.ic_favorite_white_24dp);
                    ContentValues contentValuesUnFav = new ContentValues();
                    contentValuesUnFav.put(KitaContract.KitaEntry.COLUMN_FAV,
                            getString(R.string.status_fav));
                    int updatedRows = getContext().getContentResolver().update(
                            kitaUri,
                            contentValuesUnFav,
                            KitaContract.KitaEntry._ID+ " = ?",
                            new String[] {String.valueOf(kitaId)}
                    );
                    Log.d(TAG,"Faved Kita-Id "+kitaId+ "and updated "+updatedRows+" rows");
                }
            }
        });
    }

    private Boolean isFav(Integer kitaId){
        Log.d(TAG, "isFav()");
        Uri kitaUri = KitaContract.KitaEntry.CONTENT_URI;
        Cursor favCursor = getContext().getContentResolver().query(
                kitaUri,
                new String[]{KitaContract.KitaEntry.COLUMN_FAV},
                KitaContract.KitaEntry._ID+ " = ?",
                new String[] {String.valueOf(kitaId)},
                null
        );
        String isFavString = "";
        //Get string from freschly queried cursor
        if (favCursor.moveToFirst()) isFavString = (favCursor.getString(0));
        //make Boolean from String
        if (!isFavString.equals(getString(R.string.status_not_fav)))
            return true;
        else return false;

    }

    public void composeEmail(String address) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + address));
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.mail_subject));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.mail_text));
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            try {
                startActivity(intent);
                Log.d(TAG, "startActivity() - Mail");
            } catch (Error e){
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }

        }
    }

    public void callKita(String telNumber) {
        Log.d(TAG, " callKita ()");
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + telNumber));
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            try {
                startActivity(intent);
                Log.d(TAG, "startActivity() - Call");
            } catch (Error e){
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }

        }
    }

    private void makeToast(final String text) {
        Toast addressFoundToast = Toast.makeText(getContext(),
                text,
                Toast.LENGTH_SHORT
        );
        addressFoundToast.show();
    }

    private void showSnackbar(final String text, View view) {
        View container = view.findViewById(R.id.detail_fragment);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }
}
