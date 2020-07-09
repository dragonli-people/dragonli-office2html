package com.chianxservice.crypto.chainx.gamecenter.advice;

import com.alibaba.dubbo.config.annotation.Reference;
import com.chainxservice.chainx.AuthService;
import com.chianxservice.crypto.chainx.gamecenter.element.AuthDto;
import com.chianxservice.crypto.chainx.gamecenter.element.ControllerVars;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Component
@ControllerAdvice(annotations = Controller.class)
public class ResultFormatHandler implements ResponseBodyAdvice {

    @Value("${spring.dubbo-chainx-api.private_key}")
    protected String privateKey;

    @Value("${spring.dubbo-chainx-api.domain}")
    protected String domain;

    protected static final Logger logger = LoggerFactory.getLogger(ResultFormatHandler.class);


//    @Autowired
//    protected AuthValidateUtil authValidateUtil;

    @Reference
    AuthService authService;

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        //todo 变成true
        return true;
    }

    @ResponseBody
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class selectedConverterType, ServerHttpRequest req, ServerHttpResponse res) {

        HttpServletRequest request = ((ServletServerHttpRequest)req).getServletRequest();
        HttpServletResponse response = ((ServletServerHttpResponse)res).getServletResponse();
        try {
            writeCookies(request,response);
        }catch (Exception e){}

        //todo
        Map<String,Object> r = new HashMap<>();
        r.put("auth", ControllerVars.currentAuth.get());
        r.put("user", ControllerVars.currentUser.get());
        r.put("enterprise", ControllerVars.currentEnterprise.get());
        r.put("MESSAGE_BODY",body);

        if(ControllerVars.startTime != null)
            logger.info("||cost time|{}|{}|",System.currentTimeMillis()-ControllerVars.startTime.get(),request.getRequestURL());
        return r;
    }

    private void writeCookies(HttpServletRequest request,HttpServletResponse response) throws Exception{
        AuthDto authDto = ControllerVars.currentAuth.get();
//        if( request.getCookies() != null
//                || null != Arrays.asList(request.getCookies()).stream()
//                .filter(cookie -> cookie.getName().equals("auth"))
//                .findFirst().orElse(null) )
//            return;
//        logger.info("authStr write1:{}", authDto.toJson());

        if(null == authDto.getKey()||authDto.getTime()<=0
                ||Math.abs(authDto.getTime()-System.currentTimeMillis())>30*24*60*60*1000)//1小时待配置
        {
            //刷新加密串
            authDto.fromJson(
                    (Map<String,Object>)authService.generate(authDto.toMap(),true,true).get("authDto"));
        }
//        logger.info("authStr write2:{}", authDto.toJson());

        String origin = request.getHeader("host");
        origin = origin.replaceAll("^http:\\/\\/","");
        origin = origin.replaceAll(":.*$","");
        String value = authDto.toJson();
        value = URLEncoder.encode(value,"UTF-8");
//        System.out.println(value);
        Cookie c = new Cookie("auth", value);
        c.setMaxAge(60 * 60 * 10 * 1000);//todo 待配置
        c.setPath("/");
//            c.setHttpOnly(true);
//            c.setSecure(true);
        c.setDomain(domain != null && !"".equals(domain.trim()) ? domain.trim() : origin);
        logger.info("write auth cookies>>>value:{},domain:{}"
                , URLDecoder.decode(c.getValue(),"UTF-8"),c.getDomain());
        response.addCookie(c);


            //just a test
//            System.out.println(2333);
//            String oldValue = Arrays.asList(request.getCookies()).stream().filter(v->v.getName().equals("test2")).map(v->v.getValue()).findFirst().orElse(null);
//            String newValue = (new Random()).nextInt(10000)+"";
//            System.out.println(request.getHeader("host")+":old value:"+oldValue+"||new value is :"+newValue);
//            Cookie cc = new Cookie("test8", newValue);
//            cc.setPath("/");
//            cc.setDomain("127.0.0.1");
//            cc.setMaxAge(10000000);
//            response.addCookie(cc);
//            System.out.println(2444);

    }
}
