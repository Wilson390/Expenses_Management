package org.community.giving;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.security.Principal;
import java.util.*;
@RestController @RequestMapping({"/api", "/api/v1"}) public class DayEndApi {
 final DonationRepository donations; final DayEndClosingRepository closings; final Api api; final BankAccountRepository bankAccounts;
 public DayEndApi(DonationRepository d,DayEndClosingRepository c,Api a,BankAccountRepository b){donations=d;closings=c;api=a;bankAccounts=b;}
 Map<String,Object> summary(LocalDate date){
  var cash=donations.findByReceivedDateAndPaymentType(date,"CASH");
  var categoryTotals=new TreeMap<String,BigDecimal>();
  var categoryMembers=new TreeMap<String,Set<Long>>();
  cash.forEach(d->{categoryTotals.merge(d.category,d.amount,BigDecimal::add);if(d.memberId!=null)categoryMembers.computeIfAbsent(d.category,k->new HashSet<>()).add(d.memberId);});
  var lineItems=new ArrayList<Map<String,Object>>();
  categoryTotals.forEach((category,amount)->{var members=categoryMembers.get(category);var item=new HashMap<String,Object>();item.put("label",category);item.put("amount",amount);if(members!=null && !members.isEmpty())item.put("count",members.size());lineItems.add(item);});
  var denomTotals=new TreeMap<Integer,Integer>(Comparator.reverseOrder());
  cash.forEach(d->d.denominations.forEach((v,q)->denomTotals.merge(v,q,Integer::sum)));
  var denominations=new ArrayList<Map<String,Object>>();
  denomTotals.forEach((value,qty)->denominations.add(Map.of("value",value,"quantity",qty,"subtotal",BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(qty)))));
  BigDecimal total=categoryTotals.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add);
  var closing=closings.findByClosingDate(date);
  var result=new HashMap<String,Object>();
  result.put("date",date);result.put("total",total);result.put("lineItems",lineItems);result.put("denominations",denominations);
  result.put("status",closing.map(c->c.status).orElse("PENDING"));
  result.put("approvedBy",closing.map(c->c.approvedBy).orElse(null));result.put("approvedAt",closing.map(c->c.approvedAt).orElse(null));
  result.put("verifiedBy",closing.map(c->c.verifiedBy).orElse(null));result.put("verifiedAt",closing.map(c->c.verifiedAt).orElse(null));
  result.put("depositedBy",closing.map(c->c.depositedBy).orElse(null));result.put("depositedAt",closing.map(c->c.depositedAt).orElse(null));result.put("bankReference",closing.map(c->c.bankReference).orElse(null));
  result.put("bankAccountId",closing.map(c->c.bankAccountId).orElse(null));
  result.put("bankAccountLabel",closing.flatMap(c->c.bankAccountId==null?Optional.<BankAccount>empty():bankAccounts.findById(c.bankAccountId)).map(b->b.bankName+" · "+b.accountNumber).orElse(null));
  return result;
 }
 @GetMapping("/admin/day-end/{date}") public Map<String,Object> get(@PathVariable String date){return summary(LocalDate.parse(date));}
 @PostMapping("/admin/day-end/{date}/approve") @Transactional public Map<String,Object> approve(@PathVariable String date,Principal p){
  var d=LocalDate.parse(date);var closing=closings.findByClosingDate(d).orElseGet(()->{var c=new DayEndClosing();c.closingDate=d;return c;});
  Api.require(closing.status.equals("PENDING"),"This day has already been "+closing.status.toLowerCase()+".");
  closing.status="APPROVED";closing.approvedBy=p.getName();closing.approvedAt=Instant.now();closings.save(closing);
  return summary(d);
 }
 @PostMapping("/admin/day-end/{date}/verify") @Transactional public Map<String,Object> verify(@PathVariable String date,Principal p){
  var d=LocalDate.parse(date);var closing=closings.findByClosingDate(d).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Approve this day before verifying."));
  Api.require(closing.status.equals("APPROVED"),closing.status.equals("VERIFIED")?"This day has already been verified.":"Approve this day before verifying.");
  closing.status="VERIFIED";closing.verifiedBy=p.getName();closing.verifiedAt=Instant.now();closings.save(closing);
  return summary(d);
 }
 public record DepositRequest(Long bankAccountId,String reference){}
 @PostMapping("/admin/day-end/{date}/deposit") @Transactional public Map<String,Object> deposit(@PathVariable String date,@RequestBody DepositRequest body,Principal p){
  var d=LocalDate.parse(date);var closing=closings.findByClosingDate(d).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Verify this day before recording a bank deposit."));
  Api.require(closing.status.equals("VERIFIED"),closing.status.equals("DEPOSITED")?"This day's cash has already been deposited.":"Verify this day before recording a bank deposit.");
  Api.require(body.bankAccountId()!=null,"Select which bank account this cash was deposited into");
  Api.require(body.reference()!=null && !body.reference().isBlank(),"Bank deposit reference is required");
  closing.status="DEPOSITED";closing.depositedBy=p.getName();closing.depositedAt=Instant.now();closing.bankAccountId=body.bankAccountId();closing.bankReference=body.reference().trim();closings.save(closing);
  return summary(d);
 }
}
