package de.hpi.akka_exercise.remote.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.Serializable;
import java.util.HashMap;
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

    @Override
    public void preStart() throws Exception {
        super.preStart();
        Reaper.watchWithDefaultReaper(this);
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        log().info("Stopped {}.", this.getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(HashCrackMessage.class, this::handle)
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
        this.getSender().tell(new PWCracker.PasswordMessage(message.id, matchedHashes), this.getSelf());
    }
}
