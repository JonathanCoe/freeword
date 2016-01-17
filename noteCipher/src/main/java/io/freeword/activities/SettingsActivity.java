package io.freeword.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import io.freeword.core.NConstants;
import io.freeword.R;
import net.simonvt.numberpicker.NumberPicker;

import java.io.IOException;

@SuppressLint("NewApi")
@SuppressWarnings("deprecation")
public class SettingsActivity extends SherlockPreferenceActivity implements ICacheWordSubscriber {

	private CacheWordHandler cacheWordHandler;

    private static final String TAG = SettingsActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		cacheWordHandler = new CacheWordHandler(this);
		cacheWordHandler.connectToService();

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
		}

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// If in android 3+ use a preference fragment which is the new recommended way
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
			// Otherwise load the preferences.xml in the Activity like in previous android versions
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
    }

    void showLockScreen() {
        Intent intent = new Intent(this, LockScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("originalIntent", getIntent());
        startActivity(intent);
        finish();
    }

    public static final boolean getNoteLinesOption(Context context) {
        boolean defValue = context.getResources().getBoolean(R.bool.notecipher_uselines_default);
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(NConstants.SHARED_PREFS_NOTELINES, defValue);
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
				char[] pass = ((String) newValue).toCharArray();

				if (NConstants.validatePassword(pass)) {
					cacheWordHandler.changePassphrase((PassphraseSecrets) cacheWordHandler.getCachedSecrets(), pass);
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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.change_timeout_prompt_title);
        builder.setMessage(R.string.change_timeout_prompt);
        final NumberPicker input = new NumberPicker(this);
        input.setMinValue(1);
        input.setMaxValue(60);
        input.setValue( cacheWordHandler.getTimeout());
        builder.setView(input);

        builder.setPositiveButton("OK",
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int timeout = input.getValue();
                    cacheWordHandler.setTimeout(timeout);
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
}