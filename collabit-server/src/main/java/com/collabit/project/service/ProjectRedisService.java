package com.collabit.project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProjectRedisService {

    private static final String NEW_SURVEY_RESPONSE_KEY_PREFIX = "newSurveyResponse::";
    private final RedisTemplate<String, Object> redisTemplate;

    // 특정 userCode에 대한 모든 newSurveyResponse 키-값 쌍을 조회
    public Map<Integer, Boolean> findNewSurveyResponsesByUserCode(String userCode) {
        log.debug("해당 유저의 모든 프로젝트 조회 시작");
        try {
            // newSurveyResponse::{userCode}::* 패턴으로 Redis에서 해당 유저와 관련된 모든 키 조회
            String pattern = NEW_SURVEY_RESPONSE_KEY_PREFIX + userCode + "::*";
            Set<String> keys = redisTemplate.keys(pattern);

            if (keys == null || keys.isEmpty()) {
                return new HashMap<>();
            }

            // projectInfoCode를 key로, true를 value로 하는 Map 생성
            Map<Integer, Boolean> projectInfoCodeMap = new HashMap<>();

            for (String key : keys) {
                String[] keyParts = key.split("::");
                if (keyParts.length == 3) {
                    try {
                        // projectInfoCode 추출 후 Map(key 기준 중복x)에 저장
                        int projectInfoCode = Integer.parseInt(keyParts[2]);

                        // 해당 projectInfoCode는 읽지 않은 알림이 있는 것이므로 true
                        projectInfoCodeMap.put(projectInfoCode, true);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid projectInfoCode in Redis key: {}", key);
                        throw new RuntimeException("프로젝트 정보 코드가 올바르지 않습니다: ");
                    }
                }
            }
            return projectInfoCodeMap;
        } catch (Exception e) {
            log.error("Redis에서 newSurveyResponse 조회 중 오류 발생", e);
            return new HashMap<>();
        }
    }

    // userCode에 해당하는 모든 newSurveyResponse 삭제
    public Map<Integer, Object> removeAllNotificationByUserCode(String userCode) {
        log.debug("해당 유저의 모든 프로젝트 알림 삭제 시작");

        try {
            // 해당 유저의 모든 알림 키 조회
            String pattern = NEW_SURVEY_RESPONSE_KEY_PREFIX + userCode + "::*";
            Set<String> keys = redisTemplate.keys(pattern);

            if (keys == null || keys.isEmpty()) {
                return new HashMap<>();
            }

            // projectInfoCode별 value를 저장할 Map (참여인원 업데이트를 위해)
            Map<Integer, Object> projectInfoValues = new HashMap<>();

            for (String key : keys) {
                Object value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    String[] keyParts = key.split("::");
                    if (keyParts.length == 3) {
                        int projectInfoCode = Integer.parseInt(keyParts[2]);
                        projectInfoValues.put(projectInfoCode, value);
                    }
                }
            }

            // 키들 일괄 삭제
            redisTemplate.delete(keys);

            log.debug("삭제된 알림 수: {}", projectInfoValues.size());
            return projectInfoValues;

        } catch (Exception e) {
            log.error("Redis에서 알림 삭제 중 오류 발생", e);
            return new HashMap<>();
        }
    }

    // 특정 프로젝트의 알림만 삭제
    public Object removeNotificationByUserCodeAndProjectCode(String userCode, int projectInfoCode) {
        log.debug("특정 프로젝트의 알림 삭제 시작 - userCode: {}, projectInfoCode: {}", userCode, projectInfoCode);

        try {
            // 특정 프로젝트의 알림 키 조회
            String key = NEW_SURVEY_RESPONSE_KEY_PREFIX + userCode + "::" + projectInfoCode;

            // 삭제 전에 값을 먼저 가져옴
            Object value = redisTemplate.opsForValue().get(key);

            if (value != null) {
                redisTemplate.delete(key);
                log.debug("프로젝트 알림 삭제 완료 - key: {}, value: {}", key, value);
                return value;
            }

            return null;
        } catch (Exception e) {
            log.error("Redis에서 프로젝트 알림 삭제 중 오류 발생", e);
            return null;
        }
    }
}
