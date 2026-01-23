package com.restfulbooker.utils;

import net.serenitybdd.model.environment.EnvironmentSpecificConfiguration;
import net.thucydides.model.environment.SystemEnvironmentVariables;
import net.thucydides.model.util.EnvironmentVariables;

public class ApiConfig {

    private static final EnvironmentVariables environmentVariables = SystemEnvironmentVariables.createEnvironmentVariables();

    public static String getBaseUrl() {
        return EnvironmentSpecificConfiguration.from(environmentVariables)
                .getProperty("restapi.baseurl");
    }

    public static String getAuthEndpoint() {
        return getBaseUrl() + "/auth";
    }

    public static String getBookingEndpoint() {
        return getBaseUrl() + "/booking";
    }

    public static String getPingEndpoint() {
        return getBaseUrl() + "/ping";
    }
}