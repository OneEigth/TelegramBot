package oneEight.service;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface UpdateProducer {
    void produceUpdate(String rabbitQueue, Update update);
}
