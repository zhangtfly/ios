package com.example.myapplication;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * 藏文语音合成管理器
 * 支持长文本自动分段合成，异步播放和接收，消除停顿
 */
public class TtsManager {
    private static final String TAG = "TtsManager";
    
    private static final String TTS_WS_URL = "ws://117.68.88.175:38086/tts/v1/streaming?app_id=VXBpX2tleT@ia2VSeHh4eHh";
    private static final int SAMPLE_RATE = 24000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int MAX_CHUNK_SIZE = 500;
    
    private Context context;
    private OkHttpClient client;
    private WebSocket currentWebSocket;  // 当前WebSocket连接
    private AudioTrack audioTrack;
    private TtsCallback callback;
    
    private boolean isPlaying = false;
    private boolean isStopped = false;
    private List<String> textChunks;
    private int currentChunkIndex = 0;
    private boolean isFirstChunk = true;
    
    // 音频缓冲队列，用于异步播放
    private java.util.concurrent.BlockingQueue<byte[]> audioQueue;
    private Thread playbackThread;
    private Thread synthesisThread;  // 合成线程，顺序处理大段
    private int completedChunks = 0;  // 已完成合成的段数
    private final Object chunkLock = new Object();  // 用于同步大段完成
    
    public interface TtsCallback {
        void onStart();
        void onComplete();
        void onError(String error);
        void onProgress(int current, int total);
    }
    
    public TtsManager(Context context) {
        this.context = context;
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        audioQueue = new java.util.concurrent.LinkedBlockingQueue<>();
    }
    
    public void setCallback(TtsCallback callback) {
        this.callback = callback;
    }
    
