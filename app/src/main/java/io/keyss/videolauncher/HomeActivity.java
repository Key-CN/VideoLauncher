package io.keyss.videolauncher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

import io.keyss.videolauncher.utils.ScreenTool;

/**
 * @author MrKey
 */
public class HomeActivity extends Activity {

    private Button start;
    private static final String mCheckStartString = "time=";
    private static final String mCheckEndString = " ms";
    private Double mLastDelay;
    private Double[] mDelays = new Double[10];
    private int mDelaysIndex = 0;
    private TextView tv_wifi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        //ScreenTool.hideNavbar(this);
        ScreenTool.showNavbar(this);
        start = findViewById(R.id.b_start);
        tv_wifi = (TextView) findViewById(R.id.tv_wifi);
        start.setOnClickListener(v -> {
            if (mLastDelay > 1500) {
                Toast.makeText(HomeActivity.this, "当前网络质量不佳，暂时无法启动", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent init = new Intent(Intent.ACTION_VIEW, Uri.parse("video://init"));
            try {
                startActivity(init);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Looper.myQueue().addIdleHandler(() -> {
            onActivityInitialized();
            return false;
        });
    }

    private void onActivityInitialized() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                checkNetwork();
            }
        }).start();

        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            wifi.setWifiEnabled(true);
            WifiInfo connectionInfo = wifi.getConnectionInfo();
            connectionInfo.getSupplicantState();
            tv_wifi.post(new Runnable() {
                @Override
                public void run() {
                    tv_wifi.setText(connectionInfo.getBSSID());
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        // 禁用返回键
    }

    private void checkNetwork() {
        try {
            Process process = Runtime.getRuntime().exec("ping -n -i 3 test.futurearriving.com");
            BufferedReader bf = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int startIndex;
            while ((line = bf.readLine()) != null) {
                startIndex = line.lastIndexOf(mCheckStartString);
                if (startIndex > 0) {
                    mLastDelay = Double.valueOf(line.substring(startIndex + mCheckStartString.length(), line.lastIndexOf(mCheckEndString)));
                    mDelays[mDelaysIndex++] = mLastDelay;
                    if (mDelaysIndex > 9) {
                        mDelaysIndex = 0;
                    }
                    Log.e("print", "延时: " + mLastDelay + "  arrays: " + Arrays.toString(mDelays));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
