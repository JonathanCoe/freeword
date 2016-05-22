package io.freeword.core;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import info.guardianproject.cacheword.CacheWordHandler;
import io.freeword.activities.LockScreenActivity;
import io.freeword.database.DatabaseContentProvider;

public class AppLockHandler {

	public static void lockApp(CacheWordHandler cacheWordHandler) {
        lockCacheword(cacheWordHandler);
    	DatabaseContentProvider.closeDatabase();
        openLockScreen();
	}

    private static void lockCacheword(CacheWordHandler cacheWordHandler) {
        cacheWordHandler.lock();
        cacheWordHandler.disconnectFromService();
    }

    private static void openLockScreen() {
        Context appContext = App.getContext();
        Intent intent = new Intent(appContext, LockScreenActivity.class);
        setIntentFlags(intent);
        appContext.startActivity(intent);
    }

    private static void setIntentFlags(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) { // FLAG_ACTIVITY_CLEAR_TASK only exists in API 11 and later
        	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear the stack of activities
        }
        else {
        	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
    }
}
