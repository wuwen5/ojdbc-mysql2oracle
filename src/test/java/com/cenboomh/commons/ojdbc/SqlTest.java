package com.cenboomh.commons.ojdbc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
@SpringBootTest
@Rollback
@Transactional
public class SqlTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void testSpecialSql() {

        String sql = " SELECT @@read_only ";
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class);

        Assertions.assertEquals(0, result);

        sql = " SELECT 1 ";
        result = jdbcTemplate.queryForObject(sql, Integer.class);

        Assertions.assertEquals(1, result);

        sql = " SELECT 1 from dual;";
        jdbcTemplate.queryForObject(sql, Integer.class);
    }

    @Test
    void testNotExpressionNotUseExclamationMark() {
        String sql = " SELECT 1 from dual where !(1=2) and 1=1 and 2=2";

        jdbcTemplate.queryForObject(sql, Integer.class);

        sql = " SELECT 1 from dual where !(1=2) and (1=1 and 2=2)";

        jdbcTemplate.queryForObject(sql, Integer.class);

        sql = " SELECT 1 from dual where 1=1 and 2=2 and !(1=2)";

        jdbcTemplate.queryForObject(sql, Integer.class);

        sql = " SELECT 1 from dual where 1=1 or 2=2 or !(1=2)";

        jdbcTemplate.queryForObject(sql, Integer.class);

        sql = " SELECT 1 from dual where not(1=2)";

        jdbcTemplate.queryForObject(sql, Integer.class);
    }

    /**
     * 反引号
     */
    @Test
    void testWithBackquote() {
        String sql = " SELECT 1 from `dual`";

        jdbcTemplate.queryForObject(sql, Integer.class);

        sql = " SELECT '`' from `dual` where `dummy` = 'xx' or 1 = 1";

        String s = jdbcTemplate.queryForObject(sql, String.class);

        Assertions.assertEquals("`", s);


        jdbcTemplate.update("INSERT INTO `config_tags_relation`(`id`,`tag_name`,`tag_type`,`data_id`,`group_id`,`tenant_id`) VALUES(?,?,?,?,?,?)",
                99999, "ut-test-001", 1, "ut-test-001", "group", "namespace");

        sql = "update `config_tags_relation` set `tag_name` = '1' where `id` =  99999";

        jdbcTemplate.update(sql);

    }

    @Test
    void testNormalInsert() {
        jdbcTemplate.update("INSERT INTO config_tags_relation(id,tag_name,tag_type,data_id,group_id,tenant_id) VALUES(?,?,?,?,?,?)",
                99999, "ut-test-001", 1, "ut-test-001", "group", "namespace");

        List<Map<String, Object>> list = jdbcTemplate.queryForList("select * from config_tags_relation where tag_name = 'ut-test-001'");

        Object group_id = list.get(0).get("group_id");

        Assertions.assertEquals("group", group_id);
    }

    /**
     * limit 非预编译
     */
    @Test
    void testPageQuery() {
        String sqlCountRows = "select count(*) from his_config_info where data_id = ? and group_id = ? and tenant_id = ?";

        String sqlFetchRows =
                "select nid,data_id,group_id,tenant_id,app_name,src_ip,src_user,op_type,gmt_create,gmt_modified from his_config_info "
                        + "where data_id = ? and group_id = ? and tenant_id = ? order by nid desc limit 0, 10";

        Integer count = jdbcTemplate.queryForObject(sqlCountRows, Integer.class, "ut-test-9999", "default", "");
        Assertions.assertEquals(0, count);

        jdbcTemplate.queryForList(sqlFetchRows, "ut-test", "default", "");
    }

    /**
     * limit 预编译
     * limit 嵌套
     */
    @Test
    void testPageQuery2() {
        String sqlFetchRows = " SELECT t.id,data_id,group_id,tenant_id,app_name,md5,type,gmt_modified FROM "
                + "( SELECT id FROM config_info ORDER BY id LIMIT ?,?  ) g, config_info t WHERE g.id = t.id";

        jdbcTemplate.queryForList(sqlFetchRows, 0, 10);

        //limit 单参数
        sqlFetchRows = " SELECT id FROM config_info as tablaA LIMIT ?";
        jdbcTemplate.queryForList(sqlFetchRows, 10);

        //limit 1
        sqlFetchRows = " SELECT id FROM config_info as tablaA LIMIT 1";
        jdbcTemplate.queryForList(sqlFetchRows);
    }

    /**
     * from table 使用as别名时
     */
    @Test
    void testFromWithAliasUseAs() {

        String sqlFetchRows = " SELECT id FROM (select id from config_info as tablaA) as tableB LIMIT ?";

        jdbcTemplate.queryForList(sqlFetchRows, 10);
    }

    /**
     * limit
     * order by
     * group by
     */
    @Test
    void testPageQueryWithGroupByOrderBy() {
        String sql = "SELECT tenant_id FROM config_info WHERE tenant_id != '' GROUP BY tenant_id LIMIT ?, ?";

        List<Map<String, Object>> maps = jdbcTemplate.queryForList(sql, 0, 10);

        String sqlFetchRows = " SELECT t.id,data_id,group_id,tenant_id,app_name,content,md5 "
                + " FROM (  SELECT id FROM config_info WHERE tenant_id like ? ORDER BY id LIMIT ?,? )"
                + " g, config_info t  WHERE g.id = t.id";

        jdbcTemplate.queryForList(sqlFetchRows, "", 10, 10);
    }

    @Test
    void testDeleteWithLimit() {
        String sql = "delete from his_config_info where gmt_modified < ? limit ?";

        int update = jdbcTemplate.update(sql, Timestamp.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)), 0);

        Assertions.assertEquals(0, update);
    }

    /**
     * insert 返回主键
     */
    @Test
    void testInsertReturnId() {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        final String sql =
                "INSERT INTO config_info(data_id,group_id,tenant_id,app_name,content,md5,src_ip,src_user,gmt_create,"
                        + "gmt_modified,c_desc,c_use,effect,type,c_schema) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        final String appNameTmp = "";
        final String tenantTmp = "";

        final String desc = "";
        final String use = "";
        final String effect = "";
        final String type = "";
        final String schema = "";

        final String md5Tmp = "";


        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, "test-dataId-" + System.currentTimeMillis());
            ps.setString(2, "ut-test");
            ps.setString(3, tenantTmp);
            ps.setString(4, appNameTmp);
            ps.setString(5, "xxxx");
            ps.setString(6, md5Tmp);
            ps.setString(7, "");
            ps.setString(8, "");
            ps.setTimestamp(9, Timestamp.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
            ps.setTimestamp(10, Timestamp.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
            ps.setString(11, desc);
            ps.setString(12, use);
            ps.setString(13, effect);
            ps.setString(14, type);
            ps.setString(15, schema);

            return ps;
        }, keyHolder);
        Number nu = keyHolder.getKey();
        if (nu == null) {
            throw new IllegalArgumentException("insert config_info fail");
        }

        List<Map<String, Object>> list = jdbcTemplate.queryForList("select * from config_info where id = " + nu.longValue());

        Assertions.assertTrue(nu.longValue() > 0);
        Assertions.assertEquals(1, list.size());
    }

    /**
     * TODO mysql中的 空值条件 查询, 如: tenant_id = ?
     * <p>
     * 两种情况  一、 tenant_id = '', 这种可以sql转换为 tenant_id is null
     * 二、tenant_id = ? 预编译时，sql没法转换.
     **/
    @Test
    void testQueryEqEmpty() {

        final String insert = "INSERT INTO config_info(data_id,tenant_id,content) " +
                "VALUES('test-dataId-9999','','xxxx')";

        int update = jdbcTemplate.update(insert);

        List<Map<String, Object>> result = jdbcTemplate.queryForList("select * from config_info where data_id='test-dataId-9999'");


        //oralce
        List<Map<String, Object>> result2 = jdbcTemplate.queryForList("select * from config_info where data_id='test-dataId-9999' " +
                " and tenant_id is null");

        //mysql
        List<Map<String, Object>> result3 = jdbcTemplate.queryForList("select * from config_info where data_id='test-dataId-9999' " +
                " and tenant_id = ?", "");

        Assertions.assertEquals(1, update);
        Assertions.assertTrue(!result.isEmpty());
        Assertions.assertTrue(!result2.isEmpty());

        //TODO.. mysql中的 空值条件 查询, 如: tenant_id = ?
        Assertions.assertTrue(!result3.isEmpty());

        result3 = jdbcTemplate.queryForList("select * from config_info where data_id='test-dataId-9999' " +
                " and tenant_id like ?", "");

        Assertions.assertTrue(!result3.isEmpty());


        result3 = jdbcTemplate.queryForList("select * from config_info where data_id='test-dataId-9999' " +
                " and tenant_id like ? and tenant_id = ?", "", "");

        Assertions.assertTrue(!result3.isEmpty());
        result3 = jdbcTemplate.queryForList("select * from config_info where data_id='test-dataId-9999' " +
                " and tenant_id != ?", "");

        Assertions.assertTrue(result3.isEmpty());
    }

    @Test
    void testDateAddFun() {
        String sql = "select 1 from dual where DATE_ADD(?, INTERVAL -? SECOND) < ?";


        jdbcTemplate.queryForList(sql, Integer.class, Timestamp.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)),
                1, Timestamp.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
    }

    @Test
    void testUpdateSetNull() {

        final String insert = "INSERT INTO config_info(data_id,tenant_id,content, app_name) " +
                "VALUES('test-dataId-9999','','xxxx', 'appName')";

        jdbcTemplate.update(insert);

        String sql = "update config_info set app_name=? where data_id='test-dataId-9999'";

        jdbcTemplate.update(sql, new Object[]{null});


        String s = jdbcTemplate.queryForObject("select app_name from config_info where data_id='test-dataId-9999'",
                String.class);

        Assertions.assertNull(s);
    }

    /**
     * select 字段别名增加双引号，避免转map后key为全大写.
     */
    @Test
    void testAliasWithQuotationMark() {
        String sql = "select 'aa' app_name from dual";

        Map map = jdbcTemplate.queryForMap(sql);

        System.out.println(map);
        //spring 默认返回的result map是LinkedCaseInsensitiveMap，忽略大小写, 而mybatis中是区分大小写.

        Map matchCase = new HashMap<>(map);
        Assertions.assertEquals("aa", matchCase.get("app_name"));

        sql = "select (select 'aa' app_name from dual) a from dual";

        map = jdbcTemplate.queryForMap(sql);

        matchCase = new HashMap<>(map);
        Assertions.assertEquals("aa", matchCase.get("A"));

        sql = "select dummy app_name from dual";

        map = jdbcTemplate.queryForMap(sql);

        sql = "select count(dummy) app_name from dual";

        jdbcTemplate.queryForMap(sql);

        matchCase = new HashMap<>(map);
        Assertions.assertEquals("X", matchCase.get("app_name"));

    }

}
