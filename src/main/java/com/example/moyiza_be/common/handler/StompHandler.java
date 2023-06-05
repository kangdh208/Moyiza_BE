package com.example.moyiza_be.common.handler;


import com.example.moyiza_be.chat.dto.ChatMessageOutput;
import com.example.moyiza_be.chat.dto.ChatUserPrincipal;
import com.example.moyiza_be.chat.entity.ChatJoinEntry;
import com.example.moyiza_be.chat.repository.ChatJoinEntryRepository;
import com.example.moyiza_be.common.redis.RedisCacheService;
import com.example.moyiza_be.common.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class StompHandler implements ChannelInterceptor {
    private final JwtUtil jwtUtil;
    private final RedisCacheService redisCacheService;
    private final ChatJoinEntryRepository chatJoinEntryRepository;
//    private final SimpMessageSendingOperations sendingOperations;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        //
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(message);
        String sessionId = headerAccessor.getSessionId();
        System.out.println("headerAccessor.getCommand() = " + headerAccessor.getCommand());
        if(StompCommand.CONNECT.equals(headerAccessor.getCommand())){
            String bearerToken = headerAccessor.getFirstNativeHeader("ACCESS_TOKEN");
            String token = jwtUtil.removePrefix(bearerToken);
            if(!jwtUtil.validateToken(token)){
                throw new IllegalArgumentException("토큰이 유효하지 않습니다");
            }
            Claims claims = jwtUtil.getClaimsFromToken(token);
            ChatUserPrincipal userInfo;
            try{
//                Long subscribedChatId = getChatIdFromDestination(headerAccessor.getDestination());
                userInfo = new ChatUserPrincipal(
                        Long.valueOf(claims.get("userId").toString()),
                        claims.get("nickName").toString(),
                        claims.get("profileUrl").toString(),
                        -1L
                );
                System.out.println("userInfo = " + userInfo.getUserNickname());
                System.out.println("userInfo.getSubscribedChatId() = " + userInfo.getSubscribedChatId());
                System.out.println("userInfo.getUserId() = " + userInfo.getUserId());
                System.out.println("userInfo.getProfileUrl() = " + userInfo.getProfileUrl());
            } catch(RuntimeException e){
                log.info("채팅 : 토큰에서 유저정보를 가져올 수 없음");
                throw new NullPointerException("chat : 유저정보를 읽을 수 없습니다");
            }
            redisCacheService.saveUserInfoToCache(sessionId, userInfo);
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(headerAccessor.getCommand())
        ) {
            ChatUserPrincipal userPrincipal = redisCacheService.getUserInfoFromCache(sessionId);
            log.info("SUBSCRIBE : loaded userPrincipal : " + userPrincipal.toString());
            String destination = headerAccessor.getDestination();
            Long chatId = getChatIdFromDestination(destination);
            log.info("SUBSCRIBE : destinationChatId : " + chatId);
            userPrincipal.setSubscribedChatId(chatId);
            redisCacheService.saveUserInfoToCache(sessionId, userPrincipal);
//            redisCacheService.addSubscriptionToChatId(chatId.toString(), sessionId);
//            ChatJoinEntry chatJoinEntry =
//                    chatJoinEntryRepository.findByUserIdAndChatIdAndIsCurrentlyJoinedTrue(chatId, userPrincipal.getUserId())
//                                    .orElseThrow(() -> new NullPointerException("참여중인 채팅방이 아닙니다"));
            return message;
        }

//        if(StompCommand.UNSUBSCRIBE.equals(headerAccessor.getCommand())){
//            ChatUserPrincipal userPrincipal = redisCacheService.getUserInfoFromCache(sessionId);
//            log.info("UNSUBSCRIBE COMMAND FROM USER " + userPrincipal.getUserId());
//            unsubscribe(userPrincipal, sessionId);
//        }

//        if(StompCommand.DISCONNECT.equals(headerAccessor.getCommand())){
//            ChatUserPrincipal userPrincipal = redisCacheService.getUserInfoFromCache(sessionId);
//            log.info("DISCONNECT SESSIONID : " + sessionId);
//            if(userPrincipal.getSubscribedChatId().equals(-1L)){
//                log.info("User is not subscribed to any chat .. continue DISCONNECT");
//            }
//            else{
//                unsubscribe(userPrincipal,sessionId);
//            }
//            redisCacheService.deleteUserInfoFromCache(sessionId);
//        }

        return message;
    }

    private Long getChatIdFromDestination(String destination){
        if(destination == null){
            throw new NullPointerException("subscribe destination not defined");
        }
        return Long.valueOf(destination.replaceAll("\\D", ""));
    }
    public void unsubscribe(ChatUserPrincipal userPrincipal, String sessionId){
        log.info(userPrincipal.getUserId() + " unsubscribing chatroom " + userPrincipal.getSubscribedChatId());
        Long chatId = userPrincipal.getSubscribedChatId();
        ChatJoinEntry chatJoinEntry =
                chatJoinEntryRepository.findByUserIdAndChatIdAndIsCurrentlyJoinedTrue
                                (userPrincipal.getUserId(), userPrincipal.getSubscribedChatId())
                        .orElseThrow( () -> new NullPointerException("채팅방의 유저정보를 찾을 수 없습니다"));

        ChatMessageOutput recentMessage = redisCacheService.loadRecentChat(chatId.toString());
        if(recentMessage == null){
            log.info("recent message not present for chatId : " + chatId);
        }
        else{
            chatJoinEntry.setLastReadMessageId(recentMessage.getChatRecordId());
        }

        chatJoinEntryRepository.save(chatJoinEntry);
        redisCacheService.removeSubscriptionFromChatId(chatId.toString(), sessionId);
        userPrincipal.setSubscribedChatId(-1L);
        redisCacheService.saveUserInfoToCache(sessionId,userPrincipal);
    }

}
