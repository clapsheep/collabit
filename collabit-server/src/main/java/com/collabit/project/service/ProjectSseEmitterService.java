package com.collabit.project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@Configuration
@RequiredArgsConstructor
public class ProjectSseEmitterService {

    private final ConcurrentHashMap<String, SseEmitter> sseEmitters;
    private final ProjectRedisService projectRedisService;

    // targetUser에게 새로운 설문 응답이 왔음을 SSE로 전송
    public void sendNewSurveyResponse(String userCode, int projectInfoCode) {
        SseEmitter emitter = sseEmitters.get(userCode);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("newSurveyResponse")
                        .data(projectInfoCode));
            } catch (IOException e) {
                emitter.complete();
                sseEmitters.remove(userCode);
            }
        }
    }

    // 해당 유저에게 설문 요청이 있는 projectInfoCode SSE로 전송
    public void sendNewSurveyRequest(String userCode, List<Integer> projectInfoCodes) {
        SseEmitter emitter = sseEmitters.get(userCode);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("newSurveyRequest")
                        .data(projectInfoCodes));
            } catch (IOException e) {
                emitter.complete();
                sseEmitters.remove(userCode);
            }
        }
    }

    // 해당 유저에게 요청된 설문 알림 리스트, 신규 응답이 있는 알림 리스트
    public void sendHeaderNotification(String userCode) {
        SseEmitter emitter = sseEmitters.get(userCode);

        if(emitter == null) {
            log.warn("해당 유저의 SSE emitter를 찾을 수 없음: {}", userCode);
            return;
        }

        try {
            List<Integer> newSurveyRequestList = projectRedisService.findAllNewSurveyRequest(userCode);
            List<Integer> newSurveyResponseList = projectRedisService.findAllNewSurveyResponse(userCode);

            sendEventSafely(emitter, "newSurveyRequest", newSurveyRequestList, userCode);
            sendEventSafely(emitter, "newSurveyResponse", newSurveyResponseList, userCode);
        } catch (Exception e) {
            log.error("{} 유저에게 이벤트 전송 실패", userCode, e);
            emitter.complete();
            sseEmitters.remove(userCode);
        }
    }

    private void sendEventSafely(SseEmitter emitter, String eventName, Object data, String userCode) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            log.error("{} 유저에게 {} 이벤트 전송 실패", eventName, userCode, e);
            emitter.complete();
            sseEmitters.remove(userCode);
        }
    }
}
