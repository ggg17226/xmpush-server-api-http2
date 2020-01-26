package com.xiaomi.xmpush.server;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Message implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String collapseKey;

  private final String payload;

  private final String title;

  private final String description;

  private final Integer notifyType;

  private final Long timeToLive;

  private final Integer passThrough;

  private final Integer notifyId;

  private final String[] restrictedPackageNames;

  private final Map<String, String> extra;

  private Map<String, String> apsProperFields;

  private final Long timeToSend;

  public static final int PASS_THROUGH_PASS = 1;

  public static final int PASS_THROUGH_NOTIFICATION = 0;

  public static final int NOTIFY_TYPE_ALL = -1;

  public static final int NOTIFY_TYPE_SOUND = 1;

  public static final int NOTIFY_TYPE_VIBRATE = 2;

  public static final int NOTIFY_TYPE_LIGHTS = 4;

  public static class ExtraBuilder {
    protected Map<String, String> extra = new HashMap<>();

    public ExtraBuilder extras(Map<String, String> extras) {
      this.extra.putAll(extras);
      return this;
    }

    public Map<String, String> build() {
      return this.extra;
    }
  }

  private enum ButtonIndex {
    left,
    mid,
    right
  }

  public static class ButtonBuilder extends ExtraBuilder
      implements Constants.NotificationStyleExtra {
    private String name;

    private String effect;

    private String intentUri;

    private String className;

    private String webUrl;

    private Message.ButtonIndex index;

    private Message.NotificationStyleBuilder superBuiler;

    public ButtonBuilder(Message.ButtonIndex index, Message.NotificationStyleBuilder superBuiler) {
      this.index = index;
      this.superBuiler = superBuiler;
    }

    public ButtonBuilder name(String name) {
      this.name = name;
      return this;
    }

    public ButtonBuilder launcher() {
      this.effect = "1";
      return this;
    }

    public ButtonBuilder activity(String intentUri) {
      this.effect = "2";
      this.intentUri = intentUri;
      return this;
    }

    public ButtonBuilder activity() {
      this.effect = "2";
      return this;
    }

    public ButtonBuilder intentUri(String intentUri) {
      this.intentUri = intentUri;
      return this;
    }

    public ButtonBuilder className(String className) {
      this.className = className;
      return this;
    }

    public ButtonBuilder web(String webUrl) {
      this.effect = "3";
      this.webUrl = webUrl;
      return this;
    }

    public Map<String, String> build() {
      switch (this.index) {
        case left:
          this.extra.put("notification_style_button_left_name", this.name);
          this.extra.put("notification_style_button_left_notify_effect", this.effect);
          break;
        case mid:
          this.extra.put("notification_style_button_mid_name", this.name);
          this.extra.put("notification_style_button_mid_notify_effect", this.effect);
          break;
        case right:
          this.extra.put("notification_style_button_right_name", this.name);
          this.extra.put("notification_style_button_right_notify_effect", this.effect);
          break;
      }
      if (!"1".equals(this.effect))
        if ("2".equals(this.effect)) {
          switch (this.index) {
            case left:
              if (!XMStringUtils.isBlank(this.intentUri))
                this.extra.put("notification_style_button_left_intent_uri", this.intentUri);
              if (!XMStringUtils.isBlank(this.className))
                this.extra.put("notification_style_button_left_intent_class", this.className);
              break;
            case mid:
              if (!XMStringUtils.isBlank(this.intentUri))
                this.extra.put("notification_style_button_mid_intent_uri", this.intentUri);
              if (!XMStringUtils.isBlank(this.className))
                this.extra.put("notification_style_button_mid_intent_class", this.className);
              break;
            case right:
              if (!XMStringUtils.isBlank(this.intentUri))
                this.extra.put("notification_style_button_right_intent_uri", this.intentUri);
              if (!XMStringUtils.isBlank(this.className))
                this.extra.put("notification_style_button_right_intent_class", this.className);
              break;
          }
        } else if ("3".equals(this.effect)) {
          switch (this.index) {
            case left:
              this.extra.put("notification_style_button_left_web_uri", this.webUrl);
              break;
            case mid:
              this.extra.put("notification_style_button_mid_web_uri", this.webUrl);
              break;
            case right:
              this.extra.put("notification_style_button_right_web_uri", this.webUrl);
              break;
          }
        }
      return this.extra;
    }

    public Message.ExtraBuilder start() {
      return this;
    }

    public Message.NotificationStyleBuilder end() {
      build();
      this.superBuiler.extras(this.extra);
      return this.superBuiler;
    }
  }

  public static class NotificationStyleBuilder extends ExtraBuilder
      implements Constants.NotificationStyleExtra {
    private String type;

    private String largeIconUri;

    private String bigPicUri;

    private Message.ButtonBuilder left;

    private Message.ButtonBuilder mid;

    private Message.ButtonBuilder right;

    public NotificationStyleBuilder bigTextStyle() {
      this.type = "1";
      return this;
    }

    public NotificationStyleBuilder bigPictureStyle() {
      this.type = "2";
      return this;
    }

    public NotificationStyleBuilder largeIconUri(String uri) {
      this.largeIconUri = uri;
      return this;
    }

    public NotificationStyleBuilder bigPicUri(String uri) {
      this.bigPicUri = uri;
      return this;
    }

    public Message.ButtonBuilder leftBtn() {
      this.left = new Message.ButtonBuilder(Message.ButtonIndex.left, this);
      return this.left;
    }

    public Message.ButtonBuilder midBtn() {
      this.mid = new Message.ButtonBuilder(Message.ButtonIndex.mid, this);
      return this.mid;
    }

    public Message.ButtonBuilder rightBtn() {
      this.right = new Message.ButtonBuilder(Message.ButtonIndex.right, this);
      return this.right;
    }

    public Map<String, String> build() {
      this.extra.put("notification_style_type", this.type);
      this.extra.put("notification_large_icon_uri", this.largeIconUri);
      this.extra.put("notification_bigPic_uri", this.bigPicUri);
      if (this.left != null) this.extra.putAll(this.left.build());
      if (this.mid != null) this.extra.putAll(this.mid.build());
      if (this.right != null) this.extra.putAll(this.right.build());
      return this.extra;
    }
  }

  public static final class Builder {
    private String collapseKey;

    private String payload;

    private String title;

    private String description;

    private Integer notifyType;

    private Long timeToLive;

    private String[] restrictedPackageNames;

    private Integer passThrough = Integer.valueOf(0);

    private Integer notifyId = Integer.valueOf(0);

    private Map<String, String> extra;

    private Long timeToSend;

    public Builder() {
      this.extra = new LinkedHashMap<>();
    }

    protected Builder collapseKey(String value) {
      this.collapseKey = value;
      return this;
    }

    public Builder payload(String value) {
      this.payload = value;
      return this;
    }

    public Builder title(String value) {
      this.title = value;
      return this;
    }

    public Builder description(String value) {
      this.description = value;
      return this;
    }

    public Builder notifyType(Integer value) {
      this.notifyType = value;
      return this;
    }

    public Builder notifyId(Integer value) {
      this.notifyId = value;
      return this;
    }

    public Builder timeToLive(long value) {
      this.timeToLive = Long.valueOf(value);
      return this;
    }

    public Builder restrictedPackageName(String value) {
      this.restrictedPackageNames = new String[1];
      this.restrictedPackageNames[0] = value;
      return this;
    }

    public Builder restrictedPackageNames(String[] value) {
      this.restrictedPackageNames = value;
      return this;
    }

    public Builder passThrough(int passThrough) {
      this.passThrough = Integer.valueOf(passThrough);
      return this;
    }

    public Builder extra(String key, String value) {
      this.extra.put(key, value);
      return this;
    }

    public Builder extra(Map<String, String> extra) {
      this.extra.putAll(extra);
      return this;
    }

    public Builder timeToSend(long timeToSend) {
      this.timeToSend = Long.valueOf(timeToSend);
      return this;
    }

    public Builder instantNotify(boolean isInstantNotify) {
      if (isInstantNotify) {
        this.extra.put("instant_notify", "1");
      } else {
        this.extra.remove("instant_notify");
      }
      return this;
    }

    public Builder enableFlowControl(boolean needFlowControl) {
      if (needFlowControl) {
        this.extra.put("flow_control", "1");
      } else {
        this.extra.remove("flow_control");
      }
      return this;
    }

    public Builder hybridPath(String hybridPath) {
      extra("hybrid_pn", hybridPath);
      return this;
    }

    public Message build() {
      return new Message(this);
    }

    public void setExtra(Map<String, String> extra) {
      this.extra = extra;
    }
  }

  public static final class IOSBuilder {
    private String description;

    private Long timeToLive;

    private Map<String, String> extra = new LinkedHashMap<>();

    private Map<String, String> apsProperFields = new LinkedHashMap<>();

    private Long timeToSend;

    public IOSBuilder description(String value) {
      this.description = value;
      return this;
    }

    public IOSBuilder timeToLive(long value) {
      this.timeToLive = Long.valueOf(value);
      return this;
    }

    public IOSBuilder extra(String key, String value) {
      this.extra.put(key, value);
      return this;
    }

    public IOSBuilder timeToSend(long timeToSend) {
      this.timeToSend = Long.valueOf(timeToSend);
      return this;
    }

    public IOSBuilder title(String value) {
      this.apsProperFields.put("title", value);
      return this;
    }

    public IOSBuilder subtitle(String value) {
      this.apsProperFields.put("subtitle", value);
      return this;
    }

    public IOSBuilder body(String value) {
      this.apsProperFields.put("body", value);
      return this;
    }

    public IOSBuilder mutableContent(String value) {
      this.apsProperFields.put("mutable-content", value);
      return this;
    }

    public IOSBuilder apsProperties(String key, String value) {
      this.apsProperFields.put(key, value);
      return this;
    }

    public Message build() {
      return new Message(this);
    }

    public IOSBuilder badge(int badge) {
      this.extra.put("badge", String.valueOf(badge));
      return this;
    }

    public IOSBuilder category(String category) {
      this.extra.put("category", category);
      return this;
    }

    public IOSBuilder soundURL(String url) {
      this.extra.put("sound_url", url);
      return this;
    }

    public IOSBuilder apnsOnly() {
      this.extra.put("ios_msg_channel", "1");
      return this;
    }

    public IOSBuilder connectionOnly() {
      this.extra.put("ios_msg_channel", "2");
      return this;
    }

    public IOSBuilder contentAvailble(String value) {
      this.extra.put("content-available", value);
      return this;
    }

    public IOSBuilder showContent() {
      this.extra.put("show-content", "1");
      return this;
    }

    public void setExtra(Map<String, String> extra) {
      this.extra = extra;
    }

    public void setApsProperFields(Map<String, String> apsProperFields) {
      this.apsProperFields = apsProperFields;
    }
  }

  protected Message(IOSBuilder builder) {
    this.collapseKey = null;
    this.payload = null;
    this.title = null;
    this.description = builder.description;
    this.notifyType = null;
    this.timeToLive = builder.timeToLive;
    this.restrictedPackageNames = null;
    this.passThrough = null;
    this.notifyId = null;
    this.extra = builder.extra;
    this.apsProperFields = builder.apsProperFields;
    this.timeToSend = builder.timeToSend;
  }

  protected Message(Builder builder) {
    this.collapseKey = builder.collapseKey;
    this.payload = builder.payload;
    this.title = builder.title;
    this.description = builder.description;
    this.notifyType = builder.notifyType;
    this.timeToLive = builder.timeToLive;
    this.restrictedPackageNames = builder.restrictedPackageNames;
    this.passThrough = builder.passThrough;
    this.notifyId = builder.notifyId;
    this.extra = builder.extra;
    this.apsProperFields = null;
    this.timeToSend = builder.timeToSend;
  }

  protected String getCollapseKey() {
    return this.collapseKey;
  }

  public String getPayload() {
    return this.payload;
  }

  public String getTitle() {
    return this.title;
  }

  public String getDescription() {
    return this.description;
  }

  public Integer getNotifyType() {
    return this.notifyType;
  }

  public Integer getNotifyId() {
    return this.notifyId;
  }

  public Long getTimeToLive() {
    return this.timeToLive;
  }

  public String getRestrictedPackageName() {
    if (this.restrictedPackageNames != null && this.restrictedPackageNames.length != 0)
      return this.restrictedPackageNames[0];
    return null;
  }

  public String[] getRestrictedPackageNames() {
    return this.restrictedPackageNames;
  }

  public Integer getPassThrough() {
    return this.passThrough;
  }

  public Map<String, String> getExtra() {
    return this.extra;
  }

  public Map<String, String> getApsProperFields() {
    return this.apsProperFields;
  }

  public Long getTimeToSend() {
    return this.timeToSend;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder("Message(");
    if (!XMStringUtils.isEmpty(this.collapseKey))
      builder.append("collapseKey=").append(this.collapseKey).append(", ");
    if (!XMStringUtils.isEmpty(this.payload))
      builder.append("payload=").append(this.payload).append(", ");
    if (!XMStringUtils.isEmpty(this.title))
      builder.append("title=").append(this.title).append(", ");
    if (!XMStringUtils.isEmpty(this.description))
      builder.append("description=").append(this.description).append(", ");
    if (this.timeToLive != null) builder.append("timeToLive=").append(this.timeToLive).append(", ");
    if (this.restrictedPackageNames != null && this.restrictedPackageNames.length != 0)
      builder
          .append("restrictedPackageNames=")
          .append("[")
          .append(arrayToString(this.restrictedPackageNames))
          .append("]")
          .append(", ");
    if (this.notifyType != null) builder.append("notifyType=").append(this.notifyType).append(", ");
    if (this.notifyId != null) builder.append("notifyId=").append(this.notifyId).append(", ");
    if (!this.extra.isEmpty())
      for (Map.Entry<String, String> entry : this.extra.entrySet())
        builder
            .append("extra.")
            .append(entry.getKey())
            .append("=")
            .append(entry.getValue())
            .append(", ");
    if (this.apsProperFields != null && !this.apsProperFields.isEmpty())
      for (Map.Entry<String, String> entry : this.apsProperFields.entrySet())
        builder
            .append("aps_proper_fields.")
            .append(entry.getKey())
            .append("=")
            .append(entry.getValue())
            .append(", ");
    if (builder.charAt(builder.length() - 1) == ' ')
      builder.delete(builder.length() - 2, builder.length());
    builder.append(")");
    return builder.toString();
  }

  private String arrayToString(String[] array) {
    StringBuilder sb = new StringBuilder();
    int i;
    for (i = 0; i < array.length - 1; i++) sb.append(array[i]).append(",");
    sb.append(array[i]);
    return sb.toString();
  }
}
