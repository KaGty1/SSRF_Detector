package checker;

import burp.api.montoya.collaborator.CollaboratorPayload;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import checker.filter.RequestResponseFilter;
import checker.updater.ParamsUpdater;
import cn.hutool.core.util.RandomUtil;
import common.logger.AutoSSRFLogger;
import common.provider.*;
import lombok.Getter;
import ui.UIMain;
import ui.dashboard.DashboardTable;
import ui.dashboard.DashboardTableData;
import ui.dashboard.StatusEnum;
import ui.vuln.VulnTable;
import ui.vuln.VulnTableData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public enum SSRFChecker {
    INSTANCE;

    private DashboardTable dashboardTable;
    private VulnTable vulnTable;

    private SSRFChecker() {
        UIMain uiMain = UIProvider.INSTANCE.getUiMain();
        if (uiMain != null && uiMain.getDashboardTab() != null) {
            this.dashboardTable = uiMain.getDashboardTab().getTable();
        } else {
            this.dashboardTable = null;
        }
        if (uiMain != null && uiMain.getVulnTab() != null) {
            this.vulnTable = uiMain.getVulnTab().getTable();
        } else {
            this.vulnTable = null;
        }
    }

    private final AutoSSRFLogger logger = AutoSSRFLogger.INSTANCE;
    private final CollaboratorProvider collaboratorProvider = CollaboratorProvider.INSTANCE;
    @Getter
    private final RequestResponseFilter filter = RequestResponseFilter.INSTANCE;
    private final ParamsUpdater updater = new ParamsUpdater();
    private final SSRFPayloadsSetting ssrfPayloadsSetting = SSRFPayloadsSetting.INSTANCE;
    private final CustomizeRequestProvider customizeRequestProvider = CustomizeRequestProvider.INSTANCE;
    private final DNSLogProvider dnsLogProvider = new DNSLogProvider();

    public void check(HttpRequestResponse baseRequestResponse, Integer id) {
        HttpRequest request = baseRequestResponse.request();
        if (!filter.filter(baseRequestResponse, id)) {
            return;
        }

        
        List<String> successPayloads = new ArrayList<>();
        AtomicBoolean hasError = new AtomicBoolean(false);

        if (id == null) {
            id = dashboardTable.generateId();
            dashboardTable.addRow(DashboardTableData.buildDashboardTableData(id, baseRequestResponse));
        } else {
            dashboardTable.updateStatus(id, StatusEnum.CHECKING);
        }

        
        for (String payload : ssrfPayloadsSetting.getPayloads()) {
            HttpRequest newRequest;

            if ("collaborator".equals(payload)) {
                logger.logToOutput("Collaborator正在检测");
                CollaboratorPayload collaboratorPayload = collaboratorProvider.generatePayload();
                newRequest = updateParameterAndBuildRequest(request, filter.getParameters(), collaboratorPayload);
                if (newRequest == null) continue;

                collaboratorProvider.sendReqAndWaitCollaboratorResult(id, collaboratorPayload, newRequest)
                        .whenComplete((result, err) -> {
                            if (err != null) {
                                logger.logToError(err);
                                synchronized (this) {
                                    hasError.set(true);
                                }
                                return;
                            }
                            synchronized (this) {
                                if (result.getSuccess()) {
                                    successPayloads.add("Collaborator: " + collaboratorPayload.toString());
                                    dashboardTable.updateStatus(result.getId(), StatusEnum.SUCCESS);
                                    vulnTable.addRow(VulnTableData.buildVulnTableData(
                                            vulnTable.generateId(),
                                            result.getHttpRequestResponse(),
                                            result.getInteractions()
                                    ));
                                    logger.logToOutput("Collaborator检测 -> 可能存在SSRF: " + request.url());
                                }
                            }
                        });
            } else if ("dnslog".equals(payload)) {
                logger.logToOutput("DNSLog正在检测");
                dashboardTable.updateStatus(id, StatusEnum.CHECKING);
                String dnsLogDomain = dnsLogProvider.generateDomain();
                logger.logToOutput("DNSLog payload为" + dnsLogDomain);
                if (dnsLogDomain == null) continue;

                newRequest = dnslogUpdateParameterAndBuildRequest(request, filter.getParameters(), dnsLogDomain);
                if (newRequest == null) continue;

                HttpRequestResponse response = sendRequest(newRequest);
                logger.logToOutput("DNSLog response为" + response);
                if (response == null) continue;

                Future<DNSLogProvider.DNSLogResult> futureResult = dnsLogProvider.checkDNSLogResult(id, dnsLogDomain, response);
                final Integer checkId = id;
                new Thread(() -> {
                    try {
                        DNSLogProvider.DNSLogResult result = futureResult.get();
                        logger.logToOutput("DNSLog检测结果result: " + result.getSuccess());
                        synchronized (this) {
                            if (result.getSuccess()) {
                                successPayloads.add("DNSLog: " + dnsLogDomain);
                                dashboardTable.updateStatus(checkId, StatusEnum.SUCCESS);
                                vulnTable.addRow(VulnTableData.buildVulnTableData(
                                        vulnTable.generateId(),
                                        result.getHttpRequestResponse(),
                                        Collections.singletonList("DNSLog hit: " + dnsLogDomain)
                                ));
                                logger.logToOutput("可能存在SSRF (DNSLog): " + request.url());
                            }
                        }
                    } catch (Exception e) {
                        synchronized (this) {
                            hasError.set(true);
                        }
                        logger.logToError(new Exception("Error processing DNSLog result: " + e.getMessage()));
                    }
                }).start();
            } else {
                logger.logToOutput("自定义Payload: " + payload + "正在检测");
                newRequest = customizeUpdateParameterAndBuildRequest(request, filter.getParameters(), payload);
                if (newRequest == null) continue;

                HttpRequestResponse response = sendRequest(newRequest);
                if (response != null && response.response() != null) {
                    String responseBody = new String(response.response().body().getBytes());
                    for (String checkString : ssrfPayloadsSetting.getCheckStrings()) {
                        if (!checkString.isEmpty() && responseBody.contains(checkString)) {
                            synchronized (this) {
                                successPayloads.add("Custom Payload: " + payload + " (matched: " + checkString + ")");
                                handleCustomizeStringSuccess(id, response, checkString, request.url());
                                logger.logToOutput("自定义Payload" + payload + "检测 -> 可能存在SSRF: " + request.url());
                            }
                            break;
                        }
                    }
                }
            }
        }

        Integer finalId = id;
        new Thread(() -> {
            try {
                Thread.sleep(15000); 
                synchronized (this) {
                    if (hasError.get()) {
                        dashboardTable.updateStatus(finalId, StatusEnum.ERROR);
                    } else if (!successPayloads.isEmpty()) {
                        dashboardTable.updateStatus(finalId, StatusEnum.SUCCESS);
                        logger.logToOutput("SSRF 检测完成，成功Payload: " + successPayloads);
                    } else {
                        dashboardTable.updateStatus(finalId, StatusEnum.FAILED);
                    }
                }
            } catch (InterruptedException e) {
                logger.logToError(new Exception("Error finalizing SSRF check: " + e.getMessage()));
                dashboardTable.updateStatus(finalId, StatusEnum.ERROR);
            }
        }).start();
    }

    private void handleCustomizeStringSuccess(Integer id, HttpRequestResponse response, String detectedString, String url) {
        dashboardTable.updateStatus(id, StatusEnum.SUCCESS);
        vulnTable.addRow(VulnTableData.buildVulnTableData(
                vulnTable.generateId(),
                response,
                Arrays.asList(detectedString)
        ));
        logger.logToOutput("可能存在SSRF (String Match): " + url);
    }

    private HttpRequest updateParameterAndBuildRequest(HttpRequest request, List<ParsedHttpParameter> parameters, CollaboratorPayload payload) {
        List<HttpParameter> updateParameters = new ArrayList<>();
        for (HttpParameter parameter : parameters) {
            HttpParameter newParameter = HttpParameter.parameter(
                    parameter.name(),
                    "http://" + payload.toString() + "/" + RandomUtil.randomString(6),
                    parameter.type()
            );
            updateParameters.add(newParameter);
        }
        return updater.update(request, updateParameters);
    }

    private HttpRequest customizeUpdateParameterAndBuildRequest(HttpRequest request, List<ParsedHttpParameter> parameters, String payload) {
        List<HttpParameter> updateParameters = new ArrayList<>();
        for (HttpParameter parameter : parameters) {
            HttpParameter newParameter = HttpParameter.parameter(
                    parameter.name(),
                    payload,
                    parameter.type()
            );
            updateParameters.add(newParameter);
        }
        return updater.update(request, updateParameters);
    }

    private HttpRequest dnslogUpdateParameterAndBuildRequest(HttpRequest request, List<ParsedHttpParameter> parameters, String payload) {
        List<HttpParameter> updateParameters = new ArrayList<>();
        for (HttpParameter parameter : parameters) {
            HttpParameter newParameter = HttpParameter.parameter(
                    parameter.name(),
                    "http://" + payload,
                    parameter.type()
            );
            updateParameters.add(newParameter);
        }
        return updater.update(request, updateParameters);
    }

    private HttpRequestResponse sendRequest(HttpRequest httpRequest) {
        return HttpProvider.INSTANCE.sendRequest(httpRequest);
    }
}
