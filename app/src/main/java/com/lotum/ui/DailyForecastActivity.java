package com.lotum.ui;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.lotum.R;
import com.lotum.adapters.DailyAdapter;
import com.lotum.weather.Daily;

import java.util.Arrays;

public class DailyForecastActivity extends ListActivity {

    private Daily[] mDays;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_forecast);

        Intent intent = getIntent();
        Parcelable[] parcelables = intent.getParcelableArrayExtra(MainActivity.DAILY_FORECAST);
        mDays = Arrays.copyOf(parcelables, parcelables.length, Daily[].class);

        DailyAdapter adapter = new DailyAdapter(this, mDays);
        setListAdapter(adapter);

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        String day = mDays[position].getDayOfTheWeek();
        String condition = mDays[position].getSummary();
        String highTemp = mDays[position].getTemperatureMax() + "";
        String message = String.format("On %s the high will be %s and it will be %s", day, highTemp, condition);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
