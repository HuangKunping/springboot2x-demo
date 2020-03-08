package com.springboot2x.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeTest {

    private static Logger logger = LoggerFactory.getLogger(TypeTest.class);

    public static void main(String[] args) {
        logger.info("111=> {}, {}", "hkp", "456", new RuntimeException("xxx"));

    }
}
