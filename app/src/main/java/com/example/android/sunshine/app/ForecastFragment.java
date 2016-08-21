package com.example.android.sunshine.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;

    private final String LOG_TAG = ForecastFragment.class.getSimpleName();

    private String zipCode = null;
    private String units = null;
    SharedPreferences prefs;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        zipCode = prefs.getString("location", "30533");
        units = prefs.getString("units", "metric");
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        String[] forecastArray = { "Today - Rainy - 88/82", "Tomorrow - Sunny - 90/82", "Wednesday - Cloudy - 88/82", "Today - Rainy - 88/82", "Today - Rainy - 88/82"};

        final List<String> weekForecast = new ArrayList<String>(Arrays.asList(forecastArray));


        mForecastAdapter =
                new ArrayAdapter<String>(
                        getActivity(),
                        R.layout.list_item_forecast,
                        R.id.list_item_forecast_textview,
                        weekForecast);

        View rootView = inflater.inflate(R.layout.fragment_forecast, container, false);

        ListView forecastLV = (ListView) rootView.findViewById(R.id.listview_forecast);
        forecastLV.setAdapter(mForecastAdapter);
        forecastLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecastInfo = weekForecast.get(position);
                //Toast weatherItemToast = Toast.makeText(getActivity(), forecastInfo, Toast.LENGTH_LONG);
                //weatherItemToast.show();

                Intent startDetailActivity = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, forecastInfo);
                startActivity(startDetailActivity);
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

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_refresh) {
            if(getZipCode() != null){

                refreshWeather();
            } else {
                //displaySetZipDialog();
                Toast setZipToast = Toast.makeText(getActivity(), "Set Zip in Settings.", Toast.LENGTH_SHORT );
                setZipToast.show();
            }
            return true;
        }*/

        switch(id){
            case R.id.action_refresh:
                refreshWeather();
                break;
            case R.id.view_location:
                String location = prefs.getString(getString(R.string.pref_general_location_key),
                        getString(R.string.pref_general_location_default));
                Uri mapLocationQuery = Uri.parse("geo:0,0?q=")
                        .buildUpon()
                        .appendQueryParameter("q", location)
                        .build();
                showMap(mapLocationQuery);
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void refreshWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        String location = prefs.getString(getString(R.string.pref_general_location_key),
                getString(R.string.pref_general_location_default));
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

    public void displaySetZipDialog(){

        LayoutInflater alertInflater = LayoutInflater.from(getActivity());
        final View inflater = alertInflater.inflate(R.layout.zip_prompt, null);
        AlertDialog.Builder aDBuilder = new AlertDialog.Builder(getActivity());



        final Pattern zipPattern = Pattern.compile("^[0-9]{5}");
        final EditText zipText = (EditText) inflater.findViewById(R.id.zip_code_input);


        aDBuilder.setView(inflater)
                .setMessage("Enter Your Zip Code.")
                .setTitle("Zip Code")
                .setPositiveButton("Set Zip", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String zipString = zipText.getText().toString();

                        final Matcher zipMatcher = zipPattern.matcher(zipString);
                        boolean zipVerified = zipMatcher.matches();

                        if(zipVerified){
                            setZipCode(zipString);
                            refreshWeather();
                        } else {
                            zipText.setText("Enter 5 Digit Zip");
                            displaySetZipDialog();
                        }
                    }
                })

                .setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){
                        dialog.cancel();
                    }
                });

        aDBuilder.show();
    }

    public void setZipCode(String zipCode){
        this.zipCode = zipCode;
    }

    public String getZipCode(){
        return this.zipCode;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]>{

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        /*private AsyncTask<Void, Void, Void> updateTask = this;

        @Override
        protected void onPreExecute(){

        }*/

        private String[] weatherData;

        @Override
        protected String[] doInBackground(String... params){

            String weatherForecastData = getWeatherForecastData(params[0]);

            try {
                 weatherData = getWeatherDataFromJson(weatherForecastData, 7);
            } catch (JSONException e){
                Log.e(LOG_TAG, "Error ", e);

                // If the code didn't successfully get the Weather Data from the JSON
                // return from the function.
                return null;
            }

            return weatherData;
        }

        @Override
        protected void onPostExecute(String[] results) {
            super.onPostExecute(results);
            mForecastAdapter.clear();

            for (String oneWeather : results){
                mForecastAdapter.add(oneWeather);
            }
        }

        private String getWeatherForecastData(String zipCode){
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast

                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";

                Uri openWeatherMapQuery = Uri.parse(FORECAST_BASE_URL)
                        .buildUpon()
                        .appendQueryParameter("zip", zipCode + ",us")
                        .appendQueryParameter("appid", BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                        .appendQueryParameter("units", "metric")
                        .appendQueryParameter("mode", "json")
                        .appendQueryParameter("cnt", "7")
                        .build();

                URL url = new URL(openWeatherMapQuery.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return forecastJsonStr;
        }

        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.

            String units = prefs.getString(getString(R.string.pref_general_units_key),
                    getString(R.string.pref_general_units_metric));

            if(units.equals(getString(R.string.pref_general_units_imperial))) {
                high = (high * 1.8) + 32;
                low = (low * 1.8) +32;
            } else if (!units.equals(getString(R.string.pref_general_units_metric))) {
                Log.d(LOG_TAG, "Unit type not found: " + units);
            }

            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            /*for (String s : resultStrs) {
                Log.v(LOG_TAG, "Forecast entry: " + s);
            }*/
            return resultStrs;

        }

    }
}
