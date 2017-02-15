package com.softwaremill.kmq.example.standalone;

import com.softwaremill.kmq.KmqClient;
import com.softwaremill.kmq.example.UncaughtExceptionHandling;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteBufferDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.softwaremill.kmq.example.standalone.StandaloneConfig.*;
import static com.softwaremill.kmq.example.standalone.StandaloneSender.TOTAL_MSGS;

class StandaloneProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(StandaloneProcessor.class);

    public static void main(String[] args) throws InterruptedException, IOException {
        UncaughtExceptionHandling.setup();

        KmqClient<ByteBuffer, ByteBuffer> kmqClient = new KmqClient<>(KMQ_CONFIG,
                StandaloneProcessor::processMessage, Clock.systemDefaultZone(), KAFKA_CLIENTS,
                ByteBufferDeserializer.class, ByteBufferDeserializer.class);

        kmqClient.start();
    }

    private static Random random = new Random();
    private static Map<Integer, Integer> processedMessages = new ConcurrentHashMap<>();
    private static AtomicInteger totalProcessed = new AtomicInteger(0);
    private static boolean processMessage(ConsumerRecord<ByteBuffer, ByteBuffer> rawMsg) {
        int msg = rawMsg.value().getInt();
        // 10% of the messages are dropped
        if (random.nextInt(10) != 0) {
            // Sleeping up to 2.5 seconds
            LOG.info("Processing message: " + msg);
            try {
                Thread.sleep(random.nextInt(25)*100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Integer previous = processedMessages.put(msg, msg);
            if (previous != null) {
                LOG.warn(String.format("Message %d was already processed!", msg));
            }

            int total = totalProcessed.incrementAndGet();
            LOG.info(String.format("Done processing message: %d. Total processed: %d.", msg, total));

            return true;
        } else {
            LOG.info("Dropping message: " + msg);
            return false;
        }
    }
}
