package com.springboot2x.demo.interceptor.resolver;

import javax.sql.DataSource;

public class TomcatDataSourceResolver implements DataSourceResolver {
    @Override
    public String resolve(DataSource dataSource) {
        org.apache.tomcat.jdbc.pool.DataSource ds = (org.apache.tomcat.jdbc.pool.DataSource) dataSource;
        return ds.getUrl();
    }

    @Override
    public Class<?> resolveClass() {
        return org.apache.tomcat.jdbc.pool.DataSource.class;
    }
}
