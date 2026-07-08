package com.example.demo;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.azure.messaging.servicebus.*;

@SpringBootApplication
public class DemoApplication {
	@Value("${azure.servicebus.connection-string}")
    private String connectionString;

    @Value("${azure.servicebus.topic-name}")
    private String topicName;


	public static void main(String[] args) throws InterruptedException {
		SpringApplication.run(DemoApplication.class, args);
		DemoApplication app = new DemoApplication();
		app.sendMessage();

	}

	void sendMessage() {
		// create a Service Bus Sender client for the topic
		ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
				.connectionString(connectionString)
				.sender()
				.topicName(topicName)
				.buildClient();

		// send one message to the topic
		senderClient.sendMessage(new ServiceBusMessage("Hello, World!"));
		System.out.println("Sent a single message to the topic: " + topicName);
		senderClient.close();
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
		// create a Service Bus Sender client for the topic
		ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
				.connectionString(connectionString)
				.sender()
				.topicName(topicName)
				.buildClient();

		// Creates an ServiceBusMessageBatch where the ServiceBus.
		ServiceBusMessageBatch messageBatch = senderClient.createMessageBatch();

		// create a list of messages
		List<ServiceBusMessage> listOfMessages = createMessages();

		// We try to add as many messages as a batch can fit based on the maximum size
		// and send to Service Bus when
		// the batch can hold no more messages. Create a new batch for next set of
		// messages and repeat until all
		// messages are sent.
		for (ServiceBusMessage message : listOfMessages) {
			if (messageBatch.tryAddMessage(message)) {
				continue;
			}

			// The batch is full, so we create a new batch and send the batch.
			senderClient.sendMessages(messageBatch);
			System.out.println("Sent a batch of messages to the topic: " + topicName);

			// create a new batch
			messageBatch = senderClient.createMessageBatch();

			// Add that message that we couldn't before.
			if (!messageBatch.tryAddMessage(message)) {
				System.err.printf("Message is too large for an empty batch. Skipping. Max size: %s.",
						messageBatch.getMaxSizeInBytes());
			}
		}

		if (messageBatch.getCount() > 0) {
			senderClient.sendMessages(messageBatch);
			System.out.println("Sent a batch of messages to the topic: " + topicName);
		}

		// close the client
		senderClient.close();
	}

}
