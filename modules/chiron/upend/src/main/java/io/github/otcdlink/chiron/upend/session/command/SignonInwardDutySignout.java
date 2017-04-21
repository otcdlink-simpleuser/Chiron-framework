package io.github.otcdlink.chiron.upend.session.command;

import io.github.otcdlink.chiron.buffer.PositionalFieldReader;
import io.github.otcdlink.chiron.buffer.PositionalFieldWriter;
import io.github.otcdlink.chiron.codec.DecodeException;
import io.github.otcdlink.chiron.command.Command;
import io.github.otcdlink.chiron.designator.Designator;
import io.github.otcdlink.chiron.upend.session.SignonInwardDuty;

import java.io.IOException;

@Command.Description( name = "signout" )
public class SignonInwardDutySignout
    extends Command< Designator, SignonInwardDuty>
{

  public SignonInwardDutySignout( final Designator designatorInternal ) {
    super( designatorInternal ) ;
  }

  @Override
  public void callReceiver( final SignonInwardDuty signonInwardDuty ) {
    signonInwardDuty.signout( endpointSpecific ) ;
  }

  @Override
  public void encodeBody( final PositionalFieldWriter positionalFieldWriter ) throws IOException {
    // No field to encode.
  }

  @SuppressWarnings( "UnusedParameters" )
  public static SignonInwardDutySignout decode(
      final Designator designator,
      final PositionalFieldReader reader
  ) throws DecodeException {
    return new SignonInwardDutySignout( designator ) ;
  }

}
