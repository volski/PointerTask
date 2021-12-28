package com.volski.PointerTask;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.hazelcast.config.Config;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class Main {
  private static Logger log = Logger
      .getLogger(Main.class.getPackageName());

  public static void main(String[] args) {
    Config hazelcastConfig = new Config();

    ClusterManager mgr = new HazelcastClusterManager(hazelcastConfig);
    VertxOptions options = new VertxOptions()
        .setClusterManager(mgr);

    String containerAddress = getAddress();

    EventBusOptions ebOptions = new EventBusOptions()
        .setHost(containerAddress)
        .setClustered(true);

    options.setEventBusOptions(ebOptions);

    Vertx.clusteredVertx(options, handler -> {
      if (handler.succeeded()) {
        handler.result()
            .deployVerticle(OrderVerticle.class,
                new DeploymentOptions(),
                deployHandler -> {
                  if (handler.succeeded()) {
                    log.info("OrderVerticle Deployed");
                  } else {
                    log.severe("OrderVerticle Deployment Failed");
                  }
                });
      }
    });
  }

  private static String getAddress() {
    try {
      List<NetworkInterface> networkInterfaces = new ArrayList<>();
      NetworkInterface.getNetworkInterfaces()
          .asIterator().forEachRemaining(networkInterfaces::add);
      return networkInterfaces.stream()
          .flatMap(iface -> iface.inetAddresses()
              .filter(entry -> entry.getAddress().length == 4)
              .filter(entry -> !entry.isLoopbackAddress())
              .filter(entry -> entry.getAddress()[0] != Integer.valueOf(10).byteValue())
              .map(InetAddress::getHostAddress))
          .findFirst().orElse(null);
    } catch (SocketException e) {
      return null;
    }
  }
}