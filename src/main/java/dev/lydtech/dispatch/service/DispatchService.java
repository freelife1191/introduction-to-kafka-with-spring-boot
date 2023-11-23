package dev.lydtech.dispatch.service;

import dev.lydtech.dispatch.message.OrderCreated;
import org.springframework.stereotype.Service;

/**
 * Created by mskwon on 2023/11/23.
 */
@Service
public class DispatchService {

    public void process(OrderCreated payload) {
        // no-op
    }
}
