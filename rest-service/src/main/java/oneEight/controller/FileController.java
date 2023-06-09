package oneEight.controller;

import lombok.extern.log4j.Log4j;
import oneEight.service.FileService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Log4j
@RequestMapping("/file")
@RestController
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @RequestMapping(method = RequestMethod.GET,value = "/doc")
    public ResponseEntity<?> getDoc(@RequestParam("id") String id) {
        //TODO: для формирования badRequest добавить controllerAdvice
        var doc = fileService.getDocument(id);
        if (doc == null) {
            return ResponseEntity.badRequest().build();
        }
        var binaryContent = doc.getBinaryContent();

        var fileSystemResource = fileService.getFileSystemResource(binaryContent);
        if (fileSystemResource == null) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getMimeType()))
                .header("Content-Disposition", "attachment; filename=" + doc.getDocName())
                .body(fileSystemResource);
    }

    @RequestMapping(method = RequestMethod.GET,value = "/photo")
    public ResponseEntity<?> getPhoto(@RequestParam("id") String id) {
        //TODO: для формирования badRequest добавить controllerAdvice
        var photo = fileService.getPhoto(id);
        if (photo == null) {
            return ResponseEntity.badRequest().build();
        }
        var binaryContent = photo.getBinaryContent();

        var fileSystemResource = fileService.getFileSystemResource(binaryContent);
        if (fileSystemResource == null) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header("Content-Disposition", "attachment;")
                .body(fileSystemResource);
    }
    }

