package com.example.robert.kitafinder;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * Created by Robert on 23.10.2017.
 */

public class FavouriteFragment extends Fragment {
    private static final String TAG = ListFragment.class.getSimpleName();
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    /*
    Muss mFilter in Settings hintelegt werden??
     */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Context context = getContext();
        final View rootView = inflater.inflate(R.layout.fragment_fav, container, false);

        mRecyclerView = rootView.findViewById(R.id.fav_list);

        // to improve performance
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(context);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // create FavAdapter
        mAdapter = new FavAdapter(context, new FavAdapter.ClickListener() {
            @Override
            public void onPositionClicked(int position, int kitaId) {

            }

            @Override
            public void onLongClicked(int position, int kitaId) {
                Log.d(TAG, "Long-clicked on: -position: " + position + "  -kitaId: " + kitaId);
            }
        });

        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
