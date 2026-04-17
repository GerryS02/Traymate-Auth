package com.traymate.backend.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;


public interface MessageRepository extends JpaRepository<Message, Long> {
    
    List<Message> findByReceiverId(Long receiverId);

    //List<Message> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(Long senderId, Long receiverId);

    // List<Message> findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByCreatedAtAsc(
    //     Long sender1, Long receiver1,
    //     Long sender2, Long receiver2
    // );

    //get conversation
    @Query("""
        SELECT m FROM Message m
        WHERE (m.senderId = :userId AND m.receiverId = :otherUserId)
        OR (m.senderId = :otherUserId AND m.receiverId = :userId)
        ORDER BY m.createdAt ASC
    """)
    List<Message> getConversation(Long userId, Long otherUserId);

    //delete one specific message
    void deleteById(Long id);

    //delete the entire conversation
    // void deleteBySenderIdAndReceiverIdOrReceiverIdAndSenderId(
    //     Long senderId1, Long receiverId1,
    //     Long senderId2, Long receiverId2
    // );
    @Transactional
    @Modifying
    @Query("""
        DELETE FROM Message m
        WHERE (m.senderId = :userId AND m.receiverId = :otherUserId)
        OR (m.senderId = :otherUserId AND m.receiverId = :userId)
    """)
    void deleteConversation(Long userId, Long otherUserId);


}
