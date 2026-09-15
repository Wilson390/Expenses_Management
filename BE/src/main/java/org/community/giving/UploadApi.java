package org.community.giving;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;
@RestController @RequestMapping({"/api", "/api/v1"}) public class UploadApi {
 @Value("${app.upload-dir}") String uploadDir;
 @PostMapping("/admin/uploads") public Map<String,String> upload(@RequestParam("file") MultipartFile file) throws IOException {
  Api.require(file!=null && !file.isEmpty(),"File required");
  var type=file.getContentType();
  Api.require(type!=null && type.startsWith("image/"),"Only image files are allowed");
  var ext=switch(type){case "image/png"->".png";case "image/webp"->".webp";default->".jpg";};
  var filename=UUID.randomUUID()+ext;
  var dir=Paths.get(uploadDir);
  Files.createDirectories(dir);
  Files.copy(file.getInputStream(),dir.resolve(filename),StandardCopyOption.REPLACE_EXISTING);
  return Map.of("filename",filename);
 }
 @ExceptionHandler(MaxUploadSizeExceededException.class) @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE) public Map<String,String> tooLarge(){return Map.of("message","Image is too large. Please choose a smaller photo.");}
}
