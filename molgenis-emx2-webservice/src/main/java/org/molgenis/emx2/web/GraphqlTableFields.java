package org.molgenis.emx2.web;

import graphql.Scalars;
import graphql.schema.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.Filter;
import org.molgenis.emx2.sql.SelectColumn;
import org.molgenis.emx2.sql.SqlGraphQuery;
import org.molgenis.emx2.sql.SqlTypeUtils;
import org.molgenis.emx2.utils.MolgenisException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.schema.GraphQLEnumType.newEnum;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Order.ASC;
import static org.molgenis.emx2.Order.DESC;
import static org.molgenis.emx2.sql.Filter.f;
import static org.molgenis.emx2.sql.SqlGraphQuery.*;
import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.GraphqlApi.*;

public class GraphqlTableFields {

  private GraphqlTableFields() {
    // hide constructor
  }

  static final GraphQLEnumType orderByEnum =
      newEnum().name("MolgenisOrderByEnum").value(ASC.name(), ASC).value(DESC.name(), DESC).build();

  public static GraphQLFieldDefinition.Builder tableQueryField(Table table) {
    GraphQLObjectType tableType = GraphqlTableFields.createTableObjectType(table);
    GraphQLObjectType connection =
        GraphqlTableFields.createTableableConnectionObjectType(table, tableType);

    return newFieldDefinition()
        .name(table.getName())
        .type(connection)
        .dataFetcher(fetcherForQuery(table))
        .argument(
            GraphQLArgument.newArgument()
                .name(FILTER)
                .type(createTableFilterInputObjectType(table.getMetadata()))
                .build())
        .argument(GraphQLArgument.newArgument().name(SEARCH).type(Scalars.GraphQLString).build());
  }

  public static GraphQLFieldDefinition.Builder tableMutationField(Table table) {
    GraphQLInputObjectType inputType = createTableInputType(table);

    return newFieldDefinition()
        .name("save" + table.getName())
        .type(typeForMutationResult)
        .dataFetcher(fetcherForSave(table))
        .argument(
            GraphQLArgument.newArgument().name(Constants.INPUT).type(GraphQLList.list(inputType)));
  }

  private static DataFetcher fetcherForSave(Table aTable) {
    return dataFetchingEnvironment -> {
      try {
        Table table = aTable;
        List<Map<String, Object>> map = dataFetchingEnvironment.getArgument(Constants.INPUT);
        int count = table.update(convertToRows(map));
        return resultMessage("success. saved " + count + " records");
      } catch (MolgenisException me) {
        return transform(me);
      }
    };
  }

  private static DataFetcher fetcherForQuery(Table aTable) {
    return dataFetchingEnvironment -> {
      Table table = aTable;
      SqlGraphQuery q = new SqlGraphQuery(table);
      q.select(mapSelect(dataFetchingEnvironment.getSelectionSet()));
      if (dataFetchingEnvironment.getArgument(FILTER) != null) {
        q.filter(mapFilters(table, dataFetchingEnvironment.getArgument(FILTER)));
      }
      String search = dataFetchingEnvironment.getArgument(SEARCH);
      if (search != null) {
        q.search(search);
      }

      return transform(q.retrieve());
    };
  }

  private static GraphQLObjectType createTableableConnectionObjectType(
      Table table, GraphQLObjectType tableType) {
    GraphQLObjectType.Builder connectionBuilder = newObject().name(table.getName() + "Connection");
    connectionBuilder.field(
        newFieldDefinition().name(DATA_AGG_FIELD).type(createTableAggregationType(table)));
    connectionBuilder.field(
        newFieldDefinition()
            .name(DATA_FIELD)
            .type(GraphQLList.list(tableType))
            .argument(GraphQLArgument.newArgument().name(LIMIT).type(Scalars.GraphQLInt).build())
            .argument(GraphQLArgument.newArgument().name(OFFSET).type(Scalars.GraphQLInt).build())
            .argument(
                GraphQLArgument.newArgument()
                    .name(ORDERBY)
                    .type(tableOrderByInputObjectType(table))
                    .build()));
    return connectionBuilder.build();
  }

