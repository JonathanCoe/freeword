package io.freeword.activities;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.widget.*;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import io.freeword.R;
import io.freeword.core.AppLockHandler;
import io.freeword.data.Note;
import io.freeword.database.NoteProvider;

import java.util.ArrayList;
import java.util.Collections;

public class NotesListActivity extends ListActivity implements ICacheWordSubscriber {

    private NoteProvider noteProvider;
    private ArrayList<Note> notes;
    private ListView notesListView;

    private CacheWordHandler cacheWordHandler;

    private static final String TAG = NotesListActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupCacheWord();

        setSecureLayoutParms();

        setupLayout();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cacheWordHandler.disconnectFromService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cacheWordHandler.connectToService();
        updateListView();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        if (cacheWordHandler != null) {
            cacheWordHandler.disconnectFromService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCacheWordUninitialized() {
        Log.d(TAG, "onCacheWordUninitialized");
        AppLockHandler.lockApp(cacheWordHandler);
        finish();
    }

    @Override
    public void onCacheWordLocked() {
        Log.d(TAG, "onCacheWordLocked");
        AppLockHandler.lockApp(cacheWordHandler);
        finish();
    }

    @Override
    public void onCacheWordOpened() {
        Log.d(TAG, "onCacheWordOpened");
        setCacheWordTimeout();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.notes_list_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.notes_list_activity_menu_item_new:
                Intent intent = new Intent(getApplicationContext(), NoteActivity.class);
                intent.putExtra(NoteActivity.EXTRA_CREATE_NEW_NOTE, true);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;

            case R.id.notes_list_activity_menu_item_lock:
                if (!cacheWordHandler.isLocked()) {
                    cacheWordHandler.lock();
                }
                return true;

            case R.id.notes_list_activity_menu_item_backups:
                Intent intent2 = new Intent(this, BackupsActivity.class);
                intent2.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent2);
                return true;

            case R.id.notes_list_activity_menu_item_settings:
                Intent intent3 = new Intent(this, SettingsActivity.class);
                intent3.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent3);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupCacheWord() {
        cacheWordHandler = new CacheWordHandler(this);
        cacheWordHandler.connectToService();
    }

    private void setSecureLayoutParms() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    private void setupLayout() {
        setContentView(R.layout.notes_list);

        createNotesList();
    }

    private void setCacheWordTimeout() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int timeout = preferences.getInt(SettingsActivity.KEY_CACHEWORD_TIMEOUT, SettingsActivity.DEFAULT_CACHEWORD_TIMEOUT);
        cacheWordHandler.setTimeout(timeout);
    }

    private void createNotesList() {
        loadNotes();
        Collections.sort(notes);
        setUpListView();
    }

    private void loadNotes() {
        noteProvider = NoteProvider.get(getApplicationContext());
        notes = noteProvider.getAllNotes();
    }

    private void setUpListView() {
        NoteAdapter adapter = new NoteAdapter(notes);
        notesListView = new ListView(this);
        notesListView = (ListView) findViewById(android.R.id.list);
        setListAdapter(adapter);
    }

    private void updateListView() {

        notes = noteProvider.getAllNotes();

        // Save ListView state so that we can resume at the same scroll position
        Parcelable state = notesListView.onSaveInstanceState();

        // Re-instantiate the ListView and re-populate it
        notesListView = new ListView(this);
        notesListView = (ListView)findViewById(android.R.id.list);
        notesListView.setAdapter(new NoteAdapter(notes));

        // Restore previous state (including selected item index and scroll position)
        notesListView.onRestoreInstanceState(state);
    }

    private class NoteAdapter extends ArrayAdapter<Note>
    {
        public NoteAdapter(ArrayList<Note> notes) {
            super(getBaseContext(), android.R.layout.simple_list_item_1, notes);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // If we weren't given a view, inflate one
            if (null == convertView) {
                convertView = getLayoutInflater().inflate(R.layout.notes_row, parent, false);
            }

            // Configure the view for this Note
            final Note note = getItem(position);

            TextView noteTitleTextView = (TextView)convertView.findViewById(R.id.row_text);

            if (note.getTitle() == null) {
                noteTitleTextView.setText("[No title]");
            }

            else {
                noteTitleTextView.setText(note.getTitle());
            }

            final int selectedPosition = position;

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Note list item clicked");

                    final Note selectedNote = ((NoteAdapter)notesListView.getAdapter()).getItem(selectedPosition);
                    long noteId = selectedNote.getId();

                    Intent intent = new Intent(getApplicationContext(), NoteActivity.class);
                    intent.putExtra(NoteActivity.EXTRA_NOTE_ID, noteId);
                    startActivity(intent);
                }
            });

            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Log.d(TAG, "Note list item long clicked");

                    final Dialog longPressDialog = new Dialog(NotesListActivity.this);
                    LinearLayout dialogLayout = (LinearLayout) View.inflate(NotesListActivity.this,
                            R.layout.delete_note_dialog, null);
                    longPressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    longPressDialog.setContentView(dialogLayout);

                    WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                    lp.copyFrom(longPressDialog.getWindow().getAttributes());
                    lp.width = WindowManager.LayoutParams.MATCH_PARENT;

                    longPressDialog.show();
                    longPressDialog.getWindow().setAttributes(lp);

                    final Note selectedNote = ((NoteAdapter)notesListView.getAdapter()).getItem(selectedPosition);
                    TextView noteTitleTextView = (TextView) dialogLayout.findViewById(R.id.delete_note_dialog_delete_note_title_textview);
                    noteTitleTextView.setText(selectedNote.getTitle());

                    Button confirmDeleteButton = (Button) dialogLayout.findViewById(R.id.delete_note_dialog_confirm_button);
                    confirmDeleteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d(TAG, "Note list delete dialog confirm button clicked");
                            noteProvider.deleteNote(selectedNote);
                            updateListView();
                            longPressDialog.dismiss();
                        }
                    });

                    Button cancelDeleteButton = (Button) dialogLayout.findViewById(R.id.delete_note_dialog_cancel_button);
                    cancelDeleteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d(TAG, "Note list delete dialog cancel button clicked");
                            longPressDialog.dismiss();
                        }
                    });

                    return true;
                }
            });

            return convertView;
        }
    }
}