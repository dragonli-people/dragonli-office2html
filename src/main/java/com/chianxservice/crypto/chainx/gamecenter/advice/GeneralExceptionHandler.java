package com.chianxservice.crypto.chainx.gamecenter.advice;

import com.chianxservice.crypto.chainx.gamecenter.element.ControllerVars;
import com.chianxservice.crypto.chainx.gamecenter.element.ExceptionWithErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Component
@ControllerAdvice
public class GeneralExceptionHandler {

    protected static final Logger logger = LoggerFactory.getLogger(GeneralExceptionHandler.class);


    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public Map<String,Object> exception(HttpServletRequest request, Exception exception) {

        logger.error("excption",exception);
        Map<String,Object> map = new HashMap<>();
        map.put("error",exception.getMessage());
        if( exception instanceof ExceptionWithErrorCode)
        {
            logger.info("ExceptionWithErrorCode:{}",((ExceptionWithErrorCode)exception).getCode().name());
            map.put("errCode",((ExceptionWithErrorCode)exception).getCode().ordinal());
            map.put("errName",((ExceptionWithErrorCode)exception).getCode().name());
        }
        if(ControllerVars.startTime != null)
            logger.info("||cost time|{}|{}|",System.currentTimeMillis()-ControllerVars.startTime.get(),request.getRequestURL());
        return map;
    }

}
