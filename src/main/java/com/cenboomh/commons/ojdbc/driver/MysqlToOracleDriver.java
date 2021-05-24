package com.cenboomh.commons.ojdbc.driver;

import com.cenboomh.commons.ojdbc.wrapper.ConnectionWrapper;
import oracle.jdbc.driver.OracleDriver;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author wuwen
 */
public class MysqlToOracleDriver extends OracleDriver {

    private static Driver instance = new MysqlToOracleDriver();

    static {
        try {
            DriverManager.registerDriver(MysqlToOracleDriver.instance);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not register MysqlToOracleDriver with DriverManager.", e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        return ConnectionWrapper.wrap(super.connect(url, info));
    }

}
