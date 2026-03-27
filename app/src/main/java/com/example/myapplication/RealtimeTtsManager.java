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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * 实时TTS管理器 - 边生成边播放
 * 模拟Web端逻辑：LLM流式输出 → 实时分段 → 立即合成 → 边播边放
 */
public class RealtimeTtsManager {
    private static final String TAG = "RealtimeTtsManager";
    
    private static final String TTS_WS_URL = "ws://117.68.88.175:38086/tts/v1/streaming?app_id=VXBpX2tleT@ia2VSeHh4eHh";
    private static final int SAMPLE_RATE = 24000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    // 分段参数 - 优化为更激进的分段策略
    private static final int TARGET_CHUNK_SIZE = 30;  // 目标分段长度（藏文字）- 减小到10
    private static final int MIN_CHUNK_SIZE = 10;      // 最小分段长度 - 减小到3
    private static final int MAX_CHUNK_SIZE = 40;     // 最大分段长度 - 减小到15
    
    private Context context;
    private OkHttpClient client;
    private WebSocket currentWebSocket;
    private AudioTrack audioTrack;
    private RealtimeTtsCallback callback;
    
    // 状态控制
    private boolean isActive = false;
    private boolean isStopped = false;
    private boolean llmDone = false;
    private volatile boolean isActuallyPlaying = false; // 新增：真正的播放状态标志
    
    // 文本缓冲和分段队列
    private StringBuilder textBuffer = new StringBuilder();
    private BlockingQueue<String> segmentQueue = new LinkedBlockingQueue<>();
    
    // 音频播放队列
    private BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
    private Thread playbackThread;
    private Thread synthesisThread;
    
    // 并发合成支持 - 允许多个段落同时合成
    private static final int MAX_CONCURRENT_SYNTHESIS = 3;  // 最大并发合成数
    private volatile int activeSynthesisCount = 0;  // 当前活跃的合成任务数
    private final Object synthesisLock = new Object();  // 合成计数锁
    
    // 段落序号管理
    private volatile int nextSegmentIndex = 0;  // 下一个要合成的段落序号
    private volatile int nextPlayIndex = 0;     // 下一个要播放的段落序号
    private final Map<Integer, List<byte[]>> segmentAudioMap = new ConcurrentHashMap<>();  // 段落音频缓存
    
    public interface RealtimeTtsCallback {
        void onStart();
        void onSegmentSynthesized(int segmentIndex, int totalSegments);
        void onComplete();
        void onError(String error);
    }
    
    public RealtimeTtsManager(Context context) {
        this.context = context;
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }
    
    public void setCallback(RealtimeTtsCallback callback) {
        this.callback = callback;
    }
    
