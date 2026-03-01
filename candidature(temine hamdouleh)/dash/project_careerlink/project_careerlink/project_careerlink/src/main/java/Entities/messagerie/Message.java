package Entities.messagerie;

import java.time.LocalDateTime;

public class Message {
    private int id;
    private int conversationId;
    private int senderId;
    private String content;
    private boolean isAi;
    private boolean isRead;
    private LocalDateTime sentAt;

    public Message() {}

    public Message(int conversationId, int senderId, String content, boolean isAi) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.isAi = isAi;
    }

    private boolean isVocal;

    public boolean isVocal() { return isVocal; }
    public void setVocal(boolean vocal) { isVocal = vocal; }

    private String filePath;
    private String fileName;

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getConversationId() { return conversationId; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isAi() { return isAi; }
    public void setAi(boolean ai) { isAi = ai; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}