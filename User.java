package account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "userDetail")
@Table(name = "userData")
public class User {

    @Column
    private long id;
    @Column
    private String name;
    @Column
    private String lastname;
    @Id
    private String email;
    @Column
    private boolean locked = false;
    @Column
    private int bruteForce = 0;

    public boolean isLocked() {
        return locked;
    }

    public int getBruteForce() {
        return bruteForce;

    }
    public void clearBruteForce(){
        this.bruteForce = 0;
    }

    public void triedBruteForce(){
        this.bruteForce++;

    }
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @ManyToMany
    @JoinTable(name = "user_roles",
            joinColumns =@JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id"
            ))
    private List<UserGroup> userGroups;

    @Column
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    public User(){
        this.userGroups = new ArrayList<>();
    }


    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLastname() {
        return lastname;
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }


    public void setPassword(String password) {
        this.password = password;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<UserGroup> getUserGroups() {
        return userGroups;
    }

    public void setUserGroups(List<UserGroup> userGroups) {
        this.userGroups = userGroups;
    }

    public void addUserGroups(UserGroup group){
        this.userGroups.add(group);
    }
    public void removeUserGroups(UserGroup group){
        this.userGroups.remove(group);
    }

    public boolean hasRole(String role){
        for(UserGroup group : userGroups){
            if(group.getRole().equals(role)){
                return true;
            }
        }
        return false;
    }


}
