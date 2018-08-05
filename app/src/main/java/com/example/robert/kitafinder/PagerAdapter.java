package com.example.robert.kitafinder;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import com.google.android.gms.maps.SupportMapFragment;


/**
 * Created by Robert on 23.10.2017.
 */

public class PagerAdapter extends FragmentStatePagerAdapter{
    int mNumOfTabs;
    final static String TAG = PagerAdapter.class.getSimpleName();

    public PagerAdapter(FragmentManager fm, int NumOfTabs, Activity activity) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 1:
                Log.d(TAG, "new ListFragment()");
                ListFragment tab1 = new ListFragment();
                return tab1;
            case 0:
                Log.d(TAG, "new MapFragment()");
                SupportMapFragment tab2 = new MapFragment();
                return tab2;
            case 2:
                Log.d(TAG, "new FavouriteFragment()");
                FavouriteFragment tab3 = new FavouriteFragment();
                return tab3;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }

}