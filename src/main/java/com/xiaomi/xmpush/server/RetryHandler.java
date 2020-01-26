package com.xiaomi.xmpush.server;

public interface RetryHandler {
  boolean retryHandle(
      AbstractClient.ResponseWrapper paramResponseWrapper, Exception paramException, int paramInt);
}
