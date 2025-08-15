package ai.dat.agent.agentic.tools.email;

import com.google.common.base.Preconditions;
import jakarta.mail.*;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Properties;

/**
 * 邮件发送工具类
 * 基于Jakarta Mail API实现的邮件发送功能
 *
 * @Author JunjieM
 * @Date 2025/1/26
 */
@Slf4j
@Data
@Builder
public class EmailSender {

    private final String smtpHost;
    private final Integer smtpPort;
    private final Long smtpConnectionTimeout;
    private final Long smtpTimeout;
    private final Long smtpWriteTimeout;
    private final boolean enableAuth;
    private final boolean enableTls;
    private final String username;
    private final String password;
    private final String fromAddress;
    private final String fromName;

    /**
     * 发送简单文本邮件
     *
     * @param to      收件人邮箱地址
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 发送是否成功
     */
    public void sendTextEmail(String to, String subject, String content) {
        sendEmail(new String[]{to}, subject, content, false);
    }

    /**
     * 发送HTML格式邮件
     *
     * @param to          收件人邮箱地址
     * @param subject     邮件主题
     * @param htmlContent HTML格式的邮件内容
     * @return 发送是否成功
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        sendEmail(new String[]{to}, subject, htmlContent, true);
    }

    /**
     * 发送邮件到多个收件人
     *
     * @param to      收件人邮箱地址数组
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param isHtml  是否为HTML格式
     * @return 发送是否成功
     */
    public void sendEmail(String[] to, String subject, String content, boolean isHtml) {
        sendEmailWithCc(to, null, subject, content, isHtml);
    }

    /**
     * 发送带抄送的邮件
     *
     * @param to      收件人邮箱地址数组
     * @param cc      抄送邮箱地址数组
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param isHtml  是否为HTML格式
     * @return 发送是否成功
     */
    public void sendEmailWithCc(String[] to, String[] cc, String subject, String content, boolean isHtml) {
        Preconditions.checkArgument(to != null && to.length > 0,
                "The recipient list is empty, so emails cannot be sent");
        Preconditions.checkArgument(subject != null && !subject.trim().isEmpty(),
                "The subject of the email is empty and it cannot be sent");
        try {
            // 创建邮件会话
            Session session = createSession();
            // 创建邮件消息
            MimeMessage message = new MimeMessage(session);
            // 设置发件人
            try {
                if (fromName != null && !fromName.trim().isEmpty()) {
                    message.setFrom(new InternetAddress(fromAddress, fromName, "UTF-8"));
                } else {
                    message.setFrom(new InternetAddress(fromAddress));
                }
            } catch (AddressException e) {
                throw new RuntimeException(String.format(
                        "The sender's address format is incorrect: %s", fromAddress), e);
            }
            // 设置收件人
            try {
                InternetAddress[] toAddresses = new InternetAddress[to.length];
                for (int i = 0; i < to.length; i++) {
                    toAddresses[i] = new InternetAddress(to[i].trim());
                    toAddresses[i].validate(); // 验证邮箱地址格式
                }
                message.setRecipients(Message.RecipientType.TO, toAddresses);
            } catch (AddressException e) {
                throw new RuntimeException(String.format(
                        "The recipient's address format is incorrect: %s", String.join(", ", to)), e);
            }
            // 设置抄送
            if (cc != null && cc.length > 0) {
                try {
                    InternetAddress[] ccAddresses = new InternetAddress[cc.length];
                    for (int i = 0; i < cc.length; i++) {
                        ccAddresses[i] = new InternetAddress(cc[i].trim());
                        ccAddresses[i].validate(); // 验证邮箱地址格式
                    }
                    message.setRecipients(Message.RecipientType.CC, ccAddresses);
                } catch (AddressException e) {
                    throw new RuntimeException(String.format(
                            "The format of the copied address is incorrect: %s", String.join(", ", cc)), e);
                }
            }
            // 设置邮件主题
            message.setSubject(subject, "UTF-8");
            // 设置发送时间
            message.setSentDate(new Date());
            // 设置邮件内容
            if (isHtml) {
                message.setContent(content, "text/html; charset=UTF-8");
            } else {
                message.setText(content, "UTF-8");
            }
            // 设置邮件头信息
            message.setHeader("X-Mailer", "DAT Agent Email Sender");
            message.setHeader("X-Priority", "3"); // 普通优先级
            // 发送邮件
            Transport.send(message);

            String recipients = String.join(", ", to);
            String ccRecipients = (cc != null && cc.length > 0) ? String.join(", ", cc) : "";
            if (!ccRecipients.isEmpty()) {
                log.info("Email sent successfully: Subject={}, Recipient={}, CC={}", subject, recipients, ccRecipients);
            } else {
                log.info("The email was sent successfully: Subject={}, Recipient={}", subject, recipients);
            }
        } catch (MessagingException e) {
            throw new RuntimeException(String.format(
                    "Email sending failed - Messaging exception: Subject=%s, Recipient=%s, ErrorMessage=%s",
                    subject, String.join(", ", to), e.getMessage()), e);
        } catch (Exception e) {
            throw new RuntimeException(String.format(
                    "Email sending failed - Unknown error: Subject=%s, Recipient=%s, ErrorMessage=%s",
                    subject, String.join(", ", to), e.getMessage()), e);
        }
    }

    /**
     * 创建邮件会话
     * 使用Jakarta Mail API配置SMTP会话
     */
    private Session createSession() {
        Properties props = new Properties();

        // SMTP服务器配置
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.auth", String.valueOf(enableAuth));

        // 设置连接和读取超时
        props.put("mail.smtp.connectiontimeout", String.valueOf(smtpConnectionTimeout));
        props.put("mail.smtp.timeout", String.valueOf(smtpTimeout));
        props.put("mail.smtp.writetimeout", String.valueOf(smtpWriteTimeout));

        // TLS/SSL配置
        if (enableTls) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            // 设置TLS协议版本
            props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        }

        // SSL配置（通常用于465端口）
        if (smtpPort == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        }

        // 设置邮件传输协议
        props.put("mail.transport.protocol", "smtp");

        // 设置调试模式（生产环境中应该关闭）
        props.put("mail.debug", "false");

        // 设置邮件格式
        props.put("mail.mime.charset", "UTF-8");

        if (enableAuth && username != null && password != null) {
            return Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        } else {
            return Session.getInstance(props);
        }
    }
}
