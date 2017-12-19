package de.hpi.akka_exercise;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.PoisonPill;
import com.typesafe.config.Config;
import de.hpi.akka_exercise.messages.ShutdownMessage;
import de.hpi.akka_exercise.remote.actors.*;
import de.hpi.akka_exercise.scheduling.SchedulingStrategy;
import de.hpi.akka_exercise.util.AkkaUtils;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

public class TaskRunner {

    private static final String DEFAULT_MASTER_SYSTEM_NAME = "MasterActorSystem";
    private static final String DEFAULT_SLAVE_SYSTEM_NAME = "SlaveActorSystem";
    private static final String DEFAULT_INPUT_FILE = "students.csv";

    public static void runMaster(String host, int port, SchedulingStrategy.Factory schedulingStrategyFactory, int numLocalWorkers) {

        // Create the ActorSystem
        final Config config = AkkaUtils.createRemoteAkkaConfig(host, port);
        final ActorSystem actorSystem = ActorSystem.create(DEFAULT_MASTER_SYSTEM_NAME, config);

        // Create the Reaper.
        actorSystem.actorOf(Reaper.props(), Reaper.DEFAULT_NAME);

        // Create the Listener
        final ActorRef listener = actorSystem.actorOf(Listener.props(), Listener.DEFAULT_NAME);
        final ActorRef fileReader = actorSystem.actorOf(FileReader.props(), FileReader.DEFAULT_NAME);

        // Create the Masters
        final ActorRef master = actorSystem.actorOf(Master.props(listener, schedulingStrategyFactory, numLocalWorkers), Master.DEFAULT_NAME);

        // Create the Shepherd
        final ActorRef shepherd = actorSystem.actorOf(Shepherd.props(master), Shepherd.DEFAULT_NAME);

        // Enter interactive loop
        TaskRunner.enterInteractiveLoop(listener, master, shepherd, fileReader);

        System.out.println("Stopping...");

        // Await termination: The termination should be issued by the reaper
        TaskRunner.awaitTermination(actorSystem);
    }

    private static void enterInteractiveLoop(final ActorRef listener, final ActorRef master, final ActorRef shepherd, final ActorRef fileReader) {

        // Read ranges from the console and process them
        final Scanner scanner = new Scanner(System.in);
        String fileName = DEFAULT_INPUT_FILE;
        boolean crackPasswords = true;
        boolean compareGenomes = true;
        int numSplits = 100;
        while (true) {
            // Sleep to reduce mixing of log messages with the regular stdout messages.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            // Read input
            System.out.println("> Enter ...\n"
                    + "  \"start\" to start processing,\n"
                    + "  \"input\" to enter a different input file,\n"
                    + "  \"password\" to toggle whether or not passwords will be cracked (default yes),\n"
                    + "  \"passwordsplits\" to set number of splits used for the password cracking,\n"
                    + "  \"genome\" to toggle whether or not genomes will be compared (default yes),\n"
                    + "  \"exit\" for a graceful shutdown,\n"
                    + "  \"kill\" for a hard shutdown:");
            String line = scanner.nextLine();

            switch (line) {
                case "start":
                    TaskRunner.process(master, fileReader, fileName, numSplits, crackPasswords, compareGenomes);
                    break;
                case "input":
                    System.out.print("Enter the new input file: ");
                    fileName = scanner.nextLine();
                    System.out.println("Set input file to " + fileName);
                    break;
                case "password":
                    crackPasswords = !crackPasswords;
                    System.out.println("Set cracking passwords to " + (crackPasswords ? "on" : "off") + ".");
                    break;
                case "passwordsplits":
                    System.out.print("Enter the new number of splits: ");
                    numSplits = scanner.nextInt();
                    System.out.println("Set number of splits to " + numSplits);
                    break;
                case "genome":
                    compareGenomes = !compareGenomes;
                    System.out.println("Set comparing genomes to " + (compareGenomes ? "on" : "off") + ".");
                    break;
                case "exit":
                    TaskRunner.shutdown(shepherd, master);
                    scanner.close();
                    return;
                case "kill":
                    TaskRunner.kill(listener, master, shepherd);
                    scanner.close();
                    return;
                default:
                    System.out.println("Unknown command: " + line);
            }
        }
    }

    private static void shutdown(final ActorRef shepherd, final ActorRef master) {

        // Tell the master that we will not send any further requests and want to shutdown the system after all current jobs finished
        master.tell(new ShutdownMessage(), ActorRef.noSender());

        // Do not accept any new subscriptions
        shepherd.tell(new ShutdownMessage(), ActorRef.noSender());
    }

    private static void kill(final ActorRef listener, final ActorRef master, final ActorRef shepherd) {

        // End the listener
        listener.tell(PoisonPill.getInstance(), ActorRef.noSender());

        // End the master
        master.tell(PoisonPill.getInstance(), ActorRef.noSender());

        // End the shepherd
        shepherd.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    private static void process(final ActorRef master, final ActorRef fileReader, String fileName, int numSplits, boolean crackPasswords, boolean compareGenomes) {
        master.tell(new Master.BeginWorkMessage(fileName, fileReader, numSplits, crackPasswords, compareGenomes), ActorRef.noSender());
    }

    public static void awaitTermination(final ActorSystem actorSystem) {
        try {
            Await.ready(actorSystem.whenTerminated(), Duration.Inf());
        } catch (TimeoutException | InterruptedException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.println("ActorSystem terminated!");
    }

    public static void runSlave(String host, int port, String masterHost, int masterPort) {

        // Create the local ActorSystem
        final Config config = AkkaUtils.createRemoteAkkaConfig(host, port);
        final ActorSystem actorSystem = ActorSystem.create(DEFAULT_SLAVE_SYSTEM_NAME, config);

        // Create the reaper.
        actorSystem.actorOf(Reaper.props(), Reaper.DEFAULT_NAME);

        // Create a Slave
        final ActorRef slave = actorSystem.actorOf(Slave.props(), Slave.DEFAULT_NAME);

        // Tell the Slave to register the local ActorSystem
        slave.tell(new Slave.AddressMessage(new Address("akka.tcp", DEFAULT_MASTER_SYSTEM_NAME, masterHost, masterPort)), ActorRef
            .noSender());

        // Await termination: The termination should be issued by the reaper
        TaskRunner.awaitTermination(actorSystem);
    }
}
