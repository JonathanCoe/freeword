package io.darknote.database;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.cacheword.PassphraseSecrets;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteQueryBuilder;

import java.util.Arrays;
import java.util.HashSet;

public class DatabaseContentProvider extends ContentProvider implements ICacheWordSubscriber
{
	private static DatabaseHelper sDatabaseHelper;
	private static Context sContext;
	private static CacheWordHandler sCacheWordHandler;
	private static SQLiteDatabase sDatabase;
    
	// Used by the URI Matcher
	private static final int NOTE = 10;
	private static final int NOTE_ID = 20;
	  
    private static final String AUTHORITY = "io.darknote.database";
	  
    // The path strings for each table in the database
    private static final String PATH_NOTE = "note";
	  
    // The URIs for each table in the database
    public static final Uri CONTENT_URI_NOTE = Uri.parse("content://" + AUTHORITY + "/" + PATH_NOTE);
	  
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    
    private static final String TAG = DatabaseContentProvider.class.getSimpleName();
            	  
    static 
    {
    	sURIMatcher.addURI(AUTHORITY, PATH_NOTE, NOTE);
    	sURIMatcher.addURI(AUTHORITY, PATH_NOTE + "/#", NOTE_ID);
    }

    @SuppressLint("InlinedApi")
	@Override
    public boolean onCreate() 
    {    	
    	Log.i(TAG, "Database content provider onCreate() called");
    	
    	sContext = getContext();
    	
    	sCacheWordHandler = new CacheWordHandler(sContext, this);
    	sCacheWordHandler.connectToService();
    	
    	sDatabaseHelper = new DatabaseHelper(sContext, sCacheWordHandler);
		
		return false;
    }
    
    /**
     * Gets a writable SQLiteDatabase object
     * 
     * @return The SQLiteDatabase object
     */
    public static SQLiteDatabase openDatabase()
    {
    	Log.i(TAG, "DatabaseContentProvider.openDatabase() called");
    	
    	try
    	{
    		SQLiteDatabase.loadLibs(sContext);
    		sDatabase = sDatabaseHelper.getWritableDatabase();
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception occurred while running DatabaseContentProvider.openDatabase()." +
                    " The exception message was:\n" + e.getMessage());
    	}
    	
