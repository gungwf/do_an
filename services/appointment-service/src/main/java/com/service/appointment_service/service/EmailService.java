package com.service.appointment_service.service;

import com.service.appointment_service.dto.response.AppointmentResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendAppointmentConfirmation(AppointmentResponseDto appointment) {
        if (appointment.patient() == null || appointment.patient().email() == null) {
            System.out.println("Không thể gửi email: thiếu thông tin bệnh nhân.");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(appointment.patient().email());
        message.setSubject("Xác nhận Lịch hẹn tại Phòng khám của chúng tôi");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
        String formattedTime = appointment.appointmentTime().format(formatter);

        // Nội dung email
        String text = String.format(
                "Chào bạn %s,\n\n" +
                        "Lịch hẹn của bạn đã được tạo thành công.\n\n" +
                        "Thông tin chi tiết:\n" +
                        "- Bác sĩ: %s\n" +
                        "- Thời gian: %s\n" +
                        "- Tại: %s\n\n" +
                        "Vui lòng đến trước giờ hẹn 15 phút. Cảm ơn bạn đã tin tưởng dịch vụ của chúng tôi!\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ Phòng khám.",
                appointment.patient().fullName(),
                appointment.doctor().fullName(),
                formattedTime,
                appointment.branch().branchName()
        );

        message.setText(text);
        mailSender.send(message);
        System.out.println("Đã gửi email xác nhận đến: " + appointment.patient().email());
    }

    public void sendAppointmentReminder(AppointmentResponseDto appointment) {
        if (appointment.patient() == null || appointment.patient().email() == null) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(appointment.patient().email());
        message.setSubject("Thông báo nhắc lịch hẹn của bạn");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
        String formattedTime = appointment.appointmentTime().format(formatter);

        String text = String.format(
                "Chào bạn %s,\n\n" +
                        "Đây là email nhắc nhở về lịch hẹn sắp tới của bạn tại phòng khám của chúng tôi.\n\n" +
                        "Thông tin chi tiết:\n" +
                        "- Bác sĩ: %s\n" +
                        "- Thời gian: %s\n\n" +
                        "Rất mong được đón tiếp bạn!\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ Phòng khám.",
                appointment.patient().fullName(),
                appointment.doctor().fullName(),
                formattedTime
        );

        message.setText(text);
        mailSender.send(message);
    }

    public void sendAppointmentCancellation(AppointmentResponseDto appointment) {
        if (appointment.patient() == null || appointment.patient().email() == null) {
            System.out.println("Không thể gửi email: thiếu thông tin bệnh nhân.");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(appointment.patient().email());
        message.setSubject("Thông báo Hủy Lịch hẹn tại Phòng khám của chúng tôi");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
        String formattedTime = appointment.appointmentTime().format(formatter);

        // Nội dung email
        String text = String.format(
                "Chào bạn %s,\n\n" +
                        "Chúng tôi rất tiếc phải thông báo rằng lịch hẹn của bạn đã được hủy.\n\n" +
                        "Thông tin lịch hẹn đã hủy:\n" +
                        "- Thời gian: %s\n\n" +
                        "Nếu bạn không phải là người yêu cầu hủy, vui lòng liên hệ với chúng tôi ngay lập tức. " +
                        "Nếu bạn muốn đặt lại lịch hẹn khác, vui lòng truy cập website của chúng tôi.\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ Phòng khám.",
                appointment.patient().fullName(),
                formattedTime
        );

        message.setText(text);

        // Gửi email
        mailSender.send(message);

        System.out.println("Đã gửi email thông báo hủy lịch đến: " + appointment.patient().email());
    }

    public void sendAppointmentCompletedEmail(AppointmentResponseDto appointment) {
        if (appointment.patient() == null || appointment.patient().email() == null) {
            System.out.println("Không thể gửi email: thiếu thông tin bệnh nhân.");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(appointment.patient().email());
        message.setSubject("Cảm ơn bạn đã sử dụng dịch vụ tại Phòng khám");

        // Nội dung email
        String text = String.format(
                "Chào bạn %s,\n\n" +
                        "Chúng tôi rất mong nhận được đánh giá của bạn để cải thiện chất lượng dịch vụ. " +
                        "Bạn có thể đăng nhập vào hệ thống để lại đánh giá cho lịch hẹn này.\n\n" +
                        "Cảm ơn bạn đã tin tưởng!\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ Phòng khám.",
                appointment.patient().fullName()
        );

        message.setText(text);

        // Gửi email
        mailSender.send(message);

        System.out.println("Đã gửi email hoàn thành lịch hẹn đến: " + appointment.patient().email());
    }
}