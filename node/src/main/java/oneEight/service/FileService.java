package oneEight.service;

import oneEight.entity.AppDocument;
import oneEight.entity.AppPhoto;
import oneEight.service.enums.LinkType;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface FileService {
    AppDocument processDoc(Message telegramMessage);
    AppPhoto processPhoto(Message telegramMessage);
    String generateLink(Long docId, LinkType linkType);
}
