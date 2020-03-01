import java.io.*;
import java.util.*;
import org.json.*;


class JsonManager
{
  public JsonManager() {
  }

  // Given a JSON snippet, get the requested info
  public String getInfo(String content, String key) {
    JSONObject json = new JSONObject(content);
    try {
      String info = json.getString(key);
      return info;
    } catch(Exception e) {
      return null;
    }
  }


  // Given a name-url pair, construct a JSON snippet with the name-value pair
  public String createJson(String name, String url) {
    JSONObject json = new JSONObject();
    json.put("name", name);
    json.put("url", url);

    return json.toString();
  }
}


