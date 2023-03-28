package oneEight.service.impl;

import lombok.extern.log4j.Log4j;
import oneEight.dao.AppDocumentDao;
import oneEight.dao.AppPhotoDao;
import oneEight.dao.BinaryContentDao;
import oneEight.entity.AppDocument;
import oneEight.entity.AppPhoto;
import oneEight.entity.BinaryContent;
import oneEight.exceptions.UploadFileException;
import oneEight.service.FileService;
import oneEight.service.enums.LinkType;
import oneEight.utils.CryptoTool;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

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
    @Value("${link.address}")
    private String linkAdress;
    private final AppDocumentDao appDocumentDao;
    private final AppPhotoDao appPhotoDao;
    private final BinaryContentDao binaryContentDao;
    private final CryptoTool cryptoTool;

    public FileServiceImpl(AppDocumentDao appDocumentDao, AppPhotoDao appPhotoDao, BinaryContentDao binaryContentDao, CryptoTool cryptoTool) {
        this.appDocumentDao = appDocumentDao;
        this.appPhotoDao = appPhotoDao;
        this.binaryContentDao = binaryContentDao;
        this.cryptoTool = cryptoTool;
    }

    @Override
    public AppDocument processDoc(Message telegramMessage) {
        Document telegramDoc = telegramMessage.getDocument();
        String fileId = telegramDoc.getFileId();
        ResponseEntity<String> response = getFilePath(fileId);
        if (response.getStatusCode()== HttpStatus.OK) {
            BinaryContent persistedBinaryContent = getPersistentBinaryContent(response);
            AppDocument transientAppDocument = buildTransientAppDoc(telegramDoc, persistedBinaryContent);
            return appDocumentDao.save(transientAppDocument);
        }else {
            throw new UploadFileException("Bad response by telegram service"+response);
        }
    }
    @Override
    public AppPhoto processPhoto(Message telegramMessage) {
        var photoSizeCount = telegramMessage.getPhoto().size();
        var photoSizeIndex = photoSizeCount > 1 ? telegramMessage.getPhoto().size() - 1 : 0;
        PhotoSize telegramPhoto = telegramMessage.getPhoto().get(photoSizeIndex);
        String fileId = telegramPhoto.getFileId();
        ResponseEntity<String> response = getFilePath(fileId);
        if (response.getStatusCode()== HttpStatus.OK) {
            BinaryContent persistedBinaryContent = getPersistentBinaryContent(response);
            AppPhoto transientAppPhoto = buildTransientAppPhoto(telegramPhoto, persistedBinaryContent);
            return appPhotoDao.save(transientAppPhoto);
        }else {
            throw new UploadFileException("Bad response by telegram service"+response);
        }
    }




    private BinaryContent getPersistentBinaryContent(ResponseEntity<String> response) {
        String filePath = getFilePath(response);
        byte[]fileInByte = downloadFIle(filePath);
        BinaryContent transientBinaryContent = BinaryContent.builder()
                .fileAsArrayOfBytes(fileInByte)
                .build();
        return binaryContentDao.save(transientBinaryContent);

    }

    private String getFilePath(ResponseEntity<String> response) {
        JSONObject jsonObject = new JSONObject(response.getBody());
        return String.valueOf(jsonObject
                .getJSONObject("result")
                .get("file_path"));

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
    private AppPhoto buildTransientAppPhoto(PhotoSize telegramPhoto, BinaryContent persistedBinaryContent) {
        return AppPhoto.builder()
                .telegramFileId(telegramPhoto.getFileId())
                .fileSize(telegramPhoto.getFileSize())
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
    @Override
    public String generateLink(Long docId, LinkType linkType) {
        var hash = cryptoTool.hashOf(docId);
        return "http://"+linkAdress+"/"+linkType+"?id="+hash;
    }
}
