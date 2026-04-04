package nexus.io.tio.utils.notification;

public interface NotificationSender {
  boolean sendAsync(NotifactionWarmModel model);

  boolean send(NotifactionWarmModel model);
}
