package com.example.resourcesharingplatform.aspect;

import com.example.resourcesharingplatform.service.ChaosService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 故障注入 AOP 切面
 * 拦截所有 Controller 方法，根据 ChaosService 的状态注入延迟或异常
 */
@Aspect
@Component
@RequiredArgsConstructor
public class ChaosAspect {

    private static final Logger log = LoggerFactory.getLogger(ChaosAspect.class);

    private final ChaosService chaosService;

    /**
     * 拦截所有 Controller 方法（排除运维和故障注入接口本身）
     */
    @Around("execution(* com.example.resourcesharingplatform.controller.ResourceController.*(..)) " +
            "|| execution(* com.example.resourcesharingplatform.controller.AuthController.*(..)) " +
            "|| execution(* com.example.resourcesharingplatform.controller.UserController.*(..)) " +
            "|| execution(* com.example.resourcesharingplatform.controller.FavoriteController.*(..)) " +
            "|| execution(* com.example.resourcesharingplatform.controller.PublicController.*(..))")
    public Object injectChaos(ProceedingJoinPoint joinPoint) throws Throwable {
        // 注入延迟
        if (chaosService.shouldInjectLatency()) {
            long latencyMs = chaosService.getRandomLatencyMs();
            log.warn("[Chaos] 注入延迟 {}ms 到方法: {}", latencyMs, joinPoint.getSignature().getName());
            Thread.sleep(latencyMs);
        }

        // 注入异常
        if (chaosService.shouldInjectException()) {
            String msg = String.format("[Chaos] 注入异常到方法: %s", joinPoint.getSignature().getName());
            log.warn(msg);
            throw new RuntimeException(msg);
        }

        return joinPoint.proceed();
    }
}