    public void synthesize(String text) {
        if (text == null || text.trim().isEmpty()) {
            if (callback != null) {
                callback.onError("文本为空");
            }
            return;
        }
        
        text = text.replaceAll("\\s+", " ").trim();
        textChunks = splitText(text);
        currentChunkIndex = 0;
        completedChunks = 0;
        isFirstChunk = true;
        isStopped = false;
        
        Log.d(TAG, "文本总长度: " + text.length() + " 字符，分为 " + textChunks.size() + " 段");
        
        // 清空音频队列
        audioQueue.clear();
        
        // 初始化音频播放
        initAudioTrack();
        startPlaybackThread();
        
        // 启动合成线程，顺序处理每个大段
        startSynthesisThread();
    }
    
    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        
        if (text.length() <= MAX_CHUNK_SIZE) {
            chunks.add(text);
            return chunks;
        }
        
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_CHUNK_SIZE, text.length());
            
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf('།', end);
                if (lastPeriod > start && lastPeriod < end) {
                    end = lastPeriod + 1;
                }
            }
            
            chunks.add(text.substring(start, end).trim());
            start = end;
        }
        
        return chunks;
    }
    
    /**
     * 启动合成线程，顺序处理每个大段
     */
    private void startSynthesisThread() {
        if (synthesisThread != null && synthesisThread.isAlive()) {
            return;
        }
        
        synthesisThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "合成线程启动");
                for (int i = 0; i < textChunks.size() && !isStopped; i++) {
                    synthesizeChunkSync(textChunks.get(i), i);
                }
            }
        });
        synthesisThread.start();
    }
    
    /**
     * 同步合成单个大段（等待该段完成后再处理下一段）
     */
    private void synthesizeChunkSync(final String text, final int chunkIndex) {
        if (isStopped) {
            return;
        }
        
        Log.d(TAG, "开始合成第 " + (chunkIndex + 1) + "/" + textChunks.size() + " 段");
        
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final boolean[] hasReceivedFirstAudio = {false};  // 标记是否收到第一个音频片段
        
        Request request = new Request.Builder().url(TTS_WS_URL).build();
        
        currentWebSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket连接成功 - 段 " + (chunkIndex + 1));
                sendSynthesisRequest(webSocket, text);
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JSONObject json = new JSONObject(text);
                    int code = json.getInt("code");
                    
                    if (code == 101 || (code == 10000 && !json.has("result"))) {
                        Log.d(TAG, "握手成功 - 段 " + (chunkIndex + 1));
                        if (isFirstChunk && callback != null) {
                            callback.onStart();
                            isFirstChunk = false;
                        }
                        isPlaying = true;
                    } else if (code == 10000 && json.has("result")) {
                        JSONObject result = json.getJSONObject("result");
                        String audioBase64 = result.getString("audio");
                        boolean isEnd = result.getBoolean("is_end");
                        
                        byte[] audioData = Base64.decode(audioBase64, Base64.DEFAULT);
                        
                        // 将小段音频数据放入队列，流式播放
                        try {
                            audioQueue.put(audioData);
                            Log.d(TAG, "段 " + (chunkIndex + 1) + " 收到小段音频: " + audioData.length + " 字节，队列大小: " + audioQueue.size());
                            
                            // 标记已收到第一个音频片段
                            if (!hasReceivedFirstAudio[0]) {
                                hasReceivedFirstAudio[0] = true;
                            }
                        } catch (InterruptedException e) {
                            Log.e(TAG, "音频数据入队失败", e);
                        }
                        
                        if (isEnd) {
                            Log.d(TAG, "段 " + (chunkIndex + 1) + " 合成完成");
                            onChunkComplete(chunkIndex);
                            latch.countDown();  // 释放锁，允许处理下一个大段
                        }
                    } else {
                        String message = json.getString("message");
                        Log.e(TAG, "合成错误: " + message);
                        if (callback != null) {
                            callback.onError(message);
                        }
                        latch.countDown();
                        cleanup();
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "解析响应失败", e);
                    latch.countDown();
                }
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                // 如果是EOFException且正在播放，可能是正常的连接关闭
                if (t instanceof java.io.EOFException && isPlaying) {
                    Log.d(TAG, "WebSocket连接已关闭（可能是正常关闭） - 段 " + (chunkIndex + 1));
                    latch.countDown();
                    return;
                }
                
                if (isStopped) {
                    Log.d(TAG, "WebSocket正常关闭 - 段 " + (chunkIndex + 1));
                    latch.countDown();
                    return;
                }
                
                Log.e(TAG, "WebSocket连接失败 - 段 " + (chunkIndex + 1), t);
                if (callback != null && !isStopped) {
                    // 隐藏IP端口信息，只显示通用错误
                    String errorMsg = "语音服务连接失败，请检查网络";
                    if (t instanceof java.net.UnknownHostException) {
                        errorMsg = "网络连接失败，请检查网络设置";
                    } else if (t instanceof java.net.SocketTimeoutException) {
                        errorMsg = "网络连接超时，请稍后重试";
                    } else if (t instanceof java.io.IOException) {
                        errorMsg = "网络异常，请检查网络连接";
                    }
                    callback.onError(errorMsg);
                }
                latch.countDown();
            }
            
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket关闭 - 段 " + (chunkIndex + 1) + ": " + reason);
                latch.countDown();
            }
        });
        
        // 等待当前大段完成
        try {
            latch.await();
            
            // 如果不是最后一段，给一点时间让队列中的数据被消费
            // 避免下一段的数据立即填满队列导致阻塞
            if (chunkIndex < textChunks.size() - 1 && audioQueue.size() > 5) {
                Log.d(TAG, "段 " + (chunkIndex + 1) + " 完成，等待队列消费，当前队列大小: " + audioQueue.size());
                Thread.sleep(200);  // 短暂等待，让播放线程消费一些数据
            }
            
            Log.d(TAG, "段 " + (chunkIndex + 1) + " 处理完成，继续下一段");
        } catch (InterruptedException e) {
            Log.e(TAG, "等待段完成时被中断", e);
        }
    }
    
    /**
     * 启动播放线程，从队列中取出音频数据并播放
     */
    private void startPlaybackThread() {
        if (playbackThread != null && playbackThread.isAlive()) {
            return;
        }
        
        playbackThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "播放线程启动");
                try {
                    while (!isStopped) {
                        // 检查是否所有段都已完成且队列为空
                        if (completedChunks >= textChunks.size() && audioQueue.isEmpty()) {
                            Log.d(TAG, "所有音频数据播放完成");
                            break;
                        }
                        
                        // 使用阻塞式take()，避免轮询等待
                        // 如果队列空，会一直等待直到有数据或被中断
                        byte[] audioData = null;
                        try {
                            // 使用poll带超时，但超时时间很短，只是为了能检查停止标志
                            audioData = audioQueue.poll(50, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            if (isStopped) {
                                break;
                            }
                            continue;
                        }
                        
                        if (audioData != null) {
                            // 播放音频
                            if (audioTrack != null && !isStopped) {
                                int written = audioTrack.write(audioData, 0, audioData.length);
                                if (written < 0) {
                                    Log.e(TAG, "AudioTrack写入失败: " + written);
                                }
                            }
                        }
                    }
                } finally {
                    finishPlayback();
                }
            }
        });
        playbackThread.start();
    }
    
    private void onChunkComplete(int chunkIndex) {
        synchronized (this) {
            completedChunks++;
            Log.d(TAG, "已完成 " + completedChunks + "/" + textChunks.size() + " 段");
            
            if (callback != null) {
                callback.onProgress(completedChunks, textChunks.size());
            }
        }
    }
    
    private void sendSynthesisRequest(WebSocket webSocket, String text) {
        try {
            // 应用文本预处理
            String processedText = prepareTextForTTS(text);
            
            JSONObject request = new JSONObject();
            request.put("req_id", UUID.randomUUID().toString());
            request.put("text", processedText);
            
            String requestStr = request.toString();
            Log.d(TAG, "发送请求，原始文本长度: " + text.length() + "，处理后长度: " + processedText.length());
            webSocket.send(requestStr);
        } catch (JSONException e) {
            Log.e(TAG, "构建请求失败", e);
            if (callback != null) {
                callback.onError("构建请求失败");
            }
            cleanup();
        }
    }
    
    /**
     * 空白归一化：把多个空格/Tab/全角空格压缩为1个普通空格（不影响换行）
     */
    private String normalizeSpaces(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // 全角空格和不间断空格转为普通空格
        text = text.replace('\u3000', ' ').replace('\u00A0', ' ');
        // 多个空白字符压缩为一个空格
        text = text.replaceAll("[ \\t\\f\\u000B]+", " ");
        return text;
    }
    
    /**
     * 数字后漏读修复：给"数字/序号 + ."与后续藏文之间补一个空格
     */
    private String normalizeNumberBoundary(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String s = normalizeSpaces(text);
        // 统一换行，避免 \r\n 在切分/计数中表现不一致
        s = s.replace("\r\n", "\n").replace("\r", "\n");
        
        // 关键：把"数字/序号"与后续藏文之间强制打断，避免服务端把序号当成列表前缀时误吞掉后续首字
        // 用"་"(tsheg) 作为可见分隔符（不计字数，且更符合藏文书写习惯）
        // 例：༡.ཞ -> ༡.་ཞ； 12ཚ -> 12་ཚ
        s = s.replaceAll("([༠-༩]+)([.．])(?=[\u0F40-\u0FBC])", "$1$2་");
        s = s.replaceAll("([0-9]+)([.．])(?=[\u0F40-\u0FBC])", "$1$2་");
        s = s.replaceAll("([༠-༩]+)(?=[\u0F40-\u0FBC])", "$1་");
        s = s.replaceAll("([0-9]+)(?=[\u0F40-\u0FBC])", "$1་");
        
        return s;
    }
    
    /**
     * 发送前文本保护：避免段首"数字." 被当作列表前缀清洗，从而漏读数字
     */
    private String prepareTextForTTS(String segText) {
        if (segText == null || segText.isEmpty()) {
            return segText;
        }
        
        String s = normalizeNumberBoundary(segText);
        // 去掉段首空白（含缩进/全角空格/换行），避免后端用 ^\s*数字\. 匹配到"列表序号前缀"
        s = s.replaceAll("^[\\s\\u3000\\u00A0]+", "");
        
        // 关键：把"段首 数字 + 点号"打断成"数字 + tsheg + 点号"，避免命中常见列表序号正则
        // 例：༣.ཞ -> ༣་.་ཞ
        s = s.replaceAll("^([༠-༩]+)([.．])", "$1་$2");
        s = s.replaceAll("^([0-9]+)([.．])", "$1་$2");
        
        return s;
    }
    
    private void initAudioTrack() {
        if (audioTrack != null) {
            return;
        }
        
        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (bufferSize < 0) {
            Log.e(TAG, "无法获取AudioTrack缓冲区大小");
            return;
        }
        
        // 使用更大的缓冲区，避免underrun
        bufferSize = bufferSize * 4;
        
        try {
            android.media.AudioAttributes audioAttributes = new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            
            android.media.AudioFormat audioFormat = new android.media.AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AUDIO_FORMAT)
                    .setChannelMask(CHANNEL_CONFIG)
                    .build();
            
            audioTrack = new AudioTrack(
                    audioAttributes,
                    audioFormat,
                    bufferSize,
                    AudioTrack.MODE_STREAM,
                    android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
            );
            
            if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                Log.e(TAG, "AudioTrack初始化失败");
                audioTrack = null;
                return;
            }
            
            audioTrack.play();
            Log.d(TAG, "AudioTrack初始化成功，缓冲区大小: " + bufferSize);
        } catch (Exception e) {
            Log.e(TAG, "创建AudioTrack失败", e);
            audioTrack = null;
        }
    }
    

    
    private void finishPlayback() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 等待AudioTrack播放完缓冲区中的数据
                    Thread.sleep(500);
                    
                    if (callback != null && !isStopped) {
                        callback.onComplete();
                    }
                    
                    cleanup();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    public void stop() {
        isStopped = true;
        isPlaying = false;
        
        // 中断合成线程
        if (synthesisThread != null && synthesisThread.isAlive()) {
            synthesisThread.interrupt();
        }
        
        // 中断播放线程
        if (playbackThread != null && playbackThread.isAlive()) {
            playbackThread.interrupt();
        }
        
        cleanup();
    }
    
    private void cleanup() {
        // 关闭当前WebSocket连接
        if (currentWebSocket != null) {
            try {
                currentWebSocket.close(1000, "正常关闭");
            } catch (Exception e) {
                Log.e(TAG, "关闭WebSocket失败", e);
            }
            currentWebSocket = null;
        }
        
        // 清空音频队列
        audioQueue.clear();
        
        if (audioTrack != null) {
            try {
                audioTrack.stop();
                audioTrack.release();
            } catch (Exception e) {
                Log.e(TAG, "释放AudioTrack失败", e);
            }
            audioTrack = null;
        }
        
        isPlaying = false;
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public void release() {
        stop();
        client.dispatcher().executorService().shutdown();
    }
}
