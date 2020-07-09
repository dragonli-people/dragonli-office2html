package com.chianxservice.crypto.chainx.gamecenter.element;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alpacaframework.org.service.dubbo.interfaces.general.OtherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Deprecated
@Component
public class AuthValidateUtil {

    protected static final Logger logger = LoggerFactory.getLogger(AuthValidateUtil.class);

    @Reference
    protected OtherService otherService;

    public AuthValidateUtil(){

    }

    public boolean validate(AuthDto authDto,String privateKey)
    {
//        logger.info("test233 a:{}|||{}",authDto.getKey(),privateKey);
        //todo
        try {
            return null != authDto.getUniqueId() && null != authDto.getKey()
                    && authDto.getKey().equals(generate(authDto, privateKey, false, false));
        }catch (Exception e){return false;}
    }

    public String generate(AuthDto authDto,String privateKey,boolean refreshTime,boolean updateKey) throws Exception
    {
        //todo
        if( null == authDto.getUniqueId() ) authDto.setUniqueId(generateUniqueId(authDto.getEid(),authDto.getUid()));
        if( refreshTime ) authDto.setTime(System.currentTimeMillis());
        String key = authDto.getUniqueId()+"|"+authDto.getEid()+"|"+authDto.getUid()+"|"+authDto.getTime()+"|"+privateKey;
//        logger.info("test233 b:{}",key);
        String rKey = otherService.sha1(key);
        if(updateKey)authDto.setKey(rKey);
        return rKey;
    }

    public String generateUniqueId(long eid,long uid)
    {
        if( 0 != eid )
            return "E-"+eid;
        if( 0 != uid )
            return "U-"+uid;
        return "G-"+ UUID.randomUUID();
    }


}
