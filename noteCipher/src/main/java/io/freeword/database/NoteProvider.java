package io.freeword.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import io.freeword.data.Note;

import java.util.ArrayList;

/**
 * A singleton class which controls the creation, reading, updating, and
 * deletion of stored Note objects.
 */
public class NoteProvider
{
    private static final String TAG = NoteProvider.class.getSimpleName();

    private static NoteProvider sNoteProvider;

    private Context mAppContext;
    private static ContentResolver mContentResolver;

    private NoteProvider(Context appContext)
    {
        mAppContext = appContext;
        mContentResolver = mAppContext.getContentResolver();
    }
    
    /**
     * Returns an instance of this singleton class. 
     * 
     * @param c - A Context object for the currently running application
     */
    public static NoteProvider get(Context c)
    {
        if (sNoteProvider == null)
        {
        	Context appContext = c.getApplicationContext();
        	sNoteProvider = new NoteProvider(appContext);
        }
        
        return sNoteProvider;
    }
    
    /**
     * Takes an Note object and adds it to the app's
     * SQLite database as a new record, returning the ID of the 
     * newly created record. 
     * 
     * @param note - The Note object to be added
     * 
     * @return id - A long value representing the ID of the newly
     * created record
     */
    public long addNote(Note note)
    {
    	ContentValues values = new ContentValues();
    	values.put(NoteTable.COLUMN_TITLE, note.getTitle());
    	values.put(NoteTable.COLUMN_BODY, note.getBody());

        // Insert the new record
		Uri insertionUri = mContentResolver.insert(DatabaseContentProvider.CONTENT_URI_NOTE, values);
		
		// Parse the ID of the newly created record from the insertion Uri
		String uriString = insertionUri.toString();
		String idString = uriString.substring(uriString.indexOf("/") + 1);
		long id = Long.parseLong(idString);
        Log.d(TAG, "Note with ID " + id + " and title " + note.getTitle() + " saved to database");
		return id;
    }
    
    /**
     * Finds all Notes in the application's database that match the given field
     * 
     * @param columnName - A String specifying the name of the column in the database that 
     * should be used to find matching records. See the NoteTable class to find
     * the relevant column name. 
     * @param searchString - A String specifying the value to search for. There are 4 use cases
     * for this:<br>
     * 1) The value to search for is a String (e.g. A label from the UI). In this case the value 
     * can be passed in directly.<br>
     * 2) The value to search for is an int or long. In this case you should use String.valueOf(x)
     * and pass in the resulting String.<br>
     * 3) The value to search for is a boolean. In this case you should pass in the String "0" for 
     * false or the String "1" for true. <br>
     * 4) The value to search for is a byte[]. In this case you should encode the byte[] into a 
     * Base64 encoded String using the class android.util.Base64 and pass in the resulting String.<br><br>
     * 
     * <b>NOTE:</b> The above String conversion is very clumsy, but seems to be necessary. See 
     * https://stackoverflow.com/questions/20911760/android-how-to-query-sqlitedatabase-with-non-string-selection-args
     * 
     * @return An ArrayList containing Note objects populated with the data from
     *  the database search
     */
    public ArrayList<Note> searchNotes(String columnName, String searchString)
    {
    	ArrayList<Note> matchingRecords = new ArrayList<Note>();

    	// Specify which columns from the table we are interested in
		String[] projection = {
				NoteTable.COLUMN_ID,
				NoteTable.COLUMN_TITLE,
				NoteTable.COLUMN_BODY};
		
		// Query the database via the ContentProvider
		Cursor cursor = mContentResolver.query(
				DatabaseContentProvider.CONTENT_URI_NOTE,
				projection, 
				NoteTable.TABLE_NAME + "." + columnName + " = ? ",
				new String[]{searchString},
				null);
			
		if (cursor.moveToFirst())
    	{
    	    do 
    	    {
    	        long id = cursor.getLong(0);
    	        String title = cursor.getString(1);
    	        String body = cursor.getString(2);
    	      
    	        Note note = new Note();
    	        note.setId(id);
    	        note.setTitle(title);
    	        note.setBody(body);
    	      
    	        matchingRecords.add(note);
    	    } 
    	    while (cursor.moveToNext());
    	}	
		else
		{
			cursor.close();
			return matchingRecords;
		}
		
		cursor.close();
    	return matchingRecords;
     }
    
