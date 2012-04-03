package controllers;

import play.*;
import play.mvc.*;
import play.libs.F.*;

import org.codehaus.jackson.*;

import views.html.*;

import models.*;

public class Application extends Controller {
  
    public static Result index() {
        return ok(index.render());
    }
  
    public static Result reversi(String username) {
        if(username == null || username.trim().equals("")) {
            flash("error", "Please choose a valid username.");
            return redirect(routes.Application.index());
        }
        return ok(reversi.render(username));
    }
    
    public static WebSocket<JsonNode> game(final String username) {
        return new WebSocket<JsonNode>() {
            // Called when the Websocket Handshake is done.
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
                // Join the chat room.
                try { 
                    Reversi.join(username, in, out);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }
  
}
