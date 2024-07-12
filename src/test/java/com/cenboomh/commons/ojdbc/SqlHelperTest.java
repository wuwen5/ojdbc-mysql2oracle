package com.cenboomh.commons.ojdbc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlHelperTest {

    private String[][] testSqls = {
            {" SELECT @@read_only ", "SELECT 0 FROM DUAL"},
            {"SELECT 1", "SELECT 1 FROM DUAL"},
            {"SELECT 1 from dual;", "SELECT 1 FROM DUAL"},
            {"SELECT 1 from dual where !(1=2) and 1=1 and 2=2", "SELECT 1 FROM DUAL WHERE NOT (1 = 2) AND 1 = 1 AND 2 = 2"},
            {"SELECT 1 from dual where !(1=2) and (1=1 and 2=2)", "SELECT 1 FROM DUAL WHERE NOT (1 = 2) AND (1 = 1 AND 2 = 2)"},
            {"SELECT 1 from dual where 1=1 and 2=2 and !(1=2)", "SELECT 1 FROM DUAL WHERE 1 = 1 AND 2 = 2 AND NOT (1 = 2)"},
            {"SELECT 1 from dual where 1=1 or 2=2 or !(1=2)", "SELECT 1 FROM DUAL WHERE 1 = 1 OR 2 = 2 OR NOT (1 = 2)"},
            {"SELECT 1 from dual where not(1=2)", "SELECT 1 FROM DUAL WHERE NOT(1=2)"},
            {"select count(*) from config_info  a left join config_tags_relation b on a.id=b.id where  a.tenant_id=?  and b.tag_name in (?)",
                    "SELECT count(*) FROM config_info a LEFT JOIN config_tags_relation b ON a.id = b.id WHERE nvl(a.tenant_id, '!null!') = nvl(?, '!null!') AND b.tag_name IN (?)"},
            {"select count(*) from config_info  a left join config_tags_relation b on a.id=b.id  where  a.tenant_id like ?  and a.data_id like ?  and b.tag_name in (?) ",
                    "SELECT count(*) FROM config_info a LEFT JOIN config_tags_relation b ON a.id = b.id WHERE nvl(a.tenant_id, '!null!') LIKE nvl(?, '!null!') AND a.data_id LIKE ? AND b.tag_name IN (?)"
            },
            {" SELECT 1 from `dual`", "SELECT 1 FROM DUAL"},
            {" SELECT '`' from `dual` where `dummy` = 'xx' or 1 = 1", "SELECT '`' FROM DUAL WHERE DUMMY = 'XX' OR 1 = 1"},
            {"INSERT INTO `config_tags_relation`(`id`,`tag_name`,`tag_type`,`data_id`,`group_id`,`tenant_id`) VALUES(?,?,?,?,?,?)",
                    "INSERT INTO CONFIG_TAGS_RELATION (ID, TAG_NAME, TAG_TYPE, DATA_ID, GROUP_ID, TENANT_ID) VALUES (?, ?, ?, ?, ?, ?)"},
            {"update `config_tags_relation` set `tag_name` = '1' where `id` =  99999", "UPDATE CONFIG_TAGS_RELATION SET TAG_NAME = '1' WHERE ID = 99999"},

            {"select nid,data_id,group_id,tenant_id,app_name,src_ip,src_user,op_type,gmt_create,gmt_modified from his_config_info "
                    + "where data_id = ? and group_id = ? and tenant_id = ? order by nid desc limit 0, 10",
                    "SELECT * FROM (SELECT TMP_PAGE.*, ROWNUM ROW_ID FROM (SELECT NID, DATA_ID, GROUP_ID, TENANT_ID, APP_NAME, SRC_IP, SRC_USER, OP_TYPE, GMT_CREATE, GMT_MODIFIED FROM HIS_CONFIG_INFO WHERE DATA_ID = ? AND GROUP_ID = ? AND NVL(TENANT_ID, '!NULL!') = NVL(?, '!NULL!') ORDER BY NID DESC) TMP_PAGE) WHERE ROW_ID > 0 AND ROW_ID <= 10"},

            {"SELECT t.id,data_id,group_id,tenant_id,app_name,md5,type,gmt_modified FROM "
                    + "( SELECT id FROM config_info ORDER BY id LIMIT ?,?  ) g, config_info t WHERE g.id = t.id",
                    "SELECT T.ID, DATA_ID, GROUP_ID, TENANT_ID, APP_NAME, MD5, TYPE, GMT_MODIFIED FROM (SELECT * FROM (SELECT TMP_PAGE.*, ROWNUM ROW_ID FROM (SELECT ID FROM CONFIG_INFO ORDER BY ID) TMP_PAGE) WHERE ROW_ID > ? AND ROW_ID <= ?) G, CONFIG_INFO T WHERE G.ID = T.ID"},

            {"SELECT id FROM config_info as tablaA LIMIT ?", "SELECT * FROM (SELECT TMP_PAGE.*, ROWNUM ROW_ID FROM (SELECT ID FROM CONFIG_INFO TABLAA) TMP_PAGE) WHERE ROW_ID <= ?"},
            {"SELECT id FROM config_info as tablaA LIMIT 1", "SELECT * FROM (SELECT TMP_PAGE.*, ROWNUM ROW_ID FROM (SELECT ID FROM CONFIG_INFO TABLAA) TMP_PAGE) WHERE ROW_ID <= 1"},

            {"SELECT id FROM (select id from config_info as tablaA) as tableB LIMIT ?",
                    "SELECT * FROM (SELECT TMP_PAGE.*, ROWNUM ROW_ID FROM (SELECT ID FROM (SELECT ID FROM CONFIG_INFO TABLAA) TABLEB) TMP_PAGE) WHERE ROW_ID <= ?"},

            {"SELECT tenant_id FROM config_info WHERE tenant_id != '' GROUP BY tenant_id LIMIT ?, ?",
                    "SELECT * FROM (SELECT TMP_PAGE.*, ROWNUM ROW_ID FROM (SELECT TENANT_ID FROM CONFIG_INFO WHERE NVL(TENANT_ID, '!NULL!') != NVL('', '!NULL!') GROUP BY TENANT_ID) TMP_PAGE) WHERE ROW_ID > ? AND ROW_ID <= ?"},

            {" SELECT t.id,data_id,group_id,tenant_id,app_name,content,md5 "
                    + " FROM (  SELECT id FROM config_info WHERE tenant_id like ? ORDER BY id LIMIT ?,? )"
                    + " g, config_info t  WHERE g.id = t.id",
                    "SELECT T.ID, DATA_ID, GROUP_ID, TENANT_ID, APP_NAME, CONTENT, MD5 FROM (SELECT * FROM (SELECT TMP_PAGE.*, ROWNUM ROW_ID FROM (SELECT ID FROM CONFIG_INFO WHERE NVL(TENANT_ID, '!NULL!') LIKE NVL(?, '!NULL!') ORDER BY ID) TMP_PAGE) WHERE ROW_ID > ? AND ROW_ID <= ?) G, CONFIG_INFO T WHERE G.ID = T.ID"},

            {"delete from his_config_info where gmt_modified < ? limit ?", "DELETE FROM HIS_CONFIG_INFO WHERE GMT_MODIFIED < ? AND  ROWNUM <= ?"},

            {"select 1 from dual where DATE_ADD(?, INTERVAL -? SECOND) < ?", "SELECT 1 FROM DUAL WHERE ? + NUMTODSINTERVAL(-?, 'SECOND') < ?"},
            {"select 'aa' app_name from dual", "SELECT 'AA' \"APP_NAME\" FROM DUAL"},
            {"select (select 'aa' app_name from dual) a from dual", "SELECT (SELECT 'AA' \"APP_NAME\" FROM DUAL) A FROM DUAL"},
            {"select dummy app_name from dual", "SELECT DUMMY \"APP_NAME\" FROM DUAL"},
            {"select count(dummy) app_name from dual", "SELECT COUNT(DUMMY) \"APP_NAME\" FROM DUAL"}
    };

    @Test
    void mysql2oracle() {
        for (String[] test : testSqls) {
            assertEquals(test[1].toUpperCase(), SqlHelper.mysql2oracle(test[0]).toUpperCase());
        }
    }
    
    @Test
    void issueUpdateBackquote() {
        String sql = "UPDATE xxl_job_log_report         SET `running_count` = ?,          `suc_count` = ?,          `fail_count` = ?         WHERE `trigger_day` = ?";
        String expeSql = "UPDATE XXL_JOB_LOG_REPORT SET RUNNING_COUNT = ?, SUC_COUNT = ?, FAIL_COUNT = ? WHERE TRIGGER_DAY = ?";
        
        assertEquals(expeSql, SqlHelper.mysql2oracle(sql).toUpperCase());
    }


    

}