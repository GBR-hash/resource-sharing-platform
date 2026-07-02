package com.example.resourcesharingplatform.service;

import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 故障注入服务
 * 支持延迟注入、异常注入、CPU 高负载注入
 */
@Service
public class ChaosService {

    @Value("${ops.token:ops-secret-token-2024}")
    @Getter
    private String opsToken;

    // 当前活跃的故障类型
    private volatile String activeChaosType = null;
    private volatile int chaosDurationSeconds = 0;
    private volatile long chaosStartTime = 0;

    // CPU 高负载线程
    private final ExecutorService cpuExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chaos-cpu-worker");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean cpuRunning = new AtomicBoolean(false);
    private ScheduledFuture<?> cpuStopFuture = null;

    /**
     * 触发故障
     */
    public Map<String, Object> triggerChaos(String type, int durationSeconds) {
        // 先清除之前的故障
        resetChaos();

        activeChaosType = type;
        chaosDurationSeconds = durationSeconds;
        chaosStartTime = System.currentTimeMillis();

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("type", type);
        info.put("durationSeconds", durationSeconds);
        info.put("startTime", new java.util.Date(chaosStartTime).toString());

        switch (type) {
            case "latency" -> info.put("description", "所有业务接口将随机延迟 3-10 秒");
            case "exception" -> info.put("description", "所有业务接口将随机抛出 RuntimeException");
            case "cpu_high" -> {
                info.put("description", "启动 CPU 高负载线程，持续 " + durationSeconds + " 秒");
                startCpuStress(durationSeconds);
            }
            default -> throw new IllegalArgumentException("不支持的故障类型: " + type + "，支持: latency, exception, cpu_high");
        }

        return info;
    }

    /**
     * 清除所有故障
     */
    public void resetChaos() {
        activeChaosType = null;
        chaosDurationSeconds = 0;
        chaosStartTime = 0;
        stopCpuStress();
    }

    /**
     * 获取当前故障状态
     */
    public Map<String, Object> getChaosStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        if (activeChaosType == null) {
            status.put("active", false);
            status.put("message", "当前无故障注入");
        } else {
            long elapsed = (System.currentTimeMillis() - chaosStartTime) / 1000;
            status.put("active", true);
            status.put("type", activeChaosType);
            status.put("elapsedSeconds", elapsed);
            status.put("remainingSeconds", Math.max(0, chaosDurationSeconds - elapsed));
            status.put("cpuStressRunning", cpuRunning.get());
        }
        return status;
    }

    /**
     * 判断是否应该注入延迟
     */
    public boolean shouldInjectLatency() {
        return "latency".equals(activeChaosType) && !isChaosExpired();
    }

    /**
     * 判断是否应该注入异常
     */
    public boolean shouldInjectException() {
        return "exception".equals(activeChaosType) && !isChaosExpired();
    }

    /**
     * 获取随机延迟毫秒数（3-10秒）
     */
    public long getRandomLatencyMs() {
        return 3000 + (long) (Math.random() * 7000);
    }

    private boolean isChaosExpired() {
        if (chaosStartTime == 0) return true;
        long elapsed = (System.currentTimeMillis() - chaosStartTime) / 1000;
        if (elapsed >= chaosDurationSeconds) {
            resetChaos();
            return true;
        }
        return false;
    }

    /**
     * 启动 CPU 压力线程
     */
    private void startCpuStress(int durationSeconds) {
        cpuRunning.set(true);
        cpuExecutor.submit(() -> {
            while (cpuRunning.get() && !Thread.currentThread().isInterrupted()) {
                // 死循环占用 CPU
                Math.random();
            }
        });

        // 定时停止
        cpuStopFuture = Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            cpuRunning.set(false);
            if ("cpu_high".equals(activeChaosType)) {
                resetChaos();
            }
        }, durationSeconds, TimeUnit.SECONDS);
    }

    /**
     * 停止 CPU 压力线程
     */
    private void stopCpuStress() {
        cpuRunning.set(false);
        if (cpuStopFuture != null) {
            cpuStopFuture.cancel(false);
            cpuStopFuture = null;
        }
        cpuExecutor.shutdownNow();
    }

    @PreDestroy
    public void shutdown() {
        resetChaos();
        cpuExecutor.shutdownNow();
    }
}
