import { bootstrapApplication } from '@angular/platform-browser';
import { App, PORTAL_ROLE } from '../../shared/app';
bootstrapApplication(App,{providers:[{provide:PORTAL_ROLE,useValue:'MEMBER'}]}).catch(console.error);
