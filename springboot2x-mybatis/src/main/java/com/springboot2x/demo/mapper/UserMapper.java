package com.springboot2x.demo.mapper;

import com.springboot2x.demo.model.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface UserMapper {

    int add(User user);

    int add1(User user);

    User findById(int id);

    List<User> findByIds(@Param("ids") List<Integer> ids);

    List<User> findByArgs(@Param("args") List<Map<String, Object>> args);

    List<User> findAll();
}
