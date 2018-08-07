package core.common.jdbc;


import com.alibaba.fastjson.util.FieldInfo;
import com.alibaba.fastjson.util.TypeUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;

import core.common.util.BeanUtils;
import core.common.util.Page;
import core.common.util.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.Assert;

public abstract class JdbcBaseDao {
    private static Logger log = Logger.getLogger(JdbcBaseDao.class);
    private String tableName = "";
    private int shards = 1;
    private SimpleJdbcTemplate jdbcTemplateWrite;
    private SimpleJdbcTemplate jdbcTemplateReadOnly;
    private DataSource dataSourceReadOnly;
    private DataSource dataSourceWrite;

    public JdbcBaseDao() {
    }

    public void setShards(int shards) {
        if (shards > 1) {
            this.shards = shards;
        }

    }

    public String getTableName() {
        return this.tableName;
    }

    public int getShards() {
        return this.shards;
    }

    public DataSource getDataSourceReadOnly() {
        return this.dataSourceReadOnly;
    }

    public DataSource getDataSourceWrite() {
        return this.dataSourceWrite;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTable(long id) {
        Assert.hasText(this.tableName, "代码中没有设置tableName,请检查代码.");
        return this.tableName + "_" + id % (long)this.shards;
    }

    public void setDataSourceWrite(DataSource dataSourceWrite) {
        this.dataSourceWrite = dataSourceWrite;
        this.jdbcTemplateWrite = new SimpleJdbcTemplate(dataSourceWrite);
    }

    public void setDataSourceReadOnly(DataSource dataSourceReadOnly) {
        this.dataSourceReadOnly = dataSourceReadOnly;
        this.jdbcTemplateReadOnly = new SimpleJdbcTemplate(dataSourceReadOnly);
    }

    protected SimpleJdbcTemplate jdbcTemplateReadOnly() {
        return this.jdbcTemplateReadOnly;
    }

    protected SimpleJdbcTemplate jdbcTemplateWrite() {
        return this.jdbcTemplateWrite;
    }

    protected <T> T populate(ResultSet rs, T obj) {
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int colCount = metaData.getColumnCount();
            Field[] fields = obj.getClass().getDeclaredFields();

            for(int i = 0; i < fields.length; ++i) {
                Field f = fields[i];

                for(int j = 1; j <= colCount; ++j) {
                    Object value = rs.getObject(j);
                    String colName = metaData.getColumnName(j);
                    if (f.getName().equalsIgnoreCase(colName)) {
                        try {
                            BeanUtils.copyProperty(obj, f.getName(), value);
                        } catch (Exception var12) {
                            log.warn("BeanUtils.copyProperty error, field name: " + f.getName() + ", error: " + var12);
                        }
                    }
                }
            }
        } catch (Exception var13) {
            log.warn("populate error...." + var13);
        }

        return obj;
    }

    protected <T> T queryForObject(String sql, RowMapper<T> mapper, Object... args) {
        List<T> results = this.jdbcTemplateReadOnly().query(sql, mapper, args);
        return DataAccessUtils.singleResult(results);
    }

    public byte[] getBlobColumn(ResultSet rs, int columnIndex) throws SQLException {
        try {
            Blob blob = rs.getBlob(columnIndex);
            if (blob == null) {
                return null;
            } else {
                InputStream is = blob.getBinaryStream();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                if (is == null) {
                    return null;
                } else {
                    byte[] buffer = new byte[64];

                    for(int c = is.read(buffer); c > 0; c = is.read(buffer)) {
                        bos.write(buffer, 0, c);
                    }

                    return bos.toByteArray();
                }
            }
        } catch (IOException var8) {
            throw new SQLException("Failed to read BLOB column due to IOException: " + var8.getMessage());
        }
    }

    public void setBlobColumn(PreparedStatement stmt, int parameterIndex, byte[] value) throws SQLException {
        if (value == null) {
            stmt.setNull(parameterIndex, 2004);
        } else {
            stmt.setBinaryStream(parameterIndex, new ByteArrayInputStream(value), value.length);
        }

    }

    public String getClobColumn(ResultSet rs, int columnIndex) throws SQLException {
        try {
            Clob clob = rs.getClob(columnIndex);
            if (clob == null) {
                return null;
            } else {
                StringBuffer ret = new StringBuffer();
                InputStream is = clob.getAsciiStream();
                if (is == null) {
                    return null;
                } else {
                    byte[] buffer = new byte[64];

                    for(int c = is.read(buffer); c > 0; c = is.read(buffer)) {
                        ret.append(new String(buffer, 0, c));
                    }

                    return ret.toString();
                }
            }
        } catch (IOException var8) {
            throw new SQLException("Failed to read CLOB column due to IOException: " + var8.getMessage());
        }
    }

