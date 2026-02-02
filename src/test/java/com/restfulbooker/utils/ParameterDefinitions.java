package com.restfulbooker.utils;

import io.cucumber.java.ParameterType;

public class ParameterDefinitions {

    @ParameterType(".*")
    public Object data(String key) {
        return CucumberData.get(key);
    }
}