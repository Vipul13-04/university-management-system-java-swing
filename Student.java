import javax.persistence.*;

@Entity
public class Student extends Person {
    @Id
    private int rollNo;

    private String course;

    protected Student() {}  // JPA
    public Student(int rollNo, String name, int age, String course){
        super(name, age);
        this.rollNo=rollNo;
        this.course=course;
    }

    public int getRollNo(){ return rollNo; }
    public String getCourse(){ return course; }

    @Override
    public void showRole(){ System.out.println(name + " is a Student."); }

    @Override
    public String toString(){
        return "Student{rollNo=" + rollNo + ", name='" + name + "', age=" + age + ", course='" + course + "'}";
    }
}
