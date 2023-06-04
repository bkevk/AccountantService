package account;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CounterRepo extends JpaRepository<Counter, Long> {
}
