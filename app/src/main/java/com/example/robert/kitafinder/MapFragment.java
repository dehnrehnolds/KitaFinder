package com.example.robert.kitafinder;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;


import com.example.robert.kitafinder.data.DetailTrigger;
import com.example.robert.kitafinder.data.KitaContract;
import com.example.robert.kitafinder.data.KitaItem;
import com.example.robert.kitafinder.data.KitaProvider;
import com.example.robert.kitafinder.data.RefreshTrigger;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.maps.android.clustering.ClusterManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.example.robert.kitafinder.data.Constants.COL_AUFNAHMEA;
import static com.example.robert.kitafinder.data.Constants.COL_FK_KITA_ID;
import static com.example.robert.kitafinder.data.Constants.COL_FREMDSP;
import static com.example.robert.kitafinder.data.Constants.COL_KITAID;
import static com.example.robert.kitafinder.data.Constants.COL_LAT;
import static com.example.robert.kitafinder.data.Constants.COL_LONG;
import static com.example.robert.kitafinder.data.Constants.COL_NAME;
import static com.example.robert.kitafinder.data.Constants.COL_ÖFFNUNGSZ;

/**
 * Created by Robert on 23.10.2017.
 */

public class MapFragment extends SupportMapFragment implements OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener {

    /**
     * Demonstrates customizing the info window and/or its contents.
     */
    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        // A viewgroups containing an ImageView with id "badge" and two TextViews with id
        private final View mContents;

        CustomInfoWindowAdapter() {
            mContents = getLayoutInflater().inflate(R.layout.map_preview, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            render(marker, mContents);
            return mContents;
        }

        private void render(Marker marker, View view) {
            Log.d(TAG, "render()");


            TextView name = (TextView) view.findViewById(R.id.kita_name);
            TextView öffnungsz = (TextView) view.findViewById(R.id.kita_öffnungszeiten);
            TextView aufnahmea = (TextView) view.findViewById(R.id.kita_aufnahmealter);
            TextView sprache = (TextView) view.findViewById(R.id.kita_sprache);

            // get KitaName and ID from marker title and snippet
            String kitaName = marker.getTitle();
            String kitaId= marker.getSnippet();
            Log.d(TAG, "kitaName: " + kitaName + "    kitaId: " + kitaId);

            // catch special case "home marker"
            if (kitaName.equals("Zu Hause")) {
                name.setText(kitaName);
                öffnungsz.setVisibility(View.GONE);
                aufnahmea.setVisibility(View.GONE);
                sprache.setVisibility(View.GONE);
                return;
            }

            öffnungsz.setVisibility(View.VISIBLE);
            aufnahmea.setVisibility(View.VISIBLE);
            sprache.setVisibility(View.VISIBLE);

            // use KitaID to request all needed data from this Kita for the info-view
            Uri kitaUri = KitaContract.KitaEntry.CONTENT_URI;
            String selection = KitaContract.KitaEntry._ID + " = ?";
            String[] args = new String[]{kitaId};

            // get the Kita from DB by query
            Cursor kitaCursor = getContext().getContentResolver().query(
                    kitaUri,
                    null,
                    selection,
                    args,
                    null
            );

            // see if the cursor is not empty and morve to the next(and only) entry
            if (kitaCursor != null) kitaCursor.moveToNext();
            else Log.e(TAG, "keine Kita mit dieser ID gefunden! kitaID: " + kitaId);

            // bind kitaName (we have already from marker.title)
            if (kitaName.length() <= 30 && kitaName.length() > 0) name.setText(kitaName);
            else if (kitaName.length() > 30) name.setText(kitaName.substring(0, 27) + "...");
            else name.setText("kein Kitaname gefunden...");

            // get and bind Öffnungszeiten
            String kitaÖffnungsz = kitaCursor.getString(COL_ÖFFNUNGSZ);
            if (kitaÖffnungsz.length() > 0) öffnungsz.setText(kitaÖffnungsz + " Uhr");
            else öffnungsz.setText("Öffnungszeit n.A.");

            // get and bind Aufnahmealter
            String kitaAufnahmea = kitaCursor.getString(COL_AUFNAHMEA);
            if (kitaAufnahmea.length() == 0) aufnahmea.setText("nicht bekannt");
            else aufnahmea.setText("ab " + kitaAufnahmea + " Mon.");

            // get and bind Sprachen
            String fremdSprache = kitaCursor.getString(COL_FREMDSP);
            Log.d(TAG, "Fremdsprache: " + fremdSprache);
            if (sprache.equals("deutsch")) {
                sprache.setText("DE");
                sprache.setVisibility(View.GONE);
            } else if (sprache.length() > 0) {
                sprache.setText(fremdSprache.substring(0, 3).toUpperCase());
            } else Log.e(TAG, "Language column empty for Kita: " + kitaName);

            // always close the cursor after used
            kitaCursor.close();

        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Log.d(TAG, "DetailTrigger posted with: " + marker.getSnippet());
        EventBus.getDefault().post(new DetailTrigger(Integer.parseInt(marker.getSnippet())));
    }

    private final String TAG = MapFragment.class.getSimpleName();


    private static final String ARG_SECTION_NUMBER = "section_number";
    private GoogleMap mMap;
    Location mSearchAddress;
    LatLng mSearchAddressLL;
    Marker mHomeMarker;
    Circle mCircle;
    ClusterManager<KitaItem> mClusterManager;
    SparseArray<Marker> mMarkerSparseArray= new SparseArray<>();

    List<Marker> mMarkerList = new ArrayList<>();
    List<Integer> mIdList = new ArrayList<>();

    public MapFragment() {
    }

    @Override
    public void onCreate(Bundle onSavedInstanceState) {
        super.onCreate(onSavedInstanceState);
        mSearchAddress = getActivity().getIntent().getParcelableExtra("address");
        mSearchAddressLL = new LatLng(mSearchAddress.getLatitude(), mSearchAddress.getLongitude());
//        setUpMapIfNeeded();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
        Log.d(TAG, "onStop()");
        getActivity().getIntent().putExtra("address", mSearchAddress);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();

    }

    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        mSearchAddress = getActivity().getIntent().getParcelableExtra("address");
        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        if (mMap == null) {
            Log.d(TAG, "setUpMapIfNeeded");
            getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady");
        mMap = googleMap;
        setUpMap();
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());
        mMap.setOnInfoWindowClickListener(this);
    }

