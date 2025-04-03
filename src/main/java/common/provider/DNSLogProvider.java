package common.provider;

import burp.api.montoya.http.message.HttpRequestResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import common.logger.AutoSSRFLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

//Dnslog无回显SSRF测试
public class DNSLogProvider {
    private static final String DNSLOG_BASE_URL = "http://dnslog.cn";
    private final AutoSSRFLogger logger = AutoSSRFLogger.INSTANCE;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // 单线程异步执行

    private String phpSessionId;

    // 获取唯一的 DNSLog 子域名并保存 PHPSESSID
    public String generateDomain() {
        try {
            URL url = new URL(DNSLOG_BASE_URL + "/getdomain.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000); // 5秒连接超时
            conn.setReadTimeout(5000);    // 5秒读取超时

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                // 获取 PHPSESSID
                String setCookie = conn.getHeaderField("Set-Cookie");
                if (setCookie != null && setCookie.contains("PHPSESSID")) {
                    phpSessionId = setCookie.split(";")[0]; // 提取 PHPSESSID=xxx 部分
                    logger.logToOutput("Captured PHPSESSID: " + phpSessionId);
                } else {
                    logger.logToError(new Exception("No PHPSESSID found in response headers"));
                }

                // 读取临时域名
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                String domain = in.readLine().trim();
                in.close();
                logger.logToOutput("Generated DNSLog domain: " + domain);
                return domain;
            } else {
                logger.logToError(new RuntimeException("Failed to get DNSLog domain: " + responseCode));
                return null;
            }
        } catch (Exception e) {
            logger.logToError(new RuntimeException("Error generating DNSLog domain: " + e.getMessage()));
            return null;
        }
    }

    // 检查 DNSLog 记录（异步）
    public Future<DNSLogResult> checkDNSLogResult(Integer id, String domain, HttpRequestResponse requestResponse) {
        return executorService.submit(() -> {
            try {
                if (phpSessionId == null) {
                    logger.logToError(new Exception("PHPSESSID is not initialized. Call generateDomain() first."));
                    return new DNSLogResult(id, requestResponse, false);
                }

                int maxAttempts = 15; // 最大尝试次数
                int delaySeconds = 2; // 每次检查间隔
                for (int i = 0; i < maxAttempts; i++) {
                    URL url = new URL(DNSLOG_BASE_URL + "/getrecords.php");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestProperty("Cookie", phpSessionId); // 设置 PHPSESSID

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        String contentType = conn.getContentType();
                        String charset = "UTF-8";
                        if (contentType != null && contentType.contains("charset=")) {
                            charset = contentType.substring(contentType.indexOf("charset=") + 8);
                        }
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), charset));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                        in.close();

                        logger.logToOutput("Raw response: " + response.toString());

                        JsonArray records = JsonParser.parseString(response.toString()).getAsJsonArray();
                        logger.logToOutput("DNS records found: " + records.toString()); // 修正 getAsString() 为 toString()
                        for (JsonElement record : records) {
//                            String hostname = record.getAsJsonObject().get("host").getAsString();
//                            logger.logToOutput("Checking hostname: " + hostname);
                            if (record.toString().contains(domain)) {
                                logger.logToOutput("DNSLog hit detected for domain: " + domain);
                                return new DNSLogResult(id, requestResponse, true);
                            }
                        }
                    } else {
                        logger.logToError(new Exception("Failed to fetch records, response code: " + responseCode));
                    }
                    Thread.sleep(delaySeconds * 1000); // 等待一段时间再检查
                }
                logger.logToOutput("No DNSLog hit detected for domain: " + domain);
                return new DNSLogResult(id, requestResponse, false);
            } catch (Exception e) {
                logger.logToError(new RuntimeException("Error checking DNSLog result: " + e.getMessage()));
                return new DNSLogResult(id, requestResponse, false);
            }
        });
    }


    // DNSLog 检查结果类
    public static class DNSLogResult {
        private final Integer id;
        private final HttpRequestResponse httpRequestResponse;
        private final boolean success;

        public DNSLogResult(Integer id, HttpRequestResponse httpRequestResponse, boolean success) {
            this.id = id;
            this.httpRequestResponse = httpRequestResponse;
            this.success = success;
        }

        public Integer getId() {
            return id;
        }

        public HttpRequestResponse getHttpRequestResponse() {
            return httpRequestResponse;
        }

        public boolean getSuccess() {
            return success;
        }
    }

    // 关闭线程池（可选，在插件卸载时调用）
    public void shutdown() {
        executorService.shutdown();
    }
}


