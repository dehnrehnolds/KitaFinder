package com.example.robert.kitafinder;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.robert.kitafinder.data.Constants;
import com.example.robert.kitafinder.data.KitaContract;

import org.apache.commons.lang3.text.WordUtils;

/**
 * Created by Robert on 18.12.2017.
 */

public class DetailFragment extends Fragment {

    private static final String TAG = DetailFragment.class.getSimpleName();

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

    private void bindViews(View rootView, Cursor cursor) {

        TextView name = rootView.findViewById(R.id.name_value);
        String kitaName = cursor.getString(Constants.COL_NAME);
        final int maxNameLength = 50;
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
        if (phoneNo.equals("")) phone.setText("k.A.");
        else phone.setText(phoneNo);

        TextView mail = rootView.findViewById(R.id.mail_value);
        String mailAdd = cursor.getString(Constants.COL_EMAIL);
        if (mailAdd.equals("")) mail.setText("k.A.");
        else mail.setText(mailAdd);

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

    }
    

    private void showSnackbar(final String text, View view) {
        View container = view.findViewById(R.id.detail_fragment);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }
}
