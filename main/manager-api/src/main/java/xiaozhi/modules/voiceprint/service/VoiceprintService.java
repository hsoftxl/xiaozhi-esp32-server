package com.example.esp32server.service;

import com.example.esp32server.model.Voiceprint;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class VoiceprintService {

    private static final Logger logger = LoggerFactory.getLogger(VoiceprintService.class);
    private final Map<String, List<Voiceprint>> voiceprintsByAgent = new ConcurrentHashMap<>();
    
    // 可配置的上传目录
    private static final String DEFAULT_UPLOAD_DIR = "/data/voiceprints";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_AUDIO_TYPES = Set.of(
            "audio/wav", "audio/mp3", "audio/mpeg", "audio/ogg", 
            "audio/flac", "audio/aac", "audio/m4a"
    );

    /**
     * 获取指定设备的声纹列表
     */
    public List<Voiceprint> getVoiceprintsByAgentId(String agentId) {
        logger.debug("获取设备 {} 的声纹列表", agentId);
        return voiceprintsByAgent.getOrDefault(agentId, new ArrayList<>());
    }

    /**
     * 获取所有声纹列表
     */
    public List<Voiceprint> getAllVoiceprints() {
        logger.debug("获取所有声纹列表");
        return voiceprintsByAgent.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * 添加新的声纹
     */
    public Voiceprint addVoiceprint(String agentId, Voiceprint voiceprint, MultipartFile file) throws IOException {
        logger.info("为设备 {} 添加声纹: {}", agentId, voiceprint.getName());
        
        // 验证文件
        validateFile(file);
        
        // 确保上传目录存在
        String uploadDir = getUploadDirectory(agentId);
        createDirectoryIfNotExists(uploadDir);
        
        // 生成唯一文件名
        String fileName = generateUniqueFileName(file.getOriginalFilename());
        String filePath = uploadDir + File.separator + fileName;
        
        try {
            // 保存文件到磁盘
            file.transferTo(new File(filePath));
            logger.info("文件保存成功: {}", filePath);
            
            // 设置声纹属性
            voiceprint.setFilePath(filePath);
            voiceprint.setCreatedTime(LocalDateTime.now());
            voiceprint.setFileSize(file.getSize());
            voiceprint.setOriginalFileName(file.getOriginalFilename());
            
            // 存储到内存Map
            voiceprintsByAgent.computeIfAbsent(agentId, k -> new ArrayList<>()).add(voiceprint);
            
            logger.info("声纹添加成功: {} (ID: {})", voiceprint.getName(), voiceprint.getId());
            return voiceprint;
        } catch (IOException e) {
            logger.error("保存声纹文件失败: {}", e.getMessage(), e);
            // 如果保存失败，尝试删除已创建的文件
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (IOException deleteException) {
                logger.warn("删除失败的文件时出错: {}", deleteException.getMessage());
            }
            throw e;
        }
    }

    /**
     * 删除声纹
     */
    public boolean deleteVoiceprint(String agentId, String voiceprintId) {
        logger.info("删除设备 {} 的声纹: {}", agentId, voiceprintId);
        
        List<Voiceprint> voiceprints = voiceprintsByAgent.get(agentId);
        if (voiceprints == null) {
            logger.warn("设备 {} 没有声纹数据", agentId);
            return false;
        }
        
        // 查找要删除的声纹
        Optional<Voiceprint> voiceprintToDelete = voiceprints.stream()
                .filter(v -> v.getId().equals(voiceprintId))
                .findFirst();
        
        if (voiceprintToDelete.isPresent()) {
            Voiceprint voiceprint = voiceprintToDelete.get();
            
            // 从列表中移除
            boolean removed = voiceprints.removeIf(v -> v.getId().equals(voiceprintId));
            
            if (removed) {
                // 删除物理文件
                deletePhysicalFile(voiceprint.getFilePath());
                logger.info("声纹删除成功: {} ({})", voiceprint.getName(), voiceprintId);
                return true;
            }
        }
        
        logger.warn("未找到要删除的声纹: {}", voiceprintId);
        return false;
    }

    /**
     * 导出声纹数据
     */
    public Map<String, Object> exportVoiceprints(String agentId) {
        logger.info("导出声纹数据，设备ID: {}", agentId);
        
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("exportTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        exportData.put("agentId", agentId);
        
        if (agentId != null && !agentId.trim().isEmpty()) {
            // 导出指定设备的声纹
            List<Voiceprint> voiceprints = getVoiceprintsByAgentId(agentId);
            exportData.put("voiceprints", voiceprints);
            exportData.put("totalCount", voiceprints.size());
        } else {
            // 导出所有声纹
            Map<String, List<Voiceprint>> allData = new HashMap<>(voiceprintsByAgent);
            exportData.put("voiceprintsByAgent", allData);
            exportData.put("totalAgents", allData.size());
            exportData.put("totalVoiceprints", allData.values().stream().mapToInt(List::size).sum());
        }
        
        return exportData;
    }

    /**
     * 获取声纹统计信息
     */
    public Map<String, Object> getVoiceprintStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalAgents = voiceprintsByAgent.size();
        int totalVoiceprints = voiceprintsByAgent.values().stream().mapToInt(List::size).sum();
        
        stats.put("totalAgents", totalAgents);
        stats.put("totalVoiceprints", totalVoiceprints);
        stats.put("averageVoiceprintsPerAgent", totalAgents > 0 ? (double) totalVoiceprints / totalAgents : 0);
        
        // 按设备统计
        Map<String, Integer> voiceprintsByAgentCount = voiceprintsByAgent.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()
                ));
        stats.put("voiceprintsByAgent", voiceprintsByAgentCount);
        
        return stats;
    }

    /**
     * 验证上传的文件
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过 " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AUDIO_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("不支持的文件类型: " + contentType);
        }
    }

    /**
     * 获取上传目录
     */
    private String getUploadDirectory(String agentId) {
        return DEFAULT_UPLOAD_DIR + File.separator + agentId;
    }

    /**
     * 创建目录（如果不存在）
     */
    private void createDirectoryIfNotExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            logger.info("创建目录: {}", dirPath);
        }
    }

    /**
     * 生成唯一文件名
     */
    private String generateUniqueFileName(String originalFileName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        
        if (originalFileName != null && originalFileName.contains(".")) {
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            return timestamp + "_" + uuid + extension;
        } else {
            return timestamp + "_" + uuid + ".wav"; // 默认扩展名
        }
    }

    /**
     * 删除物理文件
     */
    private void deletePhysicalFile(String filePath) {
        if (filePath != null && !filePath.trim().isEmpty()) {
            try {
                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    Files.delete(path);
                    logger.info("物理文件删除成功: {}", filePath);
                } else {
                    logger.warn("要删除的文件不存在: {}", filePath);
                }
            } catch (IOException e) {
                logger.error("删除物理文件失败: {}", filePath, e);
            }
        }
    }
}