package com.volski.PointerTask;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileSystem;

public class OrderVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> promise) {
        vertx.eventBus().consumer("addOrder", m -> {
            JsonObject reqBody = (JsonObject) m.body();
            addOrder(m, reqBody.getString("orderID"), reqBody.getString("orderName"), reqBody.getString("orderDate"));
        });
        vertx.eventBus().consumer("getOrders", m -> {
            getOrders(m);
        });
        promise.complete();
    }

    private void getOrders(Message<Object> m) {
        FileSystem fs = vertx.fileSystem();
        fs.readFile("orders.json", res -> {
            if (res.succeeded()) {
                Buffer buf = res.result();
                JsonArray jsonArray = buf.toJsonArray();
                m.reply(jsonArray);
            } else {
                m.reply(null);
            }
        });
    }

    private void addOrder(Message<Object> m, String orderId, String orderName, String orderDate) {
        JsonObject resJson = new JsonObject();
        FileSystem fs = vertx.fileSystem();
        fs.readFile("orders.json", res -> {
            if (res.succeeded()) {
                Buffer buf = res.result();
                JsonArray jsonArray = buf.toJsonArray();
                JsonObject newOrder = new JsonObject();
                newOrder.put("orderId", orderId);
                newOrder.put("orderName", orderName);
                newOrder.put("orderDate", orderDate);
                jsonArray.add(newOrder);
                Buffer resBuffer = jsonArray.toBuffer();
                vertx.fileSystem().writeFile("orders.json", resBuffer, ar -> {
                    if (ar.failed()) {
                        resJson.put("error", "true");
                        resJson.put("insert", "false");
                        m.reply(resJson);
                    } else {
                        resJson.put("error", "false");
                        resJson.put("insert", "true");
                        m.reply(resJson);
                    }
                });
            } else {
                resJson.put("error", "true");
                resJson.put("insert", "false");
                m.reply(resJson);
            }
        });
    }
}
