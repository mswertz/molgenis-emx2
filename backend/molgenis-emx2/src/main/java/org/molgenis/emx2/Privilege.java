package org.molgenis.emx2;

public enum Privilege {

  // can select
  VIEWER("Viewer"),
  // can insert, update, delete, implies Viewer
  EDITOR("Editor"),
  // extends Editor to create, alter, drop, implies Manager
  MANAGER("Manager"),
  // can add/remove users to schema
  OWNER("Owner"),
  // none
  NONE("None");

  private String name;

  Privilege(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
