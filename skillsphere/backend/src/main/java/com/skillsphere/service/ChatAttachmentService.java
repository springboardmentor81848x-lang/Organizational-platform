package com.skillsphere.service;

import com.skillsphere.dto.ChatDtos.AttachmentUploadResponse;
import com.skillsphere.entity.ConversationParticipant;
import com.skillsphere.entity.Message;
import com.skillsphere.entity.User;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.ConversationParticipantRepository;
import com.skillsphere.repository.MessageRepository;
import com.skillsphere.repository.SystemConfigurationRepository;
import com.skillsphere.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class ChatAttachmentService {

    private final Path uploadDir;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SystemConfigurationRepository configRepository;

    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "sh", "ps1", "vbs", "js", "jar", "msi", "com", "scr", "pif", "php", "jsp"
    );

    public ChatAttachmentService(
            @Value("${app.chat.upload-dir:uploads/chat}") String uploadDirPath,
            ConversationParticipantRepository participantRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            SystemConfigurationRepository configRepository) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.configRepository = configRepository;

        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize chat upload storage directory.", e);
        }
    }

    public AttachmentUploadResponse uploadAttachment(String userEmail, Long conversationId, MultipartFile file) {
        User user = getUserByEmail(userEmail);

        // Authorization: caller must be a conversation participant
        boolean isParticipant = participantRepository.existsByConversationIdAndUserId(conversationId, user.getId());
        if (!isParticipant) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. You cannot upload attachments to a conversation you are not part of.");
        }

        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is empty or missing.");
        }

        // 1. Validate max size
        int maxMb = getConfigInt("max_upload_size_mb", 10);
        long maxBytes = (long) maxMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File size exceeds the platform maximum limit of " + maxMb + "MB.");
        }

        // 2. Validate filename and extension
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "attachment";
        }
        // Path traversal defense
        originalName = Paths.get(originalName).getFileName().toString();

        String extension = getFileExtension(originalName).toLowerCase();
        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Executable and script attachments (." + extension + ") are prohibited for platform security.");
        }

        String allowedTypesStr = getConfigString("allowed_attachment_types", "pdf,doc,docx,png,jpg,jpeg,txt,csv,xlsx");
        Set<String> allowedSet = new HashSet<>(Arrays.asList(allowedTypesStr.toLowerCase().split("\\s*,\\s*")));
        if (!allowedSet.contains(extension)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attachment type '." + extension + "' is not permitted by current communication policy.");
        }

        // 3. Generate safe storage name with UUID prefix
        String storedName = UUID.randomUUID() + "_" + sanitizeFileName(originalName);
        Path targetPath = this.uploadDir.resolve(storedName).normalize();

        if (!targetPath.startsWith(this.uploadDir)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file storage path.");
        }

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save attachment file.");
        }

        String relativeDownloadUrl = "/api/chat/attachments/" + storedName + "/download";
        return new AttachmentUploadResponse(
                relativeDownloadUrl,
                originalName,
                file.getSize(),
                file.getContentType() != null ? file.getContentType() : "application/octet-stream"
        );
    }

    public Resource downloadAttachment(String userEmail, String storedFileName) {
        User user = getUserByEmail(userEmail);

        // Path traversal defense
        Path safePath = this.uploadDir.resolve(Paths.get(storedFileName).getFileName().toString()).normalize();
        if (!safePath.startsWith(this.uploadDir)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. Path traversal is rejected.");
        }

        if (!Files.exists(safePath)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Attachment file not found.");
        }

        // Find message associated with this attachment URL to verify participant authorization
        String expectedUrl = "/api/chat/attachments/" + storedFileName + "/download";
        Optional<Message> msgOpt = messageRepository.findAll().stream()
                .filter(m -> expectedUrl.equalsIgnoreCase(m.getAttachmentUrl()) || storedFileName.equals(m.getAttachmentName()))
                .findFirst();

        if (msgOpt.isPresent()) {
            Message msg = msgOpt.get();
            boolean isParticipant = participantRepository.existsByConversationIdAndUserId(msg.getConversation().getId(), user.getId());
            if (!isParticipant) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. You are not authorized to download attachments from this conversation.");
            }
        }

        try {
            Resource resource = new UrlResource(safePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ApiException(HttpStatus.NOT_FOUND, "Attachment file cannot be read.");
            }
        } catch (MalformedURLException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Malformed attachment file path.");
        }
    }

    private String getFileExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1) : "";
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private int getConfigInt(String key, int defaultValue) {
        return configRepository.findById(key)
                .map(c -> {
                    try { return Integer.parseInt(c.getConfigValue()); } catch (Exception e) { return defaultValue; }
                }).orElse(defaultValue);
    }

    private String getConfigString(String key, String defaultValue) {
        return configRepository.findById(key)
                .map(c -> c.getConfigValue().trim())
                .orElse(defaultValue);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }
}
