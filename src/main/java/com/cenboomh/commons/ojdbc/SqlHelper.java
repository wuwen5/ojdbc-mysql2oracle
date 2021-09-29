package com.cenboomh.commons.ojdbc;

import com.cenboomh.commons.ojdbc.function.BaseFunction;
import com.cenboomh.commons.ojdbc.function.DateAddFunction;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author wuwen
 */
public class SqlHelper {

    private static final Logger log = Logger.getLogger(SqlHelper.class.getName());

    private static Map<String, String> mapping = new HashMap<>();

    private static final ThreadLocal<Page> LOCAL_LIMIT = new ThreadLocal<>();

    static {
        mapping.put("SELECT @@READ_ONLY", "SELECT 0 FROM DUAL");

        //初始化函数处理
        DateAddFunction.init();
    }

    /**
     * 获取 Page 参数
     */
    public static Page getLocalPage() {
        return LOCAL_LIMIT.get();
    }

    /**
     * 移除本地变量
     */
    public static void clearPage() {
        LOCAL_LIMIT.remove();
    }


    /**
     * @param sql 原mysql语法
     * @return oracle 支持的语法
     */
    public static String mysql2oracle(String sql) {
        String newSql = sql;

        AtomicBoolean needModify = new AtomicBoolean(false);

        try {
            String sqlUp = sql.trim().toUpperCase();
            //固定的SQL写法
            if (mapping.containsKey(sqlUp)) {
                newSql = mapping.get(sqlUp);
                needModify.set(true);
            } else {
                Statement parse = CCJSqlParserUtil.parse(sql);
                if (sql.trim().endsWith(";")) {
                    needModify.set(true);
                }

                parse.accept(new TablesNamesFinder() {
                    public void visit(LikeExpression likeExpression){
                        tenantIdNullHandler(likeExpression, needModify);
                        super.visit(likeExpression);
                    }
                    public void visit(EqualsTo equalsTo){
                        tenantIdNullHandler(equalsTo, needModify);
                        super.visit(equalsTo);
                    }
                    public void visit(NotEqualsTo notEqualsTo){
                        tenantIdNullHandler(notEqualsTo, needModify);
                        super.visit(notEqualsTo);
                    }
                    @Override
                    public void visit(SelectExpressionItem item) {
                        super.visit(item);

                        if (item.getAlias() != null &&
                                (item.getExpression() instanceof StringValue
                                        || item.getExpression() instanceof Column
                                        || item.getExpression() instanceof Function)) {
                            if (!item.getAlias().getName().startsWith("\"")
                                    && !item.getAlias().getName().endsWith("\"")) {
                                item.getAlias().setName("\"" + item.getAlias().getName() + "\"");
                                needModify.set(true);
                            }
                        }
                    }

                    @Override
                    public void visit(PlainSelect plainSelect) {

                        super.visit(plainSelect);

                        //select 1 -> select 1 from dual
                        if (plainSelect.getFromItem() == null) {
                            plainSelect.setFromItem(new Table("dual"));
                            needModify.set(true);
                        }
                    }

                    @Override
                    public void visit(Table tableName) {

                        if (tableName.getName() != null && tableName.getName().contains("`")) {
                            tableName.setName(tableName.getName().replaceAll("`", ""));
                            needModify.set(true);
                        }

                        if (tableName.getAlias() != null && tableName.getAlias().isUseAs()) {
                            tableName.getAlias().setUseAs(false);
                            needModify.set(true);
                        }
                    }

                    @Override
                    public void visit(SubSelect subSelect) {
                        super.visit(subSelect);

                        if (subSelect.getAlias() != null && subSelect.getAlias().isUseAs()) {
                            subSelect.getAlias().setUseAs(false);
                            needModify.set(true);
                        }
                    }

                    @Override
                    public void visit(Column tableColumn) {
                        if (tableColumn.getColumnName().contains("`")) {
                            tableColumn.setColumnName(tableColumn.getColumnName().replaceAll("`", ""));
                            needModify.set(true);
                        }
                    }

                    @Override
                    public void visit(Insert insert) {
                        super.visit(insert);
                        columnsProcess(insert.getColumns());

                    }

                    @Override
                    public void visit(Update update) {
                        super.visit(update);
                        columnsProcess(update.getUpdateSets().get(0).getColumns());
                    }

                    @Override
                    public void visit(NotExpression notExpr) {
                        //非等条件,不使用感叹号.
                        if (notExpr.isExclamationMark()) {
                            notExpr.setExclamationMark(false);
                            needModify.set(true);
                        }
                    }

                    @Override
                    public void visit(Function function) {
                        super.visit(function);
                        //对函数进行替换处理
                        if (BaseFunction.process(function)) {
                            needModify.set(true);
                        }
                    }

                    private void columnsProcess(List<Column> columns) {
                        columns.stream()
                                .filter(c -> c.getColumnName().contains("`"))
                                .peek(c -> needModify.set(true))
                                .forEach(c -> c.setColumnName(c.getColumnName().replaceAll("`", "")));
                    }
                });

                parse.accept(new StatementVisitorAdapter() {
                    @Override
                    public void visit(Select select) {
                        SelectBody selectBody = select.getSelectBody();

                        selectBody.accept(new SelectVisitorAdapter() {
                            @Override
                            public void visit(PlainSelect plainSelect) {

                                if (replacePageSql(plainSelect, select::setSelectBody)) {
                                    needModify.set(true);
                                }
                            }
                        });
                    }

                    @Override
                    public void visit(Delete delete) {
                        if (delete.getLimit() != null) {
                            Limit limit = delete.getLimit();

                            delete.setLimit(new Limit() {

                                @Override
                                public Expression getRowCount() {
                                    return limit.getRowCount();
                                }

                                @Override
                                public String toString() {
                                    String retValue = "";
                                    if (delete.getWhere() != null) {
                                        retValue += " AND ";
                                    }
                                    return retValue + " rownum <= " + getRowCount();
                                }
                            });

                            needModify.set(true);
                        }
                    }
                });

                if (needModify.get()) {
                    newSql = parse.toString();
                }
            }
        } catch (Exception e) {
            if (log.isLoggable(Level.WARNING)) {
                log.log(Level.WARNING, "sql 转换异常. sql:" + sql, e);
            }
        }

        if (needModify.get()) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, String.format("源sql:%s, 转换后:%s", sql, newSql));
            }
        }
        return newSql;
    }

    /**
     * 这里主要是针对TENANT_ID的处理，mysql的''在Oracle中为null，所以要做一些特殊处理
     * @param expression
     * @param needModify
     */
    private static void tenantIdNullHandler(BinaryExpression expression, final AtomicBoolean needModify) {
        Expression leftExpression = expression.getLeftExpression();
        if(leftExpression instanceof Column){
            Column col = (Column) leftExpression;
            if(col.getColumnName().equalsIgnoreCase("tenant_id")){
                Function func = new Function();
                func.setName(Arrays.asList("nvl"));
                ExpressionList list = new ExpressionList();
                list.addExpressions(col);
                list.addExpressions(new StringValue("!null!"));
                func.setParameters(list);
                expression.setLeftExpression(func);

                Expression rightExpression = expression.getRightExpression();
                func = new Function();
                func.setName(Arrays.asList("nvl"));
                list = new ExpressionList();
                list.addExpressions(rightExpression);
                list.addExpressions(new StringValue("!null!"));
                func.setParameters(list);
                expression.setRightExpression(func);
                needModify.set(true);
            }
        }
    }

    /**
     * 替换分页语句
     *
     * @param plainSelect   select
     * @param setSelectBody select覆盖函数, Select#setSelectBody or SubSelect#setSelectBody
     */
    private static boolean replacePageSql(PlainSelect plainSelect, Consumer<SelectBody> setSelectBody) {
        Limit limit = plainSelect.getLimit();

        if (limit != null) {
            String newSql;
            plainSelect.setLimit(null);
            //非预编译limit,  limit 0, 10
            if (limit.getRowCount() instanceof LongValue && limit.getOffset() instanceof LongValue) {
                LongValue offset = (LongValue) limit.getOffset();
                LongValue rowCount = (LongValue) limit.getRowCount();

                newSql = getPageSql(plainSelect.toString(), offset.getValue(), rowCount.getValue() + offset.getValue());
            } else if (limit.getRowCount() instanceof JdbcParameter && limit.getOffset() instanceof JdbcParameter) {
                //预编译记下分页参数位置 limit ? , ? 用于PreparedStatement#setLong时替换值
                newSql = getPageSql(plainSelect.toString());
                JdbcParameter offset = (JdbcParameter) limit.getOffset();
                JdbcParameter rowCount = (JdbcParameter) limit.getRowCount();
                LOCAL_LIMIT.set(new Page().setStartRowIndex(offset.getIndex())
                        .setEndRowIndex(rowCount.getIndex()));
            } else if (limit.getRowCount() instanceof JdbcParameter) {
                newSql = getPageSqlEndRow(plainSelect.toString());
            } else if (limit.getRowCount() instanceof LongValue) {
                newSql = getPageSqlEndRow(plainSelect.toString(), ((LongValue) limit.getRowCount()).getValue());
            } else {
                plainSelect.setLimit(limit);
                log.log(Level.WARNING, "暂不支持的分页语法. [" + plainSelect.toString() + "]");
                return false;
            }

            try {
                Select newSelect = (Select) CCJSqlParserUtil.parse(newSql);
                //select::setSelectBody()
                setSelectBody.accept(newSelect.getSelectBody());
                return true;
            } catch (JSQLParserException e) {
                log.log(Level.WARNING, "分页sql解析异常. [" + newSql + "]", e);
                return false;
            }
        }

        FromItem fromItem = plainSelect.getFromItem();

        //子查询
        if (fromItem instanceof SubSelect) {
            SubSelect subSelect = (SubSelect) fromItem;

            SelectBody selectBody = subSelect.getSelectBody();

            if (selectBody instanceof PlainSelect) {
                return replacePageSql((PlainSelect) selectBody, subSelect::setSelectBody);
            }
        }

        return false;
    }

    private static String getPageSql(String sql) {
        return getPageSql0(sql, "?", "?");
    }

    private static String getPageSqlEndRow(String sql) {
        return getPageSqlEndRow0(sql, "?");
    }

    private static String getPageSqlEndRow(String sql, long endRow) {
        return getPageSqlEndRow0(sql, endRow);
    }


    private static String getPageSql(String sql, long startRow, long endRow) {
        return getPageSql0(sql, startRow, endRow);
    }

    private static String getPageSqlEndRow0(String sql, Object endRow) {
        return "SELECT * FROM ( " +
                " SELECT TMP_PAGE.*, ROWNUM ROW_ID FROM ( " +
                sql +
                " ) TMP_PAGE)" +
                " WHERE ROW_ID <= " + endRow;
    }

    private static String getPageSql0(String sql, Object startRow, Object endRow) {
        return "SELECT * FROM ( " +
                " SELECT TMP_PAGE.*, ROWNUM ROW_ID FROM ( " +
                sql +
                " ) TMP_PAGE)" +
                " WHERE ROW_ID > " + startRow + " AND ROW_ID <= " + endRow;
    }
}
