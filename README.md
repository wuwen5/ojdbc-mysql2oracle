## Mysql to Oracle

项目初期主要为适应Nacos在Oracle数据库上的部署而开发，采用驱动层代理来拦截SQL，并实时进行SQL语法转换， 可考虑在此基础上扩展支持其他数据库的语法转换。

## 功能
  mysql到oracle语法转换, 支持部分mysql语法转换.
  在 nacos-1.4.x、nacos-2.x、xxl-job 中已测试.
  
### nacos-2.2.x ~ 版本配置方式(nacos-2.2以上版本)  

- 本项目本地打包后，将`ojdbc-mysql2oracle-x.y.x.jar`放入`nacos-server/plugins`目录
- 增加`conf/application.properties`以下配置 (db.url.0配置正确oracle jdbcUrl)
  ```
  db.pool.config.driverClassName=com.cenboomh.commons.ojdbc.driver.MysqlToOracleDriver
  spring.datasource.platform=mysql
  ``` 

### nacos-1.4.x ~ Nacos-2.1 版本配置方式(nacos-2.2以下版本)  
  因nacos-1.4.x无法直接配置驱动类，所以需通过javaagent方式加载
  - 推荐将包放入nacos-server.jar同级目录
  - Linux 修改start.sh 
  
    ```bash 
    #将JAVA_OPT="${JAVA_OPT} -jar ${BASE_DIR}/target/${SERVER}.jar" 改为
    JAVA_OPT="${JAVA_OPT} -javaagent:${BASE_DIR}/target/ojdbc-mysql2oracle-1.0.0-SNAPSHOT.jar -jar ${BASE_DIR}/target/${SERVER}.jar" 
    ```
  - Windows 修改start.bat
    
    ```cmd
    rem 将 set "NACOS_OPTS=%NACOS_OPTS% -jar %BASE_DIR%\target\%SERVER%.jar" 改为
    set "NACOS_OPTS=%NACOS_OPTS% -javaagent:%BASE_DIR%\target\ojdbc-mysql2oracle-1.0.0-SNAPSHOT.jar -jar %BASE_DIR%\target\%SERVER%.jar"
    ```
    
  - 修改 nacos/conf/application.properties, 配置oracle连接信息
     ```properties
     ### Count of DB:
     db.num=1

     ### Connect URL of DB:
     db.url.0=jdbc:oracle:thin:@ip:port:orcl
     db.user.0=nacos
     db.password.0=nacos
     ```

## agent方式替换驱动的相关配置

  对于不支持驱动类配置的项目可通过`agent`方式替换驱动

  - 默认替换的mysql驱动为```com.mysql.cj.jdbc.Driver```
  ```
  java -javaagent:path/to/ojdbc-mysql2oracle-x.y.x.jar
  ```
  
  - 指定驱动类
  ```
  java -javaagent:path/to/ojdbc-mysql2oracle-x.y.x.jar=driverClassName:com.mysql.jdbc.Driver
  ```

## 支持的语法
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

