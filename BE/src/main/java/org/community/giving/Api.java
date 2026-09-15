package org.community.giving;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.security.Principal;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.io.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.community.giving.dto.PastorDto;
@RestController @RequestMapping({"/api", "/api/v1"}) public class Api {
 final MemberRepository members; final PastorRepository pastors; final AccountRepository accounts; final DonationRepository donations; final PasswordEncoder encoder; final MasterRepository masterRepository; final DayEndClosingRepository dayEndClosings;
 static final List<String> CATEGORIES=List.of("Worship offering","Monthly pledge","Graveyard","Birthday offering","Marriage anniversary","Thanksgiving","Donation","Physical donation","Lent cottage prayer","Lent box","Easter envelope","Church day envelope","Christmas envelope","New Year envelope","Harvest envelope","Harvest stall offering","Sunday school","Other receipts","Election form","Carol pocket collection");
 static final List<String> EVENTS=List.of("General","Harvest festival","Church day","Christmas","New Year","Good Friday","Sunday Worship Offerings");
 static final List<Integer> VALUES=List.of(1,2,5,10,20,50,100,200,500,1000,2000);
 public Api(MemberRepository m,PastorRepository p,AccountRepository a,DonationRepository d,PasswordEncoder e,MasterRepository master,DayEndClosingRepository dayEnd){masterRepository=master;members=m;pastors=p;accounts=a;donations=d;encoder=e;dayEndClosings=dayEnd;}
 static void require(boolean ok,String message){if(!ok)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 Account account(Principal p){var a=accounts.findByUsername(p.getName()).orElseThrow();if(a.memberId!=null && !members.findById(a.memberId).map(m->m.active).orElse(false))throw new ResponseStatusException(HttpStatus.FORBIDDEN);return a;}
 @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken t){return Map.of("token",t.getToken(),"headerName",t.getHeaderName());}
 @GetMapping("/me") public Map<String,Object> me(Principal p){var a=account(p);return Map.of("username",a.username,"role",a.role,"name",a.memberId==null?"Administrator":members.findById(a.memberId).orElseThrow().name);}
 @GetMapping("/masters") public Map<String,Object> masters(){
  var amountTypes=masterRepository.findByTypeOrderByIdAsc("amount_type").stream().map(e->e.code).filter(Objects::nonNull).toList();
  var eventTypes=masterRepository.findByTypeOrderByIdAsc("events_type").stream().map(e->e.code).filter(Objects::nonNull).toList();
  var denominationTypes=masterRepository.findByTypeOrderByIdAsc("denomination_type").stream().map(e->Integer.valueOf(e.code)).toList();
  var paymentTypes=masterRepository.findByTypeOrderByIdAsc("payment_type").stream().map(e->e.code).filter(Objects::nonNull).toList();
  var genders=masterRepository.findByTypeOrderByIdAsc("gender").stream().map(e->e.code).filter(Objects::nonNull).toList();
  var electionCategories=masterRepository.findByTypeOrderByIdAsc("election_category").stream().map(e->e.code).filter(Objects::nonNull).toList();
  var electionSubCategories=masterRepository.findByTypeOrderByIdAsc("election_sub_category").stream().map(e->e.code).filter(Objects::nonNull).toList();
  if(genders.isEmpty()) genders=masterRepository.findByGroupnameOrderByIdAsc("gender").stream().map(e->e.value).filter(Objects::nonNull).toList();
  return Map.of("electionCategories",electionCategories,"electionSubCategories",electionSubCategories,"genders",genders,"categories",amountTypes.isEmpty()?CATEGORIES:amountTypes,"events",eventTypes.isEmpty()?EVENTS:eventTypes,"denominations",denominationTypes.isEmpty()?VALUES:denominationTypes,"paymentTypes",paymentTypes.isEmpty()?List.of("CASH","ONLINE","CHEQUE"):paymentTypes);
 }
 @GetMapping("/admin/members") public List<Member> members(){return members.findAll();}
 @GetMapping("/pastors") public List<PastorDto> pastorsPublic(){return pastors.findAll().stream().map(this::pastorDto).toList();}
 @GetMapping("/admin/pastors") public List<PastorDto> pastorsAdmin(){return pastorsPublic();}
 @PostMapping("/admin/pastors") @Transactional public PastorDto createPastor(@RequestBody PastorDto dto, Principal principal){var p=from(dto);validatePastor(p);p.id=null;p.createdBy=principal.getName();if(p.isBishop)clearOtherBishops(null);return pastorDto(pastors.save(p));}
 @PutMapping("/admin/pastors/{id}") @Transactional public PastorDto updatePastor(@PathVariable Long id,@RequestBody PastorDto dto,Principal principal){var p=from(dto);validatePastor(p);require(pastors.existsById(id),"Pastor not found");p.id=id;p.updatedBy=principal.getName();if(p.isBishop)clearOtherBishops(id);return pastorDto(pastors.save(p));}
 private void clearOtherBishops(Long exceptId){pastors.findAll().stream().filter(x->x.isBishop && !x.id.equals(exceptId)).forEach(x->{x.isBishop=false;pastors.save(x);});}
 @PutMapping("/admin/pastors/{id}/status") public PastorDto setPastorStatus(@PathVariable Long id,@RequestParam boolean active,Principal principal){var p=pastors.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Pastor not found"));p.active=active;p.updatedBy=principal.getName();return pastorDto(pastors.save(p));}
 private Pastor from(PastorDto d){var p=new Pastor();p.id=d.id();p.name=d.name();p.mobileNo=d.mobileNo();p.email=d.email();p.dob=d.dob();p.doj=d.doj();p.dor=d.dor();p.gender=d.gender();p.active=d.active();p.isBishop=d.isBishop();p.profilePicImage=d.profilePicImage();return p;}
 private PastorDto pastorDto(Pastor p){return new PastorDto(p.id,p.name,p.mobileNo,p.email,p.dob,p.doj,p.dor,p.gender,p.active,p.isBishop,p.profilePicImage);}
 void validatePastor(Pastor p){require(p!=null&&p.name!=null&&!p.name.isBlank()&&p.name.length()<=150,"Pastor name is required");require(p.mobileNo!=null&&p.mobileNo.matches("[+0-9 ()-]{7,20}"),"Valid mobile number required");require(p.id==null?!pastors.existsByMobileNo(p.mobileNo):!pastors.existsByMobileNoAndIdNot(p.mobileNo,p.id),"Mobile number already belongs to another pastor");if(p.email!=null&&!p.email.isBlank())require(p.email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"),"Valid email required");if(p.gender!=null&&!p.gender.isBlank()){p.gender=p.gender.trim().toUpperCase(Locale.ROOT);require(masterRepository.existsByGroupnameAndValue("gender",p.gender),"Select a valid gender from the master list");}}
 public record MemberInput(Member member,String username,String password){}
 @PostMapping("/admin/members") @Transactional public Member create(@RequestBody MemberInput input){validate(input.member());require(input.username()!=null && input.username().matches("[a-zA-Z0-9@._+-]{3,80}"),"Username must be 3–80 valid characters");require(input.password()!=null && input.password().length()>=12 && input.password().length()<=72,"Password must be 12–72 characters");require(accounts.findByUsername(input.username()).isEmpty(),"Username already exists");var m=input.member();m.id=null;members.save(m);var a=new Account();a.username=input.username();a.passwordHash=encoder.encode(input.password());a.role="MEMBER";a.memberId=m.id;accounts.save(a);return m;}
 void validate(Member m){require(m!=null && m.name!=null && !m.name.isBlank() && m.name.length()<=150,"Member name is required (max 150 characters)");require(m.mobileNo!=null && m.mobileNo.matches("[+0-9 ()-]{7,20}"),"Valid mobile number required");require(m.id==null?!members.existsByMobileNo(m.mobileNo):!members.existsByMobileNoAndIdNot(m.mobileNo,m.id),"Mobile number already belongs to another member");if(m.gender!=null && !m.gender.isBlank()){m.gender=m.gender.trim().toUpperCase(Locale.ROOT);require(masterRepository.existsByGroupnameAndValue("gender",m.gender),"Select a valid gender from the master list");}else{m.gender=null;}}
 @PutMapping("/admin/members/{id}") public Member update(@PathVariable Long id,@RequestBody Member m){validate(m);if(!members.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND);m.id=id;return members.save(m);}
 @PutMapping("/admin/members/{id}/status") public Member setMemberStatus(@PathVariable Long id,@RequestParam boolean active){var m=members.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Member not found"));m.active=active;return members.save(m);}
 @GetMapping("/donations") public List<Donation> list(Principal p){var a=account(p);return a.role.equals("ADMIN")?donations.findAllByOrderByReceivedDateDesc():donations.findByMemberIdOrderByReceivedDateDesc(a.memberId);}
 @PostMapping("/admin/donations") @Transactional public Donation createDonation(@RequestBody Donation d,Principal p){
  Member m=null;if(d.memberId!=null){m=(Member)org.hibernate.Hibernate.unproxy(members.findById(d.memberId).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Member does not exist")));require(m.active,"Member is inactive");}
  var amountMaster=masterRepository.findByTypeOrderByIdAsc("amount_type"); var eventMaster=masterRepository.findByTypeOrderByIdAsc("events_type");
  require(amountMaster.stream().anyMatch(e->e.code.equalsIgnoreCase(d.category)),"Select a valid amount type");
  require(eventMaster.stream().anyMatch(e->e.code.equalsIgnoreCase(d.event)),"Select a valid event type");
  d.category=amountMaster.stream().filter(e->e.code.equalsIgnoreCase(d.category)).findFirst().orElseThrow().code;
  d.event=eventMaster.stream().filter(e->e.code.equalsIgnoreCase(d.event)).findFirst().orElseThrow().code;
  require(masterRepository.findByTypeOrderByIdAsc("payment_type").stream().anyMatch(e->e.code.equalsIgnoreCase(d.paymentType)),"Invalid payment type");
  require(d.receivedDate!=null && !d.receivedDate.isAfter(LocalDate.now()),"Received date cannot be in the future");
  require(dayEndClosings.findByClosingDate(d.receivedDate).map(c->c.status.equals("PENDING")).orElse(true),"This day has already been closed for entries.");
  try{YearMonth.parse(d.contributionMonth);}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Contribution month must be YYYY-MM");}
  require(d.amount!=null && d.amount.signum()>=0 && d.amount.scale()<=2 && d.amount.compareTo(new BigDecimal("999999999999.99"))<=0,"Invalid amount");
  boolean physical=d.category.toLowerCase(Locale.ROOT).startsWith("physical donation");require(physical?d.description!=null && !d.description.isBlank():d.amount.signum()>0,"Amount or physical donation description required");
  var allowedDenominations=masterRepository.findByTypeOrderByIdAsc("denomination_type").stream().map(e->Integer.valueOf(e.code)).toList();
  require(d.denominations!=null && d.denominations.entrySet().stream().allMatch(e->allowedDenominations.contains(e.getKey()) && e.getValue()!=null && e.getValue()>=0 && e.getValue()<=1000000),"Invalid denomination quantity");
  BigDecimal total=d.denominations.entrySet().stream().map(e->BigDecimal.valueOf(e.getKey()).multiply(BigDecimal.valueOf(e.getValue()))).reduce(BigDecimal.ZERO,BigDecimal::add);
  if(d.paymentType.equals("CASH"))require(total.compareTo(d.amount)==0,"Cash denominations must equal the received amount");else {require(d.denominations.isEmpty(),"Only cash can have denominations");require(d.reference!=null && !d.reference.isBlank(),"Payment reference required");}
  d.id=null;d.memberId=m!=null?m.id:null;d.memberName=m!=null?m.name:"Congregation (no specific member)";d.createdBy=p.getName();d.createdAt=Instant.now();return donations.save(d);
 }
 @GetMapping({"/donations/{id}/receipt","/donations/{id}/invoice"}) public ResponseEntity<byte[]> receipt(@PathVariable Long id,Principal p) throws IOException {
  var a=account(p);var d=donations.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));if(!a.role.equals("ADMIN") && !Objects.equals(a.memberId,d.memberId))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  try(var doc=new PDDocument();var out=new ByteArrayOutputStream()){var page=new PDPage();doc.addPage(page);try(var content=new PDPageContentStream(doc,page)){
   try(var logoStream=Api.class.getResourceAsStream("/logo-wide.png")){if(logoStream!=null){var logo=PDImageXObject.createFromByteArray(doc,logoStream.readAllBytes(),"church-logo");content.drawImage(logo,50,680,300,83);}}
   content.setStrokingColor(120/255f,40/255f,40/255f);content.setLineWidth(1.5f);content.addRect(40,40,532,740);content.stroke();content.setNonStrokingColor(245/255f,235/255f,225/255f);content.addRect(40,650,532,30);content.fill();content.setNonStrokingColor(0,0,0);
   content.beginText();content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),12);content.setLeading(20);content.newLineAtOffset(50,635);
   var lines=new ArrayList<>(List.of("COMMUNITY GIVING","DONATION INVOICE / RECEIPT","========================================","Receipt No: DON-"+String.format("%06d",d.id),"Member: "+d.memberName,"Received date: "+d.receivedDate,"Contribution month: "+d.contributionMonth,"----------------------------------------","Purpose: "+d.category,"Event: "+d.event,"Payment method: "+d.paymentType,"Payment reference: "+Objects.toString(d.reference,""),"Description: "+Objects.toString(d.description,""),"Amount received: INR "+d.amount,"----------------------------------------","DENOMINATIONS (CASH)","Value (INR)     Quantity       Total (INR)"));d.denominations.forEach((v,q)->{if(q>0)lines.add(String.format("%-15d %-14d %d",v,q,(long)v*q));});lines.add("----------------------------------------");lines.add("Recorded by: "+d.createdBy);lines.add("Thank you for your generous contribution.");
   for(String line:lines){line=line.replaceAll("[^\u0020-\u007E]","?");for(int i=0;i<Math.min(line.length(),240);i+=80){content.showText(line.substring(i,Math.min(i+80,line.length())));content.newLine();}}content.endText();}doc.save(out);return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=donation-"+d.id+".pdf").contentType(MediaType.APPLICATION_PDF).body(out.toByteArray());}
 }
}