    // This method will be called when a RefreshTrigger event is posted
    // this might happen in case filter settings are changed in the FilterFragment
    @Subscribe
    public void refreshMap(RefreshTrigger event) {
        //re-setting the Adapter will result in the refresh of the Kita list
        Log.d(TAG, "refreshMarkers() called");
        setKitaMarker();
        drawCircle(mSearchAddressLL);
    }

    private void setUpMap() {

        //mMap.setMyLocationEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.clear();

        mHomeMarker = setHomeMarker();
        createKitaMarker();
        setKitaMarker();
        mCircle = drawCircle(mSearchAddressLL);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mSearchAddressLL, 13));
        // Zoom in, animating the camera.
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
        // Zoom out to zoom level 10, animating with a duration of 2 seconds.
        mMap.animateCamera(CameraUpdateFactory.zoomTo(13), 2000, null);

        mClusterManager = new ClusterManager<>(getContext(), mMap);
        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);
        mMap.setOnInfoWindowClickListener(mClusterManager);
        //addKitaItems();
        mClusterManager.cluster();
    }


    private Marker setHomeMarker() {
        Log.d(TAG, "setHomeMarker()");

        return mMap.addMarker(new MarkerOptions().
                position(mSearchAddressLL)
                .title("Zu Hause")
                .draggable(true)
                .icon(BitmapDescriptorFactory.defaultMarker(15))
        );
    }

    private Circle drawCircle(LatLng center) {
        // define center and radius (in meters) of the circle
        SharedPreferences sharedPref = getActivity()
                .getSharedPreferences("filter", Context.MODE_PRIVATE);
        float searchRadius = (float) sharedPref.getInt(getString(R.string.search_radius), -1);
        if (searchRadius == 0) searchRadius = 0.5f;
        List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dash(30), new Gap(40));
        CircleOptions circleOptions = new CircleOptions()
                .center(center)
                .radius(1000 * searchRadius)
                .strokeWidth(5)
                .strokePattern(pattern)
                .strokeColor(Color.argb(128, 255, 0, 0))
                .fillColor(Color.argb(10, 128, 128, 128));

        // Get back the mutable Circle
        return mMap.addCircle(circleOptions);
    }

    private void createKitaMarker() {
        Log.d(TAG, "createKitaMarker()");

        Cursor allCursor = getAllKitasWithLocationsInRadius();

        //create markers for all Kitas within the raius (+ 0.5km) and display them "light"
        Log.d(TAG, "vor Erstellen der Marker");
        // counter for debug
        int markerCount = 0;
        int rowsUpdated = 0;

        // if allCursor exists, move row by row through the cursor
        if (allCursor != null && allCursor.moveToFirst())
            do {
                // get data from the cursor row
                double kitaLat = allCursor.getDouble(COL_LAT);
                double kitaLong = allCursor.getDouble(COL_LONG);
                String kitaName = allCursor.getString(COL_NAME);
                int kitaID = allCursor.getInt(COL_FK_KITA_ID);
                LatLng kitaPosition = new LatLng(kitaLat, kitaLong);

                // create a marker from that data
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(kitaPosition)
                        .visible(true)
                        .alpha(0.25f)
                        .icon(BitmapDescriptorFactory.defaultMarker(210))
                        .flat(true)
                        .title(kitaName)
                        .snippet(String.valueOf(kitaID))
                );

                // Add the marker to the sparse Array
                mMarkerSparseArray.put(kitaID, marker);

                // update this row for mapstatus "invisible"
                ContentValues contentValues = new ContentValues();
                contentValues.put(KitaContract.LocationEntry.COLUMN_MAPST,
                        getString(R.string.location_option_light));
                int rowUpdated = getActivity().getContentResolver().update(
                        KitaContract.LocationEntry.CONTENT_URI,
                        contentValues,
                        KitaContract.LocationEntry.COLUMN_FK_KITA_ID + " = ?",
                        new String[] {String.valueOf(kitaID)});

                // count markers + updated rows
                markerCount++;
                rowsUpdated += rowUpdated;
            } while (allCursor.moveToNext());

        else Log.d(TAG, "allCursor == null");

        if (allCursor != null) allCursor.close();

        Log.d(TAG, "nach Erstellen der Marker. Marker created: " + markerCount +
        "\n Rows updated: " + rowsUpdated);


    }

    private void setKitaMarker() {
        Log.d(TAG, "setKitaMarker()");

        // set counter for updated rows
        int rowsUpdated = 0;


//        // get all Kita Locations within the searchRadius (+500m) and make them "light"
//        Cursor radiusCursor = getLocationsInRadius();
//        if (radiusCursor != null && radiusCursor.moveToFirst()) {
//            do {
//                // get the kita-ID for this row
//                int kitaId = radiusCursor.getInt(COL_FK_KITA_ID);
//
//                // get the marker from the sparse array and set to visible/light
//                Marker marker = mMarkerSparseArray.get(kitaId);
//                if (marker != null) {
//                    marker.setVisible(true);
//                    marker.setAlpha(0.25f);
//                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(210));
//                    marker.setFlat(true);
//                } else Log.e(TAG, "radiusCursor => marker could not be found at ID: "+ kitaId);
//
//                // update this row for mapstatus "invisible"
//                ContentValues contentValues = new ContentValues();
//                contentValues.put(KitaContract.LocationEntry.COLUMN_MAPST,
//                        getString(R.string.location_option_light));
//                int rowUpdated = getActivity().getContentResolver().update(
//                        KitaContract.LocationEntry.CONTENT_URI,
//                        contentValues,
//                        KitaContract.LocationEntry.COLUMN_FK_KITA_ID + " = ?",
//                        new String[]{String.valueOf(kitaId)});
//                rowsUpdated += rowUpdated;
//            } while (radiusCursor.moveToNext());
//            Log.d(TAG, "Rows updated to set Kita Locations to 'light' :" + rowsUpdated);
//        } else Log.e(TAG, "No Kitas were inside searchRadius, non updated.");
//
//        if (radiusCursor != null) radiusCursor.close();


        // get all active Kita Locations (according to filter settings) and make them "bold"
        Cursor activeCursor = getActiveKitasWithLocations();
        if (activeCursor != null && activeCursor.moveToFirst()) {
            do {
                int kitaId = activeCursor.getInt(COL_FK_KITA_ID);
                Marker marker = mMarkerSparseArray.get(kitaId);
                if (marker != null) {
                    marker.setVisible(true);
                    marker.setAlpha(1.0f);
                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(240));
                    marker.setFlat(false);
                } else Log.e(TAG, "activeCursor => marker could not be found at ID: "+ kitaId);

                // update this row for mapstatus "invisible"
                ContentValues contentValues = new ContentValues();
                contentValues.put(KitaContract.LocationEntry.COLUMN_MAPST,
                        getString(R.string.location_option_bold));
                int rowUpdated = getActivity().getContentResolver().update(
                        KitaContract.LocationEntry.CONTENT_URI,
                        contentValues,
                        KitaContract.LocationEntry.COLUMN_FK_KITA_ID + " = ?",
                        new String[]{String.valueOf(kitaId)});
                rowsUpdated += rowUpdated;

            } while (activeCursor.moveToNext());
            Log.d(TAG, "Rows updated to set Kita Locations to 'bold' :" + rowsUpdated);
        } else Log.e(TAG, "No Kitas were active according to filter settings, non updated.");

        if (activeCursor != null) activeCursor.close();


        Log.d(TAG, "Marker are set.");
    }


        public static class OverviewViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            TextView öffnungsz;
            TextView aufnahmea;
            TextView sprache;

            OverviewViewHolder(View itemView) {
                super(itemView);
                name = (TextView) itemView.findViewById(R.id.kita_name);
                öffnungsz = (TextView) itemView.findViewById(R.id.kita_öffnungszeiten);
                aufnahmea = (TextView) itemView.findViewById(R.id.kita_aufnahmealter);
                sprache = (TextView) itemView.findViewById(R.id.kita_sprache);

            }
        }

    private Cursor getAllKitasWithLocationsInRadius() {
        Context context = getContext();
        Activity activity = getActivity();

        //open filter preferences
        SharedPreferences sharedPref = activity.getSharedPreferences("filter",
                Context.MODE_PRIVATE);

        //get the filter settings for each filter
        float searchRadius = (float) sharedPref.getInt(getString(R.string.search_radius), -1);
        // translate searchRaduis value 0 to 0.5 km
        if (searchRadius == 0) searchRadius = 0.5f;

        //query All Kitas with location
        return context.getContentResolver()
                .query(KitaContract.KitaEntry.buildKitaUriWithLocation(
                        context.getString(R.string.location_option_all)),
                        null,
                        KitaProvider.sDistanzSelection,
                        new String[] {String.valueOf(1000 * searchRadius + 500)},
                        null);
    }

