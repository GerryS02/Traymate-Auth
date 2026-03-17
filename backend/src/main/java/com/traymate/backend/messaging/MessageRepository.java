package com.traymate.backend.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface MessageRepository extends JpaRepository<Message, Long> {
    
    List<Message> findByReceiverId(Long receiverId);

    List<Message> findBySenderIdAndReceiverId(Long senderId, Long receiverId);
}
