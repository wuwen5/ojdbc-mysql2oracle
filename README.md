## mysql to oracle

目前主要为适应nacos部署在oracle而开发

# 功能
  mysql到oracle语法转换, 支持部分mysql语法转换.
  在 nacos-1.4.x 中已测试.
  
#### 驱动类配置

  
#### `Java`的启动参数配置  
  默认替换的mysql驱动为```com.mysql.cj.jdbc.Driver```
  ```
  java -javaagent:path/to/ojdbc-mysql2oracle-x.y.x.jar
  ```
  
  指定驱动类
  ```
  java -javaagent:path/to/ojdbc-mysql2oracle-x.y.x.jar=driverClassName:com.mysql.jdbc.Driver
  ```

----
  
  Nacos中使用
  - 推荐将包放入nacos-server.jar同级目录
  - 修改start.sh 
  
    ``` JAVA_OPT="${JAVA_OPT} -javaagent:${BASE_DIR}/target/ojdbc-mysql2oracle-1.0.0-SNAPSHOT.jar -jar ${BASE_DIR}/target/${SERVER}.jar" ```

#### 支持的语法
- ```select 1``` mysql中不带```from```的查询
- ```insert into ... returning  primarykey```  新增数据返回主键
 <br> 例如: ```JdbcTemplate```中的```int update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder)```
 <br> Mybatis中的  ```<insert id="save" parameterType="com.xxl.job.admin.core.model.XxlJobGroup" useGeneratedKeys="true" keyProperty="id" >``` 
  
    > oracle 11使用触发器+序列做自增长id

- ```delete from xxtable where ... limit ? ```
- ```limit``` 分页语句
- ```空值相等条件``` TODO:目前只支持在nacos中的 TENANT_ID = ?
- ```select 1 from dual as tableA``` from项别名使用as,oracle中去除as
- ```where !(...)``` -> ```where not(...)```
- ``` select 1 from `dual` ``` -> ``` select 1 from dual ``` 删除表名\列名中的反引号
- ```DATE_ADD(?, INTERVAL -? SECOND)```

