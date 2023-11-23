package dev.lydtech.dispatch.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Created by mskwon on 2023/11/23.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreated {

    UUID orderId;

    String item;
}
