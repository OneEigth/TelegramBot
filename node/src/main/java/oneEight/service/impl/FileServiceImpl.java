package oneEight.service.impl;

import lombok.extern.log4j.Log4j;
import oneEight.dao.AppDocumentDao;
import oneEight.dao.BinaryContentDao;
import oneEight.entity.AppDocument;
import oneEight.entity.BinaryContent;
import oneEight.exceptions.UploadFileException;
import oneEight.service.FileService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

@Service

@Log4j
public class FileServiceImpl implements FileService {
    @Value("${token}")
    private String token;
    @Value("${service.file_info.uri}")
    private String fileInfoUri;
    @Value("${service.file_storage.uri}")
    private String fileStorageUri;
    private final AppDocumentDao appDocumentDao;
    private final BinaryContentDao binaryContentDao;

    public FileServiceImpl(AppDocumentDao appDocumentDao, BinaryContentDao binaryContentDao) {
        this.appDocumentDao = appDocumentDao;
        this.binaryContentDao = binaryContentDao;
    }

    @Override
    public AppDocument processDoc(Message telegramMessage) {
        String fileId = telegramMessage.getDocument().getFileId();
        ResponseEntity<String> response = getFilePath(fileId);
        if (response.getStatusCode()== HttpStatus.OK) {
            JSONObject jsonObject = new JSONObject(response.getBody());
            String filePath = String.valueOf(jsonObject
                    .getJSONObject("result")
                    .get("file_path"));
            byte[]fileInByte = downloadFIle(filePath);
            BinaryContent transientBinaryContent = BinaryContent.builder()
                    .fileAsArrayOfBytes(fileInByte)
                    .build();
            BinaryContent persistedBinaryContent = binaryContentDao.save(transientBinaryContent);
            Document telegramDoc = telegramMessage.getDocument();
            AppDocument transientAppDocument = buildTransientAppDoc(telegramDoc, persistedBinaryContent);
            return appDocumentDao.save(transientAppDocument);
        }else {
            throw new UploadFileException("Bad response by telegram service"+response);
        }
    }

    private AppDocument buildTransientAppDoc(Document telegramDoc, BinaryContent persistedBinaryContent) {
        return AppDocument.builder()
                .telegramFileId(telegramDoc.getFileId())
                .docName(telegramDoc.getFileName())
                .fileSize(telegramDoc.getFileSize())
                .mimeType(telegramDoc.getMimeType())
                .binaryContent(persistedBinaryContent)
                .build();
    }

    private byte[] downloadFIle(String filePath) {
        String fillUri=fileStorageUri.replace("{token}",token)
                .replace("{file_path}",filePath);
        URL urlObj = null;
        try {
            urlObj = new URL(fillUri);
        } catch (MalformedURLException e) {
            throw new UploadFileException(e);
        }
        //TODO подумай над оптимизацией
        try(InputStream is = urlObj.openStream()) {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new UploadFileException(urlObj.toExternalForm(),e);
        }
    }

    private ResponseEntity<String> getFilePath(String fileId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> request = new HttpEntity<>(headers);
        return restTemplate.exchange(
                fileInfoUri,
                HttpMethod.GET,
                request,
                String.class,
                token, fileId
        );

    }
}
