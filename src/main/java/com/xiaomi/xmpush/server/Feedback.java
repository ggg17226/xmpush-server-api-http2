package com.xiaomi.xmpush.server;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

@Slf4j
public class Feedback extends PushSender<Feedback> {

  private static final String REG_ID_SPLITTER = ",";

  public Feedback(String security) {
    super(security);
  }

  public Feedback(String security, Region region) {
    super(security, region);
  }

  public String getInvalidRegIds(int retries) throws IOException {
    String result;
    try {
      log.debug("get from: " + Constants.XmPushRequestPath.V1_FEEDBACK_INVALID_REGID);
      result =
          get(
              Constants.XmPushRequestPath.V1_FEEDBACK_INVALID_REGID,
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to get invalid registration ids");
                  }));
      if (StringUtils.isBlank(result)) throw exception(retries, null);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public String getInvalidAliases(int retries) throws IOException {
    String result;
    try {
      log.debug("get from: " + Constants.XmPushRequestPath.V1_FEEDBACK_INVALID_ALIAS);
      result =
          get(
              Constants.XmPushRequestPath.V1_FEEDBACK_INVALID_ALIAS,
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to get invalid registration ids");
                  }));
      if (StringUtils.isBlank(result)) throw exception(retries, null);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public String getRegionByRegIds(List<String> regIds, int retries) throws IOException {
    String result;
    NameValuePairs body = (new NameValuePairs()).nameAndValue("regIds", String.join(",", regIds));
    try {
      log.debug("get from: " + Constants.XmPushRequestPath.V1_FEEDBACK_REGID_REGION);
      result =
          post(
              Constants.XmPushRequestPath.V1_FEEDBACK_REGID_REGION,
              body.toQueryOrFormData(),
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to get region by regid");
                  }));
      if (StringUtils.isBlank(result)) throw exception(retries, null);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public String getInvalidRegIdsNoRetry() throws IOException {
    return getInvalidRegIds(1);
  }

  public String getInvalidAliasesNoRetry() throws IOException {
    return getInvalidAliases(1);
  }

  public String getRegionByRegIdsNoRetry(List<String> regIds) throws IOException {
    return getRegionByRegIds(regIds, 1);
  }
}
