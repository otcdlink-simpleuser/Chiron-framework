package com.otcdlink.chiron.middle.tier;

import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableMap;
import com.otcdlink.chiron.toolbox.ToStringTools;
import com.otcdlink.chiron.toolbox.number.PositiveIntegerRange;

import javax.annotation.Nonnull;
import java.util.Random;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.otcdlink.chiron.middle.tier.TimeBoundary.ForAll.Key.PING_TIMEOUT_MS;

public interface TimeBoundary {

  /**
   * Timeout used by {@code DownendConnector} for initial connection, at the time
   * {@link ConnectionDescriptor} is not available.
   */
  int DEFAULT_CONNECT_TIMEOUT_MS = 1000 ;

  /**
   * Supports a lag of 500 ms (upstream <i>and</i> downstream).
   */
  @SuppressWarnings( "unused" )
  ForAll LENIENT_500 = Builder.createNew()
      .pingInterval( 1000 )
      .pongTimeoutOnDownend( 2500 )  // Need this to support a lag of 500 ms induced by HttpProxy.
      .reconnectDelay( DEFAULT_CONNECT_TIMEOUT_MS, 3000 )
      .pingTimeoutOnUpend( 2500 )    // Need this to support a lag of 500 ms induced by HttpProxy.
      .maximumSessionInactivity( 10_000 )
      .build()
  ;

  @SuppressWarnings( "unused" )
  ForAll NOPE = Builder.createNew()
      .pingIntervalNever()
      .pongTimeoutNever()
      .reconnectDelay( 1000, 1000 )
      .pingTimeoutNever()
      .maximumSessionInactivity( 10_000 )
      .build()
  ;
  /**
   * Sensible defaults for production use.
   */
  ForAll DEFAULT = LENIENT_500 ;

  /**
   * Describes the timeout-related value needed by {@code DownendConnector} before it can obtain a
   * {@link ForDownend}.
   */
  interface PrimingForDownend {

    int connectTimeoutMs() ;

    /**
     * Needed for creation of {@code TrackerCurator}.
     */
    int pongTimeoutMs() ;

    static PrimingForDownend createNew( int connectTimeoutMs, int pongTimeoutMs ) {
      checkArgument( connectTimeoutMs > 0 ) ;
      checkArgument( pongTimeoutMs > 0 ) ;
      return new PrimingForDownend( ) {
        @Override
        public int connectTimeoutMs() {
          return connectTimeoutMs ;
        }

        @Override
        public int pongTimeoutMs() {
          return pongTimeoutMs ;
        }

        @Override
        public String toString() {
          return PrimingForDownend.toString( this ) ;
        }
      } ;
    }

    static String toString( final PrimingForDownend primingTimeBoundary ) {
      return ToStringTools.getNiceClassName( primingTimeBoundary ) + "{" +
          "connectTimeoutMs=" + primingTimeBoundary.connectTimeoutMs() + ";" +
          "pongTimeoutMs=" + primingTimeBoundary.pongTimeoutMs() +
          "}"
      ;
    }

  }

  /**
   * Describes what Downend should see/use from a {@link ForAll}.
   */
  interface ForDownend extends PrimingForDownend {

    int pingIntervalMs() ;

    int reconnectDelayMs( final Random random ) ;


    static String toString( final ForDownend downendInitialTimeBoundary ) {
      return ToStringTools.getNiceClassName( downendInitialTimeBoundary ) + "{" +
          "connectTimeoutMs=" + downendInitialTimeBoundary.connectTimeoutMs() + ";" +
          "pingIntervalMs=" + downendInitialTimeBoundary.pingIntervalMs() + ";" +
          "pongTimeoutMs=" + downendInitialTimeBoundary.pongTimeoutMs() +
          "}"
      ;
    }

  }

  interface Builder {
    static PingIntervalStep createNew() {
      return new StepCombinator() ;
    }

    interface PingIntervalStep {
      PongTimeoutStep pingInterval( int ms ) ;
      default PongTimeoutStep pingIntervalNever() {
        return pingInterval( Integer.MAX_VALUE ) ;
      }
    }

    interface PongTimeoutStep {
      ReconnectDelayStep pongTimeoutOnDownend( int ms ) ;
      default ReconnectDelayStep pongTimeoutNever() {
        return pongTimeoutOnDownend( Integer.MAX_VALUE ) ;
      }
    }

    interface ReconnectDelayStep {
      default PingTimeoutStep reconnectImmediately() {
        return reconnectDelay( 0, 0 ) ;
      }
      default PingTimeoutStep reconnectNever() {
        return reconnectDelay( Integer.MAX_VALUE - 1, Integer.MAX_VALUE ) ;
      }
      default PingTimeoutStep reconnectDelayOrDouble( final int delayMs ) {
        return reconnectDelay( delayMs, delayMs * 2 ) ;
      }
      PingTimeoutStep reconnectDelay( int delayLowerBoundMs, final int delayHigherBoundMs ) ;
    }

