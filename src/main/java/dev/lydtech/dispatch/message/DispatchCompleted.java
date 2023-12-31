package dev.lydtech.dispatch.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Created by mskwon on 2023/12/31.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DispatchCompleted {
    UUID orderId;
    String dispatchedDate;
}