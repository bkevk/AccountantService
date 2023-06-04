package account;


import java.util.ArrayList;

public class UserDTO {
    private long id;
    private String name;
    private String lastname;
    private String email;
    private ArrayList<String> roles;
    public UserDTO(User user){
        roles = new ArrayList<>();
        this.id = user.getId();
        this.name = user.getName();
        this.lastname = user.getLastname();
        this.email = user.getEmail();

        for(UserGroup group : user.getUserGroups()){
            roles.add(group.getRole());
        }
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLastname() {
        return lastname;
    }

    public String getEmail() {
        return email;
    }

    public ArrayList<String> getRoles() {
        return roles;
    }
}
