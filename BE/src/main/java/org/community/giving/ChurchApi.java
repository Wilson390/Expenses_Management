package org.community.giving;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
@RestController @RequestMapping({"/api", "/api/v1"}) public class ChurchApi {
 final ChurchRepository churches; final BankAccountRepository accounts;
 public ChurchApi(ChurchRepository c,BankAccountRepository a){churches=c;accounts=a;}
 @GetMapping("/churches") public List<Church> list(){return churches.findAll();}
 @GetMapping("/public/church") public Church publicChurch(){return churches.findAll().stream().findFirst().orElse(null);}
 @PostMapping("/admin/churches") public Church create(@RequestBody Church c){Api.require(c.name!=null && !c.name.isBlank(),"Church name is required");c.id=null;return churches.save(c);}
 @PutMapping("/admin/churches/{id}") public Church update(@PathVariable Long id,@RequestBody Church c){Api.require(c.name!=null && !c.name.isBlank(),"Church name is required");Api.require(churches.existsById(id),"Church not found");c.id=id;return churches.save(c);}
 @GetMapping("/bank-accounts") public List<BankAccount> bankAccounts(){return accounts.findByActiveTrue();}
 @PostMapping("/admin/bank-accounts") public BankAccount createAccount(@RequestBody BankAccount b){
  Api.require(b.bankName!=null && !b.bankName.isBlank(),"Bank name is required");
  Api.require(b.accountNumber!=null && !b.accountNumber.isBlank(),"Account number is required");
  b.id=null;b.active=true;return accounts.save(b);
 }
 @PutMapping("/admin/bank-accounts/{id}/status") public BankAccount setAccountStatus(@PathVariable Long id,@RequestParam boolean active){var b=accounts.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Bank account not found"));b.active=active;return accounts.save(b);}
}
