package oneEight.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface AnswerConsumer {
    void consumeAnswer(SendMessage sendMessage);
}
