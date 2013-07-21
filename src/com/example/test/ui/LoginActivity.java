package com.example.test.ui;

import com.example.test.R;
import com.example.test.providers.TakeOut;
import com.example.test.transation.ConnectReceiver;
import com.example.test.transation.ConnectTaskService;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
/**
 *
 * 这个列子主要是仿照mms来做的，问题是给的这个连接的列子实在太简单了远远没有短信那么复杂，所以很多比较好的思想并没有完全表达出来。
 * 主要思想：
 * 1、当要发送数据给服务器时，先把数据保存在本地，并且显示为未发送状态。
 * 2、启动services，services里面启动thread
 * 3、通过thread来和服务器交互
 * 4、如果发送成功则更新数据库表示成功
 * 5、如果失败则更新数据库表示失败 6、无论成功失败都会有相应的处理
 * 主要好处：
 * 1、支持离线浏览
 * 2、网络状态无论何时变化都不会影响用户使用（比如：用户在离线看的时候，切换开启网络即可立马刷新界面）
 * 3、启动services不会被随意的android系统杀死
 * 4、当手机处于睡眠状态时（睡眠状态时手机中的cpu也会处于休眠状态），services可以保证处于运行状态，而使用线程却会停掉或者是处于无法唤起的状态。
 *
 */
public class LoginActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "LoginActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = (Button) findViewById(R.id.connect);
        button.setOnClickListener(this);
    }

    public Uri addLogInToUri(String mac, String version, String universityId,
            String actionCode, int status) {
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues(5);
        values.put(TakeOut.LogIn.MAC, mac);
        values.put(TakeOut.LogIn.VERSION, version);
        values.put(TakeOut.LogIn.UNIVERSITYID, universityId);
        values.put(TakeOut.LogIn.ACTIONCODE, actionCode);
        values.put(TakeOut.LogIn.STATUS, status);
        return resolver.insert(TakeOut.LogIn.CONTENT_URI, values);
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick");
        addLogInToUri("", "Android1.5.0", "","9201015", TakeOut.LogIn.STATUS_NONE);
        sendBroadcast(new Intent(ConnectTaskService.ACTION_SEND_MESSAGE,
                null,
                this,
                ConnectReceiver.class));
    }


}
