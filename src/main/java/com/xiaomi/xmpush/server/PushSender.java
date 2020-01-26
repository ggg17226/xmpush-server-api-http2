package com.xiaomi.xmpush.server;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

@Slf4j
public abstract class PushSender<T extends PushSender> {
  protected static final int BACKOFF_INITIAL_DELAY = 1000;

  protected static final int MAX_BACKOFF_DELAY = 1024000;

  protected static final String JDK_VERSION = System.getProperty("java.version", "UNKNOWN");

  protected static final String OS = System.getProperty("os.name").toLowerCase();

  protected static String LOCAL_IP;

  protected static final String LOCAL_HOST_NAME = getLocalHostName();

  protected static boolean useProxy = false;

  protected static boolean needAuth = false;

  protected static String proxyHost;

  protected static int proxyPort;

  protected static String user;

  private static String password;

  protected final Random random = new Random();

  protected final String security;

  protected final String token;

  protected final Region region;

  protected final boolean isVip;

  protected JSONObject lastResult;

  protected String remoteHost = "";

  protected String remoteIp = "";

  protected Exception lastException = null;

  private long lastRequestCostTime = 0L;

  private ERROR lastRequestError = ERROR.SUCCESS;

  private String lastRequestUrl = null;

  private String lastRequestHost = null;

  private int connectTimeout = 3000;

  private int readTimeout = 3000;

  private int writeTimeout = 3000;

  public PushSender(String security) {
    this.security = security;
    this.token = null;
    this.region = Region.China;
    this.isVip = false;
  }

  public PushSender(String security, boolean isVip) {
    this.security = security;
    this.token = null;
    this.region = Region.China;
    this.isVip = isVip;
  }

  public PushSender(String security, String token) {
    this.security = security;
    this.token = token;
    this.region = Region.China;
    this.isVip = false;
  }

  public PushSender(String security, String token, boolean isVip) {
    this.security = security;
    this.token = token;
    this.region = Region.China;
    this.isVip = isVip;
  }

  public PushSender(String security, Region region) {
    this.security = security;
    this.token = null;
    this.region = region;
    this.isVip = false;
  }

  public PushSender(String security, String token, Region region) {
    this.security = security;
    this.token = token;
    this.region = region;
    this.isVip = false;
  }