    /**
     * 开始实时TTS会话
     */
    public void startSession() {
        if (isActive) {
            Log.w(TAG, "会话已经在运行中");
            return;
        }
        
        reset();
        isActive = true;
        llmDone = false;
        isStopped = false;
        isActuallyPlaying = false; // 重置播放状态
        
        initAudioTrack();
        startPlaybackThread();
        startSynthesisThread();
        
        if (callback != null) {
            callback.onStart();
        }
        
        Log.d(TAG, "实时TTS会话已开始");
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
    
    /**
     * 推送LLM增量文本（每次LLM返回一小段就调用）
     */
    public void pushLLMDelta(String deltaText) {
        if (!isActive) {
            Log.w(TAG, "会话未启动，自动启动");
            startSession();
        }
        
        if (deltaText == null || deltaText.isEmpty()) {
            return;
        }
        
        synchronized (textBuffer) {
            textBuffer.append(deltaText);
        }
        
        // 尝试从buffer中切出段落
        tryExtractSegments(false);
    }
    
    /**
     * 结束LLM输出（LLM生成完成时调用）
     */
    public void finishLLM() {
        if (!isActive) {
            return;
        }
        
        llmDone = true;
        Log.d(TAG, "LLM输出结束，处理剩余文本");
        
        // 把buffer中剩余的文本全部切出
        tryExtractSegments(true);
    }
    
    /**
     * 尝试从buffer中提取段落
     */
    private void tryExtractSegments(boolean isFinal) {
        while (true) {
            String segment = extractOneSegment(isFinal);
            if (segment == null) {
                break;
            }
            
            try {
                segmentQueue.put(segment);
                Log.d(TAG, "段落入队，长度: " + countTibetanUnits(segment) + " 字，队列大小: " + segmentQueue.size());
            } catch (InterruptedException e) {
                Log.e(TAG, "段落入队失败", e);
                break;
            }
        }
    }
    
    /**
     * 从buffer中提取一个段落
     */
    private String extractOneSegment(boolean isFinal) {
        synchronized (textBuffer) {
            String text = textBuffer.toString().trim();
            if (text.isEmpty()) {
                return null;
            }
            
            int units = countTibetanUnits(text);
            
            // 如果是最后一次，直接返回所有剩余文本
            if (isFinal) {
                textBuffer.setLength(0);
                return text;
            }
            
            // 文本太短，继续等待
            if (units < MIN_CHUNK_SIZE) {
                return null;
            }
            
            // 寻找合适的切分点
            int cutIndex = findCutPoint(text, units);
            if (cutIndex <= 0) {
                return null;
            }
            
            String segment = text.substring(0, cutIndex).trim();
            textBuffer.delete(0, cutIndex);
            
            return segment.isEmpty() ? null : segment;
        }
    }
    
    /**
     * 寻找合适的切分点
     */
    private int findCutPoint(String text, int totalUnits) {
        if (totalUnits < MIN_CHUNK_SIZE) {
            return -1;
        }
        
        int currentUnits = 0;
        int bestBeforeIdx = -1;
        int firstAfterIdx = -1;
        int lastFallbackIdx = -1;
        
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            
            if (shouldCountAsUnit(ch, i > 0 ? text.charAt(i - 1) : ' ')) {
                currentUnits++;
            }
            
            // 记录备用切分点（逗号、空格等）
            if (currentUnits >= MIN_CHUNK_SIZE && isFallbackBreak(ch)) {
                lastFallbackIdx = i + 1;
            }
            
            // 优先在藏文句末"།"切分
            if (currentUnits >= MIN_CHUNK_SIZE && ch == '།') {
                if (currentUnits <= TARGET_CHUNK_SIZE) {
                    bestBeforeIdx = i + 1;
                } else if (firstAfterIdx == -1) {
                    firstAfterIdx = i + 1;
                    break;
                }
            }
            
            // 超过最大长度，必须切分
            if (currentUnits >= MAX_CHUNK_SIZE) {
                break;
            }
        }
        
        // 选择最佳切分点
        if (bestBeforeIdx > 0 || firstAfterIdx > 0) {
            if (bestBeforeIdx > 0 && firstAfterIdx > 0) {
                int d1 = Math.abs(countTibetanUnits(text.substring(0, bestBeforeIdx)) - TARGET_CHUNK_SIZE);
                int d2 = Math.abs(countTibetanUnits(text.substring(0, firstAfterIdx)) - TARGET_CHUNK_SIZE);
                return d2 < d1 ? firstAfterIdx : bestBeforeIdx;
            }
            return firstAfterIdx > 0 ? firstAfterIdx : bestBeforeIdx;
        }
        
        // 如果超过最大长度但没找到句末，使用备用切分点
        if (currentUnits >= MAX_CHUNK_SIZE && lastFallbackIdx > 0) {
            return lastFallbackIdx;
        }
        
        return -1;
    }
    
    /**
     * 判断字符是否应该计数
     */
    private boolean shouldCountAsUnit(char ch, char prevChar) {
        // 汉字
        if (ch >= 0x4E00 && ch <= 0x9FFF) {
            return true;
        }
        
        // 藏文分隔符
        if (isTibetanDelimiter(ch)) {
            return false;
        }
        
        // 藏文字符（连续的藏文字符只计数一次）
        if (isTibetanChar(ch)) {
            return !isTibetanChar(prevChar);
        }
        
        // 其他非空白字符
        return !Character.isWhitespace(ch);
    }
    
    private boolean isTibetanChar(char ch) {
        return ch >= 0x0F00 && ch <= 0x0FFF;
    }
    
