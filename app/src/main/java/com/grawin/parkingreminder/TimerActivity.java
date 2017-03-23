package com.grawin.parkingreminder;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class TimerActivity extends AppCompatActivity {

    private TextView mHoursText = null;
    private TextView mMinutesText = null;

    private String mTimerString = new String();

    private static final int TEXT_LENGTH = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        mHoursText = (TextView) findViewById(R.id.hours_text);
        mMinutesText = (TextView) findViewById(R.id.minutes_text);
    }

    private void updateDisplay() {
        // Build the display string.
        // Fill with 0's if length less than max length.
        String displayString = ("0000" + mTimerString).substring(mTimerString.length());

        mMinutesText.setText(displayString.substring(2, 4));
        mHoursText.setText(displayString.substring(0, 2));
    }

    public void handleKeypad(View view) {
        if (mTimerString.length() >= TEXT_LENGTH) {
            return;
        }

        TextView tv = (TextView) view;
        String number = tv.getText().toString();
        mTimerString += number;
        updateDisplay();
    }

    public void handleDelete(View view) {
        if (mTimerString.length() > 0) {
            mTimerString = mTimerString.substring(0, mTimerString.length() - 1);
            updateDisplay();
        }
    }

    public void handleStartTimer(View view) {
        String minutes = mMinutesText.getText().toString();
        int mins = Integer.valueOf(minutes);

        String hours = mHoursText.getText().toString();
        mins += Integer.valueOf(hours) * 60;

        // Log.i("Timer", "" + mins);
        if (mins > 0) {
            // Start the timer service.
            Intent intent = new Intent(this, TimerService.class);
            intent.putExtra("minutes", mins);
            startService(intent);

            finish();
        }

    }

}
