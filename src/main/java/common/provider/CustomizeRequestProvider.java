package common.provider;

import burp.api.montoya.collaborator.Interaction;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import common.CustomizeRequestResult;
import common.pool.CollaboratorThreadPool;
import ui.dashboard.StatusEnum;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

public enum CustomizeRequestProvider {
    INSTANCE;

    // 定义一个 HttpProvider 类型的常量，从单例中获取，用于发送 HTTP 请求
    private final HttpProvider httpProvider = HttpProvider.INSTANCE;

    // ThreadPoolExecutor 是 Java 提供的线程池类，用于异步处理任务
    private final ThreadPoolExecutor customizeReqPool = CollaboratorThreadPool.INSTANCE.getPool();

    public CompletableFuture<?> sendCustomizeRequest(
            Integer tableId,
            HttpRequest httpRequest
    ) {
        // 更新 UI 中的任务状态为 CHECKING，表示正在检测
        UIProvider.INSTANCE.getUiMain().getDashboardTab().getTable()
                .updateStatus(tableId, StatusEnum.CHECKING);

        // 创建 CustomizeRequestResult 实例对象，用于存储检测结果
        CustomizeRequestResult customizeRequestResult = new CustomizeRequestResult();
        customizeRequestResult.setSuccess(false);  // 默认设置为失败
        customizeRequestResult.setId(tableId);  // 设置任务 ID

        return CompletableFuture.supplyAsync(() -> {
            HttpRequestResponse httpRequestResponse = httpProvider.sendRequest(httpRequest);
            HttpResponse response = httpRequestResponse.response();
            // 如果响应为空，抛出异常表示请求失败
            if (response == null) {
                throw new RuntimeException("请求发送失败");
            } else {
                customizeRequestResult.setSuccess(true);
            }
            customizeRequestResult.setHttpRequestResponse(httpRequestResponse);
            return customizeRequestResult;
        }, customizeReqPool);
    }
}
