package at.tugraz.ist.qs2024;

import at.tugraz.ist.qs2024.actorsystem.Message;
import at.tugraz.ist.qs2024.actorsystem.SimulatedActor;
import at.tugraz.ist.qs2024.actorsystem.SimulatedActorSystem;
import at.tugraz.ist.qs2024.messageboard.*;
import at.tugraz.ist.qs2024.messageboard.clientmessages.*;
import at.tugraz.ist.qs2024.messageboard.dispatchermessages.Stop;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Simple actor, which can be used in tests, e.g. to check if the correct messages are sent by workers.
 * This actor can be sent to workers as client.
 */
class TestClient extends SimulatedActor {

    /**
     * Messages received by this actor.
     */
    final Queue<Message> receivedMessages;

    TestClient() {
        receivedMessages = new LinkedList<>();
    }

    /**
     * does not implement any logic, only saves the received messages
     *
     * @param message Non-null message received
     */
    @Override
    public void receive(Message message) {
        receivedMessages.add(message);
    }
}

public class MessageBoardTests {

    @Test
    public void testSameMessageUpdate() throws UnknownClientException, UnknownMessageException {
        // testing only the acks
        SimulatedActorSystem system = new SimulatedActorSystem();
        Dispatcher dispatcher = new Dispatcher(system, 2);
        system.spawn(dispatcher);
        TestClient client = new TestClient();
        system.spawn(client);

        // send request and run system until a response is received
        // communication id is chosen by clients
        dispatcher.tell(new InitCommunication(client, 10));
        while (client.receivedMessages.size() == 0)
            system.runFor(1);

        Message initAckMessage = client.receivedMessages.remove();
        Assert.assertEquals(InitAck.class, initAckMessage.getClass());
        InitAck initAck = (InitAck) initAckMessage;
        Assert.assertEquals(Long.valueOf(10), initAck.communicationId);

        SimulatedActor worker = initAck.worker;

        // end the communication
        worker.tell(new FinishCommunication(10));
        while (client.receivedMessages.size() == 0)
            system.runFor(1);

        Message finAckMessage = client.receivedMessages.remove();
        Assert.assertEquals(FinishAck.class, finAckMessage.getClass());
        FinishAck finAck = (FinishAck) finAckMessage;

        Assert.assertEquals(Long.valueOf(10), finAck.communicationId);
        dispatcher.tell(new Stop());

        // TODO: run system until workers and dispatcher are stopped
    }

    // TODO: Implement test cases
}

