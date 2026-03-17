package com.traymate.backend.messaging;

import java.time.LocalDateTime;
import java.util.List;

import com.traymate.backend.messaging.dto.MessageResponse;
import com.traymate.backend.messaging.dto.SendMessageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository repository;

    public MessageResponse sendMessage(Long senderId, SendMessageRequest req){

        Message message = Message.builder()
                    .senderId(senderId)
                    .receiverId(req.getReceiverId())
                    .content(req.getContent())
                    .createdAt(LocalDateTime.now())
                    .isRead(false)
                    .build();

        Message saved = repository.save(message);

        return MessageResponse.builder()
                .id(saved.getId())
                .senderId(saved.getSenderId())
                .receiverId(saved.getReceiverId())
                .content(saved.getContent())
                .createdAt(saved.getCreatedAt())
                .isRead(saved.getIsRead())
                .build();
    }

    public List<Message> getInbox(Long receiverId){
        return repository.findByReceiverId(receiverId);
    } 

    public List<Message> getConversation(Long senderId, Long receiverId){
        return repository.findBySenderIdAndReceiverId(senderId, receiverId);
    }

}
