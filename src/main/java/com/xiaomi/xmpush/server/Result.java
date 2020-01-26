package com.xiaomi.xmpush.server;

import com.xiaomi.push.sdk.ErrorCode;
import org.json.simple.JSONObject;

import java.io.Serializable;

public final class Result implements Serializable {
  private static final long serialVersionUID = 8030699726843716781L;

  private final String messageId;

  private final String traceId;

  private final ErrorCode errorCode;

  private final JSONObject data;

  private final String reason;

  public static final class Builder {
    private String messageId;

    private String traceId = null;

    private JSONObject data;

    private ErrorCode errorCode;

    private String reason;

    public Result fromJson(JSONObject json) {
      if (json.containsKey("data")) this.data = (JSONObject) json.get("data");
      this.reason = (String) json.get("reason");
      Integer code = Integer.valueOf(((Long) json.get("code")).intValue());
      this.errorCode = ErrorCode.valueOf(code, this.reason);
      this.messageId = (this.data == null) ? null : (String) this.data.get("id");
      if (json.containsKey("trace_id")) this.traceId = (String) json.get("trace_id");
      if ((this.messageId == null || this.messageId.length() == 0) && this.traceId != null)
        this.messageId = (String) json.get("trace_id");
      return build();
    }

    public Builder messageId(String msgId) {
      this.messageId = msgId;
      return this;
    }

    public Builder errorCode(ErrorCode value) {
      this.errorCode = value;
      return this;
    }

    public Result build() {
      return new Result(this);
    }
  }

  private Result(Builder builder) {
    this.messageId = builder.messageId;
    this.data = builder.data;
    this.errorCode = builder.errorCode;
    this.reason = builder.reason;
    this.traceId = builder.traceId;
  }

  public String getMessageId() {
    return this.messageId;
  }

  public String getReason() {
    return this.reason;
  }

  public ErrorCode getErrorCode() {
    return this.errorCode;
  }

  public JSONObject getData() {
    return this.data;
  }

  public String getData(String key) {
    if (this.data == null) return null;
    if (this.data.containsKey(key)) return (String) this.data.get(key);
    return null;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder("[");
    if (this.messageId != null) builder.append(" messageId=").append(this.messageId);
    if (this.errorCode != null) builder.append(" errorCode=").append(this.errorCode.getValue());
    if (this.reason != null) builder.append(" reason=").append(this.reason);
    if (this.data != null) builder.append(" data=").append(this.data.toString());
    if (this.traceId != null) builder.append(" trace_id=").append(this.traceId);
    return builder.append(" ]").toString();
  }
}
