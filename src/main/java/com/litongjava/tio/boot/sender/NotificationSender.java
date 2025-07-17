package com.litongjava.tio.boot.sender;

import com.litongjava.tio.utils.notification.NotifactionWarmModel;

public interface NotificationSender {
  boolean send(NotifactionWarmModel model);
}
