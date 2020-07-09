package com.chianxservice.crypto.chainx.gamecenter.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chainxservice.crypto.chainx.model.*;
import com.chainxservice.crypto.gamecentral.enums.TokenBackStatus;
import com.chainxservice.crypto.gamecentral.enums.TokenDepositStatus;
import com.chainxservice.crypto.gamecentral.model.TokenDepositEntity;
import com.chianxservice.crypto.chainx.gamecenter.annotations.RoleUser;
import com.chianxservice.crypto.chainx.gamecenter.element.ControllerBase;
import com.chianxservice.crypto.chainx.gamecenter.element.ErrorCode;
import com.huobi.client.SyncRequestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@ResponseBody
@RequestMapping("/token/deposit")
public class TokenDepositController extends ControllerBase {
    @Autowired
    SyncRequestClient huobiClient;
    private static final String targetCode = "USDT";

    private static final long TIMOUT = 300000000L;

    @RoleUser
    @Transactional
    @PostMapping("/order/create")
    public JSONObject createOrder(@RequestBody JSONObject body) throws Exception {

        Boolean justQueryCost = body.getBoolean("justQueryCost");
        String urlStr = body.getString("url");
        Map<String, String> urlParas = Arrays.stream(urlStr.split("&")).map(v -> v.split("=")).collect(
                Collectors.toMap(arr -> arr[0], arr -> arr[1]));

        Long applicationId = Long.parseLong(urlParas.get("applicationId"));
        Application application = applicationRepository.getOne(applicationId);
//        cporderid=pal-xxx-1&time=1569220473839&price=299.8&url_n=http%3A%2F%2Fapi.asm80.com%2Ftest%2Fcheck_notice&url_r=http%3A%2F%2Fapi.zhengyueyinhe.com%2Fcallback%2F&auth=%7B%22uniqueId%22%3A%22114%22%2C%22uid%22%3A114%2C%22eid%22%3A0%2C%22time%22%3A1568888278694%2C%22key%22%3A%22D2770EB8B937519E306A0AF207397686D5EAC505%22%7D&productid=3&currency=CNY&sign=7f9dad0744a97785ff2a5e30d510b5c40a1331a3
//        sha1(支付完成跳转地址url_r+金刚订单号cporderid+金额单位currency+产品ID productid+金额price+时间戳time+回调地址url_n+链游账号的auth+链游应用ID+secret)
//        sha1(cporderid+时间戳毫秒time +货币类型currency+支付金额money+支付结果result+secret)
        String cporderId = urlParas.get("cporderid");
        Long time = Long.parseLong(urlParas.get("time"));
        this.assertCheck(Math.abs(System.currentTimeMillis() - time) < TIMOUT, ErrorCode.UNKNOW);//先写死，前期只支持usdt
        String moneyStr = urlParas.get("price");
        BigDecimal sourceMoney = new BigDecimal(moneyStr);
        String urlN = urlParas.get("url_n");
        String urlR = urlParas.get("url_r");
        String urlV = urlParas.get("url_v");
        String authStr = urlParas.get("auth");
        String goodsType = urlParas.get("productid");
        String currency = urlParas.get("currency");
        String sign = urlParas.get("sign");

        Asset asset = assetRepository.findByCode(targetCode);
        BigDecimal rate = null;
        if ("cny".equals(currency.toLowerCase())) rate = asset.getCnyRate();
        this.assertCheck(rate != null && moneyStr != null && urlN != null && urlR != null && goodsType != null &&
                authStr != null, ErrorCode.PARAS_IS_NULL);

        String validateOriginStr =
                urlR + urlV + cporderId + currency + goodsType + moneyStr + time + urlN + authStr + applicationId +
                        application.getSecret();
        String validateStr = otherService.sha1(validateOriginStr).toLowerCase();
        logger.info("token deposit validate;;; origin:[  {}  ] , encry:[  {}  ] , sign:[  {}  ]",validateOriginStr,validateStr,sign);
        this.assertCheck(validateStr.equals(sign), ErrorCode.VALIDATECODE_WRONG);

        User user = currentUser();
        if (!authStr.trim().startsWith("{") && authStr.trim().startsWith("%"))
            authStr = URLDecoder.decode(authStr, "UTF-8");
        JSONObject auth = JSON.parseObject(authStr);
        this.assertCheck(authService.validate(auth) && user.getId() == auth.getLong("uid"), ErrorCode.UNKNOW);

        JSONObject thirdParas = new JSONObject();
        thirdParas.putAll(urlParas);
        thirdParas.put("origin", urlStr);

        JSONObject result = new JSONObject();
        //价格限6位小数
        String orderId = "cx-gc-td-" + UUID.randomUUID().toString();

        this.assertCheck("USDT".equals(targetCode), ErrorCode.UNKNOW);//先写死，前期只支持usdt
        BigDecimal amount = sourceMoney.multiply(rate);//source，如果cny；rate为usdt 2 source的比例

        UserApplication userApplication = userApplicationRepository.findByUserIdAndApplicationId(user.getId(),
                applicationId).stream().filter(v -> targetCode.equals(v.getAssetName())).findFirst().orElse(null);
        this.assertCheck(userApplication != null, ErrorCode.UNKNOW);
        this.assertCheck(amount.compareTo(BigDecimal.ZERO) > 0, ErrorCode.UNKNOW);

        Accounts accounts = accountsRepository.findByApplicationIdAndReflexIdAndAssetName(
                userApplication.getApplicationId(), userApplication.getReflexId(), userApplication.getAssetName());
        boolean enough = accounts.getBalance().compareTo(amount) >= 0;
        if (justQueryCost || !enough) {
            result.put("result", justQueryCost);
            result.put("balance", accounts.getBalance().setScale(6, RoundingMode.FLOOR).toPlainString());
            result.put("need", amount.toPlainString());
            result.put("enough", enough);
            if (!justQueryCost) result.put("errorCode", "BALANCE_NOT_ENOUGH");
            return result;
        }

        TokenDepositEntity tokenDepositEntity = new TokenDepositEntity();
        tokenDepositEntity.setUserId(user.getId());
        tokenDepositEntity.setApplicationId(application.getId());
        tokenDepositEntity.setReflexId(userApplication.getReflexId());
        tokenDepositEntity.setOrderId(orderId);
        tokenDepositEntity.setCostTargetOrderId(orderId + "-cost");
        tokenDepositEntity.setBackOrderId(orderId + "-back");
        tokenDepositEntity.setCostTargetCode(targetCode);
        tokenDepositEntity.setCostTargetAmount(amount);//测试，先写死
        tokenDepositEntity.setThirdKey(currency);//目前阶段先写死,todo:字段改名
        tokenDepositEntity.setExchangeCount(sourceMoney);//测试，先写死
        tokenDepositEntity.setGoodsType(goodsType);//todo rename:goodsType
        tokenDepositEntity.setStatus(TokenDepositStatus.INIT);
        tokenDepositEntity.setBackStatus(TokenBackStatus.INIT);
        tokenDepositEntity.setExtendInfo("{}");
        tokenDepositEntity.setThirdParas(JSON.toJSONString(thirdParas));
        //todo 检测字段

//        if(tokenDepositEntity.getId() == 0)
//        {
//            result.put("result", false);
//            result.put("err", "IN_TEST");
//            return result;
//        }

        tokenDepositEntity = tokenDepositRepository.save(tokenDepositEntity);

        result.put("result", true);
        result.put("id", tokenDepositEntity.getId());
        return result;
    }

    @RoleUser
    @Transactional
    @GetMapping("/view/order/status")
    public JSONObject buy(@RequestParam(value = "id") Long id) throws Exception {
        TokenDepositEntity tokenDepositEntity = tokenDepositRepository.getOne(id);
        this.assertCheck(this.currentUser().getId() == tokenDepositEntity.getUserId(), ErrorCode.UNKNOW);
        JSONObject thirdParas = JSON.parseObject(tokenDepositEntity.getThirdParas());
        JSONObject result = new JSONObject();
        result.put("returnGameUrl",thirdParas.containsKey("returnGameUrl")?thirdParas.getString("returnGameUrl"):"");
        result.put("status", tokenDepositEntity.getStatus().name());
        return result;
    }
}
