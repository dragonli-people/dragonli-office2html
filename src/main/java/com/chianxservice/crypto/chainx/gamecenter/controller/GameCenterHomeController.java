package com.chianxservice.crypto.chainx.gamecenter.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chainxservice.crypto.chainx.model.*;
import com.chainxservice.crypto.gamecentral.enums.NoticeType;
import com.chainxservice.crypto.gamecentral.model.Banner;
import com.chainxservice.crypto.gamecentral.model.Home;
import com.chainxservice.crypto.gamecentral.model.Notice;
import com.chianxservice.crypto.chainx.gamecenter.annotations.RoleUser;
import com.chianxservice.crypto.chainx.gamecenter.element.ControllerBase;
import com.chianxservice.crypto.chainx.gamecenter.element.ErrorCode;
import com.chianxservice.crypto.chainx.gamecenter.element.ExceptionWithErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@ResponseBody
@RequestMapping("/home")
public class GameCenterHomeController extends ControllerBase {
    @Value("${crypto.upload-path:/opt/image}")
    String uploadPath;

    //轮播图列表
    @GetMapping("/bannerList")
    public Map<String, Object> getBannerList() {
        List<Banner> banner = bannerRepository.findByValidate(true);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("result", true);
        result.put("data", banner);
        return result;
    }

    //首页游戏列表
    @GetMapping("/homeList")
    public Map<String, Object> getHomeList() {
        List<Home> home = homeRepository.findByValidate(true);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("result", true);
        result.put("data", home);
        return result;
    }

    //公告列表
    @GetMapping("/noticeList")
    public Map<String, Object> getNoticeList() {
        List<Notice> notice = noticeRepository.findByValidateAndNoticeType(true,NoticeType.GAMECENTER);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("result", true);
        result.put("data", notice);
        return result;
    }

    @GetMapping("/getApplication")
    public List<JSONObject> getApplication() {

        List<Application> apps = applicationRepository.findAll();
        List<JSONObject> list = apps.stream().map(v -> {
            JSONObject o = JSON.parseObject(JSON.toJSONString(v));
            o.remove("enterprise_id");
            o.remove("status");
            o.remove("key");
            o.remove("secret");
            return o;
        }).collect(Collectors.toList());
        return list;
    }

    @Deprecated
    @GetMapping("/getApplication2")
    public Map<String, Object> getApplication2() {
        Map<String, List<Asset>> priKey = new HashMap<>();
        List<Application> apps = applicationRepository.findAll();
        Map<String, Object> dirapps = new HashMap<>();
        apps.forEach(app -> {
            dirapps.put(app.getId() + "", app.getAppName());
        });
        List<Asset> assets = assetRepository.findAll();
        assets.stream().forEach(asset -> {
            long appId = asset.getPublishEnterpriseId();
            if (priKey.get(dirapps.get(appId)) == null) {
                List<Asset> list = null;
                list.add(asset);
                priKey.put(dirapps.get(appId).toString(), list);
            } else {
                priKey.get(dirapps.get(appId)).add(asset);
            }
        });
        List<Asset> pubKey = assets.parallelStream().filter(asset -> asset.getPublishEnterpriseId() == 0).collect(
                Collectors.toList());

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("result", true);
        result.put("priKey", priKey);
        result.put("pubKey", pubKey);

        return result;
    }

    //用户资产
    @RoleUser
    @GetMapping("/getUserPackage")
    public JSONObject getUserPackage(@RequestParam(value = "applicationId") Long applicationId,
            @RequestParam(value = "createCoin",required = false) String createCoin) throws Exception {

        Application application = applicationRepository.getOne(applicationId);
        User currentUser = this.currentUser();
        this.assertCheck(currentUser != null && application != null, ErrorCode.USER_NOT_LOGIN);

        List<UserApplication> userApplications = userApplicationRepository.findByUserIdAndApplicationId(
                currentUser.getId(), applicationId);
        String reflexId = userApplications.stream().map(v -> v.getReflexId()).findFirst().orElse(null);
        if(reflexId == null)reflexId = currentUser.getId()+"";

        String[] createCoins =
                null != createCoin && !"".equals(createCoin = createCoin.trim()) ? createCoin.split(",") : null;
        for (int i = 0; createCoins != null && i < createCoins.length; i++) {
            String currency = createCoins[i];
            if( userApplications.stream().filter(v->v.getReflexId().equals(currency)).count() > 0 )continue;
            JSONObject jsonParams = new JSONObject();
            jsonParams.put("applicationId", applicationId);
            jsonParams.put("reflexId", reflexId);
            jsonParams.put("currency", currency);
            jsonParams.put("username", currentUser.getUsername());

            userService.getUserAccount(jsonParams);
        }

        JSONObject jsonParams = new JSONObject();
        jsonParams.put("applicationId", applicationId);
        jsonParams.put("reflexId", reflexId);
        return new JSONObject(userService.userAccountList(jsonParams));
    }

    @Deprecated
    @PostMapping("/saveImage")
    public Map<String, Object> sendIdentity(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        // 先实例化一个文件解析器
        CommonsMultipartResolver coMultipartResolver = new CommonsMultipartResolver(
                request.getSession().getServletContext());
        String path = "";
        String p = "www.chainxworld.com";
        if (coMultipartResolver.isMultipart(request)) {
            // 转换request
            MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
            Map<String, String[]> parameterMap = multiRequest.getParameterMap();
            // 获得文件
            MultipartFile file = multiRequest.getFile("file");

            if (!file.isEmpty()) {
                String fileName = file.getOriginalFilename();
                String newfileName = System.currentTimeMillis() + String.valueOf(fileName);
                path = uploadPath + "image" + "/" + "banner" + "/" + newfileName;
                p = p + "/image/" + "banner/" + newfileName;
                // 创建文件实例
                File tempFile = new File(path);
                try {
                    file.transferTo(tempFile);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    result.put("result", false);
                    return result;
                } catch (IOException e) {
                    e.printStackTrace();
                    result.put("result", false);
                    return result;
                }
            }
        }
        result.put("result", true);
        result.put("data", p);
        return result;
    }
}
