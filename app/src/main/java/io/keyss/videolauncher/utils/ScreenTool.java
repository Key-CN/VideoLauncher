package io.keyss.videolauncher.utils;

import android.content.Context;
import android.content.Intent;

/**
 * Created by 钢铁侠 on 2018/6/7 15:08
 * description:
 */
public class ScreenTool {
    public static void hideNavbar(Context context) {
        //隐藏导航栏
        Intent intent1 = new Intent();
        intent1.setAction("ACTION_SHOW_NAVBAR");
        intent1.putExtra("cmd", "hide");
        context.sendBroadcast(intent1, null);
        //禁用状态栏下拉
        Intent intent2 = new Intent();
        intent2.setAction("ACTION_STATUSBAR_DROPDOWN");
        intent2.putExtra("cmd", "hide");
        context.sendBroadcast(intent2, null);
    }

    public static void showNavbar(Context context) {
        //显示导航栏
        Intent intent1 = new Intent();
        intent1.setAction("ACTION_SHOW_NAVBAR");
        intent1.putExtra("cmd", "show");
        context.sendBroadcast(intent1, null);
        //恢复状态栏下拉
        Intent intent2 = new Intent();
        intent2.setAction("ACTION_STATUSBAR_DROPDOWN");
        intent2.putExtra("cmd", "show");
        context.sendBroadcast(intent2, null);
    }
}
