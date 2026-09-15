import { Component, inject, signal, InjectionToken } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
interface Member {id?:number; name:string; mobileNo:string; active:boolean; [key:string]:any;}
interface Donation {id:number;memberId:number;memberName:string;amount:number;category:string;event:string;receivedDate:string;contributionMonth:string;paymentType:string;reference:string;description:string;denominations:Record<string,number>;attachmentImage?:string;}
export const PORTAL_ROLE = new InjectionToken<string>('PORTAL_ROLE');
@Component({selector:'app-root',standalone:true,imports:[CommonModule,FormsModule],templateUrl:'./app.html'})
export class App {
 pastors=signal<any[]>([]); pastorForm=false; pastor:any={name:'',mobileNo:'',email:'',active:true,isBishop:false};
 role=inject(PORTAL_ROLE); me=signal<any>(null); loading=signal(false); error=signal(''); notice=signal(''); members=signal<Member[]>([]); donations=signal<Donation[]>([]); masters=signal<any>({genders:['MALE','FEMALE'],categories:[],events:[],denominations:[],paymentTypes:[]}); elections=signal<any[]>([]); election:any={}; candidate:any={}; electionId=0; electionForm=false; candidateForm=false; ballot:any=null; page='overview'; publicPage=''; month=new Date().toISOString().slice(0,7); day=''; search=''; username='';password=''; csrf:any; editing=signal(false); recording=signal(false); selected=signal<Donation|null>(null); member:Member=this.emptyMember(); newUsername='';newPassword=''; donation:any=this.emptyDonation();
 profileFields=[['address','Address','text'],['profession','Profession','text'],['dob','Date of birth','date'],['baptismDate','Baptism date','date'],['confirmationDate','Confirmation date','date'],['marriageDate','Marriage date','date'],['deathDate','Death date','date']];
 showLogin=false; heroImages=['assets/church-1.png','assets/church-2.png','assets/church-3.png']; heroIndex=signal(0);
 publicElections=signal<any[]>([]); candidateIndex=signal(0); publicCommittee=signal<any[]>([]); publicView='home';
 enquiry:any={name:'',contact:'',message:''}; enquirySent=false; enquiries=signal<any[]>([]);
 printForm=false; printingElection:any=null;
 feeForm=false; feeTarget:any=null; feePayment:any={amount:null,paymentType:'CASH',reference:'',denominations:{}};
 winnerForm=false; winner:any={};
 dayEndDate=new Date().toISOString().slice(0,10); dayEnd=signal<any>(null); depositReference=''; depositAccountId:any=null;
 churches=signal<any[]>([]); bankAccounts=signal<any[]>([]); publicChurch=signal<any>(null);
 church:any={name:'',address:'',phone:'',email:'',title:'',description:'',foundedYear:null,foundedBy:'',foundationStoneImage:'',images:[]};
 bankAccount:any={bankName:'',accountNumber:'',ifscCode:'',accountHolderName:''};
 committeeAppointments=signal<any[]>([]); committeeForm=false; committeeAppointment:any={};
 postForm=false; postTarget:any=null; postValue='';
 constructor(){this.publicPage=location.pathname.slice(1);this.restore();this.loadPublicElections();setInterval(()=>{const n=this.displayHeroImages.length;if(n)this.heroIndex.set((this.heroIndex()+1)%n);},5000);setInterval(()=>{const n=this.publicCandidates.length;if(n)this.candidateIndex.set((this.candidateIndex()+1)%n);},4000);}
 async loadPublicElections(){try{this.publicElections.set(await this.api('/public/elections'));}catch{}try{this.pastors.set(await this.api('/pastors'));}catch{}try{this.publicCommittee.set(await this.api('/public/committee-members'));}catch{}try{this.publicChurch.set(await this.api('/public/church'));}catch{}}
 onChurchImage(event:Event){const input=event.target as HTMLInputElement;const file=input.files?.[0];if(!file)return;this.run(async()=>{const form=new FormData();form.append('file',file,file.name);const res=await this.uploadFile(form);this.church.images=[...(this.church.images||[]),res.filename];});}
 removeChurchImage(i:number){this.church.images=this.church.images.filter((_:any,idx:number)=>idx!==i);}
 get publicCandidates(){return this.publicElections().flatMap((e:any)=>e.candidates.map((c:any)=>({...c,electionName:e.election.name,electionEnd:e.election.endDate})));}
 get displayHeroImages(){const imgs=this.publicChurch()?.images;return imgs&&imgs.length?imgs.map((f:string)=>'/uploads/'+f):this.heroImages;}
 async submitEnquiry(){await this.run(async()=>{await this.api('/public/contact','POST',this.enquiry);this.enquiry={name:'',contact:'',message:''};this.enquirySent=true;});}
 async loadEnquiries(){await this.run(async()=>{this.enquiries.set(await this.api('/admin/contact'));});}
 async markEnquiryRead(e:any,read:boolean){await this.run(async()=>{await this.api('/admin/contact/'+e.id+'/read?read='+read,'PUT');await this.loadEnquiries();});}
 printElectionForm(electionId:number){const item=this.elections().find((e:any)=>e.election.id===electionId);if(item){this.printingElection=item.election;this.printForm=true;}}
 doPrint(){window.print();}
 get currentPastorName(){return this.pastors().find((p:any)=>p.active && !p.isBishop)?.name||'';}
 openFeeForm(c:any){this.feeTarget=c;this.feePayment={amount:null,paymentType:'CASH',reference:'',denominations:{}};this.feeForm=true;}
 async saveFee(){await this.run(async()=>{const denominations=this.feePayment.paymentType==='CASH'?Object.fromEntries(Object.entries(this.feePayment.denominations).filter(([v,q]:any)=>Number(q)>0)):{};await this.api('/admin/elections/candidates/'+this.feeTarget.id+'/fee','PUT',{...this.feePayment,denominations});await this.load();this.feeForm=false;this.notice.set('Form fee recorded.');});}
 get feeDenominationTotal(){return Object.entries(this.feePayment.denominations).reduce((s:number,[v,q]:any)=>s+Number(v)*Number(q||0),0);}
 get feeDenominationMismatch(){return this.feePayment.paymentType==='CASH' && this.feePayment.amount!=null && Number(this.feePayment.amount)!==this.feeDenominationTotal;}
 get upcomingEvents(){const recurring=[{title:'Sunday Worship Service',when:'Every Sunday · 9:00 AM',note:'Main worship service for the whole community.'},{title:'Sunday School',when:'Every Sunday · 8:00 AM',note:'Bible study and activities for children.'},{title:'Prayer Meeting',when:'Every Wednesday · 6:00 PM',note:'Midweek prayer and fellowship.'}];const elections=this.publicElections().map((e:any)=>({title:e.election.name,when:this.electionStatus(e.election)+' · Ends '+e.election.endDate,note:e.election.description||'Community committee election.'}));return [...elections,...recurring];}
 emptyMember():Member{return {name:'',mobileNo:'',gender:null,active:true,married:false};}
 emptyDonation(){return {memberId:null,noMember:false,amount:null,category:'Monthly pledge',event:'General',receivedDate:new Date().toISOString().slice(0,10),contributionMonth:new Date().toISOString().slice(0,7),paymentType:'CASH',reference:'',description:'',denominations:{}};}
 async api(path:string,method='GET',body?:any){const headers:any={};if(method!=='GET'){if(!this.csrf)this.csrf=await (await fetch('/api/csrf')).json();headers[this.csrf.headerName]=this.csrf.token;if(!(body instanceof URLSearchParams))headers['Content-Type']='application/json';}const r=await fetch('/api'+path,{method,headers,body:body instanceof URLSearchParams?body:body===undefined?undefined:JSON.stringify(body)});if(!r.ok){let message='Request failed. Please try again.';try{const e=await r.json();message=e.detail||e.message||message;}catch{}if(r.status===401){this.me.set(null);message='Please check your username and password, or sign in again.';}if(r.status===403)message='You do not have permission, or your session expired. Sign in again.';throw Error(message);}return r.status===204?null:r.headers.get('content-type')?.includes('json')?r.json():r.blob();}
 async restore(){try{const me=await this.api('/me');if(me.role===this.role){this.me.set(me);await this.load();}}catch{}}
 async run(work:()=>Promise<void>){this.loading.set(true);this.error.set('');this.notice.set('');try{await work();}catch(e){this.error.set((e as Error).message);}finally{this.loading.set(false);}}
 async login(){await this.run(async()=>{await this.api('/login','POST',new URLSearchParams({username:this.username,password:this.password}));this.csrf=null;const me=await this.api('/me');if(me.role!==this.role){await this.api('/logout','POST');throw Error('This account belongs to the '+(me.role==='ADMIN'?'admin':'member')+' portal.');}this.me.set(me);this.password='';this.showLogin=false;await this.load();});}
 async logout(){await this.run(async()=>{await this.api('/logout','POST');this.csrf=null;this.me.set(null);this.showLogin=false;this.donations.set([]);this.members.set([]);});}
 async load(){this.elections.set(await this.api('/elections'));this.masters.set(await this.api('/masters'));this.donations.set(await this.api('/donations'));this.pastors.set(await this.api('/pastors'));this.committeeAppointments.set(await this.api('/committee-members'));if(this.role==='ADMIN'){this.members.set(await this.api('/admin/members'));this.churches.set(await this.api('/churches'));this.bankAccounts.set(await this.api('/bank-accounts'));}}
 get filtered(){return this.donations().filter(d=>(!this.month||d.contributionMonth===this.month)&&(!this.day||d.receivedDate===this.day)&&(!this.search||[d.memberName,d.category,String(d.id)].join(' ').toLowerCase().includes(this.search.toLowerCase())));}
 get total(){return this.filtered.reduce((s,d)=>s+Number(d.amount),0);}
 get cash(){return this.filtered.filter(d=>d.paymentType==='CASH').reduce((s,d)=>s+Number(d.amount),0);}
 get memberCash(){return this.filtered.filter(d=>d.paymentType==='CASH' && d.memberId).reduce((s,d)=>s+Number(d.amount),0);}
 get commonCash(){return this.filtered.filter(d=>d.paymentType==='CASH' && !d.memberId).reduce((s,d)=>s+Number(d.amount),0);}
 get filteredMembers(){return this.members().filter(m=>(m.name+' '+m.mobileNo).toLowerCase().includes(this.search.toLowerCase()));}
 get electionWinners(){return this.elections().flatMap((e:any)=>e.candidates).filter((c:any)=>c.winner);}
 get allCommitteeMembers(){return [...this.electionWinners,...this.committeeAppointments()];}
 get denominationTotal(){return Object.entries(this.donation.denominations).reduce((s,[v,q])=>s+Number(v)*Number(q),0);}
 get denominationMismatch(){return this.donation.paymentType==='CASH' && this.donation.amount!==null && Number(this.donation.amount)!==this.denominationTotal;}
 edit(m?:Member){this.member=m?{...m,gender:m['gender']?.trim().toUpperCase() || null}:this.emptyMember();this.newUsername='';this.newPassword='';this.editing.set(true);}
 async setMemberStatus(m:any,active:boolean){await this.run(async()=>{await this.api('/admin/members/'+m.id+'/status?active='+active,'PUT');await this.load();this.notice.set(active?'Member activated.':'Member deactivated.');});}
 async saveMember(){await this.run(async()=>{await this.api('/admin/members'+(this.member.id?'/'+this.member.id:''),this.member.id?'PUT':'POST',this.member.id?this.member:{member:this.member,username:this.newUsername,password:this.newPassword});await this.load();this.editing.set(false);this.notice.set('Member saved successfully.');});}
 record(){this.donation=this.emptyDonation();this.recording.set(true);}
 toggleNoMember(v:boolean){this.donation.memberId=null;if(v)this.donation.category='Sunday Worship Offerings';}
 async saveDonation(){await this.run(async()=>{const {noMember,...rest}=this.donation;const d={...rest,denominations:this.donation.paymentType==='CASH'?Object.fromEntries(Object.entries(this.donation.denominations).filter(([v,q])=>Number(q)>0)):{}};await this.api('/admin/donations','POST',d);await this.load();this.recording.set(false);this.notice.set('Donation recorded. The receipt is ready to download.');});}
 async saveElection(){await this.run(async()=>{await this.api('/admin/elections','POST',this.election);await this.load();this.electionForm=false;this.notice.set('Election created. Add candidates before its start date.');});}
 async saveCandidate(){await this.run(async()=>{await this.api('/admin/elections/'+this.electionId+'/candidates','POST',this.candidate);await this.load();this.candidateForm=false;});}
 openWinner(electionId:number){this.winner={electionId,category:'',subcategory:'',candidateId:null,votes:0};this.winnerForm=true;}
 get winnerCandidates(){return (this.elections().find((e:any)=>e.election.id===this.winner.electionId)?.candidates||[]).filter((c:any)=>c.category===this.winner.category&&c.subcategory===this.winner.subcategory);}
 async saveWinner(){await this.run(async()=>{await this.api('/admin/elections/candidates/'+this.winner.candidateId+'/winner?selected=true&votes='+this.winner.votes,'PUT');await this.load();this.winnerForm=false;this.notice.set('Winner declared for '+this.winner.category+' / '+this.winner.subcategory+'.');});}
 async loadDayEnd(){await this.run(async()=>{this.dayEnd.set(await this.api('/admin/day-end/'+this.dayEndDate));});}
 async approveDayEnd(){await this.run(async()=>{await this.api('/admin/day-end/'+this.dayEndDate+'/approve','POST');this.dayEnd.set(await this.api('/admin/day-end/'+this.dayEndDate));this.notice.set('Day approved. Denominations are finalized.');});}
 async verifyDayEnd(){await this.run(async()=>{await this.api('/admin/day-end/'+this.dayEndDate+'/verify','POST');this.dayEnd.set(await this.api('/admin/day-end/'+this.dayEndDate));this.notice.set('Day verified and closed.');});}
 async depositDayEnd(){await this.run(async()=>{await this.api('/admin/day-end/'+this.dayEndDate+'/deposit','POST',{bankAccountId:this.depositAccountId,reference:this.depositReference});this.dayEnd.set(await this.api('/admin/day-end/'+this.dayEndDate));this.depositReference='';this.depositAccountId=null;this.notice.set('Cash deposit recorded.');});}
 async loadChurchSettings(){await this.run(async()=>{this.churches.set(await this.api('/churches'));this.bankAccounts.set(await this.api('/bank-accounts'));});}
 async saveChurch(){await this.run(async()=>{const path=this.church.id?'/admin/churches/'+this.church.id:'/admin/churches';await this.api(path,this.church.id?'PUT':'POST',this.church);await this.loadChurchSettings();this.notice.set('Church profile saved.');});}
 editChurch(c:any){this.church={...c};}
 async saveBankAccount(){await this.run(async()=>{await this.api('/admin/bank-accounts','POST',this.bankAccount);this.bankAccount={bankName:'',accountNumber:'',ifscCode:'',accountHolderName:''};await this.loadChurchSettings();this.notice.set('Bank account added.');});}
 async setBankAccountStatus(b:any,active:boolean){await this.run(async()=>{await this.api('/admin/bank-accounts/'+b.id+'/status?active='+active,'PUT');await this.loadChurchSettings();});}
 lineLabel(item:any){return item.count!=null?item.label+' ('+item.count+' member'+(item.count===1?'':'s')+' contributed)':item.label;}
 appointCommittee(){this.committeeAppointment={memberId:null,category:'Pastorate Committee',subcategory:'General'};this.committeeForm=true;}
 async saveCommitteeAppointment(){await this.run(async()=>{await this.api('/admin/committee-members','POST',this.committeeAppointment);await this.load();this.committeeForm=false;this.notice.set('Committee member appointed.');});}
 async removeCommitteeMember(c:any){await this.run(async()=>{await this.api('/admin/committee-members/'+c.id+'/status?active=false','PUT');await this.load();this.notice.set('Committee member removed.');});}
 assignPost(c:any){this.postTarget=c;this.postValue=c.post||'';this.postForm=true;}
 async savePost(){await this.run(async()=>{const path=this.postTarget.appointedBy?'/admin/committee-members/':'/admin/elections/candidates/';await this.api(path+this.postTarget.id+'/post?post='+encodeURIComponent(this.postValue),'PUT');await this.load();this.postForm=false;this.notice.set('Post updated.');});}
 async uploadFile(form:FormData){if(!this.csrf)this.csrf=await (await fetch('/api/csrf')).json();const r=await fetch('/api/admin/uploads',{method:'POST',headers:{[this.csrf.headerName]:this.csrf.token},body:form});if(!r.ok){let message='Upload failed. Please try again.';try{const e=await r.json();message=e.detail||e.message||message;}catch{}throw Error(message);}return r.json();}
 onFile(event:Event,target:any,field:string='profilePicImage'){const input=event.target as HTMLInputElement;const file=input.files?.[0];if(!file)return;if(field!=='profilePicImage'){this.run(async()=>{const form=new FormData();form.append('file',file,file.name);const res=await this.uploadFile(form);target[field]=res.filename;});return;}const reader=new FileReader();reader.onload=()=>{const img=new Image();img.onload=()=>{const max=480;const scale=Math.min(1,max/Math.max(img.width,img.height));const canvas=document.createElement('canvas');canvas.width=img.width*scale;canvas.height=img.height*scale;canvas.getContext('2d')!.drawImage(img,0,0,canvas.width,canvas.height);canvas.toBlob(async(blob)=>{if(!blob)return;await this.run(async()=>{const form=new FormData();form.append('file',blob,'photo.jpg');const res=await this.uploadFile(form);target.profilePicImage=res.filename;});},'image/jpeg',0.82);};img.src=reader.result as string;};reader.readAsDataURL(file);}
 async savePastor(){await this.run(async()=>{await this.api('/admin/pastors'+(this.pastor.id?'/'+this.pastor.id:''),this.pastor.id?'PUT':'POST',this.pastor);await this.load();this.pastorForm=false;this.notice.set('Pastor saved successfully.');});}
 editPastor(p?:any){this.pastor=p?{...p}: {name:'',mobileNo:'',email:'',active:true,isBishop:false};this.pastorForm=true;}
 async setPastorStatus(p:any,active:boolean){await this.run(async()=>{await this.api('/admin/pastors/'+p.id+'/status?active='+active,'PUT');await this.load();this.notice.set(active?'Pastor activated.':'Pastor deactivated.');});}
 async vote(){await this.run(async()=>{await this.api('/elections/'+this.ballot.electionId+'/votes','POST',{candidateId:this.ballot.id});this.ballot=null;await this.load();this.notice.set('Your vote has been recorded.');});}
 electionStatus(e:any){const today=new Date().toLocaleDateString('en-CA');return today<e.startDate?'Upcoming':today>e.endDate?'Closed':'Open';}
 result(e:any,id:number){return e.results.find((r:any)=>r.candidateId===id)?.count??0;}
 async download(d:Donation){await this.run(async()=>{const blob=await this.api('/donations/'+d.id+'/receipt');const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download='donation-'+d.id+'.pdf';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);});}
}
