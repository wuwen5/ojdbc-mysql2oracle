package com.cenboomh.commons.ojdbc.wrapper;

import com.cenboomh.commons.ojdbc.Page;
import com.cenboomh.commons.ojdbc.SqlHelper;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.insert.Insert;
import oracle.sql.ROWID;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wuwen
 */
public class PreparedStatementWrapper extends StatementWrapper implements PreparedStatement {

    private final PreparedStatement delegate;
    private final Statement sqlStatement;

    /**
     * 查询主键
     */
    private final String QUERY_KEY_SQL = "select col.column_name from user_constraints con,user_cons_columns col\n" +
            "where con.constraint_name=col.constraint_name and con.constraint_type='P'\n" +
            " and col.table_name = ?";

    private static final Map<String, String> TABLE_P_KEY = new ConcurrentHashMap<>();

    public static PreparedStatement wrap(PreparedStatement delegate, String sql) throws SQLException {
        if (delegate == null) {
            return null;
        }
        return new PreparedStatementWrapper(delegate, sql);
    }

    protected PreparedStatementWrapper(PreparedStatement delegate, String sql) throws SQLException {
        super(delegate);
        this.delegate = delegate;
        try {
            this.sqlStatement = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        delegate.setInt(parameterIndex, (int) pageIndexValue(parameterIndex, x));
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        delegate.setLong(parameterIndex, (long) pageIndexValue(parameterIndex, x));
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {

        delegate.setObject(parameterIndex, pageIndexValue(parameterIndex, x));
    }

    /**
     * limit 预编译sql时，分页参数值处理
     *
     * @param parameterIndex 分页参数下标
     * @param x              分页参数原值
     */
    private Object pageIndexValue(int parameterIndex, Object x) {
        Page localPage = SqlHelper.getLocalPage();
        Object obj = x;
        if (localPage != null) {
            if (localPage.getStartRowIndex() == parameterIndex && x instanceof Number) {
                localPage.setStartRowValue(((Number) x).intValue());
            }

            if (localPage.getEndRowIndex() == parameterIndex && x instanceof Number) {
                localPage.setEndRowValue(localPage.getStartRowValue() + ((Number) x).intValue());

                obj = localPage.getEndRowValue();
            }
        }

        return obj;
    }

    @Override
    public void close() throws SQLException {
        try {
            super.close();
        } finally {
            SqlHelper.clearPage();
        }
    }

    /**
     * insert 返回主键id, 默认oracle驱动返回的为rowid
     */
    @Override
    public ResultSet getGeneratedKeys() throws SQLException {

        if (sqlStatement instanceof Insert) {

            Insert insert = (Insert) sqlStatement;

            try (ResultSet generatedKeys = super.getGeneratedKeys()) {

                if (generatedKeys.next()) {

                    ROWID object = (ROWID) generatedKeys.getObject(1);

                    String rowid = object.stringValue();
                    String tableName = insert.getTable().getName().toUpperCase();

                    String queryIdSql = "select " + getPrimarykey(tableName) + " from " + tableName + " where rowid = ?";

                    PreparedStatement preparedStatement = delegate.getConnection()
                            .prepareStatement(queryIdSql);
                    preparedStatement.setString(1, rowid);

                    return preparedStatement.executeQuery();

                }

                return generatedKeys;
            }
        } else {
            return super.getGeneratedKeys();
        }
    }

    private String getPrimarykey(String tableName) {

        if (!TABLE_P_KEY.containsKey(tableName)) {
            TABLE_P_KEY.computeIfAbsent(tableName, this::getPrimaryKeyFromDb);
        }

        return TABLE_P_KEY.get(tableName);
    }

    /**
     * 查询表主键字段
     */
    private String getPrimaryKeyFromDb(String tableName) {

        try (PreparedStatement preparedStatement = delegate.getConnection().prepareStatement(QUERY_KEY_SQL)) {
            preparedStatement.setString(1, tableName);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }

        return null;
    }

    @Override
    public boolean execute() throws SQLException {
        return delegate.execute();
    }

    @Override
    public void addBatch() throws SQLException {
        delegate.addBatch();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        delegate.setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        delegate.setRef(parameterIndex, x);
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        delegate.setBlob(parameterIndex, x);
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        delegate.setClob(parameterIndex, x);
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        delegate.setArray(parameterIndex, x);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return delegate.getMetaData();
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        delegate.setDate(parameterIndex, x, cal);
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        delegate.setTime(parameterIndex, x, cal);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        delegate.setTimestamp(parameterIndex, x, cal);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        delegate.setNull(parameterIndex, sqlType, typeName);
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        delegate.setURL(parameterIndex, x);
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return delegate.getParameterMetaData();
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        delegate.setRowId(parameterIndex, x);
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        delegate.setNString(parameterIndex, value);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        delegate.setNCharacterStream(parameterIndex, value, length);
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        delegate.setNClob(parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        delegate.setClob(parameterIndex, reader, length);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        delegate.setBlob(parameterIndex, inputStream, length);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        delegate.setNClob(parameterIndex, reader, length);
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        delegate.setSQLXML(parameterIndex, xmlObject);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        delegate.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        delegate.setAsciiStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        delegate.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        delegate.setCharacterStream(parameterIndex, reader, length);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        delegate.setAsciiStream(parameterIndex, x);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        delegate.setBinaryStream(parameterIndex, x);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        delegate.setCharacterStream(parameterIndex, reader);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        delegate.setNCharacterStream(parameterIndex, value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        delegate.setClob(parameterIndex, reader);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        delegate.setBlob(parameterIndex, inputStream);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        delegate.setNClob(parameterIndex, reader);
    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        delegate.setUnicodeStream(parameterIndex, x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        delegate.setBinaryStream(parameterIndex, x, length);
    }

    @Override
    public void clearParameters() throws SQLException {
        delegate.clearParameters();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        delegate.setObject(parameterIndex, x, targetSqlType);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        delegate.setFloat(parameterIndex, x);
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        delegate.setDouble(parameterIndex, x);
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        delegate.setBigDecimal(parameterIndex, x);
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        delegate.setString(parameterIndex, x);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        delegate.setBytes(parameterIndex, x);
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        delegate.setDate(parameterIndex, x);
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        delegate.setTime(parameterIndex, x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        delegate.setTimestamp(parameterIndex, x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        delegate.setAsciiStream(parameterIndex, x, length);
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        return delegate.executeQuery();
    }

    @Override
    public int executeUpdate() throws SQLException {
        return delegate.executeUpdate();
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        delegate.setNull(parameterIndex, sqlType == Types.OTHER ? Types.NULL : sqlType);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        delegate.setBoolean(parameterIndex, x);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        delegate.setByte(parameterIndex, x);
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        delegate.setShort(parameterIndex, x);
    }
}