    /**
     * Searches the database for the Note with the given ID.
     * This method will return exactly one Note object or throw
     * a RuntimeException.
     * 
     * @param id - A long value representing the Note's ID.
     * 
     * @return The Note object with the given ID.
     */
    public Note getNote(long id)
    {
    	ArrayList<Note> retrievedRecords = searchNotes(NoteTable.COLUMN_ID, String.valueOf(id));
    	
    	if (retrievedRecords.size() != 1)
		{
			throw new RuntimeException("There should be exactly 1 record found in this search. " +
					"Instead " + retrievedRecords.size() + " records were found");
		}
		else
		{
			return retrievedRecords.get(0);
		}
    }

    /**
     * Returns an ArrayList containing all the Notes stored in the
     * application's database
     * 
     * @return An ArrayList containing one Note object for
     * each record in the Notes table.
     */
    public ArrayList<Note> getAllNotes()
    {
    	ArrayList<Note> notes = new ArrayList<Note>();
    	
        // Specify which columns from the table we are interested in
		String[] projection = {
				NoteTable.COLUMN_ID,
				NoteTable.COLUMN_TITLE,
				NoteTable.COLUMN_BODY};
		
		// Query the database via the ContentProvider
		Cursor cursor = mContentResolver.query(
				DatabaseContentProvider.CONTENT_URI_NOTE,
				projection, 
				null, 
				null, 
				null);

        if (cursor == null) {
            return new ArrayList<Note>();
        }

    	if (cursor.moveToFirst())
    	{
    	   do 
    	   {
	   	        long id = cursor.getLong(0);
	   	        String title = cursor.getString(1);
	   	        String body = cursor.getString(2);
	   	      
	   	        Note note = new Note();
	   	        note.setId(id);
	   	        note.setTitle(title);
	   	        note.setBody(body);
    	      
    	      notes.add(note);
    	   } 
    	   while (cursor.moveToNext());
    	}
    	
		cursor.close();
    	return notes;
    }
    
    /**
     * Updates the database record for a given Note object<br><br>
     * 
     * <b>NOTE:</b> This method uses the given Note's ID field to determine
     * which record in the database to update
     * 
     * @param note - The Note object to be updated
     */
    public void updateNote(Note note)
    {
    	ContentValues values = new ContentValues();
    	values.put(NoteTable.COLUMN_TITLE, note.getTitle());
    	values.put(NoteTable.COLUMN_BODY, note.getBody());
		
		long id = note.getId();
    	
		// Query the database via the ContentProvider and update the record with the matching ID
    	mContentResolver.update(DatabaseContentProvider.CONTENT_URI_NOTE,
                                values,
                                NoteTable.COLUMN_ID + " = ? ",
                                new String[]{String.valueOf(id)});
    	
    	Log.d(TAG, "Note with ID " + id + " updated");
    }

    /**
     * Deletes an Note object from the application's SQLite database<br><br>
     *
     * <b>NOTE:</b> This method uses the given Note's ID field to determine
     * which record in the database to delete
     *
     * @param note - The Note object to be deleted
     */
    public void deleteNote(Note note) {
        long id = note.getId();
        deleteNote(id);
    }

    /**
     * Deletes an Note object from the application's SQLite database<br><br>
     *
     * @param id - The ID of the Note to be deleted
     */
    public void deleteNote(long id) {
        // Query the database via the ContentProvider and delete the record with the matching ID
        int recordsDeleted = mContentResolver.delete(
                DatabaseContentProvider.CONTENT_URI_NOTE,
                NoteTable.COLUMN_ID + " = ? ",
                new String[]{String.valueOf(id)});

        Log.d(TAG, recordsDeleted + " Note(s) deleted from database");
    }
    
    /**
     * Deletes all Notes from the database
     */
    public void deleteAllNotes()
    {
		// Query the database via the ContentProvider and delete all the records
		int recordsDeleted = mContentResolver.delete(
				DatabaseContentProvider.CONTENT_URI_NOTE,
				null, 
				null);
    	
    	Log.d(TAG, recordsDeleted + " Note(s) deleted from database");
    }
}