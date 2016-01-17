package io.darknote.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import io.darknote.R;
import io.darknote.core.AppLockHandler;
import io.darknote.data.Note;
import io.darknote.database.NoteProvider;
import io.darknote.util.LinedEditText;
import io.darknote.util.NoteUtils;

public class NoteActivity extends FragmentActivity implements ICacheWordSubscriber {

    static final String EXTRA_CREATE_NEW_NOTE = "extraCreateNewNote";
    static final String EXTRA_NOTE_ID = "extraNoteId";

    private EditText titleTextView;
    private LinedEditText bodyEditText;

    private CacheWordHandler cacheWordHandler;

    private NoteProvider noteProvider;
    private Note note;

    private static final int DEFAULT_TITLE_LENGTH = 8;

    private static final String KEY_SAVED_POSITION = "keySavedPosition";
    private long mRowId = -1;
    private float mTextSize = 0;

    private static final int SAVE_ID = Menu.FIRST;
    private static final int SHARE_ID = Menu.FIRST + 1;
    private static final int BIGGER_ID = Menu.FIRST + 2;
    private static final int SMALLER_ID = Menu.FIRST + 3;

    private final static String TEXT_SIZE = "text_size";
    private final static String PREFS_NAME = "NoteEditPrefs";

    private static final String PLACEHOLDER_TEXT_FOR_DATABASE = "*********";

    private boolean noteExistsInDatabase;

    private static final String TAG = NoteActivity.class.getSimpleName();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cacheWordHandler = new CacheWordHandler(this);
        cacheWordHandler.connectToService();

        noteProvider = NoteProvider.get(getApplicationContext());

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }
        
        setContentView(R.layout.note_edit);

        titleTextView = (EditText) findViewById(R.id.title);
        bodyEditText = (LinedEditText) findViewById(R.id.body);

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Set up the data for this activity
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_CREATE_NEW_NOTE)) {
            note = new Note();
            noteExistsInDatabase = false;
        }
        else if (intent.hasExtra(EXTRA_NOTE_ID)) {
            long id = intent.getLongExtra(EXTRA_NOTE_ID, 1);
            Log.d(TAG, "Got ID " + id + " from intent extra");

            note = noteProvider.getNote(id);
            Log.d(TAG, "Retrieved the following Note from the database: " + note.toString());

            if(note.getTitle().equals(PLACEHOLDER_TEXT_FOR_DATABASE)) {
                note.setTitle("");
            }
            if (note.getBody().equals(PLACEHOLDER_TEXT_FOR_DATABASE)) {
                note.setBody("");
            }

            titleTextView.setText(note.getTitle());
            bodyEditText.setText(note.getBody());

            noteExistsInDatabase = true;
        }

        if (savedInstanceState != null) {
            mRowId = savedInstanceState.getLong(KEY_SAVED_POSITION);
            mTextSize = savedInstanceState.getFloat(TEXT_SIZE, 0);
        }

        if (mTextSize == 0) {
            mTextSize = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getFloat(TEXT_SIZE, 0);
        }

        if (mTextSize != 0) {
            bodyEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, SAVE_ID, 0, R.string.menu_save)
	        .setIcon(R.drawable.save)
	    	.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, SHARE_ID, 0, R.string.menu_share)
	        .setIcon(R.drawable.share)
	    	.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, SMALLER_ID, 0, R.string.menu_smaller)
            .setIcon(R.drawable.smaller)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, BIGGER_ID, 0, R.string.menu_bigger)
            .setIcon(R.drawable.bigger)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
	        case android.R.id.home:
	        	NavUtils.navigateUpTo(this, new Intent(this, NotesListActivity.class));
	            return true;
            case SAVE_ID:
                saveNote();
                Toast.makeText(NoteActivity.this, R.string.note_saved, Toast.LENGTH_SHORT).show();
                return true;
            case SHARE_ID:
                shareEntry();
                return true;
            case BIGGER_ID:
                changeTextSize(1.1f);
                return true;
            case SMALLER_ID:
                changeTextSize(.9f);
                return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onCacheWordUninitialized() {
        Log.d(TAG, "onCacheWordUninitialized");
        AppLockHandler.runLockRoutine(cacheWordHandler);
        finish();
    }

    @Override
    public void onCacheWordLocked() {
        Log.d(TAG, "onCacheWordLocked");
        AppLockHandler.runLockRoutine(cacheWordHandler);
        finish();
    }

    @Override
    public void onCacheWordOpened() {
        Log.d(TAG, "onCacheWordOpened");
        // Nothing to do here currently
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        saveNoteIfNotEmpty();

        if (mRowId != -1) {
            outState.putLong(KEY_SAVED_POSITION, mRowId);
        }
        if (mTextSize != 0) {
            outState.putFloat(TEXT_SIZE, mTextSize);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (!cacheWordHandler.isLocked()) {
            saveNoteIfNotEmpty();
        }
        cacheWordHandler.disconnectFromService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cacheWordHandler.connectToService();
    }

    private void changeTextSize(float factor) {
        mTextSize = bodyEditText.getTextSize();
        mTextSize *= factor;

        bodyEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putFloat(TEXT_SIZE, mTextSize)
            .commit();
    }

    private void saveNote() {
        Log.d(TAG, "saveNote() called");

        String title = titleTextView.getText().toString();
        String body = bodyEditText.getText().toString();

        if (title.isEmpty() && body.isEmpty()) {
            return;
        }

        if(title.isEmpty()) {
            if (!body.isEmpty()) {
                note.setTitle(body.substring(0, DEFAULT_TITLE_LENGTH));
            }
            else {
                note.setTitle(PLACEHOLDER_TEXT_FOR_DATABASE);
            }
        }
        if (body.isEmpty()) {
            note.setBody(PLACEHOLDER_TEXT_FOR_DATABASE);
        }

        note.setTitle(title);
        note.setBody(body);

        if (noteExistsInDatabase == false) {
            Log.d(TAG, "Adding the following note: " + note.toString());
            noteProvider.addNote(note);
        }
        else {
            Log.d(TAG, "Updating the following note: " + note.toString());
            noteProvider.updateNote(note);
        }

        noteExistsInDatabase = true;
    }

    private void saveNoteIfNotEmpty() {

        Log.d(TAG, "saveNoteIfNotEmpty() called");

        String title = titleTextView.getText().toString();
        String body = bodyEditText.getText().toString();

        if (title.isEmpty() && body.isEmpty()) {
            return;
        }
        else {
            saveNote();
        }
    }

    private void shareEntry() {
        String body = bodyEditText.getText().toString();
        NoteUtils.shareText(this, body);
    }
}