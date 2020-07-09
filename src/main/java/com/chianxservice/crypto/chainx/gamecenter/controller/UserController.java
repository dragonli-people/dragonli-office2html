package com.chianxservice.crypto.chainx.gamecenter.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alpacaframework.org.service.dubbo.interfaces.thirdparty.EmailService;
import com.chainxservice.crypto.chainx.enums.AccoutOwnerAccountType;
import com.chainxservice.crypto.chainx.enums.UserStatus;
import com.chainxservice.crypto.chainx.model.AccountOwner;
import com.chainxservice.crypto.chainx.model.Application;
import com.chainxservice.crypto.chainx.model.User;
import com.chainxservice.crypto.chainx.model.UserApplication;
import com.chainxservice.crypto.gamecentral.model.ArticleEntity;
import com.chainxservice.crypto.gamecentral.repository.ArticleRepository;
import com.chianxservice.crypto.chainx.gamecenter.annotations.RoleUser;
import com.chianxservice.crypto.chainx.gamecenter.element.ControllerBase;
import com.chianxservice.crypto.chainx.gamecenter.element.ErrorCode;
import com.chianxservice.crypto.chainx.gamecenter.element.ExceptionWithErrorCode;
import org.apache.commons.lang.RandomStringUtils;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
@ResponseBody
@RequestMapping("/user")
public class UserController extends ControllerBase {

    @Value("${spring.common.env}")
    protected String env;

    @Autowired
    ArticleRepository articleRepository;
    
    @Autowired
    RedissonClient redissonClient;
    
    @Reference
    EmailService emailService;
    
    @Value("${spring.dubbo-chainx-api.private_key}")
    protected String privateKey;

    protected static final Random random = new Random();

    @PostMapping("/register")
    public Map<String, Object> regist(
            HttpServletResponse response, HttpServletRequest request,
            @RequestBody JSONObject paras) throws Exception {
        Map<String, Object> result = new HashMap<>();
        String email = paras.getString("email");
        String password = paras.getString("password");
        String username = paras.getString("username");
        String nickname = paras.getString("nickname");

        this.assertCheck(null != email && null != username && null != password && !"".equals(email = email.trim()), ErrorCode.PARAS_IS_NULL);//todo error code
//        if(null == email && null == username && null == password){
//        	result.put("errorCode","PARAS_IS_NULL");
//        	result.put("result",false);
//        	result.put("user",null);
//        	return result;
//        }


        User user = userRepository.findByUsername(username);
        this.assertCheck(null == user, ErrorCode.USERORNAME_REPEAT);
//        if(user != null) {
//        	result.put("errorCode","USERORNAME_REPEAT");
//        	result.put("result",false);
//        	result.put("user",null);
//        	return result;
//        }

        String newpw = otherService.sha1(password);

        User u = new User();
        u.setUsername(username);
        u.setNickname(nickname);
        u.setPasswd(newpw);
        u.setPhone(paras.getString("phone"));
        u.setEmail(email);
        u.setRegistEnterpriseId(0L); //为什么要有这个
        u.setStatus(UserStatus.ACTIVE);
        u.setValicode(paras.getString("valicode"));
        u.setRegistTime(System.currentTimeMillis());
        //初始值
        u.setAccountOwnerId(0L);

        userRepository.save(u);

        AccountOwner owner = new AccountOwner();
        owner.setAccountType(AccoutOwnerAccountType.USER);
        owner.setEnterpriseId(0L);
        owner.setPrivateKey(UUID.randomUUID().toString());
        owner.setSecret(UUID.randomUUID().toString());

        //生成 默认的ownner。个人用户允许有多套钱包，但默认的只有一个
        //两边外键互相关联
        owner.setUserId(u.getId());
        accountOwnerRepository.save(owner);
        u.setAccountOwnerId(owner.getId());
        userRepository.save(u);

        this.putCurrentUser(u);
        result.put("result", true);
        result.put("user", JSON.toJSON(u));
        result.put("owner", JSON.toJSON(owner));
        return result;
    }

    @Deprecated
    @PostMapping("/login")
    public Map<String, Object> login(
            HttpServletResponse response, HttpServletRequest request,
            @RequestBody JSONObject paras) throws Exception {

        Map<String, Object> result = userService.login(paras);
        if ((boolean) result.get("result")) {
            JSON json = (JSON) JSONObject.parse(result.get("data").toString());
            User user = JSONObject.toJavaObject(json, User.class);
            this.putCurrentUser(user);
//			String auth = result.get("authInfo").toString();
//			Cookie cookie = new Cookie("auth", auth);
//			cookie.setPath("/");
//			cookie.setSecure(false);
//			cookie.setMaxAge(0);
//			cookie.setHttpOnly(true);
//			response.addCookie(cookie);
        }
        return result;
    }

