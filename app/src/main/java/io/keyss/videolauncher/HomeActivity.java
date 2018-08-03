package io.keyss.videolauncher;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import io.keyss.videolauncher.utils.ScreenTool;

/**
 * @author MrKey
 */
public class HomeActivity extends Activity {

    private Button start;
    private static final String mCheckStartString = "time=";
    private static final String mCheckEndString = " ms";
    private int mStartIndex;
    private Double mLastDelay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        //ScreenTool.hideNavbar(this);
        ScreenTool.showNavbar(this);
        start = findViewById(R.id.b_start);
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
            while ((line = bf.readLine()) != null) {
                mStartIndex = line.lastIndexOf(mCheckStartString);
                if (mStartIndex > 0) {
                    mLastDelay = Double.valueOf(line.substring(mStartIndex + mCheckStartString.length(), line.lastIndexOf(mCheckEndString)));
                    Log.e("print", "延时: " + mLastDelay);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
