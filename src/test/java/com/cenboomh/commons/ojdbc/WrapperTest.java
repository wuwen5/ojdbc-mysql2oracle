package com.cenboomh.commons.ojdbc;

import com.cenboomh.commons.ojdbc.wrapper.ConnectionWrapper;
import com.cenboomh.commons.ojdbc.wrapper.StatementWrapper;
import oracle.jdbc.OracleStatement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@SpringBootTest
@Rollback
@Transactional
public class WrapperTest {
    
    @Autowired
    JdbcTemplate jdbcTemplate;
    
    @Test
    void wrap() throws SQLException {
        Connection connection = jdbcTemplate.getDataSource().getConnection();

        Assertions.assertTrue(connection.isWrapperFor(ConnectionWrapper.class));
        connection.unwrap(ConnectionWrapper.class);

        Statement statement = connection.createStatement();
        Assertions.assertTrue(statement.isWrapperFor(StatementWrapper.class));
        statement.unwrap(StatementWrapper.class);


        statement.isWrapperFor(OracleStatement.class);
        statement.unwrap(OracleStatement.class);
    }
}
