package org.molgenis.emx2.examples;

import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.jooq.DSLContext;
import org.molgenis.Database;
import org.molgenis.MolgenisException;
import org.molgenis.Schema;

import java.util.Collection;

import static junit.framework.TestCase.fail;

public class CompareTools {

  private static Javers javers =
      JaversBuilder.javers()
          .registerIgnoredClass(DSLContext.class)
          .registerIgnoredClass(Schema.class)
          .build();

  private CompareTools() {
    // hide constructor
  }

  public static void compare(Schema schema1, Schema schema2) throws MolgenisException {
    Collection<String> tableNames1 = schema1.getTableNames();
    Collection<String> tableNames2 = schema2.getTableNames();

    for (Object tableName : tableNames1)
      if (!tableNames2.contains(tableName))
        throw new MolgenisException(
            "Schema's have different tables: schema2 doesn't contain '" + tableName + "'");
    for (Object tableName : tableNames2)
      if (!tableNames1.contains(tableName))
        throw new MolgenisException(
            "Schema's have different tables: schema1 doesn't contain '" + tableName + "'");

    for (String tableName : tableNames1) {
      Diff diff = javers.compare(schema1.getTable(tableName), schema2.getTable(tableName));

      if (diff.hasChanges()) {
        throw new MolgenisException("Roundtrip test failed: changes, " + diff.toString());
      }
    }
  }

  public static void reloadAndCompare(Database database, Schema schema) throws MolgenisException {
    // remember
    String schemaName = schema.getName();
    Collection<String> tableNames = schema.getTableNames();

    // empty the cache
    database.clearCache();

    // check reload from drive
    Schema schemaLoadedFromDisk = database.getSchema(schemaName);

    for (String tableName : tableNames) {
      Diff diff =
          javers.compare(schema.getTable(tableName), schemaLoadedFromDisk.getTable(tableName));

      if (diff.hasChanges()) {
        throw new MolgenisException("Roundtrip test failed: changes, " + diff.toString());
      }
    }
  }
}
