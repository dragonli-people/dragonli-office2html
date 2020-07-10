package org.dragonli.service.app.docx2html.controller;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
@ResponseBody
@RequestMapping("/")
public class ToHtmlController {


    @RequestMapping("/docx")
    public Map<String, Object> docx() throws Exception {
        Map<String, Object> result = new HashMap<>();

        result.put("result", true);

        return result;
    }

}