    public void setClobColumn(PreparedStatement stmt, int parameterIndex, String value) throws SQLException {
        if (value == null) {
            stmt.setNull(parameterIndex, 2005);
        } else {
            stmt.setAsciiStream(parameterIndex, new ByteArrayInputStream(value.getBytes()), value.length());
        }

    }

    protected <T> Page simplePageQuery(String sql, RowMapper<T> rowMapper, Map<String, ?> args, long pageNo, long pageSize) {
        long start = (pageNo - 1L) * pageSize;
        return this.simplePageQueryByStart(sql, rowMapper, args, start, pageSize);
    }

    protected <T> Page simplePageQueryByStart(String sql, RowMapper<T> rowMapper, Map<String, ?> args, long start, long pageSize) {
        String countSql = "select count(*) " + this.removeSelect(this.removeOrders(sql));
        long count = this.jdbcTemplateReadOnly().queryForLong(countSql, args);
        if (count == 0L) {
            log.debug("no result..");
            return new Page();
        } else {
            sql = sql + " limit " + start + "," + pageSize;
            log.debug(StringUtils.format("[Execute SQL]sql:{0},params:{1}", new Object[]{sql, args}));
            List<T> list = this.jdbcTemplateReadOnly().query(sql, rowMapper, args);
            return new Page(start, count, (int)pageSize, list);
        }
    }

    public long queryCount(String sql, Map<String, ?> args) {
        String countSql = "select count(*) " + this.removeSelect(this.removeOrders(sql));
        return this.jdbcTemplateReadOnly().queryForLong(countSql, args);
    }

    public <T> List<T> simpleListQueryByStart(String sql, RowMapper<T> rowMapper, Map<String, ?> args, long start, long pageSize) {
        sql = sql + " limit " + start + "," + pageSize;
        log.debug(StringUtils.format("[Execute SQL]sql:{0},params:{1}", new Object[]{sql, args}));
        List<T> list = this.jdbcTemplateReadOnly().query(sql, rowMapper, args);
        return (List)(list == null ? new ArrayList() : list);
    }

    protected Page simplePageQueryNotT(String sql, RowMapper rm, Map<String, ?> args, long pageNo, long pageSize) {
        String countSql = "select count(*) " + this.removeSelect(this.removeOrders(sql));
        long count = this.jdbcTemplateReadOnly().queryForLong(countSql, args);
        if (count == 0L) {
            log.debug("no result..");
            return new Page();
        } else {
            long start = (pageNo - 1L) * pageSize;
            sql = sql + " limit " + start + "," + pageSize;
            log.debug(StringUtils.format("[Execute SQL]sql:{0},params:{1}", new Object[]{sql, args}));
            List list = this.jdbcTemplateReadOnly().query(sql, rm, args);
            return new Page(start, count, (int)pageSize, list);
        }
    }

    protected String removeOrders(String sql) {
        Pattern p = Pattern.compile("order\\s*by[\\w|\\W|\\s|\\S]*", 2);
        Matcher m = p.matcher(sql);
        StringBuffer sb = new StringBuffer();

        while(m.find()) {
            m.appendReplacement(sb, "");
        }

        m.appendTail(sb);
        return sb.toString();
    }

    protected String removeSelect(String sql) {
        int beginPos = sql.toLowerCase().indexOf("from");
        return sql.substring(beginPos);
    }

    protected long getMaxId(String table, String column) {
        String sql = "SELECT max(" + column + ") FROM " + table + " ";
        long maxId = this.jdbcTemplateReadOnly().queryForLong(sql, new Object[0]);
        return maxId;
    }

    protected String makeSimpleUpdateSql(String tableName, String pkName, Object pkValue, Map<String, Object> params) {
        if (!StringUtils.isEmpty(new String[]{tableName}) && params != null && !params.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            sb.append("update ").append(tableName).append(" set ");
            Set<String> set = params.keySet();
            int index = 0;

            for(Iterator var9 = set.iterator(); var9.hasNext(); ++index) {
                String key = (String)var9.next();
                sb.append(key).append(" = :").append(key);
                if (index != set.size() - 1) {
                    sb.append(",");
                }
            }

            sb.append(" where ").append(pkName).append(" = :").append(pkName);
            return sb.toString();
        } else {
            return "";
        }
    }

    protected String makeSimpleUpdateSql(String pkName, Object pkValue, Map<String, Object> params) {
        if (!StringUtils.isEmpty(new String[]{this.getTableName()}) && params != null && !params.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            sb.append("update ").append(this.getTableName()).append(" set ");
            Set<String> set = params.keySet();
            int index = 0;

            for(Iterator var8 = set.iterator(); var8.hasNext(); ++index) {
                String key = (String)var8.next();
                sb.append(key).append(" = :").append(key);
                if (index != set.size() - 1) {
                    sb.append(",");
                }
            }

            sb.append(" where ").append(pkName).append(" = :").append(pkName);
            return sb.toString();
        } else {
            return "";
        }
    }

