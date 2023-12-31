package dev.lydtech.dispatch.service;

import dev.lydtech.dispatch.client.StockServiceClient;
import dev.lydtech.dispatch.message.DispatchCompleted;
import dev.lydtech.dispatch.message.DispatchPreparing;
import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.message.OrderDispatched;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Created by mskwon on 2023/11/23.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {

    private static final String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";
    private static final String ORDER_DISPATCHED_TOPIC = "order.dispatched";
    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private final KafkaTemplate<String, Object> kafkaProducer;
    private final StockServiceClient stockServiceClient;

    /**
     * KafkaTemplate에 미리 설정을 해두고 카프카 메세지 전송시 사용
     * @param orderCreated
     * @throws Exception
     */
    public void process(String key, OrderCreated orderCreated) throws Exception {

        String available = stockServiceClient.checkAvailability(orderCreated.getItem());
        if(!Boolean.parseBoolean(available))
            log.info("Item " + orderCreated.getItem() + " is unavailable.");

        // DISPATCH_TRACKING_TOPIC 메세지 전송
        DispatchPreparing dispatchPreparing = DispatchPreparing.builder()
                .orderId(orderCreated.getOrderId())
                .build();
        kafkaProducer.send(DISPATCH_TRACKING_TOPIC, key, dispatchPreparing).get();

        // ORDER_DISPATCHED_TOPIC 메세지 전송
        OrderDispatched orderDispatched = OrderDispatched.builder()
                .orderId(orderCreated.getOrderId())
                .processedById(APPLICATION_ID) // 어떤 서비스 인스턴스가 이벤트를 발생시켰는지 확인
                .notes("Dispatched: " + orderCreated.getItem())
                .build();
        // 메세지를 전송하고 get을 호출하여 동기식으로 응답을 받을 수 있도록 설정
        kafkaProducer.send(ORDER_DISPATCHED_TOPIC, key, orderDispatched).get();

        DispatchCompleted dispatchCompleted = DispatchCompleted.builder()
                .orderId(orderCreated.getOrderId())
                .dispatchedDate(LocalDate.now().toString())
                .build();
        kafkaProducer.send(DISPATCH_TRACKING_TOPIC, key, dispatchCompleted).get();

        // 주어진 주제 하나의 인스턴스에만 주제 파티션이 할당되고 다른 소비자는 남음
        // 연결된 인스턴스를 중단하면 유휴 인스턴스가 자동으로 연결되고 메세지를 소비하게됨
        log.info("Sent messages: key: " + key + " - orderId: " + orderCreated.getOrderId() + " - processedById: " + APPLICATION_ID);
    }
}
