package com.xiaomi.xmpush.server;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
public class HttpBase {
  protected static final String UTF8 = "UTF-8";

  protected static final int BACKOFF_INITIAL_DELAY = 1000;

  protected static final int MAX_BACKOFF_DELAY = 1024000;

  private static final String JDK_VERSION = System.getProperty("java.version", "UNKNOWN");

  private static final String OS = System.getProperty("os.name").toLowerCase();

  private static final SSLHandler sslVerifier = new SSLHandler();

  private static String LOCAL_IP;

  private static final String LOCAL_HOST_NAME = getLocalHostName();

  private static boolean useProxy = false;

  private static boolean needAuth = false;

  private static String proxyHost;

  private static int proxyPort;

  private static String user;

  private static String password;

  protected final Random random = new Random();

  protected final String security;

  protected final String token;

  protected final Region region;

  protected final boolean isVip;

  protected long lastRequestCostTime = 0L;

  protected ERROR lastRequestError = ERROR.SUCCESS;

  protected String lastRequestUrl = null;

  protected String lastRequestHost = null;

  protected String remoteHost = "";

  protected String remoteIp = "";

  protected Exception lastException = null;

  public HttpBase(String security) {
    this.security = security;
    this.token = null;
    this.region = Region.China;
    this.isVip = false;
  }

  public HttpBase(String security, boolean isVip) {
    this.security = security;
    this.token = null;
    this.region = Region.China;
    this.isVip = isVip;
  }

  public HttpBase(String security, String token) {
    this.security = security;
    this.token = token;
    this.region = Region.China;
    this.isVip = false;
  }

  public HttpBase(String security, String token, boolean isVip) {
    this.security = security;
    this.token = token;
    this.region = Region.China;
    this.isVip = isVip;
  }

  public HttpBase(String security, Region region) {
    this.security = security;
    this.token = null;
    this.region = region;
    this.isVip = false;
  }

  public HttpBase(String security, String token, Region region) {
    this.security = security;
    this.token = token;
    this.region = region;
    this.isVip = false;
  }

  private static String getLocalHostName() {
    String host = null;
    try {
      host = InetAddress.getLocalHost().getHostName();
      LOCAL_IP = InetAddress.getLocalHost().getHostAddress();
    } catch (Exception exception) {
    }
    return host;
  }

  protected static StringBuilder newBody(String name, String value) {
    return (new StringBuilder(nonNull(name))).append('=').append(nonNull(value));
  }

  private static void close(Closeable closeable) {
    if (closeable != null)
      try {
        closeable.close();
      } catch (IOException e) {
        log.trace("IOException closing stream", e);
      }
  }

