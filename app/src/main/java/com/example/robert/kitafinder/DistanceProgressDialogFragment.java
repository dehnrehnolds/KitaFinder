package com.example.robert.kitafinder;

/**
 * Created by Robert on 16.05.2018.
 */

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class DistanceProgressDialogFragment extends DialogFragment
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }



    @NonNull
    @Override
    public ProgressDialog onCreateDialog(Bundle savedInstanceState)
    {
        ProgressDialog dialog = new ProgressDialog(getActivity(), getTheme());
        dialog.setTitle("Kita Guide");
        dialog.setMessage("Suche Kitas in der Nähe");
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        return dialog;
    }
}