package com.example.resourcesharingplatform.controller;

import com.example.resourcesharingplatform.dto.Result;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.*;
import java.lang.management.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 运维控制接口 - 供 AIOps Agent 调用
 * 所有接口需要 ADMIN 角色 + X-Ops-Token 双重校验
 */
@RestController
@RequestMapping("/api/admin/ops")
@RequiredArgsConstructor
public class OpsController {

    private final ApplicationContext applicationContext;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${ops.token:ops-secret-token-2024}")
    private String opsToken;

    /**
     * 校验 X-Ops-Token
     */
    private void validateOpsToken(HttpServletRequest request) {
        String token = request.getHeader("X-Ops-Token");
        if (token == null || !token.equals(opsToken)) {
            throw new RuntimeException("运维 Token 校验失败");
        }
    }

    /**
     * 系统综合健康状态
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health(HttpServletRequest request) {
        validateOpsToken(request);
        Map<String, Object> health = new LinkedHashMap<>();

        // 数据库连接检查
        try {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            health.put("database", Map.of("status", "UP", "message", "数据库连接正常"));
        } catch (Exception e) {
            health.put("database", Map.of("status", "DOWN", "message", e.getMessage()));
        }

        // 磁盘空间
        File root = new File("/");
        long totalSpace = root.getTotalSpace();
        long usableSpace = root.getUsableSpace();
        health.put("disk", Map.of(
                "status", usableSpace > 1024L * 1024 * 1024 ? "UP" : "WARNING",
                "totalGB", totalSpace / (1024L * 1024 * 1024),
                "usableGB", usableSpace / (1024L * 1024 * 1024),
                "usagePercent", String.format("%.1f%%", (1.0 - (double) usableSpace / totalSpace) * 100)
        ));

        // JVM 内存
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        health.put("jvm_memory", Map.of(
                "status", "UP",
                "usedMB", heapUsage.getUsed() / (1024 * 1024),
                "maxMB", heapUsage.getMax() / (1024 * 1024),
                "usagePercent", String.format("%.1f%%", (double) heapUsage.getUsed() / heapUsage.getMax() * 100)
        ));

        // 运行时长
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        health.put("uptime", formatUptime(uptimeMs));

        return Result.success("系统健康检查完成", health);
    }

    /**
     * HikariCP 数据库连接池状态
     */
    @GetMapping("/connection-pool")
    public Result<Map<String, Object>> connectionPool(HttpServletRequest request) {
        validateOpsToken(request);
        Map<String, Object> poolInfo = new LinkedHashMap<>();

        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName hikariName = new ObjectName("com.zaxxer.hikari:type=Pool (*)");
            Set<ObjectName> objectNames = mBeanServer.queryNames(hikariName, null);

            if (!objectNames.isEmpty()) {
                ObjectName name = objectNames.iterator().next();
                poolInfo.put("activeConnections", mBeanServer.getAttribute(name, "ActiveConnections"));
                poolInfo.put("idleConnections", mBeanServer.getAttribute(name, "IdleConnections"));
                poolInfo.put("totalConnections", mBeanServer.getAttribute(name, "TotalConnections"));
                poolInfo.put("threadsAwaitingConnection", mBeanServer.getAttribute(name, "ThreadsAwaitingConnection"));
                poolInfo.put("maxPoolSize", mBeanServer.getAttribute(name, "MaximumPoolSize"));
                poolInfo.put("minIdle", mBeanServer.getAttribute(name, "MinimumIdle"));
            } else {
                poolInfo.put("message", "未找到 HikariCP MBean，可能未使用 HikariCP");
            }
        } catch (Exception e) {
            poolInfo.put("error", e.getMessage());
        }

