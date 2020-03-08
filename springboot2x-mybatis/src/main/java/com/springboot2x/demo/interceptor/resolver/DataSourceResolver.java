package com.springboot2x.demo.interceptor.resolver;

import javax.sql.DataSource;

public interface DataSourceResolver {

    String resolve(DataSource dataSource);

    Class<?> resolveClass();
}
