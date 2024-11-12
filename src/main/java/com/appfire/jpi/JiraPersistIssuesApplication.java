package com.appfire.jpi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

@SpringBootApplication
public class JiraPersistIssuesApplication {

	public static void main(String[] args) {
		SpringApplication.run(JiraPersistIssuesApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {
			System.out.println("init()");
		};
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}

	/**
	 * Object mapper used to persist data to XML.
	 * 
	 * @return Object mapper configuration.
	 */
	@Bean(name = "xml")
	public ObjectMapper xmlObjectMapper() {
		WstxOutputFactory outputFactory = new WstxOutputFactory();

		// Pre-configure the factory to allow multi-root/fragment.
		// Workaround the SequenceWriter bug, try to call SequenceWriter.write() twice
		// you get:
		// `com.fasterxml.jackson.core.JsonGenerationException: Trying to
		// output second root, <Issue>.`.
		outputFactory.setProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_STRUCTURE, false);

		XmlFactory xmlFactory = XmlFactory
				.builder()
				.xmlOutputFactory(outputFactory)
				.build();
		return XmlMapper
				.builder(xmlFactory)
				// If there are no Issues at all we write a dummy Empty record in the output
				// xml.
				.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
				.build();
	}

	/**
	 * Object mapper used to persist data to JSON.
	 * 
	 * @return Object mapper configuration.
	 */
	@Bean(name = "json")
	@Primary
	public ObjectMapper jsonObjectMapper() {
		return new ObjectMapper()
				.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	}

	@Bean
	public ExecutorService taskExecutor() {
		final ExecutorService executor = Executors.newFixedThreadPool(5);
		return executor;
	}
}