package Services.messagerie;

import Entities.messagerie.Message;
import Iservices.messagerie.IMessageService;
import Utils.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageService implements IMessageService {

    private Connection con;


    public MessageService() {
        con = Mydatabase.getInstance().getConnection();
    }

    //jdidddddddd

    @Override
    public void envoyerMessage(Message message) {
        String sql = "INSERT INTO messages (conversation_id, sender_id, content, is_ai, file_path, file_name, is_vocal) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, message.getConversationId());
           // ps.setInt(2, message.getSenderId());
            if (message.getSenderId() == 0) {       // ← si c'est l'IA
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setInt(2, message.getSenderId()); // ← si c'est un vrai user
            }
            ps.setString(3, message.getContent());
            ps.setBoolean(4, message.isAi());
            ps.setString(5, message.getFilePath());
            ps.setString(6, message.getFileName());
            ps.setBoolean(7, message.isVocal());

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*
    @Override
    public void envoyerMessage(Message message) {
        String sql = "INSERT INTO messages (conversation_id, sender_id, content, is_ai) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, message.getConversationId());
            ps.setInt(2, message.getSenderId());
            ps.setString(3, message.getContent());
            ps.setBoolean(4, message.isAi());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    */


    /*
    @Override
    public List<Message> getMessagesParConversation(int conversationId) {
        List<Message> liste = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE conversation_id = ? ORDER BY sent_at ASC";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, conversationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Message msg = new Message();
                msg.setId(rs.getInt("id"));
                msg.setConversationId(rs.getInt("conversation_id"));
                msg.setSenderId(rs.getInt("sender_id"));
                msg.setContent(rs.getString("content"));
                msg.setAi(rs.getBoolean("is_ai"));
                msg.setRead(rs.getBoolean("is_read"));
                msg.setSentAt(rs.getTimestamp("sent_at").toLocalDateTime());
                liste.add(msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return liste;
    }

     */
//jdidddddddddddddddddddddddd
    @Override
    public List<Message> getMessagesParConversation(int conversationId) {
        List<Message> liste = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE conversation_id = ? ORDER BY sent_at ASC";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, conversationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Message msg = new Message();
                msg.setId(rs.getInt("id"));
                msg.setConversationId(rs.getInt("conversation_id"));
                msg.setSenderId(rs.getInt("sender_id"));
                msg.setContent(rs.getString("content"));
                msg.setAi(rs.getBoolean("is_ai"));
                msg.setRead(rs.getBoolean("is_read"));
                msg.setSentAt(rs.getTimestamp("sent_at").toLocalDateTime());
                msg.setFilePath(rs.getString("file_path"));   // ✅
                msg.setFileName(rs.getString("file_name"));   // ✅
                msg.setVocal(rs.getBoolean("is_vocal"));
                liste.add(msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return liste;
    }

    @Override
    public void marquerCommeLu(int conversationId, int userId) {
        String sql = "UPDATE messages SET is_read = true WHERE conversation_id = ? AND sender_id != ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, conversationId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void envoyerFichier(int conversationId, int senderId,
                               String filePath, String fileName) {
        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setSenderId(senderId);
        msg.setContent("📎 " + fileName);
        msg.setFilePath(filePath);
        msg.setFileName(fileName);
        msg.setAi(false);
        envoyerMessage(msg);
    }
}
