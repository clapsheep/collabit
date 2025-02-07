package com.collabit.chat.service;

import com.collabit.chat.domain.dto.ChatMessageRequestDTO;
import com.collabit.chat.domain.dto.ChatMessageResponseDTO;
import com.collabit.chat.domain.dto.ChatRoomDetailResponseDTO;
import com.collabit.chat.domain.entity.ChatMessage;
import com.collabit.chat.domain.entity.ChatRoom;
import com.collabit.chat.exception.ChatRoomNotFoundException;
import com.collabit.chat.exception.UserNotInChatRoomException;
import com.collabit.chat.repository.ChatMessageRepository;
import com.collabit.chat.repository.ChatRoomRepository;
import com.collabit.global.common.PageResponseDTO;
import com.collabit.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomDetailService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRedisService chatRedisService;

    // 채팅방 디테일 조회
    public ChatRoomDetailResponseDTO getChatRoomDetail(String userCode, int roomCode) {
        if (!isUserInChatRoom(userCode, roomCode)) {
            log.debug("User {} is not in chat room", userCode);
            throw new UserNotInChatRoomException();
        }
        ChatRoom chatRoom = chatRoomRepository.findById(roomCode).orElseThrow(ChatRoomNotFoundException::new);
        User otherUser = getOtherUser(chatRoom, userCode);
        ChatRoomDetailResponseDTO chatRoomDetail = ChatRoomDetailResponseDTO.builder()
                .profileImage(otherUser.getProfileImage())
                .nickname(otherUser.getNickname())
                .roomCode(roomCode)
                .build();
        log.debug("ChatRoomDetail {}", chatRoomDetail);
        return chatRoomDetail;
    }

    //채팅방 메시지 조회
    public PageResponseDTO<ChatMessageResponseDTO> getChatRoomMessages(String userCode, int roomCode, int pageNumber) {
        if (!isUserInChatRoom(userCode, roomCode)) {
            log.debug("User {} is not in chat room", userCode);
            throw new UserNotInChatRoomException();
        }

        Pageable pageable = PageRequest.of(pageNumber, 50);
        Page<ChatMessage> chatMessagePage = chatMessageRepository.findByRoomCodeOrderByTimestampDesc(roomCode, pageable);
        log.debug("ChatMessagePage {}", chatMessagePage.toString());

        PageResponseDTO<ChatMessageResponseDTO> chatMessages = PageResponseDTO.<ChatMessageResponseDTO>builder()
                .content(chatMessagePage.getContent().stream()
                        .map(this::convertToResponseDTO)
                        .toList())
                .pageNumber(pageNumber)
                .pageSize(50)
                .totalElements((int) chatMessagePage.getTotalElements())
                .totalPages(chatMessagePage.getTotalPages())
                .last(chatMessagePage.isLast())
                .hasNext(chatMessagePage.hasNext())
                .build();

        log.debug("ChatMessages {}", chatMessages);
        return chatMessages;
    }

    public void markMessagesAsRead(int roomCode, String userCode) {
        chatRedisService.updateRoomMessageStatus(roomCode, userCode, true);
    }

    public void saveMessage(ChatMessageRequestDTO chatMessageRequestDTO, String userCode, int roomCode) {
        ChatMessage chatMessage = ChatMessage.builder()
                .roomCode(roomCode)
                .userCode(userCode)
                .message(chatMessageRequestDTO.getMessage())
                .timestamp(LocalDateTime.now())
                .isRead(false)
                .build();

        log.debug("ChatMessage saving... {}", chatMessage);
        chatMessageRepository.save(chatMessage);
        log.info("메시지 저장 완료: Room {}, Message {}", roomCode, chatMessageRequestDTO.getMessage());
    }

    private boolean isUserInChatRoom(String userCode, int roomCode) {
        return chatRoomRepository.findById(roomCode)
                .map(chatRoom -> chatRoom.getUser1().getCode().equals(userCode) || chatRoom.getUser2().getCode().equals(userCode))
                .orElse(false);
    }

    private ChatMessageResponseDTO convertToResponseDTO(ChatMessage chatMessage) {
        return ChatMessageResponseDTO.builder()
                .nickname(getUserByUserCode(chatMessage.getUserCode(), chatMessage.getRoomCode()).getNickname())
                .message(chatMessage.getMessage())
                .timestamp(chatMessage.getTimestamp())
                .build();
    }

    private User getUserByUserCode(String userCode, int roomCode) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomCode)
                .orElseThrow(ChatRoomNotFoundException::new);
        return chatRoom.getUser1().getCode().equals(userCode) ? chatRoom.getUser2() : chatRoom.getUser1();
    }

    private User getOtherUser(ChatRoom chatRoom, String userCode) {
        return chatRoom.getUser1().getCode().equals(userCode) ? chatRoom.getUser2() : chatRoom.getUser1();
    }
}
