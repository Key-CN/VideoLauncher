package io.keyss.videolauncher;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.Uri;
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

    private String TAG;
    private static final String APP_PACKAGE_NAME = "io.keyss.microbeanandroid";
    private static final int START_TIME = 8;
    private static final int END_TIME = 18;

    private Button start;
    private ImageView iv_wifi;
    private TextView tv_wifi;
    private TextView tv_wifi_rssi;
    private TextView tv_wifi_speed;
    private TextView tv_wifi_state;
    private TextView tv_ip;
    private ProgressDialog dialog;


    private boolean isMainStart;
    private boolean isActivityVisible;
    private boolean isSearching;
    private Calendar calendar;
    private ExecutorService executorService;
    private WifiManager wifiManager;
    private boolean isFirstStart = true;
    private boolean mFinishBridge;
    private BroadcastReceiver receiver;
    private boolean isWifiAvailable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TAG = getLocalClassName();
        logE("HomeActivity onCreate 开始初始化");
        initView();
        initNetwork();
        executorService = Executors.newCachedThreadPool();

        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);

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
                .putExtra("extra_prefs_set_back_text", "取消"), 2333));
        start.setOnClickListener(v -> {
            if (KeyDoubleClickCheckUtil.checkFastDouble(2000)) {
                Toast.makeText(this, "正在启动中，请稍等", Toast.LENGTH_SHORT).show();
            } else {
                startMainApp();
            }
        });
        start.setOnLongClickListener(v -> {
            ScreenTool.showNavbar(HomeActivity.this);
            return true;
        });
        findViewById(R.id.hidden_function).setOnLongClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction("ACTION_RK_REBOOT");
            sendBroadcast(intent, null);
            return true;
        });
    }

    /**
     * 初始化两个网卡
     * 172.28.128.28 网卡
     * 172.28.128.128 IPC
     */
    private void initNetwork() {
        // TODO 打开有线网络，并配置不常用IP段
        // EthernetManager mEthManager = (EthernetManager) getApplicationContext().getSystemService(Context.ETHERNET_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        /*if (wifiManager != null) {
            if (!wifiManager.isWifiEnabled()) {
                isWifiAvailable = wifiManager.setWifiEnabled(true);
            } else {
                isWifiAvailable = true;
            }
        } else {
            isWifiAvailable = false;
        }*/

        // 1行顶9行示范
        isWifiAvailable = wifiManager != null && (wifiManager.isWifiEnabled() || wifiManager.setWifiEnabled(true));

        IntentFilter filter = new IntentFilter();
        //设置意图过滤
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                    NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if (null != info) {
                        String wifiStateName = info.getDetailedState().name();
                        logE("onReceive - Wifi State Name: " + wifiStateName);
                        tv_wifi_state.setText(wifiStateName);
                        /*NetworkInfo.DetailedState state = info.getDetailedState();
                        if (state == NetworkInfo.DetailedState.CONNECTING) {
                            //connectWifiListener.showState("连接中...");
                        } else if (state == NetworkInfo.DetailedState.AUTHENTICATING) {
                            //connectWifiListener.showState("正在验证身份信息...");
                        } else if (state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                            //connectWifiListener.showState("正在获取IP地址...");
                        } else if (state == NetworkInfo.DetailedState.FAILED) {
                            //connectWifiListener.showState("连接失败，点击任意地方关闭");
                        }*/

                        if (NetworkInfo.State.CONNECTED == info.getState()) {
                            addGateway();
                        }
                    }
                }
            }
        };

        registerReceiver(receiver, filter);
    }

    private void onActivityInitialized() {

    }

    private void startMainApp() {
        if (isWifiAvailable) {
            if (!isMainStart) {
                isMainStart = true;
                // [Terminated, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 1]
                //executorService.shutdownNow();
                Intent init = new Intent(Intent.ACTION_VIEW, Uri.parse("video://init"));
                try {
                    logE("启动APP: " + init);
                    startActivity(init);
                } catch (Exception e) {
                    // ActivityNotFoundException
                    e.printStackTrace();
                    logE("error: " + e.getLocalizedMessage());
                }
                isMainStart = false;
            } else {
                Toast.makeText(this, "正在启动中，请稍等", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "WIFI可能已损坏，请联系客服", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 打开wifi调试，正式环境中删除，外勤调试使用
     */
    private void openWifiAdb() {
        try {
            /*String cmd = "su\nsetprop service.adb.tcp.port 5555\nstop adbd\nstart adbd\n";
            Runtime.getRuntime().exec(cmd);*/
            Process exec = Runtime.getRuntime().exec("su");
            // su root 环境继续输入
            OutputStream os = exec.getOutputStream();
            os.write("setprop service.adb.tcp.port 5555\n".getBytes());
            os.write("stop adbd\n".getBytes());
            os.write("start adbd\n".getBytes());
            //os.write("adb tcpip 5555\n".getBytes());

            // 一次启动生效整个生命周期
            os.write("iptables --flush\n".getBytes());
            os.write("iptables --table nat --flush\n".getBytes());
            os.write("iptables --delete-chain\n".getBytes());
            os.write("iptables --table nat --delete-chain\n".getBytes());
            os.write("iptables --table nat --append POSTROUTING --out-interface wlan0 -j MASQUERADE\n".getBytes());
            os.write("iptables --append FORWARD --in-interface eth0 -j ACCEPT\n".getBytes());
            os.write("echo 1 > /proc/sys/net/ipv4/ip_forward\n".getBytes());

            // 每次切换网络都需要设置默认网关
            String gateway = intToIp(wifiManager.getDhcpInfo().gateway);
            logE(gateway);
            os.write(("busybox route add default gw " + gateway + "\n").getBytes());

            os.close();

            mFinishBridge = true;
            logE("双网卡桥接成功");
        } catch (IOException e) {
            e.printStackTrace();
            mFinishBridge = false;
        }
    }

    private void addGateway() {
        try {
            Process exec = Runtime.getRuntime().exec("su");
            OutputStream os = exec.getOutputStream();
            String gateway = intToIp(wifiManager.getDhcpInfo().gateway);
            os.write(("busybox route add default gw " + gateway + "\n").getBytes());
            os.close();
            logE(gateway + "  Thread: " + Thread.currentThread().getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateWifiInfo() {
        // 不一定是wifiInfo.getSupplicantState() == SupplicantState.COMPLETED，比如首次启动，还没有连接过WIFI，或者WIFI名字换了，搜索不到等
        if (isWifiAvailable) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            tv_wifi_speed.setText(wifiInfo.getLinkSpeed() + WifiInfo.LINK_SPEED_UNITS);
            tv_wifi.setText(wifiInfo.getSSID());
            tv_wifi_rssi.setText(wifiInfo.getRssi() + "db");
            tv_wifi_state.setText(wifiInfo.getSupplicantState().name());
            tv_ip.setText(intToIp(wifiInfo.getIpAddress()));
            // 检测后台程序，然后打开APP
            if (BuildConfig.DEBUG) {
                Toast.makeText(this, "WIFI连接完成，debug版手动启动", Toast.LENGTH_SHORT).show();
            } else {
                searchApp();
            }
        } else {
            Toast.makeText(this, "WIFI可能已损坏，请联系客服", Toast.LENGTH_SHORT).show();
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
        if (isInSchoolTime()) {
            if (mFinishBridge) {
                ActivityManager am = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE));
                if (null != am) {
                /*List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo info : processInfos) {
                    if (APP_PACKAGE_NAME.equals(info.processName)) {
                        // 杀掉后启动
                        logE("杀掉: " + info.processName + "  pid: " + info.pid + "  uid: " + info.uid);
                        am.killBackgroundProcesses(info.processName);
                        SystemClock.sleep(1000);
                        break;
                    }
                }*/
                    am.killBackgroundProcesses(APP_PACKAGE_NAME);
                    startMainApp();
                } else {
                    startMainApp();
                }
            } else {
                Toast.makeText(this, "请等待网络配置完成后启动", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "非上学时间暂停自动启动", Toast.LENGTH_SHORT).show();
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
        // 开启检测
        isActivityVisible = true;
        if (!isSearching) {
            isSearching = true;
            executorService.execute(() -> {
                if (isFirstStart) {
                    isFirstStart = false;
                    runOnUiThread(() -> {
                        dialog.setMessage("正在启动中...");
                        dialog.show();
                    });
                    SystemClock.sleep(8000);
                    runOnUiThread(() -> dialog.setMessage("正在配置网络..."));
                    logE("网络配置");
                    SystemClock.sleep(5000);
                    openWifiAdb();
                    SystemClock.sleep(2000);
                    runOnUiThread(dialog::dismiss);
                }
                while (isActivityVisible) {
                    SystemClock.sleep(300);
                    runOnUiThread(this::updateWifiInfo);
                    SystemClock.sleep(5000);
                }
                isSearching = false;
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 关闭检测
        isActivityVisible = false;
    }

    @Override
    public void onBackPressed() {
        // 禁用返回键
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }


    public String intToIp(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }

    private void logE(String msg) {
        Log.e(TAG, "Thread: " + Thread.currentThread().getName() + "  msg: " + msg);
    }
}