package com.cenboomh.commons.ojdbc;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wuwen
 */
public class SqlHelper {

    private static final Logger log = Logger.getLogger(SqlHelper.class.getName());

    private static Map<String, String> mapping = new HashMap<>();

    private static Map<Pattern, String> sqlReplace = new HashMap<>();

    private static final ThreadLocal<Page> LOCAL_LIMIT = new ThreadLocal<>();

    static {
        mapping.put("SELECT @@READ_ONLY", "SELECT 0 FROM DUAL");

        //TODO 配置特殊替换规则,放到配置文件中 (nacos 空值eq操作, TENANT_ID = ?)
        String reg = "((?i)TENANT_ID[ ]?[=][ ]?[?])";
        sqlReplace.put(Pattern.compile(reg), "nvl(TENANT_ID, '!null!') = nvl(?, '!null!')");

        sqlReplace.put(Pattern.compile("((?i)TENANT_ID[ ]?!=[ ]?[?])"), "nvl(TENANT_ID, '!null!') != nvl(?, '!null!')");
        sqlReplace.put(Pattern.compile("((?i)TENANT_ID[ ]?like[ ]?[?])"), "nvl(TENANT_ID, '!null!') like nvl(?, '!null!')");
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
            if (mapping.containsKey(sqlUp)) {
                newSql = mapping.get(sqlUp);
                needModify.set(true);
            } else {

                StringBuilder sb = new StringBuilder(sql);

                sqlReplace.entrySet().stream()
                        .map(e -> matcherReplace(e, sb.toString()))
                        .filter(Optional::isPresent)
                        .peek(s -> needModify.set(true))
                        .map(Optional::get)
                        .forEach(s -> sb.replace(0, sb.length(), s));

                Statement parse = CCJSqlParserUtil.parse(sb.toString());

                parse.accept(new StatementVisitorAdapter() {
                    @Override
                    public void visit(Select select) {
                        SelectBody selectBody = select.getSelectBody();

                        selectBody.accept(new SelectVisitorAdapter() {
                            @Override
                            public void visit(PlainSelect plainSelect) {
                                //select 1 -> select 1 from dual
                                if (plainSelect.getFromItem() == null) {
                                    plainSelect.setFromItem(new Table("dual"));
                                    needModify.set(true);
                                }

                                if (tableAliasNotUseAs(plainSelect)) {
                                    needModify.set(true);
                                }

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
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, String.format("源sql:%s, 转换后:%s", sql, newSql));
            }
        }
        return newSql;
    }

    private static Optional<String> matcherReplace(Map.Entry<Pattern, String> entry, String sql) {
        Matcher matcher = entry.getKey().matcher(sql);
        if (matcher.find()) {
            String s = matcher.replaceAll(entry.getValue());
            return Optional.of(s);
        }
        return Optional.empty();
    }

    /**
     * FromItemm中存在别名时不使用as
     * */
    private static boolean tableAliasNotUseAs(PlainSelect plainSelect) {

        FromItem fromItem = plainSelect.getFromItem();

        //子查询
        if (fromItem instanceof SubSelect) {
            SubSelect subSelect = (SubSelect) fromItem;

            SelectBody selectBody = subSelect.getSelectBody();

            boolean update = false;

            if (selectBody instanceof PlainSelect) {
                update = tableAliasNotUseAs((PlainSelect) selectBody);
            }

            if (fromItem.getAlias() != null && fromItem.getAlias().isUseAs()) {
                fromItem.getAlias().setUseAs(false);
                update = true;
            }

            return update;
        } else {

            if (fromItem.getAlias() != null && fromItem.getAlias().isUseAs()) {
                fromItem.getAlias().setUseAs(false);
                return true;
            }

            return false;
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
            } else {
                log.log(Level.WARNING, "暂不支持的分页语法. [" + limit.toString() + "]");
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
        return "SELECT * FROM ( " +
                " SELECT TMP_PAGE.*, ROWNUM ROW_ID FROM ( " +
                sql +
                " ) TMP_PAGE)" +
                " WHERE ROW_ID > ? AND ROW_ID <= ?";
    }

    private static String getPageSqlEndRow(String sql) {
        return "SELECT * FROM ( " +
                " SELECT TMP_PAGE.*, ROWNUM ROW_ID FROM ( " +
                sql +
                " ) TMP_PAGE)" +
                " WHERE ROW_ID <= ?";
    }

    private static String getPageSql(String sql, long startRow, long endRow) {
        return "SELECT * FROM ( " +
                " SELECT TMP_PAGE.*, ROWNUM ROW_ID FROM ( " +
                sql +
                " ) TMP_PAGE)" +
                " WHERE ROW_ID > " + startRow + " AND ROW_ID <= " + endRow;
    }
}
