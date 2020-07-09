package com.chianxservice.crypto.chainx.gamecenter.schedule;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chainxservice.crypto.chainx.model.Deposit;
import com.chainxservice.crypto.chainx.repository.DepositRepository;
import com.chainxservice.crypto.gamecentral.enums.ExchangeMallOrderStatus;
import com.chainxservice.crypto.gamecentral.model.ExchangeMallOrder;
import com.chainxservice.crypto.gamecentral.repository.ExchangeMallOrderRepository;
import com.chianxservice.crypto.chainx.gamecenter.service.ExchangeMallService;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
public class AutoExchangeAfterDepositSchedule {

    private static final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    private static final ConcurrentMap<Long,Boolean> executing = new ConcurrentHashMap<>();


    @Autowired
    DepositRepository depositRepository;

    @Autowired
    ExchangeMallOrderRepository exchangeMallOrderRepository;

    @Autowired
    ExchangeMallService exchangeMallService;

    @Autowired
    RedissonClient redissonClient;

    @Value("${spring.dubbo-jackpot-service.info.redis.chainxServerPauseSignal}")
    String serverPauseSignalRedisKey;

    @Value("${spring.dubbo-jackpot-service.info.redis.chainxServerPauseSignalInfo}")
    String serverPauseSignalRedisInfoKey;

    @PostConstruct
    @Transactional
    public void start(){

        List<ExchangeMallOrder> doing = exchangeMallOrderRepository.findAllByStatusIn(
                Arrays.asList(ExchangeMallOrderStatus.DEPOSIT_INIT, ExchangeMallOrderStatus.DEPOSIT_BEGIN
                        ,ExchangeMallOrderStatus.DEPOSIT_CONFIRM, ExchangeMallOrderStatus.DEPOSIT_SUCCESS) );

        doing = doing.stream().filter(v->v.getDepositId() != null).collect(Collectors.toList());
        doing.stream().forEach(v->addWatch(v.getId(),v.getDepositId()));//初始化
        ExchangeMallOrder max = exchangeMallOrderRepository.findFirstByDepositIdNotNullOrderByDepositIdDesc();
        final Long initMaxId = max != null ? max.getDepositId() : Optional.ofNullable(
                depositRepository.findFirstByOrderByIdDesc()).map(v->v.getId()).orElse(0L);

        //定时取得
        new Thread(()->{
            long id = 0L,maxId = initMaxId;
            while (true){
                try {
                    id = tickDeposit(maxId);//尝试获取最新的充值记录，并进行解析。如果成功，id将有增加
                }catch (Exception e){
                    id = maxId + 1;//跳过有异常的
                    e.printStackTrace();
                }

                try {
                    if( id == maxId)//如果没有新的，等一等又何妨
                        Thread.sleep(10000L);
                    else//否则少休息一会儿，尽快追上最新充值记录是正经
                        Thread.sleep(32L);
                    maxId = id;
                }catch (Exception e){
                    continue;
                }
            }
        }).start();
    }

    @Transactional
    public long tickDeposit(Long maxId) throws Exception{
        Object serverSignalSource = redissonClient.getBucket(serverPauseSignalRedisKey).get();
        final int serverSignal = (null == serverSignalSource || "".equals(serverSignalSource.toString().trim()))
                ? 0 : Integer.parseInt(serverSignalSource.toString().trim());
        if(serverSignal!=0)return maxId;
        Deposit deposit = depositRepository.findFirstByIdGreaterThanOrderById(maxId);
        if(deposit == null)return maxId;
        JSONObject memo = parseMemo( deposit.getExtendedInfo() );
        if( memo == null || (memo = memo.getJSONObject("after")) == null )return deposit.getId();;
        if( !"GC-EXCHANGE".equals( memo.getString("type") ) )return deposit.getId();
        Long exchangeOrderId = memo.getLong("id");
        ExchangeMallOrder order = null;
        if(exchangeOrderId == null || null == (order = exchangeMallOrderRepository.get(exchangeOrderId)))return deposit.getId();
        if(!deposit.getUserId().equals( order.getUserId() ))return deposit.getId();
        if( !order.getStatus().isDepositing() && null != order.getDepositId() )return deposit.getId();
        //检查完毕，这笔充值对应的订单存在，且状态是对的，且确实是该用户的订单，则该用户有权支配他新充的值，无论memo的id填写为什么
        addWatch(order.getId(),deposit.getId());
        return deposit.getId();
    }

    protected JSONObject parseMemo(String info){
        if(info == null || "".equals(info=info.trim()))return null;
        try {
            JSONObject object = JSON.parseObject(info);
            return object;
        }catch (Exception e){
            return null;
        }
    }

    public void addWatch(Long orderId,Long depositeId){
        if(executing.containsKey(orderId))return;
        executing.put(orderId,true);
        cachedThreadPool.execute(()->{
            while (true){
                try {
                    if( exchangeMallService.autoBuyTick(orderId,depositeId) )break;
                    Thread.sleep(5000L);
                }catch (Exception e){
                    break;
                }
            }
        });
    }



}
