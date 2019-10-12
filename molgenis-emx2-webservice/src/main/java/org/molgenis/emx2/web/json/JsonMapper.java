package org.molgenis.emx2.web.json;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.*;
import org.jooq.DSLContext;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.Row;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JsonMapper {

  private JsonMapper() {
    // hide constructor
  }

  private static ObjectWriter writer;

  public static String schemaToJson(SchemaMetadata schema) throws JsonProcessingException {
    List<TableMetadata> tables = new ArrayList<>();
    for (String tableName : schema.getTableNames()) {
      tables.add(schema.getTableMetadata(tableName));
    }
    return getWriter().writeValueAsString(tables);
  }

  public static String tableToJson(TableMetadata table) throws JsonProcessingException {
    return getWriter().writeValueAsString(table);
  }

  public static String rowsToJson(Iterable<Row> rows) throws JsonProcessingException {
    List<Map<String, Object>> values = new ArrayList<>();
    for (Row r : rows) {
      Map<String, Object> map = r.getValueMap();
      preprocessRow(map);
      values.add(r.getValueMap());
    }
    return getWriter().writeValueAsString(values);
  }

  public static String rowToJson(Row row) throws JsonProcessingException {
    Map<String, Object> map = row.getValueMap();
    preprocessRow(map);
    return getWriter().writeValueAsString(map);
  }

  private static void preprocessRow(Map<String, Object> map) {
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (entry.getValue() instanceof UUID)
        map.put(entry.getKey(), ((UUID) entry.getValue()).toString());
    }
  }

  public static ObjectWriter getWriter() {

    if (writer == null) {
      DefaultPrettyPrinter printer =
          new DefaultPrettyPrinter()
              .withArrayIndenter(new DefaultIndenter("  ", "\n"))
              .withObjectIndenter(new DefaultIndenter("  ", "\n"));
      writer =
          new ObjectMapper()
              .addMixIn(SchemaMetadata.class, MixinForJsonIgnore.class)
              .addMixIn(TableMetadata.class, MixinForJsonIgnore.class)
              .addMixIn(Column.class, MixinForJsonIgnore.class)
              .addMixIn(SqlDatabase.class, MixinForJsonIgnore.class)
              .addMixIn(DSLContext.class, MixinForJsonIgnore.class)
              .setSerializationInclusion(JsonInclude.Include.NON_NULL)
              .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
              .writer(printer);
    }
    return writer;
  }

  @JsonIgnoreType
  public static class MixinForJsonIgnore {}
}