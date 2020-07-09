package com.chianxservice.crypto.chainx.gamecenter.schedule;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chainxservice.crypto.gamecentral.enums.ExchangeMallOrderStatus;
import com.chainxservice.crypto.gamecentral.model.ExchangeMallOrder;
import com.chianxservice.crypto.chainx.gamecenter.service.ExchangeOrderService;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AutoBuyExchange {

    private static final String key = "/gamecentral/autobuy/queue";

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    ExchangeOrderService exchangeOrderService;

    @PostConstruct
    @Transactional
    public void start(){


        //定时取得
        new Thread(()->{

            while (true){
                try {
                    tickAutoBuy();
                }catch (Exception e){
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(1000L);

                }catch (Exception e){
                    continue;
                }
            }
        }).start();
    }

    public void tickAutoBuy() throws Exception{
        RQueue<String> queue = redissonClient.getQueue(key);
        if( queue.size() == 0 ) return ;

        String str = queue.poll();
        if(null == str||"".equals(str=str.trim())) return ;

        JSONObject para = JSON.parseObject(str);
        Long id = para.getLong("id");
        Long uid = para.getLong("uid");
        BigDecimal count = new BigDecimal( para.getString("count") );
        exchangeOrderService.buy(id,count,uid,null,null,false);
//        var para = {id:this.paras.id,count:this.paras.count,uid:user.id
//                ,username:user.username,reflextId:this.paras.reflextId || user.id.toString() }
    }

}
