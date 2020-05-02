package com.xiaomi.xmpush.server;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@Slf4j
public class URLConnectionClient extends AbstractClient {
  private static final SSLHandler sslVerifier = new SSLHandler();

  public URLConnectionClient(
      boolean useProxy,
      boolean needAuth,
      String proxyHost,
      int proxyPort,
      String user,
      String password) {
    super(useProxy, needAuth, proxyHost, proxyPort, user, password);
  }

  public URLConnectionClient(
      boolean useProxy,
      boolean needAuth,
      String proxyHost,
      int proxyPort,
      String user,
      String password,
      int connectTimeout,
      int readTimeout,
      int writeTimeout) {
    super(
        useProxy,
        needAuth,
        proxyHost,
        proxyPort,
        user,
        password,
        connectTimeout,
        readTimeout,
        writeTimeout);
  }

  public AbstractClient.ResponseWrapper post(String url, byte[] body, NameValuePairs headers)
      throws Exception {
    final HttpURLConnection connection = getConnection(url);
    connection.setConnectTimeout(this.connectTimeout);
    connection.setReadTimeout(this.readTimeout);
    connection.setDoOutput(true);
    connection.setUseCaches(false);
    connection.setFixedLengthStreamingMode(body.length);
    connection.setRequestMethod("POST");
    if (headers != null && !headers.isEmpty())
      headers
          .getPairs()
          .forEach(
              header -> {
                if (header.getValues() != null)
                  Arrays.stream(header.getValues())
                      .filter(Objects::nonNull)
                      .forEach(h -> {});
              });
    try (OutputStream out = connection.getOutputStream()) {
      out.write(body);
    }
    final int status = connection.getResponseCode();
    if (status >= 500) {
      log.debug("XmPush service is unavailable (status " + status + ")");
      return null;
    }
    try (InputStream is =
        (status == 200) ? connection.getInputStream() : connection.getErrorStream()) {
      final byte[] content = readFixedLengthBytes(is, connection.getContentLength());
      return new AbstractClient.ResponseWrapper() {
        public int status() {
          return status;
        }

        public byte[] content() {
          return content;
        }

        public String header(String name) {
          return connection.getHeaderField(name);
        }
      };
    }
  }

  private byte[] readFixedLengthBytes(InputStream is, int length) throws IOException {
    byte[] buffer = new byte[length];
    is.read(buffer);
    return buffer;
  }

  public AbstractClient.ResponseWrapper get(String url, NameValuePairs headers) throws Exception {
    final HttpURLConnection connection = getConnection(url);
    connection.setConnectTimeout(this.connectTimeout);
    connection.setReadTimeout(this.readTimeout);
    connection.setDoOutput(true);
    connection.setUseCaches(false);
    connection.setRequestMethod("GET");
    if (headers != null && !headers.isEmpty())
      headers
          .getPairs()
          .forEach(
              header -> {
                if (header.getValues() != null)
                  Arrays.stream(header.getValues())
                      .filter(Objects::nonNull)
                      .forEach(h -> {});
              });
    final int status = connection.getResponseCode();
    if (status >= 500) {
      log.debug("XmPush service is unavailable (status " + status + ")");
      return null;
    }
    try (InputStream is =
        (status == 200) ? connection.getInputStream() : connection.getErrorStream()) {
      final byte[] content = readFixedLengthBytes(is, connection.getContentLength());
      return new AbstractClient.ResponseWrapper() {
        public int status() {
          return status;
        }

        public byte[] content() {
          return content;
        }

        public String header(String name) {
          return connection.getHeaderField(name);
        }
      };
    }
  }

  public AbstractClient.ResponseWrapper upload(String url, File file, NameValuePairs headers)
      throws Exception {
    final HttpURLConnection connection = getConnection(url);
    String BOUNDARY = "------WKFB" + UUID.randomUUID().toString();
    String TWO_HYPHENS = "--";
    String LINE_END = "\r\n";
    connection.setConnectTimeout(this.connectTimeout);
    connection.setReadTimeout(this.readTimeout);
    connection.setDoOutput(true);
    connection.setUseCaches(false);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Charsert", "UTF-8");
    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
    StringBuffer sb = new StringBuffer();
    sb.append(LINE_END);
    sb.append(TWO_HYPHENS);
    sb.append(BOUNDARY);
    sb.append(LINE_END);
    sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"");
    sb.append(LINE_END);
    sb.append("Content-Type: image/*");
    sb.append(LINE_END);
    sb.append("Content-Lenght: " + file.length());
    sb.append(LINE_END);
    sb.append(LINE_END);
    FileInputStream in = new FileInputStream(file);
    try (OutputStream out = connection.getOutputStream()) {
      out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
      int bytes = 0;
      byte[] buffer = new byte[1024];
      while ((bytes = in.read(buffer)) != -1) out.write(buffer, 0, bytes);
      byte[] endData = (LINE_END + TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END).getBytes();
      out.write(endData);
      out.flush();
    }
    final int status = connection.getResponseCode();
    if (status >= 500) {
      log.debug("XmPush service is unavailable (status " + status + ")");
      return null;
    }
    try (InputStream is =
        (status == 200) ? connection.getInputStream() : connection.getErrorStream()) {
      final byte[] content = readFixedLengthBytes(is, connection.getContentLength());
      return new AbstractClient.ResponseWrapper() {
        public int status() {
          return status;
        }

        public byte[] content() {
          return content;
        }

        public String header(String name) {
          return connection.getHeaderField(name);
        }
      };
    }
  }

  private HttpURLConnection getConnection(String url) throws Exception {
    HttpURLConnection connection;
    URL u = new URL(url);
    if (this.useProxy) {
      System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
      try {
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, new TrustManager[] {sslVerifier}, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(sslVerifier);
      } catch (Exception e) {
        log.debug("https config ssl failure: " + e);
        throw e;
      }
      Proxy proxy =
          new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.proxyHost, this.proxyPort));
      connection = (HttpsURLConnection) u.openConnection(proxy);
    } else if (Constants.USE_HTTPS) {
      connection = (HttpsURLConnection) u.openConnection();
      ((HttpsURLConnection) connection).setHostnameVerifier((s, sslSession) -> true);
    } else {
      connection = (HttpURLConnection) u.openConnection();
    }
    return connection;
  }

  private static class SSLHandler implements X509TrustManager, HostnameVerifier {
    private SSLHandler() {}

    public X509Certificate[] getAcceptedIssuers() {
      return null;
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) {}

    public void checkClientTrusted(X509Certificate[] chain, String authType) {}

    public boolean verify(String paramString, SSLSession paramSSLSession) {
      return true;
    }
  }
}
