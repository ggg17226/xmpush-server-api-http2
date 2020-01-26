package com.xiaomi.xmpush.server;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Slf4j
public class DevTools extends PushSender<DevTools> {

  private static final String REG_ID_SPLITTER = ",";

  public DevTools(String security) {
    super(security);
  }

  public DevTools(String security, Region region) {
    super(security, region);
  }

  public String getAliasesOf(String packageName, String regId, int retries) throws IOException {
    String result;
    NameValuePairs queryParams =
        (new NameValuePairs())
            .nameAndValue("restricted_package_name", packageName)
            .nameAndValue("registration_id", regId);
    try {
      log.debug("get from: " + Constants.XmPushRequestPath.V1_GET_ALL_ALIAS);
      result =
          get(
              Constants.XmPushRequestPath.V1_GET_ALL_ALIAS,
              null,
              queryParams,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to get all aliases of the device.");
                  }));
      if (XMStringUtils.isBlank(result)) throw exception(retries, null);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public String getTopicsOf(String packageName, String regId, int retries) throws IOException {
    String result;
    NameValuePairs queryParams =
        (new NameValuePairs())
            .nameAndValue("restricted_package_name", packageName)
            .nameAndValue("registration_id", regId);
    try {
      log.debug("get from: " + Constants.XmPushRequestPath.V1_GET_ALL_TOPIC);
      result =
          get(
              Constants.XmPushRequestPath.V1_GET_ALL_TOPIC,
              null,
              queryParams,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to get all topics of the device.");
                  }));
      if (XMStringUtils.isBlank(result)) throw exception(retries, null);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public String getAccountsOf(String packageName, String regId, int retries) throws IOException {
    String result;
    NameValuePairs queryParams =
        (new NameValuePairs())
            .nameAndValue("restricted_package_name", packageName)
            .nameAndValue("registration_id", regId);
    try {
      log.debug("get from: " + Constants.XmPushRequestPath.V1_GET_ALL_ACCOUNT);
      result =
          get(
              Constants.XmPushRequestPath.V1_GET_ALL_ACCOUNT,
              null,
              queryParams,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug(
                        "Attempt #" + executionCount + " to get all user account of the device.");
                  }));
      if (XMStringUtils.isBlank(result)) throw exception(retries, null);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public String getPresence(String packageName, String regId, int retries) throws IOException {
    String result;
    NameValuePairs queryParams =
        (new NameValuePairs())
            .nameAndValue("restricted_package_name", packageName)
            .nameAndValue("registration_id", regId);
    try {
      Constants.RequestPath requestPath =
          regId.contains(",")
              ? Constants.XmPushRequestPath.V2_REGID_PRESENCE
              : Constants.XmPushRequestPath.V1_REGID_PRESENCE;
      log.debug("get from: " + requestPath);
      result =
          get(
              requestPath,
              null,
              queryParams,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to get presence of the device.");
                  }));
      if (XMStringUtils.isBlank(result)) throw exception(retries, null);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public String getPresence(String packageName, List<String> regIds, int retries)
      throws IOException {
    return getPresence(packageName, String.join(",", regIds), retries);
  }
}
