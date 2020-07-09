package com.chianxservice.crypto.chainx.gamecenter.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@WebFilter(urlPatterns = { "/*" } )
public class CorsFilter implements Filter {

    protected static final Logger logger = LoggerFactory.getLogger(CorsFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

//        System.out.println("====1====="+req.getMethod().toUpperCase());

        String origin = req.getHeader("Origin");
        if( origin == null ) origin = req.getHeader("host");

        res.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        res.setHeader("Access-Control-Allow-Origin", origin);
        res.setHeader("Access-Control-Max-Age", "10");
//        "host","connection","access-control-request-method","sec-fetch-mode","access-control-request-headers","origin","referer","user-agent","accept-encoding","accept-language"
        res.setHeader("Access-Control-Allow-Headers", "Origin,x-requested-with,Content-Type,Accept,credentials,X-XSRF-TOKEN");
//        res.setHeader("Access-Control-Allow-Headers", "*");

        res.setHeader("Access-Control-Allow-Credentials", "true");
//        System.out.println("====2====="+req.getMethod().toUpperCase()+"|||"+origin);
        if ( "OPTIONS".equals(req.getMethod().toUpperCase())) {

//        if (req.getHeader("Access-Control-Request-Method") != null && "OPTIONS".equals(req.getMethod().toUpperCase())) {
            //本处理器只处理OPTIONS相关
            //如果不是OPTIONS，交给下一个过滤器处理
            res.setHeader("Content-Type","application/json;charset=UTF-8");
            res.getWriter().write("[OPTIONS]");
            return ;
        }
//        logger.info("httpheads:{}",JSON.toJSONString(req.getHeaderNames()));
//        String oldValue = Arrays.asList(req.getCookies()).stream().filter(v->v.getName().equals("test2")).map(v->v.getValue()).findFirst().orElse(null);
//        String newValue = (new Random()).nextInt(10000)+"";
//        System.out.println(origin+"|"+req.getHeader("host")+":old value:"+oldValue+"||new value is :"+newValue);
//        Cookie cc = new Cookie("test2", newValue);
//        cc.setPath("/");
//        cc.setDomain("127.0.0.1");
//        cc.setMaxAge(10000000);
//        res.addCookie(cc);
        chain.doFilter(request, response);
    }
}
