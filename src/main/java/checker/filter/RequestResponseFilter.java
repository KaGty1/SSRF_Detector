package checker.filter;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import checker.filter.cache.FilterCache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.digest.MD5;
import lombok.Getter;
import cn.hutool.cache.CacheUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


@Getter
public class RequestResponseFilter {
    public static final RequestResponseFilter INSTANCE = new RequestResponseFilter();

    private List<ParsedHttpParameter> parameters;

    private final FilterCache<String, Byte> cache;

    private List<String> checkKeywords;

    public RequestResponseFilter() {
        try {
            cache = new FilterCache<>(CacheUtil.newLRUCache(50000));
            checkKeywords = new ArrayList<>();
            checkKeywords.add("url");    
            checkKeywords.add("src");    
            checkKeywords.add("http");   
            checkKeywords.add("https");  
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateCheckKeywords(List<String> newKeywords) {
        this.checkKeywords = new ArrayList<>(newKeywords);
    }

    
    public boolean filter(HttpRequestResponse baseRequestResponse, Integer id) {
        if (id != null) {
            return true;
        }
        HttpRequest request = baseRequestResponse.request();
        String checkString = buildCheckString(request);
        if (checkString == null) {
            return false;
        }
        String hash = MD5.create().digestHex(checkString);
        if (!cache.contains(hash)) {
            cache.put(hash, Byte.valueOf("0"));
            return true;
        }
        return false;
    }

    
    private String buildCheckString(HttpRequest request) {
        URL url = URLUtil.url(request.url());
        String baseURL = url.getProtocol() + "://" + url.getHost() + "/";
        String path = url.getPath();
        List<ParsedHttpParameter> parameters = request.parameters();
        if (parameters.isEmpty()) {
            return null;
        }
        List<ParsedHttpParameter> updateParameters = new ArrayList<>();
        parameters.sort(Comparator.comparing(ParsedHttpParameter::name));
        ArrayList<String> queries = new ArrayList<>();
        ArrayList<String> params = new ArrayList<>();
        ArrayList<String> cookies = new ArrayList<>();
        for (ParsedHttpParameter parameter : parameters) {
            if (!checkParameter(parameter)) {
                continue;
            }
            HttpParameterType type = parameter.type();
            switch (type) {
                case URL: {
                    queries.add(parameter.name());
                    updateParameters.add(parameter);
                    break;
                }
                case COOKIE: {
                    cookies.add(parameter.name());
                    updateParameters.add(parameter);
                    break;
                }
                default: {
                    params.add(parameter.name());
                    updateParameters.add(parameter);
                    break;
                }
            }
        }

        String queryString = "";
        String paramString = "";
        String cookieString = "";
        if (!queries.isEmpty()) {
            queryString = String.format("path: %s|query: %s", path, queries);
        }
        if (!params.isEmpty()) {
            paramString = String.format("path %s|param: %s", path, params);
        }
        if (!cookies.isEmpty()) {
            cookieString = String.format("cookie: %s", cookies);
        }
        this.parameters = updateParameters;
        if (StrUtil.isAllBlank(queryString, paramString, cookieString)) {
            return null;
        }
        return baseURL + queryString + paramString + cookieString;
    }

    
    private boolean checkParameter(ParsedHttpParameter parameter) {
        String name = parameter.name().toLowerCase();
        String value = parameter.value().toLowerCase();
        for (String keyword : checkKeywords) {
            if (name.contains(keyword) || value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
