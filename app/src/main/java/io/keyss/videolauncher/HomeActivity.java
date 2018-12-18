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
import android.os.Environment;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.keyss.videolauncher.utils.KeyDoubleClickCheckUtil;
import io.keyss.videolauncher.utils.ScreenTool;

/**
 * @author MrKey
 */
public class HomeActivity extends Activity {

    private String TAG;
    private final String APP_PACKAGE_NAME = "io.keyss.microbeanandroid";
    // 动态获取工作时间
    private Date START_TIME;
    private Date END_TIME;

    private Button start;
    private TextView tv_wifi;
    private TextView tv_work_time;
    private ProgressDialog dialog;

    private boolean isMainStart;
    private boolean isActivityVisible;
    private boolean isSearching;
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

    private void initTime() {
        // 先设置默认值，省的多种判断后添加
        setDefaultTime();

        logE("file: " + getWorkTimeFile().exists());

        if (getWorkTimeFile().exists()) {
            DateFormat workFormat = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss", Locale.SIMPLIFIED_CHINESE);
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.SIMPLIFIED_CHINESE).format(new Date());
            try {
                BufferedReader reader = new BufferedReader(new FileReader(getWorkTimeFile()));
                String time = reader.readLine();
                logE("读到的文本: " + time);
                reader.close();
                if (!TextUtils.isEmpty(time) && time.contains(":") && time.contains("-")) {
                    // 08:00:00-18:30:00
                    String[] split = time.split("-");
                    START_TIME = workFormat.parse(today + split[0]);
                    END_TIME = workFormat.parse(today + split[1]);
                }
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }
        DateFormat workFormat = new SimpleDateFormat("HH:mm", Locale.SIMPLIFIED_CHINESE);
        String workTimeText = "工作时间:  " + workFormat.format(START_TIME) + " - " + workFormat.format(END_TIME);
        logE(workTimeText);
        tv_work_time.setText(workTimeText);
    }

    private void setDefaultTime() {
        START_TIME = new Date();
        START_TIME.setHours(7);
        START_TIME.setMinutes(30);
        START_TIME.setSeconds(0);
        END_TIME = new Date();
        END_TIME.setHours(18);
        END_TIME.setMinutes(30);
        END_TIME.setSeconds(0);
    }

    private void initView() {
        setContentView(R.layout.activity_home);
        if (BuildConfig.DEBUG) {
            ScreenTool.showNavbar(this);
        } else {
            ScreenTool.hideNavbar(this);
        }
        start = findViewById(R.id.b_start);
        tv_wifi = findViewById(R.id.tv_wifi);
        tv_work_time = findViewById(R.id.tv_work_time);
        tv_wifi.setOnClickListener(v -> startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS)
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

        // 上方 1行顶9行示范
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
                        setWifiInfo();
                        /*
                        WIFI 连接的状态顺序
                        info.getDetailedState()
                        DISCONNECTED
                        OBTAINING_IPADDR
                        VERIFYING_POOR_LINK
                        CAPTIVE_PORTAL_CHECK
                        CONNECTED or FAILED

                        info.getState()
                        DISCONNECTED
                        CONNECTING
                        CONNECTED
                         */
                        /*NetworkInfo.DetailedState state = info.getDetailedState();
                        if (state == NetworkInfo.DetailedState.CONNECTING) {
                            //tv_wifi_state.setText("连接中...");
                        } else if (state == NetworkInfo.DetailedState.AUTHENTICATING) {
                            //tv_wifi_state.setText("正在验证身份信息...");
                        } else if (state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                            //tv_wifi_state.setText("正在获取IP地址...");
                        } else if (state == NetworkInfo.DetailedState.FAILED) {
                            //tv_wifi_state.setText("连接失败，点击任意地方关闭");
                        }*/

                        if (NetworkInfo.State.CONNECTED == info.getState()) {
                            executorService.execute(() -> addGateway());
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
                if (mFinishBridge) {
                    isMainStart = true;
                    // [Terminated, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 1]
                    //executorService.shutdownNow();
                    Intent startIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("video://start"));
                    try {
                        logE("启动APP: " + startIntent);
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
                        }
                        startActivity(startIntent);
                    } catch (Exception e) {
                        // ActivityNotFoundException
                        e.printStackTrace();
                        logE("error: " + e.getLocalizedMessage());
                    }
                    isMainStart = false;
                } else {
                    Toast.makeText(this, "请等待网络配置完成后启动", Toast.LENGTH_SHORT).show();
                }
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
            SystemClock.sleep(10);
            // su root 环境继续输入
            OutputStream os = exec.getOutputStream();
            os.write("setprop service.adb.tcp.port 5555\n".getBytes());
            SystemClock.sleep(10);
            os.write("stop adbd\n".getBytes());
            SystemClock.sleep(10);
            os.write("start adbd\n".getBytes());
            //os.write("adb tcpip 5555\n".getBytes());
            SystemClock.sleep(10);

