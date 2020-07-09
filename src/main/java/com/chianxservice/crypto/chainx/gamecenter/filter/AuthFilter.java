package com.chianxservice.crypto.chainx.gamecenter.filter;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alpacaframework.org.service.dubbo.interfaces.general.OtherService;
import com.chainxservice.chainx.AuthService;
import com.chainxservice.crypto.chainx.model.AccountOwner;
import com.chainxservice.crypto.chainx.model.Enterprise;
import com.chainxservice.crypto.chainx.model.User;
import com.chainxservice.crypto.chainx.repository.*;
import com.chianxservice.crypto.chainx.gamecenter.element.AuthDto;
import com.chianxservice.crypto.chainx.gamecenter.element.ControllerVars;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

@Component
@WebFilter(urlPatterns = {"/*"})
public class AuthFilter implements Filter {
    protected static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);
    @Value("${spring.dubbo-chainx-api.private_key}")
    protected String privateKey;
    @Value("${spring.common.env}")
    protected String env;
    //    @Autowired
//    protected AuthValidateUtil authValidateUtil;
    @Reference
    AuthService authService;
    @Reference
    protected OtherService otherService;
    @Autowired
    protected EnterpriseRepository enterpriseRepository;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected AccountOwnerRepository accountOwnerRepository;
    @Autowired
    protected AssetSecretRepository assetSecretRepository;
    @Autowired
    protected AssetApplicationRepository assetApplicationRepository;

    protected static final Random random = new Random();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        ControllerVars.startTime.set(System.currentTimeMillis());
        ControllerVars.requestId.set(String.format("%08x",random.nextInt(Integer.MAX_VALUE)));
        HttpServletRequest req = (HttpServletRequest) request;
//        HttpServletResponse res = (HttpServletResponse) response;
        if (req.getMethod().toUpperCase().equals("OPTIONS")) {
            chain.doFilter(request, response);
            return;
        }

        AuthDto authDto = null;
        try {
            authDto = preHandlerAuth(req);
        } catch (Exception e) {
        }
        if (null == authDto) return;//不该发生的事

        ControllerVars.currentAuth.set(authDto);

        User u = null;
        Enterprise e = null;
        AccountOwner ao = null;
        if (0 != authDto.getEid()) e = enterpriseRepository.get(authDto.getEid());
        if (0 != authDto.getUid()) u = userRepository.get(authDto.getUid());
        boolean accident = 0 != authDto.getEid() && e == null;
        accident = accident || (0 != authDto.getUid() && u == null);
        if(accident){
            authDto.setEid(0);
            authDto.setUniqueId(authService.generateUniqueId(0L,0L));
            authDto.setUid(0L);
            authDto.setKey(null);
        }

//        if( null != e ) ao = accountOwnerRepository.getOne(e.getAccountOwnerId());
//        else if( null != u ) ao = accountOwnerRepository.getOne(u.getAccountOwnerId());

        ControllerVars.currentEnterprise.set(e);
        ControllerVars.currentUser.set(u);
        ControllerVars.defaultOwner.set(ao);

        logger.info("request||{}", req.getRequestURL() + "?" + req.getQueryString());
        /*
        AssetApplication aa = null;
        String appAssetKey = req.getParameter("appAssetKey");
        String appAssetTime = req.getParameter("appAssetTime");
        String appAssetCode = req.getParameter("appAssetCode");
        if (null != appAssetKey && null != appAssetTime && null != appAssetCode) {
            //如果传来了鉴权加密串，验证密文
            //如果都通过，将aa设为相应的值
            try {
                //todo 不能超过15秒
//                System.currentTimeMillis() - Long.parseLong(appAssetTime) > 15000 ...

                AssetSecret assetSecret = assetSecretRepository.findByKey(appAssetKey);
//                assetDb.from(AssetSecret.class)
//                        .where("key = ?",appAssetKey).list()
//                        .stream().findFirst().orElse(null);

                //todo not null

                String rKey = assetSecret.getKey() + "|" + appAssetTime + "|" + assetSecret.getSecret();
                rKey = otherService.sha1(rKey);
                //doto 加密串要相等
                if (rKey.equals(appAssetCode)) aa = assetApplicationRepository.getOne(assetSecret.getApplicationId());
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        ControllerVars.requestApp.set(aa);
        */
//        if(req.getMethod().toUpperCase().equals("OPTIONS"))
//        {
////            chain.doFilter(request, response);
//            res.getWriter().println("OPTIONS");
//            return;
//        }

        // 放行
        //交给下一个过滤器处理
        chain.doFilter(request, response);
//        return ;

    }

    private AuthDto preHandlerAuth(HttpServletRequest req) throws Exception {

        AuthDto authDto = null;
        boolean dontCheck = "dev".equals(env) && req.getParameter("WHO_AM_I") != null;
        if (dontCheck) {
            Long userId = Long.parseLong(req.getParameter("WHO_AM_I").toString());
            authDto = new AuthDto(null);
            authDto.setUid(userId);
            authDto.setUniqueId(userId.toString());
            authDto.setEid(0L);
            authDto.fromJson((Map<String, Object>) authService.generate(authDto.toMap(), true, true).get("authDto"));
            return authDto;//也无需验证了
        }
        Cookie authCookie = null == req.getCookies() ? null : Arrays.asList(req.getCookies()).stream().filter(
                v -> v != null && null != v.getName() && v.getName().equals("auth")).findFirst().orElse(null);
        String authCookieStr = authCookie == null ? null : URLDecoder.decode(authCookie.getValue(), "UTF-8");

        String authStr = authCookieStr == null ? null : authCookieStr;
        if (req.getParameter("auth") != null) authCookieStr = authStr = req.getParameter("auth");
        logger.info("authStr||{}",  authStr);

        authDto = authStr == null ? null : new AuthDto(authStr);
        boolean f1 = authStr == null;
        f1 = f1 || authDto == null || null == authDto.getKey() || "".equals(authDto.getKey());//
//        logger.info("test233 0:{}",f1);
//        f1 = f1 || Math.abs(System.currentTimeMillis()-authDto.getTime()) > 30*24*3600000;
        f1 = f1 || !authService.validate(authDto.toMap());//既然有key就验证一下，验证不过与key不存在，都视为新人
        if (f1) {
            //新人，或验证不通过者，或key为空（这个等同于非法攻击）
//            logger.info("authStr 验证之后没有身份:{}|||{}",req.getRequestURL(),req.getMethod().toUpperCase());

            authDto = new AuthDto(null);
            authDto.setUniqueId(authService.generateUniqueId(0L, 0L));
            authDto.fromJson((Map<String, Object>) authService.generate(authDto.toMap(), true, true).get("authDto"));
            return authDto;//也无需验证了
        }

        return authDto;

//        if ((ControllerVars.ENV_DEV.equals(env) || ControllerVars.ENV_TEST.equals(env)) &&
//                null != req.getAttribute("needNotValidateAuth")) {
//            if (null != req.getAttribute("eid")) authDto.setEid(Long.parseLong((String) req.getAttribute("eid")));
//            else if (null != req.getAttribute("uid")) authDto.setUid(Long.parseLong((String) req.getAttribute
//            ("uid")));
//            try {
//                authValidateUtil.generate(authDto, privateKey, true, true);
//            } catch (Exception e) {
//            }
//        }
    }

    @Override
    public void destroy() {

    }
}
