package com.loblaw.metrics.config;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loblaw.metrics.service.CounterInfoService;
import com.loblaw.metrics.service.impl.CounterInfoServiceImpl;
import com.loblaw.metrics.shared.util.FileUtil;
import com.loblaw.metrics.shared.util.NumberUtil;
import com.loblaw.metrics.shared.util.RestCallUtil;
import com.loblaw.metrics.shared.util.ServerUtil;
import com.loblaw.metrics.shared.util.StringUtil;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

@Configuration
public class SmlConfiguration {
	private static final String ROLLING_APPENDER_LOGGER = "rolling-appender";
	private static final String DATASOURCE_DRIVER = "datasource.driver";
	private static final String DATASOURCE_URL = "datasource.url";
	private static final String DATASOURCE_USERNAME = "datasource.username";
	private static final String DATASOURCE_PASSWORD = "datasource.password";

	@Autowired
	Environment env;

	@Value("${rest-template.timeout.read}")
	private Integer readTimeoutS;

	@Value("${rest-template.timeout.connect}")
	private Integer connectTimeoutMs;

	@Bean("in-counter")
	public CounterInfoService inCounter() {
		return new CounterInfoServiceImpl();
	}

	@Bean("out-counter")
	public CounterInfoService outCounter() {
		return new CounterInfoServiceImpl();
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	public Logger logger() {
		Logger logger = LogManager.getLogger(ROLLING_APPENDER_LOGGER);
		return logger;
	}

	@Bean
	public FileUtil fileUtil() {
		return new FileUtil();
	}

	@Bean
	public StringUtil stringUtil() {
		return new StringUtil();
	}

	@Bean
	public NumberUtil numberUtil() {
		return new NumberUtil();
	}

	@Bean
	public ServerUtil serverUtil() {
		return new ServerUtil();
	}

	@Bean
	public RestCallUtil restCallUtil() {
		return new RestCallUtil();
	}

	@Bean
	public WebClient webClient() {

		HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
				.doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutS))
						.addHandlerLast(new WriteTimeoutHandler(readTimeoutS)));

		ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

		return WebClient.builder().clientConnector(connector).build();
	}

	@Bean
	public DataSource dataSource() {
		if (env.containsProperty(DATASOURCE_URL) && env.containsProperty(DATASOURCE_USERNAME)
				&& env.containsProperty(DATASOURCE_PASSWORD)) {
			DriverManagerDataSource dataSource = new DriverManagerDataSource();

			dataSource.setDriverClassName(env.getProperty(DATASOURCE_DRIVER));
			dataSource.setUrl(env.getProperty(DATASOURCE_URL));
			dataSource.setUsername(env.getProperty(DATASOURCE_USERNAME));
			dataSource.setPassword(env.getProperty(DATASOURCE_PASSWORD));
			return dataSource;
		}

		else {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
		}
	}
}
