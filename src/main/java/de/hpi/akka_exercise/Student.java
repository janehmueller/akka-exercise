package de.hpi.akka_exercise;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class Student implements Serializable {
    private int index;
    private String name;
    private String passwordHash;
    private String password = "";
    private String genomeSequence;
    private String closestGenomeSequence = "";
    private int closestGenomeSequenceNeighbor;
    private Set<Integer> genomeUpdates = new HashSet<>();

    public Student(String line) {
        String[] splitLines = line.split(",");
        assert(splitLines.length == 4);
        this.index = Integer.parseInt(splitLines[0]);
        this.name = splitLines[1];
        this.passwordHash = splitLines[2];
        this.genomeSequence = splitLines[3];
    }

    public void updateGenomeNeighbor(int neighborId, String genomeSequence) {
        genomeUpdates.add(neighborId);
        if(closestGenomeSequence.length() < genomeSequence.length()) {
            closestGenomeSequence = genomeSequence;
            closestGenomeSequenceNeighbor = neighborId;
        }
    }

    public boolean isGenomeNeighborFound(int numStudents) {
        return genomeUpdates.size() == numStudents - 1;
    }

    public String toCSV() {
        return index + "," + name + "," + password + "," + closestGenomeSequenceNeighbor + "," + closestGenomeSequence;
    }
}
