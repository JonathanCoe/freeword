package io.darknote.database;

import android.util.Log;
import net.sqlcipher.database.SQLiteDatabase;

public class NoteTable
{
  public static final String TABLE_NAME = "note";

  public static final String COLUMN_ID = "id";
  public static final String COLUMN_TITLE = "title";
  public static final String COLUMN_BODY = "body";

  private static final String DATABASE_CREATE_STATEMENT = "create table "
      + TABLE_NAME
      + "(" 
      + COLUMN_ID     + " integer primary key autoincrement, "
      + COLUMN_TITLE  + " text not null, "
      + COLUMN_BODY   + " text not null"
      + ");";

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE_STATEMENT);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(NoteTable.class.getName(), "Upgrading database from version " + oldVersion + " to "
                                     + newVersion  + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(database);
    }
}