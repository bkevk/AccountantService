package account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentRepo extends JpaRepository<Payment, String> {
    @Query(value = "SELECT * FROM PAYMENT WHERE EMPLOYEE = ?1",
            nativeQuery = true)
    List<Payment> findUserByUser(String employee);
}
