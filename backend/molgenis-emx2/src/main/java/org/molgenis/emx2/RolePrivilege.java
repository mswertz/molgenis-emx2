package org.molgenis.emx2;

import java.util.Objects;

public class RolePrivilege {
  private String role;
  private String table;
  private Privilege privilege;

  public RolePrivilege(String role, String table, Privilege privilege) {
    assert role != null;
    this.role = role;
    assert table != null;
    this.table = table;
    assert privilege != null;
    this.privilege = privilege;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    assert role != null;
    this.role = role;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    assert table != null;
    this.table = table;
  }

  public Privilege getPrivilege() {
    return privilege;
  }

  public void setPrivilege(Privilege privilege) {
    assert privilege != null;
    this.privilege = privilege;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof RolePrivilege)) return false;
    RolePrivilege that = (RolePrivilege) o;
    return role.equals(that.role) && table.equals(that.table) && privilege.equals(that.privilege);
  }

  @Override
  public int hashCode() {
    return Objects.hash(role, table, privilege);
  }
}
