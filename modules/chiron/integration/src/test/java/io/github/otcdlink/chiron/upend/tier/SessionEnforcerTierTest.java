package io.github.otcdlink.chiron.upend.tier;

import io.github.otcdlink.chiron.fixture.BlockingMonolist;
import io.github.otcdlink.chiron.fixture.Monolist;
import io.github.otcdlink.chiron.integration.ReactiveSessionFixture;
import io.github.otcdlink.chiron.middle.ChannelTools;
import io.github.otcdlink.chiron.middle.session.SessionLifecycle;
import io.github.otcdlink.chiron.upend.UpendConnector;
import io.github.otcdlink.chiron.upend.session.OutwardSessionSupervisor;
import io.github.otcdlink.chiron.upend.session.SessionSupervisor;
import io.github.otcdlink.chiron.upend.session.SessionSupervisor.PrimarySignonAttemptCallback;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import mockit.FullVerificationsInOrder;
import mockit.Injectable;
import mockit.StrictExpectations;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

import static io.github.otcdlink.chiron.integration.ReactiveSessionFixture.SECONDARY_CODE_1;
import static io.github.otcdlink.chiron.integration.ReactiveSessionFixture.SECONDARY_TOKEN_1;
import static io.github.otcdlink.chiron.integration.ReactiveSessionFixture.SESSION_1;
import static io.github.otcdlink.chiron.integration.ReactiveSessionFixture.USER_X;
import static io.github.otcdlink.chiron.integration.ReactiveSessionFixture.USER_X_PRIMARY_SIGNON_GOOD_WITH_MODIFY_CHANNEL_ROLE;
import static io.github.otcdlink.chiron.integration.ReactiveSessionFixture.USER_X_RESIGNON_GOOD_WITH_MODIFY_CHANNEL_ROLE;
import static io.github.otcdlink.chiron.integration.ReactiveSessionFixture.USER_X_SECONDARY_SIGNON_GOOD_WITH_MODIFY_CHANNEL_ROLE;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings( "TestMethodWithIncorrectSignature" )
public class SessionEnforcerTierTest {

  @Test
  public void simpleSignonAndOff(
      @Injectable final OutwardSessionSupervisor< Channel, SocketAddress > sessionSupervisor,
      @Injectable final UpendConnector.ChannelRegistrar channelRegistrar
  ) throws Exception {
    final EmbeddedChannel embeddedChannel = createChannel( sessionSupervisor,channelRegistrar ) ;

    successfulPrimarySignon( embeddedChannel, channelRegistrar, sessionSupervisor ) ;

    signoff( embeddedChannel, sessionSupervisor, channelRegistrar ) ;

  }

  @Test
  public void resignon(
      @Injectable final OutwardSessionSupervisor< Channel, SocketAddress > sessionSupervisor,
      @Injectable final UpendConnector.ChannelRegistrar channelRegistrar
  ) throws Exception {
    final EmbeddedChannel embeddedChannelOld = createChannel( sessionSupervisor,channelRegistrar ) ;
    successfulPrimarySignon( embeddedChannelOld, channelRegistrar, sessionSupervisor ) ;
    embeddedChannelOld.close() ;

    final EmbeddedChannel embeddedChannelNew = createChannel( sessionSupervisor,channelRegistrar ) ;

    final BlockingMonolist<SessionSupervisor.ReuseCallback> reuseCallbackCaptor =
        new BlockingMonolist<>() ;

    new StrictExpectations() {{
      sessionSupervisor.tryReuse(
          SESSION_1,
          embeddedChannelNew,
          withCapture( reuseCallbackCaptor )
      ) ;
      channelRegistrar.registerChannel( SESSION_1, ( Channel ) any ) ;
    }} ;

    embeddedChannelNew.writeInbound( USER_X_RESIGNON_GOOD_WITH_MODIFY_CHANNEL_ROLE ) ;
    reuseCallbackCaptor.getOrWait().reuseOutcome( null ) ;

    assertThat( ( ( SessionLifecycle.Phase ) embeddedChannelNew.readOutbound() ) )
        .isEqualTo( SessionLifecycle.SessionValid.create( SESSION_1 ) ) ;

    LOGGER.info( HANDLER_CLASSNAME + " wrote outbound that " +
        SESSION_1 + " is a valid session." ) ;

  }

