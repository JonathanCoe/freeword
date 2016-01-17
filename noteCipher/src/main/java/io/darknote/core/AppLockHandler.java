package io.darknote.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import info.guardianproject.cacheword.CacheWordHandler;
import io.darknote.activities.LockScreenActivity;
import io.darknote.database.DatabaseContentProvider;

/**
 * Handles the process of locking app.
 */
@SuppressLint("InlinedApi")
public class AppLockHandler
{
	/**
	 * Does all the work necessary to securely lock the application.
	 * 
	 * @param cacheWordHandler - An instance of CacheWordHandler to be
	 * provided by the caller of this method
	 */
	public static void runLockRoutine(CacheWordHandler cacheWordHandler)
	{
    	cacheWordHandler.lock();
        cacheWordHandler.disconnectFromService();
    	DatabaseContentProvider.closeDatabase();
    	
    	// Open the lock screen activity
		Context appContext = App.getContext();
        Intent intent = new Intent(appContext, LockScreenActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) { // FLAG_ACTIVITY_CLEAR_TASK only exists in API 11 and later
        	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear the stack of activities
        }
        else {
        	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        appContext.startActivity(intent);
	}
}