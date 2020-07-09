package com.chianxservice.crypto.chainx.gamecenter.controller;

import com.alibaba.fastjson.JSONObject;
import com.chianxservice.crypto.chainx.gamecenter.element.ControllerBase;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
@RequestMapping("/server")
public class ServerController extends ControllerBase {

    @GetMapping(value = "/info")
    public JSONObject  info(){

        return new JSONObject(userService.getServicePauseSignal());
    }

}
