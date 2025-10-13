package com.service.medical_record_service.service;

import com.service.medical_record_service.dto.TemplateRequest;
import com.service.medical_record_service.entity.DiagnosisTemplate;
import com.service.medical_record_service.entity.PrescriptionTemplateItem;
import com.service.medical_record_service.repository.DiagnosisTemplateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateService {
    private final DiagnosisTemplateRepository templateRepository;

    @Transactional
    public DiagnosisTemplate createTemplate(UUID doctorId, TemplateRequest request) {
        DiagnosisTemplate template = new DiagnosisTemplate();
        template.setDoctorId(doctorId);
        template.setTemplateName(request.getTemplateName());
        template.setDiagnosisContent(request.getDiagnosisContent());
        template.setIcd10Code(request.getIcd10Code());

        if (request.getPrescriptionItems() != null) {
            List<PrescriptionTemplateItem> items = request.getPrescriptionItems().stream().map(dto -> {
                PrescriptionTemplateItem item = new PrescriptionTemplateItem();
                item.setProductId(dto.productId());
                item.setQuantity(dto.quantity());
                item.setDosage(dto.dosage());
                item.setTemplate(template); // Liên kết ngược lại
                return item;
            }).collect(Collectors.toList());
            template.setPrescriptionItems(items);
        }
        return templateRepository.save(template);
    }

    public List<DiagnosisTemplate> getTemplatesByDoctor(UUID doctorId) {
        // Cần thêm phương thức findByDoctorId vào repository
        return templateRepository.findByDoctorId(doctorId);
    }
}