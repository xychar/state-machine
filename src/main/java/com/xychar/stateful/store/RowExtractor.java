package com.xychar.stateful.store;

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
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class RowExtractor<T> implements ResultSetExtractor<List<T>> {
    private final HashMap<String, BiConsumer<T, String>> stringColumnMappings = new LinkedHashMap<>();
    private final HashMap<String, BiConsumer<T, Integer>> integerColumnMappings = new LinkedHashMap<>();

    private final Supplier<T> beanConstructor;

    public RowExtractor(Supplier<T> beanConstructor) {
        this.beanConstructor = beanConstructor;
    }

    public void mapStringProperty(String columnName, BiConsumer<T, String> setter) {
        String columnNameInUpper = StringUtils.upperCase(columnName);
        stringColumnMappings.put(columnNameInUpper, setter);
    }

    public void mapIntegerProperty(String columnName, BiConsumer<T, Integer> setter) {
        String columnNameInUpper = StringUtils.upperCase(columnName);
        integerColumnMappings.put(columnNameInUpper, setter);
    }

    @Override
    public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
        ResultSetMetaData metaData = rs.getMetaData();
        List<BiConsumer<T, String>> stringConsumers = new ArrayList<>(metaData.getColumnCount());
        for (int i = 0; i < metaData.getColumnCount(); i++) {
            String columnName = StringUtils.upperCase(metaData.getColumnName(i));
            BiConsumer<T, String> columnSetter = stringColumnMappings.get(columnName);
            stringConsumers.add(columnSetter);
        }

        List<BiConsumer<T, Integer>> integerConsumers = new ArrayList<>(metaData.getColumnCount());
        for (int i = 0; i < metaData.getColumnCount(); i++) {
            String columnName = StringUtils.upperCase(metaData.getColumnName(i));
            BiConsumer<T, Integer> columnSetter = integerColumnMappings.get(columnName);
            integerConsumers.add(columnSetter);
        }

        List<T> beanList = new ArrayList<>();
        while (rs.next()) {
            T bean = beanConstructor.get();
            for (int i = 0; i < stringConsumers.size(); i++) {
                BiConsumer<T, String> setter = stringConsumers.get(i);
                if (setter != null) {
                    setter.accept(bean, rs.getString(i));
                }
            }

            for (int i = 0; i < integerConsumers.size(); i++) {
                BiConsumer<T, Integer> setter = integerConsumers.get(i);
                if (setter != null) {
                    setter.accept(bean, rs.getInt(i));
                }
            }

            beanList.add(bean);
        }

        return beanList;
    }
}
