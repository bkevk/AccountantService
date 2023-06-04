package account;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImplementation implements UserDetailsService {
    @Autowired
    UserRepo userRepo;
    @Autowired
    GroupRepo groupRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepo.findById(username.toLowerCase()).get();
        user.setUserGroups(groupRepo.findRoleByUser(user.getEmail()));

        if (user == null) {
            throw new UsernameNotFoundException("Not found");
        }

        return new UserDetailsImplementation(user);
    }


}
