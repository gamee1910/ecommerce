package com.ecommerce.service.notification.email;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "EmailService")
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.notification.from-email}")
    private String fromEmail;

    @Value("${app.notification.from-name}")
    private String fromName;

    @Value("${app.notification.dry-run:true}")
    private boolean dryRun;

    /**
     * Sends a simple email. In dry-run mode, only logs the message.
     */
    public void send(String toEmail, String subject, String body) {
        if (dryRun) {
            log.info(
                    "[DRY-RUN] Would send email to={} subject='{}' body='{}'",
                    toEmail, subject, body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(String.format("%s <%s>", fromName, fromEmail));
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            message.setSentDate(java.util.Date.from(OffsetDateTime.now().toInstant()));

            mailSender.send(message);
            log.info("Email sent to={} subject='{}'", toEmail, subject);
        } catch (Exception e) {
            log.error("Failed to send email to={} subject='{}': {}", toEmail, subject, e.getMessage());
            throw e;
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Template builders
    // ────────────────────────────────────────────────────────────────────────────

    public void sendWelcomeEmail(String toEmail) {
        String subject = "Chào mừng bạn đến với E-Commerce Platform!";
        String body = """
                Xin chào!

                Chào mừng bạn đã gia nhập nền tảng E-Commerce của chúng tôi.
                Tài khoản của bạn đã được tạo thành công với email: %s

                Hãy bắt đầu khám phá hàng ngàn sản phẩm ngay hôm nay!

                Trân trọng,
                Đội ngũ E-Commerce Platform
                """.formatted(toEmail);
        send(toEmail, subject, body);
    }

    public void sendOrderConfirmationEmail(String toEmail, String orderId, String totalAmount) {
        String subject = String.format("Xác nhận đơn hàng #%s thành công", orderId.substring(0, 8));
        String body = """
                Xin chào!

                Đơn hàng của bạn đã được đặt thành công.

                Mã đơn hàng: %s
                Tổng tiền: %s VNĐ

                Chúng tôi sẽ sớm xử lý và giao hàng đến bạn.
                Bạn có thể theo dõi trạng thái đơn hàng trong tài khoản của mình.

                Cảm ơn bạn đã mua sắm tại E-Commerce Platform!

                Trân trọng,
                Đội ngũ E-Commerce Platform
                """.formatted(orderId, totalAmount);
        send(toEmail, subject, body);
    }
}
