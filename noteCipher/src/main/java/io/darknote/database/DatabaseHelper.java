package io.darknote.database;

import android.content.Context;
import info.guardianproject.cacheword.CacheWordHandler;
import net.sqlcipher.database.SQLiteDatabase;

public class DatabaseHelper extends SQLCipherOpenHelper
{
	protected static final String DATABASE_NAME = "darknote_database";
	private static final int DATABASE_VERSION = 1;
	
	public DatabaseHelper(Context context, CacheWordHandler cacheWordHandler) {
		super(cacheWordHandler, context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
        NoteTable.onCreate(database);
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		// Nothing to do here currently
	}
}