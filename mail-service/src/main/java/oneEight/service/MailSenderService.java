package oneEight.service;

import oneEight.utils.dto.MailParams;

public interface MailSenderService {
    void sendMail(MailParams mailParams);

}