		return sDatabase;
    }
    
    /**
     * Changes the database passphrase
     * 
     * @param newPassphrase - The new passphrase
     * 
     * @return A boolean indicating whether or not the database passphrase was
     * changed successfully
     */
    public static boolean changeDatabasePassphrase(String newPassphrase)
    {
    	Log.i(TAG, "DatabaseContentProvider.changeDatabasePassphrase() called");
    	
    	try
    	{
	    	// Get the old encryption key
    		String oldEncryptionKey = DatabaseHelper.encodeRawKeyToStr(sCacheWordHandler.getEncryptionKey());
    		
    		// Set CacheWord to use the new passphrase
			sCacheWordHandler.changePassphrase((PassphraseSecrets) sCacheWordHandler.getCachedSecrets(),
                                                newPassphrase.toCharArray());
			
			// Get the new encryption key
			String newEncryptionKey = DatabaseHelper.encodeRawKeyToStr(sCacheWordHandler.getEncryptionKey());
	    	
	    	sDatabase.execSQL("PRAGMA key = \"" + oldEncryptionKey + "\";");
	    	sDatabase.execSQL("PRAGMA rekey = \"" + newEncryptionKey + "\";");
	    	
	    	openDatabase();
	    	return true;
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception occurred while running DatabaseContentProvider.changeDatabasePassphrase(). " +
                    "The exception message was:\n" + e.getMessage());
    		return false;
    	}
    }
    
	/**
	 * Closes the SQLiteDatabase object that we use to interact with the
	 * app's database. This method is intended to be used when the user locks
	 * the app. 
	 */
	public static void closeDatabase()
	{
		Log.i(TAG, "DatabaseContentProvider.closeDatabase() called.");
		if (sDatabase != null)
		{
			Log.d(TAG, "About to close database");
			sDatabase.close();
			sDatabase = null;
			System.gc();
		}
	}
    
    @Override
    public String getType(Uri uri)
    {
    	return null; // This method will not be called unless the application changes to specifically
                     // invoke it. Thus it can be safely left to return null.
    }
    
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
    	// Using SQLiteQueryBuilder instead of query() method
	    SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
	    
	    int uriType = sURIMatcher.match(uri);

	    // Check if the caller has requested a column which does not exists
	    checkColumns(projection, uriType);
    
	    switch (uriType)
	    {
	        case NOTE_ID:
	            // Adding the ID to the original query
	            queryBuilder.appendWhere(NoteTable.COLUMN_ID + "=" + uri.getLastPathSegment());
	        case NOTE:
	            queryBuilder.setTables(NoteTable.TABLE_NAME);
	            break;
	      
		    default:
		    	throw new IllegalArgumentException("Unknown URI: " + uri +
                        " Exception occurred in DatabaseContentProvider.query()");
	    }
	    
	    Cursor cursor = queryBuilder.query(sDatabase, projection, selection, selectionArgs, null, null, sortOrder);
	    // Make sure that potential listeners are getting notified
	    cursor.setNotificationUri(sContext.getContentResolver(), uri);
	    return cursor;
	  }

	  @Override
	  public Uri insert(Uri uri, ContentValues values)
	  {
		int uriType = sURIMatcher.match(uri);
	    long id = 0;
	    String path;
	    
	    switch (uriType) 
	    {
		    case NOTE:
			      id = sDatabase.insert(NoteTable.TABLE_NAME, null, values);
			      path = PATH_NOTE;
			      break;
		      
		    default:
		    	  throw new IllegalArgumentException("Unknown URI: " + uri +
                          " Exception occurred in DatabaseContentProvider.insert()");
	    }
	    
	    sContext.getContentResolver().notifyChange(uri, null);
	    return Uri.parse(path + "/" + id);
	  }

	  @Override
	  public int delete(Uri uri, String selection, String[] selectionArgs)
	  {
		int uriType = sURIMatcher.match(uri);
	    int rowsDeleted = 0;
	    String id;
	    
	    switch (uriType)
	    {
		    case NOTE:
			      rowsDeleted = sDatabase.delete(NoteTable.TABLE_NAME, selection, selectionArgs);
			      break;      
		    case NOTE_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection))
			      {
			    	  rowsDeleted = sDatabase.delete(NoteTable.TABLE_NAME, NoteTable.COLUMN_ID + "=" + id, null);
			      }
			      else 
			      {
			    	  rowsDeleted = sDatabase.delete(NoteTable.TABLE_NAME,
                                                     NoteTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
			      }
			      break;
			      
		    default:
		    	  throw new IllegalArgumentException("Unknown URI: " + uri +
                          " Exception occurred in DatabaseContentProvider.delete()");
	    }
	    sContext.getContentResolver().notifyChange(uri, null);
	    return rowsDeleted;
	  }

	  @Override
	  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
	  {
		int uriType = sURIMatcher.match(uri);
	    int rowsUpdated = 0;
	    String id;
	    
	    switch (uriType)
	    {
		    case NOTE:
			      rowsUpdated = sDatabase.update(NoteTable.TABLE_NAME, values, selection, selectionArgs);
			      break;
		    case NOTE_ID:
			      id = uri.getLastPathSegment();
			      if (TextUtils.isEmpty(selection))
			      {
			    	  rowsUpdated = sDatabase.update(NoteTable.TABLE_NAME, values,
                                                     NoteTable.COLUMN_ID + "=" + id, null);
			      } 
			      else 
			      {
			    	  rowsUpdated = sDatabase.update(NoteTable.TABLE_NAME, values,
                                                     NoteTable.COLUMN_ID + "=" + id  + " and " + selection, selectionArgs);
			      }
			      break;
		      
		    default:
		    	  throw new IllegalArgumentException("Unknown URI: " + uri +
                          " Exception occurred in DatabaseContentProvider.update()");
	    }
	    sContext.getContentResolver().notifyChange(uri, null);
	    return rowsUpdated;
	  }
	  
	  private void checkColumns(String[] projection, int uriType)
	  {
		    String[] available = getAvailable(uriType);

		    if (projection != null)
		    {
			    HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
			    HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
			    // check if all columns which are requested are available
			    if (!availableColumns.containsAll(requestedColumns)) 
			    {
				    throw new IllegalArgumentException("Unknown columns in projection. " +
                            "Exception occurred in DatabaseContentProvider.checkColumns()");
			    }
		   }
	  }
	  
	  private String[] getAvailable(int uriType)
	  {
		    if (uriType == NOTE || uriType == NOTE_ID) {
		    	String[] available = {
						NoteTable.COLUMN_ID,
						NoteTable.COLUMN_TITLE,
						NoteTable.COLUMN_BODY};
		    	return available;
		    }
	
		    else {
		    	 throw new IllegalArgumentException("Unknown URI Type: " + uriType + " " +
						 "Exception occurred in DatabaseContentProvider.getAvailable()");
		    }
	  }
	
	@SuppressLint("InlinedApi")
	@Override
	public void onCacheWordLocked()
	{
		Log.d(TAG, "DatabaseContentProvider.onCacheWordLocked() called.");
	}

	@Override
	public void onCacheWordOpened()
	{
		Log.d(TAG, "DatabaseContentProvider.onCacheWordOpened() called.");
		openDatabase();
	}

	@Override
	public void onCacheWordUninitialized()
	{
		Log.d(TAG, "DatabaseContentProvider.onCacheWordUninitialized() called.");
	   // Nothing to do here
	}
}