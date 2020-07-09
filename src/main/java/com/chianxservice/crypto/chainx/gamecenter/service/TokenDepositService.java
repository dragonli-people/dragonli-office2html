package com.chianxservice.crypto.chainx.gamecenter.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alpacaframework.org.service.dubbo.interfaces.general.OtherService;
import com.chainxservice.chainx.ChainXService;
import com.chainxservice.crypto.chainx.enums.AccountAdjustmentStatus;
import com.chainxservice.crypto.chainx.enums.TransferStatus;
import com.chainxservice.crypto.chainx.model.Application;
import com.chainxservice.crypto.chainx.model.User;
import com.chainxservice.crypto.chainx.model.UserApplication;
import com.chainxservice.crypto.chainx.repository.ApplicationRepository;
import com.chainxservice.crypto.chainx.repository.UserApplicationRepository;
import com.chainxservice.crypto.chainx.repository.UserRepository;
import com.chainxservice.crypto.gamecentral.enums.TokenBackStatus;
import com.chainxservice.crypto.gamecentral.enums.TokenDepositStatus;
import com.chainxservice.crypto.gamecentral.model.ExchangeTokenEntity;
import com.chainxservice.crypto.gamecentral.model.TokenDepositEntity;
import com.chainxservice.crypto.gamecentral.repository.TokenDepositRepository;
import com.chianxservice.crypto.chainx.gamecenter.element.ErrorCode;
import com.huobi.client.SyncRequestClient;
import com.huobi.client.model.Order;
import com.huobi.client.model.enums.AccountType;
import com.huobi.client.model.enums.OrderState;
import com.huobi.client.model.enums.OrderType;
import com.huobi.client.model.request.NewOrderRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import retrofit2.http.Url;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLDecoder;

