package io.darknote;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;

import info.guardianproject.cacheword.CacheWordSettings;

public class App extends Application {

    private CacheWordSettings mCWSettings;

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply the Google PRNG fixes to properly seed SecureRandom
        PRNGFixes.apply();

        mCWSettings = new CacheWordSettings(getApplicationContext());
        mCWSettings.setNotificationIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(this, NoteCipher.class), Intent.FLAG_ACTIVITY_NEW_TASK ));
    }

    public CacheWordSettings getCWSettings() {
        return mCWSettings;
    }


}
