package io.netty.example.ex1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author meijingling
 * @date 18/11/5
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Session {
    private String userId;
    private String userName;
}
