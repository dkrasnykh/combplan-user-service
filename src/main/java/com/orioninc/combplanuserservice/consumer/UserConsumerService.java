package com.orioninc.combplanuserservice.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orioninc.combplanuserservice.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Collections;

@Slf4j
@Service
public class UserConsumerService {
    private final KafkaConsumer<Long, String> consumer;
    private final ObjectMapper mapper;

    @Value("${kafka.topic.user}")
    private String topic;

    @Autowired
    public UserConsumerService(KafkaConsumer<Long, String> consumer, ObjectMapper mapper) {
        this.consumer = consumer;
        this.mapper = mapper;
    }

    @PostConstruct
    public void consume(){
        consumer.subscribe(Collections.singletonList(topic));

        try {
            while (true) {
                ConsumerRecords<Long, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<Long, String> record : records) {
                    String value = record.value();
                    log.info("=> consumed {}", value);
                    UserDto userDto = readValue(value);
                    //requestService.createRequest(requestDto);
                }
            }
        } finally {
            consumer.close();
        }
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
