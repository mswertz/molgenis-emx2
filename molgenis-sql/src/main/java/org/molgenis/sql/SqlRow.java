package org.molgenis.sql;

import org.jooq.Field;
import org.jooq.Record;
import org.molgenis.Row;

import java.sql.Array;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.molgenis.sql.SqlDatabase.RowLevelSecurity.MG_EDIT_ROLE;

public class SqlRow extends Row {

  public SqlRow(Record record) throws SQLException {
    Map<String, Object> values = new LinkedHashMap<>();
    for (Field f : record.fields()) {
      Object value = record.get(f);
      if (value instanceof Array) {
        values.put(f.getName(), ((Array) value).getArray());
      } else {
        values.put(f.getName(), value);
      }
    }
    this.set(values);
  }
}