  @Test
  public void secondarySignonAndOff(
      @Injectable final OutwardSessionSupervisor< Channel, SocketAddress > sessionSupervisor,
      @Injectable final UpendConnector.ChannelRegistrar channelRegistrar
  ) throws Exception {
    final EmbeddedChannel embeddedChannel = createChannel( sessionSupervisor, channelRegistrar ) ;

    final PrimarySignonAttemptCallback primarySignonAttemptCallback =
        attemptPrimarySignon( embeddedChannel, sessionSupervisor ) ;

    primarySignonAttemptCallback.needSecondarySignon( USER_X, SECONDARY_TOKEN_1 ) ;

    assertThat( ( ( SessionLifecycle.Phase ) embeddedChannel.readOutbound() ) )
        .isEqualTo( SessionLifecycle.SecondarySignonNeeded.create( SECONDARY_TOKEN_1 ) ) ;

    LOGGER.info( HANDLER_CLASSNAME + " now expecting " +
        SECONDARY_TOKEN_1 + "-" + SECONDARY_CODE_1 + " pair." ) ;


    final Monolist< SessionSupervisor.SecondarySignonAttemptCallback >
        secondarySignonAttemptCallbackCapture = new Monolist<>() ;

    new StrictExpectations() {{
      sessionSupervisor.attemptSecondarySignon(
          embeddedChannel,
          withInstanceOf( SocketAddress.class ),
          SECONDARY_TOKEN_1,
          SECONDARY_CODE_1,
          withCapture( secondarySignonAttemptCallbackCapture )
      ) ;
    }} ;
    embeddedChannel.writeInbound( USER_X_SECONDARY_SIGNON_GOOD_WITH_MODIFY_CHANNEL_ROLE ) ;

    new FullVerificationsInOrder() {{ }} ;

    LOGGER.info( HANDLER_CLASSNAME + "successfully read " +
        USER_X_SECONDARY_SIGNON_GOOD_WITH_MODIFY_CHANNEL_ROLE + "." ) ;


    secondarySignonAttemptCallbackCapture.get().sessionAttributed( SESSION_1 ) ;

    assertThat( ( ( SessionLifecycle.Phase ) embeddedChannel.readOutbound() ) )
        .isEqualTo( SessionLifecycle.SessionValid.create( SESSION_1 ) ) ;

    LOGGER.info( HANDLER_CLASSNAME + " wrote outbound that " +
        SESSION_1 + " is a valid session." ) ;


    signoff( embeddedChannel, sessionSupervisor, channelRegistrar ) ;

  }

  @Test
  public void connectionLossCausesNotification(
      @Injectable final OutwardSessionSupervisor< Channel, SocketAddress > sessionSupervisor,
      @Injectable final UpendConnector.ChannelRegistrar channelRegistrar
  ) throws Exception {

    final EmbeddedChannel embeddedChannel = createChannel( sessionSupervisor, channelRegistrar ) ;
    successfulPrimarySignon( embeddedChannel, channelRegistrar, sessionSupervisor ) ;

    new StrictExpectations() {{
      sessionSupervisor.closed( embeddedChannel, SESSION_1, false ) ;
      channelRegistrar.unregisterChannel( embeddedChannel ); ;
    }} ;
    embeddedChannel.close() ;

    new FullVerificationsInOrder() {{ }} ;

    LOGGER.info( HANDLER_CLASSNAME + " notified of " + CHANNEL_CLASSNAME + " closing." ) ;
  }



// ==================
// Factored test code
// ==================

