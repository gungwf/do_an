package com.service.sys_srv.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        String imageUrl = (String) uploadResult.get("secure_url");
        return imageUrl;
    }

    public String uploadImage(MultipartFile file, String folder, String resourceType) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(
            file.getBytes(),
            ObjectUtils.asMap(
                "folder", folder,
                "resource_type", resourceType
            )
        );
        String imageUrl = (String) uploadResult.get("secure_url");
        String publicId = (String) uploadResult.get("public_id");
        
        // Return format: "imageUrl|publicId" để dễ lưu trữ
        return imageUrl + "|" + publicId;
    }

    public void deleteImage(String publicId) throws IOException {
        if (publicId == null || publicId.isEmpty()) {
            return;
        }
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}
