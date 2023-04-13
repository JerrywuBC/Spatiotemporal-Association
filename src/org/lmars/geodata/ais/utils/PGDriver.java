package org.lmars.geodata.ais.utils;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.beans.PropertyVetoException;
import java.util.Map;

public class PGDriver {
    private ComboPooledDataSource pgdb = null;
    private static PGDriver pgDriver = new PGDriver();

    public static PGDriver getInstance() {
        return pgDriver;
    }

    public void init(Map<String, String> conf) {
        String dbIP = conf.get("cachedb.ip");
        String dbPort = conf.get("cachedb.port");
        String database = conf.get("cachedb.name");
        String userName = conf.get("cachedb.user");
        String password = conf.get("cachedb.password");

        ComboPooledDataSource cpds = new ComboPooledDataSource();
        try {
            cpds.setDriverClass("org.postgresql.Driver");
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
        cpds.setJdbcUrl("jdbc:postgresql://" + dbIP + ":" + dbPort + "/" + database);
        cpds.setUser(userName);
        cpds.setPassword(password);

        cpds.setMinPoolSize(5);
        cpds.setAcquireIncrement(5);
        cpds.setMaxPoolSize(20);
        cpds.setMaxStatements(300);

        this.pgdb = cpds;
    }

    public ComboPooledDataSource getPGDB() {
        return this.pgdb;
    }
}
