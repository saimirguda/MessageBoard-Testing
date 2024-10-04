package at.tugraz.ist.qs2024;

import at.tugraz.ist.qs2024.actorsystem.Message;
import at.tugraz.ist.qs2024.actorsystem.SimulatedActor;
import at.tugraz.ist.qs2024.actorsystem.SimulatedActorSystem;
import at.tugraz.ist.qs2024.messageboard.*;
import at.tugraz.ist.qs2024.messageboard.clientmessages.*;
import at.tugraz.ist.qs2024.messageboard.dispatchermessages.Stop;
import at.tugraz.ist.qs2024.messageboard.messagestoremessages.SearchInStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.Queue;

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

    private static final int COMMUNICATION_ID = 10;
    private static final int WRONG_COMMUNICATION_ID = -1;

    private SimulatedActorSystem system;
    private Dispatcher dispatcher;
    private SimulatedActor worker;

    private TestClient client;


    public void setUpNeededInstances(int numOfWorkers) throws UnknownMessageException, UnknownClientException {
        system = new SimulatedActorSystem();
        dispatcher = new Dispatcher(system, numOfWorkers);
        system.spawn(dispatcher);
        client = new TestClient();
        system.spawn(client);

        InitCommunication initCommunication = new InitCommunication(client, COMMUNICATION_ID);
        Assert.assertTrue(initCommunication.getDuration() != 0);
        dispatcher.tell(initCommunication);

        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        Message initAckMessage = client.receivedMessages.remove();
        Assert.assertEquals(InitAck.class, initAckMessage.getClass());
        InitAck initAck = (InitAck) initAckMessage;
        Assert.assertEquals(Long.valueOf(COMMUNICATION_ID), initAck.communicationId);
        Assert.assertTrue(initAck.getDuration() != 0);

        worker = initAck.worker;
    }

    @Before
    public void setUp() throws UnknownMessageException, UnknownClientException {
        setUpNeededInstances(1);
    }

    @Test
    public void testPostMessage() throws UnknownClientException, UnknownMessageException {
        // Set up the simulated actor system

        // Success Publish
        UserMessage message = new UserMessage("Saimir", "HelloWorld");
        Assert.assertNotSame("", message.toString());
        worker.tell(new Publish(message, 10)); // Assuming client ID is 10
        while (client.receivedMessages.isEmpty()) {
            system.runFor(1);
        }
        Message postMessage = client.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, postMessage.getClass());

        // Success Publish diff message
        UserMessage dmessage = new UserMessage("Saimir", "Hi hi");
        Assert.assertNotSame("", dmessage.toString());
        Publish pubmessage = new Publish(dmessage, 10);
        Assert.assertTrue(pubmessage.getDuration() != 0);
        worker.tell(pubmessage); // Assuming client ID is 10
        while (client.receivedMessages.isEmpty()) {
            system.runFor(1);
        }
        Message dpostMessage = client.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, dpostMessage.getClass());

        // Fail Same Publish
        UserMessage message10 = new UserMessage("Saimir", "HelloWorld");
        Assert.assertNotSame("", message10.toString());
        worker.tell(new Publish(message10, 10)); // Assuming client ID is 10
        while (client.receivedMessages.isEmpty()) {
            system.runFor(1);
        }
        Message postMessage10 = client.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, postMessage10.getClass());

        // Fail Message 2 too long
        UserMessage message2 = new UserMessage("Saimir", "Hello World");
        Assert.assertNotSame("", message2.toString());
        worker.tell(new Publish(message2, 10)); // Assuming client ID is 10
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message postMessage2 = client.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, postMessage2.getClass());


        // Fail Changing ID
        message2.setMessageId(10);
        worker.tell(new Publish(message2, 10)); // Assuming client ID is 10
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        postMessage2 = client.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, postMessage2.getClass());


        // Fail Publishing message with likes
        Like likeMessage = new Like("Saimir", 10, message.getMessageId());
        Assert.assertTrue(likeMessage.getDuration() != 0);
        worker.tell(likeMessage); // Assuming client ID is 20 and message ID is 1
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message likeAck = client.receivedMessages.remove();
        Assert.assertTrue(likeAck instanceof ReactionResponse);

        worker.tell(new Publish(message, 10)); // Assuming client ID is 10
        while (client.receivedMessages.isEmpty()) {
            system.runFor(1);
        }
        Message postMessage3 = client.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, postMessage3.getClass());


        // Fail Publishing message with dislikes
        Dislike dislikeMessage = new Dislike("Saimir", 10, message.getMessageId());
        Assert.assertTrue(dislikeMessage.getDuration() != 0);
        worker.tell(dislikeMessage); // Assuming client ID is 20 and message ID is 1
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message dislikeAck = client.receivedMessages.remove();
        Assert.assertTrue(dislikeAck instanceof ReactionResponse);

        worker.tell(new Publish(message, 10)); // Assuming client ID is 10
        while (client.receivedMessages.isEmpty()) {
            system.runFor(1);
        }
        Message postMessage4 = client.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, postMessage4.getClass());

        // Fail Message by wrong author and diff mesage
        Edit editMessage = new Edit(message.getMessageId(), "WrongAuthor", "Edit", COMMUNICATION_ID);
        Assert.assertTrue(editMessage.getDuration() != 0);
        worker.tell(editMessage); // Assuming client ID is 20
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message editAck6 = client.receivedMessages.remove();
        Assert.assertTrue(editAck6 instanceof OperationFailed);

        // Exception wrong comID
        try {
            worker.tell(new Publish(message2, 1));
            while (client.receivedMessages.isEmpty())
                system.runFor(1);
            Assert.fail();
        } catch (Exception ex) {
            Assert.assertTrue(ex instanceof UnknownClientException);

        }

        Stop stop = new Stop();
        Assert.assertTrue(stop.getDuration() != 0);
        worker.tell(stop);
    }

    @Test
    public void testRetrieveMessages() throws UnknownClientException, UnknownMessageException {
        SimulatedActorSystem system = new SimulatedActorSystem();
        Dispatcher dispatcher = new Dispatcher(system, 2);
        system.spawn(dispatcher);
        TestClient client = new TestClient();
        system.spawn(client);

        InitCommunication initCommunication = new InitCommunication(client, COMMUNICATION_ID);
        Assert.assertTrue(initCommunication.getDuration() != 0);
        dispatcher.tell(initCommunication);
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        Message initAckMessage = client.receivedMessages.remove();
        Assert.assertTrue(initAckMessage instanceof InitAck);
        InitAck initAck = (InitAck) initAckMessage;
        Assert.assertTrue(initAck.getDuration() != 0);
        SimulatedActor worker = initAck.worker;

        UserMessage message = new UserMessage("Saimir", "Hello");
        Assert.assertNotSame("", message.toString());

        // Send a "Publish" message to the worker
        worker.tell(new Publish(message, COMMUNICATION_ID)); // Assuming client ID is 10

        // Run the system until the message is processed
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify that the message is successfully posted on the message board
        Message postMessage = client.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, postMessage.getClass());


        worker.tell(new RetrieveMessages("Saimir", COMMUNICATION_ID));
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        Message retrieveMessage = client.receivedMessages.remove();
        Assert.assertTrue(retrieveMessage instanceof FoundMessages);
        Assert.assertTrue(retrieveMessage.getDuration() != 0);

        //wrong author for retrieve
        worker.tell(new RetrieveMessages("Dino", COMMUNICATION_ID));
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message wrongAuthorRetrieve = client.receivedMessages.remove();
        Assert.assertTrue(wrongAuthorRetrieve instanceof FoundMessages);
        Assert.assertTrue(wrongAuthorRetrieve.getDuration() != 0);
        Assert.assertEquals(0, ((FoundMessages) wrongAuthorRetrieve).messages.size());

        try {
            worker.tell(new RetrieveMessages("Saimir", WRONG_COMMUNICATION_ID));
            while (client.receivedMessages.isEmpty())
                system.runFor(1);
            Assert.fail();

        } catch (Exception ex) {
            Assert.assertTrue(ex instanceof UnknownClientException);

        }

        Stop stop = new Stop();
        Assert.assertTrue(stop.getDuration() != 0);
        dispatcher.tell(stop);

        dispatcher.tell(initCommunication);
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify that the client receives an acknowledgment (InitAck)
        Message initAckMessage2 = client.receivedMessages.remove();
        Assert.assertEquals(OperationFailed.class, initAckMessage2.getClass());
        Stop stop1 = new Stop();
        Assert.assertTrue(stop1.getDuration() != 0);
        dispatcher.tell(stop1);
        system.runFor(4);


    }

    @Test
    public void testLikeMessages() throws UnknownClientException, UnknownMessageException {
        UserMessage message = new UserMessage("Saimir", "Hello");
        Assert.assertNotSame("", message.toString());

        // Send a "Publish" message to the worker
        worker.tell(new Publish(message, COMMUNICATION_ID)); // Assuming client ID is 10

        // Run the system until the message is processed
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify that the message is successfully posted on the message board
        Message postMessage = client.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, postMessage.getClass());

        worker.tell(new Like("Saimir", COMMUNICATION_ID, message.getMessageId())); // Assuming client ID is COMMUNICATION_ID and message ID is 1
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify the message was liked successfully
        Message likeAck = client.receivedMessages.poll();
        Assert.assertTrue(likeAck instanceof ReactionResponse);

        worker.tell(new Like("Saimir", COMMUNICATION_ID, message.getMessageId())); // Assuming client ID is COMMUNICATION_ID and message ID is 1
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify the message was liked successfully
        Message likeFail = client.receivedMessages.poll();
        Assert.assertTrue(likeFail instanceof OperationFailed);

        worker.tell(new Like("Saimir", COMMUNICATION_ID, message.getMessageId() + 1)); // Assuming client ID is COMMUNICATION_ID and message ID is 1
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify the message was liked successfully
        Message likeFail2 = client.receivedMessages.poll();
        Assert.assertTrue(likeFail2 instanceof OperationFailed);

        ReactionResponse reactionResponse = (ReactionResponse) likeAck;
        Assert.assertEquals(1, reactionResponse.points); // Assuming initial points are 0

        Stop stop = new Stop();
        Assert.assertTrue(stop.getDuration() != 0);
        worker.tell(stop);
    }

    @Test
    public void testDislikeMessages() throws UnknownClientException, UnknownMessageException {

        UserMessage message = new UserMessage("Dino", "Sudzuka");
        Assert.assertNotSame("", message.toString());

        // Send a "Publish" message to the worker
        worker.tell(new Publish(message, COMMUNICATION_ID)); // Assuming client ID is COMMUNICATION_ID

        // Run the system until the message is processed
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify that the message is successfully posted on the message board
        Message postMessage = client.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, postMessage.getClass());

        worker.tell(new Dislike("Dino", COMMUNICATION_ID, message.getMessageId())); // Assuming client ID is 20 and message ID is 1
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify the message was disliked successfully
        Message dislikeAck = client.receivedMessages.poll();
        Assert.assertTrue(dislikeAck instanceof ReactionResponse);

        worker.tell(new Dislike("Dino", COMMUNICATION_ID, message.getMessageId())); // Assuming client ID is 20 and message ID is 1
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify the message disliked failed, cannot dislike same message twice
        Message dislikeFail = client.receivedMessages.poll();
        Assert.assertTrue(dislikeFail instanceof OperationFailed);

        worker.tell(new Dislike("Dino", COMMUNICATION_ID, message.getMessageId() + 1)); // Assuming client ID is 20 and message ID is 1
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify the message disliked failed because wrong message ID
        Message dislikeFail2 = client.receivedMessages.poll();
        Assert.assertTrue(dislikeFail2 instanceof OperationFailed);

        Stop stop = new Stop();
        Assert.assertTrue(stop.getDuration() != 0);
        worker.tell(stop);
    }

    @Test
    public void testLikeDislikeMessages() throws UnknownClientException, UnknownMessageException {

        UserMessage message = new UserMessage("Saimir", "Hello");
        Assert.assertNotSame("", message.toString());

        // Send a "Publish" message to the worker
        worker.tell(new Publish(message, COMMUNICATION_ID)); // Assuming client ID is 10

        // Run the system until the message is processed
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify that the message is successfully posted on the message board
        Message postMessage = client.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, postMessage.getClass());

        worker.tell(new Like("Saimir", COMMUNICATION_ID, message.getMessageId())); // Assuming client ID is 20 and message ID is 1
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify the message was liked successfully
        Message likeAck = client.receivedMessages.poll();
        Assert.assertTrue(likeAck instanceof ReactionResponse);

        try {
            worker.tell(new Like("Saimir", WRONG_COMMUNICATION_ID, message.getMessageId()));
            while (client.receivedMessages.isEmpty())
                system.runFor(1);
            Assert.fail();
        } catch (Exception ex) {
            Assert.assertTrue(ex instanceof UnknownClientException);
        }

        ReactionResponse reactionResponse = (ReactionResponse) likeAck;
        Assert.assertEquals(1, reactionResponse.points); // Assuming initial points are 0

        worker.tell(new Dislike("Saimir", COMMUNICATION_ID, message.getMessageId())); // Assuming client ID is 20
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Check the dislike was processed
        Message dislikeAck = client.receivedMessages.poll();
        Assert.assertTrue(dislikeAck instanceof ReactionResponse);

        try {
            worker.tell(new Dislike("Saimir", WRONG_COMMUNICATION_ID, message.getMessageId()));
            while (client.receivedMessages.isEmpty())
                system.runFor(1);
            Assert.fail();
        } catch (Exception ex) {
            Assert.assertTrue(ex instanceof UnknownClientException);
        }

        ReactionResponse reactionResponse2 = (ReactionResponse) dislikeAck;
        Assert.assertEquals(-1, reactionResponse2.points); // Assuming initial points are 0 and dislike subtracts a point

        worker.tell(new Like("Saimir", COMMUNICATION_ID, message.getMessageId())); // Assuming client ID is 20 and message ID is 1
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify the message was liked successfully
        Message lickeAck2 = client.receivedMessages.poll();
        Assert.assertTrue(lickeAck2 instanceof ReactionResponse);

        Stop stop = new Stop();
        Assert.assertTrue(stop.getDuration() != 0);
        worker.tell(stop);
    }

    @Test
    public void testEditMessages() throws UnknownClientException, UnknownMessageException {

        UserMessage message = new UserMessage("Saimir", "Hello");
        Assert.assertNotSame("", message.toString());

        // Send a "Publish" message to the worker
        worker.tell(new Publish(message, COMMUNICATION_ID));
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message postMessage = client.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, postMessage.getClass());


        // Edit message succesful
        worker.tell(new Edit(message.getMessageId(), "Saimir", "Edited", COMMUNICATION_ID)); // Assuming client ID is 20
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message editAck = client.receivedMessages.remove();
        Assert.assertTrue(editAck instanceof OperationAck);


        // Fail message same as previous + same user
        worker.tell(new Edit(message.getMessageId(), "Saimir", "Edited", COMMUNICATION_ID)); // Assuming client ID is 20
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message wrongAck = client.receivedMessages.remove();
        Assert.assertTrue(wrongAck instanceof OperationFailed);


        // Fail Message is too long
        worker.tell(new Edit(message.getMessageId(), "Saimir", "Edited too long", COMMUNICATION_ID)); // Assuming client ID is 20
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message editAck2 = client.receivedMessages.remove();
        Assert.assertTrue(editAck2 instanceof OperationFailed);


        // Fail Message has wrong mID
        worker.tell(new Edit(message.getMessageId() + 1, "Saimir", "wrongID", COMMUNICATION_ID)); // Assuming client ID is 20
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message editAck3 = client.receivedMessages.remove();
        Assert.assertTrue(editAck3 instanceof OperationFailed);


        // Fail Message by wrong author
        worker.tell(new Edit(message.getMessageId(), "WrongAuthor", "Edited", COMMUNICATION_ID)); // Assuming client ID is 20
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message editAck5 = client.receivedMessages.remove();
        Assert.assertTrue(editAck5 instanceof OperationFailed);



        // Cause exeption
        try {
            worker.tell(new Edit(message.getMessageId(), "Saimir", "Edited  ", WRONG_COMMUNICATION_ID));
            while (client.receivedMessages.isEmpty())
                system.runFor(1);
            Assert.fail();

        } catch (Exception ex) {
            Assert.assertTrue(ex instanceof UnknownClientException);
        }
    }

    @Test
    public void testReportBannedMessages() throws UnknownClientException, UnknownMessageException {

        UserMessage message = new UserMessage("Saimir", "Hello");
        Assert.assertNotSame("", message.toString());

        worker.tell(new Publish(message, COMMUNICATION_ID)); // Assuming client ID is 10
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message postMessage = client.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, postMessage.getClass());

        //Reporting Test
        for (int i = 0; i <= MessageStore.USER_BLOCKED_AT_COUNT; i++) {
            if (i == 1) {
                worker.tell(new Like("Saimir", COMMUNICATION_ID, message.getMessageId())); // Assuming client ID is 20 and message ID is 1
                while (client.receivedMessages.isEmpty())
                    system.runFor(1);
                Message likestuff = client.receivedMessages.remove();
                Assert.assertTrue(likestuff instanceof ReactionResponse);
            }
            worker.tell(new Report("Reporter" + i, COMMUNICATION_ID, "Saimir")); // Assuming client IDs start from 30
            while (client.receivedMessages.isEmpty())
                system.runFor(1);
            Message reportAck = client.receivedMessages.remove();
            Assert.assertTrue(reportAck instanceof OperationAck);
        }

        // Banned Tests Report
        Report reportMessage = new Report("Saimir", COMMUNICATION_ID, "Reporter");
        Assert.assertTrue(reportMessage.getDuration() != 0);
        worker.tell(reportMessage); // Assuming client IDs start from 30
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message reportAck = client.receivedMessages.remove();
        Assert.assertTrue(reportAck instanceof UserBanned);


        // Banned Tests Like
        worker.tell(new Like("Saimir", COMMUNICATION_ID, message.getMessageId())); // Assuming client ID is 20 and message ID is 1
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message likeAck = client.receivedMessages.remove();
        Assert.assertTrue(likeAck instanceof UserBanned);


        // Banned Tests Dislike
        worker.tell(new Dislike("Saimir", COMMUNICATION_ID, message.getMessageId())); // Assuming client ID is 20
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message dislikeAck = client.receivedMessages.remove();
        Assert.assertTrue(dislikeAck instanceof UserBanned);

        // Banned Tests remove Dislike
        RemoveLikeOrDislike removeLikeOrDislike = new RemoveLikeOrDislike("Saimir", COMMUNICATION_ID, message.getMessageId(), RemoveLikeOrDislike.Type.LIKE);
        Assert.assertTrue(removeLikeOrDislike.getDuration() != 0);
        worker.tell(removeLikeOrDislike);// Assuming client ID is 20
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message removeDislikeAck = client.receivedMessages.remove();
        Assert.assertTrue(removeDislikeAck instanceof UserBanned);


        // Banned Tests Edit
        worker.tell(new Edit(message.getMessageId(), "Saimir", "Edited", COMMUNICATION_ID)); // Assuming client ID is 20
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message editAck = client.receivedMessages.remove();
        Assert.assertTrue(editAck instanceof UserBanned);


        // Banned Tests Retrieve
        RetrieveMessages retriveMessage = new RetrieveMessages("Saimir", COMMUNICATION_ID);
        Assert.assertTrue(retriveMessage.getDuration() != 0);
        worker.tell(retriveMessage);
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message retrieveMessage = client.receivedMessages.remove();
        Assert.assertTrue(retrieveMessage instanceof FoundMessages);
        Assert.assertTrue(retrieveMessage.getDuration() != 0);

        // Banned Tests Delete
        Delete delete_Message = new Delete(message.getMessageId(), "Saimir", COMMUNICATION_ID);
        Assert.assertTrue(delete_Message.getDuration() != 0);
        worker.tell(delete_Message);
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message deleteMessage = client.receivedMessages.remove();
        Assert.assertTrue(deleteMessage instanceof UserBanned);

        UserMessage message2 = new UserMessage("Saimir", "Hell");
        Assert.assertNotSame("", message2.toString());

        // Banned Tests Update
        worker.tell(new Publish(message2, COMMUNICATION_ID));
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message publishmessgge = client.receivedMessages.remove();
        Assert.assertTrue(publishmessgge instanceof UserBanned);

        // Banned Tests Delete
        Reaction reaction = new Reaction(message.getAuthor(), COMMUNICATION_ID, message.getMessageId(), Reaction.Emoji.LAUGHING);
        Assert.assertTrue(reaction.getDuration() != 0);
        worker.tell(reaction);
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message reactionMessage = client.receivedMessages.remove();
        Assert.assertTrue(reactionMessage instanceof UserBanned);

        // Exception
        try {
            worker.tell(new Report("Reporter" + 10, WRONG_COMMUNICATION_ID, "Saimir"));
            while (client.receivedMessages.isEmpty())
                system.runFor(1);
        } catch (Exception ex) {
            Assert.assertTrue(ex instanceof UnknownClientException);

        }

        // Failed Report
        worker.tell(new Report("Reporter" + 0, COMMUNICATION_ID, "Saimir")); // Assuming client IDs start from 30
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message reportAck2 = client.receivedMessages.remove();
        Assert.assertTrue(reportAck2 instanceof OperationFailed);

        Assert.assertFalse(client.getMessageLog().isEmpty());

        Stop stop = new Stop();
        Assert.assertTrue(stop.getDuration() != 0);
        dispatcher.tell(stop);
        while (dispatcher.getMessageLog().size() > 2) {
            system.runFor(1);
        }

        boolean didStopWork = false;
        for (Message messageLog : dispatcher.getMessageLog()) {
            if (messageLog instanceof Stop) {
                didStopWork = true;
                break;
            }
        }

        Assert.assertTrue(didStopWork);

        Stop stop1 = new Stop();
        Assert.assertTrue(stop1.getDuration() != 0);
        worker.tell(stop1);

        boolean workerStopped = false;
        for (Message messageLog : worker.getMessageLog()) {
            worker.tick();
            if (messageLog instanceof Stop) {
                workerStopped = true;
                break;
            }
        }
        Assert.assertTrue(workerStopped);

        worker.receive(new SearchInStore("random", COMMUNICATION_ID));

        try {
            worker.tell(new Publish(message, WRONG_COMMUNICATION_ID));

            while (client.receivedMessages.isEmpty())
                system.runFor(1);
            Assert.fail();
        } catch (Exception ex) {
            Assert.assertTrue(ex instanceof UnknownClientException);
            Assert.assertEquals(ex.getMessage(), "Unknown communication ID");
        }
    }

    @Test
    public void TestFeatures() throws UnknownClientException, UnknownMessageException {
        SimulatedActorSystem system = new SimulatedActorSystem();
        system.tick();
        Dispatcher dispatcher = new Dispatcher(system, 2);
        system.spawn(dispatcher);
        TestClient client = new TestClient();
        system.spawn(client);
        Assert.assertEquals(system.getCurrentTime(), client.getTimeSinceSystemStart());
        Assert.assertFalse(system.getActors().isEmpty());
        int curr_time = system.getCurrentTime();
        system.runFor(1);
        Assert.assertTrue(curr_time < system.getCurrentTime());
        Assert.assertEquals(curr_time + 1, system.getCurrentTime());
        int curr_time2 = system.getCurrentTime();
        system.runUntil(curr_time2 + 2);
        Assert.assertEquals(curr_time2 + 3, system.getCurrentTime());
        client.tick();
        Assert.assertTrue(client.getTimeSinceSystemStart() != 0);
        Assert.assertFalse(client.getTimeSinceSystemStart() < 0);
        Assert.assertTrue(client.getId() >= 0);
        Assert.assertTrue(dispatcher.getId() >= 0);
        Assert.assertTrue(client.getId() != dispatcher.getId());
        client.tick();
        MessageStore store = new MessageStore();
        Assert.assertNotNull(store);
        store.receive(null);

    }

    /**
     * This function tests processSearchMessages(Message)
     */
    @Test
    public void testProcessSearchMessages() throws UnknownMessageException, UnknownClientException {

        UserMessage message = new UserMessage("Joulian", "HelloWorld");
        Assert.assertNotSame("", message.toString());

        // Send a "Publish" message to the worker
        worker.tell(new Publish(message, COMMUNICATION_ID)); // Assuming client ID is 10

        // Run the system until the message is processed
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify that the message is successfully posted on the message board
        Message postMessage = client.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, postMessage.getClass());


        worker.tell(new SearchMessages("World", COMMUNICATION_ID));
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        Message retrieveMessageSuccess = client.receivedMessages.remove();
        Assert.assertTrue(retrieveMessageSuccess instanceof FoundMessages);
        Assert.assertEquals("HelloWorld", ((FoundMessages) retrieveMessageSuccess).messages.get(0).getMessage());
        Assert.assertEquals("Joulian", ((FoundMessages) retrieveMessageSuccess).messages.get(0).getAuthor());
        Assert.assertTrue(retrieveMessageSuccess.getDuration() != 0);

        SearchMessages searchMessage = new SearchMessages("Joulian", COMMUNICATION_ID);
        Assert.assertTrue(searchMessage.getDuration() != 0);
        worker.tell(searchMessage);
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message retrieveMessageSuccess2 = client.receivedMessages.remove();
        Assert.assertTrue(retrieveMessageSuccess2 instanceof FoundMessages);
        Assert.assertEquals("HelloWorld", ((FoundMessages) retrieveMessageSuccess2).messages.get(0).getMessage());
        Assert.assertEquals("Joulian", ((FoundMessages) retrieveMessageSuccess2).messages.get(0).getAuthor());
        Assert.assertTrue(retrieveMessageSuccess2.getDuration() != 0);

        worker.tell(new SearchMessages("wrongmessage", COMMUNICATION_ID));
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message retrieveFail1 = client.receivedMessages.remove();
        Assert.assertTrue(retrieveFail1 instanceof FoundMessages);
        Assert.assertTrue(retrieveFail1.getDuration() != 0);

        try {
            worker.tell(new SearchMessages("World", WRONG_COMMUNICATION_ID));
            while (client.receivedMessages.isEmpty())
                system.runFor(1);

            Assert.fail();
        } catch (Exception ex) {
            Assert.assertTrue(ex instanceof UnknownClientException);
            Assert.assertEquals(ex.getMessage(), "Unknown communication ID");
        }
    }

    /**
     * This function test the process stop
     *
     * @throws UnknownMessageException
     * @throws UnknownClientException
     */
    @Test
    public void testProcessStop() throws UnknownMessageException, UnknownClientException {

        UserMessage message = new UserMessage("Joulian", "Hello");
        Assert.assertNotSame("", message.toString());

        // Send a "Publish" message to the worker
        worker.tell(new Publish(message, COMMUNICATION_ID)); // Assuming client ID is 10

        // Run the system until the message is processed
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify that the message is successfully posted on the message board
        Message postMessage = client.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, postMessage.getClass());

        Stop stop = new Stop();
        Assert.assertTrue(stop.getDuration() != 0);
        dispatcher.tell(stop);

        while (dispatcher.getMessageLog().size() > 2) {
            system.runFor(1);
        }

        boolean didStopWork = false;
        for (Message messageLog : dispatcher.getMessageLog()) {
            if (messageLog instanceof Stop) {
                didStopWork = true;
                break;
            }
        }

        Assert.assertTrue(didStopWork);

        worker.tell(new Stop());
        boolean workerStopped = false;
        for (Message messageLog : worker.getMessageLog()) {
            if (messageLog instanceof Stop) {
                workerStopped = true;
                break;
            }
        }
        Assert.assertTrue(workerStopped);


        worker.tell(new Publish(message, COMMUNICATION_ID)); // Assuming client ID is 10

        // Run the system until the message is processed
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message publishFail = client.receivedMessages.remove();
        Assert.assertTrue(publishFail instanceof OperationFailed);

    }

    /**
     * This function test the process finish communication
     *
     * @throws UnknownMessageException
     * @throws UnknownClientException
     */
    @Test
    public void testProcessFinishCommunication() throws UnknownMessageException, UnknownClientException {

        UserMessage message = new UserMessage("Joulian", "Hello");
        Assert.assertNotSame("", message.toString());

        // Send a "Publish" message to the worker
        worker.tell(new Publish(message, COMMUNICATION_ID)); // Assuming client ID is 10

        // Run the system until the message is processed
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify that the message is successfully posted on the message board
        Message postMessage = client.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, postMessage.getClass());

        FinishCommunication finishCommunication = new FinishCommunication(COMMUNICATION_ID);
        Assert.assertTrue(finishCommunication.getDuration() != 0);
        worker.tell(finishCommunication);
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        Message finAckMessage = client.receivedMessages.remove();
        Assert.assertEquals(FinishAck.class, finAckMessage.getClass());
        FinishAck finAck = (FinishAck) finAckMessage;
        Assert.assertEquals(Long.valueOf(COMMUNICATION_ID), finAck.communicationId);
        Assert.assertTrue(finAck.getDuration() != 0);

        try {
            FinishCommunication finishCommunication2 = new FinishCommunication(WRONG_COMMUNICATION_ID);
            Assert.assertTrue(finishCommunication2.getDuration() != 0);
            worker.tell(finishCommunication2);
            while (client.receivedMessages.isEmpty())
                system.runFor(1);

            Assert.fail();
        } catch (Exception ex) {
            Assert.assertTrue(ex instanceof UnknownClientException);
            Assert.assertEquals(ex.getMessage(), "Unknown communication ID");
        }

    }

    @Test
    public void testProcessDeleteLikeOrDislike() throws UnknownMessageException, UnknownClientException {

        UserMessage message = new UserMessage("Dino", "Cevapi");
        Assert.assertNotSame("", message.toString());

        // Send a "Publish" message to the worker
        worker.tell(new Publish(message, COMMUNICATION_ID)); // Assuming client ID is 30

        // Run the system until the message is processed
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify that the message is successfully posted on the message board
        Message postMessage = client.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, postMessage.getClass());

        worker.tell(new Like("Dino", COMMUNICATION_ID, message.getMessageId())); // Assuming client ID is 30
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message likeAck = client.receivedMessages.remove();
        Assert.assertTrue(likeAck instanceof ReactionResponse);
        Assert.assertEquals(1, message.getLikes().size()); //check if like was added to list

        RemoveLikeOrDislike removeLikeOrDislike = new RemoveLikeOrDislike("Dino", COMMUNICATION_ID, message.getMessageId(), RemoveLikeOrDislike.Type.LIKE);
        Assert.assertTrue(removeLikeOrDislike.getDuration() != 0);
        worker.tell(removeLikeOrDislike);
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message likeRemoveAck = client.receivedMessages.remove();
        Assert.assertTrue(likeRemoveAck instanceof ReactionResponse);
        Assert.assertEquals(0, message.getLikes().size()); //check if like was removed from list

        //==============wrong message ID
        RemoveLikeOrDislike removeLikeOrDislike2 = new RemoveLikeOrDislike("Dino", COMMUNICATION_ID, message.getMessageId() + 10, RemoveLikeOrDislike.Type.LIKE);
        Assert.assertTrue(removeLikeOrDislike2.getDuration() != 0);
        worker.tell(removeLikeOrDislike2);// Assuming client ID is 20
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message removeDislikeAck = client.receivedMessages.remove();
        Assert.assertTrue(removeDislikeAck instanceof OperationFailed);

        try {
            RemoveLikeOrDislike removeLikeOrDislike3 = new RemoveLikeOrDislike("Dino", WRONG_COMMUNICATION_ID, message.getMessageId(), RemoveLikeOrDislike.Type.LIKE);
            Assert.assertTrue(removeLikeOrDislike3.getDuration() != 0);
            worker.tell(removeLikeOrDislike3);
            while (client.receivedMessages.isEmpty())
                system.runFor(1);

            Assert.fail();
        } catch (Exception ex) {
            Assert.assertTrue(ex instanceof UnknownClientException);
            Assert.assertEquals(ex.getMessage(), "Unknown communication ID");
        }
    }


    @Test
    public void testProcessDeleteMessage() throws UnknownMessageException, UnknownClientException {

        UserMessage message = new UserMessage("Dino", "Cevapi");
        Assert.assertNotSame("", message.toString());

        // Send a "Publish" message to the worker
        worker.tell(new Publish(message, COMMUNICATION_ID)); // Assuming client ID is 30

        // Run the system until the message is processed
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify that the message is successfully posted on the message board
        Message postMessage = client.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, postMessage.getClass());

        //wrong author try to delete
        worker.tell(new Delete(message.getMessageId(), "Saimir", COMMUNICATION_ID));
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message msgDeleteFail1 = client.receivedMessages.remove();
        Assert.assertTrue(msgDeleteFail1 instanceof OperationFailed);
        Assert.assertEquals(OperationFailed.class, msgDeleteFail1.getClass()); //verify that the message wasn't deleted

        //correct author try to delete
        worker.tell(new Delete(message.getMessageId(), "Dino", COMMUNICATION_ID));
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message msgDeleteAck = client.receivedMessages.remove();

        Assert.assertTrue(msgDeleteAck instanceof OperationAck);

        //invalid author
        worker.tell(new Delete(message.getMessageId(), "Dino" + 1, COMMUNICATION_ID));
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message msgDeleteFail2 = client.receivedMessages.remove();

        Assert.assertTrue(msgDeleteFail2 instanceof OperationFailed);

        try {
            worker.tell(new Delete(message.getMessageId(), "Dino", WRONG_COMMUNICATION_ID)); //wrong communication ID
            while (client.receivedMessages.isEmpty())
                system.runFor(1);

            Assert.fail();
        } catch (Exception ex) {
            Assert.assertTrue(ex instanceof UnknownClientException);
            Assert.assertEquals(ex.getMessage(), "Unknown communication ID");
        }


    }


    @Test
    public void testWorkerTick() throws UnknownMessageException, UnknownClientException {
        // An instance of a simulated actor system is created. This system manages actors and message passing between them.
        SimulatedActorSystem system = new SimulatedActorSystem();

        // A dispatcher actor is created with a capacity of 1. The dispatcher is responsible for managing message routing and delivery.
        Dispatcher dispatcher = new Dispatcher(system, 20);

        // A test client actor is created. This represents a client in the simulated system.
        TestClient client = new TestClient();

        // The dispatcher actor is spawned within the actor system.
        system.spawn(dispatcher);

        // The test client actor is spawned within the actor system.
        system.spawn(client);
        SimulatedActor[] workers = new SimulatedActor[20];

        for (int i = 0; i < 20; i++) {
            InitCommunication initCommunication = new InitCommunication(client, COMMUNICATION_ID + i);
            Assert.assertTrue(initCommunication.getDuration() != 0);
            dispatcher.tell(initCommunication);
            // A loop runs until the client receives some messages. The runFor method advances the simulation by 1 time unit.
            while (client.receivedMessages.isEmpty())
                system.runFor(1);

            //  Once the client receives a message, it is removed from its list of received messages.
            Message initAckMessage = client.receivedMessages.remove();
            // An assertion checks if the received message is an instance of InitAck.
            // If true then the communication initiation was successful
            Assert.assertTrue(initAckMessage instanceof InitAck);

            InitAck initAck = (InitAck) initAckMessage;
            Assert.assertTrue(initAck.getDuration() != 0);
            workers[i] = initAck.worker;

        }

        for (int i = 0; i < 20; i++) {
            UserMessage message = new UserMessage("Dino" + i, "Cevapi");
            Assert.assertNotSame("", message.toString());

            // Send a "Publish" message to the worker
            workers[i].tell(new Publish(message, COMMUNICATION_ID + i)); // Assuming client ID is 10


            // Verify that the message is successfully posted on the message board
        }
        for (int i = 0; i < 20; i++) {
            while (client.receivedMessages.isEmpty())
                system.runFor(1);


            // Verify that the message is successfully posted on the message board
            client.receivedMessages.remove();
        }
    }

    @Test
    public void testMessageWrongType() {

        try {
            worker.receive(null);
            Assert.fail();
        } catch (Exception ex) {
            Assert.assertTrue(ex instanceof UnknownMessageException);
            Assert.assertEquals(ex.getMessage(), "Worker received message of not existing type.");
        }
    }

    @Test
    public void testReaction() throws UnknownMessageException, UnknownClientException {
        // An instance of a simulated actor system is created. This system manages actors and message passing between them.
        SimulatedActorSystem system = new SimulatedActorSystem();

        // A dispatcher actor is created with a capacity of 1. The dispatcher is responsible for managing message routing and delivery.
        Dispatcher dispatcher = new Dispatcher(system, 1);

        // A test client actor is created. This represents a client in the simulated system.
        TestClient client = new TestClient();

        // The dispatcher actor is spawned within the actor system.
        system.spawn(dispatcher);

        // The test client actor is spawned within the actor system.
        system.spawn(client);

        long comID = 30L;
        InitCommunication initcom = new InitCommunication(client, comID);
        Assert.assertTrue(initcom.getDuration() != 0);
        initcom.setCommunicationId(comID);

        // Client sends InitCommunication message to the Dispatcher
        dispatcher.tell(initcom);
        // A loop runs until the client receives some messages. The runFor method advances the simulation by 1 time unit.
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        //  Once the client receives a message, it is removed from its list of received messages.
        Message initAckMessage = client.receivedMessages.remove();
        // An assertion checks if the received message is an instance of InitAck.
        // If true then the communication initiation was successful
        Assert.assertTrue(initAckMessage instanceof InitAck);

        InitAck initAck = (InitAck) initAckMessage;
        Assert.assertTrue(initAck.getDuration() != 0);
        SimulatedActor worker = initAck.worker;

        UserMessage message = new UserMessage("Dino", "Cevapi");
        Assert.assertNotSame("", message.toString());

        // Send a "Publish" message to the worker
        worker.tell(new Publish(message, initcom.getCommunicationId())); // Assuming client ID is 30

        // Run the system until the message is processed
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify that the message is successfully posted on the message board
        Message postMessage = client.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, postMessage.getClass());

        Reaction reaction = new Reaction(message.getAuthor(), initcom.getCommunicationId(), message.getMessageId(), Reaction.Emoji.COOL);
        Assert.assertTrue(reaction.getDuration() != 0);
        worker.tell(reaction);
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        Message reactionMessage = client.receivedMessages.remove();
        Assert.assertTrue(reactionMessage instanceof ReactionResponse);
        Assert.assertEquals(1, message.getReactions().size()); //check if reaction was added

        //wrong message ID
        Reaction reaction2 = new Reaction(message.getAuthor(), initcom.getCommunicationId(), message.getMessageId() + 1, Reaction.Emoji.COOL);
        Assert.assertTrue(reaction2.getDuration() != 0);
        worker.tell(reaction2);

        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        Message reactionMessage2 = client.receivedMessages.remove();
        Assert.assertTrue(reactionMessage2 instanceof OperationFailed);

        //adding the same reaction twice to a message
        Reaction reaction3 =  new Reaction(message.getAuthor(), initcom.getCommunicationId(), message.getMessageId(), Reaction.Emoji.COOL);
        Assert.assertTrue(reaction3.getDuration() != 0);
        worker.tell(reaction3);

        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        Message reactionMessage3 = client.receivedMessages.remove();
        Assert.assertTrue(reactionMessage3 instanceof OperationFailed);

        //change reaction
        Reaction reaction4 = new Reaction(message.getAuthor(), initcom.getCommunicationId(), message.getMessageId(), Reaction.Emoji.LAUGHING);
        Assert.assertTrue(reaction4.getDuration() != 0);
        worker.tell(reaction4);
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        Message reactionMessage4 = client.receivedMessages.remove();
        Assert.assertTrue(reactionMessage4 instanceof ReactionResponse);
        Assert.assertEquals(1, message.getReactions().size()); //check if reaction was added


        try {
            Reaction reaction5 = new Reaction(message.getAuthor(), WRONG_COMMUNICATION_ID, message.getMessageId(), Reaction.Emoji.COOL);
            Assert.assertTrue(reaction5.getDuration() != 0);
            worker.tell(reaction5);
            while (client.receivedMessages.isEmpty())
                system.runFor(1);

            Assert.fail();
        } catch (Exception ex) {
            Assert.assertTrue(ex instanceof UnknownClientException);
            Assert.assertEquals(ex.getMessage(), "Unknown communication ID");
        }

    }

    @Test
    public void testLikeDislikeWrongAuthor() throws UnknownMessageException, UnknownClientException {

        UserMessage message = new UserMessage("Dino", "Cevapi");
        Assert.assertNotSame("", message.toString());

        // Send a "Publish" message to the worker
        worker.tell(new Publish(message, COMMUNICATION_ID)); // Assuming client ID is 30

        // Run the system until the message is processed
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify that the message is successfully posted on the message board
        Message postMessage = client.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, postMessage.getClass());

        worker.tell(new Like("Dino", COMMUNICATION_ID, message.getMessageId())); // Assuming client ID is 30
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message likeAck = client.receivedMessages.remove();
        Assert.assertTrue(likeAck instanceof ReactionResponse);
        Assert.assertEquals(1, message.getLikes().size()); //check if like was added to list

        //wrong author
        RemoveLikeOrDislike removeLikeOrDislike = new RemoveLikeOrDislike("Dino" + 1, COMMUNICATION_ID, message.getMessageId(), RemoveLikeOrDislike.Type.LIKE);
        Assert.assertTrue(removeLikeOrDislike.getDuration() != 0);
        worker.tell(removeLikeOrDislike);
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message likeRemoveAck = client.receivedMessages.remove();
        Assert.assertTrue(likeRemoveAck instanceof OperationFailed);
        Assert.assertEquals(1, message.getLikes().size()); //check if like was removed from list

        //wrong author
        RemoveLikeOrDislike removeLikeOrDislike2 = new RemoveLikeOrDislike("Dino" + 1, COMMUNICATION_ID, message.getMessageId(), RemoveLikeOrDislike.Type.DISLIKE);
        Assert.assertTrue(removeLikeOrDislike2.getDuration() != 0);
        worker.tell(removeLikeOrDislike2);
        while (client.receivedMessages.isEmpty())
            system.runFor(1);
        Message dislikeRemoveAck = client.receivedMessages.remove();
        Assert.assertTrue(dislikeRemoveAck instanceof OperationFailed);

        //wrong type
        try {
            RemoveLikeOrDislike removeLikeOrDislike3 = new RemoveLikeOrDislike("Dino", COMMUNICATION_ID, message.getMessageId(), null);
            Assert.assertTrue(removeLikeOrDislike3.getDuration() != 0);
            worker.tell(removeLikeOrDislike3);
            while (client.receivedMessages.isEmpty())
                system.runFor(1);

            Assert.fail();
        } catch (Exception ex) {
            Assert.assertTrue(ex instanceof NullPointerException);
            Assert.assertEquals(ex.getMessage(), "Unknown delete type.");
        }


    }

    @Test
    public void testDispatcher() throws UnknownClientException, UnknownMessageException {
        //this test adds a message type to dispatcher message log which is of unknown type

        UserMessage message = new UserMessage("Dino", "Cevapi");
        Assert.assertNotSame("", message.toString());

        worker.tell(new Publish(message, COMMUNICATION_ID)); // Assuming client ID is 30

        // Run the system until the message is processed
        while (client.receivedMessages.isEmpty())
            system.runFor(1);

        // Verify that the message is successfully posted on the message board
        Message postMessage = client.receivedMessages.remove();
        Assert.assertEquals(OperationAck.class, postMessage.getClass());

        Delete delete = new Delete(message.getMessageId(), "Dino", COMMUNICATION_ID);
        Assert.assertTrue(delete.getDuration() != 0);
        Assert.assertEquals(COMMUNICATION_ID, (long) delete.getCommunicationId());
        dispatcher.tell(delete);
        system.runFor(5);

        Assert.assertTrue(dispatcher.getMessageLog().get(1) instanceof Delete); //check if delete was added to dispatcher log
        Assert.assertEquals(2, dispatcher.getMessageLog().size()); //there should be init and delete in the message log
    }
}
