package ca.yorku.eecs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import java.io.IOException;


public class AddActor implements HttpHandler {

    public AddActor() {
    }

    public void handle(HttpExchange r) {
        try {
            if (r.getRequestMethod().equals("PUT")) {
                handlePut(r);
            } else {
                r.sendResponseHeaders(404, -1);
            }
        } catch (Exception e) {
           System.out.println(e.getMessage());
        }
    }

    public void handlePut(HttpExchange r) throws IOException, JSONException {
        // convert the request body
        String body = Utils.convert(r.getRequestBody());

        // get the deserialized JSON
        JSONObject deserialized = new JSONObject(body);

        // variables to hold the HTTP status code, the name and actorID of the actor
        int statusCode = 0;
        String actorName = "";
        String actorId = "";

        // check if the required information is present in the body. If not, raise error 400
        if (deserialized.has("name"))
            actorName = deserialized.getString("name");
        else
            statusCode = 400;

        if (deserialized.has("actorId"))
            actorId = deserialized.getString("actorId");
        else
            statusCode = 400;

        try (Transaction tx = Utils.driver.session().beginTransaction()) {
            // check if there is any data with the same actorId
            StatementResult result = tx.run("MATCH (a:actor {id: $actorId}) RETURN a",
                    org.neo4j.driver.v1.Values.parameters("actorId", actorId));

            // check for duplicate entries
            if (result.hasNext()) {
                statusCode = 400;
            } else {
                // make the query
                tx.run("CREATE (a:actor {Name: $actorName, id: $actorId})",
                        org.neo4j.driver.v1.Values.parameters("actorName", actorName, "actorId", actorId));

                // commit the query for persistence
                tx.success();

                System.out.println("Actor added: " + actorName);
                statusCode = 200;
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            statusCode = 500;
        }

        r.sendResponseHeaders(statusCode, -1);
    }
}
