package models;

import play.mvc.*;
import play.libs.*;
import play.libs.F.*;

import akka.util.*;
import akka.actor.*;
import akka.dispatch.*;
import static akka.pattern.Patterns.ask;

import org.codehaus.jackson.*;
import org.codehaus.jackson.node.*;

import java.util.*;

import static java.util.concurrent.TimeUnit.*;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class Reversi extends UntypedActor {

    static ActorRef game = Akka.system().actorOf(new Props(Reversi.class));
    Map<String, WebSocket.Out<JsonNode>> members = new HashMap<String, WebSocket.Out<JsonNode>>();

    public static void join(final String username, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception {
        String result = (String) Await.result(ask(game, new Join(username, out), 1000), Duration.create(1, SECONDS));
        if ("OK".equals(result)) {
            in.onMessage(new Callback<JsonNode>() {

                public void invoke(JsonNode event) {

                    // Send a Talk message to the room.
                    game.tell(new Message(
                            username, 
                            event.get("text").asText(), 
                            event.get("x").asInt(), 
                            event.get("y").asInt(),
                            event.get("uuid").asText()
                            ));

                }
            });
            in.onClose(new Callback0() {

                public void invoke() {
                    game.tell(new Quit(username));
                }
            });
        } else {
            ObjectNode error = Json.newObject();
            error.put("error", result);
            out.write(error);
        }

    }

    public void onReceive(Object message) throws Exception {
        if (message instanceof Join) {
            Join join = (Join) message;
            if (members.containsKey(join.username)) {
                getSender().tell("This username is already used");
            } else {
                members.put(join.username, join.channel);
                notifyAll("join", join.username, "has entered the room", null, null, null);
                getSender().tell("OK");
            }
        } else if (message instanceof Message) {
            Message talk = (Message) message;
            notifyAll("talk", talk.username, talk.text, talk.x, talk.y, talk.uuid);
        } else if (message instanceof Quit) {
            Quit quit = (Quit) message;
            members.remove(quit.username);
            notifyAll("quit", quit.username, "has leaved the room", null, null, null);
        } else {
            unhandled(message);
        }
    }

    public void notifyAll(String kind, String user, String text, Integer x, Integer y, String uuid) {
        for (WebSocket.Out<JsonNode> channel : members.values()) {
            ObjectNode event = Json.newObject();
            event.put("kind", kind);
            event.put("user", user);
            event.put("x", x);
            event.put("y", y);
            event.put("uuid", uuid);
            event.put("message", text);
            ArrayNode m = event.putArray("members");
            for (String u : members.keySet()) {
                m.add(u);
            }
            System.out.println(ToStringBuilder.reflectionToString(event, ToStringStyle.MULTI_LINE_STYLE));
            channel.write(event);
        }        
    }
    public static class Join {

        final String username;
        final WebSocket.Out<JsonNode> channel;

        public Join(String username, WebSocket.Out<JsonNode> channel) {
            this.username = username;
            this.channel = channel;
        }
    }
    public static class Message {

        final String username;
        final String text;
        final Integer x;
        final Integer y;
        final String uuid;

        public Message(String username, String text, Integer x, Integer y, String uuid) {
            this.username = username;
            this.text = text;
            this.x = x;
            this.y = y;
            this.uuid = uuid;
        }
    }
    public static class Quit {
        final String username;
        public Quit(String username) {
            this.username = username;
        }
    }
}
