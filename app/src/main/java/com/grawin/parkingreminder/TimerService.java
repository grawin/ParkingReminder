package com.grawin.parkingreminder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

/**
 * Created by Ryan on 2/26/2016.
 */
public class TimerService extends Service {
    /** Used to identify the source of a log message. */
    private final static String TAG = TimerService.class.getName();

    /** CountDownTimer is inaccurate so use a tick rate less than a second. */
    private static final long TICK_RATE_MS = 100;

    public static final String TIMER_BR = "com.grawin.parkingreminder.timer_br";
    Intent bi = new Intent(TIMER_BR);

    /** Underlying timer implementation. */
    CountDownTimer mTimer = null;

    /** The remaining time in seconds. */
    private long mRemaining_sec;
    //private long mElapsed_sec;
    /** The duration of the timer in seconds. */
    private long mDuration_sec;

    private NotificationManager mNotifier;
    private NotificationCompat.Builder mNotifyBuilder;

    /** Media player for countdown sound notification. */
    MediaPlayer mMediaPlayer = new MediaPlayer();

    private void playSound(Context context, Uri alert) {
        //mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(context, alert);
            final AudioManager audioManager = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            }
        } catch (IOException e) {
            // TODO - do nothing for now, as if the user has NOT selected a sound... we will hit this case
            /*
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Failed to load timer sound, select different sound in settings. If problem persists, try re-installing.")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            */
            // mMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.beep_1khz);
            // mMediaPlayer.start();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mTimer.cancel();

        bi.putExtra("end", true);
        sendBroadcast(bi);

        mMediaPlayer.release();
        // Log.i(TAG, "Timer cancelled");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        mDuration_sec = intent.getIntExtra("minutes", 0) * 60;

        // If duration is 0 then bail.
        if (mDuration_sec == 0) {
            return START_NOT_STICKY;
        }

        mRemaining_sec = mDuration_sec;

        // mMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.beep_1khz);

        setupNotification();

        // Log.i(TAG, "Starting timer...");

        createTimer(mDuration_sec * 1000);
        mTimer.start();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private void setupNotification() {
        Context context = getApplicationContext();

        Intent notificationIntent = new Intent(context, MapsActivity.class);

        // Flags needed to pass Intent with notification.
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // FLAG_UPDATE_CURRENT needed to pass Intent with notification and call onNewIntent.
        PendingIntent intent = PendingIntent.getActivity(context, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mNotifyBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(getString(R.string.app_name))
                // .setContentText("Parking time remaining: ")
                .setSmallIcon(R.drawable.ic_local_parking_blue_24dp)
                .setContentIntent(intent);

        Notification notification = mNotifyBuilder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR
                | Notification.FLAG_ONGOING_EVENT;

        mNotifier = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifier.notify(DataStore.NOTIFICATION_ID, notification);
    }

    private void createTimer(long duration_ms) {
        mTimer = new CountDownTimer(duration_ms, TICK_RATE_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Determine if this is a new second.
                final int newRemaining_sec = Math.round((float) millisUntilFinished / 1000.0f);
                if (newRemaining_sec != mRemaining_sec) {
                    // Update state with new time.
                    DataStore.setTimerSec(newRemaining_sec);
                    mRemaining_sec = newRemaining_sec;
                    //mElapsed_sec = mDuration_sec - mRemaining_sec;

                    // Log.i(TAG, "Countdown seconds remaining: " + mRemaining_sec);
                    
                    String notifText = "Time remaining: " +
                            DataStore.formatTimeString(newRemaining_sec);

                    mNotifyBuilder.setContentText(notifText);

                    Notification notification = mNotifyBuilder.build();
                    notification.flags |= Notification.FLAG_NO_CLEAR
                            | Notification.FLAG_ONGOING_EVENT;

                    mNotifier.notify(DataStore.NOTIFICATION_ID, notification);

                    // Handle notifications

                    final boolean sound = DataStore.isSoundNotifEnabled();
                    final boolean vibrate = DataStore.isVibrateNotifEnabled();
                    if (sound || vibrate) {
                        final int notifCount = DataStore.getNotifCountdownSec();
                        if (mRemaining_sec > 0 && mRemaining_sec <= notifCount) {
                            //Log.i(TAG, "s " + sound + " v " + vibrate + " n " + notifCount);
                            if (sound && !mMediaPlayer.isPlaying()) {
                                SharedPreferences getAlarms = PreferenceManager.
                                        getDefaultSharedPreferences(getBaseContext());
                                String alarms = getAlarms.getString("ringtone", "default ringtone");
                                Uri uri = Uri.parse(alarms);
                                playSound(getBaseContext(), uri);
                            }
                            if (vibrate) {
                                ((Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate(500);
                            }
                        }
                    }

                    // Inform subscribers of tick event now that data store is updated.
                    bi.putExtra("end", false);
                    sendBroadcast(bi);
                }
            }

            @Override
            public void onFinish() {

                mMediaPlayer.stop();

                String currentDateTimeString =
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date());
                mNotifyBuilder.setContentText(getString(R.string.str_complete) + " " +
                        currentDateTimeString);
                Notification notification = mNotifyBuilder.build();
                notification.flags |= Notification.FLAG_AUTO_CANCEL;

                mNotifier.notify(DataStore.NOTIFICATION_ID, notification);

                // Log.i(TAG, "Timer finished");
                bi.putExtra("end", true);
                sendBroadcast(bi);
            }
        };
    }
}
