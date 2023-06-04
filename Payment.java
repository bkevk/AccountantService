package account;

import javax.persistence.*;

@Entity
@Table
public class Payment {
    @Id
    private String id;
    @Column
    private String employee;
    @Column
    private String period;
    @Column
    private Long salary;

    public void setId(){
        this.id = this.employee + this.period;
    }

    public Long getSalary() {
        return salary;
    }

    public String getEmployee() {
        return employee;
    }

    public String getPeriod() {
        return period;
    }

    public String getId() {
        return id;
    }
}
