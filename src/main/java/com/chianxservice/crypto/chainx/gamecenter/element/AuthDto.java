package com.chianxservice.crypto.chainx.gamecenter.element;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AuthDto {

    protected static final Logger logger = LoggerFactory.getLogger(AuthDto.class);

    protected static final Random random = new Random();

    private String uniqueId;
    private long uid;
    private long eid;
    private long time;
    private String key;

    public AuthDto(String cookiesStr){
        this.uniqueId = null;
        this.uid = 0;
        this.eid = 0;
        this.time = System.currentTimeMillis();
        this.key = "";

        if(null != cookiesStr && !"".equals(cookiesStr=cookiesStr.trim())){
            JSONObject map = null;
            try{
                map =JSON.parseObject(cookiesStr);
            }catch (Exception e){logger.error("authStr parse err:",e);return ;}
            if(map == null)return;
            fromJson(map);
        }
        if(null == uniqueId)generateUniqueId();
    }

    public void generateUniqueId(){
        uniqueId = "g-"+System.currentTimeMillis() + "-" + random.nextInt(1000000);//todo 统一化
    }

    public String toJson()
    {
        return JSON.toJSONString(toMap());
    }

    public Map<String,Object> toMap()
    {
        Map<String,Object> map = new HashMap<>();
        map.put("uniqueId",uniqueId+"");
        map.put("uid",uid+"");
        map.put("eid",eid+"");
        map.put("time",time+"");
        map.put("key",key);
        return map;
    }

    public void fromJson(Map<String,Object> info)
    {
        JSONObject map = new JSONObject(info);
        this.uniqueId = map.getString("uniqueId");
        this.uid = Long.parseLong(map.get("uid").toString());
        this.eid = Long.parseLong( map.get("eid").toString() );
        this.time = Long.parseLong( map.get("time").toString() );
        this.key = map.getString("key");
    }


    public void update(String uniqueId,long uid,long eid,long time,String key){
        this.uniqueId = uniqueId;
        this.uid = uid;
        this.eid = eid;
        this.time = time;
        this.key = key;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public long getEid() {
        return eid;
    }

    public void setEid(long eid) {
        this.eid = eid;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
