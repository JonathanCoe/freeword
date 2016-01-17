package io.darknote.core;

import android.app.Application;
import android.content.Context;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

public class App extends Application implements ICacheWordSubscriber {

    private static Context context;
    private CacheWordHandler cacheWordHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();

        // Apply the Google PRNG fixes to properly seed SecureRandom
        PRNGFixes.apply();

        // Start and subscribe to the CacheWordService
        cacheWordHandler = new CacheWordHandler(getApplicationContext(), this);
        cacheWordHandler.connectToService();
    }

    /**
     * Returns the application context. <br><br>
     *
     * <b>NOTE!!!</b> There is no guarantee that the normal, non-static onCreate() will have been called before
     * this method is called. This means that this method can sometimes return null, particularly if called when the
     * app has been running for a short time, e.g. during unit testing.
     *
     * @return application context
     */
    public static Context getContext() {
        return context;
    }

    @Override
    public void onCacheWordLocked() {
        // Currently nothing to do here
    }

    @Override
    public void onCacheWordOpened() {
        // Currently nothing to do here
    }

    @Override
    public void onCacheWordUninitialized() {
        // Currently nothing to do here
    }
}