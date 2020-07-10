package org.dragonli.service.app.docx2html;


import org.dragonli.service.dubbosupport.DubboApplicationBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;


import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableAsync
@EnableScheduling
public class Application implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
	@Value("${http.port}")
	int port;


	@Override
	public void customize(ConfigurableWebServerFactory factory) {
		// TODO Auto-generated method stub
		factory.setPort(this.port);
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