    private boolean isTibetanDelimiter(char ch) {
        return Character.isWhitespace(ch) || ch == '་' || ch == '།' || ch == '༎' || ch == '༏' || ch == '༐' || ch == '༑' || ch == '༔';
    }
    
    private boolean isFallbackBreak(char ch) {
        return Character.isWhitespace(ch) || ch == '，' || ch == ',' || ch == '。' || ch == '.' || 
               ch == '！' || ch == '!' || ch == '？' || ch == '?' || ch == '；' || ch == ';' ||
               ch == '：' || ch == ':' || ch == '、' || isTibetanDelimiter(ch);
    }
    
    /**
     * 计算藏文单位数
     */
    private int countTibetanUnits(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        boolean inTibetanToken = false;
        
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            
            // 汉字
            if (ch >= 0x4E00 && ch <= 0x9FFF) {
                count++;
                inTibetanToken = false;
                continue;
            }
            
            // 藏文分隔符
            if (isTibetanDelimiter(ch)) {
                inTibetanToken = false;
                continue;
            }
            
            // 藏文字符
            if (isTibetanChar(ch)) {
                if (!inTibetanToken) {
                    count++;
                    inTibetanToken = true;
                }
                continue;
            }
            
            // 其他非空白字符
            if (!Character.isWhitespace(ch)) {
                count++;
            }
            inTibetanToken = false;
        }
        