    @GetMapping("/logout")
    public Map<String, Object> logout() throws Exception {
        this.putCurrentEnterprise(null);
        this.putCurrentUser(null);
        return new HashMap<>();
    }

    @RoleUser
    @GetMapping("/changePassword")
    public Map<String, Object> changePassword(
            @RequestParam(value = "newPassword") String newPassword,
            @RequestParam(value = "passwd") String password
    ) throws Exception {
//		Map<String,Object> result = new HashMap<>();
        User currentUser = this.currentUser();
//        this.assertCheck(currentUser != null, ErrorCode.USER_NOT_LOGIN);

        return excuteChangePassword(currentUser.getUsername(),password,newPassword);
//        JSONObject paras = new JSONObject();
//        paras.put("username", currentUser.getUsername());
//        paras.put("password", password);
//        paras.put("newPassword", newPassword);
////        String password = (String) jsonParams.get("password");
////        String newpw = (String) jsonParams.get("newPassword");
////        String username = (String) jsonParams.get("username");
//        Map<String, Object> changePassword = userService.changePassword(paras);
//
//        return changePassword;
    }

    protected Map<String, Object> excuteChangePassword(String username,String oldPassword,String newPassword) throws Exception{
        JSONObject paras = new JSONObject();
        paras.put("username", username);
        paras.put("password", oldPassword);
        paras.put("newPassword", newPassword);
        paras.put("DONT_CHECK_OLD",oldPassword == null);
//        String password = (String) jsonParams.get("password");
//        String newpw = (String) jsonParams.get("newPassword");
//        String username = (String) jsonParams.get("username");
        return userService.changePassword(paras);
    }

    @Deprecated
    //找回密码
//    @RoleUser
//    @GetMapping("/getValidateCode")
    public Map<String, Object> getValidateCode2(@RequestParam(value = "email") String email) throws ExceptionWithErrorCode {
    	this.assertCheck(!"".equals(email = email.trim()), ErrorCode.UNKNOW);
    	User findByEmail = userRepository.findByEmail(email);
    	this.assertCheck(findByEmail != null,ErrorCode.USER_NOT_FOUND);
    	
    	StringBuilder str = new StringBuilder();
    	String code = RandomStringUtils.randomAlphanumeric(10);
//        Random random = new Random();
//        for (int i = 0; i < 6; i++) {
//            str.append(random.nextInt(10));
//        }
//        String code = str.toString();
    	RMapCache passCode = redissonClient.getMapCache("FIND_PASSWORD_CODE");
    	passCode.put(findByEmail.getEmail(), code,3,TimeUnit.MINUTES);
    	//TODO 发送邮件
    	emailService.sendEmail(email, "临时密码", "您的临时密码为 : "+code+",密码3分钟内有效。请及时登陆使用!");
		Map<String,Object> result = new HashMap<>();
		result.put("code", code);
        return result;
    }

    @GetMapping("/getValidateCode")
    public Map<String, Object> getValidateCode(
            @RequestParam(value = "username") String username,
            @RequestParam(value = "email") String email) throws Exception {
        JSONObject result = new JSONObject();
        this.assertCheck(!"".equals(email = email.trim()), ErrorCode.UNKNOW);
        User findByEmail = userRepository.findByUsername(username);
        if( null == findByEmail ){
            result.put("result",false);
            result.put("errorCode","USER_NOT_FOUND");
            return result;
        }
        if( findByEmail != null && !email.equals(findByEmail.getEmail()) ){
            result.put("result",false);
            result.put("errorCode","EMAIL_NOT_MATCH");
            return result;
        }
        this.assertCheck(findByEmail != null && email.equals(findByEmail.getEmail()),ErrorCode.USER_NOT_FOUND);

        String code = System.currentTimeMillis()+"";
        findByEmail.setValicode(code);
        userRepository.save(findByEmail);

        code = findByEmail.getPasswd()+code;
        code = otherService.sha1(code,12);
        emailService.sendEmail(email, "临时密码", "您的临时密码为 : "+code+",密码3分钟内有效。请及时登陆使用!");

        logger.info("getValidateCode code:{}",code);

        if( "dev".equals(env) )result.put("code",code);
        result.put("result",true);
        return result;
    }
    

