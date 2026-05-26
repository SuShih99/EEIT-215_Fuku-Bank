package com.javaeasybank.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 檔案上傳服務 — 將圖片存至本地檔案系統，回傳可存取的 URL 路徑。
 */
@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(
            @Value("${app.upload.dir:uploads}") String uploadPath) {
        this.uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("無法建立上傳目錄: " + this.uploadDir, e);
        }
    }

    /**
     * 刪除檔案
     *
     * @param fileUrl store() 回傳的 URL 路徑（如 /uploads/loan-documents/APP123/uuid.pdf）
     */
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;
        // /uploads/ 前綴去除，換算回實體路徑
        String relativePath = fileUrl.startsWith("/uploads/")
                ? fileUrl.substring("/uploads/".length())
                : fileUrl;
        Path target = uploadDir.resolve(relativePath).normalize();
        // 防止 path traversal
        if (!target.startsWith(uploadDir)) {
            throw new IllegalArgumentException("非法的檔案路徑");
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new RuntimeException("檔案刪除失敗: " + target, e);
        }
    }

    /**
     * 儲存檔案，回傳相對路徑（如 /uploads/abc123.webp）
     *
     * @param file     上傳的檔案
     * @param subDir   子目錄（如 "id-cards"）
     * @return 檔案的 URL 路徑
     */
    public String store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上傳檔案不可為空");
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        // 只允許圖片與 PDF 格式
        String lower = ext.toLowerCase();
        if (!lower.equals(".jpg") && !lower.equals(".jpeg")
                && !lower.equals(".png") && !lower.equals(".webp") && !lower.equals(".pdf")) {
            throw new IllegalArgumentException("僅支援 JPG / PNG / WEBP / PDF 格式");
        }

        // 檔案大小限制 10MB
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("檔案大小不可超過 10MB");
        }

        String fileName = UUID.randomUUID() + ext;
        Path targetDir = uploadDir.resolve(subDir);

        try {
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + subDir + "/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("檔案儲存失敗", e);
        }
    }
}
