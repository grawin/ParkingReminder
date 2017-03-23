package com.grawin.parkingreminder;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Ryan on 2/29/2016.
 */
public class DataStore {

    /** Static ID for the notification used while timer service is active. */
    public static final int NOTIFICATION_ID = 1;

    // Saved preferences

    private static SharedPreferences sharedPrefs;

    // Option menu saved preferences

    private static String prefStrNotifSound;

    private static String prefStrNotifVibrate;

    private static String prefStrNotifCoundown;

    private static String prefStrTrackPos;

    // Stored data

    private static int timerSec;

    // Utilities

    /**
     * Returns a time formatted string (MM:SS) based on provided seconds value.
     * @param seconds Number of seconds.
     * @return Formatted time string (MM:SS).
     */
    public static String formatTimeString(int seconds) {
        return String.format("%02d", seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    // Implementation

    /**
     * Initialize any data that depends on application context.
     * @param context The main application context.
     */
    public static void initialize(Context context) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefStrNotifSound = context.getString(R.string.pref_key_notif_sound);
        prefStrNotifVibrate = context.getString(R.string.pref_key_notif_vibrate);
        prefStrNotifCoundown = context.getString(R.string.pref_key_notif_countdown);
        prefStrTrackPos = context.getString(R.string.pref_key_track_pos);
    }

    // Shared Preferences Methods

    public static boolean isSoundNotifEnabled() {
        return sharedPrefs.getBoolean(prefStrNotifSound, true);
    }

    public static boolean isVibrateNotifEnabled() {
        return sharedPrefs.getBoolean(prefStrNotifVibrate, false);
    }

    public static int getNotifCountdownSec() {
        return sharedPrefs.getInt(prefStrNotifCoundown, 3);
    }

    public static boolean isTrackPosEnabled() {
        return sharedPrefs.getBoolean(prefStrTrackPos, true);
    }

    public static int getTimerSec() {
        return timerSec;
    }

    public static void setTimerSec(int timerSec) {
        DataStore.timerSec = timerSec;
    }
}