@Service
public class TokenDepositService {
    @Autowired
    UserApplicationRepository userApplicationRepository;
    @Autowired
    TokenDepositRepository tokenDepositRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ApplicationRepository applicationRepository;
    @Reference
    protected ChainXService chainXService;
    @Reference
    protected OtherService otherService;
    @Autowired
    SyncRequestClient huobiClient;
    protected final static Logger logger = LoggerFactory.getLogger(TokenDepositService.class);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleInit(Long id) throws Exception {
        TokenDepositEntity tokenDeposit = tokenDepositRepository.getOne(id);

        if (!payByTokenDeposit(tokenDeposit, tokenDeposit.getCostTargetOrderId(),tokenDeposit.getCostTargetCode(),
                tokenDeposit.getCostTargetAmount().negate(),"DEDUCTION")) {
            tokenDeposit.setStatus(TokenDepositStatus.FAILED);
//            tokenDeposit.setBackStatus(TokenBackStatus.NEED_BACK);
            tokenDeposit.setRemark("USER_APPLICATION_IS_NULL");
            tokenDepositRepository.save(tokenDeposit);
            return;
        }

        tokenDeposit.setStatus(TokenDepositStatus.BEGIN_DEDUCTION_TARGET);
        tokenDepositRepository.save(tokenDeposit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleBeginDeductionTarget(Long id) throws Exception {
        TokenDepositEntity tokenDeposit = tokenDepositRepository.getOne(id);

        TransferStatus status = viewPayStatus(chainXService, tokenDeposit.getCostTargetOrderId(), 32L, 1024);
        if (status == TransferStatus.FAILED || System.currentTimeMillis() - tokenDeposit.getUpdatedAt() > 32000) {
            tokenDeposit.setStatus(TokenDepositStatus.FAILED);
            tokenDeposit.setRemark(status == TransferStatus.FAILED ? "BALANCE_NOT_ENOUGH" : "COST_TARGET_TIMEOUT");
            tokenDepositRepository.save(tokenDeposit);
            return;
        }
        if (status != TransferStatus.SUCCESS) return;

        tokenDeposit.setStatus(TokenDepositStatus.HAD_DEDUCTION_TARGET);
        tokenDepositRepository.save(tokenDeposit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleHadDeductionTarget(Long id) throws Exception {
        //向第三方下单
        TokenDepositEntity tokenDeposit = tokenDepositRepository.getOne(id);
        Application application = applicationRepository.getOne(tokenDeposit.getApplicationId());

        JSONObject jsonObject = JSON.parseObject(tokenDeposit.getThirdParas());
        String urlN = URLDecoder.decode( jsonObject.getString("url_n"),"UTF-8" );
        String urlR = jsonObject.containsKey("url_r") ? URLDecoder.decode( jsonObject.getString("url_r"),"UTF-8" ) : null;

        URIBuilder uriBuilder = new URIBuilder(urlN);
        //应该先查询状态。希望对方提供接口，根据我方订单号查询
//        sha1(cporderid+时间戳毫秒time +货币类型currency+支付金额money+支付结果result+secret)

        String cporderId = jsonObject.getString("cporderid");
        Long time = System.currentTimeMillis();
        String currency = jsonObject.getString("currency");
        String moneyStr = jsonObject.getString("price");
        String result = "0";

        String originStr = cporderId +time+currency+moneyStr+result+application.getSecret();
        String encryStr = otherService.sha1(originStr).toLowerCase();
        uriBuilder.addParameter("cporderid",cporderId);
        uriBuilder.addParameter("time",time.toString());
        uriBuilder.addParameter("currency",currency);
        uriBuilder.addParameter("money",moneyStr);
        uriBuilder.addParameter("result",result);
        uriBuilder.addParameter("sign",encryStr);

        if(urlR != null) jsonObject.put("returnGameUrl",urlR+"?"+uriBuilder.build().getQuery());
        logger.info("callback invoke;;; origin:[  {}  ] , sign:[  {}  ]",originStr,encryStr);
        logger.info("game deposit call back::: {}",uriBuilder.build().toString());

        HttpClient httpClient = HttpClients.createDefault();
        HttpGet get = new HttpGet(uriBuilder.build());
        get.setConfig(RequestConfig.custom().setSocketTimeout(2000).setConnectTimeout(5000).build());
        httpClient.execute(get);

        tokenDeposit.setThirdParas(JSON.toJSONString(jsonObject));
        tokenDeposit.setStatus(TokenDepositStatus.HAD_INVOKE_GAME_API);
        tokenDepositRepository.save(tokenDeposit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleHadInvokeGameApi(Long id) throws Exception {
        //向第三方查询订单状态，该超时超时，该成功成功
        TokenDepositEntity tokenDeposit = tokenDepositRepository.getOne(id);
        Application application = applicationRepository.getOne(tokenDeposit.getApplicationId());

        JSONObject thirdParas = JSON.parseObject(tokenDeposit.getThirdParas());
        String urlView = URLDecoder.decode( thirdParas.getString("url_v"),"UTF-8" );


        String cporderId = thirdParas.getString("cporderid");
        Long time = System.currentTimeMillis();

        //
//        JSONObject exchangeOrderInfo = JSON.parseObject(tokenDeposit.getExchangeOrderInfo());
//        //todo 直接从字段里取
//        String thirdDepositOrderId = exchangeOrderInfo.getString("third-deposit-order-id");

        //sleep且轮询，除非超时，直到成功
        boolean successed = false;
        for (int i = 0; i < 2048; i++) {
            Thread.sleep(32L);
            URIBuilder uriBuilder = new URIBuilder(urlView);
            String originStr = cporderId +time+application.getSecret();
            String encryStr = otherService.sha1(originStr).toLowerCase();
            uriBuilder.addParameter("cporderid",cporderId);
            uriBuilder.addParameter("time",time.toString());
            uriBuilder.addParameter("sign",encryStr);

            logger.info("callback view;;; origin:[  {}  ] , sign:[  {}  ]",originStr,encryStr);
            logger.info("game deposit view status::: {}",uriBuilder.build().toString());

            HttpClient httpClient = HttpClients.createDefault();
            HttpGet get = new HttpGet(uriBuilder.build());
            get.setConfig(RequestConfig.custom().setSocketTimeout(2000).setConnectTimeout(5000).build());
            HttpResponse response = httpClient.execute(get);
            String result = EntityUtils.toString(response.getEntity(), "UTF-8");
            thirdParas.put("response",result);
            logger.info("game deposit view response: {}",result);
            try {
                JSONObject o = JSON.parseObject(result);
                if( o.containsKey("status") && o.getInteger("status") == 1 ) successed = true;//先模拟
            }catch (Exception e){
                break;
            }

            //todo 根据订单号查看订单状态
            if (successed) break;
        }
        if (!successed) {
            //这个分支是确定失败，没有充值成功，需要退钱
            tokenDeposit.setThirdParas(JSON.toJSONString(thirdParas));
            tokenDeposit.setStatus(TokenDepositStatus.EXCEPTION_BY_GAME_INVOKE);
            tokenDeposit.setRemark("deposit to third timeout");
            tokenDeposit.setBackStatus(TokenBackStatus.NEED_BACK);
            tokenDepositRepository.save(tokenDeposit);
            return;
        }

        //如果异常，可置为这个状态：EXCEPTION_BY_GAME_INVOKE，此时需要手工确认是否成功
        tokenDeposit.setThirdParas(JSON.toJSONString(thirdParas));
        tokenDeposit.setStatus(TokenDepositStatus.SUCCESS);
        tokenDepositRepository.save(tokenDeposit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleHadInvokeBackMoney(Long id) throws Exception {
        TokenDepositEntity tokenDeposit = tokenDepositRepository.getOne(id);
//        if (tokenDeposit.getBackOrderId() == null) {
//            tokenDeposit.setBackOrderId(tokenDeposit.getOrderId() + "-backtarget");
//            payByTokenDeposit(tokenDeposit, tokenDeposit.getBackOrderId()
//                    , tokenDeposit.getCostTargetCode(), tokenDeposit.getCostTargetAmount(),"back");
//            tokenDepositRepository.save(tokenDeposit);
//            return;
//        }

        payByTokenDeposit(tokenDeposit, tokenDeposit.getBackOrderId()
                    ,tokenDeposit.getCostTargetCode(), tokenDeposit.getCostTargetAmount(),"back");

        TransferStatus status = viewPayStatus(chainXService, tokenDeposit.getBackOrderId(), 32L, 32);
        if (status == TransferStatus.FAILED ||
                System.currentTimeMillis() - tokenDeposit.getUpdatedAt() > 3 * 60 * 1000)//超时
        {
            //撤销订单。此时有极小概率造成部分成交
//            syncClient.cancelOrder("iostusdt",exchangeOrderId);
            tokenDeposit.setBackStatus(TokenBackStatus.BACK_FAIL_TIMEOUT);
            tokenDeposit.setRemark(status.name());
            tokenDepositRepository.save(tokenDeposit);
            return;
        }
        if (status != TransferStatus.SUCCESS) return;

        tokenDeposit.setBackStatus(TokenBackStatus.BACKED);
        tokenDepositRepository.save(tokenDeposit);
    }

    protected boolean payByTokenDeposit(TokenDepositEntity tokenDeposit, String orderId,
                                        String code, BigDecimal amount,String remark) throws Exception {

        User user = userRepository.getOne(tokenDeposit.getUserId());
        UserApplication userApplication = userApplicationRepository.findByUserIdAndApplicationId(
                tokenDeposit.getUserId(), tokenDeposit.getApplicationId()).stream().findFirst().orElse(null);

        if (userApplication == null) return false;

        JSONObject jsonParams = new JSONObject();
        jsonParams.put("orderId", orderId);
        jsonParams.put("applicationId", tokenDeposit.getApplicationId().toString());
        jsonParams.put("reflexId", userApplication.getReflexId());
        jsonParams.put("amount", amount.toPlainString());
        jsonParams.put("currency", code);
        jsonParams.put("tokenUrl", "");
        jsonParams.put("remark", "game center token deposit[" + tokenDeposit.getId() + "]-"+remark);
        jsonParams.put("username", user.getUsername());
        logger.info("payment:{}", JSON.toJSONString(chainXService.payment(jsonParams)));

        return true;
    }

    protected TransferStatus viewPayStatus(ChainXService chainXService, String orderId, long interval,
                                           int times) throws Exception {
        JSONObject jsonParams = new JSONObject();
        jsonParams.put("orderId", orderId);
        jsonParams.put("readOnly", true);
        TransferStatus status = null;
        for (int i = 0; i < times; i++) {
            Thread.sleep(interval);
            JSONObject query = new JSONObject(chainXService.payment(jsonParams));
            status = query.get("status") != null ? TransferStatus.valueOf(
                    query.getString("status")) : TransferStatus.FAILED;
            if (status == TransferStatus.SUCCESS || status == TransferStatus.FAILED) return status;
        }
        return status;
    }
}
