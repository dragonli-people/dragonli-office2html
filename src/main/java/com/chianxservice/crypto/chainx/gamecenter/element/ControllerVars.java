package com.chianxservice.crypto.chainx.gamecenter.element;

import com.chainxservice.crypto.chainx.model.AccountOwner;
import com.chainxservice.crypto.chainx.model.AssetApplication;
import com.chainxservice.crypto.chainx.model.Enterprise;
import com.chainxservice.crypto.chainx.model.User;

public class ControllerVars {
    public static final ThreadLocal<Long> startTime = new ThreadLocal<>();
    public static final ThreadLocal<String> requestId = new ThreadLocal<>();
    public static final ThreadLocal<User> currentUser = new ThreadLocal<>();
    public static final ThreadLocal<Enterprise> currentEnterprise = new ThreadLocal<>();
    public static final ThreadLocal<AccountOwner> defaultOwner = new ThreadLocal<>();
    public static final ThreadLocal<AuthDto> currentAuth = new ThreadLocal<>();
    public static final ThreadLocal<AssetApplication> requestApp = new ThreadLocal<>();

    public static final String ENV_DEV = "dev";
    public static final String ENV_TEST = "test";
}
