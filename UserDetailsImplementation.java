package account;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserDetailsImplementation implements UserDetails {
    private final String username;
    private final String password;
    private final List<GrantedAuthority> rolesAndAuthorities;

    @Autowired
    UserRepo userRepo;

    public UserDetailsImplementation(User user){

        this.username = user.getEmail();
        this.password = user.getPassword();

        rolesAndAuthorities = new ArrayList<>();

        for(UserGroup group: user.getUserGroups()) {
            this.rolesAndAuthorities.add(new SimpleGrantedAuthority(group.getRole()));
        }






    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.rolesAndAuthorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