  private static GraphQLInputObjectType createTableInputType(Table table) {
    GraphQLInputObjectType.Builder inputBuilder = newInputObject().name(table.getName() + "Input");
    for (Column col : table.getMetadata().getColumns()) {
      GraphQLInputType type;
      ColumnType columnType = col.getColumnType();
      if (REF.equals(columnType)) columnType = SqlTypeUtils.getRefColumnType(col);
      switch (columnType) {
        case DECIMAL:
          type = Scalars.GraphQLBigDecimal;
          break;
        case INT:
          type = Scalars.GraphQLInt;
          break;
        default:
          type = Scalars.GraphQLString;
          break;
      }
      inputBuilder.field(newInputObjectField().name(col.getName()).type(type));
    }
    return inputBuilder.build();
  }

  private static GraphQLObjectType createTableObjectType(Table table) {
    GraphQLObjectType.Builder tableBuilder = newObject().name(table.getName());
    for (Column col : table.getMetadata().getColumns())
      switch (col.getColumnType()) {
        case DECIMAL:
          tableBuilder.field(
              newFieldDefinition().name(col.getName()).type(Scalars.GraphQLBigDecimal));
          break;
        case INT:
          tableBuilder.field(newFieldDefinition().name(col.getName()).type(Scalars.GraphQLInt));
          break;
        case REF:
          tableBuilder.field(
              newFieldDefinition()
                  .name(col.getName())
                  .type(GraphQLTypeReference.typeRef(col.getRefTableName())));
          break;
        case REF_ARRAY:
          tableBuilder.field(
              newFieldDefinition()
                  .name(col.getName())
                  .type(GraphQLList.list(GraphQLTypeReference.typeRef(col.getRefTableName())))
                  .argument(
                      GraphQLArgument.newArgument().name(LIMIT).type(Scalars.GraphQLInt).build())
                  .argument(
                      GraphQLArgument.newArgument().name(OFFSET).type(Scalars.GraphQLInt).build())
                  .argument(
                      GraphQLArgument.newArgument()
                          .name(ORDERBY)
                          .type(GraphQLTypeReference.typeRef(col.getRefTableName() + ORDERBY))
                          .build()));
          tableBuilder.field(
              newFieldDefinition()
                  .name(col.getName() + "_agg")
                  .type(GraphQLTypeReference.typeRef(col.getRefTableName() + "Aggregate")));
          break;
        default:
          tableBuilder.field(newFieldDefinition().name(col.getName()).type(Scalars.GraphQLString));
      }
    return tableBuilder.build();
  }

  private static GraphQLObjectType createTableAggregationType(Table table) {
    GraphQLObjectType.Builder builder = newObject().name(table.getName() + "Aggregate");
    builder.field(newFieldDefinition().name("count").type(Scalars.GraphQLInt));
    for (Column col : table.getMetadata().getColumns()) {
      ColumnType type = col.getColumnType();
      if (INT.equals(type) || DECIMAL.equals(type)) {
        builder.field(
            newFieldDefinition()
                .name(col.getName())
                .type(
                    newObject()
                        .name(table.getName() + "AggregatorFor" + col.getName())
                        .field(newFieldDefinition().name(MAX_FIELD).type(graphQLTypeOf(type)))
                        .field(newFieldDefinition().name(MIN_FIELD).type(graphQLTypeOf(type)))
                        .field(newFieldDefinition().name(AVG_FIELD).type(Scalars.GraphQLFloat))
                        .field(newFieldDefinition().name(SUM_FIELD).type(graphQLTypeOf(type)))));
      }
    }
    return builder.build();
  }

  private static GraphQLInputObjectType createTableFilterInputObjectType(TableMetadata table) {
    GraphQLInputObjectType.Builder filterBuilder =
        newInputObject().name(table.getTableName() + FILTER1);
    for (Column col : table.getColumns()) {
      if (REF.equals(col.getColumnType()) || REF_ARRAY.equals((col.getColumnType()))) {
        filterBuilder.field(
            newInputObjectField()
                .name(col.getName())
                .type(GraphQLTypeReference.typeRef(col.getRefTableName() + FILTER1))
                .build());
      } else {
        filterBuilder.field(
            newInputObjectField()
                .name(col.getName())
                .type(columnFilterInputObjectType(col))
                .build());
      }
    }
    return filterBuilder.build();
  }

  private static GraphQLInputObjectType tableOrderByInputObjectType(Table table) {
    GraphQLInputObjectType.Builder orderByBuilder =
        newInputObject().name(table.getName() + ORDERBY);
    for (Column col : table.getMetadata().getColumns()) {
      if (!REF_ARRAY.equals((col.getColumnType()))) {
        orderByBuilder.field(newInputObjectField().name(col.getName()).type(orderByEnum));
      }
    }
    return orderByBuilder.build();
  }

