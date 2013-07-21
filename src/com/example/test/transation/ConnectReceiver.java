package com.example.test.transation;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;


public class ConnectReceiver extends BroadcastReceiver {
    private final static String TAG ="ConnectReceiver";
    static final Object mStartingServiceSync = new Object();
    static PowerManager.WakeLock mStartingService;
    private static ConnectReceiver sInstance;

    public static ConnectReceiver getInstance() {
        if (sInstance == null) {
            sInstance = new ConnectReceiver();
        }
        return sInstance;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, " onReceive intent = " + intent.toString());
        onReceiveWithPrivilege(context, intent);
    }

    protected void onReceiveWithPrivilege(Context context, Intent intent) {
        intent.setClass(context, ConnectTaskService.class);
        intent.putExtra("result", getResultCode());
        Log.d(TAG, " onReceiveWithPrivilege getResultCode() = " + getResultCode());
        beginStartingService(context, intent);
    }


    /**
     * Start the service to process the current event notifications, acquiring
     * the wake lock before returning to ensure that the service will run.
     */
    public static void beginStartingService(Context context, Intent intent) {

        synchronized (mStartingServiceSync) {
            if (mStartingService == null) {
                PowerManager pm =
                    (PowerManager)context.getSystemService(Context.POWER_SERVICE);
                mStartingService = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        "StartingAlertService");
                mStartingService.setReferenceCounted(false);
            }
            mStartingService.acquire();
            context.startService(intent);
        }
    }

    /**
     * Called back by the service when it has finished processing notifications,
     * releasing the wake lock if the service is now stopping.
     */
    public static void finishStartingService(Service service, int startId) {
        synchronized (mStartingServiceSync) {
            if (mStartingService != null) {
                if (service.stopSelfResult(startId)) {
                    mStartingService.release();
                }
            }
        }
    }


}
