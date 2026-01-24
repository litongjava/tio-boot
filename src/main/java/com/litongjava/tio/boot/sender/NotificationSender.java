package com.litongjava.tio.boot.sender;

import com.litongjava.tio.utils.notification.NotifactionWarmModel;

public interface NotificationSender {
  boolean sendAsync(NotifactionWarmModel model);
  boolean send(NotifactionWarmModel model);
}
