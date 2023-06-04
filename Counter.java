package account;

import org.springframework.stereotype.Component;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table
public class Counter {
    @Id
    private Long counter;
    public Counter(Long count){
        this.counter = count;
    }
    public Counter(){

    }

}
