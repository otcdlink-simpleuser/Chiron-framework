package com.otcdlink.chiron.flow.journal;

import com.google.common.base.Charsets;
import com.otcdlink.chiron.command.Command;
import com.otcdlink.chiron.designator.Designator;
import com.otcdlink.chiron.integration.echo.EchoUpwardDuty;
import com.otcdlink.chiron.toolbox.text.LineBreak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class IntradayHeapPersisterTest extends AbstractJournalPersisterTest {

// =======
// Fixture
// =======

  @SuppressWarnings( "unused" )
  private static final Logger LOGGER = LoggerFactory.getLogger( IntradayHeapPersisterTest.class ) ;

  @Override
  protected PersisterKit newPersisterKit() {
    return new PrivatePersisterKit( 1000, LineBreak.CR_UNIX ) ;
  }

  private final class PrivatePersisterKit extends PersisterKit {
    public PrivatePersisterKit(
        final int bufferSize,
        final LineBreak lineBreak
    ) {
      super( bufferSize, lineBreak ) ;
    }

    @Override
    public String loadActualFile() throws IOException {
      final byte[] bytes = ( ( JournalHeapPersister ) persister ).getBytes() ;
      return new String( bytes, Charsets.US_ASCII ) ;
    }

    @Override
    protected JournalPersister<Command< Designator, EchoUpwardDuty< Designator > > > createPersister() {
      return new JournalHeapPersister<>(
          bufferSize,
          new FileDesignatorCodecTools.InwardDesignatorEncoder(),
          1,
          "JustTesting",
          lineBreak
      ) ;

    }
  }

}