import javax.persistence.*;
import javax.persistence.criteria.*;
import java.util.*;
import java.util.function.Consumer;

class OODB {
    // Creates/opens embedded ODB at ./db/university.odb
    static final EntityManagerFactory EMF =
            Persistence.createEntityManagerFactory("$objectdb/db/university.odb");

    static void inTx(Consumer<EntityManager> work){
        EntityManager em = EMF.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try{
            tx.begin();
            work.accept(em);
            tx.commit();
        }catch(RuntimeException ex){
            if(tx.isActive()) tx.rollback();
            throw ex;
        }finally{
            em.close();
        }
    }

    static <T> List<T> list(String jpql, Class<T> type){
        EntityManager em = EMF.createEntityManager();
        try{
            return em.createQuery(jpql, type).getResultList();
        }finally{
            em.close();
        }
    }

    static <T> List<T> list(String jpql, Class<T> type, Consumer<TypedQuery<T>> binder){
        EntityManager em = EMF.createEntityManager();
        try{
            TypedQuery<T> q = em.createQuery(jpql, type);
            binder.accept(q);
            return q.getResultList();
        }finally{
            em.close();
        }
    }

    static <T> T find(Class<T> type, Object id){
        EntityManager em = EMF.createEntityManager();
        try{ return em.find(type, id); }
        finally{ em.close(); }
    }
}

/* ---------- StudentRepository: JPQL + Criteria ---------- */
class StudentRepository {
    void save(Student s){
        OODB.inTx(em -> em.merge(s));   // merge = insert or update by @Id
    }
    void deleteById(int rollNo){
        OODB.inTx(em -> {
            Student found = em.find(Student.class, rollNo);
            if(found != null) em.remove(found);
        });
    }
    List<Student> findAll(){
        return OODB.list("SELECT s FROM Student s ORDER BY s.rollNo", Student.class);
    }
    List<Student> findByCourse(String course){
        return OODB.list("SELECT s FROM Student s WHERE s.course = :c ORDER BY s.rollNo",
                Student.class, q -> q.setParameter("c", course));
    }
    List<Student> findAgeGreaterThanCriteria(int minAge){
        EntityManager em = OODB.EMF.createEntityManager();
        try{
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Student> cq = cb.createQuery(Student.class);
            Root<Student> root = cq.from(Student.class);
            cq.select(root).where(cb.gt(root.get("age"), minAge)).orderBy(cb.asc(root.get("rollNo")));
            return em.createQuery(cq).getResultList();
        }finally{ em.close(); }
    }
    double averageAge(){
        EntityManager em = OODB.EMF.createEntityManager();
        try{
            Double avg = em.createQuery("SELECT AVG(s.age) FROM Student s", Double.class).getSingleResult();
            return avg == null ? 0.0 : avg;
        }finally{ em.close(); }
    }
}

/* ---------- FacultyRepository: JPQL ---------- */
class FacultyRepository {
    void save(Faculty f){
        OODB.inTx(em -> em.merge(f));
    }
    void deleteById(String id){
        OODB.inTx(em -> {
            Faculty found = em.find(Faculty.class, id);
            if(found != null) em.remove(found);
        });
    }
    List<Faculty> findAll(){
        return OODB.list("SELECT f FROM Faculty f ORDER BY f.facultyId", Faculty.class);
    }
    List<Faculty> findBySubjectPrefix(String prefix){
        return OODB.list("SELECT f FROM Faculty f WHERE f.subject LIKE :p ORDER BY f.facultyId",
                Faculty.class, q -> q.setParameter("p", prefix + "%"));
    }
}

public class UniversityOODBJPQL {
    private static final Scanner sc = new Scanner(System.in);
    private static final StudentRepository studentRepo = new StudentRepository();
    private static final FacultyRepository facultyRepo = new FacultyRepository();

