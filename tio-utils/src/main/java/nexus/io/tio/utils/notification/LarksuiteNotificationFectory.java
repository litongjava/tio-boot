package nexus.io.tio.utils.notification;

public class LarksuiteNotificationFectory implements INotificationFactory {

  @Override
  public INotification getNotifaction() {
    return new LarksuiteNotification();
  }

}
