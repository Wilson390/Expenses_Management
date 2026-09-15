package org.community.giving;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
@Configuration public class Security {
 @Bean PasswordEncoder encoder(){return new BCryptPasswordEncoder();}
 @Bean UserDetailsService users(AccountRepository accounts, MemberRepository members){return username->{var a=accounts.findByUsername(username).orElseThrow(()->new UsernameNotFoundException("Invalid credentials")); boolean active=a.memberId==null || members.findById(a.memberId).map(m->m.active).orElse(false); return User.withUsername(a.username).password(a.passwordHash).roles(a.role).disabled(!active).build();};}
 @Bean SecurityFilterChain chain(HttpSecurity http) throws Exception {
  return http.authorizeHttpRequests(a->a.requestMatchers("/api/csrf","/api/v1/csrf","/api/login","/api/v1/login","/api/public/**","/api/v1/public/**","/api/pastors","/api/v1/pastors","/actuator/health/**").permitAll().requestMatchers("/api/admin/**","/api/v1/admin/**").hasRole("ADMIN").anyRequest().authenticated())
   .formLogin(f->f.loginProcessingUrl("/api/login").successHandler((q,r,a)->r.setStatus(204)).failureHandler((q,r,e)->r.sendError(401,"Invalid credentials")))
   .logout(l->l.logoutUrl("/api/logout").logoutSuccessHandler((q,r,a)->r.setStatus(204)))
   .exceptionHandling(e->e.authenticationEntryPoint((q,r,x)->r.sendError(401))).build();
 }
 @Bean CommandLineRunner bootstrap(AccountRepository accounts,PasswordEncoder encoder,@Value("${app.admin-username}") String username,@Value("${app.admin-password}") String password){return args->{if(accounts.count()==0){if(password.length()<12)throw new IllegalStateException("Set ADMIN_PASSWORD to at least 12 characters before first startup");var a=new Account();a.username=username;a.passwordHash=encoder.encode(password);a.role="ADMIN";accounts.save(a);}};}
}
