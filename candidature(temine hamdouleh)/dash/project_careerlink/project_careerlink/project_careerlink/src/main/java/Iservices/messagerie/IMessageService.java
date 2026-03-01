package Iservices.messagerie;

import Entities.messagerie.Message;
import java.util.List;

public interface IMessageService {
    void envoyerMessage(Message message);
    List<Message> getMessagesParConversation(int conversationId);
    void marquerCommeLu(int conversationId, int userId);
    void envoyerFichier(int conversationId, int senderId, String filePath, String fileName);
}
