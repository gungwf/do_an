package com.service.appointment_service.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class StartProtocolRequest {
    private UUID patientId;
    private UUID protocolId;
}