package com.example.myapplication;

import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * 网络诊断工具类
 */
public class NetworkDiagnostic {
    
    private static final String TAG = "NetworkDiagnostic";
    
    /**
     * 执行完整的网络诊断
     */
    public static void performDiagnosis(String serverUrl, NetworkDiagnosticCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 1. 解析服务器地址
                    String host = extractHost(serverUrl);
                    int port = extractPort(serverUrl);
                    
                    callback.onProgress("正在解析服务器地址: " + host + ":" + port);
                    
                    // 2. DNS解析测试
                    boolean dnsResolved = testDNSResolution(host);
                    callback.onProgress("DNS解析: " + (dnsResolved ? "成功" : "失败"));
                    
                    if (!dnsResolved) {
                        callback.onComplete("DNS解析失败，无法连接到服务器");
                        return;
                    }
                    
                    // 3. 端口连通性测试
                    boolean portReachable = testPortConnectivity(host, port);
                    callback.onProgress("端口连通性: " + (portReachable ? "成功" : "失败"));
                    
                    if (!portReachable) {
                        callback.onComplete("端口 " + port + " 无法访问");
                        return;
                    }
                    
                    // 4. HTTP连接测试
                    boolean httpReachable = testHttpConnection(serverUrl);
                    callback.onProgress("HTTP连接: " + (httpReachable ? "成功" : "失败"));
                    
                    if (httpReachable) {
                        callback.onComplete("网络连接正常");
                    } else {
                        callback.onComplete("HTTP连接失败");
                    }
                    
                } catch (Exception e) {
                    callback.onComplete("诊断过程出错: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * 测试DNS解析
     */
    private static boolean testDNSResolution(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            Log.d(TAG, "DNS解析成功: " + host + " -> " + address.getHostAddress());
            return true;
        } catch (UnknownHostException e) {
            Log.e(TAG, "DNS解析失败: " + host, e);
            return false;
        }
    }
    
    /**
     * 测试端口连通性
     */
    private static boolean testPortConnectivity(String host, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(host, port), 5000); // 5秒超时
            socket.close();
            Log.d(TAG, "端口连通性测试成功: " + host + ":" + port);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "端口连通性测试失败: " + host + ":" + port, e);
            return false;
        }
    }
    
    /**
     * 测试HTTP连接
     */
    private static boolean testHttpConnection(String url) {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .build();
            
            Request request = new Request.Builder()
                    .url(url + "/health")
                    .get()
                    .build();
            
            Response response = client.newCall(request).execute();
            boolean success = response.isSuccessful();
            response.close();
            
            Log.d(TAG, "HTTP连接测试: " + url + " -> " + (success ? "成功" : "失败"));
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "HTTP连接测试失败: " + url, e);
            return false;
        }
    }
    
    /**
     * 从URL中提取主机名
     */
    private static String extractHost(String url) {
        try {
            if (url.startsWith("http://")) {
                url = url.substring(7);
            } else if (url.startsWith("https://")) {
                url = url.substring(8);
            }
            
            int colonIndex = url.indexOf(':');
            if (colonIndex > 0) {
                return url.substring(0, colonIndex);
            }
            return url;
        } catch (Exception e) {
            return url;
        }
    }
    
    /**
     * 从URL中提取端口号
     */
    private static int extractPort(String url) {
        try {
            if (url.startsWith("http://")) {
                url = url.substring(7);
            } else if (url.startsWith("https://")) {
                url = url.substring(8);
            }
            
            int colonIndex = url.indexOf(':');
            if (colonIndex > 0) {
                String portStr = url.substring(colonIndex + 1);
                return Integer.parseInt(portStr);
            }
            return 80; // 默认HTTP端口
        } catch (Exception e) {
            return 80;
        }
    }
    
    /**
     * 网络诊断回调接口
     */
    public interface NetworkDiagnosticCallback {
        void onProgress(String message);
        void onComplete(String result);
    }
}







