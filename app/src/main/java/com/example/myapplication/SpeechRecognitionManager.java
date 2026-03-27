package com.example.myapplication;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SpeechRecognitionManager {
    private static final String TAG = "SpeechRecognition";
    
    private static final String API_URL = "http://222.19.82.143:7100/api/v1";
    private static final String LANG_TYPE = "bo-CN"; 
    
    private static final int SAMPLE_RATE = 16000; 
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO; 
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; 
    
    private Context context;
    private OkHttpClient client;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread;
    private ByteArrayOutputStream audioData;
    
    // 问题2优化：使用静音检测而不是固定延迟
    // 在录音开始和结束时自动检测并保留静音段，避免截断
    private static final int SILENCE_THRESHOLD = 500;  // 静音阈值（振幅）
    private static final int PRE_BUFFER_SIZE = 8000;   // 预缓冲大小（约0.5秒的音频）
    private byte[] preBuffer = new byte[PRE_BUFFER_SIZE]; // 循环预缓冲区
    private int preBufferPos = 0;  // 预缓冲区当前位置
    private boolean hasStartedSaving = false; // 是否已开始保存音频
    
    private RecognitionCallback callback;
    
    public interface RecognitionCallback {
        void onRecordingStart();
        void onRecordingStop();
        void onRecognitionSuccess(String result);
        void onRecognitionError(String error);
        void onAmplitudeChanged(float amplitude); // 新增：实时音频振幅回调
    }
    
    public SpeechRecognitionManager(Context context, OkHttpClient client) {
        this.context = context;
        this.client = client;
    }
    
    public void setCallback(RecognitionCallback callback) {
        this.callback = callback;
    }
    
    public void startRecording() {
        if (isRecording) {
            Log.w(TAG, "已经在录音中");
            return;
        }
        
        try {
            int bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            );
            
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            );
            
            audioData = new ByteArrayOutputStream();
            preBufferPos = 0;
            hasStartedSaving = false;
            
            audioRecord.startRecording();
            isRecording = true;
            
            if (callback != null) {
                callback.onRecordingStart();
            }
            
            Log.d(TAG, "🎤 开始录音（智能缓冲模式）");
            
            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[bufferSize];
                    
                    while (isRecording) {
                        int read = audioRecord.read(buffer, 0, buffer.length);
                        if (read > 0) {
                            // 计算音频振幅
                            final float amplitude = calculateAmplitude(buffer, read);
                            
                            // 问题2优化：智能缓冲策略
                            if (!hasStartedSaving) {
                                // 还没开始保存，先放入循环预缓冲区
                                addToPreBuffer(buffer, read);
                                
                                // 检测到有效声音（非静音）时，开始保存
                                if (amplitude > 0.05f) { // 振幅阈值
                                    hasStartedSaving = true;
                                    // 将预缓冲区的内容写入主缓冲区
                                    flushPreBufferToMain();
                                    Log.d(TAG, "检测到声音，开始保存音频");
                                }
                            } else {
                                // 已经开始保存，直接写入主缓冲区
                                audioData.write(buffer, 0, read);
                            }
                            
                            // 回调振幅
                            if (callback != null) {
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onAmplitudeChanged(amplitude);
                                    }
                                });
                            }
                        }
                    }
                }
            });
            recordingThread.start();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 录音失败", e);
            if (callback != null) {
                callback.onRecognitionError("录音失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 添加数据到循环预缓冲区
     */
    private void addToPreBuffer(byte[] data, int length) {
        for (int i = 0; i < length; i++) {
            preBuffer[preBufferPos] = data[i];
            preBufferPos = (preBufferPos + 1) % PRE_BUFFER_SIZE;
        }
    }
    
    /**
     * 将预缓冲区的内容刷新到主缓冲区
     */
    private void flushPreBufferToMain() {
        // 按正确顺序写入预缓冲区的数据
        // 从 preBufferPos 开始到末尾
        if (preBufferPos < PRE_BUFFER_SIZE) {
            audioData.write(preBuffer, preBufferPos, PRE_BUFFER_SIZE - preBufferPos);
        }
        // 从开头到 preBufferPos
        if (preBufferPos > 0) {
            audioData.write(preBuffer, 0, preBufferPos);
        }
    }
    
    public void stopRecording() {
        if (!isRecording) {
            return;
        }
        
        isRecording = false;
        
        // 问题2优化：继续录制一小段时间以捕获尾音
        // 但不阻塞UI，在后台线程中完成
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 继续录制约200ms以捕获尾音
                    int extraSamples = SAMPLE_RATE * 2 / 10; // 0.2秒
                    byte[] tailBuffer = new byte[extraSamples * 2]; // 16位=2字节
                    
                    if (audioRecord != null && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        int read = audioRecord.read(tailBuffer, 0, tailBuffer.length);
                        if (read > 0 && hasStartedSaving) {
                            audioData.write(tailBuffer, 0, read);
                            Log.d(TAG, "捕获尾音: " + read + " 字节");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "捕获尾音失败", e);
                }
                
                // 停止录音
                if (audioRecord != null) {
                    try {
                        audioRecord.stop();
                        audioRecord.release();
                        audioRecord = null;
                    } catch (Exception e) {
                        Log.e(TAG, "停止录音异常", e);
                    }
                }
                
                if (recordingThread != null) {
                    try {
                        recordingThread.join(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                
                Log.d(TAG, "🛑 停止录音");
                
                // 通知UI
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onRecordingStop();
                        }
                    });
                }
                
                // 发送识别请求
                if (audioData != null && audioData.size() > 0) {
                    sendRecognitionRequest(audioData.toByteArray());
                } else {
                    Log.w(TAG, "没有录制到有效音频");
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onRecognitionError("没有检测到声音");
                            }
                        });
                    }
                }
            }
        }).start();
    }
    
    /**
     * 取消录音（不发送识别请求）
     */
    public void cancelRecording() {
        if (!isRecording) {
            return;
        }
        
        isRecording = false;
        
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            } catch (Exception e) {
                Log.e(TAG, "停止录音异常", e);
            }
        }
        
        if (recordingThread != null) {
            try {
                recordingThread.join(500); // 最多等待500ms
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        if (callback != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    callback.onRecordingStop();
                }
            });
        }
        
        Log.d(TAG, "❌ 取消录音");
        
        // 清空音频数据，不发送识别请求
        if (audioData != null) {
            audioData.reset();
        }
    }
    
    private void sendRecognitionRequest(byte[] audioBytes) {
        
        byte[] wavData = convertToWav(audioBytes);
        
        Log.d(TAG, "📤 发送语音识别请求");
        Log.d(TAG, "   音频大小: " + wavData.length + " bytes");
        
        String url = API_URL + 
            "?lang_type=" + LANG_TYPE +
            "&format=wav" +
            "&sample_rate=" + SAMPLE_RATE +
            "&bit_depth=16" +
            "&enable_punctuation_prediction=true" +
            "&enable_inverse_text_normalization=true";
        
        Log.d(TAG, "   API: " + url);
        
        RequestBody body = RequestBody.create(
            wavData,
            MediaType.parse("application/octet-stream")
        );
        
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/octet-stream")
            .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "❌ 请求失败", e);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onRecognitionError("识别失败: " + e.getMessage());
                        }
                    }
                });
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "📥 收到识别结果: " + responseBody);
                    
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        String status = json.optString("status");
                        
                        if ("00000".equals(status)) {
                            
                            String result = json.optString("result");
                            Log.d(TAG, "✅ 识别成功: " + result);
                            
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) {
                                        callback.onRecognitionSuccess(result);
                                    }
                                }
                            });
                        } else {
                            
                            String message = json.optString("message", "未知错误");
                            Log.e(TAG, "❌ 识别失败: " + message);
                            
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) {
                                        callback.onRecognitionError("识别失败: " + message);
                                    }
                                }
                            });
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "❌ 解析响应失败", e);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onRecognitionError("解析结果失败");
                                }
                            }
                        });
                    }
                } else {
                    Log.e(TAG, "❌ 服务器返回错误: " + response.code());
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onRecognitionError("服务器错误: " + response.code());
                            }
                        }
                    });
                }
            }
        });
    }
    
    private byte[] convertToWav(byte[] pcmData) {
        int totalDataLen = pcmData.length + 36;
        int totalAudioLen = pcmData.length;
        int channels = 1; 
        int byteRate = SAMPLE_RATE * channels * 2; 
        
        byte[] header = new byte[44];
        
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; 
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; 
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (SAMPLE_RATE & 0xff);
        header[25] = (byte) ((SAMPLE_RATE >> 8) & 0xff);
        header[26] = (byte) ((SAMPLE_RATE >> 16) & 0xff);
        header[27] = (byte) ((SAMPLE_RATE >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * 2); 
        header[33] = 0;
        header[34] = 16; 
        header[35] = 0;
        
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        
        byte[] wavData = new byte[header.length + pcmData.length];
        System.arraycopy(header, 0, wavData, 0, header.length);
        System.arraycopy(pcmData, 0, wavData, header.length, pcmData.length);
        
        return wavData;
    }
    
    public boolean isRecording() {
        return isRecording;
    }
    
    public void testWithAudioFile(String fileName) {
        try {
            
            java.io.InputStream is = context.getAssets().open(fileName);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            
            byte[] data = new byte[1024];
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            is.close();
            
            byte[] audioBytes = buffer.toByteArray();
            
            Log.d(TAG, "🧪 使用测试音频文件: " + fileName);
            Log.d(TAG, "   文件大小: " + audioBytes.length + " bytes");
            
            sendRecognitionRequest(audioBytes);
            
        } catch (IOException e) {
            Log.e(TAG, "❌ 读取测试音频文件失败", e);
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onRecognitionError("读取测试文件失败: " + e.getMessage());
                    }
                });
            }
        }
    }
    
    /**
     * 计算音频振幅
     * @param buffer PCM音频数据
     * @param length 有效数据长度
     * @return 归一化的振幅值 (0.0 - 1.0)
     */
    private float calculateAmplitude(byte[] buffer, int length) {
        if (buffer == null || length <= 0) {
            return 0f;
        }
        
        long sum = 0;
        int sampleCount = 0;
        
        // PCM 16位数据，每2个字节组成一个采样点
        for (int i = 0; i < length - 1; i += 2) {
            // 小端序：低字节在前，高字节在后
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            sum += Math.abs(sample);
            sampleCount++;
        }
        
        if (sampleCount == 0) {
            return 0f;
        }
        
        // 计算平均振幅
        float average = sum / (float) sampleCount;
        
        // 归一化到 0-1 范围
        // 16位PCM的最大值是32768
        float normalized = average / 32768f;
        
        // 应用对数缩放，使小声音也能显示
        normalized = (float) Math.log10(1 + normalized * 9) / (float) Math.log10(10);
        
        // 限制范围
        return Math.min(1.0f, Math.max(0.0f, normalized));
    }
    
    /**
     * 需求3: 预连接语音识别接口
     * 发送一个空的或极小的请求来预热连接,减少首次使用延迟
     */
    public void preconnect() {
        Log.d(TAG, "🔌 预连接语音识别接口");
        
        // 创建一个最小的WAV文件(只有头部,没有实际音频数据)
        byte[] emptyPcm = new byte[0];
        byte[] emptyWav = convertToWav(emptyPcm);
        
        String url = API_URL + 
            "?lang_type=" + LANG_TYPE +
            "&format=wav" +
            "&sample_rate=" + SAMPLE_RATE +
            "&bit_depth=16";
        
        RequestBody body = RequestBody.create(
            emptyWav,
            MediaType.parse("application/octet-stream")
        );
        
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/octet-stream")
            .build();
        
        // 异步发送,不关心结果
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(TAG, "预连接失败(正常): " + e.getMessage());
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d(TAG, "✅ 预连接完成,状态码: " + response.code());
                if (response.body() != null) {
                    response.body().close();
                }
            }
        });
    }
    
    public void release() {
        if (isRecording) {
            stopRecording();
        }
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
    }
}
