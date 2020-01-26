package com.xiaomi.xmpush.server;

import com.xiaomi.push.sdk.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class Sender extends PushSender<Sender> {
  private static final int BROADCAST_TOPIC_MAX = 5;

  private static final String TOPIC_SPLITTER = ";$;";

  private static final String COMMA = ",";

  public Sender(String security) {
    super(security);
  }

  public Sender(String security, boolean isVip) {
    super(security, isVip);
  }

  public Sender(String security, Region region) {
    super(security, region);
  }

  public Sender(String security, String token) {
    super(security, token);
  }

  public Sender(String security, String token, boolean isVip) {
    super(security, token, isVip);
  }

  public Sender(String security, String token, Region region) {
    super(security, token, region);
  }

  protected static void tryAddJson(JSONObject json, String parameterName, Object value) {
    if (!XMStringUtils.isEmpty(parameterName) && value != null) json.put(parameterName, value);
  }

  private static boolean isMultiPackageName(Message message) {
    String[] packageNames = message.getRestrictedPackageNames();
    return (packageNames == null
        || packageNames.length == 0
        || (message.getRestrictedPackageNames()).length >= 2);
  }

  public Result send(Message message, String registrationId, int retries) throws IOException {
    Result result;
    NameValuePairs body =
        buildFormDataFromMessage(message).nameAndValue("registration_id", registrationId);
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V3_REGID_MESSAGE,
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
                            + " to send message "
                            + message
                            + " to regIds "
                            + registrationId);
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(retries, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public Result unionSend(Message message, List<String> registrationIds, int retries)
      throws IOException {
    Result result;
    NameValuePairs body =
        buildFormDataFromMessage(message)
            .nameAndValue("registration_id", String.join(",", registrationIds));
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V4_REGID_MESSAGE,
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
                            + " to send message "
                            + message
                            + " to regIds "
                            + registrationIds);
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(retries, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  private Result sendHybridMessageByRegId(
      Message message, String registrationId, boolean isDebug, int retries)
      throws IOException, ParseException {
    Result result;
    message.getExtra().put("push_server_action", "hybrid_message");
    if (isDebug) message.getExtra().put("hybrid_debug", "1");
    NameValuePairs body =
        buildFormDataFromMessage(message).nameAndValue("registration_id", registrationId);
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V2_REGID_MESSAGE,
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
                            + " to send message "
                            + message
                            + " to regIds "
                            + registrationId);
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(retries, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public Result broadcastHybridAll(Message message, int retries)
      throws IOException, ParseException {
    return broadcastHybridAll(message, false, retries);
  }

  public Result broadcastHybridAll(Message message, boolean isDebug, int retries)
      throws IOException, ParseException {
    message.getExtra().put("push_server_action", "hybrid_message");
    if (isDebug) message.getExtra().put("hybrid_debug", "1");
    return broadcastAll(message, retries);
  }

  public Result broadcast(Message message, String topic, int retries)
      throws IOException, ParseException {
    Result result;
    NameValuePairs body = buildFormDataFromMessage(message).nameAndValue("topic", topic);
    try {
      String response =
          post(
              isMultiPackageName(message)
                  ? Constants.XmPushRequestPath.V3_BROADCAST
                  : Constants.XmPushRequestPath.V2_BROADCAST,
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
                            + " to broadcast message "
                            + message
                            + " to topic: "
                            + topic);
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(retries, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public Result multiTopicBroadcast(
      Message message, List<String> topics, BROADCAST_TOPIC_OP topicOp, int retries)
      throws IOException, ParseException, IllegalArgumentException {
    if (topics == null || topics.size() <= 0 || topics.size() > 5)
      throw new IllegalArgumentException("topics size invalid");
    if (topics.size() == 1) return broadcast(message, topics.get(0), retries);
    NameValuePairs body =
        buildFormDataFromMessage(message)
            .nameAndValue("topic_op", topicOp.toString())
            .nameAndValue("topics", String.join(";$;", topics));
    try {
      String response =
          post(
              isMultiPackageName(message)
                  ? Constants.XmPushRequestPath.V3_MULTI_TOPIC_BROADCAST
                  : Constants.XmPushRequestPath.V2_MULTI_TOPIC_BROADCAST,
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
                            + " to broadcast message "
                            + message
                            + " to topic: "
                            + String.join(";$;", topics)
                            + " op="
                            + topicOp.toString());
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(retries, null);
      Result result = parseResult(response);
      return result;
    } catch (Exception e) {
      throw exception(retries, e);
    }
  }

  public Result broadcastAll(Message message, int retries) throws IOException, ParseException {
    Result result;
    NameValuePairs body = buildFormDataFromMessage(message);
    NameValuePairs headers =
        (new NameValuePairs())
            .nameAndValue("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
    try {
      String response =
          post(
              isMultiPackageName(message)
                  ? Constants.XmPushRequestPath.V3_BROADCAST_TO_ALL
                  : Constants.XmPushRequestPath.V2_BROADCAST_TO_ALL,
              body.toQueryOrFormData(),
              headers,
              null,
              new PushSender.DefaultPushRetryHandler(
                  retries,
                  1000,
                  executionCount -> {
                    log.debug(
                        "Attempt #" + retries + " to broadcast message " + message + " to all.");
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(retries, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public Result sendToAlias(Message message, String alias, int retries)
      throws IOException, ParseException {
    List<String> aliases = new ArrayList<>();
    aliases.add(alias);
    return sendToAlias(message, aliases, retries);
  }

  public Result sendToAlias(Message message, List<String> aliases, int retries)
      throws IOException, ParseException {
    Result result;
    NameValuePairs body =
        buildFormDataFromMessage(message).nameAndValue("alias", aliases.toArray());
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V3_ALIAS_MESSAGE,
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
                            + " to send message "
                            + message
                            + " to alias ["
                            + String.join(",", aliases)
                            + "].");
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(retries, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public Result sendToRegion(Message message, List<String> regions, int retries)
      throws IOException, ParseException {
    Result result;
    NameValuePairs body =
        buildFormDataFromMessage(message).nameAndValue("region", regions.toArray());
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V2_REGION_MESSAGE,
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
                            + " to send message "
                            + message
                            + " to regions ["
                            + String.join(",", regions)
                            + "]");
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(retries, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public Result sendHybridMessageByRegId(Message message, List<String> regIds, int retries)
      throws IOException, ParseException {
    return sendHybridMessageByRegId(message, regIds, false, retries);
  }

  public Result sendHybridMessageByRegId(
      Message message, List<String> regIds, boolean isDebug, int retries)
      throws IOException, ParseException {
    StringBuilder sb = new StringBuilder(regIds.get(0));
    for (int i = 1; i < regIds.size(); i++) sb.append(",").append(regIds.get(i));
    return sendHybridMessageByRegId(message, sb.toString(), isDebug, retries);
  }

  public Result sendToUserAccount(Message message, String userAccount, int retries)
      throws IOException, ParseException {
    List<String> userAccounts = new ArrayList<>();
    userAccounts.add(userAccount);
    return sendToUserAccount(message, userAccounts, retries);
  }

  public Result sendToUserAccount(Message message, List<String> userAccounts, int retries)
      throws IOException, ParseException {
    Result result;
    NameValuePairs body =
        buildFormDataFromMessage(message).nameAndValue("user_account", userAccounts.toArray());
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V2_USERACCOUNT_MESSAGE,
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
                            + " to send message "
                            + message
                            + " to user account ["
                            + String.join(",", userAccounts)
                            + "].");
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(retries, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  private NameValuePairs buildFormDataFromMessage(Message message) {
    NameValuePairs formData =
        (new NameValuePairs())
            .nameAndValue(
                "restricted_package_name",
                isMultiPackageName(message)
                    ? message.getRestrictedPackageNames()
                    : message.getRestrictedPackageName())
            .nameAndValue("collapse_key", message.getCollapseKey())
            .nameAndValue("time_to_live", message.getTimeToLive())
            .nameAndValue("payload", message.getPayload())
            .nameAndValue("title", message.getTitle())
            .nameAndValue("description", message.getDescription())
            .nameAndValue("notify_type", message.getNotifyType())
            .nameAndValue("pass_through", message.getPassThrough())
            .nameAndValue("notify_id", message.getNotifyId())
            .nameAndValue("time_to_send", message.getTimeToSend());
    Map<String, String> extraInfo = message.getExtra();
    if (extraInfo != null && !extraInfo.isEmpty())
      for (Map.Entry<String, String> entry : extraInfo.entrySet()) {
        formData.nameAndValue("extra." + entry.getKey(), entry.getValue());
      }
    Map<String, String> apsProperFieldsInfo = message.getApsProperFields();
    if (apsProperFieldsInfo != null && !apsProperFieldsInfo.isEmpty())
      for (Map.Entry<String, String> entry : apsProperFieldsInfo.entrySet()) {
        formData.nameAndValue("aps_proper_fields." + entry.getKey(), entry.getValue());
      }
    return formData;
  }

  public Result revokeAliasMessage(List<String> aliases, int notifyId, String packageName)
      throws IOException, ParseException {
    Result result;
    NameValuePairs body =
        (new NameValuePairs())
            .nameAndValue("aliases", aliases.toArray((Object[]) new String[0]))
            .nameAndValue("restricted_package_name", packageName)
            .nameAndValue("notify_id", Integer.valueOf(notifyId));
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V1_REVOKE_ALIAS_MESSAGE,
              body.toQueryOrFormData(),
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  0,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to revoke alias message!");
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(0, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(0, e);
    }
    return result;
  }

  public Result revokeRegIdMessage(List<String> registrationIds, int notifyId, String packageName)
      throws IOException, ParseException {
    Result result;
    NameValuePairs body =
        (new NameValuePairs())
            .nameAndValue("registration_ids", registrationIds.toArray((Object[]) new String[0]))
            .nameAndValue("restricted_package_name", packageName)
            .nameAndValue("notify_id", Integer.valueOf(notifyId));
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V1_REVOKE_REGID_MESSAGE,
              body.toQueryOrFormData(),
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  0,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to revoke regid message!");
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(0, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(0, e);
    }
    return result;
  }

  public Result revokeUserAccountMessage(
      List<String> userAccounts, int notifyId, String packageName)
      throws IOException, ParseException {
    Result result;
    NameValuePairs body =
        (new NameValuePairs())
            .nameAndValue("user_accounts", userAccounts.toArray((Object[]) new String[0]))
            .nameAndValue("restricted_package_name", packageName)
            .nameAndValue("notify_id", Integer.valueOf(notifyId));
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V1_REVOKE_USERACCOUNT_MESSAGE,
              body.toQueryOrFormData(),
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  0,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to revoke user account message!");
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(0, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(0, e);
    }
    return result;
  }

  public Result revokeMiidMessage(List<String> miids, int notifyId, String packageName)
      throws IOException, ParseException {
    Result result;
    NameValuePairs body =
        (new NameValuePairs())
            .nameAndValue("miids", miids.toArray((Object[]) new String[0]))
            .nameAndValue("restricted_package_name", packageName)
            .nameAndValue("notify_id", Integer.valueOf(notifyId));
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V1_REVOKE_MIID_MESSAGE,
              body.toQueryOrFormData(),
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  0,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to revoke miid message!");
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(0, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(0, e);
    }
    return result;
  }

  public Result revokeImeimd5Message(List<String> imeimd5s, int notifyId, String packageName)
      throws IOException, ParseException {
    Result result;
    NameValuePairs body =
        (new NameValuePairs())
            .nameAndValue("imeimd5s", imeimd5s.toArray((Object[]) new String[0]))
            .nameAndValue("restricted_package_name", packageName)
            .nameAndValue("notify_id", Integer.valueOf(notifyId));
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V1_REVOKE_IMEIMD5_MESSAGE,
              body.toQueryOrFormData(),
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  0,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to revoke imeimd5 message!");
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(0, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(0, e);
    }
    return result;
  }

  public Result revokeTopicMessage(String topic, int notifyId, String packageName)
      throws IOException, ParseException {
    Result result;
    NameValuePairs body =
        (new NameValuePairs())
            .nameAndValue("topic", topic)
            .nameAndValue("restricted_package_name", packageName)
            .nameAndValue("notify_id", Integer.valueOf(notifyId));
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V1_REVOKE_TOPIC_MESSAGE,
              body.toQueryOrFormData(),
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  0,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to revoke topic message!");
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(0, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(0, e);
    }
    return result;
  }

  public Result revokeMultiTopicMessage(
      List<String> topics, String topicOp, int notifyId, String packageName)
      throws IOException, ParseException {
    Result result;
    NameValuePairs body =
        (new NameValuePairs())
            .nameAndValue("topics", topics.toArray((Object[]) new String[0]))
            .nameAndValue("topic_op", topicOp)
            .nameAndValue("restricted_package_name", packageName)
            .nameAndValue("notify_id", Integer.valueOf(notifyId));
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V1_REVOKE_MULTITOPIC_MESSAGE,
              body.toQueryOrFormData(),
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  0,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to revoke multi topic message!");
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(0, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(0, e);
    }
    return result;
  }

  public Result stopMessageById(String packageName, List<String> msgIds) throws IOException {
    Result result;
    NameValuePairs body =
        (new NameValuePairs())
            .nameAndValue("msg_ids", msgIds.toArray((Object[]) new String[0]))
            .nameAndValue("restricted_package_name", packageName);
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V1_STOP_MESSAGE_BY_ID,
              body.toQueryOrFormData(),
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  0,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to revoke multi topic message!");
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(0, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(0, e);
    }
    return result;
  }

  public Result stopMessageByJobKey(String packageName, List<String> jobKeys) throws IOException {
    Result result;
    NameValuePairs body =
        (new NameValuePairs())
            .nameAndValue("job_keys", jobKeys.toArray((Object[]) new String[0]))
            .nameAndValue("restricted_package_name", packageName);
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V1_STOP_MESSAGE_BY_JOBKEY,
              body.toQueryOrFormData(),
              null,
              null,
              new PushSender.DefaultPushRetryHandler(
                  0,
                  1000,
                  executionCount -> {
                    log.debug("Attempt #" + executionCount + " to revoke multi topic message!");
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(0, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(0, e);
    }
    return result;
  }

  public Result send(Message message, List<String> regIds, int retries)
      throws IOException, ParseException {
    StringBuilder sb = new StringBuilder(regIds.get(0));
    for (int i = 1; i < regIds.size(); i++) sb.append(",").append(regIds.get(i));
    return send(message, sb.toString(), retries);
  }

  public Result send(List<TargetedMessage> messages, int retries)
      throws IOException, ParseException {
    return send(messages, retries, 0L);
  }

  public Result send(List<TargetedMessage> messages, int retries, long timeToSend)
      throws IOException, ParseException {
    Result result;
    Constants.XmPushRequestPath requestPath;
    if (messages.isEmpty()) {
      log.warn(
          "Empty message, returned. Remote server " + this.remoteHost + "(" + this.remoteIp + ")");
      return (new Result.Builder()).errorCode(ErrorCode.Success).build();
    }
    if (messages.get(0).getTargetType() == 2) {
      requestPath = Constants.XmPushRequestPath.V2_SEND_MULTI_MESSAGE_WITH_ALIAS;
    } else if (messages.get(0).getTargetType() == 3) {
      requestPath = Constants.XmPushRequestPath.V2_SEND_MULTI_MESSAGE_WITH_ACCOUNT;
    } else {
      requestPath = Constants.XmPushRequestPath.V2_SEND_MULTI_MESSAGE_WITH_REGID;
    }
    NameValuePairs body =
        (new NameValuePairs())
            .nameAndValue("messages", toString(messages))
            .nameAndValue("time_to_send", Long.valueOf(timeToSend));
    try {
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
                        "Attempt #" + executionCount + " to send messages " + messages.size());
                  }));
      if (XMStringUtils.isBlank(response)) throw exception(retries, null);
      result = parseResult(response);
    } catch (Exception e) {
      throw exception(retries, e);
    }
    return result;
  }

  public Result deleteScheduleJob(String jobId) throws IOException, ParseException {
    NameValuePairs body = (new NameValuePairs()).nameAndValue("job_id", jobId);
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V2_DELETE_SCHEDULE_JOB,
              body.toQueryOrFormData(),
              null,
              null,
              null);
      if (XMStringUtils.isBlank(response)) throw exception(1, null);
      return parseResult(response);
    } catch (Exception e) {
      throw exception(1, e);
    }
  }

  public Result deleteScheduleJobByJobKey(String jobKey) throws IOException, ParseException {
    NameValuePairs body = (new NameValuePairs()).nameAndValue("jobkey", jobKey);
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V3_DELETE_SCHEDULE_JOB,
              body.toQueryOrFormData(),
              null,
              null,
              null);
      if (XMStringUtils.isBlank(response)) throw exception(1, null);
      return parseResult(response);
    } catch (Exception e) {
      throw exception(1, e);
    }
  }

  public Result checkScheduleJobExist(String jobId) throws IOException, ParseException {
    NameValuePairs body = (new NameValuePairs()).nameAndValue("job_id", jobId);
    try {
      String response =
          post(
              Constants.XmPushRequestPath.V2_CHECK_SCHEDULE_JOB_EXIST,
              body.toQueryOrFormData(),
              null,
              null,
              null);
      if (XMStringUtils.isBlank(response)) throw exception(1, null);
      return parseResult(response);
    } catch (Exception e) {
      throw exception(1, e);
    }
  }

  private String toString(List<TargetedMessage> messages) {
    JSONArray jsonArray = new JSONArray();
    for (TargetedMessage message : messages) {
      JSONObject jsonMessage = new JSONObject();
      JSONObject msg = toJson(message.getMessage());
      tryAddJson(jsonMessage, "target", message.getTarget());
      tryAddJson(jsonMessage, "message", msg);
      jsonArray.add(jsonMessage);
    }
    return jsonArray.toString();
  }

  protected void before(PushSender.SenderContext context) {
    after(context);
    if (Constants.INCLUDE_LAST_METRICS
        && this.lastResult != null
        && this.lastResult.containsKey("trace_id")) {
      String traceId = (String) this.lastResult.get("trace_id");
      context.headers().nameAndValue("X-PUSH-LAST-REQUEST-ID", traceId);
    }
  }

  private JSONObject toJson(Message msg) {
    JSONObject json = new JSONObject();
    tryAddJson(json, "payload", msg.getPayload());
    tryAddJson(json, "title", msg.getTitle());
    tryAddJson(json, "description", msg.getDescription());
    tryAddJson(json, "notify_type", msg.getNotifyType());
    tryAddJson(json, "notify_id", msg.getNotifyId());
    tryAddJson(json, "pass_through", msg.getPassThrough());
    tryAddJson(json, "restricted_package_name", msg.getRestrictedPackageName());
    tryAddJson(json, "time_to_live", msg.getTimeToLive());
    tryAddJson(json, "time_to_send", msg.getTimeToSend());
    tryAddJson(json, "collapse_key", msg.getCollapseKey());
    Map<String, String> extraInfo = msg.getExtra();
    if (extraInfo != null && !extraInfo.isEmpty()) {
      JSONObject extraJson = new JSONObject();
      for (Map.Entry<String, String> entry : extraInfo.entrySet())
        tryAddJson(extraJson, entry.getKey(), entry.getValue());
      tryAddJson(json, "extra", extraJson);
    }
    Map<String, String> apsInfo = msg.getApsProperFields();
    if (apsInfo != null && !apsInfo.isEmpty()) {
      JSONObject apsJson = new JSONObject();
      for (Map.Entry<String, String> entry : apsInfo.entrySet())
        tryAddJson(apsJson, entry.getKey(), entry.getValue());
      tryAddJson(json, "aps_proper_fields", apsJson);
    }
    return json;
  }

  public enum BROADCAST_TOPIC_OP {
    UNION,
    INTERSECTION,
    EXCEPT
  }
}
