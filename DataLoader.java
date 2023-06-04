package account;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataLoader {

    private GroupRepo groupRepo;

    @Autowired
    public DataLoader(GroupRepo groupRepository) {
        this.groupRepo = groupRepository;
        createRoles();
    }

    private void createRoles() {
        try {
            UserGroup admin = new UserGroup();
            admin.setRole("ROLE_ADMINISTRATOR");
            UserGroup user = new UserGroup();
            user.setRole("ROLE_USER");
            UserGroup acc = new UserGroup();
            acc.setRole("ROLE_ACCOUNTANT");
            UserGroup auditor = new UserGroup();
            auditor.setRole("ROLE_AUDITOR");
            groupRepo.save(admin);
            groupRepo.save(user);
            groupRepo.save(acc);
            groupRepo.save(auditor);
        } catch (Exception e) {

        }
    }
}