    protected String makeSimpleReplaceSql(String tableName, Map<String, Object> params) {
        if (!StringUtils.isEmpty(new String[]{tableName}) && params != null && !params.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            sb.append("replace into ").append(tableName);
            StringBuffer sbKey = new StringBuffer();
            StringBuffer sbValue = new StringBuffer();
            sbKey.append("(");
            sbValue.append("(");
            Set<String> set = params.keySet();
            int index = 0;

            for(Iterator var9 = set.iterator(); var9.hasNext(); ++index) {
                String key = (String)var9.next();
                sbKey.append(key);
                sbValue.append(" :").append(key);
                if (index != set.size() - 1) {
                    sbKey.append(",");
                    sbValue.append(",");
                }
            }

            sbKey.append(")");
            sbValue.append(")");
            sb.append(sbKey).append("VALUES").append(sbValue);
            return sb.toString();
        } else {
            return "";
        }
    }

    protected String makeSimpleReplaceSql(String tableName, Map<String, Object> params, List<Object> values) {
        if (!StringUtils.isEmpty(new String[]{tableName}) && params != null && !params.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            sb.append("replace into ").append(tableName);
            StringBuffer sbKey = new StringBuffer();
            StringBuffer sbValue = new StringBuffer();
            sbKey.append("(");
            sbValue.append("(");
            Set<String> set = params.keySet();
            int index = 0;
            Iterator var10 = set.iterator();

            while(var10.hasNext()) {
                String key = (String)var10.next();
                sbKey.append(key);
                sbValue.append(" ?");
                if (index != set.size() - 1) {
                    sbKey.append(",");
                    sbValue.append(",");
                }

                ++index;
                values.add(params.get(key));
            }

            sbKey.append(")");
            sbValue.append(")");
            sb.append(sbKey).append("VALUES").append(sbValue);
            return sb.toString();
        } else {
            return "";
        }
    }

    protected String makeSimpleInsertSql(String tableName, Map<String, Object> params) {
        if (!StringUtils.isEmpty(new String[]{tableName}) && params != null && !params.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            sb.append("insert into ").append(tableName);
            StringBuffer sbKey = new StringBuffer();
            StringBuffer sbValue = new StringBuffer();
            sbKey.append("(");
            sbValue.append("(");
            Set<String> set = params.keySet();
            int index = 0;

            for(Iterator var9 = set.iterator(); var9.hasNext(); ++index) {
                String key = (String)var9.next();
                sbKey.append(key);
                sbValue.append(" :").append(key);
                if (index != set.size() - 1) {
                    sbKey.append(",");
                    sbValue.append(",");
                }
            }

            sbKey.append(")");
            sbValue.append(")");
            sb.append(sbKey).append("VALUES").append(sbValue);
            return sb.toString();
        } else {
            return "";
        }
    }

    protected String makeSimpleInsertSql(String tableName, Map<String, Object> params, List<Object> values) {
        if (!StringUtils.isEmpty(new String[]{tableName}) && params != null && !params.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            sb.append("insert into ").append(tableName);
            StringBuffer sbKey = new StringBuffer();
            StringBuffer sbValue = new StringBuffer();
            sbKey.append("(");
            sbValue.append("(");
            Set<String> set = params.keySet();
            int index = 0;
            Iterator var10 = set.iterator();

            while(var10.hasNext()) {
                String key = (String)var10.next();
                sbKey.append(key);
                sbValue.append(" ?");
                if (index != set.size() - 1) {
                    sbKey.append(",");
                    sbValue.append(",");
                }

                ++index;
                values.add(params.get(key));
            }

            sbKey.append(")");
            sbValue.append(")");
            sb.append(sbKey).append("VALUES").append(sbValue);
            return sb.toString();
        } else {
            return "";
        }
    }

    protected Serializable doInsertRuturnKey(Map<String, Object> params) {
        final List<Object> values = new ArrayList();
        final String sql = this.makeSimpleInsertSql(this.getTableName(), params, values);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(this.getDataSourceWrite());

        try {
            jdbcTemplate.update(new PreparedStatementCreator() {
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement ps = con.prepareStatement(sql, 1);

                    for(int i = 0; i < values.size(); ++i) {
                        ps.setObject(i + 1, values.get(i) == null ? null : values.get(i));
                    }

                    return ps;
                }
            }, keyHolder);
        } catch (DataAccessException var9) {
            log.error("error", var9);
        }

