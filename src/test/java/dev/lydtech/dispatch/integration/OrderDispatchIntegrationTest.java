package dev.lydtech.dispatch.integration;

import dev.lydtech.dispatch.DispatchConfiguration;
import dev.lydtech.dispatch.message.DispatchCompleted;
import dev.lydtech.dispatch.message.DispatchPreparing;
import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.message.OrderDispatched;
import dev.lydtech.dispatch.util.TestEventData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static dev.lydtech.dispatch.integration.WiremockUtils.stubWiremock;
import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Created by mskwon on 2023/12/18.
 */
@Slf4j
// @TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@SpringBootTest(classes = {DispatchConfiguration.class})
@AutoConfigureWireMock(port=0)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) // 각 테스트 마다 스프링 컨텍스트가 로드되지 않음 (테스트 속도 향상)
@ActiveProfiles("test")
// @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
// @TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@EmbeddedKafka(controlledShutdown = true, kraft = false) // 내장 카프카 테스트후 자동 종료
public class OrderDispatchIntegrationTest {

    // 수신 주문 생성
    private final static String ORDER_CREATED_TOPIC = "order.created";
    // 주문 발송
    private final static String ORDER_DISPATCHED_TOPIC = "order.dispatched";
    // 발송 추적
    private final static String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";
    // 데드 레터 발송
    private final static String ORDER_CREATED_DLT_TOPIC = "order.created.DLT";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private KafkaTestListener testListener;

    @Configuration
    static class TestConfig {

        @Bean
        public KafkaTestListener testListener() {
            return new KafkaTestListener();
        }
    }

    /**
     * 수신된 이벤트 수 추적
     * Use this receiver to consume messages from the outbound topics.
     */
    @KafkaListener(groupId = "KafkaIntegrationTest", topics = { DISPATCH_TRACKING_TOPIC, ORDER_DISPATCHED_TOPIC, ORDER_CREATED_DLT_TOPIC })
    public static class KafkaTestListener {
        AtomicInteger dispatchPreparingCounter = new AtomicInteger(0);
        AtomicInteger orderDispatchedCounter = new AtomicInteger(0);
        AtomicInteger dispatchCompletedCounter = new AtomicInteger(0);
        AtomicInteger orderCreatedDLTCounter = new AtomicInteger(0);

        @KafkaHandler
        void receiveDispatchPreparing(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload DispatchPreparing payload) {
            log.debug("Received DispatchPreparing key: " + key + " - payload: " + payload);
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            dispatchPreparingCounter.incrementAndGet();
        }

        @KafkaHandler
        void receiveOrderDispatched(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload OrderDispatched payload) {
            log.debug("Received OrderDispatched key: " + key + " - payload: " + payload);
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            orderDispatchedCounter.incrementAndGet();
        }

        @KafkaHandler
        void receiveDispatchCompleted(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload DispatchCompleted payload) {
            log.debug("Received DispatchCompleted key: " + key + " - payload: " + payload);
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            dispatchCompletedCounter.incrementAndGet();
        }

        @KafkaHandler
        void receiveOrderCreatedDLT(@Header(KafkaHeaders.RECEIVED_KEY) String key, @Payload OrderCreated payload) {
            log.debug("Received OrderCreated DLT key: " + key + " - payload: " + payload);
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            orderCreatedDLTCounter.incrementAndGet();
        }
    }

    @BeforeEach
    public void setUp() {
        // 배송 추적 이벤트
        testListener.dispatchPreparingCounter.set(0);
        // 주문 배송 이벤트
        testListener.orderDispatchedCounter.set(0);
        testListener.dispatchCompletedCounter.set(0);
        testListener.orderCreatedDLTCounter.set(0);

        WiremockUtils.reset();

        // Wait until the partitions are assigned.  The application listener container has one topic and the test
        // 파티션이 할당될 때까지 기다림. 애플리케이션 리스너 컨테이너에는 하나의 주제와 테스트가 있음
        // listener container has multiple topics, so take that into account when awaiting for topic assignment.
        // 리스너 컨테이너에는 여러 주제가 있으므로 주제 할당을 기다릴 때 이를 고려
        // spring-kafka-test 3.1.0 에서 에러 발생
        // kraft 모드를 false로 놓고 테스트하면 잘됨
        registry.getAllListenerContainers()
                .forEach(container -> ContainerTestUtils.waitForAssignment(container,
                        Objects.requireNonNull(container.getContainerProperties().getTopics()).length * embeddedKafkaBroker.getPartitionsPerTopic()));
        // https://stackoverflow.com/questions/69298878/integration-test-using-embeddedkafka-containertestutil-waitforassignment-throws
        // registry.getAllListenerContainers().stream()
        //         .filter(SmartLifecycle::isAutoStartup)
        //         .forEach(container -> {
        //             log.debug("## Topic Length: {}, PartitionsPerTopic: {}", Objects.requireNonNull(container.getContainerProperties().getTopics()).length, embeddedKafkaBroker.getPartitionsPerTopic());
        //             ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
        //                     // 강의에서 제공되는 소스코드로 하면 갯수가 일치하지 않아 파티션 갯수가 충족될때까지 기다리다가 끝남
        //                     // Objects.requireNonNull(container.getContainerProperties().getTopics()).length * embeddedKafkaBroker.getPartitionsPerTopic());
        //         });
    }

