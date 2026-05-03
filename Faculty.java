import javax.persistence.*;

@Entity
public class Faculty extends Person {
    @Id
    private String facultyId;

    private String subject;

    protected Faculty() {}  // JPA
    public Faculty(String facultyId, String name, int age, String subject){
        super(name, age);
        this.facultyId=facultyId;
        this.subject=subject;
    }

    public String getFacultyId(){ return facultyId; }
    public String getSubject(){ return subject; }

    @Override
    public void showRole(){ System.out.println(name + " is a Faculty."); }

    @Override
    public String toString(){
        return "Faculty{id='" + facultyId + "', name='" + name + "', age=" + age + ", subject='" + subject + "'}";
    }
}
