package com.example.test.providers;

import android.net.Uri;
import android.provider.BaseColumns;

public final class TakeOut {
    private final static String TAG = "TakeOut";
    public static final String AUTHORITY = "takeout";

    // Constructor
    public TakeOut() {
    }

    public static final class LogIn implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI =
            Uri.parse("content://takeout/login");

        public static final String MAC = "mac";
        public static final String VERSION = "version";
        public static final String UNIVERSITYID = "universityId";
        public static final String ACTIONCODE = "actionCode";
        /**
         * 显示发送状态的变化，
         * 未发送时为 STATUS_NONE,
         * 发送成功为 STATUS_COMPLETE ,
         * 发送中为 STATUS_PENDING ，
         * 发送失败为STATUS_FAILED
         */
        public static final String STATUS = "status";

        public static final int STATUS_NONE = -1;
        public static final int STATUS_COMPLETE = 0;
        public static final int STATUS_PENDING = 32;
        public static final int STATUS_FAILED = 64;
        /**
         * The date the message was received
         * <P>Type: INTEGER (long)</P>
         */
        public static final String DATE ="date";
        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "date DESC";
    }
}
