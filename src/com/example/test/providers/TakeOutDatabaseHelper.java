package com.example.test.providers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class TakeOutDatabaseHelper extends SQLiteOpenHelper {
    private final static String TAG = "TakeOutDatabaseHelper";
    private static TakeOutDatabaseHelper sInstance = null;
    static final String DATABASE_NAME = "takeout.db";
    static final int DATABASE_VERSION = 1;
    private final Context mContext;

    private TakeOutDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    /**
     * Return a singleton helper for the combined MMS and SMS
     * database.
     */
    /* package */
    static synchronized TakeOutDatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new TakeOutDatabaseHelper(context);
        }
        return sInstance;
    }

    private void createMmsTables(SQLiteDatabase db) {
        // N.B.: Whenever the columns here are changed, the columns in
        // {@ref MmsSmsProvider} must be changed to match.
        db.execSQL("CREATE TABLE " + TakeOutProvider.TABLE_LOGIN  + " (" +
                   " _id INTEGER PRIMARY KEY, " +
                   " mac TEXT, " +
                   " version TEXT, " +
                   " universityId TEXT, " +
                   " actionCode TEXT, " +
                   " status INTEGER DEFAULT -1," +
                   " date INTEGER " +
                   ");");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createMmsTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TakeOutProvider.TABLE_LOGIN );
        onCreate(db);
    }

}
