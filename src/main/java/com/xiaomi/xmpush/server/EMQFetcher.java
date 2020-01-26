package com.xiaomi.xmpush.server;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.HttpURLConnection;

@Slf4j
public class EMQFetcher extends HttpBase {
  public EMQFetcher(String security) {
    super(security);
  }

  public EMQFetcher(String security, Region region) {
    super(security, region);
  }

  public String fetchAckInfo(String packageName, int retries) throws IOException {
    int attempt = 0;
    String result = null;
    int backoff = 1000;
    while (true) {
      attempt++;
      log.debug("Attempt #" + attempt + " to fetch ack info ");
      result = fetchAckInfoNoRetry("package_name=" + packageName);
      boolean tryAgain = (result == null && attempt <= retries);
      if (tryAgain) {
        int sleepTime = backoff / 2 + this.random.nextInt(backoff);
        sleep(sleepTime);
        if (2 * backoff < 1024000) backoff *= 2;
      }
      if (!tryAgain) {
        if (result == null) throw exception(attempt);
        return result;
      }
    }
  }

  public String fetchClickInfo(String packageName, int retries) throws IOException {
    int attempt = 0;
    String result = null;
    int backoff = 1000;
    while (true) {
      attempt++;
      log.debug("Attempt #" + attempt + " to fetch click info ");
      result = fetchClickInfoNoRetry("package_name=" + packageName);
      boolean tryAgain = (result == null && attempt <= retries);
      if (tryAgain) {
        int sleepTime = backoff / 2 + this.random.nextInt(backoff);
        sleep(sleepTime);
        if (2 * backoff < 1024000) backoff *= 2;
      }
      if (!tryAgain) {
        if (result == null) throw exception(attempt);
        return result;
      }
    }
  }

  public String fetchInvalidRegId(String packageName, int retries) throws IOException {
    int attempt = 0;
    String result = null;
    int backoff = 1000;
    while (true) {
      attempt++;
      log.debug("Attempt #" + attempt + " to fetch invalid regid ");
      result = fetchInvalidRegIdNoRetry("package_name=" + packageName);
      boolean tryAgain = (result == null && attempt <= retries);
      if (tryAgain) {
        int sleepTime = backoff / 2 + this.random.nextInt(backoff);
        sleep(sleepTime);
        if (2 * backoff < 1024000) backoff *= 2;
      }
      if (!tryAgain) {
        if (result == null) throw exception(attempt);
        return result;
      }
    }
  }

  private String fetchAckInfoNoRetry(String packageName) throws IOException {
    HttpURLConnection conn;
    int status;
    String responseBody;
    try {
      conn = doGet(Constants.XmPushRequestPath.V1_EMQ_ACK_INFO, packageName);
      status = conn.getResponseCode();
    } catch (IOException e) {
      log.warn(
          "IOException while fetch ack info: remote server "
              + this.remoteHost
              + "("
              + this.remoteIp
              + ")",
          e);
      this.lastException = e;
      return null;
    }
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

  private String fetchClickInfoNoRetry(String packageName) throws IOException {
    HttpURLConnection conn;
    int status;
    String responseBody;
    try {
      conn = doGet(Constants.XmPushRequestPath.V1_EMQ_CLICK_INFO, packageName);
      status = conn.getResponseCode();
    } catch (IOException e) {
      log.warn(
          "IOException while fetch click info: remote server "
              + this.remoteHost
              + "("
              + this.remoteIp
              + ")",
          e);
      this.lastException = e;
      return null;
    }
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

  private String fetchInvalidRegIdNoRetry(String packageName) throws IOException {
    HttpURLConnection conn;
    int status;
    String responseBody;
    try {
      conn = doGet(Constants.XmPushRequestPath.V1_EMQ_INVALID_REGID, packageName);
      status = conn.getResponseCode();
    } catch (IOException e) {
      log.warn(
          "IOException while fetch invalid regId: remote server "
              + this.remoteHost
              + "("
              + this.remoteIp
              + ")",
          e);
      this.lastException = e;
      return null;
    }
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
}
