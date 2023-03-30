package oneEight.service.impl;

import oneEight.dao.AppUserDao;
import oneEight.service.UserActivationService;
import oneEight.utils.CryptoTool;
import org.springframework.stereotype.Service;

@Service
public class UserActivationServiceImpl implements UserActivationService {
    private final AppUserDao appUserDao;
    private final CryptoTool cryptoTool;

    public UserActivationServiceImpl(AppUserDao appUserDao, CryptoTool cryptoTool) {
        this.appUserDao = appUserDao;
        this.cryptoTool = cryptoTool;
    }

    @Override
    public boolean activation(String cryptoUserId) { //метод активации
        var userId = cryptoTool.idOf(cryptoUserId); //дешефруем полученный id
        var optional=appUserDao.findById(userId); //ищем пользователя по id
        if (optional.isPresent()) { //если нашли
            var user = optional.get();
            user.setIsActive(true); //активируем
            appUserDao.save(user);
            return true;// и возвращаем true в контроллер
        }
        return false;
    }

}
