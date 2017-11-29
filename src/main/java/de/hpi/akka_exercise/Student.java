package de.hpi.akka_exercise;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Student {

    private int index;
    private String name;
    private String passwordHash;
    private String password;
    private String genomeSequence;
    private int[] closestGenomeSequenceIndex;

}
