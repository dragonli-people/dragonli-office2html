package com.chianxservice.crypto.chainx.gamecenter.element;

public enum ErrorCode {
    UNKNOW,
	PARAS_IS_NULL, //参数错误  1
	USERORNAME_REPEAT, //用户名或昵称重复    2 
    EMAIL_HAD_BE_USED, // 3
    USER_NOT_FOUND, //用户不存在4
    USERORPASSWORD_WRONG, //用户名或密码错误5
    APPLICATION_REPEAT, //app重复6
    ENTERPRISE_NOT_FOUND, //企业不存在7
    APPLICATION_NOT_FOUND, //app 不存在8
    ENTERPRISE_IS_USED, //企业已存在
    USER_NOT_LOGIN,
    USER_NEVER_PLAY_IN_GAME,//玩家从没玩过这款游戏
    VALIDATECODE_WRONG,
    PERMISSION_DENIED,
}