    public static void main(String[] args){
        System.out.println("Embedded OODB file: db/university.odb");
        while(true){
            System.out.println("\n=== OODB (JPA + JPQL) Menu ===");
            System.out.println("1. Add Student");
            System.out.println("2. Add Faculty");
            System.out.println("3. List Students (all)");
            System.out.println("4. List Faculty (all)");
            System.out.println("5. Find Students by Course (JPQL)");
            System.out.println("6. Find Students age > N (Criteria API)");
            System.out.println("7. Avg Student Age (JPQL)");
            System.out.println("8. Find Faculty by Subject prefix (JPQL)");
            System.out.println("9. Delete Student by RollNo");
            System.out.println("10. Delete Faculty by Id");
            System.out.println("0. Exit");
            System.out.print("Choice: ");

            int ch = readInt();
            try{
                switch(ch){
                    case 1 -> addStudent();
                    case 2 -> addFaculty();
                    case 3 -> listStudents();
                    case 4 -> listFaculty();
                    case 5 -> findStudentsByCourse();
                    case 6 -> findStudentsByAgeCriteria();
                    case 7 -> avgStudentAge();
                    case 8 -> findFacultyBySubjectPrefix();
                    case 9 -> deleteStudent();
                    case 10 -> deleteFaculty();
                    case 0 -> {
                        OODB.EMF.close();
                        System.out.println("Bye!");
                        return;
                    }
                    default -> System.out.println("Invalid choice.");
                }
            }catch(Exception ex){
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    /* ---- Menu handlers ---- */
    static void addStudent(){
        System.out.print("Roll No: "); int roll = readInt();
        System.out.print("Name: "); String name = sc.next();
        System.out.print("Age: "); int age = readInt();
        System.out.print("Course: "); String course = sc.next();
        studentRepo.save(new Student(roll, name, age, course));
        System.out.println("Student saved.");
    }

    static void addFaculty(){
        System.out.print("Faculty ID: "); String id = sc.next();
        System.out.print("Name: "); String name = sc.next();
        System.out.print("Age: "); int age = readInt();
        System.out.print("Subject: "); String subject = sc.next();
        facultyRepo.save(new Faculty(id, name, age, subject));
        System.out.println("Faculty saved.");
    }

    static void listStudents(){
        List<Student> all = studentRepo.findAll();
        if(all.isEmpty()) System.out.println("(none)");
        else all.forEach(System.out::println);
    }

    static void listFaculty(){
        List<Faculty> all = facultyRepo.findAll();
        if(all.isEmpty()) System.out.println("(none)");
        else all.forEach(System.out::println);
    }

    static void findStudentsByCourse(){
        System.out.print("Course: "); String c = sc.next();
        List<Student> res = studentRepo.findByCourse(c);
        if(res.isEmpty()) System.out.println("(none)");
        else res.forEach(System.out::println);
    }

    static void findStudentsByAgeCriteria(){
        System.out.print("Min age: "); int a = readInt();
        List<Student> res = studentRepo.findAgeGreaterThanCriteria(a);
        if(res.isEmpty()) System.out.println("(none)");
        else res.forEach(System.out::println);
    }

    static void avgStudentAge(){
        System.out.printf("Average age = %.2f%n", studentRepo.averageAge());
    }

    static void findFacultyBySubjectPrefix(){
        System.out.print("Subject prefix (e.g. 'Ja' -> Java): ");
        String p = sc.next();
        List<Faculty> res = facultyRepo.findBySubjectPrefix(p);
        if(res.isEmpty()) System.out.println("(none)");
        else res.forEach(System.out::println);
    }

    static void deleteStudent(){
        System.out.print("Roll No to delete: "); int r = readInt();
        studentRepo.deleteById(r);
        System.out.println("Deleted (if existed).");
    }

    static void deleteFaculty(){
        System.out.print("Faculty Id to delete: "); String id = sc.next();
        facultyRepo.deleteById(id);
        System.out.println("Deleted (if existed).");
    }

    static int readInt(){
        while(true){
            try{ return Integer.parseInt(sc.next()); }
            catch(Exception e){ System.out.print("Enter integer: "); }
        }
    }
}
