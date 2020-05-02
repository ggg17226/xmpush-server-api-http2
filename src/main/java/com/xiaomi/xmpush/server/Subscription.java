package com.xiaomi.xmpush.server;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class Subscription extends PushSender<Subscription> {
  public Subscription(String security) {
    super(security);
  }

  public Subscription(String security, Region region) {
    super(security, region);
  }

  protected static String joinString(List<String> stringList, char sep) {
    StringBuffer sb = new StringBuffer();
    for (String s : stringList) sb.append(sep).append(s);
    return sb.substring(1);
  }

  public Result subscribeTopic(List<String> regIds, String topic, String category, int retries)
      throws IOException, ParseException {
    return topicSubscribeBase(regIds, topic, category, retries, true);
  }

  public Result subscribeTopic(
      List<String> regIds, String topic, String packageName, String category, int retries)
      throws IOException, ParseException {
    return topicSubscribeBase(regIds, topic, packageName, category, retries, true);
  }

  public Result subscribeTopic(List<String> regIds, String topic, String category)
      throws IOException, ParseException {
    return subscribeTopic(regIds, topic, category, 1);
  }

  public Result subscribeTopic(String regId, String topic, String category, int retries)
      throws IOException, ParseException {
    return subscribeTopic(Arrays.asList(regId), topic, category, retries);
  }

  public Result subscribeTopic(
      String regId, String topic, String packageName, String category, int retries)
      throws IOException, ParseException {
    List<String> regIds = new ArrayList<>();
    regIds.add(regId);
    return subscribeTopic(regIds, topic, packageName, category, retries);
  }

  public Result subscribeTopic(String regId, String topic, String category)
      throws IOException, ParseException {
    return subscribeTopic(regId, topic, category, 1);
  }

  public Result subscribeTopic(String regId, String topic, String packageName, String category)
      throws IOException, ParseException {
    return subscribeTopic(regId, topic, packageName, category, 1);
  }

  public Result unsubscribeTopic(List<String> regIds, String topic, String category, int retries)
      throws IOException, ParseException {
    return topicSubscribeBase(regIds, topic, category, retries, false);
  }

  public Result unsubscribeTopic(List<String> regIds, String topic, String category)
      throws IOException, ParseException {
    return unsubscribeTopic(regIds, topic, category, 1);
  }

  public Result unsubscribeTopic(
      List<String> regIds, String topic, String packageName, String category, int retries)
      throws IOException, ParseException {
    return topicSubscribeBase(regIds, topic, packageName, category, retries, false);
  }

  public Result unsubscribeTopic(String regId, String topic, String category, int retries)
      throws IOException, ParseException {
    List<String> regIds = new ArrayList<>();
    regIds.add(regId);
    return unsubscribeTopic(regIds, topic, category, retries);
  }

  public Result unsubscribeTopic(
      String regId, String topic, String packageName, String category, int retries)
      throws IOException, ParseException {
    return unsubscribeTopic(
        Arrays.asList(regId), topic, packageName, category, retries);
  }

  public Result unsubscribeTopic(String regId, String topic, String category)
      throws IOException, ParseException {
    return unsubscribeTopic(regId, topic, category, 1);
  }

  public Result unsubscribeTopic(String regId, String topic, String packageName, String category)
      throws IOException, ParseException {
    return unsubscribeTopic(regId, topic, packageName, category, 1);
  }

  public Result subscribeTopicByAlias(
      String topic, List<String> aliases, String category, int retries)
      throws IOException, ParseException {
    Result result;
    NameValuePairs body =
        (new NameValuePairs())
            .nameAndValue("aliases", String.join(",", aliases))
            .nameAndValue("topic", topic);
    if (category != null) body.nameAndValue("category", category);
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V2_SUBSCRIBE_TOPIC_BY_ALIAS,
              body.toQueryOrFormData(),
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug(
                        "Attempt #"
                            + executionCount
                            + " to subscribe topic "
                            + topic
                            + " for aliases "
                            + aliases);
                  }));
      if (StringUtils.isBlank(response)) throw exception(retries, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public Result subscribeTopicByAlias(
      String topic, List<String> aliases, String packageName, String category, int retries)
      throws IOException, ParseException {
    Result result;
    NameValuePairs body =
        (new NameValuePairs())
            .nameAndValue("aliases", String.join(",", aliases))
            .nameAndValue("topic", topic);
    if (!StringUtils.isBlank(packageName))
      body.nameAndValue("restricted_package_name", packageName);
    if (category != null) body.nameAndValue("category", category);
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V2_SUBSCRIBE_TOPIC_BY_ALIAS,
              body.toQueryOrFormData(),
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug(
                        "Attempt #"
                            + executionCount
                            + " to subscribe topic "
                            + topic
                            + " for aliases "
                            + aliases);
                  }));
      if (StringUtils.isBlank(response)) throw exception(retries, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public Result unsubscribeTopicByAlias(
      String topic, List<String> aliases, String category, int retries)
      throws IOException, ParseException {
    Result result;
    NameValuePairs body =
        (new NameValuePairs())
            .nameAndValue("aliases", String.join(",", aliases))
            .nameAndValue("topic", topic);
    if (category != null) body.nameAndValue("category", category);
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V2_UNSUBSCRIBE_TOPIC_BY_ALIAS,
              body.toQueryOrFormData(),
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug(
                        "Attempt #"
                            + executionCount
                            + " to unsubscribe topic "
                            + topic
                            + " for aliases "
                            + aliases);
                  }));
      if (StringUtils.isBlank(response)) throw exception(retries, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public Result unsubscribeTopicByAlias(
      String topic, List<String> aliases, String packageName, String category, int retries)
      throws IOException, ParseException {
    Result result;
    NameValuePairs body =
        (new NameValuePairs())
            .nameAndValue("aliases", String.join(",", aliases))
            .nameAndValue("topic", topic);
    if (!StringUtils.isBlank(packageName))
      body.nameAndValue("restricted_package_name", packageName);
    if (category != null) body.nameAndValue("category", category);
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V2_UNSUBSCRIBE_TOPIC_BY_ALIAS,
              body.toQueryOrFormData(),
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug(
                        "Attempt #"
                            + executionCount
                            + " to unsubscribe topic "
                            + topic
                            + " for aliases "
                            + aliases);
                  }));
      if (StringUtils.isBlank(response)) throw exception(retries, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  protected Result topicSubscribeBase(
      List<String> regIds, String topic, String category, int retries, boolean isSubscribe)
      throws IOException, ParseException {
    Result result;
    String regIdsStr = String.join(",", regIds);
    NameValuePairs body =
        (new NameValuePairs())
            .nameAndValue("registration_id", regIdsStr)
            .nameAndValue("topic", topic);
    if (category != null) body.nameAndValue("category", category);
    try {
      String type = isSubscribe ? "subscribe" : "unsubscribe";
      Constants.XmPushRequestPath requestPath =
          isSubscribe
              ? Constants.XmPushRequestPath.V2_SUBSCRIBE_TOPIC
              : Constants.XmPushRequestPath.V2_UNSUBSCRIBE_TOPIC;
      String response =
          post(
              requestPath,
              body.toQueryOrFormData(),
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug(
                        "Attempt #"
                            + executionCount
                            + " to send "
                            + type
                            + " topic "
                            + topic
                            + " to regIds "
                            + regIdsStr);
                  }));
      if (StringUtils.isBlank(response)) throw exception(retries, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  protected Result topicSubscribeBase(
      List<String> regIds,
      String topic,
      String packageName,
      String category,
      int retries,
      boolean isSubscribe)
      throws IOException, ParseException {
    Result result;
    String regIdsStr = String.join(",", regIds);
    NameValuePairs body =
        (new NameValuePairs())
            .nameAndValue("registration_id", regIdsStr)
            .nameAndValue("topic", topic);
    if (!StringUtils.isBlank(packageName))
      body.nameAndValue("restricted_package_name", packageName);
    if (category != null) body.nameAndValue("category", category);
    try {
      String type = isSubscribe ? "subscribe" : "unsubscribe";
      Constants.RequestPath requestPath =
          isSubscribe
              ? Constants.XmPushRequestPath.V2_SUBSCRIBE_TOPIC
              : Constants.XmPushRequestPath.V2_UNSUBSCRIBE_TOPIC;
      String response =
          post(
              requestPath,
              body.toQueryOrFormData(),
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug(
                        "Attempt #"
                            + executionCount
                            + " to send "
                            + type
                            + " topic "
                            + topic
                            + " to regIds "
                            + regIdsStr);
                  }));
      if (StringUtils.isBlank(response)) throw exception(retries, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }
}
