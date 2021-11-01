package com.xychar.stateful.store;

import org.apache.commons.dbutils.BeanProcessor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class DataExtractor<T> implements ResultSetExtractor<List<T>> {
    private final Map<String, String> columnMappings = new LinkedHashMap<>();
    private final BeanProcessor processor = new BeanProcessor(columnMappings);

    private final Class<T> beanClass;

    public DataExtractor(Class<T> beanClass) {
        this.beanClass = beanClass;
    }

    public void mapColumnToProperty(String columnName, String propertyName) {
        columnMappings.put(columnName, propertyName);
    }

    @Override
    public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
        return processor.toBeanList(rs, beanClass);
    }
}
