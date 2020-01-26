package com.xiaomi.xmpush.server;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerSwitch {
  private static final long REFRESH_SERVER_HOST_INTERVAL = 300000L;

  private static Server defaultServer = new Server(Constants.HOST_PRODUCTION, 1, 90, 10, 5);

  private static ServerSwitch INSTANCE = new ServerSwitch();

  private static Random random = new Random(System.currentTimeMillis());

  private Server[] servers;

  private Server feedback = new Server(Constants.HOST_PRODUCTION_FEEDBACK, 100, 100, 0, 0);

  private Server sandbox = new Server(Constants.HOST_SANDBOX, 100, 100, 0, 0);

  private Server specified = new Server(Constants.host, 100, 100, 0, 0);

  private Server emq = new Server(Constants.HOST_EMQ, 100, 100, 0, 0);

  private Server messageGlobal = new Server(Constants.HOST_GLOBAL_PRODUCTION, 100, 100, 0, 0);

  private Server feedbackGlobal =
      new Server(Constants.HOST_GLOBAL_PRODUCTION_FEEDBACK, 100, 100, 0, 0);

  private Server messageEurope = new Server(Constants.HOST_EUROPE_PRODUCTION, 100, 100, 0, 0);

  private Server messageVip = new Server(Constants.HOST_VIP, 100, 100, 0, 0);

  private Server feedbackEurope =
      new Server(Constants.HOST_EUROPE_PRODUCTION_FEEDBACK, 100, 100, 0, 0);

  private Server messageRussia = new Server(Constants.HOST_RUSSIA_PRODUCTION, 100, 100, 0, 0);

  private Server feedbackRussia =
      new Server(Constants.HOST_RUSSIA_PRODUCTION_FEEDBACK, 100, 100, 0, 0);

  private Server messageIndia = new Server(Constants.HOST_INDIA_PRODUCTION, 100, 100, 0, 0);

  private Server feedbackIndia =
      new Server(Constants.HOST_INDIA_PRODUCTION_FEEDBACK, 100, 100, 0, 0);

  private boolean inited = false;

  private long lastRefreshTime = System.currentTimeMillis();

  public static ServerSwitch getInstance() {
    return INSTANCE;
  }

  static String buildFullRequestURL(Server server, Constants.RequestPath requestPath) {
    return Constants.HTTP_PROTOCOL + "://" + server.getHost() + requestPath.getPath();
  }

  public boolean needRefreshHostList() {
    return (!this.inited || System.currentTimeMillis() - this.lastRefreshTime >= 300000L);
  }

  synchronized void initialize(String values) {
    if (!needRefreshHostList()) return;
    String[] vs = values.split(",");
    Server[] servers = new Server[vs.length];
    int i = 0;
    for (String s : vs) {
      String[] sp = s.split(":");
      if (sp.length < 5) {
        servers[i++] = defaultServer;
      } else {
        try {
          servers[i] =
              new Server(
                  sp[0],
                  Integer.valueOf(sp[1]).intValue(),
                  Integer.valueOf(sp[2]).intValue(),
                  Integer.valueOf(sp[3]).intValue(),
                  Integer.valueOf(sp[4]).intValue());
        } catch (Throwable e) {
          servers[i] = defaultServer;
        }
        if (this.servers != null)
          for (Server srv : this.servers) {
            if (srv.getHost().equals(servers[i].getHost()))
              (servers[i]).priority.set(srv.getPriority());
          }
        i++;
      }
    }
    this.inited = true;
    this.lastRefreshTime = System.currentTimeMillis();
    this.servers = servers;
  }

  Server selectServer(Constants.RequestPath requestPath, Region region, boolean isVip) {
    if (Constants.host != null) return this.specified.setHost(Constants.host);
    if (Constants.sandbox) return this.sandbox;
    switch (requestPath.getRequestType()) {
      case FEEDBACK:
        switch (region) {
          case Europe:
            return this.feedbackEurope;
          case Russia:
            return this.feedbackRussia;
          case India:
            return this.feedbackIndia;
          case Other:
            return this.feedbackGlobal;
        }
        return this.feedback;
      case EMQ:
        return this.emq;
    }
    if (isVip) return this.messageVip;
    switch (region) {
      case Europe:
        return this.feedbackEurope;
      case Russia:
        return this.feedbackRussia;
      case India:
        return this.feedbackIndia;
      case Other:
        return this.feedbackGlobal;
    }
    return selectServer();
  }

  private Server selectServer() {
    if (!Constants.autoSwitchHost || !this.inited) return defaultServer;
    int allPriority = 0;
    int[] priority = new int[this.servers.length];
    for (int i = 0; i < this.servers.length; i++) {
      priority[i] = this.servers[i].getPriority();
      allPriority += priority[i];
    }
    int randomPoint = random.nextInt(allPriority);
    int sum = 0;
    for (int j = 0; j < priority.length; j++) {
      sum += priority[j];
      if (randomPoint <= sum) return this.servers[j];
    }
    return defaultServer;
  }

  public static class Server {
    private String host;

    private AtomicInteger priority;

    private int minPriority;

    private int maxPriority;

    private int decrStep;

    private int incrStep;

    Server(String host, int minPriority, int maxPriority, int decrStep, int incrStep) {
      this.host = host;
      this.priority = new AtomicInteger(maxPriority);
      this.maxPriority = maxPriority;
      this.minPriority = minPriority;
      this.decrStep = decrStep;
      this.incrStep = incrStep;
    }

    String getHost() {
      return this.host;
    }

    Server setHost(String host) {
      this.host = host;
      return this;
    }

    int getPriority() {
      return this.priority.get();
    }

    void incrPriority() {
      changePriority(true, this.incrStep);
    }

    void decrPriority() {
      changePriority(false, this.decrStep);
    }

    private void changePriority(boolean incr, int step) {
      int old;
      int newValue;
      do {
        old = this.priority.get();
        newValue = incr ? (old + step) : (old - step);
        if (newValue < this.minPriority) newValue = this.minPriority;
        if (newValue <= this.maxPriority) continue;
        newValue = this.maxPriority;
      } while (!this.priority.compareAndSet(old, newValue));
    }

    public String toString() {
      return this.host
          + ":<"
          + this.minPriority
          + ","
          + this.maxPriority
          + ">+"
          + this.incrStep
          + "-"
          + this.decrStep
          + ":"
          + this.priority;
    }
  }
}
