package com.yang.log.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yang.log.config.JacksonConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class LogFilter extends OncePerRequestFilter {

    private static final String REQUEST_UUID = "requestUUID";

    private ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        setRequestUUID(httpServletResponse);
        if (shouldSkip(httpServletRequest)) {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } else {
            ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpServletRequest);
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpServletResponse);
            setRequestUUID(responseWrapper);
            long startTime = System.currentTimeMillis();
            filterChain.doFilter(requestWrapper, responseWrapper);
            long endTime = System.currentTimeMillis();
            log.warn("info:{}", generateAccessLog(startTime, endTime, requestWrapper, responseWrapper));
            responseWrapper.copyBodyToResponse();
        }
        MDC.remove(REQUEST_UUID);
        MDC.clear();
    }

    private void setRequestUUID(HttpServletResponse httpServletResponse) {
        String uuid = UUID.randomUUID().toString().toUpperCase();
        MDC.put(REQUEST_UUID, uuid);
        httpServletResponse.setHeader(REQUEST_UUID, uuid);
    }

    private boolean shouldSkip(HttpServletRequest httpServletRequest) {
        if (httpServletRequest.getRequestURI().contains("/upload")) {
            return true;
        }
        if (httpServletRequest.getRequestURI().contains("/download")) {
            return true;
        }
        if (httpServletRequest.getRequestURI().contains("/static")) {
            return true;
        }
        return false;
    }

    private AccessLog generateAccessLog(long startTime, long endTime, ContentCachingRequestWrapper requestWrapper, ContentCachingResponseWrapper responseWrapper) {
        return AccessLog.builder()
                .host(requestWrapper.getRemoteHost())
                .method(requestWrapper.getMethod())
                .userAgent(requestWrapper.getHeader(HttpHeaders.USER_AGENT))
                .referer(requestWrapper.getHeader(HttpHeaders.REFERER))
                .responseTime((int) (endTime - startTime))
                .responseStatus(responseWrapper.getStatus())
                .responseBodySize(responseWrapper.getContentSize())
                .url(getRequestURL(requestWrapper))
                .body(getRequestBody(requestWrapper))
                .responseBody(getResponseBody(responseWrapper))
                .remoteAddr(requestWrapper.getRemoteAddr())
                .build();
    }

    private String getRequestURL(ContentCachingRequestWrapper requestWrapper) {
        String url = requestWrapper.getRequestURI();
        Map<String, String[]> parameterMap = requestWrapper.getParameterMap();
        if (!parameterMap.isEmpty()) {
            List<String> params = new ArrayList<>();
            Iterator<Map.Entry<String, String[]>> iterator = parameterMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String[]> entry = iterator.next();
                Arrays.stream(entry.getValue()).forEach(value -> params.add(String.format("%s=%s", entry.getKey(), value)));
            }
            url = String.format("%s?%s", url, String.join("&", params));
        }
        return url;
    }

    private Object getRequestBody(ContentCachingRequestWrapper requestWrapper) {
        byte[] contentAsByteArray = requestWrapper.getContentAsByteArray();
        return getContentByByte(contentAsByteArray, requestWrapper.getContentType());
    }

    private Object getResponseBody(ContentCachingResponseWrapper responseWrapper) {
        byte[] contentAsByteArray = responseWrapper.getContentAsByteArray();
        return getContentByByte(contentAsByteArray, responseWrapper.getContentType());
    }

    private Object getContentByByte(byte[] contentAsByteArray, String contentType) {
        if (ObjectUtils.isNotEmpty(contentAsByteArray)) {
            if (StringUtils.isNotBlank(contentType) && contentType.equals("application/json")) {
                try {
                    return objectMapper.readValue(contentAsByteArray, Object.class);
                } catch (IOException e) {
                    return new String(contentAsByteArray, StandardCharsets.UTF_8);
                }
            } else {
                return new String(contentAsByteArray, StandardCharsets.UTF_8);
            }
        } else {
            return null;
        }
    }
}
