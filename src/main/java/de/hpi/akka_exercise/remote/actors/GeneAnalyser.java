package de.hpi.akka_exercise.remote.actors;

import akka.actor.Props;

public class GeneAnalyser extends StudentAnalyzer {

    public static final String DEFAULT_NAME = "geneanalyzer";

    public static Props props() {
        return Props.create(GeneAnalyser.class);
    }

    private void handle(StudentsMessage message) {
        return;
    }
}
