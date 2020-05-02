package com.xiaomi.xmpush.server;

public enum PushJobType {
  Invalid((byte) 0),
  Topic((byte) 1),
  Common((byte) 2),
  Alias((byte) 3),
  BatchAlias((byte) 4),
  BatchRegId((byte) 5),
  ALL((byte) 6),
  UserAccount((byte) 7),
  BatchUserAccount((byte) 8),
  DeviceId((byte) 9),
  ImeiMd5((byte) 10),
  PublicWelfare((byte) 11),
  Miid((byte) 12),
  BatchMiid((byte) 13);

  private static final PushJobType[] VALID_JOB_TYPES;

  static {
    VALID_JOB_TYPES = new PushJobType[] {Topic, Common, Alias, UserAccount, ImeiMd5, Miid};
  }

  private final byte value;

  PushJobType(byte value) {
    this.value = value;
  }

  public static PushJobType from(byte value) {
    for (PushJobType type : VALID_JOB_TYPES) {
      if (type.value == value) return type;
    }
    return Invalid;
  }

  public byte value() {
    return this.value;
  }
}
