package oneEight.service.impl;

import oneEight.Dto.MailParams;
import oneEight.service.MailSenderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailSenderServiceImpl implements MailSenderService {
    private final JavaMailSender javaMailSender;
    @Value("${spring.mail.username}")
    private String emailFrom;
    @Value("${service.activation.uri}")
    private String activationServiceUri;

    public MailSenderServiceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void sendMail(MailParams mailParams) {
        var subject = "Активация учетной записи";
        var massageBody = getActivationMailBody(mailParams.getId());
        var emailTo = mailParams.getEmailTo();

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(emailFrom);
        mailMessage.setTo(emailTo);
        mailMessage.setSubject(subject);
        mailMessage.setText(massageBody);

        javaMailSender.send(mailMessage);
    }

    private String getActivationMailBody(String id) {
    var msg = String.format("Для завершения регистрации, перейдите по ссылке:\n%s", activationServiceUri);
        return msg.replace("{id}", id);
    }
}