    @Deprecated
//    @GetMapping("/findPasswordByCode")
    public Map<String,Object> findPasswordByCode(@RequestParam(value = "email") String email,
    		@RequestParam(value = "code") String code,
    		@RequestParam(value = "newPassword") String newPassword,
            @RequestParam(value = "passwd") String password) throws Exception{
    	this.assertCheck(!"".equals(email = email.trim()) && !"".equals(code = code.trim()), ErrorCode.UNKNOW);
    	RMapCache passCode = redissonClient.getMapCache("FIND_PASSWORD_CODE");
    	String redisCode = passCode.get(email).toString();
    	this.assertCheck(redisCode.equals(code), ErrorCode.VALIDATECODE_WRONG);
    	passCode.remove(email);
    	User findByEmail = userRepository.findByEmail(email);
    	this.assertCheck(findByEmail != null,ErrorCode.USER_NOT_FOUND);
    	Map<String,Object> jsonParams = new HashMap<>();
    	jsonParams.put("newPassword", newPassword);
    	jsonParams.put("passwd", password);
    	jsonParams.put("username", findByEmail.getUsername());
    	return userService.changePassword(jsonParams);
	
    }

    @Deprecated
//    @GetMapping("/loginByCode")
    public Map<String,Object> loginByCode(@RequestParam(value = "email") String email,
    		@RequestParam(value = "code") String code
    		) throws Exception{
    	this.assertCheck(!"".equals(email = email.trim()) && !"".equals(code = code.trim()), ErrorCode.UNKNOW);
    	RMapCache passCode = redissonClient.getMapCache("FIND_PASSWORD_CODE");
    	String redisCode = passCode.get(email).toString();
    	this.assertCheck(redisCode.equals(code), ErrorCode.VALIDATECODE_WRONG);
    	passCode.remove(email);
    	User findByEmail = userRepository.findByEmail(email);
    	this.assertCheck(findByEmail != null,ErrorCode.USER_NOT_FOUND);
    	this.putCurrentUser(findByEmail);
    	Map<String,Object> result = new HashMap<>();
    	result.put("userId",findByEmail.getId()+"");
        result.put("authInfo",JSON.toJSONString(generateAuthInfo(findByEmail.getId()+"")));
    	return result;
    }
    

    //绑定邮箱
    @RoleUser
    @GetMapping("/bindEmail")
    public Map<String, Object> bindEmail(
            @RequestParam(value = "email") final String email,
            @RequestParam(value = "code" ) String code
    ) throws Exception {

        final User currentUser = this.currentUser();
//        this.assertCheck(currentUser != null, ErrorCode.USER_NOT_LOGIN);

        return userService.changeEmail(new HashMap<String, Object>() {{
            put("username", currentUser.getUsername());
            put("newEmail", email);
            put("code",code);
        }});
    }

    @RoleUser
    @GetMapping(value = "/deposit/info")
    public JSONObject getUserAccountMemo(
            @RequestParam(value = "applicationId") Long applicationId,
            @RequestParam(value = "currency") String currency) throws Exception {
        final User currentUser = this.currentUser();
        Application application = applicationRepository.getOne(applicationId);
        UserApplication userApplication = userApplicationRepository
                .findByUserIdAndApplicationId(currentUser.getId(), applicationId)
                .stream().filter(v -> currency.equals(v.getAssetName())).findFirst().orElse(null);
        this.assertCheck(currentUser != null && application != null && userApplication != null
                , ErrorCode.USER_NOT_LOGIN);


        return new JSONObject(
                userService.getUserAccountMemo(applicationId, userApplication.getReflexId()
                        , currency, currentUser.getUsername()));

    }

    @RoleUser
    @GetMapping(value = "/withdrawal")
    public JSONObject withdrawal(
            @RequestParam(value = "applicationId") Long applicationId,
            @RequestParam(value = "address") String address,
            @RequestParam(value = "addressExtend") String addressExtend,
            @RequestParam(value = "amount") String amountStr,
            @RequestParam(value = "currency") String currency) throws Exception {
        final User currentUser = this.currentUser();
        Application application = applicationRepository.getOne(applicationId);
        UserApplication userApplication = userApplicationRepository
                .findByUserIdAndApplicationId(currentUser.getId(), applicationId)
                .stream().filter(v -> currency.equals(v.getAssetName())).findFirst().orElse(null);
        this.assertCheck(currentUser != null && application != null && userApplication != null
                , ErrorCode.USER_NOT_LOGIN);

        String orderId = "chainx--w-"+UUID.randomUUID().toString();
        return new JSONObject(
                chainXService.childAccountWithdrawal(applicationId, userApplication.getReflexId(),amountStr
                        , currency,orderId, address,addressExtend));

    }

