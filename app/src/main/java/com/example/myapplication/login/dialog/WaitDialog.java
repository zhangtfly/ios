package com.example.myapplication.login.dialog;

import android.app.Activity;

import com.example.myapplication.R;
import com.hjq.window.EasyWindow;

public class WaitDialog {
    public static void showWaitDialog(Activity activity, CharSequence text) {
        EasyWindow.with(activity)
                .setContentView(R.layout.dialog_wait)
                .setWindowTag("WaitDialog")
                .setOutsideTouchable(false)
                .setTextByTextView(R.id.tv_wait_message, text)
                // 设置动画样式
                .setWindowAnim(android.R.style.Animation_Translucent)
                .show();
    }
}
