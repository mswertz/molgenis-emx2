package org.molgenis;

public interface Column {

  Table getTable();

  String getName();

  Type getType();

  Boolean isNullable();

  Boolean isReadonly();

  Boolean isUnique();

  String getDefaultValue();

  String getRefTableName();

  String getRefColumnName() throws MolgenisException;

  String getReverseColumnName();

  String getReverseRefColumn();

  String getJoinTable();

  String getDescription();

  Column getRefColumn() throws MolgenisException;

  Column nullable(boolean nillable) throws MolgenisException;

  void setReadonly(boolean readonly);

  void setDescription(String description);

  void setDefaultValue(String defaultValue);

  Column unique() throws MolgenisException;

  // for fluent api in Table
  Column addColumn(String name) throws MolgenisException;

  Column addColumn(String name, Type type) throws MolgenisException;

  Column addRef(String name, String toTable) throws MolgenisException;

  Column addRef(String name, String toTable, String toColumn) throws MolgenisException;

  Column addRefArray(String name, String toTable) throws MolgenisException;

  Column addRefArray(String name, String toTable, String toColumn) throws MolgenisException;

  void primaryKey() throws MolgenisException;

  Column setReference(String refTable, String refColumn);

  Column setReverseReference(String reverseName, String reverseRefColumn);

  Column setJoinTable(String joinTableName);
}
