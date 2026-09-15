package org.community.giving;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.LocalDate;
import java.time.Instant;
import java.security.Principal;
import java.util.*;
@RestController @RequestMapping({"/api", "/api/v1"}) public class ElectionApi {
 final ElectionRepository elections;final CandidateRepository candidates;final VoteRepository votes;final Api api; final MemberRepository members; final CommitteeMemberRepository committee;
 static final List<String> CATEGORIES=List.of("Pastorate Committee","Diocese Council"), SUBCATEGORIES=List.of("Youth","General","Womens");
 public ElectionApi(ElectionRepository e,CandidateRepository c,VoteRepository v,Api a,MemberRepository m,CommitteeMemberRepository cm){members=m;elections=e;candidates=c;votes=v;api=a;committee=cm;}
 @GetMapping("/committee-members") public List<CommitteeMember> committeeMembers(){return committee.findByActiveTrue();}
 @PostMapping("/admin/committee-members") public CommitteeMember appoint(@RequestBody CommitteeMember body,Principal p){
  Api.require(body.memberId!=null,"Member required");var m=members.findById(body.memberId).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Member not found"));Api.require(m.active,"Member must be active");
  Api.require(CATEGORIES.contains(body.category)&&SUBCATEGORIES.contains(body.subcategory),"Invalid committee seat");
  var cm=new CommitteeMember();cm.memberId=m.id;cm.memberName=m.name;cm.profilePicImage=m.profilePicImage;cm.category=body.category;cm.subcategory=body.subcategory;cm.active=true;cm.appointedBy=p.getName();cm.appointedAt=Instant.now();
  return committee.save(cm);
 }
 @PutMapping("/admin/committee-members/{id}/status") public CommitteeMember setCommitteeStatus(@PathVariable Long id,@RequestParam boolean active){var cm=committee.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Committee member not found"));cm.active=active;return committee.save(cm);}
 @PutMapping("/admin/committee-members/{id}/post") public CommitteeMember setCommitteePost(@PathVariable Long id,@RequestParam(required=false) String post){var cm=committee.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Committee member not found"));cm.post=(post==null||post.isBlank())?null:post.trim();return committee.save(cm);}
 @PutMapping("/admin/elections/candidates/{id}/post") public Candidate setCandidatePost(@PathVariable Long id,@RequestParam(required=false) String post){var c=candidates.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));c.post=(post==null||post.isBlank())?null:post.trim();return candidates.save(c);}
 @PutMapping("/admin/elections/candidates/{id}/fee") @Transactional public Candidate payFee(@PathVariable Long id,@RequestBody Donation body,Principal p){
  var c=candidates.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  body.memberId=c.memberId;body.category="Election Form Fee";
  if(body.event==null||body.event.isBlank())body.event="General";
  if(body.receivedDate==null)body.receivedDate=LocalDate.now();
  if(body.contributionMonth==null||body.contributionMonth.isBlank())body.contributionMonth=java.time.YearMonth.now().toString();
  api.createDonation(body,p);
  c.feePaid=true;
  return candidates.save(c);
 }
 @GetMapping("/public/committee-members") public List<Map<String,Object>> publicCommittee(){
  var appointed=committee.findByActiveTrue().stream().map(cm->(Map<String,Object>)new HashMap<String,Object>(Map.of("memberName",cm.memberName,"category",cm.category,"subcategory",cm.subcategory,"post",Objects.toString(cm.post,""),"profilePicImage",Objects.toString(cm.profilePicImage,"")))).toList();
  var winners=candidates.findByWinnerTrue().stream().map(c->(Map<String,Object>)new HashMap<String,Object>(Map.of("memberName",c.memberName,"category",c.category,"subcategory",c.subcategory,"post",Objects.toString(c.post,""),"profilePicImage",Objects.toString(c.profilePicImage,"")))).toList();
  var all=new ArrayList<Map<String,Object>>();all.addAll(appointed);all.addAll(winners);return all;
 }
 @GetMapping("/public/elections") public List<Map<String,Object>> publicList(){return elections.findAll().stream().filter(e->!LocalDate.now().isAfter(e.endDate)).map(e->{var cs=candidates.findByElectionId(e.id);var result=new HashMap<String,Object>();result.put("election",e);result.put("candidates",cs);return (Map<String,Object>)result;}).toList();}
 @GetMapping("/elections") public List<Map<String,Object>> list(Principal p){var a=api.account(p);return elections.findAll().stream().map(e->{var cast=votes.findByElectionId(e.id);var cs=candidates.findByElectionId(e.id);boolean closed=LocalDate.now().isAfter(e.endDate);var result=new HashMap<String,Object>();result.put("election",e);result.put("candidates",cs);result.put("votedSeats",cast.stream().filter(v->Objects.equals(v.voterId,a.memberId)).map(v->v.category+"/"+v.subcategory).toList());result.put("results",closed?cs.stream().map(c->Map.of("candidateId",c.id,"count",cast.stream().filter(v->v.candidateId.equals(c.id)).count())).toList():List.of());return (Map<String,Object>)result;}).toList();}
 @PostMapping("/admin/elections") public Election create(@RequestBody Election e){Api.require(e.name!=null && !e.name.isBlank(),"Election name required");Api.require(e.startDate!=null && e.endDate!=null && !e.endDate.isBefore(e.startDate) && e.startDate.isAfter(LocalDate.now()),"Start date must be in the future and end date must be on or after start date");e.id=null;return elections.save(e);}
 @PostMapping("/admin/elections/{id}/candidates") @Transactional public Candidate candidate(@PathVariable Long id,@RequestBody Candidate c){var e=elections.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));Api.require(LocalDate.now().isBefore(e.startDate),"Candidates must be finalized before the election opens");Api.require(CATEGORIES.contains(c.category)&&SUBCATEGORIES.contains(c.subcategory),"Invalid election seat");Api.require(c.memberId!=null,"Member required");var m=members.findById(c.memberId).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Member not found"));Api.require(m.active,"Candidate must be active");Api.require(candidates.findByElectionId(id).stream().noneMatch(x->x.memberId.equals(c.memberId)&&x.category.equals(c.category)&&x.subcategory.equals(c.subcategory)),"Candidate already nominated for this seat");c.id=null;c.electionId=id;c.memberName=m.name;c.profilePicImage=m.profilePicImage;return candidates.save(c);}
 @PutMapping("/admin/elections/candidates/{candidateId}/winner") @Transactional public Candidate winner(@PathVariable Long candidateId,@RequestParam boolean selected,@RequestParam(defaultValue="0") int votes){var c=candidates.findById(candidateId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));if(selected)candidates.findByElectionId(c.electionId).stream().filter(x->x.category.equals(c.category)&&x.subcategory.equals(c.subcategory)&&!x.id.equals(c.id)).forEach(x->{x.winner=false;candidates.save(x);});c.winner=selected;c.voteCount=Math.max(0,votes);return candidates.save(c);}
 public record Ballot(Long candidateId){}
 @PostMapping("/elections/{id}/votes") @Transactional public Map<String,String> vote(@PathVariable Long id,@RequestBody Ballot ballot,Principal p){var a=api.account(p);if(!a.role.equals("MEMBER"))throw new ResponseStatusException(HttpStatus.FORBIDDEN);var e=elections.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));Api.require(!LocalDate.now().isBefore(e.startDate)&&!LocalDate.now().isAfter(e.endDate),"Election is not open");Api.require(ballot.candidateId()!=null,"Candidate required");var c=candidates.findById(ballot.candidateId()).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid candidate"));Api.require(c.electionId.equals(id),"Candidate belongs to a different election");Api.require(!votes.existsByElectionIdAndVoterIdAndCategoryAndSubcategory(id,a.memberId,c.category,c.subcategory),"You have already voted for this seat");var v=new Vote();v.electionId=id;v.voterId=a.memberId;v.candidateId=c.id;v.category=c.category;v.subcategory=c.subcategory;votes.saveAndFlush(v);return Map.of("message","Vote recorded");}
 @ExceptionHandler(DataIntegrityViolationException.class) @ResponseStatus(HttpStatus.CONFLICT) public Map<String,String> conflict(){return Map.of("message","This nomination or vote already exists.");}
}
