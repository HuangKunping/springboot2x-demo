package com.springboot2x.demo;

import com.mysql.cj.jdbc.Blob;
import com.springboot2x.demo.mapper.UserMapper;
import com.springboot2x.demo.model.User;
import org.apache.ibatis.type.JdbcType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.rowset.serial.SerialBlob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
public class ApplicationTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void testAdd() {
        LocalDate localDate = LocalDate.parse("1992-04-30");
        Instant instant = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        User user = new User();
        user.setName("黄坤平");
        user.setSex("男");
        user.setBirthday(Date.from(instant));
        userMapper.add(user);
        System.out.println(user.toString());

    }

    @Test
    public void testAdd1() throws Exception {
        LocalDate localDate = LocalDate.parse("1992-04-30");
        Instant instant = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        User user = new User();
        user.setName("黄坤平\\");
        user.setSex("男\\'\"");
        user.setBirthday(Date.from(instant));
        user.setD1(LocalDate.now());
        user.setD2(LocalDateTime.now());
        user.setD3(new Time(new Date().getTime()));
        user.setD4("hkp".getBytes());
        user.setD5(3);
        user.setD6(6);
        user.setD7(true);
        userMapper.add1(user);
        System.out.println(user.toString());

    }

    @Test
    public void testFindById() {
        User user = userMapper.findById(1);
        System.out.println(user.toString());

    }

    @Test
    public void testFindByIds() {
        List<User> users = userMapper.findByIds(Arrays.asList(1, 3, 5, 7, 9));
        System.out.println(users);

    }

    @Test
    public void testFindByArgs() {
        List<Map<String, Object>> list = new ArrayList<>();
        for(int i =1; i < 10; i+=2) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", i);
            map.put("name", "黄坤平");
            list.add(map);
        }
        List<User> users = userMapper.findByArgs(list);
        System.out.println(users);

    }

    @Test
    public void testFindAll() {
        List<User> users = userMapper.findAll();
        System.out.println(users);
    }
}
