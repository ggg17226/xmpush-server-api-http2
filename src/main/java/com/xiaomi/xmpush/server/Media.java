package com.xiaomi.xmpush.server;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;

public class Media extends PushSender<Media> {
  public Media(String security) {
    super(security);
  }

  public Media(String security, Region region) {
    super(security, region);
  }

  public Result upload(File file, boolean isIcon, boolean isGlobal) throws IOException {
    Result result;
    NameValuePairs queryParams =
        (new NameValuePairs())
            .nameAndValue("is_global", String.valueOf(isGlobal))
            .nameAndValue("is_icon", String.valueOf(isIcon));
    try {
      String response =
          upload(Constants.XmPushRequestPath.MEDIA_UPLOAD_IMAGE, file, null, queryParams, null);
      if (StringUtils.isBlank(response)) throw exception(1, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(1, e);
    }
    return result;
  }

  public Result uploadSmallIcon(File file) throws IOException {
    Result result;
    try {
      String response =
          upload(Constants.XmPushRequestPath.MEDIA_UPLOAD_SMALLICON, file, null, null, null);
      if (StringUtils.isBlank(response)) throw exception(1, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(1, e);
    }
    return result;
  }

  public Result querySmallIconInfoByAppId(long appId) throws IOException {
    Result result;
    NameValuePairs queryParams =
        (new NameValuePairs()).nameAndValue("app_id", String.valueOf(appId));
    try {
      String response =
          get(Constants.XmPushRequestPath.MEDIA_UPLOAD_SMALLICON_QUERY, null, queryParams, null);
      if (StringUtils.isBlank(response)) throw exception(1, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(1, e);
    }
    return result;
  }
}
