package org.community.giving;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
@RestController @RequestMapping({"/api", "/api/v1"}) public class EnquiryApi {
 final EnquiryRepository enquiries;
 public EnquiryApi(EnquiryRepository e){enquiries=e;}
 @PostMapping("/public/contact") public Enquiry submit(@RequestBody Enquiry e){
  Api.require(e.name!=null && !e.name.isBlank() && e.name.length()<=150,"Name is required");
  Api.require(e.contact!=null && !e.contact.isBlank() && e.contact.length()<=180,"Phone or email is required");
  Api.require(e.message!=null && !e.message.isBlank() && e.message.length()<=1000,"Message is required");
  var save=new Enquiry();save.name=e.name.trim();save.contact=e.contact.trim();save.message=e.message.trim();
  return enquiries.save(save);
 }
 @GetMapping("/admin/contact") public List<Enquiry> list(){return enquiries.findAllByOrderByCreatedAtDesc();}
 @PutMapping("/admin/contact/{id}/read") public Enquiry markRead(@PathVariable Long id,@RequestParam boolean read){var e=enquiries.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));e.read=read;return enquiries.save(e);}
}
