package com.xiaomi.xmpush.server;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;

import java.io.IOException;

@Slf4j
public class ChannelHelper extends PushSender<ChannelHelper> {

  protected JSONObject lastResult;

  public ChannelHelper(String security) {
    super(security);
  }

  public ChannelHelper(String security, Region region) {
    super(security, region);
  }

  public Result addNewChannel(ChannelInfo channelInfo, int retries) throws IOException {
    Result result;
    NameValuePairs queryParams =
        (new NameValuePairs())
            .nameAndValue("channel_id", channelInfo.getChannelId())
            .nameAndValue("channel_name", channelInfo.getChannelName())
            .nameAndValue("channel_description", channelInfo.getChannelDesc())
            .nameAndValue("sound_url", channelInfo.getSoundUrl())
            .nameAndValue("notify_type", channelInfo.getNotifyType());
    try {
      log.debug("get from: " + Constants.XmPushRequestPath.V1_ADD_NEW_CHANNEL);
      String response =
          get(
              Constants.XmPushRequestPath.V1_ADD_NEW_CHANNEL,
              null,
              queryParams,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug(
                        "Attempt #" + executionCount + " to add channel " + channelInfo.toString());
                  }));
      if (StringUtils.isBlank(response)) throw exception(retries, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public Result discardChannel(String channelId, int retries) throws IOException {
    Result result;
    NameValuePairs queryParams = (new NameValuePairs()).nameAndValue("channel_id", channelId);
    try {
      log.debug("get from: " + Constants.XmPushRequestPath.V1_DISCARD_CHANNEL);
      String response =
          get(
              Constants.XmPushRequestPath.V1_DISCARD_CHANNEL,
              null,
              queryParams,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to discard channel " + channelId);
                  }));
      if (StringUtils.isBlank(response)) throw exception(retries, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public Result getChannelList(int retries) throws IOException {
    Result result;
    try {
      log.debug("get from: " + Constants.XmPushRequestPath.V1_GET_CHANNEL_LIST);
      String response =
          get(
              Constants.XmPushRequestPath.V1_GET_CHANNEL_LIST,
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to get channel list");
                  }));
      if (StringUtils.isBlank(response)) throw exception(retries, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public Result addNewChannelNoRetry(ChannelInfo channelInfo) throws IOException {
    return addNewChannel(channelInfo, 1);
  }

  public Result discardChannelNoRetry(String channelId) throws IOException {
    return discardChannel(channelId, 1);
  }

  public Result getChannelListNoRetry() throws IOException {
    return getChannelList(1);
  }
}
