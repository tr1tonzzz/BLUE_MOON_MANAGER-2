package vn.bluemoon.util;

import vn.bluemoon.config.AppConfig;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Utility class for sending emails
 */
public class EmailSender {
    private static final AppConfig config = AppConfig.getInstance();

    /**
     * Send password reset email
     * @param toEmail Recipient email
     * @param token Reset token
     * @return true if sent successfully
     */
    public static boolean sendPasswordResetEmail(String toEmail, String token) {
        try {
            String resetUrl = "http://localhost:8080/reset-password?token=" + token;
            String subject = "Đặt lại mật khẩu - Blue Moon";
            String body = String.format(
                "Xin chào,\n\n" +
                "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản của mình.\n\n" +
                "Vui lòng click vào liên kết sau để đặt lại mật khẩu:\n" +
                "%s\n\n" +
                "Liên kết này sẽ hết hạn sau 24 giờ.\n\n" +
                "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n" +
                "Trân trọng,\n" +
                "Hệ thống Blue Moon",
                resetUrl
            );

            return sendEmail(toEmail, subject, body);
        } catch (Exception e) {
            AppLogger.error("Error sending password reset email", e);
            return false;
        }
    }

    /**
     * Send email
     * @param toEmail Recipient email
     * @param subject Email subject
     * @param body Email body
     * @return true if sent successfully
     */
    private static boolean sendEmail(String toEmail, String subject, String body) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", config.getEmailSmtpHost());
            props.put("mail.smtp.port", config.getEmailSmtpPort());
            props.put("mail.smtp.auth", config.getEmailSmtpAuth());
            props.put("mail.smtp.starttls.enable", config.getEmailSmtpStartTls());

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                        config.getEmailFrom(),
                        config.getEmailPassword()
                    );
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getEmailFrom()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            AppLogger.info("Email sent successfully to: " + toEmail);
            return true;
        } catch (Exception e) {
            AppLogger.error("Error sending email to: " + toEmail, e);
            return false;
        }
    }
}

















