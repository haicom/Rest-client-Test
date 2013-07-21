package com.example.test.providers;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;



public class TakeOutProvider extends ContentProvider {
    private final static String TAG = "TakeOutProvider";
    public  static String TABLE_LOGIN = "login";

    private SQLiteOpenHelper mOpenHelper;
    private final static String VND_ANDROID_LOGIN = "vnd.android.cursor.item/login";
    private final static String VND_ANDROID_DIR_LOGIN =
            "vnd.android.cursor.dir/login";
    @Override
    public boolean onCreate() {
        mOpenHelper = TakeOutDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        // Generate the body of the query.
        int match = sURLMatcher.match(uri);
        switch (match) {
            case TAKEOUT_LOGIN:
                qb.setTables(TABLE_LOGIN);
                break;
            case TAKEOUT_LOGIN_ID:
                qb.setTables(TABLE_LOGIN);
                qb.appendWhere("(_id = " + uri.getPathSegments().get(1) + ")");
                break;
            case TAKEOUT_NONE:
                constructQueryForBox(qb, TakeOut.LogIn.STATUS_NONE);
                break;
            case TAKEOUT_COMPLETE:
                constructQueryForBox(qb, TakeOut.LogIn.STATUS_COMPLETE);
                break;
            case TAKEOUT_PENDING:
                constructQueryForBox(qb, TakeOut.LogIn.STATUS_PENDING);
                break;
            case TAKEOUT_FAILED:
                constructQueryForBox(qb, TakeOut.LogIn.STATUS_FAILED);
                break;
        }

        String orderBy = null;

        if (!TextUtils.isEmpty(sortOrder)) {
            orderBy = sortOrder;
        } else if (qb.getTables().equals(TABLE_LOGIN)) {
            orderBy = TakeOut.LogIn.DEFAULT_SORT_ORDER;
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor ret = qb.query(db, projection, selection, selectionArgs,
                              null, null, orderBy);

        return ret;
    }

    private void constructQueryForBox(SQLiteQueryBuilder qb, int status) {
        qb.setTables(TABLE_LOGIN);
        qb.appendWhere(" status = " + status);

    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        int count;
        int match = sURLMatcher.match(uri);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (match) {
            case TAKEOUT_LOGIN:
                count = db.delete(TABLE_LOGIN, where, whereArgs);
                break;

            case TAKEOUT_LOGIN_ID:
                int message_id;

                try {
                    message_id = Integer.parseInt(uri.getPathSegments().get(1));
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                        "Bad message id: " + uri.getPathSegments().get(1));
                }

                where = DatabaseUtils.concatenateWhere("_id = " + message_id, where);
                count = db.delete(TABLE_LOGIN, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URL");
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        Log.d(TAG, " getType uri =   " + uri);
        switch (uri.getPathSegments().size()) {
            case 0:
                return VND_ANDROID_DIR_LOGIN;
            case 1:
                try {
                    Integer.parseInt(uri.getPathSegments().get(0));
                    return VND_ANDROID_LOGIN;
                } catch (NumberFormatException ex) {
                    return VND_ANDROID_DIR_LOGIN;
                }
        }

        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        ContentValues values;
        long rowID;
        int match = sURLMatcher.match(uri);
        int status = TakeOut.LogIn.STATUS_NONE;
        String table = TABLE_LOGIN;

        switch (match) {
            case TAKEOUT_LOGIN:
                break;
            case TAKEOUT_NONE:
                status = TakeOut.LogIn.STATUS_NONE;
                break;

            case TAKEOUT_COMPLETE:
                status = TakeOut.LogIn.STATUS_COMPLETE;
                break;

            case TAKEOUT_PENDING:
                status = TakeOut.LogIn.STATUS_COMPLETE;
                break;

            case TAKEOUT_FAILED:
                status = TakeOut.LogIn.STATUS_FAILED;
                break;

            default:
                Log.e(TAG, "Invalid request: " + uri);
                return null;
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        boolean addDate = false;
        boolean addStatus = false;

        // Make sure that the date and type are set
        if (initialValues == null) {
            values = new ContentValues(1);
            addDate = true;
            addStatus = true;
        } else {
            values = new ContentValues(initialValues);

            if (!initialValues.containsKey(TakeOut.LogIn.DATE)) {
                addDate = true;
            }

            if (!initialValues.containsKey(TakeOut.LogIn.STATUS)) {
                addStatus = true;
            }
        }

        if (addDate) {
            values.put(TakeOut.LogIn.DATE, new Long(System.currentTimeMillis()));
        }

        if (addStatus) {
            values.put(TakeOut.LogIn.STATUS, Integer.valueOf(status));
        }

        rowID = db.insert(table, "body", values);
        if (rowID > 0) {
            Uri url = Uri.parse("content://" + table + "/" + rowID);

            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.d(TAG, "insert " + uri + " succeeded");
            }
            notifyChange(url);
            return url;
        } else {
            Log.e(TAG,"insert: failed! " + values.toString());
        }

        return null;
    }

    @Override
    public int update(Uri uri, ContentValues initialValues, String where, String[] whereArgs) {
        ContentValues values;
        int count = 0;
        String table = TABLE_LOGIN;
        String extraWhere = null;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Log.d(TAG, "update sURLMatcher.match(uri) = " + sURLMatcher.match(uri));
        switch (sURLMatcher.match(uri)) {
            case TAKEOUT_LOGIN:
                table = TABLE_LOGIN;
                break;

            case TAKEOUT_LOGIN_ID:
                extraWhere = "_id=" + uri.getPathSegments().get(1);
                break;

            case TAKEOUT_NONE:
            case TAKEOUT_COMPLETE:
            case TAKEOUT_PENDING:
            case TAKEOUT_FAILED:
                break;
            default:
                throw new UnsupportedOperationException(
                        "URI " + uri + " not supported");
        }

        boolean addDate = false;
        // Make sure that the date and type are set
        if (initialValues == null) {
            values = new ContentValues(1);
            addDate = true;
        } else {
            values = new ContentValues(initialValues);

            if (!initialValues.containsKey(TakeOut.LogIn.DATE)) {
                addDate = true;
            }
        }

        if (addDate) {
            values.put(TakeOut.LogIn.DATE, new Long(System.currentTimeMillis()));
        }

        if (extraWhere != null) {
            where = DatabaseUtils.concatenateWhere(where, extraWhere);
        }

        Log.d(TAG, "update where = " + where);
        count = db.update(table, values, where, whereArgs);

        if (count > 0) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.d(TAG, "update " + uri + " succeeded");
            }
            notifyChange(uri);
        }

        return count;
    }

    private void notifyChange(Uri uri) {
        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(uri, null);
    }

    private static final int TAKEOUT_LOGIN = 1;
    private static final int TAKEOUT_LOGIN_ID = 2;
    private static final int TAKEOUT_NONE = 3;
    private static final int TAKEOUT_COMPLETE = 4;
    private static final int TAKEOUT_PENDING = 5;
    private static final int TAKEOUT_FAILED = 6;
    private static final UriMatcher sURLMatcher =
            new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURLMatcher.addURI(TakeOut.AUTHORITY, "login", TAKEOUT_LOGIN);
        sURLMatcher.addURI(TakeOut.AUTHORITY, "login/#", TAKEOUT_LOGIN_ID);
        sURLMatcher.addURI(TakeOut.AUTHORITY, "none", TAKEOUT_NONE);
        sURLMatcher.addURI(TakeOut.AUTHORITY, "complete", TAKEOUT_COMPLETE);
        sURLMatcher.addURI(TakeOut.AUTHORITY, "pending", TAKEOUT_PENDING);
        sURLMatcher.addURI(TakeOut.AUTHORITY, "failed", TAKEOUT_FAILED);
    }


}
