package com.chianxservice.crypto.chainx.gamecenter.controller;

import com.alibaba.fastjson.JSONObject;
import com.chainxservice.crypto.chainx.model.*;
import com.chainxservice.crypto.gamecentral.enums.ExchangeTokenStatus;
import com.chainxservice.crypto.gamecentral.enums.TokenBackStatus;
import com.chainxservice.crypto.gamecentral.model.ExchangeMallOrder;
import com.chainxservice.crypto.gamecentral.model.ExchangeTokenEntity;
import com.chianxservice.crypto.chainx.gamecenter.annotations.RoleUser;
import com.chianxservice.crypto.chainx.gamecenter.element.ControllerBase;
import com.chianxservice.crypto.chainx.gamecenter.element.ErrorCode;
import com.chianxservice.crypto.chainx.gamecenter.utils.TokenDepositUtil;
import com.huobi.client.SyncRequestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Controller
@ResponseBody
@RequestMapping("/exchange/token")
public class ExchangeTokenController extends ControllerBase {
    @Autowired
    SyncRequestClient huobiClient;

    @RoleUser
    @Transactional
    @GetMapping("/order/create")
    public JSONObject createOrder(@RequestParam(value = "applicationId") Long applicationId,
//            @RequestParam(value = "expectTargetPrice",required = false) BigDecimal expectTargetPrice,
            @RequestParam(value = "amount") BigDecimal amount,
            @RequestParam(value = "targetCode") String targetCode,
            @RequestParam(value = "baseCode") String baseCode,
            @RequestParam(value = "justQueryPrice") Boolean justQueryPrice) throws Exception {
        JSONObject result = new JSONObject();
        //价格限6位小数
        this.assertCheck(TokenDepositUtil.supportTargetCodes.contains(targetCode), ErrorCode.UNKNOW);
        this.assertCheck(TokenDepositUtil.supportBaseCodes.contains(baseCode), ErrorCode.UNKNOW);
//        this.assertCheck(justQueryPrice || expectTargetPrice != null , ErrorCode.UNKNOW);
//        this.assertCheck(expectTargetPrice == null || BigDecimal.ZERO.compareTo(expectTargetPrice)<0 , ErrorCode.UNKNOW);
        this.assertCheck(BigDecimal.ZERO.compareTo(amount)<0 , ErrorCode.UNKNOW);
//        this.assertCheck(targetPrice.setScale(6, RoundingMode.CEILING).compareTo(targetPrice) == 0, ErrorCode.UNKNOW);
        String orderId = "cx-gc-ec-" + UUID.randomUUID().toString().replaceAll("\\-","");
        //将来从redis中取。由第三方来维护
//        TokenDepositUtil
        User user = currentUser();
        Application application = applicationRepository.getOne(applicationId);
        String huobiMarketSymbol = targetCode.toLowerCase() + baseCode.toLowerCase();
        if (justQueryPrice) {
            BigDecimal lastPrice = huobiClient.getLastTrade(huobiMarketSymbol).getPrice();
            result.put("result", true);
            result.put("lastPrice",lastPrice.setScale(6, RoundingMode.FLOOR).toPlainString());
            result.put("targetAmount",
                    amount.divide(lastPrice,6,RoundingMode.CEILING)
                            .multiply(new BigDecimal("1.015")).toPlainString());
            return result;
        }


        BigDecimal targetAmount = amount;
        UserApplication userApplication = userApplicationRepository.findByUserIdAndApplicationId(user.getId(),
                applicationId).stream().filter(v->v.getAssetName().equals(targetCode)).findFirst().orElse(null);
        this.assertCheck(userApplication != null, ErrorCode.UNKNOW);

        Accounts accounts = accountsRepository.findByApplicationIdAndReflexIdAndAssetName(
                userApplication.getApplicationId(), userApplication.getReflexId(), userApplication.getAssetName());
        if (accounts.getBalance().compareTo(targetAmount) < 0) {
            result.put("result", false);
            result.put("balance", accounts.getBalance().setScale(6, RoundingMode.FLOOR).toPlainString());
            result.put("cost", targetAmount.toPlainString());
            result.put("code", targetCode);
            result.put("errorCode", "BALANCE_NOT_ENOUGH");
            return result;
        }

        ExchangeTokenEntity exchangeTokenEntity = new ExchangeTokenEntity();
        exchangeTokenEntity.setUserId(user.getId());
        exchangeTokenEntity.setApplicationId(application.getId());
        exchangeTokenEntity.setReflexId(userApplication.getReflexId());
        exchangeTokenEntity.setOrderId(orderId);
        exchangeTokenEntity.setCostTargetOrderId(orderId + "-costTarget");
        exchangeTokenEntity.setCostTargetAmount(targetAmount);//测试，先写死
        exchangeTokenEntity.setExpectTargetPrice(BigDecimal.ZERO);
        exchangeTokenEntity.setTargetCode(targetCode);
        exchangeTokenEntity.setBaseCode(baseCode);
        exchangeTokenEntity.setAddBaseOrderId(orderId + "-baseTarget");
        exchangeTokenEntity.setStatus(ExchangeTokenStatus.INIT);
        exchangeTokenEntity.setExchangeOrderInfo("{}");
        exchangeTokenEntity.setBackStatus(TokenBackStatus.INIT);

        exchangeTokenEntity = exchangeTokenRepository.save(exchangeTokenEntity);

        result.put("result", true);
        result.put("id", exchangeTokenEntity.getId());
        return result;
    }

    @RoleUser
    @Transactional
    @GetMapping("/view/order/status")
    public JSONObject buy(@RequestParam(value = "id") Long id) throws Exception {
        ExchangeTokenEntity exchangeTokenEntity = exchangeTokenRepository.getOne(id);
        this.assertCheck( this.currentUser().getId() == exchangeTokenEntity.getUserId(), ErrorCode.UNKNOW);
        JSONObject result = new JSONObject();
        result.put("status",exchangeTokenEntity.getStatus().name());
        result.put("finallyAmount",exchangeTokenEntity.getBaseFinallyAmount());
        return result;
    }
}
