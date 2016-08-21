package com.example.android.sunshine.app;


import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 */
public class ForecastSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener{


    public ForecastSettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(final Bundle savedInstanceState){
        // applyTheme();
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
        //android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) getActivity().findViewById(R.id.toolbar);
        //((ActionBarActivity)getActivity()).setSupportActionBar(toolbar);

        // For all preferences, attach an OnPreferenceChangeListener so the UI summary can be
        // updated when the preference changes.
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_general_location_key)));

        //Toolbar actionbar = (Toolbar) getActivity().findViewById(R.id.actionbar);
        //actionbar.setTitle("Settings");
        //actionbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_action_back));
        /*actionbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });*/
    }

    /*@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_forecast_settings, container, false);
    }*/

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener immediately with the preference's
        // current value.
        onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else {
            // For other preferences, set the summary to the value's simple string representation.
            preference.setSummary(stringValue);
        }
        return true;
    }

}
