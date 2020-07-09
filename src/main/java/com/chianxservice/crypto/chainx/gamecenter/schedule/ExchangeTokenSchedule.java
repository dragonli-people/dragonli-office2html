package com.chianxservice.crypto.chainx.gamecenter.schedule;

import com.chainxservice.crypto.gamecentral.enums.ExchangeTokenStatus;
import com.chainxservice.crypto.gamecentral.enums.TokenBackStatus;
import com.chainxservice.crypto.gamecentral.model.ExchangeTokenEntity;
import com.chainxservice.crypto.gamecentral.model.TokenDepositEntity;
import com.chainxservice.crypto.gamecentral.repository.ExchangeTokenRepository;
import com.chianxservice.crypto.chainx.gamecenter.service.ExchangeTokenService;
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
public class ExchangeTokenSchedule {

    private static final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();


    @Autowired
    ExchangeTokenRepository exchangeTokenRepository;

    @Autowired
    ExchangeTokenService exchangeTokenService;

    private final static ConcurrentMap<Long, Boolean> locks = new ConcurrentHashMap<>();

    @Transactional
    @Scheduled(fixedRate = 32)
    public void tick() {

        List<ExchangeTokenStatus> query = Arrays.asList(ExchangeTokenStatus.values()).stream().filter(v -> !v.isFinished()).collect(
                Collectors.toList());
        List<ExchangeTokenEntity> list1 = exchangeTokenRepository.findAllByStatusIn(query);

        for (ExchangeTokenEntity exchangeTokenEntity : list1) {
            if (locks.containsKey(exchangeTokenEntity.getId())) continue;
            locks.put(exchangeTokenEntity.getId(), true);
            final ExchangeTokenEntity t = exchangeTokenEntity;
            cachedThreadPool.execute(() -> handleOne(t));
        }

        List<ExchangeTokenEntity> list2 = exchangeTokenRepository.findAllByBackStatus(TokenBackStatus.NEED_BACK);

        for (ExchangeTokenEntity exchangeTokenEntity : list2) {
            if (locks.containsKey(exchangeTokenEntity.getId())) continue;
            locks.put(exchangeTokenEntity.getId(), true);
            final ExchangeTokenEntity t = exchangeTokenEntity;
            cachedThreadPool.execute(() -> handleOneBack(t));
        }
//        exchangeTokenRepository.findFirstByOrderId()
    }

    private void handleOne(ExchangeTokenEntity exchangeTokenEntity) {
        Long id = exchangeTokenEntity.getId();
        try {
            switch (exchangeTokenEntity.getStatus()) {
                case INIT:
                    exchangeTokenService.handleInit(id);
                    break;
                case BEGIN_DEDUCTION_TARGET:
                    exchangeTokenService.handleBeginDeductionTarget(id);
                    break;
                case HAD_DEDUCTION_TARGET:
                    exchangeTokenService.handleHadDeductionTarget(id);
                    break;
                case HAD_ORDER:
                    exchangeTokenService.handleHadOrder(id);
                    break;
                case ORDER_HAD_SUCCESS:
                    exchangeTokenService.handleHOrderHadSuccess(id);
                    break;
                case BEGIN_REFUND_BASE:
                    exchangeTokenService.handleBeginRefundBase(id);
                    break;
            }
        } catch (Exception e) {

        } finally {
            locks.remove(id);
        }


    }

    private void handleOneBack(ExchangeTokenEntity exchangeTokenEntity) {
        Long id = exchangeTokenEntity.getId();
        try {
            exchangeTokenService.handleHadInvokeBackMoney(id);
        } catch (Exception e) {

        } finally {
            locks.remove(id);
        }
    }


}
