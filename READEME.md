## mysql to oracle

# 功能
  mysql语法转换oracle, 支持部分mysql语法转换，目前可应用于nacos
  
### 驱动配置
  驱动支持可配置的项目，配置驱动类
  ```
  spring.datasource.driver-class-name=com.cenboomh.commons.ojdbc.driver.MysqlToOracleDriver
  ```
  
#### `Java`的启动参数配置  
  如驱动类无法指定，可通过javaagent配置
  ```
  java -javaagent:path/to/ojdbc-mysql2oracle-x.y.x.jar
  ```
