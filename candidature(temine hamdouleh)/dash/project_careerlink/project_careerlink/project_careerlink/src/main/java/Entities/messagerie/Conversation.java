package Entities.messagerie;

import java.time.LocalDateTime;

public class Conversation {
    private int id;
    private int candidatId;
    private int managerId;
    private LocalDateTime createdAt;

    public Conversation() {}

    public Conversation(int candidatId, int managerId) {
        this.candidatId = candidatId;
        this.managerId = managerId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCandidatId() { return candidatId; }
    public void setCandidatId(int candidatId) { this.candidatId = candidatId; }

    public int getManagerId() { return managerId; }
    public void setManagerId(int managerId) { this.managerId = managerId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}