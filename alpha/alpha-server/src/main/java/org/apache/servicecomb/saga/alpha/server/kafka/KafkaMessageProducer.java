/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server.kafka;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.AccidentHandleType;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.IAccidentHandlingService;
import org.apache.servicecomb.saga.alpha.core.configcenter.IConfigCenterService;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageProducer;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageRepository;
import org.apache.servicecomb.saga.alpha.core.kafka.KafkaMessage;
import org.apache.servicecomb.saga.alpha.core.kafka.KafkaMessageStatus;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Kafka message producer.
 *
 * @author Gannalyo
 * @since 2018/12/3
 */
public class KafkaMessageProducer implements IKafkaMessageProducer {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private KafkaProducer kafkaProducer;

    private IKafkaMessageRepository kafkaMessageRepository;

    @Autowired
    private IAccidentHandlingService accidentHandlingService;

    @Autowired
    private IConfigCenterService dbDegradationConfigService;

    private String topic;

    KafkaMessageProducer(IKafkaMessageRepository kafkaMessageRepository, String topic) {
        this.kafkaMessageRepository = kafkaMessageRepository;
        this.topic = topic;
    }

    @Override
    public void send(TxEvent event) {
        long a = System.currentTimeMillis();
        try {
            boolean enabled = dbDegradationConfigService.isEnabledConfig(event.instanceId(), event.category(), ConfigCenterType.BizInfoToKafka);
            if (enabled && EventType.SagaEndedEvent.name().equals(event.type())) {
                List<KafkaMessage> messageList = kafkaMessageRepository.findMessageListByGlobalTxId(event.globalTxId(), KafkaMessageStatus.INIT.toInteger());
                if (messageList != null && !messageList.isEmpty()) {
                    // To update message's status to 'sending'.
                    List<Long> idList = new ArrayList<>();
                    messageList.forEach(msg -> idList.add(msg.getId()));
                    // Cause current method is called in many places, and one message per globalTxId, hence, use a mutex of 'globalTxId' to avoid to send message repeatedly is planned. But, it could not work among distribution servers.
                    // So, we take advantage of the db-lock, just one server can do update successfully for same globalTxId in distribution and concurrence case.
                    boolean updateResult = kafkaMessageRepository.updateMessageStatusByIdListAndStatus(idList, KafkaMessageStatus.SENDING, KafkaMessageStatus.INIT);
                    if (updateResult) {
                        // send msg to kafka
                        sendMessage(event, messageList, idList);
                    }
                    LOG.info("Method 'KafkaMessageProducer.send' took {} milliseconds.", System.currentTimeMillis() - a);
                }
            }
        } catch (Exception e) {
            LOG.error("Fail to send Kafka message - localTxId = " + event.localTxId(), e);
            LOG.info("Method 'KafkaMessageProducer.send' took {} milliseconds.", System.currentTimeMillis() - a);
        }
    }

    private void sendMessage(TxEvent event, List<KafkaMessage> messageList, List<Long> idList) {
        try {
            String msgJson = new GsonBuilder().create().toJson(messageList);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, msgJson);
            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    LOG.info("Successfully to send Kafka message - globalTxId = [{}].", event.globalTxId());
                    // To update message's status to 'successful'.
                    kafkaMessageRepository.updateMessageStatusByIdList(idList, KafkaMessageStatus.SUCCESSFUL);
                } else {
                    if (exception instanceof RetriableException) {
                        // Kafka will retry automatically for some exceptions which can possible be successful by retrying.
                        LOG.info("Unsuccessfully to send Kafka message after exhausting retries - globalTxId = [{}].", event.globalTxId(), exception);
                    } else {
                        LOG.error("Unsuccessfully to send Kafka message without retries - globalTxId = [{}].", event.globalTxId(), exception);
                    }
                    // To report message to Accident Platform.
                    JsonObject jsonParams = new JsonObject();
                    jsonParams.addProperty("type", AccidentHandleType.SEND_MESSAGE_ERROR.toDescription());
                    jsonParams.addProperty("globaltxid", event.globalTxId());
                    jsonParams.addProperty("localtxid", event.localTxId());
                    jsonParams.addProperty("instanceid", event.instanceId());
                    jsonParams.addProperty("servicename", event.serviceName());
                    jsonParams.addProperty("bizinfo", msgJson);
                    accidentHandlingService.reportMsgToAccidentPlatform(jsonParams.toString());

                    // To update message's status to 'failed'.
                    kafkaMessageRepository.updateMessageStatusByIdList(idList, KafkaMessageStatus.FAILED);
                }
            });
        } catch (Exception e) {
            LOG.error("To send message to Kafka exception - globalTxId = [{}].", event.globalTxId(), e);
        }
    }

}
