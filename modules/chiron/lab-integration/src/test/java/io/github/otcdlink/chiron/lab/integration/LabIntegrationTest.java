package io.github.otcdlink.chiron.lab.integration;

import io.github.otcdlink.chiron.ConnectorChangeAssert;
import io.github.otcdlink.chiron.downend.DownendConnector;
import io.github.otcdlink.chiron.downend.DownendStateTools;
import io.github.otcdlink.chiron.downend.SignonMaterializer;
import io.github.otcdlink.chiron.downend.Tracker;
import io.github.otcdlink.chiron.fixture.BlockingMonolist;
import io.github.otcdlink.chiron.lab.downend.LabDownend;
import io.github.otcdlink.chiron.lab.middle.LabDownwardDuty;
import io.github.otcdlink.chiron.lab.upend.LabDaemon;
import io.github.otcdlink.chiron.lab.upend.LabUpendLogic;
import io.github.otcdlink.chiron.middle.CommandFailureNotice;
import io.github.otcdlink.chiron.middle.session.SecondaryCode;
import io.github.otcdlink.chiron.middle.session.SecondaryToken;
import io.github.otcdlink.chiron.middle.session.SignonFailure;
import io.github.otcdlink.chiron.middle.session.SignonFailureNotice;
import io.github.otcdlink.chiron.toolbox.Credential;
import io.github.otcdlink.chiron.toolbox.ObjectTools;
import io.github.otcdlink.chiron.toolbox.TcpPortBooker;
import io.github.otcdlink.chiron.toolbox.internet.HostPort;
import io.github.otcdlink.chiron.toolbox.internet.LocalAddressTools;
import io.github.otcdlink.chiron.upend.session.SecondaryAuthenticator;
import mockit.FullVerificationsInOrder;
import mockit.Injectable;
import mockit.StrictExpectations;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import static io.github.otcdlink.chiron.downend.DownendConnector.State.CONNECTED;
import static io.github.otcdlink.chiron.downend.DownendConnector.State.CONNECTING;
import static io.github.otcdlink.chiron.downend.DownendConnector.State.SIGNED_IN;
import static io.github.otcdlink.chiron.downend.DownendConnector.State.STOPPED;
import static io.github.otcdlink.chiron.downend.DownendConnector.State.STOPPING;
import static org.assertj.core.api.Assertions.assertThat;

public class LabIntegrationTest {

