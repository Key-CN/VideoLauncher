package io.keyss.videolauncher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
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

    private static final String mCheckStartString = "time=";
    private static final String mCheckEndString = " ms";
    private static final double WIFI_3 = 500;
    private static final double WIFI_2 = 1000;
    private static final double WIFI_1 = 1500;
    private static final double WIFI_X = 2000;

    private Button start;
    private ImageView iv_wifi;
    private TextView tv_wifi;

    private Double mLastDelay;
    private Double[] mDelays = new Double[10];
    private int mDelaysIndex = 0;
    private boolean isWifiConnect;
    private boolean isMainStart;
    private boolean isActivityVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();

        Looper.myQueue().addIdleHandler(() -> {
            onActivityInitialized();
            return false;
        });
    }

    private void initView() {
        setContentView(R.layout.activity_home);
        //ScreenTool.hideNavbar(this);
        ScreenTool.showNavbar(this);
        start = findViewById(R.id.b_start);
        tv_wifi = findViewById(R.id.tv_wifi);
        iv_wifi = findViewById(R.id.iv_wifi);
        start.setOnClickListener(v -> {
            if (!isWifiConnect) {
                Toast.makeText(HomeActivity.this, "当前网络质量不佳，暂时无法启动", Toast.LENGTH_SHORT).show();
                return;
            }
            startMainApp();
        });
    }

    private void startMainApp() {
        Intent init = new Intent(Intent.ACTION_VIEW, Uri.parse("video://init"));
        try {
            startActivity(init);
            isMainStart = true;
        } catch (Exception e) {
            e.printStackTrace();
            isMainStart = false;
        }
    }

    private void onActivityInitialized() {
        new Thread(this::checkNetwork).start();
    }

    private void changeWifiSSID() {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            wifi.setWifiEnabled(true);
            WifiInfo connectionInfo = wifi.getConnectionInfo();
            connectionInfo.getSupplicantState();
            tv_wifi.post(new Runnable() {
                @Override
                public void run() {
                    tv_wifi.setText(connectionInfo.getSSID());
                }
            });
        }
    }

    private double getWifiStatus() {
        if (!isWifiConnect) {
            return WIFI_X;
        }
        double ping = 0;
        for (Double delay : mDelays) {
            if (delay != null) {
                ping += delay;
            }
        }
        return ping;
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
                    if (isWifiConnect = mLastDelay < WIFI_3) {
                        if (isActivityVisible) {
                            // 三条杠WIFI
                            iv_wifi.post(() -> iv_wifi.setImageResource(R.drawable.wifi_3));
                        }
                        if (!isMainStart) {
                            startMainApp();
                        }
                    } else if (mLastDelay < WIFI_2) {
                        if (isActivityVisible) {
                            // 2条杠WIFI
                            //iv_wifi.post(() -> iv_wifi.setImageResource(R.drawable.wifi_3));
                        }
                    } else if (mLastDelay < WIFI_1) {
                        if (isActivityVisible) {
                            // 1条杠WIFI
                            //iv_wifi.post(() -> iv_wifi.setImageResource(R.drawable.wifi_3));
                        }
                    } else {
                        if (isActivityVisible) {
                            // 叉叉WIFI
                            iv_wifi.post(() -> iv_wifi.setImageResource(R.drawable.wifi_x));
                        }
                    }
                    mDelays[mDelaysIndex++] = mLastDelay;
                    if (mDelaysIndex > 9) {
                        mDelaysIndex = 0;
                        if (getWifiStatus() < WIFI_3) {
                            // TODO 发广播给主APP
                        } else {

                        }
                    }
                    Log.e("print", "延时: " + mLastDelay + "  arrays: " + Arrays.toString(mDelays));
                }
            }
            int waitFor = process.waitFor();
            Log.e("waitFor", "返回值: " + waitFor);
            if (waitFor == 0) {
                // 通畅，不过没机会到这里，被无限循环的ping阻挡了
                isWifiConnect = true;
            } else {
                // 1，需要网页认证的wifi 2，当前网络不可用， 递归检测
                isWifiConnect = false;
                if (isActivityVisible) {
                    // 叉叉WIFI
                    iv_wifi.post(() -> iv_wifi.setImageResource(R.drawable.wifi_x));
                }
                SystemClock.sleep(3000);
                checkNetwork();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        isActivityVisible = true;
        changeWifiSSID();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityVisible = false;
    }

    @Override
    public void onBackPressed() {
        // 禁用返回键
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
