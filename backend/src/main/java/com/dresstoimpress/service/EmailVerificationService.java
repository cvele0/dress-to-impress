package com.dresstoimpress.service;

import com.dresstoimpress.constants.Constants;
import com.dresstoimpress.exception.InvalidTokenException;
import com.dresstoimpress.model.EmailVerificationToken;
import com.dresstoimpress.model.User;
import com.dresstoimpress.repository.EmailVerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class EmailVerificationService {
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final JavaMailSender mailSender;
    private final ApplicationEventPublisher eventPublisher;
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailVerificationService.class);

    @Autowired
    public EmailVerificationService(EmailVerificationTokenRepository emailVerificationTokenRepository,
                                    JavaMailSender mailSender, ApplicationEventPublisher eventPublisher) {
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.mailSender = mailSender;
        this.eventPublisher = eventPublisher;
    }

    public boolean isExpiredToken(LocalDateTime expiryDateTime) {
        return expiryDateTime.isBefore(LocalDateTime.now());
    }

    public void deleteById(Long id) {
        emailVerificationTokenRepository.deleteById(id);
    }

    public void deleteByUserId(Long userId) {
        emailVerificationTokenRepository.deleteByUser_Id(userId);
    }

    public EmailVerificationToken getByToken(String token) throws InvalidTokenException {
        EmailVerificationToken savedToken = emailVerificationTokenRepository.findByToken(token);
        if (savedToken == null) {
            throw new InvalidTokenException("The token is invalid!", token);
        }
        return savedToken;
    }

    public void createAndSendVerificationToken(User user) {
        EmailVerificationToken token = createToken(user);
        emailVerificationTokenRepository.save(token);
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setSubject("Verify email to access Dress To Impress");
        mailMessage.setText(createEmailText(user, token.getToken()));
        eventPublisher.publishEvent(mailMessage);
    }

    private EmailVerificationToken createToken(User user) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDateTime(LocalDateTime.now().plusMinutes(Constants.EMAIL_TOKEN_EXPIRATION_MINUTES));
        return token;
    }

    private String createEmailText(User user, String token) {
        return "Dear " + user.getFirstName() + ",\n" +
                "Please click the following link to verify your email at Dress To Impress: " +
                "http://localhost:5173" + Constants.VERIFY_EMAIL_PATH +
                "?token=" + token + " (it expires in " + Constants.EMAIL_TOKEN_EXPIRATION_MINUTES +
                " minutes).\n\nKind regards,\nDress To Impress";
    }

    /**
     * Event listener that sends a verification token by email. Should NOT be called manually!
     * @param mailMessage object containing data about sender, receiver, subject etc.
     */
    @EventListener
    public void sendEmailForVerification(SimpleMailMessage mailMessage) {
        LOGGER.info("Sending a verification email to {}", Objects.requireNonNull(mailMessage.getTo())[0]);
        mailSender.send(mailMessage);
    }

    public List<EmailVerificationToken> getAllTokensToBeRemoved() {
        return emailVerificationTokenRepository.findAllByExpiryDateTimeBefore(LocalDateTime.now().minusDays(10));
    }
}