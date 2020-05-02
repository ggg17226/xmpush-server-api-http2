package com.xiaomi.xmpush.server;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

@Slf4j
public class Validation extends PushSender<Validation> {

  public Validation(String security) {
    super(security);
  }

  public Validation(String security, Region region) {
    super(security, region);
  }

  public String validateRegistrationIds(List<String> regIds, int retries) throws IOException {
    String result;
    NameValuePairs body = (new NameValuePairs()).nameAndValue("registration_ids", regIds.toArray());
    try {
      result =
          post(
              Constants.XmPushRequestPath.V1_VALIDATE_REGID,
              body.toQueryOrFormData(),
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to validate regids.");
                  }));
      if (StringUtils.isBlank(result)) throw exception(retries, null);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public String validateRegistrationIdsNoRetry(List<String> regIds) throws IOException {
    return validateRegistrationIds(regIds, 1);
  }
}
