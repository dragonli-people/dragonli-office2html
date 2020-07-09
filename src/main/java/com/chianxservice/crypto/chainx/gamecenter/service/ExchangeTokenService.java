package com.chianxservice.crypto.chainx.gamecenter.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chainxservice.chainx.ChainXService;
import com.chainxservice.crypto.chainx.enums.AccountAdjustmentStatus;
import com.chainxservice.crypto.chainx.enums.TransferStatus;
import com.chainxservice.crypto.chainx.model.*;
import com.chainxservice.crypto.chainx.repository.*;
import com.chainxservice.crypto.gamecentral.enums.ExchangeTokenStatus;
import com.chainxservice.crypto.gamecentral.enums.TokenBackStatus;
import com.chainxservice.crypto.gamecentral.enums.TokenDepositStatus;
import com.chainxservice.crypto.gamecentral.model.ExchangeTokenEntity;
import com.chainxservice.crypto.gamecentral.model.TokenDepositEntity;
import com.chainxservice.crypto.gamecentral.repository.ExchangeTokenRepository;
import com.chainxservice.crypto.gamecentral.repository.TokenDepositRepository;
import com.huobi.client.SyncRequestClient;
import com.huobi.client.model.Order;
import com.huobi.client.model.enums.AccountType;
import com.huobi.client.model.enums.OrderState;
import com.huobi.client.model.enums.OrderType;
import com.huobi.client.model.request.NewOrderRequest;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ExchangeTokenService {
    @Autowired
    UserApplicationRepository userApplicationRepository;
    @Autowired
    ExchangeTokenRepository exchangeTokenRepository;
    @Autowired
    ApplicationRepository applicationRepository;
    @Autowired
    EnterpriseRepository enterpriseRepository;
    @Autowired
    AccountAdjustmentRepository accountAdjustmentRepository;
    @Autowired
    UserRepository userRepository;
    @Reference
    protected ChainXService chainXService;
    @Autowired
    SyncRequestClient huobiClient;
    protected final static Logger logger = LoggerFactory.getLogger(ExchangeTokenService.class);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleInit(Long id) throws Exception {
        ExchangeTokenEntity tokenDeposit = exchangeTokenRepository.getOne(id);
        User user = userRepository.getOne(tokenDeposit.getUserId());

        if (!adjustmentByExchange(tokenDeposit, tokenDeposit.getCostTargetOrderId(),
                tokenDeposit.getTargetCode(), tokenDeposit.getCostTargetAmount().negate(),"costTarget")) {
            tokenDeposit.setStatus(ExchangeTokenStatus.FAILED);
            tokenDeposit.setRemark("USER_APPLICATION_IS_NULL");
            exchangeTokenRepository.save(tokenDeposit);
            return;
        }

        tokenDeposit.setStatus(ExchangeTokenStatus.BEGIN_DEDUCTION_TARGET);
        exchangeTokenRepository.save(tokenDeposit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleBeginDeductionTarget(Long id) throws Exception {
        ExchangeTokenEntity tokenDeposit = exchangeTokenRepository.getOne(id);

        AccountAdjustmentStatus status = viewAdjustmentStatus(chainXService, tokenDeposit.getCostTargetOrderId(), 0L, 1);
        if (status == AccountAdjustmentStatus.FAILED || System.currentTimeMillis() - tokenDeposit.getUpdatedAt() > 30000) {
            tokenDeposit.setStatus(ExchangeTokenStatus.FAILED);
            tokenDeposit.setRemark(status == AccountAdjustmentStatus.FAILED ? "BALANCE_NOT_ENOUGH" : "COST_TARGET_TIMEOUT");
            exchangeTokenRepository.save(tokenDeposit);
            return;
        }
        if (status != AccountAdjustmentStatus.SUCCESS) return;

        tokenDeposit.setStatus(ExchangeTokenStatus.HAD_DEDUCTION_TARGET);
        exchangeTokenRepository.save(tokenDeposit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleHadDeductionTarget(Long id) throws Exception {
        //去挂单，然后卖掉
        ExchangeTokenEntity tokenDeposit = exchangeTokenRepository.getOne(id);

        String marketSymbol = tokenDeposit.getTargetCode().toLowerCase() + tokenDeposit.getBaseCode().toLowerCase();
        //持单，先改变一次状态。将挂单id记录下来。此处衔接有bug，不过概率很小或可忽略

//        BigDecimal lastPrice = huobiClient.getLastTrade(marketSymbol).getPrice();
//        BigDecimal tradePrice = tokenDeposit.getCostTargetPrice().max(
//                lastPrice.multiply(new BigDecimal("1.005"))).setScale(6, RoundingMode.FLOOR);
////        lastPrice = lastPrice.multiply(new BigDecimal("0.999")).setScale(6,RoundingMode.FLOOR);//just test
        BigDecimal amount = tokenDeposit.getCostTargetAmount().setScale(4,
                RoundingMode.FLOOR);//.multiply(lastPrice).setScale(4,RoundingMode.FLOOR);
//        System.out.println("will request:" + amount + "||" + tradePrice);
        NewOrderRequest request = new NewOrderRequest(marketSymbol, AccountType.SPOT, OrderType.SELL_MARKET,
                amount, null);//tokenDeposit.getCostTargetPrice().setScale(6, RoundingMode.FLOOR));
        long exchangeOrderId = 0L;
        try {
            exchangeOrderId = huobiClient.createOrder(request);
        } catch (Exception e) {
            logger.error("huobi order fail:" + tokenDeposit.getOrderId(), e);
            logger.info("huobi order fail:{}" + tokenDeposit.getOrderId(), e);
            tokenDeposit.setStatus(ExchangeTokenStatus.FAILED);
            tokenDeposit.setBackStatus(TokenBackStatus.NEED_BACK);
            tokenDeposit.setRemark(e.getMessage().substring(0, 1024));
            return;
        }

        System.out.println(
                "下单成功：\t" + exchangeOrderId + "\t" + System.currentTimeMillis());
        JSONObject exchangeOrderInfo = JSON.parseObject(tokenDeposit.getExchangeOrderInfo());
        exchangeOrderInfo.put("exchangeOrderId", exchangeOrderId);
        exchangeOrderInfo.put("orderTime", System.currentTimeMillis());
//        exchangeOrderInfo.put("price", tradePrice.toPlainString());
        exchangeOrderInfo.put("amount", amount.toPlainString());

        tokenDeposit.setExchangeOrderInfo(JSON.toJSONString(exchangeOrderInfo));
        tokenDeposit.setStatus(ExchangeTokenStatus.HAD_ORDER);
        exchangeTokenRepository.save(tokenDeposit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleHadOrder(Long id) throws Exception {
        //查看卖的情况
        ExchangeTokenEntity tokenDeposit = exchangeTokenRepository.getOne(id);

        String marketSymbol = tokenDeposit.getTargetCode().toLowerCase() + "usdt";
        boolean finished, isSuccessed, isHalf, isCanceled;
        JSONObject exchangeOrderInfo = JSON.parseObject(tokenDeposit.getExchangeOrderInfo());
        Long exchangeOrderId = exchangeOrderInfo.getLong("exchangeOrderId");
        long orderTime = exchangeOrderInfo.getLong("orderTime");
        Order order = huobiClient.getOrder(marketSymbol, exchangeOrderId);
        System.out.println("订单查询:\t" + exchangeOrderId + "\t" + order.getPrice() +
                "\t" + order.getFilledAmount() + "\t" + order.getState() + "\t" + System.currentTimeMillis());
        finished = OrderState.FILLED.equals(order.getState()) || OrderState.CANCELED.equals(order.getState());
        isHalf = OrderState.PARTIALCANCELED.equals(order.getState());
        isSuccessed = OrderState.FILLED.equals(order.getState());
        isCanceled = order.getState() == OrderState.CANCELED;
        if (!isCanceled && System.currentTimeMillis() - orderTime > 3 * 60 * 1000)//超时
        {
            //撤销订单。此时有极小概率造成部分成交
            System.out.println("订单取消:\t" + exchangeOrderId + "\t" + System.currentTimeMillis() + "\t" +
                    tokenDeposit.getUpdatedAt());
            huobiClient.cancelOrder(marketSymbol, exchangeOrderId);
            return;
        }
        exchangeOrderInfo.put("orderInfo", order);
        //查询，进行中，返回
        if (!finished) return;

        //如果是半成交，需要手工处理
        if (isHalf) {
            tokenDeposit.setStatus(ExchangeTokenStatus.EXCEPTION_BY_ORDER);
            tokenDeposit.setExchangeOrderInfo(JSON.toJSONString(exchangeOrderInfo));
            exchangeTokenRepository.save(tokenDeposit);
            return;
        }

        //如果完全成交
        if (isSuccessed) {
            System.out.println("订单成功:\t" + exchangeOrderId + "\t" + System.currentTimeMillis());
            tokenDeposit.setStatus(ExchangeTokenStatus.ORDER_HAD_SUCCESS);
            tokenDeposit.setExchangeOrderInfo(JSON.toJSONString(exchangeOrderInfo));
            BigDecimal finallyAmount = order.getFilledCashAmount();
            tokenDeposit.setBaseAmount(finallyAmount);
            tokenDeposit.setBaseFee(order.getFilledFees());
            finallyAmount = finallyAmount.subtract(order.getFilledFees());
            BigDecimal feeChainx = finallyAmount.multiply(new BigDecimal("0.001"));//0.01是写死的
            tokenDeposit.setBaseFeeChainx(feeChainx);
            finallyAmount = finallyAmount.subtract(feeChainx);
            tokenDeposit.setBaseFinallyAmount(finallyAmount);
            tokenDeposit.setCostTargetPrice(order.getFilledCashAmount().divide(order.getFilledAmount(),6,RoundingMode.FLOOR));
            exchangeTokenRepository.save(tokenDeposit);
            return;
        }

//        if (System.currentTimeMillis() - orderTime < 2 * 60 * 1000)return;
        System.out.println("订单被放弃:\t" + exchangeOrderId + "\t" + System.currentTimeMillis());
        //如果完成取消
        tokenDeposit.setRemark("PRICE_TIME_OUT:" + order.getState());
        tokenDeposit.setStatus(ExchangeTokenStatus.FAILED);
        tokenDeposit.setExchangeOrderInfo(JSON.toJSONString(exchangeOrderInfo));
        tokenDeposit.setBackStatus(TokenBackStatus.NEED_BACK);
        exchangeTokenRepository.save(tokenDeposit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleHOrderHadSuccess(Long id) throws Exception {
        //增加usdt
        ExchangeTokenEntity tokenDeposit = exchangeTokenRepository.getOne(id);
        if( viewAdjustmentStatus(chainXService,tokenDeposit.getAddBaseOrderId(),0,1) != null )
        {
            //容错
            tokenDeposit.setStatus(ExchangeTokenStatus.BEGIN_REFUND_BASE);
            exchangeTokenRepository.save(tokenDeposit);
            return;
        }

        adjustmentByExchange(tokenDeposit, tokenDeposit.getAddBaseOrderId()
                , tokenDeposit.getBaseCode(), tokenDeposit.getBaseFinallyAmount(),"addBase");
        tokenDeposit.setStatus(ExchangeTokenStatus.BEGIN_REFUND_BASE);
        exchangeTokenRepository.save(tokenDeposit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleBeginRefundBase(Long id) throws Exception {
        //向第三方查询订单状态，该超时超时，该成功成功
        ExchangeTokenEntity tokenDeposit = exchangeTokenRepository.getOne(id);

        AccountAdjustmentStatus status = viewAdjustmentStatus(chainXService, tokenDeposit.getAddBaseOrderId(), 0L, 1);
        if (status == AccountAdjustmentStatus.FAILED || System.currentTimeMillis() - tokenDeposit.getUpdatedAt() > 30000) {
            tokenDeposit.setStatus(ExchangeTokenStatus.FAILED);
            tokenDeposit.setRemark("FAILED_ON_ADD_BASE");
            exchangeTokenRepository.save(tokenDeposit);
            return;
        }
        if (status != AccountAdjustmentStatus.SUCCESS) return;

        tokenDeposit.setStatus(ExchangeTokenStatus.SUCCESS);
        exchangeTokenRepository.save(tokenDeposit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleHadInvokeBackMoney(Long id) throws Exception {
        ExchangeTokenEntity tokenDeposit = exchangeTokenRepository.getOne(id);
        if (tokenDeposit.getBackOrderId() == null) {
            tokenDeposit.setBackOrderId(tokenDeposit.getOrderId() + "-backtarget");
            adjustmentByExchange(tokenDeposit, tokenDeposit.getBackOrderId()
                    , tokenDeposit.getTargetCode(), tokenDeposit.getCostTargetAmount(),"back");
            exchangeTokenRepository.save(tokenDeposit);
            return;
        }

        AccountAdjustmentStatus status = viewAdjustmentStatus(chainXService, tokenDeposit.getBackOrderId(), 32L, 32);
        if (status == AccountAdjustmentStatus.FAILED ||
                System.currentTimeMillis() - tokenDeposit.getUpdatedAt() > 3 * 60 * 1000)//超时
        {
            //撤销订单。此时有极小概率造成部分成交
//            syncClient.cancelOrder("iostusdt",exchangeOrderId);
            tokenDeposit.setBackStatus(TokenBackStatus.BACK_FAIL_TIMEOUT);
            tokenDeposit.setRemark(status.name());
            exchangeTokenRepository.save(tokenDeposit);
            return;
        }
        if (status != AccountAdjustmentStatus.SUCCESS) return;

        tokenDeposit.setBackStatus(TokenBackStatus.BACKED);
        exchangeTokenRepository.save(tokenDeposit);
    }

    protected boolean adjustmentByExchange(ExchangeTokenEntity tokenDeposit, String orderId,
                                           String code, BigDecimal amount,String action) throws Exception {
        User user = userRepository.getOne(tokenDeposit.getUserId());
        UserApplication userApplication = userApplicationRepository.findByUserIdAndApplicationId(
                tokenDeposit.getUserId(), tokenDeposit.getApplicationId()).stream().findFirst().orElse(null);

        if (userApplication == null) return false;

        String remark = "system-exchange-token-" + action + "-" + user.getId() + "-" + user.getUsername() + "-" + code
                + "-" + amount.toPlainString() + "-" + tokenDeposit.getId();
        JSONObject jsonParams = new JSONObject();
        Application application = applicationRepository.getOne(tokenDeposit.getApplicationId());
        jsonParams.put("orderId", orderId);
        jsonParams.put("enterpriseId", application.getEnterpriseId());
        jsonParams.put("applicationId", tokenDeposit.getApplicationId().toString());
        jsonParams.put("reflexId", userApplication.getReflexId());
        jsonParams.put("amount", amount.toPlainString());
        jsonParams.put("currency", code);
        jsonParams.put("info", remark);
        jsonParams.put("username", user.getUsername());
        logger.info("payment:{}", JSON.toJSONString(chainXService.accountRechargeAdjustment(jsonParams)));

        return true;
    }

    protected AccountAdjustmentStatus viewAdjustmentStatus(ChainXService chainXService, String orderId, long interval,
                                                           int times) throws Exception {

        AccountAdjustment accountAdjustment = accountAdjustmentRepository.findFirstByOrderId(orderId);
        return accountAdjustment == null ? null : accountAdjustment.getStatus();
    }


}
