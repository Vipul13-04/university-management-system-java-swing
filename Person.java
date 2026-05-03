import javax.persistence.*;
import java.io.Serializable;

@MappedSuperclass
public abstract class Person implements Serializable {
    protected String name;
    protected int age;

    protected Person() {}            // JPA needs no-arg
    protected Person(String n, int a){ this.name=n; this.age=a; }

    public String getName(){ return name; }
    public int getAge(){ return age; }

    public abstract void showRole();
}
