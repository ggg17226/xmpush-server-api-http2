package com.xiaomi.xmpush.server;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

@Slf4j
public class Tracer extends PushSender<Tracer> {

  public Tracer(String security) {
    super(security);
  }

  public Tracer(String security, Region region) {
    super(security, region);
  }

  public String getMessageGroupStatus(String jobKey, int retries) throws IOException {
    String result;
    NameValuePairs queryParams = (new NameValuePairs()).nameAndValue("job_key", jobKey);
    try {
      result =
          get(
              Constants.XmPushRequestPath.V1_MESSAGE_STATUS,
              null,
              queryParams,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug(
                        "Attempt #" + executionCount + " to get status of message group " + jobKey);
                  }));
      if (StringUtils.isBlank(result)) throw exception(retries, null);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public String getMessageStatus(String msgId, int retries) throws IOException {
    String result;
    NameValuePairs queryParams = (new NameValuePairs()).nameAndValue("msg_id", msgId);
    try {
      result =
          get(
              Constants.XmPushRequestPath.V1_MESSAGE_STATUS,
              null,
              queryParams,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to get status of message " + msgId);
                  }));
      if (StringUtils.isBlank(result)) throw exception(retries, null);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public String getMessageStatus(long beginTime, long endTime, int retries) throws IOException {
    String result;
    NameValuePairs queryParams =
        (new NameValuePairs())
            .nameAndValue("begin_time", Long.valueOf(beginTime))
            .nameAndValue("end_time", Long.valueOf(endTime));
    try {
      result =
          get(
              Constants.XmPushRequestPath.V1_MESSAGE_STATUS,
              null,
              queryParams,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug(
                        "Attempt #"
                            + executionCount
                            + " to get messages status between "
                            + beginTime
                            + " and "
                            + endTime);
                  }));
      if (StringUtils.isBlank(result)) throw exception(retries, null);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public String getMessageGroupStatusNoRetry(String jobKey) throws IOException {
    return getMessageGroupStatus(jobKey, 1);
  }

  public String getMessageStatusNoRetry(String msgId) throws IOException {
    return getMessageStatus(msgId, 1);
  }

  public String getMessageStatusNoRetry(long beginTime, long endTime) throws IOException {
    return getMessageStatus(beginTime, endTime, 1);
  }
}
