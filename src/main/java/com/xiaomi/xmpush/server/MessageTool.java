package com.xiaomi.xmpush.server;

import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;

@Slf4j
public class MessageTool extends HttpBase {
  public MessageTool(String security) {
    super(nonNull(security));
  }

  public MessageTool(String security, Region region) {
    super(nonNull(security), region);
  }

  public Result deleteTopic(String jobId, int retries) throws IOException, ParseException {
    StringBuilder body = newBody("id", URLEncoder.encode(jobId, "UTF-8"));
    return sendMessage(Constants.XmPushRequestPath.V2_DELETE_BROADCAST_MESSAGE, body, retries);
  }

  public Result deleteTopic(String jobId) throws IOException, ParseException {
    return deleteTopic(jobId, 1);
  }

  protected Result sendMessage(Constants.RequestPath requestPath, StringBuilder body, int retries)
      throws IOException, ParseException {
    int attempt = 0;
    Result result = null;
    int backoff = 1000;
    boolean tryAgain = false;
    while (true) {
      attempt++;
      log.debug("Attempt #" + attempt + " to send " + body + " to url " + requestPath.getPath());
      String bodyStr = body.toString();
      if (bodyStr.charAt(0) == '&') bodyStr = body.toString().substring(1);
      result = sendMessage(requestPath, bodyStr);
      tryAgain = (result == null && attempt <= retries);
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

  protected Result sendMessage(Constants.RequestPath requestPath, String message)
      throws IOException, ParseException {
    HttpURLConnection conn;
    int status;
    String responseBody;
    try {
      log.debug("post to: " + requestPath.getPath());
      conn = doPost(requestPath, message);
      status = conn.getResponseCode();
    } catch (IOException e) {
      this.lastException = e;
      log.warn(
          "IOException send message: remote server " + this.remoteHost + "(" + this.remoteIp + ")",
          e);
      return null;
    }
    if (status / 100 == 5) {
      log.debug(
          "XmPush service is unavailable (status "
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
        log.trace("Plain post error response: " + responseBody);
      } catch (IOException e) {
        responseBody = "N/A";
        this.lastException = e;
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
    try {
      JSONParser parser = new JSONParser();
      JSONObject json = (JSONObject) parser.parse(responseBody);
      return (new Result.Builder()).fromJson(json);
    } catch (ParseException e) {
      log.warn(
          "Exception parsing response: remote server "
              + this.remoteHost
              + "("
              + this.remoteIp
              + ")",
          e);
      throw new IOException("Invalid response from XmPush: " + responseBody);
    }
  }
}
