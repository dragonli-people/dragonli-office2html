package com.chianxservice.crypto.chainx.gamecenter.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chianxservice.crypto.chainx.gamecenter.element.ControllerBase;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@ResponseBody
@RequestMapping("/extend/info")
public class GameCentralExtendInfoController  extends ControllerBase {

    @GetMapping("/by/keywords")
    public List<JSONObject> query(
            @RequestParam(value = "keywords")String keywords){
        return gameCentralExtendInfoRepository.findAllByKeyWordIn(Arrays.asList(keywords.split(","))).stream()
                .map(v-> v == null ? null : JSON.parseObject(JSON.toJSONString(v))).collect(Collectors.toList());
    }

}
