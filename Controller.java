package account;

import io.micrometer.core.instrument.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.text.SimpleDateFormat;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Pattern;

@RestController
public class Controller {
    @Autowired
    UserRepo userRepo;
    @Autowired
    PaymentRepo paymentRepo;
    @Autowired
    PasswordEncoder encoder;
    @Autowired
    GroupRepo groupRepo;

    @Autowired
    CounterRepo counterRepo;
    @Autowired
    LogRepository logRepository;


    List<String> breached = List.of("PasswordForJanuary", "PasswordForFebruary", "PasswordForMarch", "PasswordForApril",
            "PasswordForMay", "PasswordForJune", "PasswordForJuly", "PasswordForAugust",
            "PasswordForSeptember", "PasswordForOctober", "PasswordForNovember", "PasswordForDecember");

    @GetMapping("/api/security/events")
    public ResponseEntity<?> getEvents(){
        return new ResponseEntity<>(logRepository.findAll(), HttpStatus.OK);
    }

    @PutMapping("/api/admin/user/access")
    public ResponseEntity<?> lock(@RequestBody Map<String, String> request, @AuthenticationPrincipal UserDetails details){
        User user = userRepo.findById(request.get("user").toLowerCase()).get();
        user.clearBruteForce();
        userRepo.save(user);
        if(request.get("operation").toUpperCase().equals("LOCK")){
            if(user.hasRole("ROLE_ADMINISTRATOR")){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't lock the ADMINISTRATOR!");
            }
            user.setLocked(true);
            userRepo.save(user);
            LogMessage log = new LogMessage();
            log.setId(logRepository.count());
            log.setDate(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
            log.setAction("LOCK_USER");
            log.setSubject(details.getUsername());
            log.setObject("Lock user " + user.getEmail());
            log.setPath("/api/admin/user/access");
            logRepository.save(log);
            Map<String, String> response = Map.of("status", "User " + user.getEmail() + " locked!");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        if(request.get("operation").toUpperCase().equals("UNLOCK")){
            user.setLocked(false);
            userRepo.save(user);
            LogMessage log = new LogMessage();
            log.setId(logRepository.count());
            log.setDate(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
            log.setAction("UNLOCK_USER");
            log.setSubject(details.getUsername());
            log.setObject("Unlock user " + user.getEmail());
            log.setPath("/api/admin/user/access");
            logRepository.save(log);
            Map<String, String> response = Map.of("status", "User " + user.getEmail() + " unlocked!");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Something went wrong!");
    }

    @GetMapping("/api/admin/user")
    public ResponseEntity<?> showUsers(@AuthenticationPrincipal UserDetails details){
        User authenticated = userRepo.findById(details.getUsername()).get();
        authenticated.clearBruteForce();
        userRepo.save(authenticated);
        if(userRepo.findById(details.getUsername()).get().hasRole("ROLE_ADMINISTRATOR")){
            ArrayList<UserDTO> response = new ArrayList<>();
            for(User user: userRepo.findAll()){
                response.add(new UserDTO(user));
            }
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    @PutMapping("/api/admin/user/role")
    public ResponseEntity<?> roleChange(@AuthenticationPrincipal UserDetails details, @RequestBody Map<String,String> request){
        User authenticated = userRepo.findById(details.getUsername()).get();
        authenticated.clearBruteForce();
        userRepo.save(authenticated);
        if(!userRepo.findById(details.getUsername()).get().hasRole("ROLE_ADMINISTRATOR")){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied!");
        }
        if(!userRepo.existsById(request.get("user").toLowerCase())){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!");
        }
        User user = userRepo.findById(request.get("user").toLowerCase()).get();
        if(!groupRepo.existsById("ROLE_"+request.get("role").toUpperCase())){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found!");
        }
        UserGroup group = groupRepo.findById("ROLE_"+request.get("role").toUpperCase()).get();

        if(request.get("operation").toUpperCase().equals("REMOVE")){
            if(group.getRole().equals("ROLE_ADMINISTRATOR")){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't remove ADMINISTRATOR role!");
            }
            if(!user.hasRole(group.getRole())){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The user does not have a role!");
            }
            if(user.getUserGroups().stream().count() == 1){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The user must have at least one role!");
            }
            LogMessage log = new LogMessage();
            log.setId(logRepository.count());
            log.setDate(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
            log.setAction("REMOVE_ROLE");
            log.setSubject(details.getUsername());
            log.setObject("Remove role " + group.getRole().substring(5) + " from " + user.getEmail());
            log.setPath("/api/admin/user/role");
            logRepository.save(log);
            user.removeUserGroups(group);
        }
        if(request.get("operation").toUpperCase().equals("GRANT")){
            List<UserGroup> list = user.getUserGroups();
            list.add(0, group);
            user.setUserGroups(list);
            if((user.hasRole("ROLE_ADMINISTRATOR") && user.hasRole("ROLE_AUDITOR")) || (user.hasRole("ROLE_ADMINISTRATOR") && user.hasRole("ROLE_ACCOUNTANT")) || (user.hasRole("ROLE_ADMINISTRATOR") && user.hasRole("ROLE_USER"))){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The user cannot combine administrative and business roles!");
            }
            LogMessage log = new LogMessage();
            log.setId(logRepository.count());
            log.setDate(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
            log.setAction("GRANT_ROLE");
            log.setSubject(details.getUsername());
            log.setObject("Grant role " + group.getRole().substring(5) + " to " + user.getEmail());
            log.setPath("/api/admin/user/role");
            logRepository.save(log);
        }
        userRepo.save(user);
        return new ResponseEntity<>(new UserDTO(user), HttpStatus.OK);

    }

    @DeleteMapping("/api/admin/user/{email}")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal UserDetails details, @PathVariable String email){
        User authenticated = userRepo.findById(details.getUsername()).get();
        authenticated.clearBruteForce();
        userRepo.save(authenticated);
        if(!userRepo.findById(details.getUsername()).get().hasRole("ROLE_ADMINISTRATOR")){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if(!userRepo.existsById(email)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!");
        }

        if(userRepo.findById(email).get().hasRole("ROLE_ADMINISTRATOR")){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't remove ADMINISTRATOR role!");
        }

        userRepo.deleteById(email);
        LogMessage log = new LogMessage();
        log.setId(logRepository.count());
        log.setDate(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
        log.setAction("DELETE_USER");
        log.setSubject(details.getUsername());
        log.setObject(email);
        log.setPath("/api/admin/user");
        logRepository.save(log);
        Map<String, String> response = Map.of("user",email,"status","Deleted successfully!");
        return new ResponseEntity<>(response, HttpStatus.OK);


    }


    @PostMapping("/api/auth/signup")
    public ResponseEntity<?> signUp(@RequestBody User user){

        if(StringUtils.isEmpty(user.getName()) || StringUtils.isEmpty(user.getLastname()) || StringUtils.isEmpty(user.getEmail()) || StringUtils.isEmpty(user.getPassword())||!patternMatches(user.getEmail())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong input!");
        }

        if(userRepo.existsById(user.getEmail().toLowerCase())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User exist!");
        }
        if(user.getPassword().length()<12){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password length must be 12 chars minimum!");
        }
        if(breached.contains(user.getPassword())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The password is in the hacker's database!");
        }


        user.setPassword(encoder.encode(user.getPassword()));
        user.setEmail(user.getEmail().toLowerCase());

        UserGroup group = groupRepo.findById("ROLE_USER").get();
        if(userRepo.count() == 0){
            group = groupRepo.findById("ROLE_ADMINISTRATOR").get();

        }
        user.setUserGroups(List.of(group));
        user.setId(counterRepo.count());
        counterRepo.save(new Counter(userRepo.count()));

        userRepo.save(user);
        LogMessage log = new LogMessage();
        log.setId(logRepository.count());
        log.setDate(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
        log.setAction("CREATE_USER");
        log.setSubject("Anonymous");
        log.setObject(user.getEmail());
        log.setPath("/api/auth/signup");
        logRepository.save(log);
        return new ResponseEntity<>(new UserDTO(user), HttpStatus.OK);
    }

    @PostMapping("/api/auth/changepass")
    public ResponseEntity<?> changePass(@AuthenticationPrincipal UserDetails details, @RequestBody Map<String,String> password){
        User authenticated = userRepo.findById(details.getUsername()).get();
        authenticated.clearBruteForce();
        userRepo.save(authenticated);
        if(password.get("new_password") == null || password.get("new_password").length()<12){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password length must be 12 chars minimum!");
        }
        if(breached.contains(password.get("new_password"))){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The password is in the hacker's database!");
        }
        if(encoder.matches(password.get("new_password"), details.getPassword())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The passwords must be different!");
        }
        User user = userRepo.getById(details.getUsername());
        user.clearBruteForce();
        user.setPassword(encoder.encode(password.get("new_password")));
        userRepo.save(user);
        LogMessage log = new LogMessage();
        log.setId(logRepository.count());
        log.setDate(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
        log.setAction("CHANGE_PASSWORD");
        log.setSubject(details.getUsername());
        log.setObject(details.getUsername());
        log.setPath("/api/auth/changepass");
        logRepository.save(log);
        Map<String,String> response = Map.of("email", details.getUsername(), "status", "The password has been updated successfully");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @GetMapping("/api/empl/payment")
    public ResponseEntity<?> payment(@AuthenticationPrincipal UserDetails details, @RequestParam(required = false) String period) {

        User authenticated = userRepo.findById(details.getUsername()).get();
        if(authenticated.isLocked()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account is locked");
        }
        authenticated.clearBruteForce();
        userRepo.save(authenticated);
        if(period == null){
            return new ResponseEntity<>(sortAndConvert(paymentRepo.findUserByUser(details.getUsername()), details), HttpStatus.OK);
        }
        if(Integer.parseInt(period.substring(0,2)) > 12 ||Integer.parseInt(period.substring(0,2)) < 1 ){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid month!");
        }
        Payment payment = paymentRepo.findById(details.getUsername()+period).get();
        if(payment == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The payment does not exist!");
        }

        return new ResponseEntity<>(sortAndConvert(List.of(payment), details).get(0), HttpStatus.OK);
        //return new ResponseEntity<>(HttpStatus.OK);

    }

    @PutMapping("/api/acct/payments")
    public ResponseEntity<?> update(@RequestBody Payment payment){
        if(payment.getSalary() < 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Salary cannot be negative!");
        }
        if(paymentRepo.existsById(payment.getEmployee()+payment.getPeriod())){
            payment.setId();
            paymentRepo.save(payment);
            return new ResponseEntity<>(Map.of("status", "Updated successfully!"), HttpStatus.OK);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not Updated!");
    }

    @Transactional
    @PostMapping("/api/acct/payments")
    public ResponseEntity<?> payments(@RequestBody List<Payment> list){
        for(Payment payment:list){
            if(!userRepo.existsById(payment.getEmployee().toLowerCase())){
              throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found!");
            }
            if(payment.getSalary() < 0){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Salary cannot be negative!");
            }
            payment.setId();
            if(Integer.parseInt(payment.getPeriod().substring(0,2)) > 12 ||Integer.parseInt(payment.getPeriod().substring(0,2)) < 1 ){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid month!");
            }
            if(paymentRepo.existsById(payment.getId())){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment exists!");
            }
            paymentRepo.save(payment);
        }
        return new ResponseEntity<>(Map.of("status", "Added successfully!"), HttpStatus.OK);
    }

    public static boolean patternMatches(String emailAddress) {
        String regexPattern = "\\w+(@acme.com)$";
        return Pattern.compile(regexPattern)
                .matcher(emailAddress)
                .matches();
    }

    public List<Map<String,String>> sortAndConvert(List<Payment> payments, UserDetails details){
        List<Map<String,String>> response = new LinkedList<>();
        for(Payment payment:payments){
            Map<String, String> entry = new HashMap<>();
            entry.put("name", userRepo.findById(details.getUsername()).get().getName());
            entry.put("lastname", userRepo.findById(details.getUsername()).get().getLastname());
            String[] parts = payment.getPeriod().split("-");
            String period = parts[1] + "-" + parts[0];
            entry.put("period", period);
            entry.put("salary", (int)Math.floor(payment.getSalary()/100) + " dollar(s) " + payment.getSalary()%100 + " cent(s)");
            response.add(entry);
        }
        Comparator<Map<String,String>> sortByPeriod = Comparator.comparing(x -> x.get("period"));
        response.sort(sortByPeriod.reversed());
        for(Map<String, String> entry:response){
            String monthString = entry.get("period").substring(5);
            switch (monthString) {
                case "01":  monthString = "January";
                    break;
                case "02":  monthString = "February";
                    break;
                case "03":  monthString = "March";
                    break;
                case "04":  monthString = "April";
                    break;
                case "05":  monthString = "May";
                    break;
                case "06":  monthString = "June";
                    break;
                case "07":  monthString = "July";
                    break;
                case "08":  monthString = "August";
                    break;
                case "09":  monthString = "September";
                    break;
                case "10": monthString = "October";
                    break;
                case "11": monthString = "November";
                    break;
                case "12": monthString = "December";
                    break;
                default: monthString = "Invalid month";
                    break;
            }
            String yearString = entry.get("period").substring(0,4);
            String date = monthString+"-"+yearString;
            entry.put("period", date);

        }
        return response;
    }
}
