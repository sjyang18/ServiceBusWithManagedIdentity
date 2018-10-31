// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.servicebus.samples.servicebuswithmanagedidentity;

import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.servicebus.*;
import com.google.gson.Gson;

import static java.nio.charset.StandardCharsets.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

import com.microsoft.azure.servicebus.security.TokenProvider;
import org.apache.commons.cli.*;

public class QueuesGettingStarted {

    static final Gson GSON = new Gson();

    public void run(ArrayList<String> sb_namespace_queuename) throws Exception {

        TokenProvider tokenProvider = new MyManagedServiceIdentityTokenProvider();
        ClientSettings clientSettings = new ClientSettings(tokenProvider);
        String sb_namespace = sb_namespace_queuename.get(0);
        System.out.printf("\nsb_namespace: %s", sb_namespace);
        String sb_queuename = sb_namespace_queuename.get(1);
        System.out.printf("\nsb_queuename: %s", sb_queuename);


        // Create a QueueClient instance for receiving using the connection string builder
        // We set the receive mode to "PeekLock", meaning the message is delivered
        // under a lock and must be acknowledged ("completed") to be removed from the queue
        QueueClient receiveClient = new QueueClient(sb_namespace,sb_queuename, clientSettings, ReceiveMode.PEEKLOCK);
        // We are using single thread executor as we are only processing one message at a time
    	ExecutorService executorService = Executors.newSingleThreadExecutor();
        this.registerReceiver(receiveClient, executorService);

        // Create a QueueClient instance for sending and then asynchronously send messages.
        // Close the sender once the send operation is complete.
        QueueClient sendClient = new QueueClient(sb_namespace,sb_queuename, clientSettings, ReceiveMode.PEEKLOCK);
        this.sendMessagesAsync(sendClient).thenRunAsync(() -> sendClient.closeAsync());

        // wait for ENTER or 10 seconds elapsing
        waitForEnter(10);

        // shut down receiver to close the receive loop
        receiveClient.close();
        executorService.shutdown();
    }

