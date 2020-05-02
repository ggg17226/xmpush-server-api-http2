package com.xiaomi.xmpush.server;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OkHttp3Client extends AbstractClient {

  private static final Object lock = new Object();

  private static OkHttpClient client;

  public OkHttp3Client(
      boolean useProxy,
      boolean needAuth,
      String proxyHost,
      int proxyPort,
      String user,
      String password) {
    super(useProxy, needAuth, proxyHost, proxyPort, user, password);
  }

  public OkHttp3Client(
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
    Request.Builder requestBuilder = (new Request.Builder()).url(url);
    requestBuilder.post(
        RequestBody.create(MediaType.get("application/x-www-form-urlencoded;charset=UTF-8"), body));
    return execute(requestBuilder, headers);
  }

  public AbstractClient.ResponseWrapper get(String url, NameValuePairs headers) throws Exception {
    Request.Builder requestBuilder = (new Request.Builder()).url(url).get();
    return execute(requestBuilder, headers);
  }

  public AbstractClient.ResponseWrapper upload(String url, File file, NameValuePairs headers)
      throws Exception {
    Request.Builder requestBuilder = (new Request.Builder()).url(url);
    requestBuilder.post(RequestBody.create(MediaType.get("image/*"), file));
    return execute(requestBuilder, headers);
  }

  private AbstractClient.ResponseWrapper execute(
      Request.Builder requestBuilder, NameValuePairs headers) throws IOException {
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
    try (Response response = getClient().newCall(requestBuilder.build()).execute()) {
      if (response.code() >= 500)
        log.debug("XmPush service is unavailable (status " + response.code() + ")");
      return new AbstractClient.ResponseWrapper() {
        final byte[] content = response.body().bytes();

        public int status() {
          return response.code();
        }

        public byte[] content() {
          return this.content;
        }

        public String header(String name) {
          return response.header(name);
        }
      };
    }
  }

  private OkHttpClient getClient() {
    if (client == null)
      synchronized (lock) {
        if (client == null) {
          OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
          try {
            Method setProtocolMethod =
                SSLParameters.class.getMethod(
                    "setApplicationProtocols", String[].class);
            Method method1 = SSLSocket.class.getMethod("getApplicationProtocol");
          } catch (NoSuchMethodException e) {
            try {
              Class.forName("org.conscrypt.Conscrypt");
              System.setProperty("okhttp.platform", "conscrypt");
            } catch (ClassNotFoundException classNotFoundException) {
            }
          }
          clientBuilder
              .connectTimeout(this.connectTimeout, TimeUnit.MILLISECONDS)
              .readTimeout(this.readTimeout, TimeUnit.MILLISECONDS)
              .writeTimeout(this.writeTimeout, TimeUnit.MILLISECONDS)
              .retryOnConnectionFailure(true)
              .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1));
          if (this.useProxy) {
            System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
            clientBuilder.proxy(
                new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.proxyHost, this.proxyPort)));
          }
          client = clientBuilder.build();
        }
      }
    return client;
  }
}
