package com.chianxservice.crypto.chainx.gamecenter.utils.log4j;


import com.alibaba.fastjson.JSON;
import com.chainxservice.crypto.chainx.model.User;
import com.chianxservice.crypto.chainx.gamecenter.element.AuthDto;
import com.chianxservice.crypto.chainx.gamecenter.element.ControllerVars;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

@SuppressWarnings("unused")
@Plugin(name = "auth", category = PatternConverter.CATEGORY)
@ConverterKeys({"auth"})
public class AuthPatternConverter extends LogEventPatternConverter {
    private AuthPatternConverter(@SuppressWarnings("unused") String[] options) {
        super("auth", "");
    }

    public static AuthPatternConverter newInstance(final String[] options) {
        return new AuthPatternConverter(options);
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        AuthDto authDto = ControllerVars.currentAuth.get();
        toAppendTo.append("|");
        if(authDto==null){
            toAppendTo.append("[auth is null]");
            return;
        }
        toAppendTo.append(JSON.toJSONString(authDto));
    }
}
