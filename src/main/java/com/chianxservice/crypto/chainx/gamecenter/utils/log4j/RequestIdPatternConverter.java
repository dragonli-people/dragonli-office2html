package com.chianxservice.crypto.chainx.gamecenter.utils.log4j;


import com.chainxservice.crypto.chainx.model.User;
import com.chianxservice.crypto.chainx.gamecenter.element.ControllerVars;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

@SuppressWarnings("unused")
@Plugin(name = "requestId", category = PatternConverter.CATEGORY)
@ConverterKeys({"reqId"})
public class RequestIdPatternConverter extends LogEventPatternConverter {
    private RequestIdPatternConverter(@SuppressWarnings("unused") String[] options) {
        super("requestId", "");
    }

    public static RequestIdPatternConverter newInstance(final String[] options) {
        return new RequestIdPatternConverter(options);
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {

        String requestId = ControllerVars.requestId.get();
        User user = ControllerVars.currentUser.get();
        toAppendTo.append("|id u|");
        if(requestId != null) toAppendTo.append(requestId).append("-");
        if(user != null) toAppendTo.append("uid-").append(user.getId()).append("-").append("uname-").append(user.getUsername()).append(" ");
        toAppendTo.append("||msg|");
    }
}
