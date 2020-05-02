package com.xiaomi.xmpush.server;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

public abstract class AbstractClient {
  protected boolean useProxy = false;

  protected boolean needAuth = false;

  protected String proxyHost;

  protected int proxyPort;

  protected String user;

  protected String password;

  protected int connectTimeout = 3000;

  protected int readTimeout = 3000;

  protected int writeTimeout = 3000;

  public AbstractClient(
      boolean useProxy,
      boolean needAuth,
      String proxyHost,
      int proxyPort,
      String user,
      String password) {
    this.useProxy = useProxy;
    this.needAuth = needAuth;
    this.proxyHost = proxyHost;
    this.proxyPort = proxyPort;
    this.user = user;
    this.password = password;
  }

  public AbstractClient(
      boolean useProxy,
      boolean needAuth,
      String proxyHost,
      int proxyPort,
      String user,
      String password,
      int connectTimeout,
      int readTimeout,
      int writeTimeout) {
    this(useProxy, needAuth, proxyHost, proxyPort, user, password);
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
    this.writeTimeout = writeTimeout;
  }

  public ResponseWrapper post(
      String url, byte[] body, NameValuePairs headers, RetryHandler retryHandler) throws Exception {
    return execute(() -> post(url, body, headers), retryHandler);
  }

  public ResponseWrapper get(String url, NameValuePairs headers, RetryHandler retryHandler)
      throws Exception {
    return execute(() -> get(url, headers), retryHandler);
  }

  public ResponseWrapper upload(
      String url, File file, NameValuePairs headers, RetryHandler retryHandler) throws Exception {
    return execute(() -> upload(url, file, headers), retryHandler);
  }

  private ResponseWrapper execute(Action action, RetryHandler retryHandler) throws Exception {
    int executionCount = 0;
    ResponseWrapper res = null;
    Exception currentException = null;
    do {
      try {
        res = action.action();
      } catch (Exception e) {
        currentException = e;
      }
    } while (retryHandler != null
        && retryHandler.retryHandle(res, currentException, ++executionCount));
    if (currentException != null) throw currentException;
    return res;
  }

  public abstract ResponseWrapper post(
      String paramString, byte[] paramArrayOfbyte, NameValuePairs paramNameValuePairs)
      throws Exception;

  public abstract ResponseWrapper get(String paramString, NameValuePairs paramNameValuePairs)
      throws Exception;

  public abstract ResponseWrapper upload(
      String paramString, File paramFile, NameValuePairs paramNameValuePairs) throws Exception;

  public interface ResponseWrapper {
    int status();

    byte[] content();

    String header(String param1String);

    default String header(String name, String defaultValue) {
      String value = header(name);
      return StringUtils.isBlank(value) ? defaultValue : value;
    }
  }

  public interface Action {
    AbstractClient.ResponseWrapper action() throws Exception;
  }
}