    interface PingTimeoutStep {
      SessionInactivityStep pingTimeoutOnUpend( int ms ) ;
      default SessionInactivityStep pingTimeoutNever() {
        return pingTimeoutOnUpend( Integer.MAX_VALUE ) ;
      }
    }

    interface SessionInactivityStep {
      BuilderStep maximumSessionInactivity( int ms ) ;
      default BuilderStep sessionInactivityImmediate() {
        return maximumSessionInactivity( 0 ) ;
      }
      default BuilderStep sessionInactivityForever() {
        return maximumSessionInactivity( Integer.MAX_VALUE ) ;
      }
    }

    interface BuilderStep {
      ForAll build() ;
    }

    class StepCombinator
        implements
        PingIntervalStep,
        PongTimeoutStep,
        ReconnectDelayStep,
        PingTimeoutStep,
        SessionInactivityStep,
        BuilderStep
    {
      private static Integer checkPositive( final int i ) {
        checkArgument( i >= 0 ) ;
        return i ;
      }

      private Integer pingInterval = null ;
      private Integer pongTimeout = null ;
      private PositiveIntegerRange reconnectDelayRange = null ;
      private Integer pingTimeout = null ;
      private Integer maximumSessionInactivity = null ;


      @Override
      public PongTimeoutStep pingInterval( final int ms ) {
        pingInterval = checkPositive( ms ) ;
        return this ;
      }

      @Override
      public ReconnectDelayStep pongTimeoutOnDownend( final int ms ) {
        pongTimeout = checkPositive( ms ) ;
        return this ;
      }


      @Override
      public PingTimeoutStep reconnectDelay(
          final int delayLowerBoundMs,
          final int delayHigherBoundMs
      ) {
        checkArgument( delayHigherBoundMs >= 0 ) ;
        reconnectDelayRange = new PositiveIntegerRange( delayLowerBoundMs, delayHigherBoundMs ) ;
        return this ;
      }

      @Override
      public SessionInactivityStep pingTimeoutOnUpend( final int ms ) {
        pingTimeout = ms ;
        return this ;
      }

      @Override
      public BuilderStep maximumSessionInactivity( final int ms ) {
        maximumSessionInactivity = checkPositive( ms ) ;
        return this ;
      }

      @Override
      public ForAll build() {
        return new ForAll(
            pingInterval,
            pongTimeout,
            reconnectDelayRange,
            maximumSessionInactivity,
            pingTimeout
        ) ;
      }
    }

  }

  /**
   * Brings various delays together.
   * Inheriting from {@link PrimingForDownend} makes the tests easier to write when
   * we create a {@code UpendConnector.Setup} from a {@code DownendConnector.Setup}, test code
   * just has to cast {@link PrimingForDownend} into a {@link ForAll}.
   *
   */
  final class ForAll implements ForDownend {

    /**
     * How long the Downend should wait before sending next ping.
     */
    public final int pingIntervalMs ;

    @Override
    public int pingIntervalMs() {
      return pingIntervalMs ;
    }

    /**
     * Maximum duration the Downend can wait for after sending a ping and without receiving a pong,
     * before deciding the Upend got unreachable.
     */
    public final int pongTimeoutMs ;

    /**
     * Range made of minimum and maximum delay to wait for before attempting to reconnect ;
     * wait is randomized to avoid reconnection storm after a general network failure.
     */
    public final PositiveIntegerRange reconnectDelayRangeMs ;

    /**
     * Maximum duration the Upend can wait for receiving no ping from Downend, before deciding the
     * Upend got unreachable.
     */
    public final int pingTimeoutMs ;

    /**
     * Maximum lifetime of a session, after deciding the Downend was unreachable.
     */
    public final int sessionInactivityMaximumMs ;

    private ForAll(
        final int pingIntervalMs,
        final int pongTimeoutMs,
        final PositiveIntegerRange reconnectDelayMs,
        final int sessionInactivityMaximumMs,
        final int pingTimeoutMs
    ) {
      checkArgument( pingIntervalMs > 0 ) ;
      this.pingIntervalMs = pingIntervalMs ;
      checkArgument( pongTimeoutMs >= 0 ) ;
      this.pongTimeoutMs = pongTimeoutMs ;

      this.reconnectDelayRangeMs = checkNotNull( reconnectDelayMs ) ;

      checkArgument( sessionInactivityMaximumMs >= 0 ) ;
      this.sessionInactivityMaximumMs = sessionInactivityMaximumMs ;

      this.pingTimeoutMs = pingTimeoutMs ;
    }

