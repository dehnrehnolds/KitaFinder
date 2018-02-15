package com.example.robert.kitafinder;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.InterpolatorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.robert.kitafinder.data.RefreshTrigger;
import com.google.android.gms.wearable.MessageEvent;

import org.greenrobot.eventbus.EventBus;
import org.w3c.dom.Text;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.V;
import static java.lang.reflect.Array.getInt;

/**
 * Created by Robert on 28.10.2017.
 */

public class FilterFragment extends BottomSheetDialogFragment implements AdapterView.OnItemSelectedListener {

    // Variable to avoid re-setting spinners
    private int mCheck = 0;
    private BottomSheetBehavior mBottomSheetBehavior;
    public static final String TAG = FilterFragment.class.getSimpleName();
    private RadioGroup rg1;
    private RadioGroup rg2;
    SharedPreferences mSharedPref;
    SharedPreferences.Editor mEditor;

    public FilterFragment(){
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mSharedPref = getActivity().getSharedPreferences("filter", Context.MODE_PRIVATE);
        mEditor = mSharedPref.edit();
        super.onCreateView(inflater, container, savedInstanceState);
        mCheck = 0;
        View rootView = inflater.inflate(R.layout.bottom_sheet_filter,container,false);
        setupSpinner(rootView);
        setupSeekbar(rootView);
        setupRadioGroup(rootView);

        mBottomSheetBehavior = BottomSheetBehavior.from(
                getActivity().findViewById(R.id.filter_fragment_container));
        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                Log.d(TAG, "newState! newState = " + newState);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                Log.d(TAG, "onSlide! slideOffset = " + slideOffset);
            }
        });
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mCheck = 0;
        Log.d(TAG, "mCheck = 0");
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        Log.d(TAG, "mCkeck = ? " + mCheck);
        // Achtung !!! mCheck might have to be reduced after converting openingHours to a SeekBar
        if(++mCheck > 2) {
            //Which Spinner has been clicked?
            if (parent.getId() == R.id.morning_spinner) {
                float morningTime = -1.0f;
                String morningTimeString = parent.getItemAtPosition(pos).toString();
                Log.d(TAG, "moningTimeString.length= " + morningTimeString.length());
                Log.d(TAG, "ItemId: " + parent.getId() + "  moiningSpinnerID = " + R.id.morning_spinner);
                //calculate float from the String (e.g.: 6:30 -> 6.5)
                if (morningTimeString.length() == 5)
                    morningTime = Float.parseFloat(morningTimeString.substring(0, 2))
                            + Float.parseFloat(morningTimeString.substring(3, 5)) / 60.0f;
                mEditor.putFloat(getString(R.string.morning_time), morningTime);
                Log.d(TAG, "morningTime stored: " + morningTime);
                mEditor.commit();
            } else if (parent.getId() == R.id.evening_spinner) {
                float eveningTime = -1.0f;
                Log.d(TAG, "ItemId: " + parent.getId() + "  eveningSpinnerID = " + R.id.evening_spinner);
                String eveningTimeString = parent.getItemAtPosition(pos).toString();
                //calculate float from the String (e.g.: 6:30 -> 6.5)
                if (eveningTimeString.length() == 5)
                    eveningTime = Float.parseFloat(eveningTimeString.substring(0, 2))
                            + Float.parseFloat(eveningTimeString.substring(3, 5)) / 60.0f;
                mEditor.putFloat(getString(R.string.evening_time), eveningTime);
                Log.d(TAG, "eveningTime stored: " + eveningTime);
                mEditor.commit();
            }
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    public void setupDialog(Dialog dialog, int style) {
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_filter, null);
        dialog.setContentView(contentView);
    }



    private void setupSeekbar(View rootView) {

        // --------------- minimum Age seekBar --------------
        SeekBar seekBarAge = (SeekBar)rootView.findViewById(R.id.min_age_slider);
        seekBarAge.setProgress(0);
        seekBarAge.setSecondaryProgress(0);
        seekBarAge.incrementProgressBy(1);
        seekBarAge.setMax(37);
        final TextView seekBarAgeValue = (TextView)rootView.findViewById(R.id.min_age_text);
        int minAge = mSharedPref.getInt(getString(R.string.minimum_age), -1);
        if (minAge < 0) {
            Log.d(TAG, "minAge < 0");
            seekBarAgeValue.setText("egal");
            seekBarAge.setProgress(37);
        } else if (minAge == 1){
            seekBarAgeValue.setText(String.format("%d Monat", minAge));
            seekBarAge.setProgress(minAge);
        } else if (minAge <= 36){
            seekBarAgeValue.setText(String.format("%d Monate", minAge));
            seekBarAge.setProgress(minAge);
        } else Log.e(TAG, "minimales Aufnahmealter außerhalb des definierten Bereichs");

        seekBarAge.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
              @Override
              public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                  if (progress == 1) {
                      seekBarAgeValue.setText(String.format("%d Monat", progress));
                  } else if (progress <= 36) {
                      seekBarAgeValue.setText(String.format("%d Monate", progress));
                  } else if (progress == 37) {
                      seekBarAgeValue.setText("egal");
                  } else Log.e(TAG, "minimales Aufnahmealter außerhalb des definierten Bereichs");
              }

              @Override
              public void onStartTrackingTouch(SeekBar seekBar) {
              }

              @Override
              public void onStopTrackingTouch(SeekBar seekBar) {
                  if (seekBar.getProgress() <= 36) {
                      mEditor.putInt(getString(R.string.minimum_age), seekBar.getProgress());
                      Log.d(TAG, String.format("Put minimum age %d to sharedPrefs.", seekBar.getProgress()));
                  } else if (seekBar.getProgress() == 37) {
                      mEditor.putInt(getString(R.string.minimum_age), -1);
                      Log.d(TAG, String.format("Put minimum age %d to sharedPrefs.", -1));
                  } else Log.e(TAG, "no valid minAge choosen by slider");

                  mEditor.commit();
              }
        });

        // --------  Opening Hours seekBar ----------
        SeekBar seekBarOpen = (SeekBar)rootView.findViewById(R.id.opening_hours_slider);
        seekBarOpen.setProgress(0);
        seekBarOpen.setSecondaryProgress(0);
        seekBarOpen.incrementProgressBy(1);
        seekBarOpen.setMax(28);
        final TextView seekBarOpenValue = (TextView)rootView.findViewById(R.id.opening_hours_text);
        float openingHours = mSharedPref.getFloat(getString(R.string.opening_hours),-1.0f);
        if (openingHours <= 0) {
            Log.d(TAG, "minAge <= 0");
            seekBarOpenValue.setText("egal");
            seekBarOpen.setProgress(0);
        } else if (openingHours == 1){
            seekBarOpenValue.setText(String.format("%1.1f Stunde", openingHours));
            seekBarOpen.setProgress((int) openingHours*2);
        }
        else if (openingHours <= 28){
            seekBarOpenValue.setText(String.format("%1.1f Stunden", openingHours));
            seekBarOpen.setProgress((int) openingHours*2);
        } else Log.e(TAG, "Öffnungsdauer außerhalb des definierten Bereichs");

        seekBarOpen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                if (progress == 0)
                    seekBarOpenValue.setText(String.format("egal", progress));
                 else if (progress == 2)
                    seekBarOpenValue.setText(String.format("%1.1f Stunde", progress/2.0f));
                else if (progress <= 28)
                    seekBarOpenValue.setText(String.format("%1.1f Stunden", progress/2.0f));
                else Log.e(TAG, "Öffnungsdauer außerhalb des definierten Bereichs");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() == 0) {
                    mEditor.putFloat(getString(R.string.opening_hours), -1.0f);
                    Log.d(TAG, String.format("Put openingHours %1.1f to sharedPrefs.", -1.0f));
                }
                else if (seekBar.getProgress() <= 28){
                    mEditor.putFloat(getString(R.string.opening_hours), seekBar.getProgress() / 2.0f);
                    Log.d(TAG, String.format("Put openingHours %1.1f to sharedPrefs.", seekBar.getProgress() / 2.0f));
                }
                mEditor.commit();
            }
        });

    }

    private void setupSpinner(View rootView) {
        // ------- Öffnungszeiten Filter (Spinner) -------
        Spinner morningSpinner = (Spinner) rootView.findViewById(R.id.morning_spinner);
        Spinner eveningSpinner = (Spinner) rootView.findViewById(R.id.evening_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> morningAdapter = ArrayAdapter.createFromResource(rootView.getContext(),
                R.array.morning_times, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> eveningAdapter = ArrayAdapter.createFromResource(rootView.getContext(),
                R.array.evening_times, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        morningAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        eveningAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        morningSpinner.setAdapter(morningAdapter);
        eveningSpinner.setAdapter(eveningAdapter);
        morningSpinner.setOnItemSelectedListener(this);
        eveningSpinner.setOnItemSelectedListener(this);



        //Open shared prefs for setting Spinner selection value
        // -- morning time
        String stunde = "";
        String minute = "";
        String morningTimeString = "egal";
        float morningTime = mSharedPref.getFloat(getString(R.string.morning_time), -1.0f);
        Log.d(TAG, "morningTime from sharedPrefs: " + morningTime);

        // Build String from float value
        if (morningTime > 0 && morningTime < 10)
            stunde = "0" + (int) (morningTime-morningTime%1);
        else if (morningTime > 10 && morningTime < 24)
            stunde = Integer.toString((int) (morningTime-morningTime%1));
        if (morningTime%1 == 0)
            minute = "00";
        else if (morningTime != 0)
            minute = Integer.toString((int)(morningTime%1*60));

        Log.d(TAG, "MorningTime : " + stunde + ":" + minute);
        // Wenn die Zeit gesetzt wurde (stunde länger 0) ...ansonster leerer String
        if (stunde.length()>0) morningTimeString = stunde + ":" + minute;

        //Find position of String in Item-Array and set Spinner to that position
        int positionMorningSpinner = morningAdapter.getPosition(morningTimeString);
        morningSpinner.setSelection(positionMorningSpinner);

        // --- evening time
        stunde = "";
        minute = "";
        String eveningTimeString = "egal";
        float eveningTime = mSharedPref.getFloat(getString(R.string.evening_time), -1.0f);
        Log.d(TAG, "eveningTime from sharedPrefs: " + eveningTime);
        // Build String from float value
        if (eveningTime > 0 && eveningTime < 10)
            stunde = "0" + (int) (eveningTime-eveningTime%1);
        else if (eveningTime > 10 && eveningTime < 24)
            stunde = Integer.toString((int) (eveningTime-eveningTime%1));
        if (eveningTime%1 == 0)
            minute = "00";
        else if (eveningTime != 0)
            minute = Integer.toString((int)(eveningTime%1*60));

        if (stunde.length()>0) eveningTimeString = stunde + ":" + minute;
        Log.d(TAG, "EveningTimeString: " + eveningTimeString);

        //Find position of String in Item-Array and set Spinner to that position
        int positionEveningSpinner = eveningAdapter.getPosition(eveningTimeString);
        eveningSpinner.setSelection(positionEveningSpinner);

    }


    private RadioGroup.OnCheckedChangeListener listener1 = new RadioGroup.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            Log.d(TAG, "listener1! checkedId: " + checkedId);
            if (checkedId != -1) {
                rg2.setOnCheckedChangeListener(null); // remove the listener before clearing
                // , so we don't throw that stackoverflow exception(like Vladimir Volodin pointed out)
                rg2.clearCheck(); // clear the second RadioGroup!
                rg2.setOnCheckedChangeListener(listener2); //reset the listener

                switch (checkedId) {
                    case R.id.radio_disable:
                        Log.d(TAG, "RodioButton: disable");
                        mEditor.putString(getString(R.string.language), "disable");
                        break;
                    case R.id.radio_none:
                        Log.d(TAG, "RodioButton: none");
                        mEditor.putString(getString(R.string.language), "deutsch");
                        break;
                    case R.id.radio_english:
                        Log.d(TAG, "RodioButton: english");
                        mEditor.putString(getString(R.string.language), "englisch");
                        break;
                    case R.id.radio_spanish:
                        Log.d(TAG, "RodioButton: spanish");
                        mEditor.putString(getString(R.string.language), "spanisch");
                        break;
                }
                mEditor.apply();
            }
        }
    };

    private RadioGroup.OnCheckedChangeListener listener2 = new RadioGroup.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            Log.d(TAG, "listener2! checkedId: " + checkedId);
            Log.d(TAG, "Id RadioButton Turkish: " + R.id.radio_turkish);
            if (checkedId != -1) {
                rg1.setOnCheckedChangeListener(null);
                rg1.clearCheck();
                rg1.setOnCheckedChangeListener(listener1);

                switch (checkedId){
                    case R.id.radio_turkish:
                        Log.d(TAG, "RodioButton: turkish");
                        mEditor.putString(getString(R.string.language), "türkisch");
                        break;
                    case R.id.radio_arabic:
                        Log.d(TAG, "RodioButton: arabic");
                        mEditor.putString(getString(R.string.language), "arabisch");
                        break;
                    case R.id.radio_kurd:
                        Log.d(TAG, "RodioButton: kurdish");
                        mEditor.putString(getString(R.string.language), "kurdisch");
                        break;
                    case R.id.radio_french:
                        Log.d(TAG, "RodioButton: french");
                        mEditor.putString(getString(R.string.language), "französisch");
                        break;
                }
                mEditor.apply();
            }
        }
    };

    private void setupRadioGroup(View rootView){

        rg1 = (RadioGroup) rootView.findViewById(R.id.language_radio1);
        rg2 = (RadioGroup) rootView.findViewById(R.id.language_radio2);
        rg1.clearCheck(); // this is so we can start fresh, with no selection on both RadioGroups
        rg2.clearCheck();
        // language is the filter setting which can be: {"disabled", "none", "english", "spanish",
        // "turkish", "arabic", "chinese", "french"}
        String language = mSharedPref.getString(getString(R.string.language), "disable");
        Log.d(TAG, "Language setting is '" + language + "'");
        if (language.equals("disable")){
            rg1.check(R.id.radio_disable);

        }else if (language.equals("deutsch")){
            rg1.check(R.id.radio_none);

        }else if (language.equals("englisch")){
            rg1.check(R.id.radio_english);

        }else if (language.equals("spanisch")){
            rg1.check(R.id.radio_spanish);

        }else if (language.equals("türkisch")){
            rg2.check(R.id.radio_turkish);

        }else if (language.equals("arabisch")){
            rg2.check(R.id.radio_arabic);

        }else if (language.equals("kurdisch")){
            rg2.check(R.id.radio_kurd);

        }else if (language.equals("französisch")){
            rg2.check(R.id.radio_french);

        }else {
            Log.e(TAG, "Language setting doesn't match given languages!");
        }
        rg1.setOnCheckedChangeListener(listener1);
        rg2.setOnCheckedChangeListener(listener2);

    }

    public void onPause(){
        super.onPause();
        int minAge = mSharedPref.getInt(getString(R.string.minimum_age),-1);
        float morningTime = mSharedPref.getFloat(getString(R.string.morning_time), -1.0f);
        float eveningTime = mSharedPref.getFloat(getString(R.string.evening_time), -1.0f);
        float openingHours = mSharedPref.getFloat(getString(R.string.opening_hours),-1.0f);
        String language = mSharedPref.getString(getString(R.string.language), "disable");
        Log.d(TAG, "-------------  Filter Settings (FilterFragment) --------------");
        Log.d(TAG, "    minAge: " + minAge);
        Log.d(TAG,"    morningTime: " + morningTime);
        Log.d(TAG,"    eveningTime: " + eveningTime);
        Log.d(TAG,"    openingHours: " + openingHours);
        Log.d(TAG,"    language: " + language);
        Log.d(TAG, "Send RefreshTrigger event _ ___ _ ___ _ ___ _ ___ _ ___ _ ");
        EventBus.getDefault().post(new RefreshTrigger());
    }
}


