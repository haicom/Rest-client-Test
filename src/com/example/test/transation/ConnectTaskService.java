package com.example.test.transation;



import java.util.ArrayList;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.example.test.providers.TakeOut;
import com.example.test.providers.TakeOut.LogIn;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

public class ConnectTaskService extends Service {
    private static final String TAG = "ConnectionTaskService";
    private ServiceHandler mServiceHandler;
    private Looper mServiceLooper;
    private int mResultCode;
    private ConnectivityManager mConnMgr;

 // This must match the column IDs below.
    private static final String[] SEND_PROJECTION = new String[] {
        TakeOut.LogIn._ID,            //0
        TakeOut.LogIn.MAC,            //1
        TakeOut.LogIn.VERSION,       //2
        TakeOut.LogIn.UNIVERSITYID,  //3
        TakeOut.LogIn.ACTIONCODE     //4
    };

    // This must match SEND_PROJECTION.
    private static final int SEND_COLUMN_ID           = 0;
    private static final int SEND_COLUMN_MAC          = 1;
    private static final int SEND_COLUMN_VERSION      = 2;
    private static final int SEND_COLUMN_UNIVERSITYID = 3;
    private static final int SEND_COLUMN_ACTIONCODE   = 4;

    public static final int CONNECTION_TIMEOUT = 20000;
    public static final int SOCKET_TIMEOUT = 20000;

    public static final String ACTION_SEND_MESSAGE= "com.android.login.transaction.MESSAGE_SENT";

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.
        Log.d(TAG, "onCreate()");
        HandlerThread thread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
     // Temporarily removed for this duplicate message track down.