    /**
     * Send in an order.created event and ensure the expected outbound events are emitted.  The call to the stock service
     * is stubbed to return a 200 Success.
     */
    @Test
    public void testOrderDispatchFlow_Success() throws Exception {
        stubWiremock("/api/stock?item=my-item", HttpStatus.OK, "true");

        OrderCreated orderCreated = TestEventData.buildOrderCreatedEvent(randomUUID(), "my-item");
        sendMessage(ORDER_CREATED_TOPIC, randomUUID().toString(), orderCreated);

        // 100ms마다 확인하면서 최대 3초를 기다림
        await().atMost(3, TimeUnit.SECONDS)
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .untilAtomic(testListener.dispatchPreparingCounter, equalTo(1));
                // .until(testListener.dispatchPreparingCounter::get, equalTo(1));
        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .untilAtomic(testListener.orderDispatchedCounter, equalTo(1));
        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .untilAtomic(testListener.dispatchCompletedCounter, equalTo(1));
        assertThat(testListener.orderCreatedDLTCounter.get(), equalTo(0));
    }

    /**
     * The call to the stock service is stubbed to return a 400 Bad Request which results in a not-retryable exception
     * being thrown, so the event is sent to the dead letter topic and the outbound events are never sent.
     */
    @Test
    public void testOrderDispatchFlow_NotRetryableException() throws Exception {
        stubWiremock("/api/stock?item=my-item", HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase());

        OrderCreated orderCreated = TestEventData.buildOrderCreatedEvent(randomUUID(), "my-item");
        sendMessage(ORDER_CREATED_TOPIC, randomUUID().toString(), orderCreated);

        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderCreatedDLTCounter::get, equalTo(1));
        assertThat(testListener.dispatchPreparingCounter.get(), equalTo(0));
        assertThat(testListener.orderDispatchedCounter.get(), equalTo(0));
        assertThat(testListener.dispatchCompletedCounter.get(), equalTo(0));
    }

    /**
     * The call to the stock service is stubbed to initially return a 503 Service Unavailable response, resulting in a
     * retryable exception being thrown.  On the subsequent attempt it is stubbed to then succeed, so the outbound events
     * are sent.
     */
    @Test
    public void testOrderDispatchFlow_RetryThenSuccess() throws Exception {
        stubWiremock("/api/stock?item=my-item", HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(), "failOnce", STARTED, "succeedNextTime");
        stubWiremock("/api/stock?item=my-item", HttpStatus.OK, "true", "failOnce", "succeedNextTime", "succeedNextTime");

        OrderCreated orderCreated = TestEventData.buildOrderCreatedEvent(randomUUID(), "my-item");
        sendMessage(ORDER_CREATED_TOPIC, randomUUID().toString(), orderCreated);

        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchPreparingCounter::get, equalTo(1));
        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderDispatchedCounter::get, equalTo(1));
        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchCompletedCounter::get, equalTo(1));
        assertThat(testListener.orderCreatedDLTCounter.get(), equalTo(0));
    }

    /**
     * The call to the stock service is stubbed to initially return a 503 Service Unavailable response.  This results in
     * retryable exceptions being thrown continually, eventually exceeding the retry limit.  The event is sent to the
     * dead letter topic, and the outbound events are never sent.
     */
    @Test
    public void testOrderDispatchFlow_RetryUntilFailure() throws Exception {
        stubWiremock("/api/stock?item=my-item", HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase());

        OrderCreated orderCreated = TestEventData.buildOrderCreatedEvent(randomUUID(), "my-item");
        sendMessage(ORDER_CREATED_TOPIC, randomUUID().toString(), orderCreated);

        await().atMost(5, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderCreatedDLTCounter::get, equalTo(1));
        assertThat(testListener.dispatchPreparingCounter.get(), equalTo(0));
        assertThat(testListener.orderDispatchedCounter.get(), equalTo(0));
        assertThat(testListener.dispatchCompletedCounter.get(), equalTo(0));
    }

    private void sendMessage(String topic, String key, Object data) throws Exception {
        SendResult<String, Object> sendResult = kafkaTemplate.send(MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.KEY, key)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build()).get();
    }
}
