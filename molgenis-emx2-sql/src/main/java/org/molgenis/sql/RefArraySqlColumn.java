package org.molgenis.sql;

import org.jooq.DSLContext;
import org.jooq.Name;
import org.molgenis.MolgenisException;

import static org.jooq.impl.DSL.keyword;
import static org.jooq.impl.DSL.name;
import static org.molgenis.Type.REF_ARRAY;
import static org.molgenis.sql.MetadataUtils.saveColumnMetadata;

public class RefArraySqlColumn extends SqlColumn {
  private DSLContext jooq;

  public RefArraySqlColumn(SqlTable table, String columnName, String toTable, String toColumn) {
    super(table, columnName, REF_ARRAY);
    this.setReference(toTable, toColumn);
    this.jooq = table.getJooq();
  }

  @Override
  public RefArraySqlColumn createColumn() throws MolgenisException {
    super.createColumn();
    this.createReferenceExistsTrigger();
    this.createIsReferencedByTrigger();
    saveColumnMetadata(this);
    return this;
  }

  private void createIsReferencedByTrigger() throws MolgenisException {
    Name triggerName = getTriggerName();
    Name toTable = name(getTable().getSchema().getName(), getRefTableName());
    Name thisTable = name(getTable().getSchema().getName(), getTable().getName());
    Name thisColumn = name(getName());
    Name toColumn = name(getRefColumnName());

    Name functionName =
        name(
            getTable().getSchema().getName(),
            getTable().getName() + "_" + getName() + "_REF_ARRAY_TRIGGER2");

    // create the function
    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS"
            + "\n$BODY$"
            + "\n\tBEGIN"
            + "\n\tIF(EXISTS(SELECT * from {1} WHERE OLD.{2} = ANY({3}) ) ) THEN "
            + "RAISE EXCEPTION 'update or delete on table "
            + toTable.unqualifiedName().toString()
            + " violates foreign key constraint "
            + triggerName.unqualifiedName().toString()
            + " on table "
            + thisTable.unqualifiedName().toString()
            + ""
            + "\nDetail: Key ("
            + toColumn.unqualifiedName().toString()
            + ")=(%) is still referenced from table "
            + thisTable.unqualifiedName().toString()
            + "',OLD.{2};"
            + "\n\tEND IF;"
            + "\n\tRETURN NEW;"
            + "\nEND;"
            + "\n$BODY$ LANGUAGE plpgsql;",
        functionName,
        thisTable,
        toColumn,
        thisColumn);

    // create the trigger
    jooq.execute(
        "CREATE CONSTRAINT TRIGGER {0} "
            + "\n\tAFTER UPDATE OR DELETE ON {1} "
            + "\n\tDEFERRABLE INITIALLY IMMEDIATE "
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {2}()",
        triggerName, toTable, functionName);
  }

  /** trigger on this column to check if foreign key exists */
  private void createReferenceExistsTrigger() throws MolgenisException {
    Name triggerName = getTriggerName();
    Name thisTable = name(getTable().getSchema().getName(), getTable().getName());
    Name thisColumn = name(getName());
    Name toTable = name(getTable().getSchema().getName(), getRefTableName());
    Name toColumn = name(getRefColumnName());

    Name functionName =
        name(
            getTable().getSchema().getName(),
            getTable().getName() + "_" + getName() + "_REF_ARRAY_TRIGGER");

    // create the function
    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS"
            + "\n$BODY$"
            + "\nDECLARE"
            + "\n\t test {1};"
            + "\nBEGIN"
            + "\n\ttest =  ARRAY (SELECT from_column FROM (SELECT UNNEST(NEW.{2}) as from_column) as from_table "
            + "LEFT JOIN (SELECT {3} as to_column FROM {4}) as to_table "
            + "ON from_table.from_column=to_table.to_column WHERE to_table.to_column IS NULL);"
            + "\n\tIF(cardinality(test) > 0) THEN "
            + "RAISE EXCEPTION 'insert or update on table "
            + thisTable.unqualifiedName().toString() // for odd reasons {5} and {6} didn't work
            + " violates foreign key constraint "
            + triggerName.unqualifiedName().toString()
            + "\nDetail: ("
            + thisColumn.unqualifiedName().toString()
            + ")=(%) is not present in table "
            + toTable.unqualifiedName().toString()
            + "',array_to_string(test,',');"
            + "\n\tEND IF;"
            + "\n\tRETURN NEW;"
            + "\nEND;"
            + "\n$BODY$ LANGUAGE plpgsql;",
        functionName,
        keyword(
            SqlTypeUtils.getPsqlType(
                    getTable()
                        .getSchema()
                        .getTable(getRefTableName())
                        .getColumn(getRefColumnName()))
                + "[]"),
        thisColumn,
        toColumn,
        toTable);

    // add the trigger
    jooq.execute(
        "CREATE CONSTRAINT TRIGGER {0} "
            + "\n\tAFTER INSERT OR UPDATE OF {1} ON {2} FROM {3}"
            + "\n\tDEFERRABLE INITIALLY IMMEDIATE "
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {4}()",
        triggerName, thisColumn, thisTable, toTable, functionName);
  }

  private Name getTriggerName() throws MolgenisException {
    return name(
        getTable().getName()
            + "."
            + getName()
            + " REFERENCES "
            + getRefTableName()
            + "."
            + getRefColumnName());
  }
}
