package com.collabit.project.controller;

import com.collabit.global.security.SecurityUtil;
import com.collabit.project.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RequestMapping("/api/sse")
@RequiredArgsConstructor
@RestController
public class SseController {

    private final SseEmitterService sseEmitterService;

    // 클라이언트의 SSE 연결 요청을 처리하는 엔드포인트 (로그인 시 요청 필요)
    @GetMapping("/subscribe")
    public SseEmitter subscribe() {
        String userCode = SecurityUtil.getCurrentUserCode();
        return sseEmitterService.subscribe(userCode);
    }

}
