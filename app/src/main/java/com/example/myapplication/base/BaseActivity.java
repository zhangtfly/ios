package com.example.myapplication.base;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.login.action.TitleBarAction;
import com.example.myapplication.login.dialog.WaitDialog;
import com.gyf.immersionbar.ImmersionBar;
import com.hjq.bar.TitleBar;
import com.hjq.http.config.IRequestApi;
import com.hjq.http.listener.OnHttpListener;
import com.hjq.toast.Toaster;
import com.hjq.window.EasyWindowManager;

public class BaseActivity extends AppCompatActivity implements OnHttpListener<Object>, TitleBarAction {
    /**
     * 标题栏对象
     */
    private TitleBar mTitleBar;
    /**
     * 状态栏沉浸
     */
    private ImmersionBar mImmersionBar;
    /**
     * 对话框数量
     */
    private int dialogCount = 0;

    /**
     * 当前加载对话框是否在显示中
     */
    public boolean isShowDialog() {
        return EasyWindowManager.existWindowShowingByTag("WaitDialog");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * 重点修复：setContentView 之后统一初始化
     */
    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        initActivity(); // 放在这里 100% 生效
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        initActivity();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        initActivity();
    }

    /**
     * 统一初始化（关键修复）
     */
    protected void initActivity() {
        // 初始化标题栏
        if (getTitleBar() != null) {
            getTitleBar().setOnTitleBarListener(this);
        }

        // 初始化沉浸式状态栏
        if (isStatusBarEnabled()) {
            getStatusBarConfig().init();
            // 标题栏适配状态栏
            if (getTitleBar() != null) {
                ImmersionBar.setTitleBar(this, getTitleBar());
            }
        }
    }

    /**
     * 获取状态栏沉浸的配置对象
     */
    @NonNull
    public ImmersionBar getStatusBarConfig() {
        if (mImmersionBar == null) {
            mImmersionBar = createStatusBarConfig();
        }
        return mImmersionBar;
    }

    /**
     * 设置标题栏的标题
     */
    @Override
    public void setTitle(@StringRes int id) {
        setTitle(getString(id));
    }

    /**
     * 设置标题栏的标题
     */
    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        if (getTitleBar() != null) {
            getTitleBar().setTitle(title);
        }
    }

    @Override
    @Nullable
    public TitleBar getTitleBar() {
        if (mTitleBar == null) {
            mTitleBar = obtainTitleBar(getContentView());
        }
        return mTitleBar;
    }


    /**
     * 获取根布局
     */
    private ViewGroup getContentView() {
        return findViewById(Window.ID_ANDROID_CONTENT);
    }

    @Override
    public void onLeftClick(TitleBar titleBar) {
        onBackPressed();
    }

    /**
     * 初始化沉浸式状态栏
     */
    @NonNull
    protected ImmersionBar createStatusBarConfig() {
        return ImmersionBar.with(this)
                .statusBarDarkFont(isStatusBarDarkFont())   // 状态栏字体深色
                .navigationBarColor(R.color.white)          // 导航栏背景色
                .autoDarkModeEnable(true, 0.2f)             // 自动暗黑模式
                .fitsSystemWindows(false);                  // 修复关键配置
    }

    /**
     * 是否使用沉浸式状态栏
     */
    protected boolean isStatusBarEnabled() {
        return true;
    }

    /**
     * 状态栏字体深色模式
     */
    protected boolean isStatusBarDarkFont() {
        return true;
    }

    /**
     * 显示加载对话框
     */
    public void showDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        dialogCount++;
        new Handler().postDelayed(() -> {
            if (dialogCount <= 0 || isFinishing() || isDestroyed()) {
                return;
            }
            if (!isShowDialog()) {
                WaitDialog.showWaitDialog(BaseActivity.this, "加载中");
            }
        }, 300);
    }

    /**
     * 隐藏加载对话框
     */
    public void hideDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (dialogCount > 0) {
            dialogCount--;
        }
        if (dialogCount != 0 || !isShowDialog()) {
            return;
        }
        EasyWindowManager.cancelWindowByTag("WaitDialog");
    }

    @Override
    public void onHttpStart(@NonNull IRequestApi api) {
        showDialog();
    }

    @Override
    public void onHttpSuccess(@NonNull Object result) {
    }

    @Override
    public void onHttpFail(@NonNull Throwable throwable) {
        Toaster.show(throwable.getMessage());
    }

    @Override
    public void onHttpEnd(@NonNull IRequestApi api) {
        hideDialog();
    }
}