package vn.uit.lms.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import vn.uit.lms.core.entity.Account;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/**
 * Service for sending emails asynchronously.
 * <p>
 * We use the {@link Async} annotation to send emails asynchronously.
 */
@Service
public class MailService {

    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);

    private static final String USER = "user";

    private static final String BASE_URL = "baseUrl";

    private static final String API_VERSION = "apiVersion";

    @Value("${spring.mail.username")
    private String sender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.api-version}")
    private String apiVersion;

    private final JavaMailSender javaMailSender;

    private final MessageSource messageSource;

    private final SpringTemplateEngine templateEngine;

    public MailService(
            JavaMailSender javaMailSender,
            MessageSource messageSource,
            SpringTemplateEngine templateEngine
    ) {
        this.javaMailSender = javaMailSender;
        this.messageSource = messageSource;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        sendEmailSync(to, subject, content, isMultipart, isHtml);
    }

    private void sendEmailSync(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        LOG.debug(
                "Send email[multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}",
                isMultipart,
                isHtml,
                to,
                subject,
                content
        );

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
            message.setTo(to);
            message.setFrom(sender);
            message.setSubject(subject);
            message.setText(content, isHtml);
            javaMailSender.send(mimeMessage);
            LOG.debug("Sent email to User '{}'", to);
        } catch (MailException | MessagingException e) {
            LOG.warn("Email could not be sent to user '{}'", to, e);
        }
    }

    @Async
    public void sendEmailFromTemplate(Account account, String templateName, String titleKey, String token) {
        sendEmailFromTemplateSync(account, templateName, titleKey, token);
    }

    private void sendEmailFromTemplateSync(Account account, String templateName, String titleKey, String token) {
        if (account.getEmail() == null) {
            LOG.debug("Email doesn't exist for user '{}'", account.getUsername());
            return;
        }
        Locale locale = Locale.forLanguageTag(account.getLangKey());
        Context context = new Context(locale);
        context.setVariable(USER, account);
        context.setVariable(BASE_URL, baseUrl);
        context.setVariable(API_VERSION, apiVersion);
        context.setVariable("token", token);
        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);
        sendEmailSync(account.getEmail(), subject, content, false, true);
    }

    @Async
    public void sendActivationEmail(Account user, String token) {
        LOG.debug("Sending activation email to '{}'", user.getEmail());
        sendEmailFromTemplateSync(user, "mail/activationEmail", "email.activation.title", token);
    }

    @Async
    public void sendCreationEmail(Account user, String token) {
        LOG.debug("Sending creation email to '{}'", user.getEmail());
        sendEmailFromTemplateSync(user, "mail/creationEmail", "email.activation.title", token);
    }

    @Async
    public void sendActivationSuccessEmail(Account user) {
        LOG.debug("Sending activation success email to '{}'", user.getEmail());
        sendEmailFromTemplateSync(user, "mail/activationSuccessEmail", "email.activation.title", "");
    }

    @Async
    public void sendPasswordResetMail(Account user, String token) {
        LOG.debug("Sending password reset email to '{}'", user.getEmail());
        sendEmailFromTemplateSync(user, "mail/passwordResetEmail", "email.reset.title", token);
    }
}
