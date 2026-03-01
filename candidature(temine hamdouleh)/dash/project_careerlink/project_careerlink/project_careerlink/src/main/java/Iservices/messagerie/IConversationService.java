package Iservices.messagerie;

import Entities.messagerie.Conversation;
import java.util.List;

public interface IConversationService {
    Entities.User.User getUserById(int id);
    Conversation creerConversation(int candidatId, int managerId);
    Conversation getConversationEntreUsers(int candidatId, int managerId);
    List<Conversation> getConversationsParUser(int userId);
    List<Entities.User.User> rechercherUsers(String nom, int currentUserId);
}
