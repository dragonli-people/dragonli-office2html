package com.chianxservice.crypto.chainx.gamecenter.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chainxservice.crypto.chainx.repository.UserApplicationRepository;
import com.chainxservice.crypto.gamecentral.enums.ExchangeMallOrderStatus;
import com.chainxservice.crypto.gamecentral.enums.NoticeType;
import com.chainxservice.crypto.gamecentral.model.ExchangeMall;
import com.chainxservice.crypto.gamecentral.model.ExchangeMallOrder;
import com.chainxservice.crypto.gamecentral.model.Notice;
import com.chainxservice.crypto.gamecentral.repository.ExchangeMallOrderRepository;
import com.chainxservice.crypto.gamecentral.repository.ExchangeMallRepository;
import com.chianxservice.crypto.chainx.gamecenter.annotations.RoleUser;
import com.chianxservice.crypto.chainx.gamecenter.element.ControllerBase;
import com.chianxservice.crypto.chainx.gamecenter.element.ErrorCode;
import com.chianxservice.crypto.chainx.gamecenter.service.ExchangeMallService;
import com.chianxservice.crypto.chainx.gamecenter.service.ExchangeOrderService;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.constraints.Max;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@ResponseBody
@RequestMapping("/exchange-mall")
public class ExchangeMallController extends ControllerBase {
    @Autowired
    ExchangeMallRepository exchangeMallRepository;
    @Autowired
    ExchangeMallOrderRepository exchangeMallOrderRepository;
    @Autowired
    UserApplicationRepository userApplicationRepository;
    @Autowired
    ExchangeMallService exchangeMallService;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    ExchangeOrderService exchangeOrderService;

    @Value("${spring.dubbo-jackpot-service.info.redis.chainxServerPauseSignal}")
    String serverPauseSignalRedisKey;

    @Value("${spring.dubbo-jackpot-service.info.redis.chainxServerPauseSignalInfo}")
    String serverPauseSignalRedisInfoKey;

    @GetMapping("/exchange/list")
    public Page<JSONObject> list(@RequestParam(value = "applicationId", required = true) Long applicationId,
            @RequestParam(value = "page", required = true) Integer page,
            @RequestParam(value = "size", required = true) @Max(128) Integer size) throws Exception {

        Page<ExchangeMall> list = exchangeMallRepository.findAllByApplicationIdAndIsShowOrderByShowIndexDescIdDesc(
                applicationId, true, PageRequest.of(page, size));
        return list.map(v -> JSON.parseObject(JSON.toJSONString(v)));
    }

    @RoleUser
    @GetMapping("/order/list")
    public Page<JSONObject> orders(@RequestParam(value = "page", required = true) Integer page,
            @RequestParam(value = "size", required = true) @Max(128) Integer size) throws Exception {
        Page<ExchangeMallOrder> list = exchangeMallOrderRepository.findAllByUserIdAndStatusInOrderByIdDesc(
                this.currentUser().getId(), Arrays.asList(ExchangeMallOrderStatus.GRANTED), PageRequest.of(page, size));
        Set<Long> exchangeIdSet = list.stream().map(v -> v.getExchangeMallId()).filter(
                v -> v != null && v.longValue() != 0L).collect(Collectors.toSet());
        List<ExchangeMall> exchangeMalls = exchangeMallRepository.findAllByIdIn(exchangeIdSet);
        Map<Long, ExchangeMall> exchangeMallMap = exchangeMalls.stream().collect(HashMap::new,
                (m, v) -> m.put(v.getId(), v), HashMap::putAll);
//        logger.info("orderlist1:{}",JSON.toJSONString(exchangeIdSet));
        final Logger logger2 = logger;
        Page<JSONObject> result = list.map(v -> JSON.parseObject(JSON.toJSONString(v)));
        result.forEach(v -> {
            Long id = v.getLong("exchangeMallId");
//            logger2.info("orderlist2:{},{}",v.getLong("id"),id);
            ExchangeMall em = exchangeMallMap.containsKey(id) ? exchangeMallMap.get(id) : null;
            v.put("exchange", em == null ? null : JSON.parseObject(JSON.toJSONString(em)));
        });

        return result;
    }

    @RoleUser
    @Transactional
    @GetMapping("/view/order/status")
    public JSONObject buy(@RequestParam(value = "id") Long id) throws Exception {
        ExchangeMallOrder order = exchangeMallOrderRepository.getOne(id);
        this.assertCheck( this.currentUser().getId() == order.getUserId(), ErrorCode.UNKNOW);
        JSONObject result = new JSONObject();
        result.put("status",order.getStatus().name());
        result.put("code",order.getCode());
        return result;
    }


