package com.chianxservice.crypto.chainx.gamecenter.element;

import java.math.BigDecimal;

public class ApplicationGoodsExchangeDto {

    private String key;
    private BigDecimal usdtPrice;
    private String privateKey;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public BigDecimal getUsdtPrice() {
        return usdtPrice;
    }

    public void setUsdtPrice(BigDecimal usdtPrice) {
        this.usdtPrice = usdtPrice;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}
