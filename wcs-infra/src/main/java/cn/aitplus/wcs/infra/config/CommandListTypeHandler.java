package cn.aitplus.wcs.infra.config;

import cn.aitplus.wcs.core.domain.model.execution.Command;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 自定义 TypeHandler, 解决 MyBatis-Plus JacksonTypeHandler 在处理 List<Command> 时丢失泛型的问题。
 */
@MappedTypes({List.class})
public class CommandListTypeHandler extends BaseTypeHandler<List<Command>> {

    private static final ObjectMapper om = new ObjectMapper();
    private static final TypeReference<List<Command>> TYPE_REF = new TypeReference<List<Command>>() {};

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Command> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, om.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("Failed to serialize Command list", e);
        }
    }

    @Override
    public List<Command> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<Command> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<Command> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<Command> parse(String json) throws SQLException {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return om.readValue(json, TYPE_REF);
        } catch (Exception e) {
            throw new SQLException("Failed to deserialize Command list", e);
        }
    }
} 