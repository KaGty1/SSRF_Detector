package common.provider;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.collaborator.*;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import common.CollaboratorResult;
import common.pool.CollaboratorThreadPool;
import ui.dashboard.StatusEnum;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// 定义一个名为 CollaboratorProvider 的枚举类，用于提供与协作服务器（如 Burp Collaborator）的交互功能
public enum CollaboratorProvider {
    // 定义单例实例 INSTANCE，枚举类默认是单例模式
    INSTANCE;

    // 定义一个私有 Collaborator 类型的变量，用于与外部 Collaborator 服务交互
    // Collaborator 是 Burp Suite API 提供的接口，用于生成负载和获取交互记录
    private Collaborator collaborator;

    // 定义一个私有 CollaboratorClient 类型的变量，用于执行具体的 Collaborator 操作
    // CollaboratorClient 是 Collaborator 的客户端实例，提供生成负载和查询交互的功能
    private CollaboratorClient client;

    // 从 CollaboratorThreadPool 单例中获取线程池，用于执行发送请求和轮询任务
    // ThreadPoolExecutor 是 Java 提供的线程池类，用于异步处理任务
    private final ThreadPoolExecutor collaboratorReqPool = CollaboratorThreadPool.INSTANCE.getPool();

    // 定义一个 HttpProvider 类型的常量，从单例中获取，用于发送 HTTP 请求
    private final HttpProvider httpProvider = HttpProvider.INSTANCE;

    // 定义轮询参数：每隔几次轮询开始延时，值为 5
    private final Integer tryCountPer = 5;
    // 定义轮询参数：每次延时的时间，单位为秒，值为 1
    private final Integer delaySeconds = 1;
    // 定义轮询参数：最大轮询次数，值为 10
    private final Integer maxTryCount = 10;

    // 定义一个静态方法，用于初始化 CollaboratorProvider 的实例
    // 参数 MontoyaApi 是 Burp Suite 的核心 API 接口
    public static void constructCollaboratorProvider(MontoyaApi api) {
        // 从 MontoyaApi 获取 Collaborator 实例
        Collaborator collaboratorInstance = api.collaborator();
        // 将 Collaborator 实例赋值给枚举单例的 collaborator 字段
        CollaboratorProvider.INSTANCE.collaborator = collaboratorInstance;
        // 创建并赋值 CollaboratorClient 实例，用于后续操作
        CollaboratorProvider.INSTANCE.client = collaboratorInstance.createClient();
    }

    // 定义一个方法，用于重新创建 CollaboratorClient 实例
    // 当客户端失效或需要刷新时调用
    public void recreateClient() {
        this.client = collaborator.createClient();
    }

    // 定义一个方法，用于生成 CollaboratorPayload 负载
    // CollaboratorPayload 是用于检测外部交互的唯一标识（如域名）
    public CollaboratorPayload generatePayload() {
        return client.generatePayload();
    }

    // 定义一个方法，用于发送请求并异步等待 Collaborator 的交互结果
    // 返回 CompletableFuture<CollaboratorResult>，表示异步任务的结果
    // CompletableFuture常用于异步编程
    public CompletableFuture<CollaboratorResult> sendReqAndWaitCollaboratorResult(
            Integer tableId,              // 任务的表格 ID，用于更新 UI 状态
            CollaboratorPayload payload,  // Collaborator 生成的负载，用于检测交互
            HttpRequest httpRequest       // 要发送的 HTTP 请求
    ) {
        // 更新 UI 中的任务状态为 CHECKING，表示正在检测
        UIProvider.INSTANCE.getUiMain().getDashboardTab().getTable()
                .updateStatus(tableId, StatusEnum.CHECKING);

        // 创建 CollaboratorResult 实例对象，用于存储检测结果
        CollaboratorResult collaboratorResult = new CollaboratorResult();
        collaboratorResult.setSuccess(false);  // 默认设置为检测失败
        collaboratorResult.setId(tableId);     // 设置任务 ID

        // 返回一个异步任务，使用 CompletableFuture.supplyAsync
        return CompletableFuture.supplyAsync(() -> {
            // 发送 HTTP 请求并获取响应
            HttpRequestResponse httpRequestResponse = httpProvider.sendRequest(httpRequest);
            HttpResponse response = httpRequestResponse.response();
            // 如果响应为空，抛出异常表示请求失败
            if (response == null) {
                throw new RuntimeException("请求发送失败");
            }
            // 将请求-响应对存储到结果对象中
            collaboratorResult.setHttpRequestResponse(httpRequestResponse);

            int tryCount = 0;  // 初始化轮询计数器
            // 开始轮询，检查 Collaborator 是否有交互记录
            do {
                // 根据负载过滤，获取与当前 payload 相关的交互记录
                List<Interaction> interactions = client.getInteractions(
                        InteractionFilter.interactionPayloadFilter(payload.toString())
                );
                // 遍历交互记录
                for (Interaction interaction : interactions) {
                    InteractionType type = interaction.type();  // 获取交互类型
                    // 如果发现 HTTP 类型的交互，视为检测成功
                    if (type == InteractionType.HTTP) {
                        collaboratorResult.setSuccess(true);        // 设置成功状态
                        collaboratorResult.setInteractions(interactions);  // 保存交互记录
                        return collaboratorResult;                  // 返回结果，结束轮询
                    }
                }
                // 如果未发现 HTTP 交互，进行延时等待
                try {
                    if (tryCount % tryCountPer == 0) {  // 每 tryCountPer 次延时一次
                        TimeUnit.MILLISECONDS.sleep(delaySeconds * 1000);  // 延时指定秒数
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);  // 如果线程被中断，抛出异常
                }

                tryCount++;  // 增加轮询次数

            } while (tryCount < maxTryCount);  // 当达到最大轮询次数时退出循环

            // 如果轮询结束仍未发现 HTTP 交互，返回默认结果（success = false）
            return collaboratorResult;
        }, collaboratorReqPool);  // 指定线程池执行异步任务
    }
}

