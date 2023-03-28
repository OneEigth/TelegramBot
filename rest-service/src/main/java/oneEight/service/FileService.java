package oneEight.service;

import oneEight.entity.AppDocument;
import oneEight.entity.AppPhoto;
import oneEight.entity.BinaryContent;
import org.springframework.core.io.FileSystemResource;

public interface FileService {
    AppDocument getDocument(String id);
    AppPhoto getPhoto(String id);
    FileSystemResource getFileSystemResource(BinaryContent binaryContent);


}
