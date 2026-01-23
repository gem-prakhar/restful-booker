// java
package com.restfulbooker.utils;

import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestContext {

    private Map<String, Object> context = new HashMap<>();
    private Response response;
    private String authToken;
    private Integer bookingId;
    private boolean healthCheckPassed = false;

    private static class Holder {
        private static final TestContext INSTANCE = new TestContext();
    }

    public static TestContext getInstance() {
        return Holder.INSTANCE;
    }



}
