package oneEight.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;


public interface ProducerService {
    void producerAnswer(SendMessage sendMessage);
}
