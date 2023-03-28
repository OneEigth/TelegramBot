package oneEight.service.impl;

import lombok.extern.log4j.Log4j;
import oneEight.dao.AppUserDao;
import oneEight.dao.RawDataDao;
import oneEight.entity.AppDocument;
import oneEight.entity.AppUser;
import oneEight.entity.RawData;
import oneEight.exceptions.UploadFileException;
import oneEight.service.FileService;
import oneEight.service.MainService;
import oneEight.service.ProducerService;
import oneEight.service.enums.ServiceCommands;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;


import static oneEight.entity.enums.UserState.BASIC_STATE;
import static oneEight.entity.enums.UserState.WAITING_FOR_EMAIL_STATE;
import static oneEight.service.enums.ServiceCommands.*;

@Service

@Log4j
class MainServiceImpl implements MainService {
    private final RawDataDao rawDataDao;
    private final ProducerService producerService;
    private final AppUserDao appUserDao;
    private final FileService fileService;

    MainServiceImpl(RawDataDao rawDataDao, ProducerService producerService, AppUserDao appUserDao, FileService fileService) {
        this.rawDataDao = rawDataDao;
        this.producerService = producerService;
        this.appUserDao = appUserDao;
        this.fileService = fileService;
    }

    @Override
    public void processTextMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var userState = appUser.getState();
        var text = update.getMessage().getText();
        var output = "";

        var serviceCommand = ServiceCommands.fromString(text);
        if(CANCEL.equals(serviceCommand)){
            output=cancelProcess(appUser);
        }else if(BASIC_STATE.equals(userState)){
            output=processServiceCommand(appUser, text);
        }else if(WAITING_FOR_EMAIL_STATE.equals(userState)) {
            //TODO добавить обработку email
        }else {
            log.error("Unknown user state: " + userState);
            output = "Неизвестная ошибка! введите /сancel и начните заново";
        }

        var chatId = update.getMessage().getChatId();
        sendAnswer(chatId, output);
        

    }

    @Override
    public void processDocMessage(Update update) {
        saveRawData(update);
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        if(isNotAllowToSendContent(chatId, appUser)) {
            return;
        }
        try{
            AppDocument doc = fileService.processDoc(update.getMessage());
            var answer = "Ваш документ успешно получен" + ", для скачивания нажмите на ссылку: http://test.kz/get-doc/";
            sendAnswer(chatId, answer);
        }catch (UploadFileException e){
            log.error(e);
            String error = "Не удалось загрузить документ! Попробуйте еще раз";
            sendAnswer(chatId, error);
        }
    }

    @Override
    public void processPhotoMessage(Update update) {
        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        if(isNotAllowToSendContent(chatId, appUser)) {
            return;
        }
        // TODO добавить сохранение фото
        var answer = "Ваше фото успешно получен, для скачивания нажмите на ссылку: http://test.kz/get-photo/888";
        sendAnswer(chatId, answer);
    }
    private boolean isNotAllowToSendContent(Long chatId, AppUser appUser) {
        var userState = appUser.getState();
        if(!appUser.getIsActive()) {
            var error = "Вы не зарегистрированы или активируйте учетную запись! Для регистрации введите /registration";
            sendAnswer(chatId, error);
            return true;
        }else if(!BASIC_STATE.equals(userState)) {
            var error = "Вы не можете отправлять контент в данный момент! Для отмены введите /cancel";
            sendAnswer(chatId, error);
            return true;
        }
        return false;
    }
    private void sendAnswer(Long chatId, String output) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(output);
        producerService.producerAnswer(sendMessage);
    }

    private String processServiceCommand(AppUser appUser, String cmd) {
        var serviceCommand = ServiceCommands.fromString(cmd);
        if(REGISTRATION.equals(serviceCommand)){
            // TODO добавить обработку регистрации
            return "Регистрация не доступна";
        }else if(HELP.equals(serviceCommand)){
            return help();
        }else if(START.equals(serviceCommand)){
            return "Привет! Чтобы узнать список доступных команд введите /help";
        }else {
            return "Неизвестная команда! Чтобы узнать список доступных команд введите /help";
        }
    }

    private String help() {
        return "Список доступных команд:\n" +
                "/help - список доступных команд\n" +
                "/registration - регистрация\n" +
                "/cancel - отмена текущей команды";
    }

    private String cancelProcess(AppUser appUser) {
        appUser.setState(BASIC_STATE);
        appUserDao.save(appUser);
        return "Команда отменена";
    }

    private AppUser findOrSaveAppUser(Update update) {
        var telegramUser = update.getMessage().getFrom();
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



