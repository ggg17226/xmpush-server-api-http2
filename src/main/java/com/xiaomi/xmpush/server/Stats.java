package com.xiaomi.xmpush.server;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

@Slf4j
public class Stats extends PushSender<Stats> {

  public Stats(String security) {
    super(security);
  }

  public Stats(String security, Region region) {
    super(security, region);
  }

  public String getStats(String startDate, String endDate, String packageName, int retries)
      throws IOException {
    String result;
    NameValuePairs queryParams =
        (new NameValuePairs())
            .nameAndValue("start_date", startDate)
            .nameAndValue("end_date", endDate)
            .nameAndValue("restricted_package_name", packageName);
    try {
      log.debug("get from: " + Constants.XmPushRequestPath.V1_GET_MESSAGE_COUNTERS);
      result =
          get(
              Constants.XmPushRequestPath.V1_GET_MESSAGE_COUNTERS,
              null,
              queryParams,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug(
                        "Attempt #"
                            + executionCount
                            + " to get realtime stats data between "
                            + startDate
                            + " and "
                            + endDate);
                  }));
      if (StringUtils.isBlank(result)) throw exception(retries, null);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  protected String getStatsNoRetry(String startDate, String endDate, String packageName)
      throws IOException {
    return getStats(startDate, endDate, packageName, 1);
  }
}
