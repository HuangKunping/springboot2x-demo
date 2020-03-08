package com.springboot2x.demo.interceptor;

import com.springboot2x.demo.interceptor.resolver.DataSourceResolver;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.ArrayUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Array;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Intercepts({
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class,
                        RowBounds.class, ResultHandler.class})
})
public class MybatisSQLInterceptor implements Interceptor {

    private static final Logger logger = LoggerFactory.getLogger(MybatisSQLInterceptor.class);

    private static final String DEFAULT_TIME_PATTERN = "HH:mm:ss";
    private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";
    private static final String DEFAULT_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final Map<Class<?>, DataSourceResolver> dataSourceResolverMap = new HashMap<>();
    private static final Map<Class<?>, Field> declaredFieldsCache = new ConcurrentReferenceHashMap<>();
    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_TYPE_MAP = new IdentityHashMap<>(8);

    static {
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Boolean.class, boolean.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Byte.class, byte.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Character.class, char.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Double.class, double.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Float.class, float.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Integer.class, int.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Long.class, long.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Short.class, short.class);
    }

    private int timeoutMs = 1000;
    private Properties properties;
    private String datePattern;
    private String timePattern;
    private String dateTimePattern;

    private DateTimeFormatter dateFormatter;
    private DateTimeFormatter timeFormatter;
    private DateTimeFormatter dateTimeFormatter;

    private boolean isDefaultTimePattern;


    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        long start = System.currentTimeMillis();

        Exception ex = null;
        try {
            return invocation.proceed();
        } catch (Exception e) {
            throw (ex = e);
        } finally {

            long cost = System.currentTimeMillis() - start;
            if (timeoutMs <= cost || ex != null || logger.isInfoEnabled()) {
                SQLInfo sqlInfo = generateSQLInfo(invocation);
                sqlInfo.setCostTimeMs(cost);
                if (ex == null) {
                    logger.info("[[SQLINF]] {} [[SQLINF]]", sqlInfo);
                } else {
                    sqlInfo.setStackTrace(throwtoString(ex));
                    logger.error("[[SQLINF]] {} [[SQLINF]]", sqlInfo);
                }
            }
        }
    }


    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }


    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
        String timeoutMsStr = properties.getProperty("timeoutMs");
        if (StringUtils.hasText(timeoutMsStr)) {
            this.timeoutMs = Integer.parseInt(timeoutMsStr);
        }
        this.datePattern = properties.getProperty("datePattern", DEFAULT_DATE_PATTERN);
        this.timePattern = properties.getProperty("timePattern", DEFAULT_TIME_PATTERN);
        this.dateTimePattern = properties.getProperty("dateTimePattern", DEFAULT_DATETIME_PATTERN);

        this.dateFormatter = DateTimeFormatter.ofPattern(datePattern);
        this.timeFormatter = DateTimeFormatter.ofPattern(timePattern);
        this.dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
        this.isDefaultTimePattern = this.timePattern.equals(DEFAULT_TIME_PATTERN);

        String dataSourceResolver = properties.getProperty("dataSourceResolver");
        if (StringUtils.hasText(dataSourceResolver)) {
            Arrays.stream(dataSourceResolver.split(","))
                    .filter(x -> StringUtils.hasText(x))
                    .forEach(x -> {
                        DataSourceResolver resolver = getResolver(x.trim());
                        if (resolver != null) {
                            dataSourceResolverMap.put(resolver.resolveClass(), resolver);
                        }
                    });
        }
    }


    private SQLInfo generateSQLInfo(Invocation invocation) {

        SQLInfo sqlInfo = new SQLInfo();

        try {

            MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
            Object parameter = invocation.getArgs().length > 1 ? invocation.getArgs()[1] : null;
            BoundSql boundSql = mappedStatement.getBoundSql(parameter);
            Configuration configuration = mappedStatement.getConfiguration();

            sqlInfo.setSqlId(mappedStatement.getId());
            sqlInfo.setSqlText(parseSql(configuration, boundSql));

            Executor executor = (Executor) invocation.getTarget();
            Transaction transaction = executor.getTransaction();
            DataSource dataSource = getDataSource(transaction);

            String jdbcUrl = "";
            if (dataSource != null) {
                DataSourceResolver resolver = dataSourceResolverMap.get(dataSource.getClass());
                jdbcUrl = resolver != null ? resolver.resolve(dataSource) : "";
            }

            sqlInfo.setJdbcUrl(jdbcUrl);

        } catch (Exception e) {
            logger.warn("Get SQL Info Exception! {}", e.getMessage());
        }

        return sqlInfo;
    }

    private DataSource getDataSource(Transaction transaction) {
        try {
            Class<?> trxClass = transaction.getClass();
            Field field = declaredFieldsCache.get(trxClass);
            if (field == null) {
                field = transaction.getClass().getDeclaredField("dataSource");
            }
            if (field != null) {
                field.setAccessible(true);
                declaredFieldsCache.put(trxClass, field);
            }
            return (DataSource) field.get(transaction);
        } catch (Exception e) {
            logger.warn("Get data source Exception! {}", e.getMessage());
        }
        return null;
    }

    private static DataSourceResolver getResolver(String className) {
        try {
            return (DataSourceResolver) Class.forName(className).newInstance();
        } catch (InstantiationException e) {
            logger.warn("InstantiationException, class: {}", className);
        } catch (IllegalAccessException e) {
            logger.warn("IllegalAccessException, class: {}", className);
        } catch (ClassNotFoundException e) {
            logger.warn("ClassNotFoundException, class: {}", className);
        }
        return null;
    }

    private String parseSql(Configuration configuration, BoundSql boundSql) {
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        if (parameterMappings.size() == 0 || parameterObject == null) {
            return sql;
        }
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
            sql = sql.replaceFirst("\\?", objectToString(parameterObject));
        } else {
            MetaObject metaObject = configuration.newMetaObject(parameterObject);
            for (ParameterMapping parameterMapping : parameterMappings) {
                String propertyName = parameterMapping.getProperty();
                if (metaObject.hasGetter(propertyName)) {
                    Object obj = metaObject.getValue(propertyName);
                    sql = sql.replaceFirst("\\?", objectToString(obj));
                } else if (boundSql.hasAdditionalParameter(propertyName)) {
                    Object obj = boundSql.getAdditionalParameter(propertyName);
                    sql = sql.replaceFirst("\\?", objectToString(obj));
                }
            }
        }
        return sql;
    }

    private String objectToString(Object o) {
        if (o == null) {
            return "null";
        }
        if (o instanceof String) {
            return escape((String) o);
        }

        if (isPrimitiveOrWrapper(o.getClass())) {
            if (o instanceof Boolean) {
                return (Boolean) o ? "1" : "0";
            }
            return o.toString();
        }
        if (o instanceof Time) {
            if (isDefaultTimePattern) {
                return escape(o.toString());
            }
            String value = formatDate((Date) o, timeFormatter);
            return escape(value);
        }
        if (o instanceof Date) {
            String value = formatDate((Date) o, dateFormatter);
            return escape(value);
        }
        if (o instanceof LocalDate) {
            String value = dateFormatter.format((LocalDate) o);
            return escape(value);
        }
        if (o instanceof LocalDateTime) {
            String value = dateTimeFormatter.format((LocalDateTime) o);
            return escape(value);
        }
        if (o instanceof Array) {
            try {
                return ArrayUtil.toString(((Array) o).getArray());
            } catch (SQLException e) {
                return o.toString();
            }
        }
        if (o.getClass().isArray()) {
            return ArrayUtil.toString(o);
        }
        return o.toString();
    }

    private static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return (clazz.isPrimitive() || PRIMITIVE_WRAPPER_TYPE_MAP.containsKey(clazz));
    }

    private static String formatDate(Date date, DateTimeFormatter formatter) {
        return date.toInstant().atZone(ZoneId.systemDefault()).format(formatter);
    }

    private static String escape(String x) {
        if (x == null) {
            return "null";
        }
        int stringLength = x.length();
        if (isEscapeNeededForString(x, x.length())) {
            StringBuilder buf = new StringBuilder((int) (x.length() * 1.1));
            buf.append('\'');
            for (int i = 0; i < stringLength; ++i) {
                char c = x.charAt(i);
                switch (c) {
                    case 0:
                        buf.append('\\');
                        buf.append('0');
                        break;
                    case '\n':
                        buf.append('\\');
                        buf.append('n');
                        break;
                    case '\r':
                        buf.append('\\');
                        buf.append('r');
                        break;
                    case '\\':
                        buf.append('\\').append('\\');
                        buf.append('\\').append('\\');
                        break;
                    case '\'':
                        buf.append('\'');
                        buf.append('\'');
                        break;
                    case '"':
                        buf.append('\\');
                        buf.append('"');
                        break;
                    case '\032':
                        buf.append('\\');
                        buf.append('Z');
                        break;
                    default:
                        buf.append(c);
                }
            }
            buf.append('\'');
            return buf.toString();
        }
        return "'" + x + "'";

    }

    private static boolean isEscapeNeededForString(String x, int stringLength) {
        boolean needsHexEscape = false;
        for (int i = 0; i < stringLength; ++i) {
            char c = x.charAt(i);
            switch (c) {
                case 0:
                case '\n':
                case '\r':
                case '\\':
                case '\'':
                case '"':
                case '\032':
                    needsHexEscape = true;
                    break;
            }
            if (needsHexEscape) {
                break;
            }
        }
        return needsHexEscape;
    }

    private static String throwtoString(Throwable e) {
        if (e == null) {
            return "";
        }
        StringWriter w = new StringWriter();
        PrintWriter p = new PrintWriter(w);
        try {
            e.printStackTrace(p);
            return w.toString();
        } finally {
            p.close();
        }
    }

    private static class SQLInfo {
        private String sqlId = "";
        private String sqlText = "";
        private String jdbcUrl = "";
        private long costTimeMs;
        private String stackTrace = "";

        public String getSqlId() {
            return sqlId;
        }

        public void setSqlId(String sqlId) {
            this.sqlId = sqlId;
        }

        public String getSqlText() {
            return sqlText;
        }

        public void setSqlText(String sqlText) {
            this.sqlText = sqlText;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public long getCostTimeMs() {
            return costTimeMs;
        }

        public void setCostTimeMs(long costTimeMs) {
            this.costTimeMs = costTimeMs;
        }

        public String getStackTrace() {
            return stackTrace;
        }

        public void setStackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("{");
            sb.append("sqlId='").append(sqlId).append('\'');
            sb.append(", sqlText='").append(sqlText).append('\'');
            sb.append(", jdbcUrl='").append(jdbcUrl).append('\'');
            sb.append(", costTimeMs=").append(costTimeMs);
            sb.append(", stackTrace='").append(stackTrace).append('\'');
            sb.append("}");
            return sb.toString();
        }
    }
}
