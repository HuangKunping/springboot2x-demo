package com.springboot2x.demo.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {

    static final ObjectMapper mapper = new ObjectMapper();

    public static String toString(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化对象失败!", e);
        }
    }
}
