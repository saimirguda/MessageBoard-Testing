package at.tugraz.ist.qs2024.messageboard.clientmessages;

/**
 * Reply message sent from worker to client if a request succeeded.
 */
public class OperationAck extends Reply {
    public OperationAck(long communicationId) {
        super(communicationId);
    }
}
