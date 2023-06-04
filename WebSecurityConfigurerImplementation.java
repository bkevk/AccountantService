package account;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Base64;

@EnableWebSecurity
public class WebSecurityConfigurerImplementation extends WebSecurityConfigurerAdapter {
    @Autowired
    UserDetailsService userDetailsService;
    @Autowired
    LogRepository logRepository;
    @Autowired
    UserRepo userRepo;


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(userDetailsService)
                .passwordEncoder(getEncoder());

    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.httpBasic()
                //.authenticationEntryPoint(new RestAuthenticationEntryPoint()) // Handle auth error
                .and()
                .csrf().disable().headers().frameOptions().disable() // for Postman, the H2 console
                .and()
                .authorizeRequests() // manage access

                .mvcMatchers("/api/empl/payment").hasAnyRole("ACCOUNTANT", "USER")
                .mvcMatchers("/api/acct/payments").hasRole("ACCOUNTANT")
                .mvcMatchers("/api/security/events").hasRole("AUDITOR")
                .mvcMatchers("/api/auth/changepass").authenticated()
                .mvcMatchers("/api/admin/**").hasRole("ADMINISTRATOR")
                .antMatchers(HttpMethod.POST, "/api/signup").permitAll()
                // other matchers
                .and()
                .exceptionHandling().accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
                        LogMessage log = new LogMessage();
                        log.setId(logRepository.count());
                        log.setDate(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
                        log.setAction("ACCESS_DENIED");
                        log.setSubject(request.getUserPrincipal().getName());
                        log.setObject(new UrlPathHelper().getPathWithinApplication(request));
                        log.setPath(new UrlPathHelper().getPathWithinApplication(request));
                        logRepository.save(log);
                        response.sendError(HttpStatus.FORBIDDEN.value(), "Access Denied!");
                    }
                })
                .and()
                .httpBasic().authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
                        if(request.getHeader("Authorization") != null){
                            LogMessage log = new LogMessage();
                            log.setId(logRepository.count());
                            log.setDate(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
                            log.setAction("LOGIN_FAILED");
                            String encodedString = request.getHeader("Authorization").substring(6);
                            byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
                            String email = new String(decodedBytes).split(":")[0];
                            log.setSubject(email);
                            String path = new UrlPathHelper().getPathWithinApplication(request);
                            log.setObject(path);
                            log.setPath(path);
                            logRepository.save(log);
                            if(userRepo.existsById(email)){
                                User user = userRepo.findById(email).get();
                                user.triedBruteForce();
                                userRepo.save(user);
                                if(user.getBruteForce() >= 5 ){
                                    LogMessage msg = new LogMessage();
                                    msg.setId(logRepository.count());
                                    msg.setDate(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
                                    msg.setAction("BRUTE_FORCE");
                                    msg.setSubject(email);
                                    msg.setObject(path);
                                    msg.setPath(path);
                                    logRepository.save(msg);
                                    user.setLocked(true);
                                    userRepo.save(user);
                                    LogMessage lock = new LogMessage();
                                    lock.setId(logRepository.count());
                                    lock.setDate(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
                                    lock.setAction("LOCK_USER");
                                    lock.setSubject(email);
                                    lock.setObject("Lock user " + email);
                                    lock.setPath(path);
                                    logRepository.save(lock);
                                }
                            }

                        }

                        response.sendError(HttpStatus.UNAUTHORIZED.value(), "Wrong password!");

                    }
                })
                .and()
                                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
        //
    @Bean
    public PasswordEncoder getEncoder() {
        return new BCryptPasswordEncoder(13);
    }


}
