package com.xychar.stateful.store;

import org.apache.commons.dbutils.BeanProcessor;
import org.mybatis.dynamic.sql.render.RenderingStrategies;
import org.mybatis.dynamic.sql.select.SelectModel;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.util.Buildable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataExtractor<T> implements ResultSetExtractor<List<T>>
        , RowMapper<T> {
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

    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        return processor.toBean(rs, beanClass);
    }

    public List<T> selectList(Buildable<SelectModel> statement, NamedParameterJdbcTemplate template) {
        SelectStatementProvider provider = statement.build().render(RenderingStrategies.SPRING_NAMED_PARAMETER);
        return template.<List<T>>query(provider.getSelectStatement(), provider.getParameters(), this);
    }

    public T selectOne(Buildable<SelectModel> statement, NamedParameterJdbcTemplate template) {
        SelectStatementProvider provider = statement.build().render(RenderingStrategies.SPRING_NAMED_PARAMETER);
        return template.queryForObject(provider.getSelectStatement(), provider.getParameters(), this);
    }
}
