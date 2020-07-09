package com.chianxservice.crypto.chainx.gamecenter.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alpacaframework.org.service.dubbo.interfaces.general.ZookeeperService;
import com.chainxservice.chainx.ChainXService;
import com.chainxservice.chainx.UserService;
import com.chainxservice.crypto.chainx.enums.TransferStatus;
import com.chainxservice.crypto.chainx.model.User;
import com.chainxservice.crypto.chainx.model.UserApplication;
import com.chainxservice.crypto.chainx.repository.DepositRepository;
import com.chainxservice.crypto.chainx.repository.UserApplicationRepository;
import com.chainxservice.crypto.chainx.repository.UserRepository;
import com.chainxservice.crypto.gamecentral.enums.ExchangeMallOrderStatus;
import com.chainxservice.crypto.gamecentral.model.ExchangeMall;
import com.chainxservice.crypto.gamecentral.model.ExchangeMallOrder;
import com.chainxservice.crypto.gamecentral.repository.ExchangeMallOrderRepository;
import com.chainxservice.crypto.gamecentral.repository.ExchangeMallRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class ExchangeOrderService {
    protected static final Logger logger = LoggerFactory.getLogger(ExchangeOrderService.class);
    @Autowired
    ExchangeMallRepository exchangeMallRepository;
    @Autowired
    ExchangeMallOrderRepository exchangeMallOrderRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    DepositRepository depositRepository;
    @Autowired
    UserApplicationRepository userApplicationRepository;
    @Autowired
    @Lazy
    ExchangeMallService exchangeMallService;
    @Reference
    protected UserService userService;
    @Reference
    protected ChainXService chainXService;
    @Reference
    protected ZookeeperService zookeeperService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JSONObject buy(Long id, BigDecimal count, Long userId, Long exchangeOrderId,BigDecimal oldPrice,boolean wait) throws Exception {

        JSONObject result = new JSONObject();
        ExchangeMall exchangeMall = exchangeMallRepository.getOne(id);
        if( oldPrice != null && oldPrice.compareTo(exchangeMall.getPrice()) < 0 ){
            result.put("errorCode", "PRICE_CHASNGED");
            result.put("result", false);
            return result;
        }

        if ( BigDecimal.ZERO.compareTo(exchangeMall.getSurplus() ) >= 0 || !exchangeMall.getStatus().isCanExchange()) {
            result.put("errorCode", "HAD_FINISH");
            result.put("result", false);
            return result;
        }

        User user = userRepository.getOne(userId);
        String orderId = "chainx--m-o-" + UUID.randomUUID().toString();
        final String lockKey = "ExchangeMallBuyLock";
        result.put("orderId", orderId);
        ExchangeMallOrder exchangeMallOrder = null;
        if (count.compareTo(exchangeMall.getSurplus()) > 0) count = exchangeMall.getSurplus();
        count = count.setScale(0, RoundingMode.FLOOR);
        BigDecimal amount = exchangeMall.getPrice().multiply(count);
        UserApplication userApplication = userApplicationRepository.findByUserIdAndApplicationId(userId,
                exchangeMall.getApplicationId()).stream().findFirst().orElse(null);

        JSONObject account = new JSONObject(userService.getUserAccount(new HashMap<String, Object>() {{
            put("applicationId", userApplication.getApplicationId());
            put("reflexId", userApplication.getReflexId());
            put("currency", exchangeMall.getCurrency());
            put("username", user.getUsername());
        }}));
        //wait状态不检查余额，因为后面不会真正发生购买，而是等充值到了再自动购买
        if (!wait && ( !account.getBoolean("result") ||
                new BigDecimal(account.getJSONObject("userAccount").getString("balance")).compareTo(amount) < 0) ) {
            logger.info("exchange_buy:{},{},{}", userApplication.getId(), amount,
                    account.getJSONObject("userAccount").getString("balance"));
            result.put("errorCode", "BALANCE_NOT_ENOUGH");
            result.put("result", false);
            return result;
        }

        String currentLockKey = lockKey + exchangeMall.getId();
        int code = (new Random()).nextInt(100000);
        if (!wait && !zookeeperService.lock(currentLockKey, code)) {
            result.put("result", false);
            result.put("errorCode", "LOCK_FAIL");
            return result;
        }

        //若xchangeOrderId不为null，则wait一定为false
        try {
            if( !wait )
                exchangeMall.setSurplus(exchangeMall.getSurplus().subtract(count));
            exchangeMallOrder = exchangeOrderId != null ?
                    exchangeMallOrderRepository.getOne(  exchangeOrderId ) : new ExchangeMallOrder();
            if(exchangeOrderId==null){
                exchangeMallOrder.setOrderId(orderId);
                exchangeMallOrder.setExchangeMallId(exchangeMall.getId());
                exchangeMallOrder.setUserId(user.getId());
                exchangeMallOrder.setAmount(amount);
                exchangeMallOrder.setApplicationId(exchangeMall.getApplicationId());
                exchangeMallOrder.setCount(count);
                exchangeMallOrder.setPrice(exchangeMall.getPrice());
                exchangeMallOrder.setCurrency(exchangeMall.getCurrency());
                exchangeMallOrder.setCode("");
                exchangeMallOrder.setFromAccountId(account.getJSONObject("userAccount").getLong("id"));
            }
            exchangeMallOrder.setStatus(wait?ExchangeMallOrderStatus.DEPOSIT_WAITING : ExchangeMallOrderStatus.BEGIN);

            //新事务，必须存入
            exchangeMallOrder = exchangeMallService.exchange(exchangeMallOrder, exchangeMall.getId(), count,wait);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("errorCode", "UNKNOW");
            result.put("result", false);
            return result;
        } finally {
            if(!wait) zookeeperService.releaseLock(currentLockKey, code);//wait状态没有上锁，也就不会解锁
        }
        if(wait){
            JSONObject after = new JSONObject();
            after.put("type","GC-EXCHANGE");
            after.put("id",exchangeMallOrder.getId());
            Map<String,Object> depositInfo = userService.getUserAccountMemo(exchangeMall.getApplicationId()
                    , userApplication.getReflexId(), exchangeMall.getCurrency(), user.getUsername());
            JSONObject memo = JSON.parseObject((String)depositInfo.get("memo"));
            memo.put("after",after);
            result.put("address",depositInfo.get("address"));
            result.put("memo",memo);
            result.put("exchangeMallOrderId", exchangeMallOrder.getId());
            result.put("result", true);
            return result;
        }

        JSONObject jsonParams = new JSONObject();
        jsonParams.put("orderId", exchangeMallOrder.getOrderId());
        jsonParams.put("applicationId", exchangeMall.getApplicationId().toString());
        jsonParams.put("reflexId", userApplication.getReflexId());
        jsonParams.put("amount", exchangeMallOrder.getAmount().negate().toPlainString());
        jsonParams.put("currency", exchangeMall.getCurrency());
        jsonParams.put("tokenUrl", "");
        jsonParams.put("remark", "game center exchange order[" + exchangeMallOrder.getId() + "]");
        jsonParams.put("username", user.getUsername());
        chainXService.payment(jsonParams);

        jsonParams = new JSONObject();
        jsonParams.put("orderId", exchangeMallOrder.getOrderId());
        jsonParams.put("readOnly", true);
        boolean successed = false;
        for (int i = 0; i < 32; i++) {
            Thread.sleep(32L);
            JSONObject query = new JSONObject(chainXService.payment(jsonParams));
            TransferStatus status = TransferStatus.valueOf(query.getString("status"));
            successed = status == TransferStatus.SUCCESS;
            if (status == TransferStatus.SUCCESS || status == TransferStatus.FAILED) break;
        }

        String grantCode = null;
        if (successed) grantCode = exchangeMallService.grantCode(exchangeMallOrder.getId(), exchangeMall.getId());

        result.put("exchangeMallOrderId", exchangeMallOrder.getId());
        result.put("code", grantCode);
        result.put("result", successed);
        return result;
    }
}