    @Deprecated
    @GetMapping("/test")
    public Map<String, Object> test() {
        ArticleEntity articleEntity = articleRepository.getOne(1L);
        return articleEntity == null ? null : JSON.parseObject(JSON.toJSONString(articleEntity));
    }

    @Deprecated
    @GetMapping("/test2")
    public Map<String, Object> test2() throws Exception {
        ArticleEntity articleEntity = articleRepository.getOne(1L);
        if (articleEntity == null || articleEntity.getId() > 0)
            throw new ExceptionWithErrorCode(ErrorCode.PARAS_IS_NULL);
        return articleEntity == null ? null : JSON.parseObject(JSON.toJSONString(articleEntity));
    }


    @GetMapping(value = "/login2")
    public ResponseEntity<JSONObject> login2(
            @RequestParam(value = "username") String username,
            @RequestParam(value = "passwd") String passwd) throws Exception {

        this.assertCheck(!"".equals(username = username.trim()), ErrorCode.UNKNOW);
        this.assertCheck(!"".equals(passwd = passwd.trim()), ErrorCode.UNKNOW);
        this.assertCheck(username.length() < 32, ErrorCode.UNKNOW);
        this.assertCheck(passwd.length() < 32, ErrorCode.UNKNOW);

        JSONObject paras = new JSONObject();
        paras.put("username", username);
        paras.put("password", passwd);
        paras.put("TRY_VALIDATA_CROPTO_AND_RESET" , "".equals( passwd.toLowerCase()
                .replaceAll("^[a-f0-9]{12,12}$","") ) ? 12 : null );
        JSONObject result = new JSONObject(userService.login(paras));
        if (!result.getBoolean("result")) return new ResponseEntity<>(result, HttpStatus.OK);
        User user = JSON.parseObject(JSON.toJSONString(result.getJSONObject("data")), User.class);
        this.putCurrentUser(user);
        result.remove("data");

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping(value = "/register2")
    public ResponseEntity<JSONObject> regist2(
            @RequestParam(value = "username") String username,
            @RequestParam(value = "passwd") String passwd,
            @RequestParam(value = "email") String email,
            @RequestParam(value = "recommendCode",required = false) String recommendCode) throws Exception {
        this.assertCheck(!"".equals(username = username.trim()), ErrorCode.UNKNOW);
        this.assertCheck(!"".equals(passwd = passwd.trim()), ErrorCode.UNKNOW);
        this.assertCheck(username.length() < 32, ErrorCode.UNKNOW);
        this.assertCheck(passwd.length() < 32, ErrorCode.UNKNOW);

        JSONObject paras = new JSONObject();
        paras.put("username", username);
        paras.put("nickname", username);
        paras.put("password", passwd);
        paras.put("email", email);//username+"-"+(random.nextInt(10000))+"@chainxgame.com");
        paras.put("phone", "188" + (80000000 + random.nextInt(10000000)));
        paras.put("channel", "nodebb");//兼容历史遗留
        paras.put("recommendCode",recommendCode);
        paras.put("valicode", random.nextInt(10000) + "");

        JSONObject result = new JSONObject(userService.registUser(paras));
        if (!result.getBoolean("result")) return new ResponseEntity<>(result, HttpStatus.OK);
        User user = JSON.parseObject(JSON.toJSONString(result.getJSONObject("data")), User.class);
        this.putCurrentUser(user);
        result.remove("data");
        result.remove("owner");
        return new ResponseEntity<>(result, HttpStatus.OK);
    }


    @Deprecated
    @PostMapping("/logout23333")
    public Map<String, Object> logoutBak(
            HttpServletResponse response, HttpServletRequest request,
            @RequestBody JSONObject paras) throws Exception {
        Cookie cookie = new Cookie("auth", "");
        cookie.setPath("/");
        cookie.setSecure(false);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        return new HashMap<>();
    }
    

    @Deprecated
    protected JSONObject generateAuthInfo(String uid) throws Exception{
        JSONObject authInfo = new JSONObject();
        long now = System.currentTimeMillis();
        authInfo.put("uniqueId",uid);
        authInfo.put("uid",uid);
        authInfo.put("time",now+"");
        authInfo.put("key",otherService.sha1(uid+"|"+uid+"|"+now+"|"+privateKey));
        return authInfo;
    }
}
