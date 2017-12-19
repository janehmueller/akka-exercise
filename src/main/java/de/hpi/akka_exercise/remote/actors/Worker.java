package de.hpi.akka_exercise.remote.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import de.hpi.akka_exercise.Student;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Worker extends AbstractLoggingActor {
    private final static int passwordLength = 7;

    public static Props props() { return Props.create(Worker.class); }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class HashCrackMessage implements Serializable {
        private int id;
        private Map<String, Integer> hashIndexMap;
        private int rangeMin, rangeMax;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class FindGeneMatchMessage implements Serializable {
        private int id;
        private Student x;
        private Student y;
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        Reaper.watchWithDefaultReaper(this);
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        this.log().info("Stopped {}.", this.getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(HashCrackMessage.class, this::handle)
            .match(FindGeneMatchMessage.class, this::handle)
            .matchAny(object -> this.log().error(this.getClass().getName() + " received unknown message: " + object.toString()))
            .build();
    }

    private void handle(HashCrackMessage message) {
        Map<Integer, String> matchedHashes = new HashMap<>();
        Set<String> passwordHashes = message.hashIndexMap.keySet();
        String passwordHash, password, leadingZeros;
        for(int i = message.rangeMin; i < message.rangeMax; i++) {
            password = "" + i;
            leadingZeros = new String(new char[passwordLength - password.length()]).replace("\0", "0");
            password = leadingZeros + password;
            passwordHash = DigestUtils.sha256Hex(password);
            if(passwordHashes.contains(passwordHash)) {
                matchedHashes.put(message.hashIndexMap.get(passwordHash), password);
            }
        }
        this.getSender().tell(new Master.PasswordMessage(message.id, matchedHashes), this.getSelf());
    }

    private void handle(FindGeneMatchMessage message) {
        Set<String> mostCommonSubstrings = mostCommonSubstring(message.x.getGenomeSequence(), message.y.getGenomeSequence());
        if(mostCommonSubstrings.isEmpty()) {
            return;
        }
        String mostCommonSubstring = mostCommonSubstrings.iterator().next();
        if(mostCommonSubstrings.size() > 1) {
            this.log().info("Found more than one most common substring between students {} and {}. Choosing {}.", message.x.getIndex(), message.y.getIndex(), mostCommonSubstring);
        }
        this.getSender().tell(new Master.GenomeMatchMessage(message.id, message.x.getIndex(), message.y.getIndex(), mostCommonSubstring), this.getSelf());
    }

    private Set<String> mostCommonSubstring(String x, String y) {
        Set<String> ret = new HashSet<>();
        int[][] L = new int[x.length()][y.length()];
        int z = 0;
        for(int i = 0; i < x.length(); i++) {
            for(int j = 0; j < y.length(); j++) {
                if(x.charAt(i) == y.charAt(j)) {
                    if(i == 0 || j == 0) {
                        L[i][j] = 1;
                    } else {
                        L[i][j] = L[i - 1][j - 1] + 1;
                    }
                    if(L[i][j] > z) {
                        z = L[i][j];
                        ret = new HashSet<>();
                        ret.add(x.substring(i - z + 1, i + 1));
                    } else if(L[i][j] == z) {
                        ret.add(x.substring(i - z + 1, i + 1));
                    }
                } else {
                    L[i][j] = 0;
                }
            }
        }
        return ret;
    }
}
