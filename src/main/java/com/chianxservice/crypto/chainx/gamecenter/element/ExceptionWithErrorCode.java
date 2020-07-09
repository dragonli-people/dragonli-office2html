package com.chianxservice.crypto.chainx.gamecenter.element;


public class ExceptionWithErrorCode extends Exception {
    private ErrorCode code;

    public ErrorCode getCode() {
        return code;
    }

    public ExceptionWithErrorCode(ErrorCode code){
        this.code = code;
    }
}
