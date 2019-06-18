import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import org.junit.Test;
import org.molgenis.MolgenisException;
import org.molgenis.Query;
import org.molgenis.Row;
import org.molgenis.Table;
import org.molgenis.beans.QueryBean;
import org.molgenis.beans.RowBean;
import org.molgenis.beans.TableBean;
import org.molgenis.emx2.web.JsonMapper;

import java.util.ArrayList;
import java.util.List;

import static org.molgenis.Column.Type.DECIMAL;
import static org.molgenis.Column.Type.INT;
import static org.molgenis.Column.Type.STRING;

public class TestJson {

  @Test
  public void testJsonToRow() throws MolgenisException {

    Table t = new TableBean("Person");
    t.addColumn("FirstName", STRING);
    t.addColumn("Age", INT);
    t.addColumn("Weight", DECIMAL);

    String json = "{\"FirstName\":\"Donald\", \"Age\":50, \"Weight\":15.4}";
    Any any = JsonIterator.deserialize(json);
    Row r = JsonMapper.map(t, any);

    try {
      String malformed = "{\"FirstName\":\"Donald\", \"Age\":\"blaat\"}";
      any = JsonIterator.deserialize(malformed);
      r = JsonMapper.map(t, any);
    } catch (Exception e) {
      System.out.println(e);
    }

    try {
      String malformed = "{\"FirstName\":[\"Donald\",\"Duck\"], \"Age\":50}";
      any = JsonIterator.deserialize(malformed);
      r = JsonMapper.map(t, any);
    } catch (Exception e) {
      System.out.println(e);
    }

    try {
      String malformed = "{\"FirstName\":\"Donald\", \"Age\":50, \"Weight\":\"blaat\"}";
      any = JsonIterator.deserialize(malformed);
      r = JsonMapper.map(t, any);
    } catch (Exception e) {
      System.out.println(e);
    }

    System.out.println(r);
  }

  @Test
  public void testRowsToJson() {
    List<Row> rows = new ArrayList<>();

    rows.add(new RowBean().setString("FirstName", "Donald"));

    System.out.println(JsonMapper.map(rows));
  }

  @Test
  public void testQuery() {
    Query q = new QueryBean();

    q.select("FirstName").select("LastName").where("Age").eq(50);

    System.out.println(JsonMapper.map(q));
  }
}
