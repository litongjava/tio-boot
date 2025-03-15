package com.litongjava.tio.boot.agent;

import com.litongjava.tio.utils.notification.NotifactionWarmModel;

public interface NotificationSender {
  boolean send(NotifactionWarmModel model);
}
