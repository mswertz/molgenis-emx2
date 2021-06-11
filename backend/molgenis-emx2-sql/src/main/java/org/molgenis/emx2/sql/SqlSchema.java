package org.molgenis.emx2.sql;

import static org.molgenis.emx2.ColumnType.REFBACK;
import static org.molgenis.emx2.sql.SqlColumnExecutor.executeRemoveRefConstraints;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;
import static org.molgenis.emx2.sql.SqlSchemaMetadataExecutor.*;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.executeRevokeAllPrivileges;
import static org.molgenis.emx2.utils.TableSort.sortTableByDependency;

import java.util.*;
import org.jooq.DSLContext;
import org.molgenis.emx2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlSchema implements Schema {
  private static Logger logger = LoggerFactory.getLogger(SqlSchema.class);
  private SqlDatabase db;
  private SqlSchemaMetadata metadata;

  public SqlSchema(SqlDatabase db, SqlSchemaMetadata metadata) {
    this.db = db;
    this.metadata = metadata;
  }

  @Override
  public SqlTable getTable(String name) {
    SqlTableMetadata tableMetadata = getMetadata().getTableMetadata(name);
    if (tableMetadata == null) return null;
    if (tableMetadata.exists()) return new SqlTable(db, tableMetadata);
    else return null;
  }

  @Override
  public List<Table> getTablesSorted() {
    List<TableMetadata> tableMetadata = getMetadata().getTables();
    sortTableByDependency(tableMetadata);
    List<Table> result = new ArrayList<>();
    for (TableMetadata tm : tableMetadata) {
      result.add(new SqlTable(db, (SqlTableMetadata) tm));
    }
    return result;
  }

  @Override
  public void dropTable(String name) {
    getMetadata().drop(name);
  }

  @Override
  public void addMember(String user, String role) {
    executeAddMembers(getMetadata().getJooq(), this, new Member(user, role));
  }

  @Override
  public List<Member> getMembers() {
    // only admin or other members can see
    if (db.getActiveUser() == null
        || ADMIN.equals(db.getActiveUser())
        || getRoleForActiveUser() != null) {
      return executeGetMembers(getMetadata().getJooq(), getMetadata());
    } else {
      return new ArrayList<>();
    }
  }

  @Override
  public void removeMembers(List<Member> members) {
    tx(database -> executeRemoveMembers(getMetadata().getJooq(), this, members));
  }

  @Override
  public void removeMember(String user) {
    removeMembers(new Member(user, null));
  }

  @Override
  public void removeMembers(Member... members) {
    removeMembers(Arrays.asList(members));
  }

  @Override
  public List<String> getRoles() {
    return executeGetRoles(getMetadata().getJooq(), this.getMetadata().getName());
  }

  @Override
  public List<RolePrivilege> getPrivileges() {
    return executeGetPrivileges(getMetadata().getJooq(), this);
  }

  @Override
  public String getRoleForUser(String user) {
    if (user == null) user = ANONYMOUS;
    user = user.trim();
    for (Member m : executeGetMembers(getMetadata().getJooq(), getMetadata())) {
      if (m.getUser().equals(user)) return m.getRole();
    }
    return null;
  }

  @Override
  public synchronized List<String> getInheritedRolesForUser(String user) {
    if (user == null) return new ArrayList<>();
    user = user.trim();
    // elevate permissions temporarily
    String current = db.getActiveUser();
    db.clearActiveUser();
    try {
      return SqlSchemaMetadataExecutor.getInheritedRoleForUser(
          db.getJooq(), this.getMetadata().getName(), user);
    } finally {
      if (current != null) {
        db.setActiveUser(current);
      }
    }
  }

  @Override
  public String getRoleForActiveUser() {
    return getRoleForUser(db.getActiveUser());
  }

  @Override
  public List<String> getInheritedRolesForActiveUser() {
    return getInheritedRolesForUser(db.getActiveUser());
  }

  @Override
  public Table create(TableMetadata metadata) {
    getMetadata().create(metadata);
    return getTable(metadata.getTableName());
  }

  @Override
  public void create(TableMetadata... metadata) {
    getMetadata().create(metadata);
  }

  @Override
  public Database getDatabase() {
    return db;
  }

  @Override
  public SqlSchemaMetadata getMetadata() {
    return metadata;
  }

  @Override
  public Collection<String> getTableNames() {
    return getMetadata().getTableNames();
  }

  @Override
  public Query query(String tableName) {
    return getTable(tableName).query();
  }

  @Override
  public Query agg(String tableName) {
    return getTable(tableName).agg();
  }

  @Override
  public Query query(String field, SelectColumn... selection) {
    return new SqlQuery(this.getMetadata(), field, selection);
  }

  @Override
  public void tx(Transaction transaction) {
    db.tx(transaction);
  }

  @Override
  public void discard(SchemaMetadata discardSchema) {
    // check if all tables and columns are known
    List<String> errors = new ArrayList<>();
    for (TableMetadata discardTable : discardSchema.getTables()) {
      TableMetadata existingTable = getMetadata().getTableMetadata(discardTable.getTableName());
      if (existingTable == null) {
        errors.add("Table '" + discardTable.getTableName() + " not found");
      } else {
        for (String discardColumn : discardTable.getLocalColumnNames()) {
          if (!existingTable.getLocalColumnNames().contains(discardColumn))
            errors.add(
                "Column '" + discardTable.getTableName() + "." + discardColumn + " not found");
        }
      }
    }
    if (!errors.isEmpty()) {
      throw new MolgenisException(
          "Discard failed: Discard of tables out of schema "
              + getMetadata().getName()
              + " failed: "
              + String.join("\n", errors));
    }

    // get all tables, sorted and use that as scaffold
    tx(
        dsl -> {
          List<TableMetadata> tables = getMetadata().getTables();
          Collections.reverse(tables);

          // remove whole tables unless columns attached
          for (TableMetadata existingTable : tables) {
            // if no coluns then we delete whole table
            if (discardSchema.getTableMetadata(existingTable.getTableName()) != null) {
              TableMetadata discardTable =
                  discardSchema.getTableMetadata(existingTable.getTableName());
              if (discardTable.getLocalColumnNames().isEmpty()
                  || discardTable
                      .getLocalColumnNames()
                      .containsAll(existingTable.getLocalColumnNames())) {
                dropTable(existingTable.getTableName());
                MetadataUtils.deleteTable(getMetadata().getJooq(), existingTable);
              } else {
                // or column names
                for (String discardColumn : discardTable.getLocalColumnNames()) {
                  Column existingColumn = existingTable.getColumn(discardColumn);
                  existingTable.dropColumn(discardColumn);
                  MetadataUtils.deleteColumn(getMetadata().getJooq(), existingColumn);
                }
              }
            }
          }
        });
  }

  @Override
  public void migrate(SchemaMetadata mergeSchema) {
    tx(
        database -> {

          // create list, sort dependency order
          List<TableMetadata> mergeTableList = new ArrayList<>();
          mergeSchema.setDatabase(database);
          for (String tableName : mergeSchema.getTableNames()) {
            mergeTableList.add(mergeSchema.getTableMetadata(tableName));
          }
          sortTableByDependency(mergeTableList);

          // first loop
          // create, alter
          // (drop is last thing we do, as columns might need deleting)
          // todo, fix if we rename to existing tables, then order matters
          for (TableMetadata mergeTable : mergeTableList) {

            // get the old table, if exists
            Table oldTableSource =
                mergeTable.getOldName() == null
                    ? this.getTable(mergeTable.getTableName())
                    : this.getTable(mergeTable.getOldName());
            TableMetadata oldTable = oldTableSource != null ? oldTableSource.getMetadata() : null;

            // set oldName in case table does exist, and oldName was not provided
            if (mergeTable.getOldName() == null && oldTable != null) {
              mergeTable.setOldName(oldTable.getTableName());
            }

            // create table if not exists
            if (oldTable == null && !mergeTable.isDrop()) {
              this.create(new TableMetadata(mergeTable.getTableName())); // only the name
            } else if (oldTable != null
                && !oldTable.getTableName().equals(mergeTable.getTableName())) {
              this.getMetadata().renameTable(oldTable, mergeTable.getTableName());
            }
          }

          // for create/alter
          //  add missing columns (except refback),
          //  remove triggers in case of table name or column type changes
          //  remove refback
          List<String> created = new ArrayList<>();
          for (TableMetadata mergeTable : mergeTableList) {

            if (!mergeTable.isDrop()) {
              TableMetadata oldTable = this.getTable(mergeTable.getTableName()).getMetadata();

              // set inheritance
              if (mergeTable.getInherit() != null) {
                if (mergeTable.getImportSchema() != null) {
                  oldTable.setImportSchema(mergeTable.getImportSchema());
                }
                oldTable.setInherit(mergeTable.getInherit());
              } else if (oldTable.getInherit() != null) {
                oldTable.removeInherit();
              }

              // update table settings
              oldTable.setSettings(mergeTable.getSettings());
              oldTable.setDescription(mergeTable.getDescription());
              oldTable.setSemantics(mergeTable.getSemantics());
              MetadataUtils.saveTableMetadata(db.getJooq(), oldTable);

              // add missing (except refback),
              // remove triggers if existing column if type changed
              // drop columns marked with 'drop'
              for (Column newColumn : mergeTable.getColumns()) {
                Column oldColumn =
                    newColumn.getOldName() != null
                        ? oldTable.getColumn(newColumn.getOldName())
                        : oldTable.getColumn(newColumn.getName());

                // drop columns that need dropping
                if (newColumn.isDrop()) {
                  oldTable.dropColumn(oldColumn.getName());
                } else
                // if new column and not inherited
                if (oldColumn == null
                    && !(oldTable.getInherit() != null
                        && oldTable.getInheritedTable().getColumn(newColumn.getName()) != null)
                    && !newColumn.getColumnType().equals(REFBACK)) {
                  oldTable.add(newColumn);
                  created.add(newColumn.getTableName() + "." + newColumn.getName());
                } else
                // if column exist but type has changed remove triggers
                if (oldColumn != null
                    && !newColumn.getColumnType().equals(oldColumn.getColumnType())) {
                  executeRemoveRefConstraints(getMetadata().getJooq(), oldColumn);
                }
              }
            }
          }

          // second pass,
          // update existing columns to the new types, and new names, reconnect refback
          for (TableMetadata newTable : mergeTableList) {
            if (!newTable.isDrop()) {
              TableMetadata oldTable = this.getTable(newTable.getTableName()).getMetadata();
              for (Column newColumn : newTable.getNonInheritedColumns()) {
                Column oldColumn =
                    newColumn.getOldName() != null
                        ? oldTable.getColumn(newColumn.getOldName()) // when renaming
                        : oldTable.getColumn(newColumn.getName()); // when not renaming

                if (oldColumn != null && !newColumn.isDrop()) {
                  if (!created.contains(newColumn.getTableName() + "." + newColumn.getName())) {
                    oldTable.alterColumn(oldColumn.getName(), newColumn);
                  }
                } else
                // don't forget to add the refbacks
                if (oldColumn == null && newColumn.getColumnType().equals(REFBACK)) {
                  this.getTable(newTable.getTableName()).getMetadata().add(newColumn);
                }
              }
            }
          }

          // finally, drop tables, in reverse dependency order
          Collections.reverse(mergeTableList);
          for (TableMetadata mergeTable : mergeTableList) {
            // idempotent so we only drop if exists
            if (mergeTable.isDrop() && getTable(mergeTable.getOldName()) != null) {
              getTable(mergeTable.getOldName()).getMetadata().drop();
            }
          }
        });
    this.getMetadata().reload();
    db.getListener().schemaChanged(getName());
  }

  public String getName() {
    return getMetadata().getName();
  }

  public void createRole(String role) {
    executeCreateRole(db.getJooq(), this, role);
    logger.debug(db.getActiveUser() + " created role " + role);
  }

  public void dropRole(String role) {
    db.getJooq()
        .transaction(
            t -> {
              DSLContext dsl = t.dsl();
              getPrivileges()
                  .forEach(
                      p -> {
                        if (p.getRole().equals(role)) {
                          executeRevokeAllPrivileges(dsl, getTable(p.getTable()), p.getRole());
                        }
                      });
              executeDropRole(dsl, this, role);
            });
    logger.debug(db.getActiveUser() + " dropped role " + role);
  }
}
