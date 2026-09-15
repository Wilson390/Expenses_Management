package org.community.giving;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1","app.admin-password=test-admin-password"})
@Transactional class WorkflowTest {
 @Autowired MemberRepository members; @Autowired ElectionRepository elections; @Autowired Api api; @Autowired ElectionApi electionApi;
 Member member(String username){var m=new Member();m.name="Test Member";m.mobileNo="987654"+String.format("%04d",Math.abs(username.hashCode())%10000);return api.create(new Api.MemberInput(m,username,"test-member-password"));}
 Donation donation(Long id){var d=new Donation();d.memberId=id;d.category="Monthly pledge";d.event="General";d.receivedDate=LocalDate.now();d.contributionMonth="2026-10";d.paymentType="CASH";d.amount=new BigDecimal("200.00");d.denominations=Map.of(100,2);return d;}
 @Test void cashMustReconcile(){var m=member("cash-member");var d=donation(m.id);d.denominations=Map.of(100,1);assertThrows(ResponseStatusException.class,()->api.createDonation(d,()->"admin"));}
 @Test void memberOnlySeesOwnContributionsAndReceipts() throws Exception {var one=member("one");var two=member("two");var d=api.createDonation(donation(one.id),()->"admin");assertEquals(1,api.list(()->"one").size());assertTrue(api.list(()->"two").isEmpty());assertThrows(ResponseStatusException.class,()->api.receipt(d.id,()->"two"));assertTrue(api.receipt(d.id,()->"one").getBody().length>100);assertEquals("2026-10",d.contributionMonth);}
 @Test void negativeDenominationsRejected(){var m=member("negative");var d=donation(m.id);d.denominations=Map.of(100,-2);assertThrows(ResponseStatusException.class,()->api.createDonation(d,()->"admin"));}
 @Test void duplicateVoteAndWrongElectionRejected(){var m=member("voter");var e=new Election();e.name="Election";e.startDate=LocalDate.now().plusDays(1);e.endDate=e.startDate.plusDays(2);e=electionApi.create(e);var c=new Candidate();c.memberId=m.id;c.category="Pastorate Committee";c.subcategory="General";c=electionApi.candidate(e.id,c);e.startDate=LocalDate.now();elections.save(e);final Long eid=e.id,cid=c.id;electionApi.vote(eid,new ElectionApi.Ballot(cid),()->"voter");assertThrows(ResponseStatusException.class,()->electionApi.vote(eid,new ElectionApi.Ballot(cid),()->"voter"));}
 @Test void inactiveMemberIsBlocked(){var m=member("inactive");m.active=false;members.save(m);assertThrows(ResponseStatusException.class,()->api.list(()->"inactive"));}
}
