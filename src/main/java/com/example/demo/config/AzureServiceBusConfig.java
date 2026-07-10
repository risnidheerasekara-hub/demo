package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

@Configuration
public class AzureServiceBusConfig {
	private static final Logger logger = LoggerFactory.getLogger(AzureServiceBusConfig.class);

	@Bean
	ServiceBusSenderClient senderClient(
			@Value("${azure.servicebus.connection-string}") String connectionString,
			@Value("${azure.servicebus.topic-name}") String topicName) {

		return new ServiceBusClientBuilder()
				.connectionString(connectionString)
				.sender()
				.topicName(topicName)
				.buildClient();
	}

	@Bean(destroyMethod = "close")
	ServiceBusProcessorClient processorClient(
			@Value("${azure.servicebus.connection-string}") String connectionString,
			@Value("${azure.servicebus.topic-name}") String topicName,
			@Value("${azure.servicebus.subscription-name}") String subscriptionName) {

		ServiceBusProcessorClient processorClient = new ServiceBusClientBuilder()
				.connectionString(connectionString)
				.sessionProcessor()
				.topicName(topicName)
				.subscriptionName(subscriptionName)
				.maxConcurrentSessions(2)
				.processMessage(context -> {
					logger.info("received the notification subscription message:{}", context.getMessage().getBody());
				})
				.processError(context -> {
					logger.error("Error occurred while processing message: {}", context.getException().getMessage());
				})
				.buildProcessorClient();

		processorClient.start();
		return processorClient;
	}

}
