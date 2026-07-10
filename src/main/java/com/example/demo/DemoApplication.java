package com.example.demo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
	CommandLineRunner commandLineRunner() {
		return args -> {
			sendMessage("order-123", "Order Created");
			sendMessageBatch();
		};
	}

	void sendMessage(String sessionId, String messageContent) {

		ServiceBusMessage message = new ServiceBusMessage(messageContent);
		message.setSessionId(sessionId);
		senderClient.sendMessage(message);
		logger.info("Sent a single message with sessionId '{}' to the topic: {}", sessionId, topicName);

	}

	void sendMessageBatch() {

		List<ServiceBusMessage> listOfMessages = createMessages();

		Map<String, List<ServiceBusMessage>> messagesBySession = listOfMessages.stream()
				.collect(Collectors.groupingBy(ServiceBusMessage::getSessionId));

		for (List<ServiceBusMessage> sessionMessages : messagesBySession.values()) {
			sendBatchForSession(sessionMessages);
		}

	}

	static List<ServiceBusMessage> createMessages() {
		
		ServiceBusMessage[] messages = {
				withSessionId(new ServiceBusMessage("First message"), "order-123"),
				withSessionId(new ServiceBusMessage("Second message"), "order-123"),
				withSessionId(new ServiceBusMessage("Third message"), "order-456")
		};
		return Arrays.asList(messages);
	}

	private static ServiceBusMessage withSessionId(ServiceBusMessage message, String sessionId) {
		message.setSessionId(sessionId);
		return message;
	}

	private void sendBatchForSession(List<ServiceBusMessage> sessionMessages) {

		ServiceBusMessageBatch messageBatch = senderClient.createMessageBatch();

		for (ServiceBusMessage message : sessionMessages) {
			if (messageBatch.tryAddMessage(message)) {
				continue;
			}

			senderClient.sendMessages(messageBatch);
			logger.info("Sent a batch of messages to the topic: {}", topicName);

			messageBatch = senderClient.createMessageBatch();

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
