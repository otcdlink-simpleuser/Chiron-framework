package com.otcdlink.chiron.fixture.tcp;

import com.google.common.base.Charsets;
import com.otcdlink.chiron.toolbox.internet.Hostname;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.ScheduledFuture;
import mockit.Injectable;
import org.junit.Test;

import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

public class TcpTransitTest {


  @Test
  public void wholeChain( @Injectable final ScheduledFuture< ? > scheduledFuture )
      throws Exception
  {
    final TcpTransitServer.Transfer.IngressEntry transferAtIngressEntry =
        new TcpTransitServer.Transfer.IngressEntry( ROUTE, TcpTransitServer.Edge.TARGET, true, asByteBuf( "Hello" ) ) ;
    assertThat( transferAtIngressEntry.forwardingRoute() ).isSameAs( ROUTE ) ;
    assertThat( transferAtIngressEntry.origin() ).isEqualTo( TcpTransitServer.Edge.TARGET ) ;
    assertThat( transferAtIngressEntry.payloadSize() ).isEqualTo( 5 ) ;
    assertThat( transferAtIngressEntry.payload() ).isEqualTo( new byte[] { 72, 101, 108, 108, 111 } ) ;
    assertThat( transferAtIngressEntry.delayMs() ).isNull() ;
    assertThat( transferAtIngressEntry.progress() ).isEqualTo( TcpTransitServer.Watcher.Progress.RECEIVED ) ;

    final TcpTransitServer.Transfer.IngressExit transferAtIngressExit =
        new TcpTransitServer.Transfer.IngressExit( ROUTE, TcpTransitServer.Edge.TARGET, true, asByteBuf( "H3llo" ) ) ;
    assertThat( transferAtIngressExit.forwardingRoute() ).isSameAs( ROUTE ) ;
    assertThat( transferAtIngressExit.origin() ).isEqualTo( TcpTransitServer.Edge.TARGET ) ;
    assertThat( transferAtIngressExit.payloadSize() ).isEqualTo( 5 ) ;
    assertThat( transferAtIngressExit.payload() ).isEqualTo( byteArray( "H3llo" ) ) ; // No change.
    assertThat( transferAtIngressExit.delayMs() ).isNull() ;

    final TcpTransitServer.Transfer.EgressExit transferAtEgressExit =
        new TcpTransitServer.Transfer.EgressExit( ROUTE, TcpTransitServer.Edge.TARGET, true, asByteBuf( "HELlo" ) ) ;
    assertThat( transferAtEgressExit.forwardingRoute() ).isSameAs( ROUTE ) ;
    assertThat( transferAtEgressExit.origin() ).isEqualTo( TcpTransitServer.Edge.TARGET ) ;
    assertThat( transferAtEgressExit.payloadSize() ).isEqualTo( 5 ) ;
    assertThat( transferAtEgressExit.payload() ).isEqualTo( byteArray( "HELlo" ) ) ;
    assertThat( transferAtEgressExit.delayMs() ).isNull() ;
    assertThat( transferAtEgressExit.progress() ).isEqualTo( TcpTransitServer.Watcher.Progress.EMITTED ) ;

    final TcpTransitServer.Transfer.Delayed transferDelayed =
        new TcpTransitServer.Transfer.Delayed<>( scheduledFuture, transferAtIngressExit, 33 ) ;
    assertThat( transferDelayed.forwardingRoute() ).isSameAs( ROUTE ) ;
    assertThat( transferDelayed.origin() ).isEqualTo( TcpTransitServer.Edge.TARGET ) ;
    assertThat( transferDelayed.payloadSize() ).isEqualTo( 5 ) ;
    assertThat( transferDelayed.payload() ).isEqualTo( byteArray( "H3llo" ) ) ;
    assertThat( transferDelayed.delayMs() ).isEqualTo( 33 ) ;
    assertThat( transferDelayed.progress() ).isEqualTo( TcpTransitServer.Watcher.Progress.DELAYED ) ;
    assertThat( transferDelayed.toString() ).contains( "33" ) ;


  }

// =======
// Fixture
// =======

  private static final Route ROUTE ;

  static {
    try {
      ROUTE = new Route( Hostname.LOCALHOST.hostPort( 1 ).asInetSocketAddress() );
    } catch( final UnknownHostException e ) {
      throw new RuntimeException( e ) ;
    }
  }

  private static ByteBuf asByteBuf( final String string ) {
    return Unpooled.copiedBuffer( byteArray( string ) );
  }

  private static byte[] byteArray( String string ) {
    return string.getBytes( Charsets.US_ASCII );
  }


}