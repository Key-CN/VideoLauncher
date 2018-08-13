package io.keyss.videolauncher;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.keyss.videolauncher.utils.KeyDoubleClickCheckUtil;
import io.keyss.videolauncher.utils.ScreenTool;

/**
 * @author MrKey
 */
public class HomeActivity extends Activity {

    private String mAppPackageName = "io.keyss.microbeanandroid";
    private static final int START_TIME = 8;
    private static final int END_TIME = 18;

    private Button start;
    private ImageView iv_wifi;
    private TextView tv_wifi;
    private TextView tv_wifi_rssi;
    private TextView tv_wifi_speed;
    private TextView tv_wifi_state;
    private TextView tv_ip;

    private boolean isMainStart;
    private boolean isActivityVisible;
    private Calendar calendar;
    private ExecutorService executorService;
    private WifiManager wifiManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
            }
        }
        executorService = Executors.newSingleThreadExecutor();

        Looper.myQueue().addIdleHandler(() -> {
            onActivityInitialized();
            return false;
        });
    }

    private void initView() {
        setContentView(R.layout.activity_home);
        ScreenTool.hideNavbar(this);
        //ScreenTool.showNavbar(this);
        start = findViewById(R.id.b_start);
        tv_wifi = findViewById(R.id.tv_wifi);
        tv_wifi_rssi = findViewById(R.id.tv_wifi_rssi);
        tv_wifi_speed = findViewById(R.id.tv_wifi_speed);
        tv_wifi_state = findViewById(R.id.tv_wifi_state);
        tv_ip = findViewById(R.id.tv_ip);
        iv_wifi = findViewById(R.id.iv_wifi);
        iv_wifi.setOnClickListener(v -> startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS)
                .putExtra("wifi_enable_next_on_connect", true) //是否打开网络连接检测功能（如果连上wifi，则下一步按钮可被点击）
                .putExtra("extra_prefs_show_button_bar", true)
                .putExtra("extra_prefs_set_next_text", "完成")
                .putExtra("extra_prefs_set_back_text", "取消"), 2223));
        start.setOnClickListener(v -> {
            if (isMainStart || KeyDoubleClickCheckUtil.checkFastDouble(2000)) {
                Toast.makeText(this, "正在启动", Toast.LENGTH_SHORT).show();
            } else {
                startMainApp();
            }
        });
        findViewById(R.id.hidden_function).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction("ACTION_RK_REBOOT");
            sendBroadcast(intent, null);
        });
    }

    private void startMainApp() {
        if (!isMainStart) {
            isMainStart = true;
            executorService.shutdownNow();
            Intent init = new Intent(Intent.ACTION_VIEW, Uri.parse("video://init"));
            try {
                Log.e("Keyss.io", "启动APP: " + init);
                startActivity(init);
            } catch (Exception e) {
                // ActivityNotFoundException
                e.printStackTrace();
            }
            SystemClock.sleep(1000);
            isMainStart = false;
        }
    }

    private void onActivityInitialized() {
        openWifiAdb();
    }

    /**
     * 打开wifi调试，正式环境中删除，外勤调试使用
     */
    private void openWifiAdb() {
        try {
            /*String cmd = "su\nsetprop service.adb.tcp.port 5555\nstop adbd\nstart adbd\n";
            Runtime.getRuntime().exec(cmd);*/
            Process exec = Runtime.getRuntime().exec("su");
            OutputStream os = exec.getOutputStream();
            os.write("setprop service.adb.tcp.port 5555\n".getBytes());
            os.write("stop adbd\n".getBytes());
            os.write("start adbd\n".getBytes());
            //os.write("adb tcpip 5555\n".getBytes());
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateWifiInfo() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        Log.e("Key123", "wifiInfo: " + wifiInfo.getSupplicantState());
        tv_wifi_speed.setText(wifiInfo.getLinkSpeed() + WifiInfo.LINK_SPEED_UNITS);
        tv_wifi.setText(wifiInfo.getSSID());
        tv_wifi_rssi.setText(wifiInfo.getRssi() + "db");
        tv_wifi_state.setText(wifiInfo.getSupplicantState().name());
        tv_ip.setText(intToIp(wifiInfo.getIpAddress()));
        // COMPLETED
        if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
            // 检测后台程序，然后打开APP
            searchApp();
        }
    }

    private boolean isInSchoolTime() {
        if (calendar == null) {
            calendar = Calendar.getInstance();
        }
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        return hour >= START_TIME && hour < END_TIME;
    }

    public void searchApp() {
        ActivityManager am = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE));
        if (null != am) {
            /*List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo info : processInfos) {
                if (mAppPackageName.equals(info.processName)) {
                    // TODO 杀掉后启动
                    Log.e("Key123", "杀掉: " + info.processName + "  pid: " + info.pid + "  uid: " + info.uid);
                    am.killBackgroundProcesses(info.processName);
                    SystemClock.sleep(1000);
                    break;
                }
            }*/
            am.killBackgroundProcesses(mAppPackageName);
            SystemClock.sleep(1000);
            startMainApp();
        } else {
            startMainApp();
        }
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
    protected void onResume() {
        super.onResume();
        // TODO 开启检测
        isActivityVisible = true;
        if (isInSchoolTime()) {
            executorService.execute(() -> {
                while (isActivityVisible) {
                    SystemClock.sleep(3000);
                    runOnUiThread(this::updateWifiInfo);
                    SystemClock.sleep(3000);
                }
            });
        } else {
            tv_wifi.setText("非上学时间暂停检测");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // TODO 关闭检测
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


    public String intToIp(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }
}