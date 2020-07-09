package com.chianxservice.crypto.chainx.gamecenter;

import com.alibaba.dubbo.config.spring.context.annotation.DubboComponentScan;
import com.huobi.client.RequestOptions;
import com.huobi.client.SyncRequestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;

import com.alpacaframework.org.core.service.DubboApplicationBase;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class
		,scanBasePackages={"com.alpacaframework","com.chainxservice.crypto","com.chianxservice.crypto.chainx.gamecenter"})
@DubboComponentScan(basePackages = "com.chainxservice.crypto.chainx")
@EnableAsync
@EnableScheduling
public class GameCenterApplication extends DubboApplicationBase{
	public GameCenterApplication(
			@Value("${spring.dubbo-game-center.application.name}") String applicationName,
			@Value("${spring.common.registry.address}") String registryAddr,
			@Value("${spring.dubbo-game-center.protocol.name}") String protocolName,
			@Value("${spring.dubbo-game-center.protocol.port}") Integer protocolPort,
			@Value("${spring.dubbo-game-center.scan}") String registryId,
			@Value("${micro-service-port.game-center-port}") int port
		)
	{
		super(applicationName, registryAddr, protocolName, protocolPort, registryId, port);
	}

	@Bean
	public SyncRequestClient huoBiClient(
			@Value("${spring.huobi.api.proxyUrl}") String proxyUrl,
			@Value("${spring.huobi.api.key}") String key,
			@Value("${spring.huobi.api.secret}") String secret
	){
		//todo 有待配置
		RequestOptions options = new RequestOptions();
		if( proxyUrl != null && !"".equals(proxyUrl.trim()) )
			options.setProxyUrl(proxyUrl);
		return SyncRequestClient.create(key, secret,options);
	}

	public static void main(String[] args) {
		SpringApplication.run(GameCenterApplication.class, args);
	}
}
