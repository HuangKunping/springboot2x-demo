package com.springboot2x.demo.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

@MappedTypes({String.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public class NullToEmptyStringTypeHandler extends BaseTypeHandler<String> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String s, JdbcType jdbcType) throws SQLException {
        ps.setString(i, s);
    }

    @Override
    public String getNullableResult(ResultSet rs, String s) throws SQLException {
        String value = rs.getString(s);
        return Objects.isNull(value) ? "" : value;
    }

    @Override
    public String getNullableResult(ResultSet rs, int i) throws SQLException {
        String value = rs.getString(i);
        return Objects.isNull(value) ? "" : value;
    }

    @Override
    public String getNullableResult(CallableStatement cs, int i) throws SQLException {
        String value = cs.getString(i);
        return Objects.isNull(value) ? "" : value;
    }
}