  protected static StringBuilder newBodyWithArrayParameters(String name, List<String> parameters)
      throws UnsupportedEncodingException {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < parameters.size(); i++) {
      if (i == 0) {
        sb.append(nonNull(name))
            .append("=")
            .append(URLEncoder.encode(nonNull(parameters.get(i)), "UTF-8"));
      } else {
        nonNull(sb)
            .append('&')
            .append(nonNull(name))
            .append('=')
            .append(URLEncoder.encode(nonNull(parameters.get(i)), "UTF-8"));
      }
    }
    if (parameters.size() == 0) sb.append(name).append("=");
    return sb;
  }

  protected static void addParameter(StringBuilder body, String name, String value) {
    nonNull(body).append('&').append(nonNull(name)).append('=').append(nonNull(value));
  }

  protected static String getString(InputStream stream) throws IOException {
    if (stream == null) return "";
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
    StringBuilder content = new StringBuilder();
    while (true) {
      String newLine = reader.readLine();
      if (newLine != null) content.append(newLine).append('\n');
      if (newLine == null) {
        if (content.length() > 0) content.setLength(content.length() - 1);
        return content.toString();
      }
    }
  }

  protected static String getAndClose(InputStream stream) throws IOException {
    try {
      return getString(stream);
    } finally {
      if (stream != null) close(stream);
    }
  }

  static <T> T nonNull(T argument) {
    if (argument == null) throw new IllegalArgumentException("argument cannot be null");
    return argument;
  }

  public static void setProxy(String host, int port) {
    setProxy(host, port, null, null);
  }

  public static void setProxy(String host, int port, String authUser, String authPassword) {
    if (XMStringUtils.isBlank(host) || port <= 0)
      throw new IllegalArgumentException("proxy host or port invalid.");
    useProxy = true;
    needAuth = (!XMStringUtils.isBlank(authUser) && !XMStringUtils.isBlank(authPassword));
    proxyHost = host;
    proxyPort = port;
    user = authUser;
    password = authPassword;
  }

  public static void unsetProxy() {
    useProxy = false;
    needAuth = false;
  }

  protected IOException exception(int attemptNum) {
    String msg =
        "Failed to send http request after "
            + attemptNum
            + " attempts: remote server "
            + this.remoteHost
            + "("
            + this.remoteIp
            + ")";
    if (this.lastException != null) {
      msg =
          msg
              + "\nException "
              + this.lastException.getClass().getCanonicalName()
              + " : "
              + this.lastException.getLocalizedMessage()
              + "\n"
              + this.lastException.getCause();
      StackTraceElement[] traces = this.lastException.getStackTrace();
      if (traces != null)
        for (StackTraceElement trace : traces)
          msg =
              msg
                  + "\n  "
                  + trace.getClassName()
                  + trace.getMethodName()
                  + " ("
                  + trace.getFileName()
                  + ":"
                  + trace.getLineNumber()
                  + ")";
    }
    this.lastException = null;
    return new IOException(msg);
  }

  private HttpURLConnection httpRequest(HttpAction action, Constants.RequestPath requestPath)
      throws IOException {
    ServerSwitch.Server server =
        ServerSwitch.getInstance().selectServer(requestPath, this.region, this.isVip);
    long start = System.currentTimeMillis();
    boolean succ = false;
    try {
      HttpURLConnection result = action.action(server);
      succ = true;
      this.lastRequestError = ERROR.SUCCESS;
      Constants.autoSwitchHost =
          (result.getHeaderField("X-PUSH-DISABLE-AUTO-SELECT-DOMAIN") == null);
      Constants.accessTimeOut = result.getHeaderFieldInt("X-PUSH-CLIENT-TIMEOUT-MS", 5000);
      String hosts = result.getHeaderField("X-PUSH-HOST-LIST");
      if (hosts != null) ServerSwitch.getInstance().initialize(hosts);
      return result;
    } catch (SocketTimeoutException e) {
      this.lastRequestError = ERROR.SocketTimeoutException;
      throw e;
    } catch (IOException e) {
      this.lastRequestError = ERROR.IOException;
      throw e;
    } finally {
      this.lastRequestCostTime = System.currentTimeMillis() - start;
      if (this.lastRequestCostTime > Constants.accessTimeOut) {
        this.lastRequestError = ERROR.CostTooMuchTime;
        server.decrPriority();
      } else if (succ) {
        server.incrPriority();
      } else {
        server.decrPriority();
      }
      this.lastRequestUrl = requestPath.getPath();
      this.lastRequestHost = server.getHost();
    }
  }

  protected String getResponseBody(HttpURLConnection conn) throws IOException {
    String responseBody;
    int status = conn.getResponseCode();
    if (status / 100 == 5) {
      log.debug(
          "Service is unavailable (status "
              + status
              + "): remote server "
              + this.remoteHost
              + "("
              + this.remoteIp
              + ")");
      return null;
    }
    if (status != 200) {
      try {
        responseBody = getAndClose(conn.getErrorStream());
        log.trace("Plain get error response: " + responseBody);
      } catch (IOException e) {
        responseBody = "N/A";
        log.warn(
            "Exception reading response: remote server "
                + this.remoteHost
                + "("
                + this.remoteIp
                + ")",
            e);
      }
      throw new InvalidRequestException(status, responseBody);
    }
    try {
      responseBody = getAndClose(conn.getInputStream());
    } catch (IOException e) {
      log.warn(
          "Exception reading response: remote server "
              + this.remoteHost
              + "("
              + this.remoteIp
              + ")",
          e);
      return null;
    }
    return responseBody;
  }

  protected HttpURLConnection doPost(final Constants.RequestPath requestPath, final String body)
      throws IOException {
    return httpRequest(
        new HttpAction() {
          public HttpURLConnection action(ServerSwitch.Server server) throws IOException {
            return HttpBase.this.doPost(
                server, requestPath, "application/x-www-form-urlencoded;charset=UTF-8", body);
          }
        },
        requestPath);
  }

  protected HttpURLConnection doGet(final Constants.RequestPath requestPath, final String parameter)
      throws IOException {
    return httpRequest(
        new HttpAction() {
          public HttpURLConnection action(ServerSwitch.Server server) throws IOException {
            return HttpBase.this.doGet(
                server, requestPath, "application/x-www-form-urlencoded;charset=UTF-8", parameter);
          }
        },
        requestPath);
  }

  protected HttpURLConnection doUpload(
      final Constants.RequestPath requestPath, final File file, final String parameter)
      throws IOException {
    return httpRequest(
        new HttpAction() {
          public HttpURLConnection action(ServerSwitch.Server server) throws IOException {
            return HttpBase.this.doUpload(server, requestPath, file, parameter);
          }
        },
        requestPath);
  }

  protected HttpURLConnection doPost(
      ServerSwitch.Server server,
      Constants.RequestPath requestPath,
      String contentType,
      String body)
      throws IOException {
    if (requestPath == null || body == null)
      throw new IllegalArgumentException("arguments cannot be null");
    log.debug("Sending post to " + server.getHost() + " " + requestPath.getPath());
    log.trace("post body: " + body);
    HttpURLConnection conn = getConnection(server, requestPath);
    prepareConnection(conn);
    byte[] bytes = body.getBytes();
    conn.setConnectTimeout(20000);
    conn.setReadTimeout(20000);
    conn.setDoOutput(true);
    conn.setUseCaches(false);
    conn.setFixedLengthStreamingMode(bytes.length);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", contentType);
    conn.setRequestProperty("Authorization", "key=" + this.security);
    if (this.token != null) conn.setRequestProperty("X-PUSH-AUDIT-TOKEN", this.token);
    OutputStream out = conn.getOutputStream();
    try {
      out.write(bytes);
    } finally {
      close(out);
    }
    return conn;
  }

  public HttpURLConnection doUpload(
      ServerSwitch.Server server, Constants.RequestPath requestPath, File file, String parameter)
      throws IOException {
    if (requestPath == null || file == null || !file.exists())
      throw new IllegalArgumentException("arguments cannot be null");
    log.debug("Upload to " + server.getHost() + " " + requestPath.getPath());
    log.trace("post file name : " + file);
    String BOUNDARY = "------WKFB" + UUID.randomUUID().toString();
    String TWO_HYPHENS = "--";
    String LINE_END = "\r\n";
    HttpURLConnection conn = getConnection(server, requestPath, parameter);
    prepareConnection(conn);
    conn.setConnectTimeout(20000);
    conn.setReadTimeout(20000);
    conn.setDoOutput(true);
    conn.setUseCaches(false);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Charsert", "UTF-8");
    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
    conn.setRequestProperty("Authorization", "key=" + this.security);
    if (this.token != null) conn.setRequestProperty("X-PUSH-AUDIT-TOKEN", this.token);
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
    OutputStream out = conn.getOutputStream();
    try {
      out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
      int bytes = 0;
      byte[] buffer = new byte[1024];
      while ((bytes = in.read(buffer)) != -1) out.write(buffer, 0, bytes);
      byte[] endData = (LINE_END + TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END).getBytes();
      out.write(endData);
      out.flush();
    } finally {
      close(out);
    }
    return conn;
  }

  protected HttpURLConnection doGet(
      ServerSwitch.Server server,
      Constants.RequestPath requestPath,
      String contentType,
      String parameter)
      throws IOException {
    if (requestPath == null || parameter == null)
      throw new IllegalArgumentException("arguments cannot be null");
    log.debug("Sending get to " + server.getHost() + " " + requestPath.getPath());
    log.trace("get parameter: " + parameter);
    HttpURLConnection conn = getConnection(server, requestPath, parameter);
    prepareConnection(conn);
    conn.setConnectTimeout(20000);
    conn.setReadTimeout(20000);
    conn.setDoOutput(true);
    conn.setUseCaches(false);
    conn.setRequestMethod("GET");
    conn.setRequestProperty("Content-Type", contentType);
    conn.setRequestProperty("Authorization", "key=" + this.security);
    conn.getInputStream();
    return conn;
  }

  protected void prepareConnection(HttpURLConnection conn) {
    conn.setRequestProperty("X-PUSH-SDK-VERSION", "JAVA_SDK_V1.0.3");
    conn.setRequestProperty("X-PUSH-JDK-VERSION", JDK_VERSION);
    conn.setRequestProperty("X-PUSH-OS", OS);
    conn.setRequestProperty("X-PUSH-REMOTEIP", this.remoteIp);
    if (LOCAL_HOST_NAME != null) conn.setRequestProperty("X-PUSH-CLIENT-HOST", LOCAL_HOST_NAME);
    if (LOCAL_IP != null) conn.setRequestProperty("X-PUSH-CLIENT-IP", LOCAL_IP);
    if (Constants.INCLUDE_LAST_METRICS) {
      if (this.lastRequestCostTime > 0L) {
        conn.setRequestProperty("X-PUSH-LAST-REQUEST-DURATION", this.lastRequestCostTime + "");
        this.lastRequestCostTime = 0L;
      }
      if (this.lastRequestUrl != null) {
        conn.setRequestProperty("X-PUSH-LAST-REQUEST-URL", this.lastRequestUrl);
        this.lastRequestUrl = null;
      }
      if (this.lastRequestHost != null) {
        conn.setRequestProperty("X-PUSH-LAST-REQUEST-HOST", this.lastRequestHost);
        this.lastRequestHost = null;
      }
      conn.setRequestProperty("X-PUSH-LAST-ERROR", this.lastRequestError.name());
      this.lastRequestError = ERROR.SUCCESS;
    }
    if (Constants.autoSwitchHost && ServerSwitch.getInstance().needRefreshHostList())
      conn.setRequestProperty("X-PUSH-HOST-LIST", "true");
  }

  protected HttpURLConnection getConnection(
      ServerSwitch.Server server, Constants.RequestPath requestPath) throws IOException {
    return getConnection(server, requestPath, null);
  }

  protected HttpURLConnection getConnection(Constants.RequestPath requestPath) throws IOException {
    return getConnection(null, requestPath, null);
  }

  protected HttpURLConnection getConnection(
      ServerSwitch.Server server, Constants.RequestPath requestPath, String parameter)
      throws IOException {
    if (server == null)
      server = ServerSwitch.getInstance().selectServer(requestPath, this.region, this.isVip);
    String urlSpec = ServerSwitch.buildFullRequestURL(server, requestPath);
    if (parameter != null) urlSpec = urlSpec + "?" + parameter;
    URL url = new URL(urlSpec);
    if (useProxy) return setProxy(url);
    this.remoteHost = "";
    this.remoteIp = "";
    try {
      this.remoteHost = url.getHost();
      InetAddress address = InetAddress.getByName(this.remoteHost);
      this.remoteIp = address.getHostAddress();
    } catch (Exception e) {
      this.lastException = e;
      log.warn("Get remote ip failed for " + this.remoteHost, e);
    }
    if (Constants.USE_HTTPS) {
      HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
      conn.setHostnameVerifier(
          new HostnameVerifier() {
            public boolean verify(String s, SSLSession sslSession) {
              return true;
            }
          });
      return conn;
    }
    return (HttpURLConnection) url.openConnection();
  }

  private HttpsURLConnection setProxy(URL url) throws IOException {
    System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
    try {
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, new TrustManager[] {sslVerifier}, null);
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      HttpsURLConnection.setDefaultHostnameVerifier(sslVerifier);
    } catch (Exception e) {
      log.debug("https config ssl failure: " + e);
      this.lastException = e;
    }
    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
    HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection(proxy);
    if (needAuth) {
      String encoded = new String(Base64.encodeBase64((user + ":" + password).getBytes()));
      httpsURLConnection.setRequestProperty("Proxy-Authorization", "Basic " + encoded);
    }
    return httpsURLConnection;
  }

  void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      this.lastException = e;
      Thread.currentThread().interrupt();
    }
  }

  enum ERROR {
    SUCCESS,
    SocketTimeoutException,
    IOException,
    CostTooMuchTime
  }

  interface HttpAction {
    HttpURLConnection action(ServerSwitch.Server param1Server) throws IOException;
  }

  private static class SSLHandler implements X509TrustManager, HostnameVerifier {
    private SSLHandler() {}

    public X509Certificate[] getAcceptedIssuers() {
      return null;
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {}

    public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {}

    public boolean verify(String paramString, SSLSession paramSSLSession) {
      return true;
    }
  }
}