    CompletableFuture<Void> sendMessagesAsync(QueueClient sendClient) {
        List<HashMap<String, String>> data =
                GSON.fromJson(
                        "[" +
                                "{'name' = 'Einstein', 'firstName' = 'Albert'}," +
                                "{'name' = 'Heisenberg', 'firstName' = 'Werner'}," +
                                "{'name' = 'Curie', 'firstName' = 'Marie'}," +
                                "{'name' = 'Hawking', 'firstName' = 'Steven'}," +
                                "{'name' = 'Newton', 'firstName' = 'Isaac'}," +
                                "{'name' = 'Bohr', 'firstName' = 'Niels'}," +
                                "{'name' = 'Faraday', 'firstName' = 'Michael'}," +
                                "{'name' = 'Galilei', 'firstName' = 'Galileo'}," +
                                "{'name' = 'Kepler', 'firstName' = 'Johannes'}," +
                                "{'name' = 'Kopernikus', 'firstName' = 'Nikolaus'}" +
                                "]",
                        new TypeToken<List<HashMap<String, String>>>() {}.getType());

        List<CompletableFuture> tasks = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            final String messageId = Integer.toString(i);
            Message message = new Message(GSON.toJson(data.get(i), Map.class).getBytes(UTF_8));
            message.setContentType("application/json");
            message.setLabel("Scientist");
            message.setMessageId(messageId);
            message.setTimeToLive(Duration.ofMinutes(2));
            System.out.printf("\nMessage sending: Id = %s", message.getMessageId());
            tasks.add(
                    sendClient.sendAsync(message).thenRunAsync(() -> {
                        System.out.printf("\n\tMessage acknowledged: Id = %s", message.getMessageId());
                    }));
        }
        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture<?>[tasks.size()]));
    }

    void registerReceiver(QueueClient queueClient, ExecutorService executorService) throws Exception {

    	
        // register the RegisterMessageHandler callback with executor service
        queueClient.registerMessageHandler(new IMessageHandler() {
                                               // callback invoked when the message handler loop has obtained a message
                                               public CompletableFuture<Void> onMessageAsync(IMessage message) {
                                                   // receives message is passed to callback
                                                   if (message.getLabel() != null &&
                                                           message.getContentType() != null &&
                                                           message.getLabel().contentEquals("Scientist") &&
                                                           message.getContentType().contentEquals("application/json")) {

                                                       byte[] body = message.getBody();
                                                       Map scientist = GSON.fromJson(new String(body, UTF_8), Map.class);

                                                       System.out.printf(
                                                               "\n\t\t\t\tMessage received: \n\t\t\t\t\t\tMessageId = %s, \n\t\t\t\t\t\tSequenceNumber = %s, \n\t\t\t\t\t\tEnqueuedTimeUtc = %s," +
                                                                       "\n\t\t\t\t\t\tExpiresAtUtc = %s, \n\t\t\t\t\t\tContentType = \"%s\",  \n\t\t\t\t\t\tContent: [ firstName = %s, name = %s ]\n",
                                                               message.getMessageId(),
                                                               message.getSequenceNumber(),
                                                               message.getEnqueuedTimeUtc(),
                                                               message.getExpiresAtUtc(),
                                                               message.getContentType(),
                                                               scientist != null ? scientist.get("firstName") : "",
                                                               scientist != null ? scientist.get("name") : "");
                                                   }
                                                   return CompletableFuture.completedFuture(null);
                                               }

                                               // callback invoked when the message handler has an exception to report
                                               public void notifyException(Throwable throwable, ExceptionPhase exceptionPhase) {
                                                   System.out.printf(exceptionPhase + "-" + throwable.getMessage());
                                               }
                                           },
                // 1 concurrent call, messages are auto-completed, auto-renew duration
                new MessageHandlerOptions(1, true, Duration.ofMinutes(1)),
                executorService);

    }

    public static void main(String[] args) {

        System.exit(runApp(args, (sb_namespace_queuename) -> {
            QueuesGettingStarted app = new QueuesGettingStarted();
            try {
                app.run(sb_namespace_queuename);
                return 0;
            } catch (Exception e) {
                System.out.printf("%s", e.toString());
                return 1;
            }
        }));
    }

    static final String SB_NAMESPACE = "SB_NAMESPACE";
    static final String SB_QUEUENAME = "SB_QUEUENAME";

    public static int runApp(String[] args, Function<ArrayList<String>, Integer> run) {
        try {

            String sb_namespace = null;
            String sb_queuename = null;

            // parse connection string from command line
            Options options = new Options();
            options.addOption(new Option("n", true, "SB_NAMESPACE"));
            options.addOption(new Option("q", true, "SB_QUEUENAME"));
            CommandLineParser clp = new DefaultParser();
            CommandLine cl = clp.parse(options, args);
            if (cl.getOptionValue("n") != null) {
                sb_namespace = cl.getOptionValue("n");
            }
            if (cl.getOptionValue("q") != null) {
                sb_queuename = cl.getOptionValue("q");
            }

            // get overrides from the environment
            String env_namespace = System.getenv(SB_NAMESPACE);
            if (env_namespace != null) {
                sb_namespace = env_namespace;
            }
            String env_queuename = System.getenv(SB_QUEUENAME);
            if (env_queuename != null) {
                sb_queuename = env_queuename;
            }

            if (sb_queuename == null || sb_namespace == null) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("run jar with", "", options, "", true);
                return 2;
            }
            ArrayList<String> sb_ns_queuname = new ArrayList<String>();
            sb_ns_queuname.add(sb_namespace);
            sb_ns_queuname.add(sb_queuename);
            return run.apply(sb_ns_queuname);
        } catch (Exception e) {
            System.out.printf("%s", e.toString());
            return 3;
        }
    }

    private void waitForEnter(int seconds) {
        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            executor.invokeAny(Arrays.asList(() -> {
                System.in.read();
                return 0;
            }, () -> {
                Thread.sleep(seconds * 1000);
                return 0;
            }));
        } catch (Exception e) {
            // absorb
        }
    }
}
