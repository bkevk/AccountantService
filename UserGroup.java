package account;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table
public class UserGroup {
    @Id
    private String role;

    @ManyToMany(mappedBy = "userGroups")
    private List<User> users;

    public UserGroup(){

    }

    public void setRole(String role){
        this.role = role;
    }

    public String getRole() {
        return role;
    }
}
