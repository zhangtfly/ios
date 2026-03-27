package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * 语音波形图自定义View
 * 显示实时音频振幅的波形效果，类似微信语音输入
 */
public class VoiceWaveformView extends View {
    private static final int MAX_BARS = 20; // 波形条数量
    private static final float MIN_BAR_HEIGHT_RATIO = 0.1f; // 最小条形高度比例
    private static final float MAX_BAR_HEIGHT_RATIO = 0.9f; // 最大条形高度比例
    
    private float[] amplitudes = new float[MAX_BARS];
    private Paint paint;
    private Paint cancelPaint; // 需求5: 取消提示的画笔
    private int currentIndex = 0;
    private boolean showCancelHint = false; // 需求5: 是否显示取消提示
    
    public VoiceWaveformView(Context context) {
        super(context);
        init();
    }
    
    public VoiceWaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public VoiceWaveformView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xFF2196F3); // 蓝色
        paint.setStrokeWidth(8);
        paint.setStrokeCap(Paint.Cap.ROUND);
        
        // 需求5: 初始化取消提示画笔
        cancelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cancelPaint.setColor(0xFFFF0000); // 红色
        cancelPaint.setStrokeWidth(8);
        cancelPaint.setStrokeCap(Paint.Cap.ROUND);
        
        // 初始化振幅数组
        for (int i = 0; i < MAX_BARS; i++) {
            amplitudes[i] = 0.2f; // 初始小幅度
        }
    }
    
    /**
     * 更新音频振幅（忽略振幅大小，只做流动动画）
     * @param amplitude 振幅值（不使用）
     */
    public void updateAmplitude(float amplitude) {
        // 忽略实际振幅，使用固定的流动效果
        // 不需要做任何处理，动画由 startAnimation 驱动
    }
    
    /**
     * 需求5: 设置取消提示是否可见
     * @param visible 是否显示取消提示
     */
    public void setCancelHintVisible(boolean visible) {
        if (showCancelHint != visible) {
            showCancelHint = visible;
            invalidate(); // 触发重绘
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        
        if (width == 0 || height == 0) {
            return;
        }
        
        int barWidth = width / MAX_BARS;
        int barSpacing = Math.max(2, barWidth / 4);
        int actualBarWidth = barWidth - barSpacing;
        
        // 需求5: 根据是否显示取消提示选择画笔颜色
        Paint currentPaint = showCancelHint ? cancelPaint : paint;
        
        for (int i = 0; i < MAX_BARS; i++) {
            float amplitude = amplitudes[i];
            
            // 计算条形高度（最小10%，最大90%）
            float barHeight = height * (MIN_BAR_HEIGHT_RATIO + amplitude * (MAX_BAR_HEIGHT_RATIO - MIN_BAR_HEIGHT_RATIO));
            
            // 计算X坐标
            float x = i * barWidth + barWidth / 2f;
            
            // 计算Y坐标（居中）
            float startY = (height - barHeight) / 2f;
            float stopY = startY + barHeight;
            
            // 设置不透明
            currentPaint.setAlpha(255);
            
            // 绘制条形
            canvas.drawLine(x, startY, x, stopY, currentPaint);
        }
    }
    
    /**
     * 重置波形图
     */
    public void reset() {
        for (int i = 0; i < MAX_BARS; i++) {
            amplitudes[i] = 0.2f;
        }
        currentIndex = 0;
        showCancelHint = false; // 需求5: 重置取消提示状态
        invalidate();
    }
    
    /**
     * 开始动画（即使没有音频输入也显示基础动画）
     */
    public void startAnimation() {
        post(animationRunnable);
    }
    
    /**
     * 停止动画
     */
    public void stopAnimation() {
        removeCallbacks(animationRunnable);
    }
    
    private final Runnable animationRunnable = new Runnable() {
        @Override
        public void run() {
            // 蓝色流动波浪效果 - 从左向右流动
            // 使用正弦波创建波浪效果
            long currentTime = System.currentTimeMillis();
            float phase = (currentTime % 2000) / 2000f * (float) Math.PI * 2; // 2秒一个周期
            
            for (int i = 0; i < MAX_BARS; i++) {
                // 创建波浪效果：使用正弦函数
                float waveOffset = (float) Math.sin(phase + i * 0.5f);
                // 振幅在 0.3 到 0.7 之间波动
                amplitudes[i] = 0.5f + waveOffset * 0.2f;
            }
            
            // 触发重绘
            invalidate();
            
            // 每50ms更新一次（20fps）
            postDelayed(this, 50);
        }
    };
}