  // cache for the next method
  static Map<ColumnType, GraphQLInputObjectType> filterInputTypes = new LinkedHashMap<>();

  private static GraphQLInputObjectType columnFilterInputObjectType(Column column) {
    ColumnType type = column.getColumnType();
    // singleton
    if (filterInputTypes.get(type) == null) {
      String typeName = type.toString().toLowerCase();
      typeName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
      GraphQLInputObjectType.Builder builder =
          newInputObject().name("Molgenis" + typeName + FILTER1);
      for (Operator operator : type.getOperators()) {
        builder.field(
            newInputObjectField()
                .name(operator.getAbbreviation())
                .type(GraphQLList.list(graphQLTypeOf(type))));
      }
      filterInputTypes.put(type, builder.build());
    }
    return filterInputTypes.get(type);
  }

  private static GraphQLScalarType graphQLTypeOf(ColumnType type) {
    switch (type) {
      case BOOL:
      case BOOL_ARRAY:
        return Scalars.GraphQLBoolean;
      case INT:
      case INT_ARRAY:
        return Scalars.GraphQLInt;
      case DECIMAL:
      case DECIMAL_ARRAY:
        return Scalars.GraphQLFloat;
      case DATE_ARRAY:
      case DATE:
      case DATETIME:
      case DATETIME_ARRAY:
      case STRING:
      case TEXT:
      case STRING_ARRAY:
      case TEXT_ARRAY:
      case UUID:
      case UUID_ARRAY:
      case REF:
      case REF_ARRAY:
      case MREF:
    }
    return Scalars.GraphQLString;
  }

  private static Filter[] mapFilters(Table table, Map<String, Object> filter) {
    List<Filter> subFilters = new ArrayList<>();
    for (Map.Entry<String, Object> entry : filter.entrySet()) {
      Column c = table.getMetadata().getColumn(entry.getKey());
      if (c == null)
        throw new GraphqlApiException(
            "Column " + entry.getKey() + " unknown in table " + table.getName());
      if (REF.equals(c.getColumnType()) || REF_ARRAY.equals(c.getColumnType())) {
        subFilters.add(
            f(
                c.getName(),
                mapFilters(
                    table.getSchema().getTable(c.getRefTableName()), (Map) entry.getValue())));
      } else {
        if (entry.getValue() instanceof Map) {
          subFilters.add(mapToFilter(entry.getKey(), (Map<String, Object>) entry.getValue()));
        } else {
          throw new GraphqlApiException(
              "unknown filter expression " + entry.getValue() + " for column " + entry.getKey());
        }
      }
    }
    return subFilters.toArray(new Filter[subFilters.size()]);
  }

  private static Filter mapToFilter(String name, Map<String, Object> subFilter) {
    Filter f = f(name);
    for (Map.Entry<String, Object> entry2 : subFilter.entrySet()) {
      Operator op = Operator.fromAbbreviation(entry2.getKey());
      if (entry2.getValue() instanceof List) {
        f.add(op, (List) entry2.getValue());
      } else {
        f.add(op, entry2.getValue());
      }
    }
    return f;
  }

  /** creates a list like List.of(field1,field2, path1, List.of(pathsubfield1), ...) */
  private static SelectColumn[] mapSelect(DataFetchingFieldSelectionSet selection) {
    List<SelectColumn> result = new ArrayList<>();
    for (SelectedField s : selection.getFields()) {
      if (!s.getQualifiedName().contains("/") && !s.getSelectionSet().getFields().isEmpty()) {
        SelectColumn sc = new SelectColumn(s.getName(), mapSelect(s.getSelectionSet()));
        // get limit and offset for the selection
        Map<String, Object> args = s.getArguments();
        if (args.containsKey(LIMIT)) {
          sc.setLimit((int) args.get(LIMIT));
        }
        if (args.containsKey(OFFSET)) {
          sc.setOffset((int) args.get(OFFSET));
        }
        if (args.containsKey(ORDERBY)) {
          sc.setOrderBy((Map<String, Order>) args.get(ORDERBY));
        }
        result.add(sc);
      } else {
        result.add(new SelectColumn(s.getName()));
      }
    }
    return result.toArray(new SelectColumn[result.size()]);
  }
}