  private static PrimarySignonAttemptCallback attemptPrimarySignon(
      final EmbeddedChannel embeddedChannel,
      final OutwardSessionSupervisor< Channel, SocketAddress > sessionSupervisor
  ) {
    final Monolist< PrimarySignonAttemptCallback >
        primarySignonAttemptCallbackCapture = new Monolist<>() ;

    new StrictExpectations() {{
      sessionSupervisor.attemptPrimarySignon(
          USER_X_PRIMARY_SIGNON_GOOD_WITH_MODIFY_CHANNEL_ROLE.login(),
          USER_X_PRIMARY_SIGNON_GOOD_WITH_MODIFY_CHANNEL_ROLE.password(),
          embeddedChannel,
          withInstanceOf( SocketAddress.class ),
          withCapture( primarySignonAttemptCallbackCapture )
      ) ;
    }} ;
    embeddedChannel.writeInbound( USER_X_PRIMARY_SIGNON_GOOD_WITH_MODIFY_CHANNEL_ROLE ) ;

    new FullVerificationsInOrder() {{ }} ;

    LOGGER.info( HANDLER_CLASSNAME + "successfully read " +
        USER_X_PRIMARY_SIGNON_GOOD_WITH_MODIFY_CHANNEL_ROLE + "." ) ;

    return primarySignonAttemptCallbackCapture.get() ;
  }

  private static void successfulPrimarySignon(
      final EmbeddedChannel embeddedChannel,
      final UpendConnector.ChannelRegistrar channelRegistrar,
      final OutwardSessionSupervisor< Channel, SocketAddress > sessionSupervisor
  ) {
    final PrimarySignonAttemptCallback primarySignonAttemptCallback =
        attemptPrimarySignon( embeddedChannel, sessionSupervisor ) ;

    new StrictExpectations() {{
      channelRegistrar.registerChannel( SESSION_1, ( Channel ) any ) ;
    }} ;

    primarySignonAttemptCallback.sessionAttributed(
        ReactiveSessionFixture.SESSION_1 ) ;

    /** Since we mocked {@link UpendConnector.ChannelRegistrar} we must set the
     * attribute by hand. */
    embeddedChannel.attr( ChannelTools.SESSION_KEY ).set( SESSION_1 ) ;

    final SessionLifecycle.SessionValid sessionValid = embeddedChannel.readOutbound() ;
    assertThat( sessionValid.sessionIdentifier() ).isEqualTo( SESSION_1 ) ;

    new FullVerificationsInOrder() {{ }} ;

    LOGGER.info( HANDLER_CLASSNAME + " successfully wrote " + sessionValid + ".") ;

  }

  private static void signoff(
      final EmbeddedChannel embeddedChannel,
      final OutwardSessionSupervisor< Channel, SocketAddress > sessionSupervisor,
      final UpendConnector.ChannelRegistrar channelRegistrar
  ) {
    new StrictExpectations() {{
      channelRegistrar.unregisterChannel( embeddedChannel ) ; result = SESSION_1;
      sessionSupervisor.closed( embeddedChannel, SESSION_1, true ) ;
    }} ;
    final SessionLifecycle.Signoff signoff = SessionLifecycle.Signoff.create() ;
    embeddedChannel.writeInbound( signoff ) ;

    new FullVerificationsInOrder() {{ }} ;

    LOGGER.info( HANDLER_CLASSNAME + " successfully read " + signoff + "." ) ;

    UpendChannelTools.closeChannelQuietly( embeddedChannel ) ;

    new FullVerificationsInOrder() {{ }} ;

    LOGGER.info( "Closing the " + Channel.class.getSimpleName() +
        " did not trigger further notifications." ) ;
  }



// =======
// Fixture
// =======

  private static final Logger LOGGER =
      LoggerFactory.getLogger( SessionEnforcerTierTest.class ) ;

  private static final String HANDLER_CLASSNAME = SessionEnforcerTier.class.getSimpleName() ;
  private static final String CHANNEL_CLASSNAME = Channel.class.getSimpleName() ;

  private static EmbeddedChannel createChannel(
      final OutwardSessionSupervisor< Channel, SocketAddress > sessionSupervisor,
      final UpendConnector.ChannelRegistrar channelRegistrar
  ) {
    return new EmbeddedChannel(
        new SessionEnforcerTier<>(
            sessionSupervisor,
            channelRegistrar,
            Channel::remoteAddress
        )
    ) ;
  }

}