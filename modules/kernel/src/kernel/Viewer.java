package kernel;

import rescuecore2.connection.Connection;
import rescuecore2.messages.Command;
import rescuecore2.messages.control.Update;
import rescuecore2.messages.control.Commands;
import rescuecore2.worldmodel.Entity;

import java.util.Collection;
import java.util.Collections;

/**
   This class is the kernel interface to a viewer.
 */
public class Viewer extends AbstractComponent {
    private int id;

    /**
       Construct a viewer.
       @param c The connection to the viewer.
       @param id The ID of the viewer.
     */
    public Viewer(Connection c, int id) {
        super(c);
        this.id = id;
    }

    /**
       Send an update message to this viewer.
       @param time The simulation time.
       @param updates The updated entities.
    */
    public void sendUpdate(int time, Collection<? extends Entity> updates) {
        send(Collections.singleton(new Update(id, time, updates)));
    }

    /**
       Send a set of agent commands to this viewer.
       @param time The current time.
       @param commands The agent commands to send.
     */
    public void sendAgentCommands(int time, Collection<? extends Command> commands) {
        send(Collections.singleton(new Commands(id, time, commands)));
    }

    @Override
    public String toString() {
        return "Viewer " + id + ": " + getConnection().toString();
    }
}