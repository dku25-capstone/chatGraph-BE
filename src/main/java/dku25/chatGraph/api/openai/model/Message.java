package dku25.chatGraph.api.openai.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class Message {
    private String role;
    private String content;
}
