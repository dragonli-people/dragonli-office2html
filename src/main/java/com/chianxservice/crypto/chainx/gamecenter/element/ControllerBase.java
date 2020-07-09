package com.chianxservice.crypto.chainx.gamecenter.element;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alpacaframework.org.service.dubbo.interfaces.general.OtherService;
import com.alpacaframework.org.service.dubbo.interfaces.general.ZookeeperService;
import com.chainxservice.chainx.AuthService;
import com.chainxservice.chainx.ChainXService;
import com.chainxservice.chainx.UserService;
import com.chainxservice.crypto.chainx.model.*;
import com.chainxservice.crypto.chainx.repository.*;
import com.chainxservice.crypto.gamecentral.model.TokenDepositEntity;
import com.chainxservice.crypto.gamecentral.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class ControllerBase {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected AccountAssetsRecordRepository accountAssetsRecordRepository;
    @Autowired
    protected AccountOwnerRepository accountOwnerRepository;
    @Autowired
    protected AccountsRepository accountsRepository;
    @Autowired
    protected ApplicationRepository applicationRepository;
    @Autowired
    protected AssetApplicationRepository assetApplicationRepository;
    @Autowired
    protected AssetRepository assetRepository;
    @Autowired
    protected AssetSecretRepository assetSecretRepository;
    @Autowired
    protected EnterpriseRepository enterpriseRepository;
    @Autowired
    protected UserApplicationRepository userApplicationRepository;
    @Autowired
    protected BusinessRepository businessRepository;
    @Autowired
    protected TransferRepository transferRepository;
    @Autowired
    protected BannerRepository bannerRepository;
    @Autowired
    protected HomeRepository homeRepository;
    @Autowired
    protected NoticeRepository noticeRepository;
    @Autowired
    protected GameCentralExtendInfoRepository gameCentralExtendInfoRepository;
    @Autowired
    protected TokenDepositRepository tokenDepositRepository;
    @Autowired
    protected ExchangeTokenRepository exchangeTokenRepository;

//    @Autowired
//    protected AuthValidateUtil authValidateUtil;
    @Reference
    protected AuthService authService;
    @Reference
    protected OtherService otherService;
    @Reference
    protected UserService userService;
    @Reference
    protected ChainXService chainXService;
    @Reference
    protected ZookeeperService zookeeperService;

    protected void assertCheck(boolean f, ErrorCode code) throws ExceptionWithErrorCode {
        if (f) return;
        throw new ExceptionWithErrorCode(code);
    }

    protected void putCurrentUser(User u) throws Exception {
        this.currentAuth().setUniqueId(u == null ? authService.generateUniqueId(0L, 0L) : u.getId() + "");
        this.currentAuth().setUid(u == null ? 0L : u.getId());
        this.currentAuth().setKey(null);
        Map<String,Object> result = authService.generate(this.currentAuth().toMap(), true, true);
        this.currentAuth().fromJson( (Map<String, Object>) result.get("authDto"));
        ControllerVars.currentUser.set(u);
    }

    protected void putCurrentEnterprise(Enterprise e) {
        //todo
        this.currentAuth().setKey(null);
        ControllerVars.currentEnterprise.set(e);
    }

    protected AuthDto currentAuth() {
        return ControllerVars.currentAuth.get();
    }

    protected User currentUser() {
        return ControllerVars.currentUser.get();
    }

    protected Enterprise currentEnterprise() {
        return ControllerVars.currentEnterprise.get();
    }

    protected AccountOwner defaultOwner() {
        return ControllerVars.defaultOwner.get();
    }

    protected AssetApplication requestApp() {
        return ControllerVars.requestApp.get();
    }

    protected Asset byName(String assetName) {
        return null;
    }
}
