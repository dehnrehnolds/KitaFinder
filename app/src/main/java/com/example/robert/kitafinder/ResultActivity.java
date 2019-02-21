package com.example.robert.kitafinder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.robert.kitafinder.data.DetailTrigger;
import com.example.robert.kitafinder.data.DistancesCalculatedTrigger;
import com.example.robert.kitafinder.data.FavRefreshTrigger;
import com.example.robert.kitafinder.data.ListRefreshTrigger;
import com.example.robert.kitafinder.data.MapRefreshTrigger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;


public class ResultActivity extends AppCompatActivity {

    Intent detailIntent;
    SharedPreferences mPrefs;
    SharedPreferences.Editor mEditor;
    private static final String TAG = ResultActivity.class.getSimpleName();
    private boolean mTwoPane;

    @Override
    public void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop()");
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe
    public void startDetailIntent(DetailTrigger event) {
        detailIntent.putExtra("kitaId", event.kitaId);
        Log.d(TAG, "detailIntent extra: event.kitaId= " + event.kitaId);
        startActivity(detailIntent);
        Log.d(TAG, "detailIntent starts Activity");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        detailIntent = new Intent(this, DetailActivity.class);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mPrefs.edit();

        boolean addressChanged = mPrefs.getBoolean(getString(R.string.pref_address_changed_map),
                false);
        if (addressChanged) {
            Log.d(TAG, "addressChanged, MapRefreshTrigger posted");
            EventBus.getDefault().post(new MapRefreshTrigger());
            mEditor.putBoolean(getString(R.string.pref_address_changed_map), false);
            mEditor.apply();
        } else Log.d(TAG, "!adressChanged (onCreate())");

//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
//        SharedPreferences.Editor editor = prefs.edit();
//        editor.putFloat(getString(R.string.morning_time), -1.0f);
//        editor.putFloat(getString(R.string.evening_time), -1.0f);
//        editor.putFloat(getString(R.string.opening_hours), 12.0f);
//        editor.putInt(getString(R.string.minimum_age), -1);
//        editor.putString(getString(R.string.language), "dontcare");
//        // Commit the edits!
//        editor.commit();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        // Enable the Up button
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Karte"));
        tabLayout.addTab(tabLayout.newTab().setText("Liste"));
        tabLayout.addTab(tabLayout.newTab().setText("Favouriten"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(2);
        final PagerAdapter adapter = new PagerAdapter
                (getSupportFragmentManager(), tabLayout.getTabCount(), this);
        mViewPager.setAdapter(adapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
                if (tab.getPosition() == 0){
                    Log.d(TAG, "tab.getPosition == 0");
                    boolean addressChanged = mPrefs.getBoolean(getString(R.string.pref_address_changed_map),
                            false);
                    if (addressChanged) {
                        Log.d(TAG, "addressChanged, MapRefreshTrigger posted");
                        EventBus.getDefault().post(new MapRefreshTrigger());
                    } else Log.d(TAG, "!addressChaged (onTabSelected())");
                } else if (tab.getPosition() == 1){
                    Log.d(TAG, "tab.getPosition == 1");
                } else if (tab.getPosition() == 2){
                    EventBus.getDefault().post(new FavRefreshTrigger());
                    Log.d(TAG, "tab.getPosition == 2");
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm = getSupportFragmentManager();
                final FilterFragment filterFragment = new FilterFragment();
                final FragmentTransaction ft = fm.beginTransaction();
                FilterFragment prev = (FilterFragment)getSupportFragmentManager()
                        .findFragmentByTag("FilterFragment");
                if (prev != null) {
                    ft.remove(prev);
                }
                filterFragment.show(ft,"FilterFragment");
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.result, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