  public T connectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
    return (T) this;
  }

  public T readTimeout(int readTimeout) {
    this.readTimeout = readTimeout;
    return (T) this;
  }

  public T writeTimeout(int writeTimeout) {
    this.writeTimeout = writeTimeout;
    return (T) this;
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

  void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      this.lastException = e;
      Thread.currentThread().interrupt();
    }
  }

  private String buildUrl(
      ServerSwitch.Server server, Constants.RequestPath requestPath, NameValuePairs queryParams)
      throws UnsupportedEncodingException {
    String url = ServerSwitch.buildFullRequestURL(server, requestPath);
    if (queryParams != null && !queryParams.isEmpty())
      url = url + "?" + queryParams.toQueryOrFormData();
    return url;
  }

  protected void before(SenderContext context) {}

  protected void after(SenderContext context) {}

  private String execute(
      SenderAction<SenderContext, AbstractClient.ResponseWrapper> action,
      Constants.RequestPath requestPath,
      NameValuePairs headers,
      NameValuePairs queryParams)
      throws Exception {
    long start = System.currentTimeMillis();
    boolean isSuccess = false;
    ServerSwitch.Server server =
        ServerSwitch.getInstance().selectServer(requestPath, this.region, this.isVip);
    try {
      headers = prepareRequestHeaders(headers);
      String url = buildUrl(server, requestPath, queryParams);
      SenderContext context =
          (new SenderContext.Builder())
              .server(server)
              .headers(headers)
              .requestPath(requestPath)
              .url(url)
              .build();
      try {
        URL uri = new URL(url);
        this.remoteHost = uri.getHost();
        InetAddress address = InetAddress.getByName(this.remoteHost);
        this.remoteIp = address.getHostAddress();
      } catch (Exception e) {
        this.lastException = e;
        log.warn("Get remote ip failed for " + this.remoteHost, e);
      }
      before(context);
      AbstractClient.ResponseWrapper responseWrapper = action.action(context);
      context.responseWrapper = responseWrapper;
      after(context);
      isSuccess = true;
      this.lastRequestError = ERROR.SUCCESS;
      Constants.autoSwitchHost =
          (responseWrapper.header("X-PUSH-DISABLE-AUTO-SELECT-DOMAIN") == null);
      Constants.accessTimeOut =
          Integer.valueOf(responseWrapper.header("X-PUSH-CLIENT-TIMEOUT-MS", "5000")).intValue();
      String hosts = responseWrapper.header("X-PUSH-HOST-LIST");
      if (hosts != null) ServerSwitch.getInstance().initialize(hosts);
      return new String(responseWrapper.content());
    } catch (Exception e) {
      if (e instanceof java.net.SocketTimeoutException) {
        this.lastRequestError = ERROR.SocketTimeoutException;
      } else if (e instanceof IOException) {
        this.lastRequestError = ERROR.IOException;
      }
      throw e;
    } finally {
      this.lastRequestCostTime = System.currentTimeMillis() - start;
      if (this.lastRequestCostTime > Constants.accessTimeOut) {
        this.lastRequestError = ERROR.CostTooMuchTime;
        server.decrPriority();
      } else if (isSuccess) {
        server.incrPriority();
      } else {
        server.decrPriority();
      }
      this.lastRequestUrl = requestPath.getPath();
      this.lastRequestHost = server.getHost();
    }
  }

  private AbstractClient getClient() {
    AbstractClient client;
    if (Constants.useOkHttp3) {
      try {
        Class.forName("okhttp3.OkHttpClient");
        client =
            new OkHttp3Client(
                useProxy,
                needAuth,
                proxyHost,
                proxyPort,
                user,
                password,
                this.connectTimeout,
                this.readTimeout,
                this.writeTimeout);
      } catch (Exception e) {
        client =
            new URLConnectionClient(
                useProxy,
                needAuth,
                proxyHost,
                proxyPort,
                user,
                password,
                this.connectTimeout,
                this.readTimeout,
                this.writeTimeout);
      }
    } else {
      client = new URLConnectionClient(useProxy, needAuth, proxyHost, proxyPort, user, password);
    }
    return client;
  }

  protected String post(
      Constants.RequestPath requestPath,
      String body,
      NameValuePairs headers,
      NameValuePairs queryParams,
      RetryHandler retryHandler)
      throws Exception {
    return execute(
        context ->
            getClient()
                .post(
                    context.url(),
                    body.getBytes(StandardCharsets.UTF_8),
                    context
                        .headers()
                        .nameAndValue(
                            "Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"),
                    retryHandler),
        requestPath,
        headers,
        queryParams);
  }

  protected String get(
      Constants.RequestPath requestPath,
      NameValuePairs headers,
      NameValuePairs queryParams,
      RetryHandler retryHandler)
      throws Exception {
    return execute(
        context ->
            getClient()
                .get(
                    context.url(),
                    context
                        .headers()
                        .nameAndValue(
                            "Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"),
                    retryHandler),
        requestPath,
        headers,
        queryParams);
  }

  protected String upload(
      Constants.RequestPath requestPath,
      File file,
      NameValuePairs headers,
      NameValuePairs queryParams,
      RetryHandler retryHandler)
      throws Exception {
    return execute(
        context -> getClient().upload(context.url(), file, context.headers(), retryHandler),
        requestPath,
        headers,
        queryParams);
  }

  protected IOException exception(int attemptNum, Exception e) {
    if (e != null) this.lastException = e;
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

  private NameValuePairs prepareRequestHeaders(NameValuePairs headers) {
    if (headers == null) headers = new NameValuePairs();
    headers
        .nameAndValue("X-PUSH-SDK-VERSION", "JAVA_SDK_V1.0.3")
        .nameAndValue("X-PUSH-JDK-VERSION", JDK_VERSION)
        .nameAndValue("X-PUSH-OS", OS)
        .nameAndValue("X-PUSH-REMOTEIP", this.remoteIp)
        .nameAndValue("Authorization", "key=" + this.security)
        .nameAndValue("X-PUSH-AUDIT-TOKEN", this.token)
        .nameAndValue("X-PUSH-CLIENT-HOST", LOCAL_HOST_NAME)
        .nameAndValue("X-PUSH-CLIENT-IP", LOCAL_IP);
    if (useProxy && needAuth) {
      String encoded = new String(Base64.encodeBase64((user + ":" + password).getBytes()));
      headers.nameAndValue("Proxy-Authorization", "Basic " + encoded);
    }
    if (Constants.INCLUDE_LAST_METRICS) {
      if (this.lastRequestCostTime > 0L) {
        headers.nameAndValue(
            "X-PUSH-LAST-REQUEST-DURATION", String.valueOf(this.lastRequestCostTime));
        this.lastRequestCostTime = 0L;
      }
      if (this.lastResult != null && this.lastResult.containsKey("trace_id"))
        headers.nameAndValue("X-PUSH-LAST-REQUEST-ID", this.lastResult.get("trace_id").toString());
      headers
          .nameAndValue("X-PUSH-LAST-REQUEST-URL", this.lastRequestUrl)
          .nameAndValue("X-PUSH-LAST-REQUEST-HOST", this.lastRequestHost)
          .nameAndValue("X-PUSH-LAST-ERROR", this.lastRequestError.name());
      this.lastRequestUrl = null;
      this.lastRequestHost = null;
      this.lastRequestError = ERROR.SUCCESS;
    }
    if (Constants.autoSwitchHost && ServerSwitch.getInstance().needRefreshHostList())
      headers.nameAndValue("X-PUSH-HOST-LIST", "true");
    return headers;
  }

  protected Result parseResult(String content) throws IOException {
    try {
      JSONParser parser = new JSONParser();
      JSONObject json = (JSONObject) parser.parse(content);
      this.lastResult = json;
      return (new Result.Builder()).fromJson(json);
    } catch (ParseException e) {
      log.warn(
          "Exception parsing response: remote server "
              + this.remoteHost
              + "("
              + this.remoteIp
              + ")",
          e);
      this.lastException = e;
      throw new IOException(
          "Invalid response from XmPush: "
              + content
              + "\n server "
              + this.remoteHost
              + " ip "
              + this.remoteIp);
    }
  }

  enum ERROR {
    SUCCESS,
    SocketTimeoutException,
    IOException,
    CostTooMuchTime
  }

  protected static class SenderContext {
    private ServerSwitch.Server server;

    private Constants.RequestPath requestPath;

    private NameValuePairs headers;

    private String url;

    private AbstractClient.ResponseWrapper responseWrapper;

    private Map<String, Object> extra = new HashMap<>();

    protected ServerSwitch.Server server() {
      return this.server;
    }

    protected NameValuePairs headers() {
      return this.headers;
    }

    protected Constants.RequestPath requestPath() {
      return this.requestPath;
    }

    protected String url() {
      return this.url;
    }

    protected <T> T extra(String name) {
      return (T) this.extra.get(name);
    }

    protected AbstractClient.ResponseWrapper responseWrapper() {
      return this.responseWrapper;
    }

    protected static class Builder {
      private PushSender.SenderContext context = new PushSender.SenderContext();

      Builder server(ServerSwitch.Server server) {
        this.context.server = server;
        return this;
      }

      Builder headers(NameValuePairs headers) {
        this.context.headers = headers;
        return this;
      }

      Builder requestPath(Constants.RequestPath requestPath) {
        this.context.requestPath = requestPath;
        return this;
      }

      Builder responseWrapper(AbstractClient.ResponseWrapper responseWrapper) {
        this.context.responseWrapper = responseWrapper;
        return this;
      }

      Builder url(String url) {
        this.context.url = url;
        return this;
      }

      Builder extra(String name, Object value) {
        this.context.extra.put(name, value);
        return this;
      }

      PushSender.SenderContext build() {
        return this.context;
      }
    }
  }

  protected static class Builder {
    private PushSender.SenderContext context;

    protected Builder() {
      this.context = new PushSender.SenderContext();
    }

    Builder server(ServerSwitch.Server server) {
      this.context.server = server;
      return this;
    }

    Builder headers(NameValuePairs headers) {
      this.context.headers = headers;
      return this;
    }

    Builder requestPath(Constants.RequestPath requestPath) {
      this.context.requestPath = requestPath;
      return this;
    }

    Builder responseWrapper(AbstractClient.ResponseWrapper responseWrapper) {
      this.context.responseWrapper = responseWrapper;
      return this;
    }

    Builder url(String url) {
      this.context.url = url;
      return this;
    }

    Builder extra(String name, Object value) {
      this.context.extra.put(name, value);
      return this;
    }

    PushSender.SenderContext build() {
      return this.context;
    }
  }

  public class DefaultPushRetryHandler implements RetryHandler {
    private int retries;

    private int backoff;

    private Random random = new Random();

    private Consumer<Integer> consumer;

    public DefaultPushRetryHandler(int retries, int backoff, Consumer<Integer> consumer) {
      this.retries = retries;
      this.consumer = consumer;
      this.backoff = backoff;
    }

    public boolean retryHandle(
        AbstractClient.ResponseWrapper responseWrapper, Exception e, int executionCount) {
      boolean retry = false;
      if (this.retries > 1
          && executionCount <= this.retries
          && (responseWrapper.status() >= 500 || responseWrapper.content() == null || e != null))
        retry = true;
      if (retry) {
        if (this.backoff > 0) {
          int sleepTime = this.backoff / 2 + this.random.nextInt(this.backoff);
          PushSender.this.sleep(sleepTime);
          if (2 * this.backoff < 1024000) this.backoff *= 2;
        }
        if (this.consumer != null) this.consumer.accept(Integer.valueOf(executionCount));
      }
      return retry;
    }
  }

  private interface SenderAction<T, R> {
    R action(T param1T) throws Exception;
  }
}
