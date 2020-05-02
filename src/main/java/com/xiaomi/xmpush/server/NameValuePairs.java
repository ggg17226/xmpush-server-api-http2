package com.xiaomi.xmpush.server;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NameValuePairs {
  private final List<NameValuePair> pairs = new ArrayList<>();

  public List<NameValuePair> getPairs() {
    return this.pairs;
  }

  NameValuePairs nameAndValue(String name, Object... values) {
    if (values != null && values.length > 0) this.pairs.add(new NameValuePair(name, values));
    return this;
  }

  public boolean isEmpty() {
    return (this.pairs == null || this.pairs.size() == 0);
  }

  public String toQueryOrFormData() throws UnsupportedEncodingException {
    StringBuilder data = new StringBuilder();
    if (this.pairs != null && this.pairs.size() > 0) {
      Iterator<NameValuePair> iter = this.pairs.iterator();
      if (iter.hasNext()) {
        NameValuePair pair = iter.next();
        for (Object value : pair.values) {
          if (value != null)
            data.append(pair.name).append("=").append(URLEncoder.encode(value.toString(), "UTF-8"));
        }
        while (iter.hasNext()) {
          pair = iter.next();
          for (Object value : pair.values) {
            if (value != null)
              data.append("&")
                  .append(pair.name)
                  .append("=")
                  .append(URLEncoder.encode(value.toString(), "UTF-8"));
          }
        }
      }
    }
    return data.toString();
  }

  public static class NameValuePair {
    private final String name;

    private final Object[] values;

    public NameValuePair(String name, Object[] values) {
      this.name = name;
      this.values = values;
    }

    public String getName() {
      return this.name;
    }

    public Object[] getValues() {
      return this.values;
    }
  }
}
