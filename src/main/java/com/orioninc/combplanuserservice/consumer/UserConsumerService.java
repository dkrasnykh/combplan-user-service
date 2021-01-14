package com.orioninc.combplanuserservice.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orioninc.combplanuserservice.dto.UserDto;
import com.orioninc.combplanuserservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.UnexpectedRollbackException;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserConsumerService {
    private final ObjectMapper mapper;
    private final UserService userService;
    private final Properties properties;
    private volatile boolean doneConsuming = false;
    private ExecutorService executorService;

    @Value("${kafka.topic.user}")
    private String topic;

    @Autowired
    public UserConsumerService(ObjectMapper mapper, UserService userService, Properties properties) {
        this.properties = properties;
        this.mapper = mapper;
        this.userService = userService;
    }

    @PostConstruct
    public void consume() {
        startConsuming();
    }

    public void startConsuming() {
        executorService = Executors.newSingleThreadExecutor();
        Runnable consumerThread = getConsumerThread(properties);
        executorService.submit(consumerThread);
    }

    private Runnable getConsumerThread(Properties properties) {
        return () -> {
            Consumer<Long, String> consumer = null;
            try {
                consumer = new KafkaConsumer<>(properties);
                consumer.subscribe(Collections.singletonList(topic));
                while (!doneConsuming) {
                    ConsumerRecords<Long, String> records = consumer.poll(Duration.ofMillis(5000));
                    for (ConsumerRecord<Long, String> record : records) {
                        String value = record.value();
                        log.info("=> consumed {}", value);
                        UserDto userDto = readValue(value);
                        try {
                            userService.createUser(userDto);
                        } catch (UnexpectedRollbackException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (consumer != null) {
                    consumer.close();
                }
            }
        };
    }

    public void stopConsuming() throws InterruptedException {
        doneConsuming = true;
        executorService.awaitTermination(10000, TimeUnit.MILLISECONDS);
        executorService.shutdownNow();
    }

    private UserDto readValue(String value) {
        try {
            return mapper.readValue(value, UserDto.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Writing value to RequestDto failed: " + value);
        }
    }
}
