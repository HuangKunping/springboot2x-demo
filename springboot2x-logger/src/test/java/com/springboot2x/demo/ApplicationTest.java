package com.springboot2x.demo;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(value = SpringRunner.class)
@SpringBootTest(classes = {Application.class})
public class ApplicationTest {

    static final String changeUrl = "http://localhost:8080/change/{1}/{2}";
    static final String printLogUrl = "http://localhost:8080/printLog";

    @Autowired
    private RestTemplate restTemplate;

    @Test
    public void testChangedLogLevel() {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        Map<String, String> params = new HashMap<>();
//        params.put("name", "root");
//        params.put("level", "info");
//        HttpEntity<Map<String, String>> request = new HttpEntity<>(params, headers);

//        String response = restTemplate.postForObject(changeUrl, request, String.class);
        String response = restTemplate.getForObject(changeUrl, String.class, "root", "warn");
        Assert.assertEquals("ok", response);
    }

    @Test
    public void testPrintLog() {
        String response = restTemplate.getForObject(printLogUrl, String.class);
        Assert.assertEquals("ok", response);
    }
}
