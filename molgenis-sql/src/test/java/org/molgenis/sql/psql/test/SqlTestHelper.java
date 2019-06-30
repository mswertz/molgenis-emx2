package org.molgenis.sql.psql.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.molgenis.Column;
import org.molgenis.Database;
import org.molgenis.MolgenisException;
import org.molgenis.sql.SqlDatabase;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class SqlTestHelper {

  private static HikariDataSource dataSource = null;
  private static DSLContext jooq = null;
  private static SqlDatabase db = null;

  public static Database getEmptyDatabase() throws MolgenisException, SQLException {
    if (db == null) {
      Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
      rootLogger.setLevel(Level.INFO);

      String userName = "molgenis";
      String password = "molgenis";
      String url = "jdbc:postgresql:molgenis";

      // create data source
      dataSource = new HikariDataSource();
      dataSource.setJdbcUrl(url);
      dataSource.setUsername(userName);
      dataSource.setPassword(password);

      Connection conn = dataSource.getConnection();

      // setup Jooq
      jooq = DSL.using(conn, SQLDialect.POSTGRES_10);
      emptyDatabase();
      // create database to test against
      db = new SqlDatabase(dataSource);
    }
    return db;
  }

  public static Database reload() throws MolgenisException {
    return new SqlDatabase(dataSource);
  }

  public static void emptyDatabase() {
    deleteAll();
  }

  public static void deleteAll() {
    // delete all foreign key constaints
    for (org.jooq.Table t : jooq.meta().getTables()) {
      for (ForeignKey k : (List<ForeignKey>) t.getReferences()) {
        jooq.alterTable(t).dropConstraint(k.getName()).execute();
      }
    }
    // delete all tables
    for (org.jooq.Table t : jooq.meta().getTables()) {
      jooq.dropTable(t).execute();
    }
  }

  public static HikariDataSource getDataSource() {
    return dataSource;
  }

  public static DSLContext getJooq() {
    return jooq;
  }

  public static void checkColumnExists(Column c) throws MolgenisException {
    List<org.jooq.Table<?>> tables =
        SqlTestHelper.getJooq().meta().getTables(c.getTable().getName());
    if (tables.size() == 0)
      throw new MolgenisException("Table '" + c.getTable().getName() + "' does not exist");
    org.jooq.Table<?> table = tables.get(0);
    Field f = table.field(c.getName());
    if (f == null)
      throw new MolgenisException("Field '" + c.getName() + "." + c.getName() + "' does not exist");
  }
}
