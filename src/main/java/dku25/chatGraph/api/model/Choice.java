package dku25.chatGraph.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class Choice {
    private int index;
    private Message message;
    @JsonProperty("finish_reason")
    private String finishReason;
}