    @Override
    public String toString() {
      return ToStringTools.getNiceClassName( this ) + '{' +
          "pingIntervalMs=" + pingIntervalMs + ";" +
          "pongTimeoutMs=" + pongTimeoutMs + ";" +
          "reconnectDelayRangeMs=[" + reconnectDelayRangeMs.lowerBound + "," +
          reconnectDelayRangeMs.upperBound + "];" +
          "pingTimeoutMs=" + pingTimeoutMs + ";" +
          "sessionInactivityMaximumMs=" + sessionInactivityMaximumMs +
          '}'
      ;
    }

    public int reconnectDelayMs( final Random random ) {
      return reconnectDelayRangeMs.random( random ) ;
    }

    @Override
    public int connectTimeoutMs() {
      return reconnectDelayRangeMs.lowerBound ;
    }

    @Override
    public int pongTimeoutMs() {
      return pongTimeoutMs ;
    }

    @Override
    public boolean equals( Object other ) {
      if( this == other ) {
        return true ;
      }
      if( other == null || getClass() != other.getClass() ) {
        return false ;
      }
      final ForAll that = ( ForAll ) other ;
      return EQUIVALENCE.equivalent( this, that ) ;
    }
    
    public enum Key {
      PING_INTERVAL_MS,
      PONG_TIMEOUT_MS,
      RECONNECT_DELAY_RANGE_MS_LOWER_BOUND,
      RECONNECT_DELAY_RANGE_MS_UPPER_BOUND,
      PING_TIMEOUT_MS,
      SESSION_INACTIVITY_MAXIMUM_MS
    }
    
    public ImmutableMap< Key, Integer > asMap() {
      return ImmutableMap.< Key, Integer > builder()
          .put( Key.PING_INTERVAL_MS, pingIntervalMs )
          .put( Key.PONG_TIMEOUT_MS, pongTimeoutMs )
          .put( Key.RECONNECT_DELAY_RANGE_MS_LOWER_BOUND, reconnectDelayRangeMs.lowerBound )
          .put( Key.RECONNECT_DELAY_RANGE_MS_UPPER_BOUND, reconnectDelayRangeMs.upperBound )
          .put( PING_TIMEOUT_MS, pingTimeoutMs )
          .put( Key.SESSION_INACTIVITY_MAXIMUM_MS, sessionInactivityMaximumMs )
          .build()
      ;
    }

    public static ForAll parse( Function< Key, Integer > valueResolver ) {
      return TimeBoundary.Builder.createNew()
          .pingInterval( valueResolver.apply( Key.PING_INTERVAL_MS ) )
          .pongTimeoutOnDownend( valueResolver.apply( Key.PONG_TIMEOUT_MS ) )
          .reconnectDelay(
              valueResolver.apply( Key.RECONNECT_DELAY_RANGE_MS_LOWER_BOUND ),
              valueResolver.apply( Key.RECONNECT_DELAY_RANGE_MS_UPPER_BOUND )
          )
          .pingTimeoutOnUpend( valueResolver.apply( Key.PING_TIMEOUT_MS ) )
          .maximumSessionInactivity( valueResolver.apply( Key.SESSION_INACTIVITY_MAXIMUM_MS ) )
          .build()
      ;
    }

    @Override
    public int hashCode() {
      return EQUIVALENCE.hash( this ) ;
    }

    public static final Equivalence< ForAll > EQUIVALENCE = new Equivalence< ForAll >() {
      @Override
      protected boolean doEquivalent( @Nonnull final ForAll first, @Nonnull final ForAll second ) {
        if( first.pingIntervalMs != second.pingIntervalMs ) {
          return false ;
        }
        if( first.pongTimeoutMs != second.pongTimeoutMs ) {
          return false ;
        }
        if( first.pingTimeoutMs != second.pingTimeoutMs ) {
          return false ;
        }
        if( first.sessionInactivityMaximumMs != second.sessionInactivityMaximumMs ) {
          return false ;
        }
        return first.reconnectDelayRangeMs.equals( second.reconnectDelayRangeMs ) ;
      }

      @Override
      protected int doHash( @Nonnull final ForAll forAll ) {
        int result = forAll.pingIntervalMs ;
        result = 31 * result + forAll.pongTimeoutMs ;
        result = 31 * result + forAll.reconnectDelayRangeMs.hashCode() ;
        result = 31 * result + forAll.pingTimeoutMs ;
        result = 31 * result + forAll.sessionInactivityMaximumMs ;
        return result ;
      }
    };
  }

}