        if (keyHolder == null) {
            return "";
        } else {
            Map<String, Object> keys = keyHolder.getKeys();
            if (keys != null && keys.size() != 0 && keys.values().size() != 0) {
                Object key = keys.values().toArray()[0];
                if (key != null && key instanceof Serializable) {
                    if (!(key instanceof Number)) {
                        return (Serializable)(key instanceof String ? (String)key : (Serializable)key);
                    } else {
                        Class clazz = key.getClass();
                        return clazz != Integer.TYPE && clazz != Integer.class ? ((Number)key).longValue() : (long)((Number)key).intValue();
                    }
                } else {
                    return "";
                }
            } else {
                return "";
            }
        }
    }

    protected String makeDefaultSimpleUpdateSql(Object pkValue, Map<String, Object> params) {
        return this.makeSimpleUpdateSql(this.getTableName(), this.getPKColumn(), pkValue, params);
    }

    protected String makeDefaultSimpleInsertSql(Map<String, Object> params) {
        return this.makeSimpleInsertSql(this.getTableName(), params);
    }

    protected Object doLoad(String tableName, String pkName, Object pkValue, RowMapper rm) {
        StringBuffer sb = new StringBuffer();
        sb.append("select * from ").append(tableName).append(" where ").append(pkName).append(" = ?");
        List<Object> list = this.jdbcTemplateReadOnly().query(sb.toString(), rm, new Object[]{pkValue});
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }

    protected <T> T doLoad(Object pkValue, RowMapper<T> rowMapper) {
        Object obj = this.doLoad(this.getTableName(), this.getPKColumn(), pkValue, rowMapper);
        return obj != null ? (T)obj : null;
    }

    protected int doDelete(String tableName, String pkName, Object pkValue) {
        StringBuffer sb = new StringBuffer();
        sb.append("delete from ").append(tableName).append(" where ").append(pkName).append(" = ?");
        int ret = this.jdbcTemplateWrite().update(sb.toString(), new Object[]{pkValue});
        return ret;
    }

    protected int doDelete(Object pkValue) {
        return this.doDelete(this.getTableName(), this.getPKColumn(), pkValue);
    }

    protected int doUpdate(String tableName, String pkName, Object pkValue, Map<String, Object> params) {
        params.put(pkName, pkValue);
        String sql = this.makeSimpleUpdateSql(tableName, pkName, pkValue, params);
        int ret = this.jdbcTemplateWrite().update(sql, params);
        return ret;
    }

    protected int doUpdate(String pkName, Object pkValue, Map<String, Object> params) {
        params.put(pkName, pkValue);
        String sql = this.makeSimpleUpdateSql(pkName, pkValue, params);
        int ret = this.jdbcTemplateWrite().update(sql, params);
        return ret;
    }

    protected int doUpdate(Object pkValue, Map<String, Object> params) {
        String sql = this.makeDefaultSimpleUpdateSql(pkValue, params);
        params.put(this.getPKColumn(), pkValue);
        int ret = this.jdbcTemplateWrite().update(sql, params);
        return ret;
    }

    protected boolean doReplace(Map<String, Object> params) {
        String sql = this.makeSimpleReplaceSql(this.getTableName(), params);
        int ret = this.jdbcTemplateWrite().update(sql, params);
        return ret == 1;
    }

    protected boolean doReplace(String tableName, Map<String, Object> params) {
        String sql = this.makeSimpleReplaceSql(tableName, params);
        int ret = this.jdbcTemplateWrite().update(sql, params);
        return ret == 1;
    }

    protected boolean doInsert(String tableName, Map<String, Object> params) {
        String sql = this.makeSimpleInsertSql(tableName, params);
        int ret = this.jdbcTemplateWrite().update(sql, params);
        return ret == 1;
    }

    protected boolean doInsert(Map<String, Object> params) {
        String sql = this.makeSimpleInsertSql(this.getTableName(), params);
        int ret = this.jdbcTemplateWrite().update(sql, params);
        return ret == 1;
    }

    protected abstract String getPKColumn();

    protected Map<String, Object> convertMap(Object obj) {
        Map<String, Object> map = new HashMap();
        List<FieldInfo> getters = TypeUtils.computeGetters(obj.getClass(), (Map)null);
        int i = 0;

        for(int len = getters.size(); i < len; ++i) {
            FieldInfo fieldInfo = (FieldInfo)getters.get(i);
            String name = fieldInfo.getName();

            try {
                Object value = fieldInfo.get(obj);
                map.put(name, value);
            } catch (Exception var9) {
                log.error(String.format("convertMap error object:%s  field: %s", obj.toString(), name));
            }
        }

        return map;
    }
}
