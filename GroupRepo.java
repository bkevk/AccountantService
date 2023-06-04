package account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GroupRepo extends JpaRepository<UserGroup, String> {
    @Query(value = "SELECT GROUP_ID as ROLE FROM USER_ROLES WHERE USER_ID = ?1",
            nativeQuery = true)
    List<UserGroup> findRoleByUser(String employee);
}
