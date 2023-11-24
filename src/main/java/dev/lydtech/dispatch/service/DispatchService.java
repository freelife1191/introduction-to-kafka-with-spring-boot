package dev.lydtech.dispatch.service;

import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.message.OrderDispatched;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Created by mskwon on 2023/11/23.
 */
@Service
@RequiredArgsConstructor
public class DispatchService {

    private static final String ORDER_DISPATCHED_TOPIC = "order.dispatched";
    private final KafkaTemplate<String, Object> kafkaProducer;

    /**
     * KafkaTemplate에 미리 설정을 해두고 카프카 메세지 전송시 사용
     * @param orderCreated
     * @throws Exception
     */
    public void process(OrderCreated orderCreated) throws Exception {
        OrderDispatched orderDispatched = OrderDispatched.builder()
                .orderId(orderCreated.getOrderId())
                .build();
        // 메세지를 전송하고 get을 호출하여 동기식으로 응답을 받을 수 있도록 설정
        kafkaProducer.send(ORDER_DISPATCHED_TOPIC, orderDispatched).get();
    }
}
