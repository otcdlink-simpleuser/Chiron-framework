package io.github.otcdlink.chiron.toolbox.netty;

import java.net.InetSocketAddress;

public interface SocketServer extends InputOutputLifecycled {
  InetSocketAddress listenAddress() ;
}
