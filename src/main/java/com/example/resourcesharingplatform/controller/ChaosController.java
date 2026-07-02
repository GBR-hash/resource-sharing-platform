package com.example.resourcesharingplatform.controller;

import com.example.resourcesharingplatform.dto.Result;
import com.example.resourcesharingplatform.service.ChaosService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 故障注入控制器 - 供 AIOps Agent 测试排查能力
 * 所有接口需要 ADMIN 角色 + X-Ops-Token 双重校验
 */
@RestController
@RequestMapping("/api/admin/chaos")
@RequiredArgsConstructor
public class ChaosController {

    private final ChaosService chaosService;

    /**
     * 触发故障
     * @param type 故障类型：latency / exception / cpu_high
     * @param durationSeconds 持续时间（秒），默认 60
     */
    @PostMapping("/trigger")
    public Result<Map<String, Object>> trigger(
            @RequestParam String type,
            @RequestParam(defaultValue = "60") int durationSeconds,
            HttpServletRequest request) {

        String token = request.getHeader("X-Ops-Token");
        if (token == null || !token.equals(chaosService.getOpsToken())) {
            return Result.forbidden("运维 Token 校验失败");
        }

        Map<String, Object> info = chaosService.triggerChaos(type, durationSeconds);
        return Result.success("故障注入已触发", info);
    }

    /**
     * 清除所有故障注入
     */
    @PostMapping("/reset")
    public Result<String> reset(HttpServletRequest request) {
        String token = request.getHeader("X-Ops-Token");
        if (token == null || !token.equals(chaosService.getOpsToken())) {
            return Result.forbidden("运维 Token 校验失败");
        }

        chaosService.resetChaos();
        return Result.success("所有故障注入已清除", "ok");
    }

    /**
     * 查看当前故障注入状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> status(HttpServletRequest request) {
        String token = request.getHeader("X-Ops-Token");
        if (token == null || !token.equals(chaosService.getOpsToken())) {
            return Result.forbidden("运维 Token 校验失败");
        }

        return Result.success("故障状态查询完成", chaosService.getChaosStatus());
    }
}
