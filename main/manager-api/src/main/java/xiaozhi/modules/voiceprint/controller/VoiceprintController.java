package com.example.esp32server.controller;

import com.example.esp32server.model.Voiceprint;
import com.example.esp32server.service.VoiceprintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voiceprints")
@CrossOrigin(origins = "*")
public class VoiceprintController {

    private static final Logger logger = LoggerFactory.getLogger(VoiceprintController.class);

    @Autowired
    private VoiceprintService voiceprintService;

    /**
     * 获取指定设备的声纹列表
     */
    @GetMapping("/{agentId}")
    public ResponseEntity<?> getVoiceprintsByAgentId(@PathVariable String agentId) {
        try {
            logger.info("获取设备 {} 的声纹列表", agentId);
            
            if (agentId == null || agentId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("设备ID不能为空"));
            }
            
            List<Voiceprint> voiceprints = voiceprintService.getVoiceprintsByAgentId(agentId);
            logger.info("设备 {} 共有 {} 个声纹", agentId, voiceprints.size());
            
            return ResponseEntity.ok(voiceprints);
        } catch (Exception e) {
            logger.error("获取声纹列表失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("获取声纹列表失败: " + e.getMessage()));
        }
    }

    /**
     * 上传新的声纹文件
     */
    @PostMapping("/{agentId}")
    public ResponseEntity<?> addVoiceprint(
            @PathVariable String agentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name) {
        try {
            logger.info("为设备 {} 上传声纹文件: {}", agentId, file.getOriginalFilename());
            
            // 参数验证
            if (agentId == null || agentId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("设备ID不能为空"));
            }
            
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("请选择要上传的文件"));
            }
            
            // 文件类型验证
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                return ResponseEntity.badRequest().body(createErrorResponse("请上传音频文件"));
            }
            
            // 文件大小验证 (限制为10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(createErrorResponse("文件大小不能超过10MB"));
            }
            
            // 创建声纹对象
            Voiceprint voiceprint = new Voiceprint();
            if (name != null && !name.trim().isEmpty()) {
                voiceprint.setName(name.trim());
            } else {
                // 如果没有提供名称，使用文件名（去掉扩展名）
                String fileName = file.getOriginalFilename();
                if (fileName != null && fileName.contains(".")) {
                    fileName = fileName.substring(0, fileName.lastIndexOf("."));
                }
                voiceprint.setName(fileName != null ? fileName : "未命名声纹");
            }
            
            Voiceprint savedVoiceprint = voiceprintService.addVoiceprint(agentId, voiceprint, file);
            logger.info("声纹上传成功: {} (ID: {})", savedVoiceprint.getName(), savedVoiceprint.getId());
            
            return ResponseEntity.ok(savedVoiceprint);
        } catch (IOException e) {
            logger.error("文件上传失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("文件上传失败: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("声纹添加失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("声纹添加失败: " + e.getMessage()));
        }
    }

    /**
     * 删除指定的声纹
     */
    @DeleteMapping("/{agentId}/{voiceprintId}")
    public ResponseEntity<?> deleteVoiceprint(@PathVariable String agentId, @PathVariable String voiceprintId) {
        try {
            logger.info("删除设备 {} 的声纹: {}", agentId, voiceprintId);
            
            if (agentId == null || agentId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("设备ID不能为空"));
            }
            
            if (voiceprintId == null || voiceprintId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("声纹ID不能为空"));
            }
            
            boolean deleted = voiceprintService.deleteVoiceprint(agentId, voiceprintId);
            if (deleted) {
                logger.info("声纹删除成功: {}", voiceprintId);
                return ResponseEntity.ok(createSuccessResponse("声纹删除成功"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("删除声纹失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("删除声纹失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有声纹列表（不按设备分组）
     */
    @GetMapping
    public ResponseEntity<?> getAllVoiceprints(@RequestParam(value = "agentId", required = false) String agentId) {
        try {
            if (agentId != null && !agentId.trim().isEmpty()) {
                // 如果提供了agentId参数，则返回该设备的声纹
                return getVoiceprintsByAgentId(agentId);
            } else {
                // 否则返回所有声纹
                List<Voiceprint> allVoiceprints = voiceprintService.getAllVoiceprints();
                return ResponseEntity.ok(allVoiceprints);
            }
        } catch (Exception e) {
            logger.error("获取声纹列表失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("获取声纹列表失败: " + e.getMessage()));
        }
    }

    /**
     * 导出声纹数据
     */
    @GetMapping("/export")
    public ResponseEntity<?> exportVoiceprints(@RequestParam(value = "agentId", required = false) String agentId) {
        try {
            logger.info("导出声纹数据，设备ID: {}", agentId);
            
            Map<String, Object> exportData = voiceprintService.exportVoiceprints(agentId);
            return ResponseEntity.ok(exportData);
        } catch (Exception e) {
            logger.error("导出声纹数据失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("导出声纹数据失败: " + e.getMessage()));
        }
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * 创建成功响应
     */
    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}