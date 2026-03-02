package Iservices.messagerie;

import Entities.messagerie.Notification;
import java.util.List;

public interface INotificationService {
    void ajouterNotification(int userId, String message, String type);
    List<Notification> getNotificationsNonLues(int userId);
    int compterNotificationsNonLues(int userId);
    void marquerCommeLue(int notificationId);
    void marquerToutesCommeLues(int userId);
}
