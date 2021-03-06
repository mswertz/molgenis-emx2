package org.molgenis.emx2.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;

public class TaskReference {
  // for the toString method
  private static ObjectMapper mapper =
      new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

  private String id;
  private String schemaName;

  public TaskReference(String id, Schema schema) {
    this.id = id;
    this.schemaName = schema.getName();
  }

  public String getId() {
    return id;
  }

  public String getUrl() {
    return "/" + schemaName + "/api/task/" + id;
  }

  public String toString() {
    try {
      return mapper.writeValueAsString(this);
    } catch (Exception e) {
      throw new MolgenisException("internal error");
    }
  }
}
