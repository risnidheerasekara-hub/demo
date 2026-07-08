package com.example.demo;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.azure.messaging.servicebus.*;

@SpringBootApplication
public class DemoApplication {

	private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);

	@Value("${azure.servicebus.topic-name}")
	private String topicName;

	private final ServiceBusSenderClient senderClient;

	DemoApplication(ServiceBusSenderClient senderClient) {
		this.senderClient = senderClient;
	}

	public static void main(String[] args) throws InterruptedException {
		SpringApplication.run(DemoApplication.class, args);
	}

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

	@Bean
	CommandLineRunner commandLineRunner() {
		return args -> {
			sendMessage();
			sendMessageBatch();
		};
	}

	void sendMessage() {

		ServiceBusMessage message = new ServiceBusMessage("Order Created");
		senderClient.sendMessage(message);
		logger.info("Sent a single message to the topic: {}", topicName);

	}

	static List<ServiceBusMessage> createMessages() {
		// create a list of messages and return it to the caller
		ServiceBusMessage[] messages = {
				new ServiceBusMessage("First message"),
				new ServiceBusMessage("Second message"),
				new ServiceBusMessage("Third message")
		};
		return Arrays.asList(messages);
	}

	void sendMessageBatch() {

		// Creates an ServiceBusMessageBatch where the ServiceBus.
		ServiceBusMessageBatch messageBatch = senderClient.createMessageBatch();

		// create a list of messages
		List<ServiceBusMessage> listOfMessages = createMessages();

		for (ServiceBusMessage message : listOfMessages) {
			if (messageBatch.tryAddMessage(message)) {
				continue;
			}

			// The batch is full, so we create a new batch and send the batch.
			senderClient.sendMessages(messageBatch);
			logger.info("Sent a batch of messages to the topic: {}", topicName);

			// create a new batch
			messageBatch = senderClient.createMessageBatch();

			// Add that message that we couldn't before.
			if (!messageBatch.tryAddMessage(message)) {
				logger.error("Message is too large for an empty batch. Skipping. Max size: {}",
						messageBatch.getMaxSizeInBytes());
			}
		}

		if (messageBatch.getCount() > 0) {
			senderClient.sendMessages(messageBatch);
			logger.info("Sent a batch of messages to the topic: {}", topicName);
		}

	}

}
