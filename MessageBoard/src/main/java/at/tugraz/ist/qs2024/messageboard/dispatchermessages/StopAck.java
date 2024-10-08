package at.tugraz.ist.qs2024.messageboard.dispatchermessages;

import at.tugraz.ist.qs2024.actorsystem.Message;
import at.tugraz.ist.qs2024.actorsystem.SimulatedActor;

/**
 * Message sent from worker to dispatcher to acknowledge the
 * stop message.
 */
public class StopAck implements Message {
    /**
     * The sender of this message
     */
    public final SimulatedActor sender;

    public StopAck(SimulatedActor sender) {
        this.sender = sender;
    }

    @Override
    public int getDuration() {
        return 2;
    }
}
