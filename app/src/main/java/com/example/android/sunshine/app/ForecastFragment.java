package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.sunshine.app.data.WeatherContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    private ForecastAdapter mForecastAdapter;

    private final String LOG_TAG = ForecastFragment.class.getSimpleName();

    SharedPreferences prefs;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        String locationSetting = Utility.getPreferredLocation(getActivity());

        //Sort order: Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis()
        );

        Cursor cur = getActivity().getContentResolver().query(weatherForLocationUri, null, null, null, sortOrder);

        mForecastAdapter = new ForecastAdapter(getContext(), cur, 0);

        View rootView = inflater.inflate(R.layout.fragment_forecast, container, false);

        ListView forecastLV = (ListView) rootView.findViewById(R.id.listview_forecast);
        forecastLV.setAdapter(mForecastAdapter);
        forecastLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //String forecastInfo = weekForecast.get(position);
                /*Intent startDetailActivity = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, forecastInfo);
                startActivity(startDetailActivity);*/
            }
        });
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshWeather();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id){
            case R.id.action_refresh:
                refreshWeather();
                break;
            case R.id.view_location:
                String location = Utility.getPreferredLocation(getActivity());
                Uri mapLocationQuery = Uri.parse("geo:0,0?q=")
                        .buildUpon()
                        .appendQueryParameter("q", location)
                        .build();
                showMap(mapLocationQuery);
                break;
            case R.id.action_settings:
                Intent forecastSettings = new Intent(getActivity(), ForecastSettingsActivity.class);
                startActivity(forecastSettings);
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void refreshWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask(getContext());
        String location = prefs.getString(getString(R.string.pref_general_location_key), getString(R.string.pref_general_location_default));
        Log.d(LOG_TAG, location);
        weatherTask.execute(location);

    }

    public void showMap(Uri geoLocation) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);
        if (intent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(LOG_TAG, "Couldn't start a map activity.");
        }
    }

}
