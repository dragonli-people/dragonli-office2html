package com.chianxservice.crypto.chainx.gamecenter.interceptor;

import com.chianxservice.crypto.chainx.gamecenter.annotations.RoleUser;
import com.chianxservice.crypto.chainx.gamecenter.element.ControllerVars;
import com.chianxservice.crypto.chainx.gamecenter.element.ErrorCode;
import com.chianxservice.crypto.chainx.gamecenter.element.ExceptionWithErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class GeneralInterceptor implements HandlerInterceptor {

    @Value("${spring.dubbo-chainx-api.private_key}")
    protected String privateKey;

    @Value("${spring.dubbo-chainx-api.domain}")
    protected String domain;

//    @Reference
//    AuthService authService;

//    @Autowired
//    protected AuthValidateUtil authValidateUtil;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{
        if (handler instanceof HandlerMethod) {
            HandlerMethod method = (HandlerMethod) handler;
            if( null != method.getMethodAnnotation(RoleUser.class) && ControllerVars.currentUser.get() == null )
                throw new ExceptionWithErrorCode(ErrorCode.PERMISSION_DENIED);
        }
        return true;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        Cookie cc = new Cookie("test7", "111");
        cc.setPath("/");
        cc.setDomain("127.0.0.1");
        cc.setMaxAge(10000000);
        response.addCookie(cc);
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {

        if ( "OPTIONS".equals(request.getMethod().toUpperCase())) return;


//        User u = ControllerVars.currentUser.get();
//        Enterprise e = ControllerVars.currentEnterprise.get();
//        AuthDto auth = ControllerVars.currentAuth.get();
//        if(null == auth)return;
//        auth.setUid(null == u ? 0 : u.getId());
//        auth.setEid(null == e ? 0 : e.getId());
//        if(null == auth.getKey()||auth.getTime()-System.currentTimeMillis()<3600000)//1小时待配置
//        {
//            //刷新加密串
//            authValidateUtil.generate(auth,privateKey,true,true);
//        }
//        domain = "http://127.0.0.1:8505";
//        if( request.getCookies() == null
//                || null == Arrays.asList(request.getCookies()).stream()
//                    .filter(cookie -> cookie.getName().equals("auth"))
//                    .findFirst().orElse(null) ) {
//
//            String origin = request.getHeader("Origin");
//            if( origin == null ) origin = request.getHeader("host");
//            Cookie c = new Cookie("auth", ControllerVars.currentAuth.get().toJson());
//            c.setMaxAge(60 * 60 * 10 * 1000);//todo 待配置
//            c.setPath("/");
////            c.setHttpOnly(true);
////            c.setSecure(true);
//            c.setDomain(domain != null && !"".equals(domain.trim()) ? domain.trim() : origin);
//            response.addCookie(c);
//        }
//        Cookie cc = new Cookie("test1", "111");
//        cc.setPath("/");
//        cc.setDomain("127.0.0.1:8505");
//        cc.setMaxAge(10000000);
//        response.addCookie(cc);

        ControllerVars.currentAuth.remove();
        ControllerVars.currentUser.remove();
        ControllerVars.currentEnterprise.remove();
        ControllerVars.defaultOwner.remove();
        ControllerVars.requestApp.remove();
    }


}
