package io.github.otcdlink.chiron.integration.twoend;

import io.github.otcdlink.chiron.downend.DownendConnector;
import io.github.otcdlink.chiron.downend.DownendConnectorTest;
import io.github.otcdlink.chiron.downend.SignonMaterializer;
import io.github.otcdlink.chiron.fixture.BlockingMonolist;
import io.github.otcdlink.chiron.fixture.tcp.http.ConnectProxy;
import io.github.otcdlink.chiron.toolbox.netty.NettyTools;
import io.github.otcdlink.chiron.upend.session.OutwardSessionSupervisor;
import io.github.otcdlink.chiron.upend.session.SessionSupervisor;
import io.netty.channel.Channel;
import mockit.Injectable;
import mockit.StrictExpectations;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

import static io.github.otcdlink.chiron.AbstractConnectorFixture.SESSION_IDENTIFIER;

/**
 * More tests, using {@link ConnectProxy}.
 */
@SuppressWarnings( "TestMethodWithIncorrectSignature" )
public class EndToEndPongTest {


  @Test( timeout = TIMEOUT_MS )
  public void simpleReconnect(
      @Injectable final SignonMaterializer signonMaterializer,
      @Injectable final OutwardSessionSupervisor< Channel, InetAddress >
          outboundSessionSupervisor
  ) throws Exception {
    EndToEndTestFragments.authenticate(
        fixture, true, signonMaterializer, outboundSessionSupervisor ) ;

    final BlockingMonolist<SessionSupervisor.ReuseCallback> reuseCallbackCapture =
        new BlockingMonolist<>() ;
    new StrictExpectations() {{
      outboundSessionSupervisor.closed( ( Channel ) any, SESSION_IDENTIFIER, false );
      outboundSessionSupervisor.tryReuse(
          SESSION_IDENTIFIER,
          ( Channel ) any,
          withCapture( reuseCallbackCapture )
      ) ;
    }} ;

    fixture.restartHttpProxy() ;


    final SessionSupervisor.ReuseCallback reuseCallback = reuseCallbackCapture.getOrWait() ;
    LOGGER.info( "Got " + reuseCallback ) ;
    reuseCallback.reuseOutcome( null ) ;

    while( true ) {
      final DownendConnector.Change change = fixture.nextDownendChange() ;
      LOGGER.debug( "Obtained " + change + " while actively waiting for " +
          DownendConnector.State.SIGNED_IN + " ..." ) ;
      if( change.kind == DownendConnector.State.SIGNED_IN ) {
        break ;
      }
    }
    fixture.commandRoundtrip( SESSION_IDENTIFIER ) ;
    EndToEndTestFragments.terminate( fixture ) ;
  }


// =======
// Fixture
// =======

  @SuppressWarnings( "unused" )
  private static final Logger LOGGER =
      LoggerFactory.getLogger( DownendConnectorTest.class ) ;

  static {
    NettyTools.forceNettyClassesToLoad() ;
    LOGGER.debug( "====== Ready to run tests ======" ) ;
  }


      private static final long TIMEOUT_MS = 5_000 ;
//  private static final long TIMEOUT_MS = 1_000_000 ;

  private final EndToEndFixture fixture = new EndToEndFixture() ;

  @After
  public void tearDown() throws Exception {
    fixture.stopAll() ;
  }
}
