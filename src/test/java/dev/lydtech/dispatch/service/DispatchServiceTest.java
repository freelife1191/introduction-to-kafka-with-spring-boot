package dev.lydtech.dispatch.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by mskwon on 2023/11/23.
 */
class DispatchServiceTest {

    private DispatchService service;

    @BeforeEach
    void setUp() {
        service = new DispatchService();
    }

    @Test
    void process() {
        service.process("payload");
    }
}