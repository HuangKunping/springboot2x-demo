package com.springboot2x.demo.interceptor.resolver;

import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class HikariDataSourceResolver implements DataSourceResolver {

    @Override
    public String resolve(DataSource dataSource) {
        HikariDataSource ds = (HikariDataSource) dataSource;
        return ds.getJdbcUrl();
    }

    @Override
    public Class<?> resolveClass() {
        return HikariDataSource.class;
    }
}
