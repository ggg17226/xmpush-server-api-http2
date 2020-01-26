package com.xiaomi.xmpush.server;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;

@Slf4j
public class MessageRevoker extends HttpBase {
  public MessageRevoker(String security) {
    super(nonNull(security));
  }

  public MessageRevoker(String security, Region region) {
    super(nonNull(security), region);
  }

  public String revokeMessage(
      String packageName,
      String title,
      String description,
      int notifyId,
      String[] topics,
      String msgId,
      int retries)
      throws IOException {
    StringBuilder body = new StringBuilder();
    if (!XMStringUtils.isEmpty(packageName))
      addParameter(body, "restricted_package_name", URLEncoder.encode(packageName, "UTF-8"));
    if (!XMStringUtils.isEmpty(title))
      addParameter(body, "title", URLEncoder.encode(title, "UTF-8"));
    if (!XMStringUtils.isEmpty(description))
      addParameter(body, "description", URLEncoder.encode(description, "UTF-8"));
    if (!XMStringUtils.isEmpty(Integer.toString(notifyId)))
      addParameter(body, "notify_id", URLEncoder.encode(Integer.toString(notifyId), "UTF-8"));
    if (topics != null && topics.length != 0)
      for (String topic : topics) addParameter(body, "topics", URLEncoder.encode(topic, "UTF-8"));
    if (!XMStringUtils.isEmpty(msgId))
      addParameter(body, "msg_id", URLEncoder.encode(msgId, "UTF-8"));
    String strBody = body.toString();
    int attempt = 0;
    String result = null;
    int backoff = 1000;
    while (true) {
      attempt++;
      log.debug("Attempt #" + attempt + " to revoke message.");
      result = revokeMessageNoRetry(strBody);
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

  private String revokeMessageNoRetry(String params) throws IOException {
    HttpURLConnection conn;
    int status;
    String responseBody;
    try {
      conn = doPost(Constants.XmPushRequestPath.V1_REVOKE_MESSAGE, params);
      status = conn.getResponseCode();
    } catch (IOException e) {
      log.warn(
          "IOException while fetch revoke message: remote server "
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
