package com.chianxservice.crypto.chainx.gamecenter.schedule;

import com.chainxservice.crypto.gamecentral.enums.TokenBackStatus;
import com.chainxservice.crypto.gamecentral.enums.TokenDepositStatus;
import com.chainxservice.crypto.gamecentral.model.TokenDepositEntity;
import com.chainxservice.crypto.gamecentral.repository.TokenDepositRepository;
import com.chianxservice.crypto.chainx.gamecenter.service.TokenDepositService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
public class TokenDepositSchedule {

    private static final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();


    @Autowired
    TokenDepositRepository tokenDepositRepository;

    @Autowired
    TokenDepositService tokenDepositService;

    private final static ConcurrentMap<Long,Boolean> locks = new ConcurrentHashMap<>();

    @Transactional
    @Scheduled(fixedRate = 32)
    public void tick() {

        List<TokenDepositStatus> query = Arrays.asList(TokenDepositStatus.values()).stream().filter(v->!v.isFinished()).collect(
                Collectors.toList());
        List<TokenDepositEntity> list1 = tokenDepositRepository.findAllByStatusIn(query);

        for( TokenDepositEntity tokenDepositEntity : list1 ){
            if(locks.containsKey(tokenDepositEntity.getId()))continue;
            locks.put(tokenDepositEntity.getId(),true);
            final TokenDepositEntity t = tokenDepositEntity;
            cachedThreadPool.execute( ()->handleOne(t) );
        }

        List<TokenDepositEntity> list2 = tokenDepositRepository.findAllByBackStatus(TokenBackStatus.NEED_BACK);

        for( TokenDepositEntity tokenDepositEntity : list2 ){
            if(locks.containsKey(tokenDepositEntity.getId()))continue;
            locks.put(tokenDepositEntity.getId(),true);
            final TokenDepositEntity t = tokenDepositEntity;
            cachedThreadPool.execute( ()->handleOneBack(t) );
        }
//        tokenDepositRepository.findFirstByOrderId()
    }

    private void handleOne(TokenDepositEntity tokenDepositEntity){
        Long id = tokenDepositEntity.getId();
        try {
            switch (tokenDepositEntity.getStatus()){
                case INIT:
                    tokenDepositService.handleInit(id);
                    break;
                case BEGIN_DEDUCTION_TARGET:
                    tokenDepositService.handleBeginDeductionTarget(id);
                    break;
                case HAD_DEDUCTION_TARGET:
                    tokenDepositService.handleHadDeductionTarget(id);
                    break;
                case HAD_INVOKE_GAME_API:
                    tokenDepositService.handleHadInvokeGameApi(id);
                    break;
            }
        }catch (Exception e){

        }
        finally {
            locks.remove(tokenDepositEntity.getId());
        }



    }

    private void handleOneBack(TokenDepositEntity tokenDepositEntity){
        Long id = tokenDepositEntity.getId();
        try {
            tokenDepositService.handleHadInvokeBackMoney(id);
        }catch (Exception e){

        }
        finally {
            locks.remove(tokenDepositEntity.getId());
        }
    }


}
