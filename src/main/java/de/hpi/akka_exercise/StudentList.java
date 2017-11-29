package de.hpi.akka_exercise;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class StudentList implements Serializable {
    private List<Student> students;

    public StudentList() {
        this.students = new ArrayList<>();
    }

    public void addStudent(Student student) { this.students.add(student); }
    public void removeStudent(Student student) { this.students.remove(student); }
    public Student getStudent(int index) { return this.students.get(index); }

    public Map<String, Integer> createHashIndexMap() {
        Map<String, Integer> hashes = new HashMap<>();
        for(Student student: students) {
            hashes.put(student.getPasswordHash(), student.getIndex());
        }
        return hashes;
    }

    public void updateStudentPassword(int studentIndex, String password) {
        this.getStudent(studentIndex).setPassword(password);
    }
}
