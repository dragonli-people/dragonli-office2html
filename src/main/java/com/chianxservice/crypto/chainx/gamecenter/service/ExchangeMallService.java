package com.chianxservice.crypto.chainx.gamecenter.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chainxservice.crypto.chainx.model.Deposit;
import com.chainxservice.crypto.chainx.repository.DepositRepository;
import com.chainxservice.crypto.gamecentral.enums.ExchangeMallOrderStatus;
import com.chainxservice.crypto.gamecentral.model.ExchangeMall;
import com.chainxservice.crypto.gamecentral.model.ExchangeMallOrder;
import com.chainxservice.crypto.gamecentral.repository.ExchangeMallOrderRepository;
import com.chainxservice.crypto.gamecentral.repository.ExchangeMallRepository;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ExchangeMallService {

    protected final static Logger logger = LoggerFactory.getLogger(ExchangeMallService.class);

    @Autowired
    ExchangeMallRepository exchangeMallRepository;
    @Autowired
    ExchangeMallOrderRepository exchangeMallOrderRepository;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    DepositRepository depositRepository;
    @Autowired
    @Lazy
    ExchangeOrderService exchangeOrderService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExchangeMallOrder exchange(ExchangeMallOrder order, Long exchangeId, BigDecimal count,Boolean wait) {
        ExchangeMall exchangeMall = exchangeMallRepository.getOne(exchangeId);
        if(!wait )exchangeMall.setSurplus(exchangeMall.getSurplus().subtract(count));
        exchangeMallRepository.save(exchangeMall);
        order = exchangeMallOrderRepository.save(order);
        return JSON.parseObject(JSON.toJSONString(order), ExchangeMallOrder.class);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String grantCode(Long orderId, Long exchangeId) {
        ExchangeMallOrder exchangeMallOrder = exchangeMallOrderRepository.getOne(orderId);
        String code = null;
        while (true) {

            RQueue<String> queue = redissonClient.getQueue("/gamecentral/exchage/" + exchangeId);//todo 键值定义不要硬编码
            code = queue.size() == 0 ? null : queue.poll();// UUID.randomUUID().toString();
            logger.info("grant-try||key:{}||code:{}","/gamecentral/exchage/" + exchangeId,code);
            if (code == null) break;
            ExchangeMallOrder other = exchangeMallOrderRepository.findFirstByExchangeMallIdAndCode(
                    exchangeMallOrder.getExchangeMallId(), code);
            if (other == null) break;
        }
        logger.info("grant-result||key:{}||code:{}","/gamecentral/exchage/" + exchangeId,code);
        exchangeMallOrder.setCode(code == null ? "" : code);
        exchangeMallOrder.setStatus(ExchangeMallOrderStatus.GRANTED);
        exchangeMallOrderRepository.save(exchangeMallOrder);
        return code;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean autoBuyTick(Long id, Long depositId) throws Exception {
        ExchangeMallOrder order = exchangeMallOrderRepository.getOne(id);
        if (order.getDepositId() == null) {
            order.setDepositId(depositId);
            order.setStatus(ExchangeMallOrderStatus.DEPOSIT_INIT);
            order = exchangeMallOrderRepository.save(order);
        }
        if (!order.getStatus().isDepositing()) return true;
        Deposit deposit = depositRepository.getOne(order.getDepositId());
        ExchangeMallOrderStatus status = ExchangeMallOrderStatus.valueOf(
                "DEPOSIT_" + deposit.getDepositStatus().name());
        if (order.getStatus() != status) order = exchangeMallOrderRepository.save(order);
        if (status == ExchangeMallOrderStatus.DEPOSIT_FAILED || status == ExchangeMallOrderStatus.DEPOSIT_PAYMENT_FAILD)
            return true;
        if (status == ExchangeMallOrderStatus.DEPOSIT_SUCCESS) {
            JSONObject result = exchangeOrderService.buy(order.getExchangeMallId(), order.getCount(), order.getUserId(),
                    order.getId(), order.getPrice(),false);
            logger.info("autobuy||exchangeOrderId:{}||result:{}",order.getId(),JSON.toJSONString(result));
            if(result.getBoolean("result"))return true;//成功了，后续已无需要这个线程来处理
            //抢锁失败者，要再给机会
            if(!result.getBoolean("result") && "LOCK_FAIL".equals(result.getString("errorCode")) )return false;
            order.setStatus(ExchangeMallOrderStatus.FAILED);
            order.setRemark(result.getString("errorCode"));
            exchangeMallOrderRepository.save(order);
            return true;
        }
        return false;
    }
}
