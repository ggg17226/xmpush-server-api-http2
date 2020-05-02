package com.xiaomi.xmpush.server;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

public class ChannelInfo implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String channelId;

  private final String channelName;

  private final String channelDesc;

  private final String soundUrl;

  private final Integer notifyType;

  protected ChannelInfo(Builder builder) {
    this.channelId = builder.channelId;
    this.channelName = builder.channelName;
    this.channelDesc = builder.channelDesc;
    this.soundUrl = builder.soundUrl;
    this.notifyType = builder.notifyType;
  }

  public String getChannelId() {
    return this.channelId;
  }

  public String getChannelName() {
    return this.channelName;
  }

  public String getChannelDesc() {
    return this.channelDesc;
  }

  public String getSoundUrl() {
    return this.soundUrl;
  }

  public Integer getNotifyType() {
    return this.notifyType;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder("ChannelInfo(");
    if (!StringUtils.isEmpty(this.channelId))
      builder.append("channelId=").append(this.channelId).append(", ");
    if (!StringUtils.isEmpty(this.channelName))
      builder.append("channelName=").append(this.channelName).append(", ");
    if (!StringUtils.isEmpty(this.channelDesc))
      builder.append("channelDesc=").append(this.channelDesc).append(", ");
    if (!StringUtils.isEmpty(this.soundUrl))
      builder.append("soundUrl=").append(this.soundUrl).append(", ");
    if (this.notifyType != null) builder.append("notifyType=").append(this.notifyType);
    builder.append(")");
    return builder.toString();
  }

  public static class Builder {
    private String channelId;

    private String channelName;

    private String channelDesc;

    private String soundUrl;

    private Integer notifyType;

    public Builder channelId(String channelId) {
      this.channelId = channelId;
      return this;
    }

    public Builder channelName(String channelName) {
      this.channelName = channelName;
      return this;
    }

    public Builder channelDesc(String channelDesc) {
      this.channelDesc = channelDesc;
      return this;
    }

    public Builder soundUrl(String soundUrl) {
      this.soundUrl = soundUrl;
      return this;
    }

    public Builder notifyType(Integer notifyType) {
      this.notifyType = notifyType;
      return this;
    }

    public ChannelInfo build() {
      return new ChannelInfo(this);
    }
  }
}
