package com.chianxservice.crypto.chainx.gamecenter.utils;

import com.alibaba.fastjson.JSON;
import com.chianxservice.crypto.chainx.gamecenter.element.ApplicationGoodsExchangeDto;

import java.util.*;

public class TokenDepositUtil {


    public static final Map<String, ApplicationGoodsExchangeDto> goodsExchangeConfig = new HashMap() {{
        put("3-GOLD",
                JSON.parseObject(JSON.toJSONString(new HashMap(){{
                    put("key","GOLD");put("usdtPrice","0.14");put("privateKey","K#&jFT7bb3njElIh");
                }}),
                ApplicationGoodsExchangeDto.class));
    }};

    public static final Set<String> supportTargetCodes = new HashSet<>(Arrays.asList("IOST"));
    public static final Set<String> supportBaseCodes = new HashSet<>(Arrays.asList("USDT"));
}