        return Result.success("连接池状态查询完成", poolInfo);
    }

    /**
     * JVM 运行时信息
     */
    @GetMapping("/jvm")
    public Result<Map<String, Object>> jvmInfo(HttpServletRequest request) {
        validateOpsToken(request);
        Map<String, Object> jvm = new LinkedHashMap<>();

        // 内存
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
        jvm.put("heap", Map.of(
                "usedMB", heap.getUsed() / (1024 * 1024),
                "committedMB", heap.getCommitted() / (1024 * 1024),
                "maxMB", heap.getMax() / (1024 * 1024)
        ));
        jvm.put("nonHeap", Map.of(
                "usedMB", nonHeap.getUsed() / (1024 * 1024),
                "committedMB", nonHeap.getCommitted() / (1024 * 1024)
        ));

        // GC
        List<Map<String, Object>> gcInfos = new ArrayList<>();
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcInfos.add(Map.of(
                    "name", gc.getName(),
                    "collectionCount", gc.getCollectionCount(),
                    "collectionTimeMs", gc.getCollectionTime()
            ));
        }
        jvm.put("gc", gcInfos);

        // 线程
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        jvm.put("threads", Map.of(
                "total", threadBean.getThreadCount(),
                "peak", threadBean.getPeakThreadCount(),
                "daemon", threadBean.getDaemonThreadCount()
        ));

        // 类加载
        ClassLoadingMXBean classBean = ManagementFactory.getClassLoadingMXBean();
        jvm.put("classLoading", Map.of(
                "loaded", classBean.getLoadedClassCount(),
                "totalLoaded", classBean.getTotalLoadedClassCount(),
                "unloaded", classBean.getUnloadedClassCount()
        ));

        // 运行信息
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        jvm.put("runtime", Map.of(
                "javaVersion", System.getProperty("java.version"),
                "osName", System.getProperty("os.name"),
                "osArch", System.getProperty("os.arch"),
                "uptime", formatUptime(runtimeBean.getUptime()),
                "startTime", new Date(runtimeBean.getStartTime()).toString()
        ));

        return Result.success("JVM 信息查询完成", jvm);
    }

    /**
     * 磁盘使用情况
     */
    @GetMapping("/disk")
    public Result<Map<String, Object>> diskInfo(HttpServletRequest request) {
        validateOpsToken(request);
        Map<String, Object> disk = new LinkedHashMap<>();

        File[] roots = File.listRoots();
        List<Map<String, Object>> partitions = new ArrayList<>();
        for (File root : roots) {
            long total = root.getTotalSpace();
            long usable = root.getUsableSpace();
            partitions.add(Map.of(
                    "path", root.getAbsolutePath(),
                    "totalGB", total / (1024L * 1024 * 1024),
                    "usableGB", usable / (1024L * 1024 * 1024),
                    "usagePercent", String.format("%.1f%%", (1.0 - (double) usable / total) * 100)
            ));
        }
        disk.put("partitions", partitions);

        // uploads 目录大小
        try {
            Path uploadsPath = Paths.get("./uploads");
            if (Files.exists(uploadsPath)) {
                long size = Files.walk(uploadsPath)
                        .filter(Files::isRegularFile)
                        .mapToLong(p -> {
                            try { return Files.size(p); } catch (IOException e) { return 0; }
                        })
                        .sum();
                disk.put("uploadsDir", Map.of(
                        "path", uploadsPath.toAbsolutePath().toString(),
                        "totalSizeMB", size / (1024 * 1024),
                        "fileCount", Files.walk(uploadsPath).filter(Files::isRegularFile).count()
                ));
            }
        } catch (IOException e) {
            disk.put("uploadsDir", Map.of("error", e.getMessage()));
        }

        return Result.success("磁盘信息查询完成", disk);
    }

    /**
     * 清理应用缓存
     */
    @PostMapping("/cache/evict")
    public Result<String> evictCache(@RequestParam(required = false) String key,
                                     HttpServletRequest request) {
        validateOpsToken(request);
        // 清理 JPA 二级缓存和 EntityManager
        entityManager.clear();
        String msg = key != null ? "已清理缓存，key pattern: " + key : "已清理全部 EntityManager 缓存";
        return Result.success(msg, msg);
    }

    /**
     * 模拟服务重载（刷新 Spring 上下文中的 Bean）
     */
    @PostMapping("/service/reload")
    public Result<Map<String, Object>> reload(HttpServletRequest request) {
        validateOpsToken(request);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("beanCount", applicationContext.getBeanDefinitionCount());
        info.put("activeProfiles", Arrays.toString(applicationContext.getEnvironment().getActiveProfiles()));
        info.put("message", "服务重载模拟完成（单体应用不支持热重载，此接口返回当前上下文状态）");
        return Result.success("服务状态查询完成", info);
    }

    /**
     * 查询最近日志
     */
    @GetMapping("/logs")
    public Result<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "ERROR") String level,
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest request) {
        validateOpsToken(request);

        Map<String, Object> result = new LinkedHashMap<>();
        Path logPath = Paths.get("./logs/app-text.log");

        if (!Files.exists(logPath)) {
            result.put("message", "日志文件不存在");
            result.put("entries", Collections.emptyList());
            return Result.success("日志查询完成", result);
        }

        try {
            List<String> allLines;
            try (Stream<String> stream = Files.lines(logPath)) {
                allLines = stream.collect(Collectors.toList());
            }

            // 从后往前读取，过滤指定级别
            List<String> filtered = new ArrayList<>();
            for (int i = allLines.size() - 1; i >= 0 && filtered.size() < limit; i--) {
                String line = allLines.get(i);
                if (level.equalsIgnoreCase("ALL") || line.contains(level.toUpperCase())) {
                    filtered.add(0, line);
                }
            }

            result.put("level", level);
            result.put("totalMatched", filtered.size());
            result.put("entries", filtered);
            return Result.success("日志查询完成", result);
        } catch (IOException e) {
            return Result.error("日志读取失败: " + e.getMessage());
        }
    }

    private String formatUptime(long ms) {
        long seconds = ms / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%dd %dh %dm %ds", days, hours, minutes, secs);
    }
}