        return count;
    }

    
    /**
     * 合成线程 - 从队列中取段落并发送TTS请求（支持并发）
     */
    private void startSynthesisThread() {
        if (synthesisThread != null && synthesisThread.isAlive()) {
            return;
        }
        
        synthesisThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "合成线程启动");
                
                while (!isStopped && !Thread.currentThread().isInterrupted()) {
                    // 检查是否结束
                    if (llmDone && segmentQueue.isEmpty()) {
                        synchronized (synthesisLock) {
                            if (activeSynthesisCount == 0) {
                                Log.d(TAG, "所有段落已合成完成");
                                break;
                            }
                        }
                    }
                    
                    // 检查并发数限制
                    boolean canStartNew = false;
                    synchronized (synthesisLock) {
                        canStartNew = activeSynthesisCount < MAX_CONCURRENT_SYNTHESIS;
                    }
                    
                    if (!canStartNew) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Log.d(TAG, "合成线程被中断");
                            break;
                        }
                        continue;
                    }
                    
                    // 再次检查停止标志
                    if (isStopped || Thread.currentThread().isInterrupted()) {
                        Log.d(TAG, "合成线程收到停止信号");
                        break;
                    }
                    
                    // 从队列取段落
                    String segment = null;
                    try {
                        segment = segmentQueue.poll(100, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "合成线程被中断");
                        break;
                    }
                    
                    if (segment != null && !isStopped) {
                        final int segmentIndex = nextSegmentIndex++;
                        final String segmentText = segment;
                        
                        // 增加活跃合成计数
                        synchronized (synthesisLock) {
                            activeSynthesisCount++;
                        }
                        
                        // 异步合成段落
                        new Thread(() -> synthesizeSegmentAsync(segmentText, segmentIndex)).start();
                    }
                }
                
                // 等待所有合成完成
                while (true) {
                    synchronized (synthesisLock) {
                        if (activeSynthesisCount == 0) {
                            break;
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                
                if (callback != null && !isStopped) {
                    callback.onComplete();
                }
                
                isActive = false;
                Log.d(TAG, "合成线程结束");
            }
        });
        synthesisThread.start();
    }
    
    /**
     * 异步合成单个段落
     */
    private void synthesizeSegmentAsync(final String text, final int segmentIndex) {
        Log.d(TAG, "开始异步合成段落 " + segmentIndex + "，长度: " + countTibetanUnits(text) + " 字");
        
        // 初始化该段落的音频缓存
        segmentAudioMap.put(segmentIndex, new ArrayList<>());
        
        Request request = new Request.Builder().url(TTS_WS_URL).build();
        
        WebSocket webSocket = client.newWebSocket(request, new WebSocketListener() {
            private boolean isEnd = false;
            
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket连接成功 - 段落 " + segmentIndex);
                sendSynthesisRequest(webSocket, text);
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JSONObject json = new JSONObject(text);
                    int code = json.getInt("code");
                    
                    if (code == 101 || (code == 10000 && !json.has("result"))) {
                        Log.d(TAG, "握手成功 - 段落 " + segmentIndex);
                    } else if (code == 10000 && json.has("result")) {
                        JSONObject result = json.getJSONObject("result");
                        String audioBase64 = result.getString("audio");
                        isEnd = result.getBoolean("is_end");
                        
                        byte[] audioData = Base64.decode(audioBase64, Base64.DEFAULT);
                        
                        if (audioData.length > 0 && !isStopped) {
                            // 将音频数据添加到该段落的缓存中
                            List<byte[]> segmentAudio = segmentAudioMap.get(segmentIndex);
                            if (segmentAudio != null) {
                                segmentAudio.add(audioData);
                                Log.d(TAG, "段落 " + segmentIndex + " 音频片段缓存: " + audioData.length + " 字节");
                            }
                        }
                        
                        if (isEnd) {
                            Log.d(TAG, "段落 " + segmentIndex + " 合成完成");
                            
                            // 尝试将完成的段落音频加入播放队列
                            tryEnqueueCompletedSegments();
                            
                            if (callback != null) {
                                callback.onSegmentSynthesized(segmentIndex, segmentQueue.size() + 1);
                            }
                            
                            // 减少活跃合成计数
                            synchronized (synthesisLock) {
                                activeSynthesisCount--;
                            }
                            
                            webSocket.close(1000, "正常关闭");
                        }
                    } else {
                        String message = json.getString("message");
                        Log.e(TAG, "合成错误: " + message);
                        if (callback != null) {
                            callback.onError(message);
                        }
                        
                        // 减少活跃合成计数
                        synchronized (synthesisLock) {
                            activeSynthesisCount--;
                        }
                        webSocket.close(1000, "错误关闭");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "解析响应失败", e);
                    // 减少活跃合成计数
                    synchronized (synthesisLock) {
                        activeSynthesisCount--;
                    }
                }
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                if (isStopped) {
                    Log.d(TAG, "WebSocket正常关闭 - 段落 " + segmentIndex);
                    return;
                }
                
                Log.e(TAG, "WebSocket连接失败 - 段落 " + segmentIndex, t);
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
                
                // 减少活跃合成计数
                synchronized (synthesisLock) {
                    activeSynthesisCount--;
                }
            }
            
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket关闭 - 段落 " + segmentIndex);
                // 减少活跃合成计数
                synchronized (synthesisLock) {
                    activeSynthesisCount--;
                }
            }
        });
    }
    
    /**
     * 尝试将已完成的段落音频按顺序加入播放队列
     */
    private void tryEnqueueCompletedSegments() {
        while (true) {
            List<byte[]> segmentAudio = segmentAudioMap.get(nextPlayIndex);
            if (segmentAudio == null || segmentAudio.isEmpty()) {
                // 下一个要播放的段落还没准备好
                break;
            }
            
            // 将该段落的所有音频片段加入播放队列
            for (byte[] audioData : segmentAudio) {
                try {
                    audioQueue.put(audioData);
                    Log.d(TAG, "段落 " + nextPlayIndex + " 音频入队: " + audioData.length + " 字节");
                } catch (InterruptedException e) {
                    Log.e(TAG, "音频数据入队失败", e);
                    return;
                }
            }
            
            // 清理已播放的段落缓存
            segmentAudioMap.remove(nextPlayIndex);
            nextPlayIndex++;
        }
    }
    
    /**
     * 发送合成请求
     */
    private void sendSynthesisRequest(WebSocket webSocket, String text) {
        try {
            // 应用文本预处理
            String processedText = prepareTextForTTS(text);
            
            JSONObject request = new JSONObject();
            request.put("req_id", UUID.randomUUID().toString());
            request.put("text", processedText);
            
            webSocket.send(request.toString());
            Log.d(TAG, "发送TTS请求，原始文本长度: " + text.length() + "，处理后长度: " + processedText.length());
        } catch (JSONException e) {
            Log.e(TAG, "构建请求失败", e);
            if (callback != null) {
                callback.onError("构建请求失败");
            }
        }
    }
    
    /**
     * 播放线程 - 从队列中取音频数据并播放（优化缓冲）
     */
    private void startPlaybackThread() {
        if (playbackThread != null && playbackThread.isAlive()) {
            return;
        }
        
        playbackThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "播放线程启动");
                
                while (!isStopped && !Thread.currentThread().isInterrupted()) {
                    // 检查是否结束
                    boolean allDone = false;
                    synchronized (synthesisLock) {
                        allDone = llmDone && segmentQueue.isEmpty() && 
                                 activeSynthesisCount == 0 && audioQueue.isEmpty();
                    }
                    
                    if (allDone) {
                        Log.d(TAG, "所有音频播放完成");
                        isActuallyPlaying = false; // 播放完成，重置标志
                        Log.d(TAG, "❌ 播放完成，isActuallyPlaying = false");
                        break;
                    }
                    
                    // 从队列取音频数据（减少等待时间）
                    byte[] audioData = null;
                    try {
                        audioData = audioQueue.poll(30, TimeUnit.MILLISECONDS);  // 减少到30ms
                    } catch (InterruptedException e) {
                        Log.d(TAG, "播放线程被中断");
                        isActuallyPlaying = false; // 中断时重置标志
                        break;
                    }
                    
                    // 再次检查停止标志
                    if (isStopped || Thread.currentThread().isInterrupted()) {
                        Log.d(TAG, "播放线程收到停止信号");
                        isActuallyPlaying = false; // 停止时重置标志
                        break;
                    }
                    
                    if (audioData != null && audioTrack != null) {
                        // 有音频数据，设置播放标志
                        if (!isActuallyPlaying) {
                            isActuallyPlaying = true;
                            Log.d(TAG, "✅ 开始播放音频，isActuallyPlaying = true");
                        }
                        
                        int written = audioTrack.write(audioData, 0, audioData.length);
                        if (written < 0) {
                            Log.e(TAG, "AudioTrack写入失败: " + written);
                        }
                    } else {
                        // 没有音频数据，检查是否应该重置播放标志
                        // 如果队列为空且没有正在合成的段落，说明暂时没有音频
                        synchronized (synthesisLock) {
                            if (audioQueue.isEmpty() && activeSynthesisCount == 0 && isActuallyPlaying) {
                                isActuallyPlaying = false;
                                Log.d(TAG, "⏸️ 暂无音频数据，isActuallyPlaying = false");
                            }
                        }
                    }
                }
                
                isActuallyPlaying = false; // 线程结束，重置标志
                Log.d(TAG, "播放线程结束");
            }
        });
        playbackThread.start();
    }
    
    /**
     * 初始化AudioTrack（优化缓冲区大小）
     */
    private void initAudioTrack() {
        if (audioTrack != null) {
            return;
        }
        
        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (bufferSize < 0) {
            Log.e(TAG, "无法获取AudioTrack缓冲区大小");
            return;
        }
        
        // 使用更小的缓冲区减少延迟（从4倍改为2倍）
        bufferSize = bufferSize * 2;
        
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
    
    /**
     * 停止
     */
    public void stop() {
        Log.d(TAG, "停止实时TTS");
        
        // 立即设置停止标志
        isStopped = true;
        isActive = false;
        llmDone = true;
        isActuallyPlaying = false; // 重置播放标志
        
        // 重置计数器
        synchronized (synthesisLock) {
            activeSynthesisCount = 0;
        }
        
        // 立即停止AudioTrack播放
        if (audioTrack != null) {
            try {
                audioTrack.pause();
                audioTrack.flush();
                Log.d(TAG, "AudioTrack已暂停并清空");
            } catch (Exception e) {
                Log.e(TAG, "暂停AudioTrack失败", e);
            }
        }
        
        // 中断线程
        if (synthesisThread != null && synthesisThread.isAlive()) {
            synthesisThread.interrupt();
            try {
                synthesisThread.join(500);  // 等待最多500ms
            } catch (InterruptedException e) {
                Log.w(TAG, "等待合成线程结束被中断");
            }
        }
        
        if (playbackThread != null && playbackThread.isAlive()) {
            playbackThread.interrupt();
            try {
                playbackThread.join(500);  // 等待最多500ms
            } catch (InterruptedException e) {
                Log.w(TAG, "等待播放线程结束被中断");
            }
        }
        
        // 清理资源
        cleanup();
        
        Log.d(TAG, "实时TTS已完全停止");
    }
    
    /**
     * 强制立即停止（用于紧急情况）
     */
    public void forceStop() {
        Log.d(TAG, "强制停止实时TTS");
        
        // 立即设置停止标志
        isStopped = true;
        isActive = false;
        llmDone = true;
        
        // 重置计数器
        synchronized (synthesisLock) {
            activeSynthesisCount = 0;
        }
        
        // 立即停止AudioTrack
        if (audioTrack != null) {
            try {
                audioTrack.stop();
                audioTrack.flush();
                Log.d(TAG, "AudioTrack已强制停止");
            } catch (Exception e) {
                Log.e(TAG, "强制停止AudioTrack失败", e);
            }
        }
        
        // 强制中断线程
        if (synthesisThread != null) {
            synthesisThread.interrupt();
        }
        
        if (playbackThread != null) {
            playbackThread.interrupt();
        }
        
        // 立即清理
        cleanup();
        
        Log.d(TAG, "实时TTS已强制停止");
    }
    
    /**
     * 清理资源
     */
    private void cleanup() {
        Log.d(TAG, "开始清理资源");
        
        // 关闭WebSocket（现在可能有多个）
        if (currentWebSocket != null) {
            try {
                currentWebSocket.close(1000, "正常关闭");
            } catch (Exception e) {
                Log.e(TAG, "关闭WebSocket失败", e);
            }
            currentWebSocket = null;
        }
        
        // 清空队列和缓存
        audioQueue.clear();
        segmentQueue.clear();
        segmentAudioMap.clear();
        
        // 重置计数器
        nextSegmentIndex = 0;
        nextPlayIndex = 0;
        synchronized (synthesisLock) {
            activeSynthesisCount = 0;
        }
        
        // 停止并释放AudioTrack
        if (audioTrack != null) {
            try {
                if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.pause();
                }
                audioTrack.flush();
                audioTrack.stop();
                audioTrack.release();
                Log.d(TAG, "AudioTrack已释放");
            } catch (Exception e) {
                Log.e(TAG, "释放AudioTrack失败", e);
            }
            audioTrack = null;
        }
        
        Log.d(TAG, "资源清理完成");
    }
    
    /**
     * 重置状态
     */
    private void reset() {
        synchronized (textBuffer) {
            textBuffer.setLength(0);
        }
        segmentQueue.clear();
        audioQueue.clear();
        segmentAudioMap.clear();
        
        // 重置计数器
        nextSegmentIndex = 0;
        nextPlayIndex = 0;
        synchronized (synthesisLock) {
            activeSynthesisCount = 0;
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stop();
        client.dispatcher().executorService().shutdown();
    }
    
    /**
     * 是否正在运行
     */
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * 是否正在播放音频
     * 用于判断是否应该禁用手动播放按钮
     */
    public boolean isPlaying() {
        // 综合判断：会话激活 + 播放线程运行 + (有音频数据或正在合成)
        boolean hasAudioOrSynthesizing = false;
        synchronized (synthesisLock) {
            hasAudioOrSynthesizing = !audioQueue.isEmpty() || activeSynthesisCount > 0;
        }
        
        // 只有在真正有音频数据或正在合成时才认为是播放中
        boolean result = isActive && 
                        playbackThread != null && 
                        playbackThread.isAlive() && 
                        hasAudioOrSynthesizing;
        
        Log.d(TAG, "isPlaying() 调用 - isActive: " + isActive + 
                   ", playbackThread.isAlive: " + (playbackThread != null && playbackThread.isAlive()) +
                   ", audioQueue.size: " + audioQueue.size() +
                   ", activeSynthesisCount: " + activeSynthesisCount +
                   ", 返回: " + result);
        return result;
    }
}
