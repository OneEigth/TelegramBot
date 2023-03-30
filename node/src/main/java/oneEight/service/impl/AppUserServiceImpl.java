package oneEight.service.impl;

import lombok.extern.log4j.Log4j;
import oneEight.dao.AppUserDao;
import oneEight.entity.AppUser;
import oneEight.service.AppUserService;
import oneEight.utils.CryptoTool;
import oneEight.utils.dto.MailParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import static oneEight.entity.enums.UserState.BASIC_STATE;
import static oneEight.entity.enums.UserState.WAITING_FOR_EMAIL_STATE;

@Service
@Log4j
public class AppUserServiceImpl implements AppUserService {
    private final AppUserDao appUserDao;
    private final CryptoTool cryptoTool;

    @Value("${service.mail.uri}")
    private String mailServiceUri;

    public AppUserServiceImpl(AppUserDao appUserDao, CryptoTool cryptoTool) {
        this.appUserDao = appUserDao;
        this.cryptoTool = cryptoTool;
    }

    @Override
    public String registerUser(AppUser appUser) {
        if (appUser.getIsActive()) {
            return "Пользователь уже зарегистрирован";
        } else if (appUser.getEmail() != null) {
            return "Вам на почту уже было отправлено письмо с подтверждением регистрации." +
                    "Перейдите по ссылке в письме, чтобы завершить регистрацию";
        }
        appUser.setState(WAITING_FOR_EMAIL_STATE);
        appUserDao.save(appUser);

        return "Введите пожалуйста ваш email";
    }

    @Override
    public String setEmail(AppUser appUser, String email) {
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException e) {
            log.error("Invalid email: " + email, e);
            e.printStackTrace();
            return "Неверный формат email. Попробуйте еще раз, или введите /cancel";
        }

        var optional = appUserDao.findByEmail(email);
        if (optional.isEmpty()) {
            appUser.setEmail(email);
            appUser.setState(BASIC_STATE);
            appUserDao.save(appUser);

            var cryptoUserId = cryptoTool.hashOf(appUser.getId());
            var response = sendRequestToMailService(cryptoUserId, email);
            if (response.getStatusCode() != HttpStatus.OK) {
                var msg = String.format("Отправка эл. письма на почту %s не удалась", email);
                log.error(msg);
                appUser.setEmail(null);
                appUserDao.save(appUser);
                return msg;
            }
            return "Вам на почту было отправлено письмо с подтверждением регистрации." +
                    "Перейдите по ссылке в письме, чтобы завершить регистрацию";

        } else {
            return "Введенный Вами email уже используется. Введите корректный email."
                    + "Для отмены регистрации введите /cancel";
        }
    }

    private ResponseEntity<String> sendRequestToMailService(String cryptoUserId, String email) {
        var restTemplate = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var mailParams = MailParams.builder()
                .emailTo(email)
                .id(cryptoUserId)
                .build();
        var request = new HttpEntity<MailParams>(mailParams, headers);
        return restTemplate.exchange(mailServiceUri, HttpMethod.POST, request, String.class);
    }
}
