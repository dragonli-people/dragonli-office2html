package org.dragonli.service.app.office2html.controller;

import org.dragonli.service.app.office2html.utils.tohtml.DocConverter;
import org.dragonli.service.app.office2html.utils.tohtml.DocxConverter;
import org.dragonli.service.app.office2html.utils.tohtml.PdfConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Controller
@ResponseBody
@RequestMapping("/")
public class ToHtmlController {


    @RequestMapping("/docx")
    public Map<String, Object> docx(@RequestParam("fileData") MultipartFile file) throws Exception {
        Map<String, Object> o = new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        o.put("result",result);
        result.put("value",(new DocxConverter()).convert(file.getInputStream()));
        return o;
    }

    @RequestMapping("/doc")
    public Map<String, Object> doc(@RequestParam("fileData") MultipartFile file) throws Exception {
        Map<String, Object> o = new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        o.put("result",result);
        result.put("value", (new DocConverter()).convert(file.getInputStream()));
        return o;
    }

    @RequestMapping("/pdf")
    public Map<String, Object> pdf(@RequestParam("fileData") MultipartFile file) throws Exception {
        Map<String, Object> o = new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        o.put("result",result);
        String code = (new PdfConverter()).convert(file.getInputStream());
        result.put("value", code);
//        result.put("value", "<img src=\"data:image/jpg;base64,"+code+"\"/>");
        return o;
    }
}
