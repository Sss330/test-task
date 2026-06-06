package test.task.bfg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import test.task.bfg.model.entity.Message;
import test.task.bfg.model.enums.MessageStatus;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
            select m from Message m
            where (m.sender.id = :firstUserId and m.receiver.id = :secondUserId)
               or (m.sender.id = :secondUserId and m.receiver.id = :firstUserId)
            order by m.createdAt asc
            """)
    List<Message> findConversation(UUID firstUserId, UUID secondUserId);

    List<Message> findByReceiver_IdAndStatus(UUID receiverId, MessageStatus status);
}