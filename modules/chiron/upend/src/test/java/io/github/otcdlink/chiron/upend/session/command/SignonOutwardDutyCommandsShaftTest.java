package io.github.otcdlink.chiron.upend.session.command;

import io.github.otcdlink.chiron.command.Stamp;
import io.github.otcdlink.chiron.designator.Designator;
import io.github.otcdlink.chiron.designator.DesignatorForger;
import io.github.otcdlink.chiron.middle.PhoneNumber;
import io.github.otcdlink.chiron.middle.session.SessionIdentifier;
import io.github.otcdlink.chiron.middle.session.SignonDecision;
import io.github.otcdlink.chiron.middle.session.SignonFailure;
import io.github.otcdlink.chiron.middle.session.SignonFailureNotice;
import io.github.otcdlink.chiron.middle.shaft.CrafterShaft;
import io.github.otcdlink.chiron.middle.shaft.MethodCallVerifier;
import io.github.otcdlink.chiron.middle.shaft.MethodCaller;
import io.github.otcdlink.chiron.upend.session.SignableUser;
import io.github.otcdlink.chiron.upend.session.SignonOutwardDuty;
import org.junit.Test;

public class SignonOutwardDutyCommandsShaftTest {

  @Test
  public void transientMethodsWithCrafterShaft() throws Exception {
    new CrafterShaft<>( SignonOutwardDutyCrafter::new, DESIGNATOR )
        .submit( TRANSIENT_METHODS_CALLER, VERIFIER ) ;
  }


// ==============
// Method callers
// ==============

  private static final MethodCaller< SignonOutwardDuty > TRANSIENT_METHODS_CALLER =
      new MethodCaller.Default< SignonOutwardDuty >() {
        @Override
        public void callMethods( final SignonOutwardDuty signonOutwardDuty ) {
          signonOutwardDuty.primarySignonAttempted(
              DESIGNATOR,
              new SignonDecision<>( SIGNABLE_USER )
          ) ;
          signonOutwardDuty.secondarySignonAttempted(
              DESIGNATOR,
              new SignonFailureNotice( SignonFailure.SESSION_ALREADY_EXISTS )
          ) ;
          signonOutwardDuty.sessionCreated(
              DESIGNATOR,
              new SessionIdentifier( "535510n" ),
              "TheLogin"
          ) ;
          signonOutwardDuty.terminateSession(
              DESIGNATOR,
              new SessionIdentifier( "535510n" )
          ) ;
        }
      }
  ;

// =====
// Other
// =====

  /**
   * Skips the first parameter, which is a {@link Designator} on Upend and something else
   * on Downend.
   */
  private static final MethodCallVerifier VERIFIER = new MethodCallVerifier.Skipping( 0 ) ;

  private static final Designator DESIGNATOR = DesignatorForger
      .newForger().instant( Stamp.FLOOR ).internal() ;


  private static final SignableUser SIGNABLE_USER = new SignableUser() {
    @Override
    public String login() {
      return "TheLogin";
    }

    @Override
    public PhoneNumber phoneNumber() {
      return new PhoneNumber( "+36987654323" ) ;
    }
  } ;


}