package io.keyss.videolauncher;

import android.app.Activity;
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

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.keyss.videolauncher.utils.KeyDoubleClickCheckUtil;
import io.keyss.videolauncher.utils.ScreenTool;

/**
 * @author MrKey
 */
public class HomeActivity extends Activity {

    public static boolean isWifiConnected;

    private static final int START_TIME = 8;
    private static final int END_TIME = 18;

    private Button start;
    private ImageView iv_wifi;
    private TextView tv_wifi;
    private TextView tv_wifi_rssi;
    private TextView tv_wifi_speed;
    private TextView tv_wifi_state;


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
        executorService = Executors.newCachedThreadPool();

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
        tv_wifi_rssi = findViewById(R.id.tv_wifi_rssi);
        tv_wifi_speed = findViewById(R.id.tv_wifi_speed);
        tv_wifi_state = findViewById(R.id.tv_wifi_state);
        iv_wifi = findViewById(R.id.iv_wifi);
        iv_wifi.setOnClickListener(v -> startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS)
                .putExtra("wifi_enable_next_on_connect", true) //是否打开网络连接检测功能（如果连上wifi，则下一步按钮可被点击）
                .putExtra("extra_prefs_show_button_bar", true)
                .putExtra("extra_prefs_set_next_text", "完成")
                .putExtra("extra_prefs_set_back_text", "取消"), 2223));
        start.setOnClickListener(v -> {
            if (KeyDoubleClickCheckUtil.checkFastDouble(2000)) {
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

    }

    private void updateWifiInfo() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        Log.e("Key123", "wifiInfo: " + wifiInfo.getSupplicantState());
        tv_wifi_speed.setText(wifiInfo.getLinkSpeed() + WifiInfo.LINK_SPEED_UNITS);
        tv_wifi.setText(wifiInfo.getSSID());
        tv_wifi_rssi.setText(wifiInfo.getRssi() + "db");
        tv_wifi_state.setText(wifiInfo.getSupplicantState().name());
        // COMPLETED
        if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
            // 检测后台程序，然后打开APP
        }
    }

    private boolean isInSchoolTime() {
        if (calendar == null) {
            calendar = Calendar.getInstance();
        }
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        return hour >= START_TIME && hour < END_TIME;
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
        isActivityVisible = true;
        // TODO 开启检测
        if (isInSchoolTime()) {
            executorService.execute(() -> {
                while (isActivityVisible) {
                    runOnUiThread(this::updateWifiInfo);
                    SystemClock.sleep(5000);
                }
            });
        } else {
            tv_wifi.setText("非上学时间暂停检测");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityVisible = false;
        // TODO 关闭检测
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