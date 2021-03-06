package com.lotum.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.lotum.BuildConfig;
import com.lotum.R;
import com.lotum.weather.Current;
import com.lotum.weather.Daily;
import com.lotum.weather.Forecast;
import com.lotum.weather.Hourly;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String ERROR_DATA = "data_exception";
    public static final String ERROR_NETWORK = "network_unavailable";
    public static final String DAILY_FORECAST = "DAILY_FORECAST";
    public static final String HOURLY_FORECAST = "HOURLY_FORECAST";
    private Forecast mForecast;

    private TextView mTimeLabel;
    private TextView mTemperatureLabel;
    private TextView mHumidityValue;
    private TextView mPrecipValue;
    private TextView mSummaryLabel;
    private ImageView mIconImage;
    private ImageView mRefreshImage;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: 12/13/2019 check network
        setContentView(R.layout.activity_main);
        initViews();
        // TODO: 12/13/2019 get city latlan from user
        getForecast();
    }

    private void initViews() {
        mTimeLabel = findViewById(R.id.tv_time);
        mTemperatureLabel = findViewById(R.id.tv_temperature);
        mHumidityValue = findViewById(R.id.tv_humidity_value);
        mPrecipValue = findViewById(R.id.tv_precip_value);
        mSummaryLabel = findViewById(R.id.tv_summary);
        mIconImage = findViewById(R.id.img_icon);
        mRefreshImage = findViewById(R.id.img_refresh);
        mProgressBar = findViewById(R.id.progressBar);
        Button daily = findViewById(R.id.btn_daily);
        Button hourly = findViewById(R.id.btn_hourly);

        mProgressBar.setVisibility(View.INVISIBLE);
        mRefreshImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getForecast();
            }
        });

        daily.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DailyForecastActivity.class);
                intent.putExtra(DAILY_FORECAST, mForecast.getDailyForecast());
                startActivity(intent);
            }
        });

        hourly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, HourlyForecastActivity.class);
                intent.putExtra(HOURLY_FORECAST, mForecast.getHourlyForecast());
                startActivity(intent);
            }
        });
    }

    private void getForecast() {

        double latitude = 30.2801;
        double longitude = 31.1106;
        String forecastUrl = "https://api.forecast.io/forecast/" + BuildConfig.API_KEY +
                "/" + latitude + "," + longitude;

        if (isNetworkAvailable()) {
            toggleRefresh();

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastUrl)
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    alertUserAboutError(ERROR_DATA);
                }

                @Override
                public void onResponse(Response response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    try {
                        String jsonData = response.body().string();
                        Log.v(TAG, jsonData);
                        if (response.isSuccessful()) {
                            mForecast = parseForecastDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });
                        } else {
                            alertUserAboutError(ERROR_DATA);
                        }
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }
            });
        } else {
            alertUserAboutError(ERROR_NETWORK);
        }
    }

    private void toggleRefresh() {
        if (mProgressBar.getVisibility() == View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImage.setVisibility(View.INVISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImage.setVisibility(View.VISIBLE);
        }
    }

    private void updateDisplay() {
        Current current = mForecast.getCurrentForecast();
        mTemperatureLabel.setText(Integer.toString(current.getTemperature()));
        mTimeLabel.setText("At " + current.getFormattedTime() + " it will be");
        mHumidityValue.setText(Double.toString(current.getHumidity()));
        mPrecipValue.setText(current.getPercipChance() + "%");
        mSummaryLabel.setText(current.getSummary());

        Drawable drawable = getResources().getDrawable(current.getIconId());
        mIconImage.setImageDrawable(drawable);
    }

    private Forecast parseForecastDetails(String jsonData) throws JSONException {
        Forecast forecast = new Forecast();
        forecast.setCurrentForecast(getCurrentDetails(jsonData));
        forecast.setHourlyForecast(getHourlyDetails(jsonData));
        forecast.setDailyForecast(getDailyDetails(jsonData));
        return forecast;
    }

    private Daily[] getDailyDetails(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timeZone = forecast.getString("timezone");

        JSONObject daily = forecast.getJSONObject("daily");
        JSONArray data = daily.getJSONArray("data");

        int length = data.length();
        Daily[] dailyData = new Daily[length];
        for (int i = 0; i < length; i++) {
            JSONObject jsonDay = data.getJSONObject(i);

            dailyData[i] = new Daily();
            dailyData[i].setSummary(jsonDay.getString("summary"));
            dailyData[i].setTemperatureMax(jsonDay.getDouble("temperatureMax"));
            dailyData[i].setTime(jsonDay.getLong("time"));
            dailyData[i].setIcon(jsonDay.getString("icon"));
            dailyData[i].setTimezone(timeZone);
        }
        return dailyData;
    }

    private Hourly[] getHourlyDetails(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timeZone = forecast.getString("timezone");

        JSONObject hourly = forecast.getJSONObject("hourly");
        JSONArray data = hourly.getJSONArray("data");

        int length = data.length();
        Hourly[] hourlyData = new Hourly[length];
        for (int i = 0; i < length; i++) {
            JSONObject jsonHour = data.getJSONObject(i);

            hourlyData[i] = new Hourly();
            hourlyData[i].setSummary(jsonHour.getString("summary"));
            hourlyData[i].setTemperature(jsonHour.getDouble("temperature"));
            hourlyData[i].setTime(jsonHour.getLong("time"));
            hourlyData[i].setIcon(jsonHour.getString("icon"));
            hourlyData[i].setTimezone(timeZone);
        }
        return hourlyData;
    }

    private Current getCurrentDetails(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timeZone = forecast.getString("timezone");
        Log.i(TAG, "From JSON:" + timeZone);

        JSONObject weather = forecast.getJSONObject("currently");
        Current current = new Current();
        current.setHumidity(weather.getDouble("humidity"));
        current.setSummary(weather.getString("summary"));
        current.setTime(weather.getLong("time"));
        current.setIcon(weather.getString("icon"));
        current.setTemperature(weather.getDouble("temperature"));
        current.setPercipChance(weather.getDouble("precipProbability"));
        current.setTimeZone(timeZone);

        Log.d(TAG, current.getFormattedTime());

        return current;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if(manager != null)
            networkInfo = manager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void alertUserAboutError(String error) {
        if (error.equals(ERROR_DATA)) {
            DataAlertDialogFragment dialog = new DataAlertDialogFragment();
            dialog.show(getSupportFragmentManager(), "error_dialog");
        } else if (error.equals(ERROR_NETWORK)) {
            NetworkAlertDialogFragment dialog = new NetworkAlertDialogFragment();
            dialog.show(getSupportFragmentManager(), "error_dialog");
        }
    }
}
