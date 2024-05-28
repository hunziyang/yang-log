package com.yang.log.filter;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccessLog {

    private String remoteAddr;
    private String userAgent;
    private String referer;
    private String host;
    private String method;
    private String url;
    private Object body;
    private int responseStatus;
    private int responseTime;
    private int responseBodySize;
    private Object responseBody;
}
