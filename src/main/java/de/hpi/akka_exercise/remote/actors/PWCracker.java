package de.hpi.akka_exercise.remote.actors;

import akka.actor.Props;

public class PWCracker extends StudentAnalyzer {
    public static final String DEFAULT_NAME = "pwcracker";

    public static Props props() {
        return Props.create(PWCracker.class);
    }

    private void handle(StudentsMessage message) {
        return;
    }
}
