package at.tugraz.ist.qs2024.actorsystem;

import at.tugraz.ist.qs2024.messageboard.UnknownClientException;
import at.tugraz.ist.qs2024.messageboard.UnknownMessageException;

import java.util.ArrayList;
import java.util.List;

public class SimulatedActorSystem implements ISimulatedActorSystem {
    private final List<SimulatedActor> actors = new ArrayList<>();
    private int currentTime = 0;

    /**
     * Integral number used for creating actor IDs, which is incremented every time an actor is started.
     */
    private long currentActorId = 0;

    @Override
    public List<SimulatedActor> getActors() {
        return this.actors;
    }

    @Override
    public int getCurrentTime() {
        return this.currentTime;
    }

    @Override
    public void spawn(SimulatedActor actor) {
        actors.add(actor);
        actor.setId(currentActorId++);
        actor.atStartUp();
        actor.setTimeSinceSystemStart(currentTime);
    }

    @Override
    public void runFor(int numberOfTicks) throws UnknownClientException, UnknownMessageException {
        for (int i = 0; i < numberOfTicks; i++) {
            tick();
        }
    }

    @Override
    public void runUntil(int endTime) throws UnknownClientException, UnknownMessageException {
        while (currentTime <= endTime) {
            tick();
        }
    }

    @Override
    public void stop(SimulatedActor actor) {
        actors.remove(actor);
    }

    @Override
    public void tick() throws UnknownClientException, UnknownMessageException {
        // need to copy list, because actors might be spawned or stopped
        // during tick which modifies the actors-list
        List<SimulatedActor> currentlyAliveActors = new ArrayList<>(actors);
        for (SimulatedActor actor : currentlyAliveActors) {
            actor.tick();
        }
        currentTime++;
    }
}
