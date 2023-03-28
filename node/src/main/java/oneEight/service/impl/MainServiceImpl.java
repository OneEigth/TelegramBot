package oneEight.service.impl;

import oneEight.dao.AppUserDao;
import oneEight.dao.RawDataDao;
import oneEight.entity.AppUser;
import oneEight.entity.RawData;
import lombok.AllArgsConstructor;
import oneEight.service.MainService;
import oneEight.service.ProducerService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import static oneEight.entity.enums.UserState.BASIC_STATE;

@Service
@AllArgsConstructor
public class MainServiceImpl implements MainService {
    private final RawDataDao rawDataDao;
    private final ProducerService producerService;
    private final AppUserDao appUserDao;

    @Override
    public void processTextMessage(Update update) {
        saveRawData(update);
        var textMessage = update.getMessage();
        var telegramUser = textMessage.getFrom();
        var appUser = findOrSaveAppUser(telegramUser);

        var message = update.getMessage();
        var sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText("Hello from Node, " + message.getFrom().getFirstName() + "!");
        producerService.producerAnswer(sendMessage);
    }
    private AppUser findOrSaveAppUser(User telegramUser) {
                    AppUser persistentAppUser = appUserDao.findAppUserByTelegramId(telegramUser.getId());
                    if (persistentAppUser == null) {
                        AppUser transientAppUser = AppUser.builder()
                                .telegramId(telegramUser.getId())
                                .firstName(telegramUser.getFirstName())
                                .lastName(telegramUser.getLastName())
                                .userName(telegramUser.getUserName())
                                //TODO изменить значение по умолчанию после добавления регистрации
                                .isActive(true)
                                .state(BASIC_STATE)
                                .build();
                        return appUserDao.save(transientAppUser);
                    }
                    return persistentAppUser;
    }

    private void saveRawData(Update update) {
        RawData rawData = RawData.builder()
                .event(update)
                .build();
        rawDataDao.save(rawData);
    }
}
