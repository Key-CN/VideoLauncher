package io.keyss.videolauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * @author Key
 * Time: 2018/8/4 14:03
 * Description:
 */
public class WifiBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("WifiBroadcastReceiver", "action: " + intent.getAction());
        if (null != intent.getAction()) {
            switch (intent.getAction()) {
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo localNetworkInfo = (connectivityManager == null ? null : connectivityManager.getActiveNetworkInfo());
                    if (localNetworkInfo != null) {
                        //已连接
                        HomeActivity.isWifiConnected = localNetworkInfo.isConnected();
                        Log.e("Key", "isWifiConnected1: " + HomeActivity.isWifiConnected);
                    } else {
                        HomeActivity.isWifiConnected = false;
                        Log.e("Key", "isWifiConnected2: " + HomeActivity.isWifiConnected);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
