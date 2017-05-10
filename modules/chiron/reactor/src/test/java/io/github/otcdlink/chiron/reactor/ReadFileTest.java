package io.github.otcdlink.chiron.reactor;

import io.github.otcdlink.chiron.testing.NameAwareRunner;
import io.github.otcdlink.chiron.toolbox.concurrent.ExecutorTools;
import io.github.otcdlink.chiron.toolbox.text.Plural;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Semaphore;

@RunWith( NameAwareRunner.class )
public class ReadFileTest {


  @Test
  public void readFile() throws Exception {
    final File file = new File( NameAwareRunner.testDirectory(), "lines.txt" ) ;
    writeFile( file, 100 ) ;
    final Semaphore completion = new Semaphore( 0 ) ;

    Flux
        .fromIterable( new FileChunker( file ).linesAsIterable() )
        .publishOn( scheduler( "file" ) )
        .doOnNext( line -> LOGGER.info( "Hook: can see '" + line + "'." ) )
        .publishOn( scheduler( "print" ) )
        .doOnComplete( () -> {
          LOGGER.info( "Hook: complete." ) ;
          completion.release() ;
        } )
        .subscribe( line -> LOGGER.info( "Got '" + line + "'." ) )
    ;

    completion.acquire() ;
  }


// =======
// Fixture
// =======

  private static final Logger LOGGER = LoggerFactory.getLogger( ReadFileTest.class ) ;

  private void writeFile( final File file, final int lineCount ) throws IOException {
    try( final FileWriter fileWriter = new FileWriter( file ) ;
         final BufferedWriter bufferedWriter = new BufferedWriter( fileWriter )
    ) {
      for( int i = 0 ; i < lineCount ; i ++ ) {
        bufferedWriter.write( "line " + i + "\n" ) ;
      }
    }
    LOGGER.info(
        "Wrote " + Plural.s( lineCount, "line" ) + " into " + file.getAbsolutePath() + "." ) ;
  }

  private static Scheduler scheduler( final String threadpoolRadix ) {
    return Schedulers.fromExecutor(
        ExecutorTools.singleThreadedExecutorServiceFactory( threadpoolRadix ).create() ) ;
  }


}