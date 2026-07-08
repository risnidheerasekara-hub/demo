package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

@Configuration
public class AzureServiceBusConfig {
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
}
