package de.hpi.akka_exercise;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

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

    public Student(String line) {
        String[] splitLines = line.split(",");
        assert(splitLines.length == 4);
        this.index = Integer.parseInt(splitLines[0]);
        this.name = splitLines[1];
        this.passwordHash = splitLines[2];
        this.genomeSequence = splitLines[3];
    }

    public boolean isCracked() {
        return password != null;
    }

    public void updateGenomeNeighbor(int neighborId, String genomeSequence) {
        if(closestGenomeSequence.length() < genomeSequence.length()) {
            closestGenomeSequence = genomeSequence;
            closestGenomeSequenceNeighbor = neighborId;
        }
    }

    public String toCSV() {
        return index + "," + name + "," + password + "," + closestGenomeSequence + "," + closestGenomeSequenceNeighbor;
    }
}