  @SuppressWarnings( "TestMethodWithIncorrectSignature" )
  @Test
  public void primarySignonOkAndNiceFailure(
      @Injectable final SignonMaterializer signonMaterializer,
      @Injectable final LabDownwardDuty< Tracker > labDownwardDuty
  ) throws Exception {
    final LabDaemon labDaemon = new LabDaemon( hostPort, null ) ;
    labDaemon.start() ;

    final QueuingChangeWatcher changeWatcher ;
    final LabDownend labDownend ;
    {
      final ObjectTools.Holder< LabDownend > labDownendHolder = ObjectTools.newHolder() ;
      changeWatcher = new QueuingChangeWatcher( LOGGER, labDownendHolder ) ;

      labDownend = new LabDownend(
          hostPort,
          signonMaterializer,
          changeWatcher,
          labDownwardDuty
      ) ;
      labDownendHolder.set( labDownend ) ;
    }

    final BlockingMonolist< Consumer< Credential > > credentialConsumerCapture =
        new BlockingMonolist<>() ;

    new StrictExpectations() {{
      signonMaterializer.readCredential( withCapture( credentialConsumerCapture ) ) ;
      signonMaterializer.setProgressMessage( "Signing in …" ) ;
      signonMaterializer.setProgressMessage( null ) ;
      signonMaterializer.done() ;
    }} ;

    labDownend.start() ;
    credentialConsumerCapture.getOrWait().accept( CREDENTIAL_ALICE ) ;

    assertStateChanges( changeWatcher, CONNECTING, CONNECTED, SIGNED_IN ) ;
    LOGGER.info( "Started and signed into " + labDownend + "." ) ;

    labDownend.upwardDuty().increment( blockingTracker, 0 ) ;
    final CommandFailureNotice commandFailureNotice = blockingTracker.waitForRemoteFailure() ;
    assertThat( commandFailureNotice.message ).isEqualTo( "Bad delta" ) ;

    new FullVerificationsInOrder() {{ }} ;

    LOGGER.info( "Obtained expected failure and nothing else." ) ;

    labDownend.stop() ;
    labDaemon.stop() ;
  }
  @SuppressWarnings( "TestMethodWithIncorrectSignature" )
  @Test
  public void lastLoginInNotification(
      @Injectable final SignonMaterializer signonMaterializer,
      @Injectable final LabDownwardDuty< Tracker > labDownwardDuty
  ) throws Exception {
    final LabDaemon labDaemon = new LabDaemon( hostPort, null ) ;
    labDaemon.start() ;

    final QueuingChangeWatcher changeWatcher ;
    final LabDownend labDownend ;
    {
      final ObjectTools.Holder< LabDownend > labDownendHolder = ObjectTools.newHolder() ;
      changeWatcher = new QueuingChangeWatcher( LOGGER, labDownendHolder ) ;

      labDownend = new LabDownend(
          hostPort,
          signonMaterializer,
          changeWatcher,
          labDownwardDuty
      ) ;
      labDownendHolder.set( labDownend ) ;
    }

    final BlockingMonolist< Consumer< Credential > > credentialConsumerCapture =
        new BlockingMonolist<>() ;

    new StrictExpectations() {{
      signonMaterializer.readCredential( withCapture( credentialConsumerCapture ) ) ;
      signonMaterializer.setProgressMessage( "Signing in …" ) ;
      signonMaterializer.setProgressMessage( null ) ;
      signonMaterializer.done() ;
    }} ;

    labDownend.start() ;
    credentialConsumerCapture.getOrWait().accept( CREDENTIAL_ALICE ) ;

    assertStateChanges( changeWatcher, CONNECTING, CONNECTED ) ;
    LOGGER.info( "Connected, now verifying last ." ) ;

    final DownendConnector.Change.SuccessfulSignon change =
        ( DownendConnector.Change.SuccessfulSignon ) changeWatcher.nextChange() ;
    final URL semanticUrl = change.semanticConnectionUrl() ;

    LOGGER.info( "Signon happened, got '" + semanticUrl + "'." ) ;

    final URL setupUrl = labDownend.setup().url ;
    assertThat( semanticUrl )
        .hasProtocol( "ws" )
        .hasAuthority( CREDENTIAL_ALICE.getLogin() + "@" +
            setupUrl.getHost() + ":" + setupUrl.getPort() )
    ;


    labDownend.stop() ;
    labDaemon.stop() ;
  }


  @SuppressWarnings( "TestMethodWithIncorrectSignature" )
  @Test
  public void secondarySignonOk(
      @Injectable final SignonMaterializer signonMaterializer,
      @Injectable final LabDownwardDuty< Tracker > labDownwardDuty,
      @Injectable final SecondaryAuthenticator secondaryAuthenticator
  ) throws Exception {
    final LabDaemon labDaemon = new LabDaemon( hostPort, secondaryAuthenticator ) ;
    labDaemon.start() ;

    final QueuingChangeWatcher changeWatcher ;
    final LabDownend labDownend ;
    {
      final ObjectTools.Holder< LabDownend > labDownendHolder = ObjectTools.newHolder() ;
      changeWatcher = new QueuingChangeWatcher( LOGGER, labDownendHolder ) ;

      labDownend = new LabDownend(
          hostPort,
          signonMaterializer,
          changeWatcher,
          labDownwardDuty
      ) ;
      labDownendHolder.set( labDownend ) ;
    }

    final BlockingMonolist< Consumer< Credential > > credentialConsumerCapture =
        new BlockingMonolist<>() ;

    final BlockingMonolist< SecondaryAuthenticator.SecondaryTokenCallback >
        secondaryTokenCallbackCapture = new BlockingMonolist<>() ;

    final BlockingMonolist< Consumer< SecondaryCode > > secondaryCodeConsumerCapture =
        new BlockingMonolist<>() ;

    final BlockingMonolist< SecondaryAuthenticator.VerificationCallback >
        secondaryCodeVerificationCapture = new BlockingMonolist<>() ;


    new StrictExpectations() {{
      signonMaterializer.readCredential( withCapture( credentialConsumerCapture ) ) ;
      signonMaterializer.setProgressMessage( "Signing in …" ) ;
      signonMaterializer.setProgressMessage( null ) ;
      secondaryAuthenticator.requestAuthentication(
          USER_ALICE.phoneNumber(), withCapture( secondaryTokenCallbackCapture ) ) ;
      signonMaterializer.setProblemMessage(
          new SignonFailureNotice( SignonFailure.MISSING_SECONDARY_CODE ) ) ; ;
      signonMaterializer.readSecondaryCode( withCapture( secondaryCodeConsumerCapture ) ) ;
      signonMaterializer.setProgressMessage( "Signing in …" ) ;
      secondaryAuthenticator.verifySecondaryCode(
          SECONDARY_TOKEN, SECONDARY_CODE, withCapture( secondaryCodeVerificationCapture ) ) ;
      signonMaterializer.done() ;
    }} ;

    labDownend.start() ;
    credentialConsumerCapture.getOrWait().accept( CREDENTIAL_ALICE ) ;
    secondaryTokenCallbackCapture.getOrWait().secondaryToken( SECONDARY_TOKEN ) ;
    secondaryCodeConsumerCapture.getOrWait().accept( SECONDARY_CODE ) ;
    secondaryCodeVerificationCapture.getOrWait().secondaryAuthenticationResult( null ) ;

    assertStateChanges( changeWatcher, CONNECTING, CONNECTED, SIGNED_IN ) ;
    LOGGER.info( "Started and signed into " + labDownend + "." ) ;

    labDownend.upwardDuty().increment( blockingTracker, 1 ) ;
    blockingTracker.waitForResponse() ;

    new FullVerificationsInOrder() {{
      labDownwardDuty.counter( ( Tracker ) any, 1 ) ;
    }} ;

    LOGGER.info( "Obtained expected response." ) ;

    labDownend.stop() ;
    assertStateChanges( changeWatcher, STOPPING, STOPPED ) ;
    LOGGER.info( "Successfully stopped " + labDownend + "." ) ;

    labDaemon.stop() ;
    LOGGER.info( "Successfully stopped " + labDaemon + "." ) ;
  }



// =======
// Fixture
// =======

