package io.keyss.videolauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

/**
 * @author Key
 * Time: 2018/8/4 14:03
 * Description:
 * <receiver android:name=".ControlBroadcast" >
 * <intent-filter>
 * <action android:name="android.net.conn.CONNECTIVITY_CHANGE" />
 * </intent-filter>
 * </receiver>
 */
@Deprecated
public class ControlBroadcast extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("ControlBroadcast", "action: " + intent.getAction());
        if (null != intent.getAction()) {
            switch (intent.getAction()) {
                case ConnectivityManager.CONNECTIVITY_ACTION:

                    break;
                default:
                    break;
            }
        }
    }
}
