package com.otcdlink.chiron.middle.shaft;

import com.google.common.collect.ImmutableList;
import com.otcdlink.chiron.command.Command;
import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;

public interface CommandFailureVerifier {

  static CommandFailureVerifier simpleVerifier(
      final Class< ? extends Command> expectedCommandClass,
      final Class< ? extends Throwable > expectedThrowableClass
  ) {
    return commandExecutionFailures -> {
      assertThat( commandExecutionFailures.size() ).isEqualTo( 1 ) ;
      final CommandExecutionFailure failure = commandExecutionFailures.get( 0 ) ;
      Assertions.assertThat( failure.command ).isInstanceOf( expectedCommandClass ) ;
      assertThat( failure.throwable ).isInstanceOf( expectedThrowableClass ) ;
    } ;
  }

  void verify( ImmutableList< CommandExecutionFailure > commandExecutionFailures ) ;
}