        mResultCode = intent != null ? intent.getIntExtra("result", 0) : 0;
        mConnMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mResultCode != 0) {
            Log.v(TAG, "onStart: #" + startId + " mResultCode: " + mResultCode +
                    " = " + translateResultCode(mResultCode));
        }

        /*
         * 主要目的是查看网络的状态，
         * 如果处于没有网络的时候就通过注册广播来监听状态的变化
         * 等有网络的时候继续操作
         */

        Log.v(TAG, "onStart: #" + startId + ": " + intent.getExtras() + " intent = " + intent);
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);

        return Service.START_NOT_STICKY;
    }

    private static String translateResultCode(int resultCode) {
        switch (resultCode) {
            case Activity.RESULT_OK:
                return "Activity.RESULT_OK";
            default:
                return "Unknown error code";
        }
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        /**
         * Handle incoming transaction requests. The incoming requests are
         * initiated by the MMSC Server or by the MMS Client itself.
         */
        @Override
        public void handleMessage(Message msg) {
            int serviceId = msg.arg1;
            Intent intent = (Intent) msg.obj;
            Log.d(TAG, "handleMessage serviceId: " + serviceId 	+ " intent: " + intent);


            if (intent != null) {
                String action = intent.getAction();

                int error = intent.getIntExtra("errorCode", 0);

                Log.v(TAG, "handleMessage action: " + action + " error: " + error);
                if (ACTION_SEND_MESSAGE.endsWith(action)) {
                    handleSendMessage();
                }
            }

            // NOTE: We MUST not call stopSelf() directly, since we need to
            // make sure the wake lock acquired by AlertReceiver is released.
            ConnectReceiver.finishStartingService(ConnectTaskService.this,  serviceId);
        }
    }

    /*
     * 处理需要发送的信息
     * 查询数据库找到未发送的信息状态为STATUS_NONE，
     * 然后发送状态为 STATUS_PENDING
     * 发送成功为 STATUS_COMPLETE
     * 发送失败为 STATUS_FAILED
     *
     */
    public synchronized void handleSendMessage() {
        // 查询所有未发送的信息从数据库中
        final Uri uri = Uri.parse("content://takeout/none");
        ContentResolver resolver = getContentResolver();
        Cursor c = resolver.query(uri, SEND_PROJECTION, null,
                                  null, "date ASC"); // date ASC so we send out in
                                                     // same order the user tried
                                                     // to send messages.
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    String mac = c.getString(SEND_COLUMN_MAC);
                    String version = c.getString(SEND_COLUMN_VERSION);
                    String universityId = c.getString(SEND_COLUMN_UNIVERSITYID);
                    String actionCode = c.getString(SEND_COLUMN_ACTIONCODE);

                    int msgId = c.getInt(SEND_COLUMN_ID);
                    Uri msgUri = ContentUris.withAppendedId(TakeOut.LogIn.CONTENT_URI, msgId);
                    Log.d(TAG, "sendFirstQueuedMessage " + msgUri +
                                ", mac =  " + mac +
                                ", version = " + version +
                                ", universityId = " + universityId +
                                ", actionCode = " + actionCode);

                    try {
                        loginConnection(mac, version, universityId, actionCode, msgUri);
                    } catch (Exception e) {
                        Log.e(TAG, "sendFirstQueuedMessage: failed to send message " + msgUri
                                + ", caught ", e);
                        //如果连接有异常则在数据库中保存异常信息,怎么处理这些异常信息呢？我个人感觉不如在网络开启时处理这些异常信息
                        updateLoginStatus(msgUri, LogIn.STATUS_FAILED);
                    }
                }
            } finally {
                c.close();
            }
        }

    }

    /**
     * 我现在不清楚这部分与服务器连接的部分会有什么异常，并且会有什么异常信息，如果有的话最好是保存
     * @param mac
     * @param version
     * @param universityId
     * @param actionCode
     * @param url
     * @return
     * @throws Exception
     */
    private String loginConnection(String mac, String version,
                String universityId, String actionCode, Uri url) throws Exception {
        String result = null;
        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters,
                CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParameters, SOCKET_TIMEOUT);
        HttpProtocolParams.setUserAgent(httpParameters, "Android");
        HttpProtocolParams.setContentCharset(httpParameters, "UTF-8");
        HttpPost httpPost = new HttpPost("http://101.227.252.20:10001/wm2/mainsrv20");
        ArrayList<BasicNameValuePair> pair = new ArrayList<BasicNameValuePair>();
        pair.add( new BasicNameValuePair("mac", mac) );
        pair.add( new BasicNameValuePair("version", version) );
        pair.add( new BasicNameValuePair("universityId", universityId) );
        pair.add( new BasicNameValuePair("actionCode", actionCode) );
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("version", version);
        pair.add(new BasicNameValuePair("formData", jsonObject.toString()));
        UrlEncodedFormEntity uee = new UrlEncodedFormEntity(pair, "utf-8");
        httpPost.setEntity(uee);
        HttpClient httpClient = new DefaultHttpClient(httpParameters);
        updateLoginStatus(url, LogIn.STATUS_PENDING);
        HttpResponse httpResponse = httpClient.execute(httpPost);
        if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            result = EntityUtils.toString(httpResponse.getEntity(), "UTF_8");
            updateLoginStatus(url, LogIn.STATUS_COMPLETE);
        } else {
            updateLoginStatus(url, LogIn.STATUS_FAILED);
        }

        if (httpPost != null) {
            httpPost.abort();
        }
        if (httpClient != null) {
            httpClient.getConnectionManager().shutdown();
        }
        return result;
    }

    /**
     * 更新登陆状态
     *
     * @param uri the message to move
     * @param status the status to move to
     * @return true if the operation succeeded
     */
    public  boolean updateLoginStatus(Uri uri, int status) {
        ContentResolver resolver = getContentResolver();
        if (uri == null) {
            return false;
        }

        ContentValues values = new ContentValues(1);

        values.put(TakeOut.LogIn.STATUS, status);

        return 1 == resolver.update(uri, values, null, null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /***
     * 当网络改变时即把错误的数据全部设置为原始数据
     *
     */
    public static class NetWorkBroadcastReceiver extends BroadcastReceiver {
        Context context ;
        @Override
        public void onReceive(Context context, Intent intent) {
            this.context = context;
            String action = intent.getAction();
            Log.d(TAG, "NetWorkBroadcastReceiver onReceive() action: " + action);

            if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                return;
            }

            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
            /*
             * If we are being informed that connectivity has been established
             * to allow MMS traffic, then proceed with processing the pending
             * transaction, if any.
             */
            Log.d(TAG, "Handle NetWorkBroadcastReceiver onReceive(): networkInfo = " + networkInfo );

            if (networkInfo != null) {
                Log.d(TAG, "NetWorkBroadcastReceiver networkInfo.getType() = " + networkInfo.getType() + " networkInfo.isConnected() = " + networkInfo.isConnected());
            }

            // Check availability of the mobile network.
            if ( ( networkInfo == null ) ||
                 !( ( networkInfo.getType() == ConnectivityManager.TYPE_WIFI ) ||
                    ( networkInfo.getType() == ConnectivityManager.TYPE_MOBILE ) ) ) {
                Log.d(TAG, " NetWorkBroadcastReceiver type is not TYPE_WIFI TYPE_MOBILE, bail");
                return;
            }

            if ( ! networkInfo.isConnected() ) {
                Log.d(TAG, " NetWorkBroadcastReceiver Wifi or Mobile not connected, bail");
                return;
            }

            updateFailedStatus(TakeOut.LogIn.CONTENT_URI, LogIn.STATUS_NONE,
                    LogIn.STATUS_FAILED);
        }


        public  boolean updateFailedStatus(Uri uri, int status, int failedStatus) {
            ContentResolver resolver = context.getContentResolver();
            String where = null;
            if (uri == null) {
                return false;
            }

            ContentValues values = new ContentValues(1);

            values.put(TakeOut.LogIn.STATUS, status);
            where =  TakeOut.LogIn.STATUS + " = " + failedStatus;
            return 1 == resolver.update(uri, values, where, null);
        }
    };

}