  private static final Logger LOGGER = LoggerFactory.getLogger( LabIntegrationTest.class ) ;

  private final HostPort hostPort = LocalAddressTools.LOCALHOST_HOSTNAME
      .hostPort( TcpPortBooker.THIS.find() ) ;

  private final LabUpendLogic.LabUserKey USER_ALICE = LabUpendLogic.USERS.iterator().next() ;

  private final Credential CREDENTIAL_ALICE =
      new Credential( USER_ALICE.login(), USER_ALICE.login() ) ;

  private final SecondaryToken SECONDARY_TOKEN = new SecondaryToken( "53c0nd4rY_T0k3n" ) ;
  private final SecondaryCode SECONDARY_CODE = new SecondaryCode( "53c0nd4rY_c0d3" ) ;

  public LabIntegrationTest() throws UnknownHostException { }

  private final BlockingTracker blockingTracker = new BlockingTracker() ;

  private static final class BlockingTracker extends Tracker.Adapter {

    private final Semaphore responseSemaphore = new Semaphore( 0 ) ;

    private final BlockingQueue< CommandFailureNotice > failureQueue =
        new ArrayBlockingQueue<>( 1 ) ;

    @Override
    public void afterResponseHandled() {
      responseSemaphore.release() ;
    }

    public void waitForResponse() throws InterruptedException {
      responseSemaphore.acquire() ;
    }

    @Override
    public void afterRemoteFailure( final CommandFailureNotice commandFailureNotice ) {
      failureQueue.add( commandFailureNotice ) ;
    }

    public CommandFailureNotice waitForRemoteFailure() throws InterruptedException {
      return failureQueue.take() ;
    }
  }

  private static class QueuingChangeWatcher extends DownendStateTools.StateWatcherLoggingAdapter {

    private final BlockingQueue< DownendConnector.Change > blockingQueue =
        new ArrayBlockingQueue<>( 10 ) ;

    public QueuingChangeWatcher(
        final Logger logger,
        final ObjectTools.Holder< ? > tostringProviderHolder
    ) {
      super( logger, () -> tostringProviderHolder.get().toString() ) ;
    }

    @Override
    protected void doStateChanged( final DownendConnector.Change change ) {
      blockingQueue.add( change ) ;
    }

    public DownendConnector.Change nextChange() throws InterruptedException {
      return blockingQueue.take() ;
    }
  }

  private static void assertStateChanges(
      final QueuingChangeWatcher changeWatcher,
      final DownendConnector.State... kinds
  ) throws InterruptedException {
    for( final DownendConnector.State kind : kinds ) {
      ConnectorChangeAssert.assertThat( changeWatcher.nextChange() ).hasKind( kind ) ;
    }
  }

}
