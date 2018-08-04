package io.keyss.videolauncher;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
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
import java.util.Calendar;

import io.keyss.videolauncher.utils.ScreenTool;

/**
 * @author MrKey
 */
public class HomeActivity extends Activity {

    public static boolean isWifiConnected;

    private static final String mCheckStartString = "time=";
    private static final String mCheckEndString = " ms";
    private static final int START_TIME = 8;
    private static final int END_TIME = 18;

    private Button start;
    private ImageView iv_wifi;
    private TextView tv_wifi;

    private Double mLastDelay = WifiStatus.WIFI_X.value;
    private Double[] mDelays = new Double[10];
    private int mDelaysIndex = 0;
    private boolean isMainStart;
    private boolean isActivityVisible;
    private Calendar calendar;


    enum WifiStatus {
        WIFI_X(2000), WIFI_1(1500), WIFI_2(1000), WIFI_3(500);

        double value;

        WifiStatus(double value) {
            this.value = value;
        }
    }

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
            if (mLastDelay < WifiStatus.WIFI_2.value) {
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
        // TODO 开启一个定时任务，而不是无限循环，迟早要断
        new Thread(this::checkNetwork).start();
    }

    private boolean isInSchoolTime() {
        if (calendar == null) {
            calendar = Calendar.getInstance();
        }
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        return hour >= START_TIME && hour < END_TIME;
    }

    private void changeWifiSSID() {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            wifi.setWifiEnabled(true);
            WifiInfo connectionInfo = wifi.getConnectionInfo();
            connectionInfo.getSupplicantState();
            tv_wifi.post(() -> tv_wifi.setText(connectionInfo.getSSID()));
        }
    }

    private int getWifiStatus() {
        if (!isWifiConnected) {
            return WifiStatus.WIFI_X.ordinal();
        }
        double ping = 0;
        int count = 0;
        for (Double delay : mDelays) {
            if (delay != null) {
                ping += delay;
                count++;
            }
        }
        return ping / count < WifiStatus.WIFI_3.value ?
                WifiStatus.WIFI_3.ordinal() : ping < WifiStatus.WIFI_2.value ?
                WifiStatus.WIFI_2.ordinal() : ping < WifiStatus.WIFI_1.value ?
                WifiStatus.WIFI_1.ordinal() : WifiStatus.WIFI_X.ordinal();
    }

    private void checkNetwork() {
        try {
            changeWifiSSID();
            Process process = Runtime.getRuntime().exec("ping -c 1 test.futurearriving.com");
            BufferedReader bf = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int startIndex;
            while ((line = bf.readLine()) != null) {
                //Log.e("Key", line);
                startIndex = line.lastIndexOf(mCheckStartString);
                if (startIndex > 0) {
                    mLastDelay = Double.valueOf(line.substring(startIndex + mCheckStartString.length(), line.lastIndexOf(mCheckEndString)));
                    if (isWifiConnected = mLastDelay < WifiStatus.WIFI_3.value) {
                        if (isActivityVisible) {
                            // 三条杠WIFI
                            iv_wifi.post(() -> iv_wifi.setImageResource(R.drawable.wifi_3));
                        }
                        if (isInSchoolTime() && !isMainStart) {
                            startMainApp();
                            sendWifiStatusBroadcast(getWifiStatus());
                        }
                    } else if (mLastDelay < WifiStatus.WIFI_2.value) {
                        if (isActivityVisible) {
                            // 2条杠WIFI
                            iv_wifi.post(() -> iv_wifi.setImageResource(R.drawable.wifi_2));
                        }
                    } else if (mLastDelay < WifiStatus.WIFI_1.value) {
                        if (isActivityVisible) {
                            // 1条杠WIFI
                            iv_wifi.post(() -> iv_wifi.setImageResource(R.drawable.wifi_1));
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
                        sendWifiStatusBroadcast(getWifiStatus());

                    }
                    Log.i("Key", "延时: " + mLastDelay + "  arrays: " + Arrays.toString(mDelays));
                }
            }
            int waitFor = process.waitFor();
            Log.e("Key", "waitFor: " + waitFor);
            if (waitFor == 0) {
                // 0就直接重启
                SystemClock.sleep(3000);
                // StackOverflowError
                checkNetwork();
            } else {
                // 1，需要网页认证的wifi 2，当前网络不可用， 递归检测
                isWifiConnected = false;
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

    private void sendWifiStatusBroadcast(int status) {
        Intent intent = new Intent();
        intent.setAction("io.keyss.action.WifiStatus");
        intent.putExtra("status", status);
        //发送广播
        sendBroadcast(intent);
    }

    public void controlAlarm(long startTime, long matchId, Intent nextIntent) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int) matchId, nextIntent, PendingIntent.FLAG_ONE_SHOT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, startTime, pendingIntent);
    }

    public void cancelAlarm(String action, long matchId) {
        Intent intent = new Intent(action);
        PendingIntent sender = PendingIntent.getBroadcast(this, (int) matchId, intent, 0);

        // And cancel the alarm.
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }

    @Override
    protected void onStart() {
        super.onStart();
        isActivityVisible = true;
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