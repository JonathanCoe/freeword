package io.freeword.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.Constants;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.cacheword.PassphraseSecrets;
import io.freeword.R;
import io.freeword.util.PassphraseUtil;
import net.simonvt.numberpicker.NumberPicker;

import java.io.IOException;

public class SettingsActivity extends SherlockPreferenceActivity implements ICacheWordSubscriber {

    static final String KEY_CACHEWORD_TIMEOUT = "cacheWordTimeout";
    static final int DEFAULT_CACHEWORD_TIMEOUT = 300;

    private CacheWordHandler cacheWordHandler;

    private static final String SHOW_LINES_IN_NOTES = "use_lines_in_notes";

    private static final String TAG = SettingsActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setupCacheWord();

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
		}

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// If in Android 3+ use a preference fragment which is the new recommended way
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PreferenceFragment() {
                    @Override
                    public void onCreate(final Bundle savedInstanceState) {
                        super.onCreate(savedInstanceState);

                        addPreferencesFromResource(R.xml.settings);

                        findPreference(Constants.SHARED_PREFS_TIMEOUT_SECONDS)
                            .setOnPreferenceClickListener(changeLockTimeoutListener);

                        findPreference(Constants.SHARED_PREFS_SECRETS)
                            .setOnPreferenceChangeListener(passphraseChangeListener);
                    }
                })
            .commit();
		}
        else {
			// Otherwise load the preferences.xml in the Activity like in previous Android versions
			addPreferencesFromResource(R.xml.settings);

			findPreference(Constants.SHARED_PREFS_TIMEOUT_SECONDS)
				.setOnPreferenceClickListener(changeLockTimeoutListener);

			findPreference(Constants.SHARED_PREFS_SECRETS)
				.setOnPreferenceChangeListener(passphraseChangeListener);
		}
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpTo(this, new Intent(this, NotesListActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
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
    }

    @Override
    public void onCacheWordUninitialized() {
        Log.d(TAG, "onCacheWordUninitialized");
        System.gc();
        showLockScreen();
    }

    @Override
    public void onCacheWordLocked() {
        Log.d(TAG, "onCacheWordLocked");
        System.gc();
        showLockScreen();
    }

    @Override
    public void onCacheWordOpened() {
        Log.d(TAG, "onCacheWordOpened");
        setCacheWordTimeout();
    }

    void showLockScreen() {
        Intent intent = new Intent(this, LockScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("originalIntent", getIntent());
        startActivity(intent);
        finish();
    }

    public static boolean getNoteLinesOption(Context context) {
        boolean defValue = context.getResources().getBoolean(R.bool.notecipher_uselines_default);
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SHOW_LINES_IN_NOTES, defValue);
    }

    private void setupCacheWord() {
        cacheWordHandler = new CacheWordHandler(this);
        cacheWordHandler.connectToService();
    }

    private void setCacheWordTimeout() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int timeout = preferences.getInt(SettingsActivity.KEY_CACHEWORD_TIMEOUT, SettingsActivity.DEFAULT_CACHEWORD_TIMEOUT);
        cacheWordHandler.setTimeout(timeout);
    }

    private Preference.OnPreferenceClickListener changeLockTimeoutListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference pref) {
            changeTimeoutPrompt();
            return true;
        }
	};

    private Preference.OnPreferenceChangeListener passphraseChangeListener = new OnPreferenceChangeListener(){
		@Override
		public boolean onPreferenceChange(Preference pref, Object newValue) {
			// Save option internally in cacheword as well
			try {
				char[] passphrase = ((String) newValue).toCharArray();

				if (PassphraseUtil.validatePassphrase(passphrase)) {
					cacheWordHandler.changePassphrase((PassphraseSecrets) cacheWordHandler.getCachedSecrets(), passphrase);
                    Toast.makeText(getApplicationContext(), R.string.passphrase_changed, Toast.LENGTH_SHORT).show();
				}
                else {
					Toast.makeText(getApplicationContext(), R.string.pass_err_length, Toast.LENGTH_SHORT).show();
				}
			}
            catch (IOException e) {
				Toast.makeText(getApplicationContext(),
						R.string.pass_err, Toast.LENGTH_SHORT).show();
			}
			return false;
		}
	};

    private void changeTimeoutPrompt() {

		if (cacheWordHandler.isLocked()) {
			return;
		}

        AlertDialog.Builder builder = setupAlertDialogBuilder();
        final NumberPicker input = setupNumberPicker();
        int currentTimeout = getCurrentTimeout();
        input.setValue(currentTimeout / 60); // Convert from seconds to minutes
        builder.setView(input);

        builder.setPositiveButton("OK",
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int newTimeout = input.getValue() * 60; // Convert from minutes to seconds
                    setNewTimeout(newTimeout);
                    dialog.dismiss();
                }
            });
        builder.setNegativeButton("Cancel",
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

        builder.show();
    }

    private AlertDialog.Builder setupAlertDialogBuilder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.change_timeout_prompt_title);
        builder.setMessage(R.string.change_timeout_prompt);
        return builder;
    }

    private NumberPicker setupNumberPicker() {
        final NumberPicker input = new NumberPicker(this);
        input.setMinValue(1);
        input.setMaxValue(60);
        return input;
    }

    private int getCurrentTimeout() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int currentTimeout = preferences.getInt(KEY_CACHEWORD_TIMEOUT, DEFAULT_CACHEWORD_TIMEOUT);
        Log.d(TAG, "Got the following currentTimeout: " + currentTimeout);
        return currentTimeout;
    }

    private void setNewTimeout(int newTimeout) {

        Log.d(TAG, "Setting the following newTimeout: " + newTimeout);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_CACHEWORD_TIMEOUT, newTimeout);
        editor.commit();

        cacheWordHandler.setTimeout(newTimeout);
    }
}