            // 一次启动生效整个生命周期
            os.write("iptables --flush\n".getBytes());
            os.write("iptables --table nat --flush\n".getBytes());
            os.write("iptables --delete-chain\n".getBytes());
            os.write("iptables --table nat --delete-chain\n".getBytes());
            os.write("iptables --table nat --append POSTROUTING --out-interface wlan0 -j MASQUERADE\n".getBytes());
            os.write("iptables --append FORWARD --in-interface eth0 -j ACCEPT\n".getBytes());
            os.write("echo 1 > /proc/sys/net/ipv4/ip_forward\n".getBytes());
            SystemClock.sleep(10);

            // 设置网卡IP
            os.write("busybox ifconfig eth0 172.28.128.28 netmask 255.255.0.0\n".getBytes());
            SystemClock.sleep(10);

            // 每次切换网络都需要设置默认网关
            String gateway = intToIp(wifiManager.getDhcpInfo().gateway);
            os.write(("busybox route add default gw " + gateway + "\n").getBytes());

            SystemClock.sleep(100);
            // 退出
            os.write("exit\n".getBytes());
            os.close();
            exec.destroy();
            mFinishBridge = true;
            logE("设置查询到的网关: " + gateway + "  双网卡桥接成功");
        } catch (IOException e) {
            e.printStackTrace();
            mFinishBridge = false;
        }
    }

    private void addGateway() {
        try {
            Process exec = Runtime.getRuntime().exec("su");
            OutputStream os = exec.getOutputStream();
            // 设置网卡IP
            os.write("busybox ifconfig eth0 172.28.128.28 netmask 255.255.0.0\n".getBytes());
            SystemClock.sleep(10);
            String gateway = intToIp(wifiManager.getDhcpInfo().gateway);
            os.write(("busybox route add default gw " + gateway + "\n").getBytes());
            SystemClock.sleep(100);
            os.write("exit\n".getBytes());
            SystemClock.sleep(10);
            os.close();
            SystemClock.sleep(10);
            exec.destroy();
            mFinishBridge = true;
            logE("单独设置网关: " + gateway);
        } catch (IOException e) {
            e.printStackTrace();
            mFinishBridge = false;
        }
    }

    private void updateWifiInfo() {
        // 不一定是wifiInfo.getSupplicantState() == SupplicantState.COMPLETED，比如首次启动，还没有连接过WIFI，或者WIFI名字换了，搜索不到等
        if (isWifiAvailable) {
            setWifiInfo();
            // 检测后台程序，然后打开APP
            if (BuildConfig.DEBUG) {
                Toast.makeText(this, "WIFI已打开，debug版手动启动", Toast.LENGTH_SHORT).show();
            } else {
                autoStartApp();
            }
        } else {
            Toast.makeText(this, "WIFI可能已损坏，请联系客服", Toast.LENGTH_SHORT).show();
        }
    }

    private void setWifiInfo() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String text = wifiInfo.getSSID() + "\n"
                + wifiInfo.getLinkSpeed() + WifiInfo.LINK_SPEED_UNITS + "\n"
                + wifiInfo.getRssi() + "db\n"
                + wifiInfo.getSupplicantState().name() + "\n"
                + intToIp(wifiInfo.getIpAddress()) + "\n"
                + "Version: " + BuildConfig.VERSION_NAME;
        tv_wifi.setText(text);
    }

    private boolean isInSchoolTime() {
        Date now = new Date();
        return now.after(START_TIME) && now.before(END_TIME);
    }

    public void autoStartApp() {
        if (isInSchoolTime() || System.currentTimeMillis() < 1540000000000L) {
            startMainApp();
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
        initTime();
        if (!isSearching) {
            isSearching = true;
            executorService.execute(() -> {
                if (isFirstStart) {
                    isFirstStart = false;
                    runOnUiThread(() -> {
                        dialog.setMessage("正在启动中...");
                        dialog.show();
                    });
                    SystemClock.sleep(7000);
                    runOnUiThread(() -> dialog.setMessage("正在配置网络..."));
                    SystemClock.sleep(5000);
                    openWifiAdb();
                    SystemClock.sleep(1000);
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


    private String intToIp(int ipInt) {
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

    private File getWorkTimeFile() {
        return new File(getWriteFolder(), "WorkTime.txt");
    }

    private File getWriteFolder() {
        File file = new File(Environment.getExternalStorageDirectory(), "MicroBean");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }
}