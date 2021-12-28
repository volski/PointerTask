package com.volski.PointerTask;

import java.util.logging.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class RestVerticle extends AbstractVerticle {
    private static Logger log = Logger.getLogger(RestVerticle.class.getPackageName());

    @Override
    public void start(Promise<Void> startPromise) {
        Router router = createRouter();
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(9999, http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        System.out.println("HTTP server started on port 9999");
                    } else {
                        startPromise.fail(http.cause());
                    }
                });
    }

    private Router createRouter() {
        Router router = Router.router(vertx);
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
        router.route().handler(BodyHandler.create());
        router.get("/").handler(this::helloHandler);
        router.post("/Login").handler(this::loginHandler);
        router.post("/Logout").handler(this::checkAuth).handler(this::logoutHandler);
        router.get("/GetOrders").handler(this::checkAuth).handler(this::getOrdersHandler);
        router.post("/AddOrder").handler(this::checkAuth).handler(this::addOrderHandler);
        return router;
    }

    private void checkAuth(RoutingContext routingContext) {
        Session session = routingContext.session();
        Boolean _isLoggedIn = session.get("isLoggedIn");
        if (_isLoggedIn != null && _isLoggedIn == true) {
            routingContext.next();
        } else {
            routingContext.fail(401);
        }
    }

    private void loginHandler(RoutingContext routingContext) {
        Session session = routingContext.session();
        JsonObject json = routingContext.getBodyAsJson();
        JsonObject resJson = new JsonObject();
        FileSystem fs = vertx.fileSystem();
        fs.readFile("users.json", res -> {
            if (res.succeeded()) {
                Buffer buf = res.result();
                JsonArray jsonArray = buf.toJsonArray();
                for (Object aJsonArray : jsonArray) {
                    JsonObject obj = (JsonObject) aJsonArray;
                    if (obj.getValue("username").equals(json.getValue("username"))
                            && obj.getValue("password").equals(json.getValue("password"))) {
                        session.put("isLoggedIn", true);
                        log.info("logged in");
                        resJson.put("error", "false");
                        resJson.put("login", "true");
                        routingContext.response()
                                .putHeader("content-type", "application/json")
                                .setStatusCode(200)
                                .end(Json.encodePrettily(resJson));
                        return;
                    }
                }
                resJson.put("error", "false");
                resJson.put("login", "false");
                routingContext.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(200)
                        .end(Json.encodePrettily(resJson));

            } else {
                resJson.put("error", "cant read file");
                resJson.put("login", "false");
                routingContext.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(200)
                        .end(Json.encodePrettily(resJson));
            }
        });

        // log.info(file.getName());

    }

    private void logoutHandler(RoutingContext routingContext) {
        Session session = routingContext.session();
        JsonObject resJson = new JsonObject();
        session.put("isLoggedIn", false);
        session.destroy();
        resJson.put("isLoggedIn", "false");
        routingContext.response()
                .putHeader("content-type", "application/json")
                .setStatusCode(200)
                .end(Json.encodePrettily(resJson));

    }

    private void addOrderHandler(RoutingContext routingContext) {
        JsonObject jsonBody = routingContext.getBodyAsJson();
        vertx.eventBus().request("addOrder", jsonBody, handler -> {
            if (handler.succeeded()) {
                log.info("ACCESS 200 [/addOrder]");
                routingContext.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(200)
                        .end(Json.encodePrettily(handler.result().body()));
            } else {
                log.info("ACCESS 500 [/addOrder]");
                routingContext.response().setStatusCode(500)
                        .end(handler.cause().getMessage());
            }
        });
    }

    private void getOrdersHandler(RoutingContext routingContext) {
        vertx.eventBus().request("getOrders", null, handler -> {
            if (handler.succeeded()) {
                log.info("ACCESS 200 [/getOrders]");
                routingContext.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(200)
                        .end(Json.encodePrettily(handler.result().body()));
            } else {
                log.info("ACCESS 500 [/getOrders]");
                routingContext.response().setStatusCode(500)
                        .end(handler.cause().getMessage());
            }
        });
    }

    private void helloHandler(RoutingContext context) {
        log.info("ACCESS 200 [/]");
        context.response()
                .putHeader("content-type", "text/plain")
                .end("Hello from Vert.x!");
    }
}
