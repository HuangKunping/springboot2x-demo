package com.springboot2x.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.web.bind.annotation.*;

@RestController
public class DynimicLoggerController {

    private static final Logger log = LoggerFactory.getLogger(DynimicLoggerController.class);

    @Autowired
    private LoggingSystem loggingSystem;

    @GetMapping(path = "/change/{name}/{level}")
    public String changedLogLevel(@PathVariable("name") String loggerName,
                                  @PathVariable("level") String logLevel) {
        LogLevel level = LogLevel.valueOf(logLevel.toUpperCase());
        loggingSystem.setLogLevel(loggerName, level);
        System.out.println("Change logger level, logger name: " + loggerName + ", log level: " + logLevel);
        return "ok";
    }

    @GetMapping(path = "/printLog")
    public String printLog() {
        log.debug("============ debug logger============");
        log.info("============ info  logger============");
        log.warn("============ warn  logger============");
        log.error("============ error logger============");
        return "ok";
    }

}