//    private Cursor getLocationsInRadius() {
//        Context context = getContext();
//        Activity activity = getActivity();
//
//        //open filter preferences
//        SharedPreferences sharedPref = activity.getSharedPreferences("filter",
//                Context.MODE_PRIVATE);
//
//        //get the filter settings for each filter
//        float searchRadius = (float) sharedPref.getInt(getString(R.string.search_radius), -1);
//        // translate searchRaduis value 0 to 0.5 km
//        if (searchRadius == 0) searchRadius = 0.5f;
//
//        //query all Kitas within given search radius
//        return context.getContentResolver()
//                .query(KitaContract.LocationEntry.CONTENT_URI,
//                        null,
//                        KitaProvider.sDistanzSelection + " AND "
//                                + KitaProvider.sLocationKitaSelection,
//                        new String[]{
//                                String.valueOf(1000 * searchRadius + 500),
//                                context.getString(R.string.location_option_all)},
//                        null);
//    }

    private Cursor getVisibleLocations() {
        Context context = getContext();
        Activity activity = getActivity();

        //open filter preferences
        SharedPreferences sharedPref = activity.getSharedPreferences("filter",
                Context.MODE_PRIVATE);

        //get the filter settings for each filter
        float searchRadius = (float) sharedPref.getInt(getString(R.string.search_radius), -1);
        // translate searchRaduis value 0 to 0.5 km
        if (searchRadius == 0) searchRadius = 0.5f;

        //query all Kitas within given search radius
        return context.getContentResolver()
                .query(KitaContract.LocationEntry.CONTENT_URI,
                        null,
                        KitaProvider.sDistanzSelection + " AND "
                                + KitaProvider.sLocationVisibleSelection,
                        new String[]{
                                context.getString(R.string.location_option_light),
                                context.getString((R.string.location_option_bold))},
                        null);
    }


    private Cursor getActiveKitasWithLocations() {
        Context context = getContext();
        Activity activity = getActivity();
        //open filter preferences
        SharedPreferences sharedPref = activity.getSharedPreferences("filter",
                Context.MODE_PRIVATE);

        //get the filter settings for each filter
        float searchRadius = (float) sharedPref.getInt(getString(R.string.search_radius), -1);
        int minAge = sharedPref.getInt(activity.getString(R.string.minimum_age), -1);
        float morningTime = sharedPref.getFloat(activity.getString(R.string.morning_time), -1.0f);
        float eveningTime = sharedPref.getFloat(activity.getString(R.string.evening_time), -1.0f);
        float openingHours = sharedPref.getFloat(activity.getString(R.string.opening_hours), -1.0f);
        String language = sharedPref.getString(activity.getString(R.string.language), "disable");

        // translate a searchRadius of 0 to 0.5 km
        if (searchRadius == 0) searchRadius = 0.5f;

        Log.d(TAG, "------------  Filter Settings (MapFragment) --------------");
        Log.d(TAG, "    searchRadius: " + searchRadius);
        Log.d(TAG, "    minAge: " + minAge);
        Log.d(TAG, "    morningTime: " + morningTime);
        Log.d(TAG, "    eveningTime: " + eveningTime);
        Log.d(TAG, "    openingHours: " + openingHours);
        Log.d(TAG, "    language: " + language);

        //make array with all the values
        String[] filterValues = {
                String.valueOf(1000 * searchRadius),
                String.valueOf(minAge),
                String.valueOf(morningTime),
                String.valueOf(eveningTime),
                String.valueOf(openingHours),
                language
        };

        //make array with the selection phrases for each filter
        String[] filterSelectionList = {
                KitaProvider.sDistanzSelection,
                KitaProvider.sAufnahmealterSelection,
                KitaProvider.sÖffnetSelection,
                KitaProvider.sSchließtSelection,
                KitaProvider.sOpeningHoursSelection,
                KitaProvider.sFremdspracheSelection
        };

        List<String> argsList = new ArrayList<>();

        Log.d(TAG, "filterList.size(): " + filterValues.length);

        String filterSelection = "";
        int index = 0;

        //running through the filters with number values (first4)
        while (index < filterValues.length - 1) {
            Log.d(TAG, "index: " + index);
            if (Float.parseFloat(filterValues[index]) >= 0) {
                Log.d(TAG, String.format("Filter at index %d activ", index));
                if (filterSelection.equals("")) filterSelection = filterSelection
                        + filterSelectionList[index];
                else filterSelection = filterSelection + " AND " + filterSelectionList[index];
                argsList.add(filterValues[index]);
            } else Log.d(TAG, String.format("Filter at index %d nicht activ", index));
            index++;
        }
        Log.d(TAG, "filterValues[index]:" + filterValues[index]);

        // handle extra case for language (not numeric)
        if (filterValues[index].equals("disable"))
            Log.d(TAG, String.format("Filter at index %d not activ", index));
        else {
            Log.d(TAG, String.format("Filter at index %d activ", index));
            if (filterSelection.equals("")) filterSelection = filterSelection
                    + filterSelectionList[index];
            else filterSelection = filterSelection + " AND " + filterSelectionList[index];
            argsList.add(filterValues[index]);
        }

        //convert argsList (type List<String>) to args (type String[])
        String[] args = new String[argsList.size()];
        argsList.toArray(args);

        // declare sortOrder and Uri
        String sortOrder = KitaContract.LocationEntry.COLUMN_DIST + " ASC";
        Uri joinUri = KitaContract.KitaEntry.buildKitaUriWithLocation(
                context.getString(R.string.location_option_all));

        //Log of query parameters
        Log.d(TAG, "filterSelection: " + filterSelection);
        for (String arg : args) {
            Log.d(TAG, "arg: " + arg);
        }

        //query all Kitas with given sort order
        return context.getContentResolver()
                .query(joinUri, null, filterSelection, args, sortOrder);

    }

}
