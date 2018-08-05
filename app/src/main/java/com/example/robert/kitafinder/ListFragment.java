package com.example.robert.kitafinder;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.robert.kitafinder.data.DistancesCalculatedTrigger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Created by Robert on 23.10.2017.
 */

public class ListFragment extends Fragment{

    private static final String TAG = ListFragment.class.getSimpleName();
    private  RecyclerView mRecyclerView;
    private  RecyclerView.Adapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private  Context mContext;
    private Location mSearchAddress;


    /*
    Muss mFilter in Settings hintelegt werden??
     */


    public ListFragment(){
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getContext();
        final View rootView = inflater.inflate(R.layout.fragment_list, container, false);


        mRecyclerView = rootView.findViewById(R.id.kita_list);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(mLayoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                mLayoutManager.getOrientation());
        mRecyclerView.addItemDecoration(dividerItemDecoration);

        return rootView;
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart()");

        //open SharedPrefs & editor
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();
        boolean addressChanged = prefs.getBoolean(getString(R.string.pref_address_changed_list), false);

        mSearchAddress = getActivity().getIntent().getParcelableExtra("address");

        if (addressChanged) {
            new GetDistanceTask(getActivity(),
                    getActivity().getSupportFragmentManager(),
                    getActivity().getContentResolver(),
                    mSearchAddress)
                    .execute();
            Log.d(TAG, "GetDistanceTask.execute()");
            editor.putBoolean(getString(R.string.pref_address_changed_list), false);
            editor.apply();
        } else setAdapter();
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }


    private void setAdapter(){

        // get the active filter TODO
        mAdapter = new ListAdapter(mContext,"none", new ListAdapter.ClickListener() {
            @Override
            public void onPositionClicked(int position, int kitaId) {
            }

            @Override
            public void onLongClicked(int position, int kitaId) {
                Log.d(TAG, "Long-clicked on: -position: "+position+"  -kitaId: "+ kitaId);
            }
        });
        mRecyclerView.setAdapter(mAdapter);
    }

    @Subscribe
    public void initRecycleView(DistancesCalculatedTrigger event) {
        // same effect as before, but different reason to trigger
        setAdapter();
    }
}