package com.springboot2x.demo.model;

import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.io.Serializable;
import java.sql.Blob;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@Alias("User")
public class User implements Serializable {

    private int id;
    private String name;
    private String sex;
    private Date birthday;
    private String desc;
    private Date createTime;

    private LocalDate d1;

    private LocalDateTime d2;

    private Time d3;

    private byte[] d4;

    private int d5;
    private Integer d6;
    private boolean d7;

}