    @RoleUser
    @Transactional
    @GetMapping("/buy")
    public JSONObject buy(@RequestParam(value = "id") Long id,
            @RequestParam(value = "count") BigDecimal count,
            @RequestParam(value = "price",required = false) BigDecimal price,
            @RequestParam(value = "wait",required = false) Boolean wait
    ) throws Exception {
        Object serverSignalSource = redissonClient.getBucket(serverPauseSignalRedisKey).get();
        final int serverSignal = (null == serverSignalSource || "".equals(serverSignalSource.toString().trim()))
                ? 0 : Integer.parseInt(serverSignalSource.toString().trim());
        if(serverSignal!=0)return new JSONObject(new HashMap<String,Object>()
        {{
            put("errorCode", "SERVICE_PAUSING");
            put("result", false);
        }});
        wait = wait == null ? false : wait;
        return exchangeOrderService.buy(id,count,this.currentUser().getId(),null,price,wait);

        /*
        String orderId = "chainx--m-o-" + UUID.randomUUID().toString();
        final String lockKey = "ExchangeMallBuyLock";
        JSONObject result = new JSONObject();
        result.put("orderId", orderId);
        ExchangeMall exchangeMall = exchangeMallRepository.getOne(id);
        ExchangeMallOrder exchangeMallOrder = null;
        if (count.compareTo(exchangeMall.getSurplus()) > 0) count = exchangeMall.getSurplus();
        count = count.setScale(0, RoundingMode.FLOOR);
        BigDecimal amount = exchangeMall.getPrice().multiply(count);
        UserApplication userApplication = userApplicationRepository.findByUserIdAndApplicationId(
                this.currentUser().getId(), exchangeMall.getApplicationId()).stream().findFirst().orElse(null);

        User user = this.currentUser();
        if (!exchangeMall.getStatus().isCanExchange()) {
            result.put("errorCode", "HAD_FINISH");
            result.put("result", false);
            return result;
        }
        JSONObject account = new JSONObject(userService.getUserAccount(new HashMap<String, Object>() {{
            put("applicationId", userApplication.getApplicationId());
            put("reflexId", userApplication.getReflexId());
            put("currency", exchangeMall.getCurrency());
            put("username", user.getUsername());
        }}));
        if (!account.getBoolean("result") ||
                new BigDecimal(account.getJSONObject("userAccount").getString("balance")).compareTo(amount) < 0) {
            logger.info("exchange_buy:{},{},{}",userApplication.getId(),amount
                    ,account.getJSONObject("userAccount").getString("balance"));
            result.put("errorCode", "BALANCE_NOT_ENOUGH");
            result.put("result", false);
            return result;
        }

        String currentLockKey = lockKey + exchangeMall.getId();
        int code = (new Random()).nextInt(100000);
        if (!zookeeperService.lock(currentLockKey, code)) {
            result.put("result", false);
            result.put("errorCode", "LOCK_FAIL");
            return result;
        }

        try {
            exchangeMall.setSurplus(exchangeMall.getSurplus().subtract(count));
            exchangeMallOrder = new ExchangeMallOrder();
            exchangeMallOrder.setExchangeMallId(exchangeMall.getId());
            exchangeMallOrder.setUserId(this.currentUser().getId());
            exchangeMallOrder.setOrderId(orderId);
            exchangeMallOrder.setAmount(amount);
            exchangeMallOrder.setApplicationId(exchangeMall.getApplicationId());
            exchangeMallOrder.setCount(count);
            exchangeMallOrder.setPrice(exchangeMall.getPrice());
            exchangeMallOrder.setCurrency(exchangeMall.getCurrency());
            exchangeMallOrder.setCode("");
            exchangeMallOrder.setStatus(ExchangeMallOrderStatus.BEGIN);
            exchangeMallOrder.setFromAccountId(account.getJSONObject("userAccount").getLong("id"));

            //新事务，必须存入
            exchangeMallOrder = exchangeMallService.exchange(exchangeMallOrder, exchangeMall.getId(), count);
        } catch (Exception e) {
            result.put("errorCode", "UNKNOW");
            result.put("result", false);
            return result;
        } finally {
            zookeeperService.releaseLock(currentLockKey, code);
        }

        JSONObject jsonParams = new JSONObject();
        jsonParams.put("orderId", orderId);
        jsonParams.put("applicationId", exchangeMall.getApplicationId().toString());
        jsonParams.put("reflexId", userApplication.getReflexId());
        jsonParams.put("amount", amount.negate().toPlainString());
        jsonParams.put("currency", exchangeMall.getCurrency());
        jsonParams.put("tokenUrl", "");
        jsonParams.put("remark", "game center exchange order[" + exchangeMallOrder.getId() + "]");
        jsonParams.put("username", this.currentUser().getUsername());
        chainXService.payment(jsonParams);

        jsonParams = new JSONObject();
        jsonParams.put("orderId", orderId);
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
        if (successed) grantCode = exchangeMallService.grantCode(exchangeMallOrder.getId(),exchangeMall.getId());

        result.put("exchangeMallOrderId",exchangeMallOrder.getId());
        result.put("code",grantCode);
        result.put("result", successed);
        return result;
        */
    }
    
    @GetMapping("/noticeList")
    public Map<String, Object> getNoticeList() {
        List<Notice> notice = noticeRepository.findByValidateAndNoticeType(true,NoticeType.MALL);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("result", true);
        result.put("data", notice);
        return result;
    }

    @GetMapping("/exchange/intro")
    public Map<String, Object> intro(@RequestParam(value = "id", required = true) Long id) {
        Map<String,String> map = redissonClient.getMap("/gamecentral/game/info/intro" );//todo 键值定义不要硬编码
        String intro = map.get(id.toString());
        final String intro2 = intro == null ? "Server under maintenance" : intro;
        return new HashMap<String, Object>(){{
            put("intro",intro2);
        }};
    }

}
