package de.hpi.akka_exercise.remote.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import de.hpi.akka_exercise.StudentList;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

public class PWCracker extends StudentAnalyzer {

    private void handle(StudentsMessage message) {
        return;
    }
}
