package org.dragonli.service.app.docx2html;

import org.dragonli.service.app.docx2html.utils.tohtml.DocConverter;
import org.dragonli.service.app.docx2html.utils.tohtml.DocxConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

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

	public static void main(String[] args) throws Exception {
		SpringApplication.run(Application.class, args);
//		File file1 = new File("/Users/liruifan/Documents/狸花猫/外包/项目/进行中/卫健委/空间分析0619测试发现的问题.docx");
//		InputStream file = new FileInputStream(file1);
//		String s = (new DocxConverter()).convert(file);
//		System.out.println(s.length());
	}
}
