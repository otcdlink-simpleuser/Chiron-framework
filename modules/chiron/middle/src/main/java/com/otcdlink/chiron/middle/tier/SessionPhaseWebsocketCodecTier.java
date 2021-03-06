package com.otcdlink.chiron.middle.tier;

import com.google.common.base.Charsets;
import com.otcdlink.chiron.buffer.BytebufCoat;
import com.otcdlink.chiron.buffer.BytebufTools;
import com.otcdlink.chiron.command.Command;
import com.otcdlink.chiron.middle.ChannelTools;
import com.otcdlink.chiron.middle.session.FeatureMapper;
import com.otcdlink.chiron.middle.session.SessionLifecycle;
import com.otcdlink.chiron.toolbox.ToStringTools;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

public abstract class SessionPhaseWebsocketCodecTier
    extends SelectiveDuplexTier< WebSocketFrame, SessionLifecycle.Phase >
{
  @SuppressWarnings( "unused" )
  private static final Logger LOGGER = LoggerFactory.getLogger(
      SessionPhaseWebsocketCodecTier.class ) ;

  /**
   * Any {@link Command} with such name will cause some mess.
   * But it's unlikely because we use a nested class name without escaping.
   */
  private static final String PHASE_MAGIC =
      ToStringTools.getNiceName( SessionLifecycle.Phase.class ) + ' ' ;

  private static final Charset ENCODING = Charsets.US_ASCII ;

  public static final ChannelTools.ByteSequenceMatcher MAGIC_MATCHER =
      new ChannelTools.ByteSequenceMatcher( ( PHASE_MAGIC ).getBytes( ENCODING ) ) ;

  private final BytebufTools.Coating coating = BytebufTools.threadLocalRecyclableCoating() ;

  @Override
  protected void inboundMessage(
      final ChannelHandlerContext channelHandlerContext,
      final WebSocketFrame webSocketFrame
  ) throws Exception {
    webSocketFrame.touch( "Deserializing inbound message " + webSocketFrame ) ;
    final BytebufCoat coat = coating.coat( webSocketFrame.content() ) ;

    final SessionLifecycle.Phase phase ;

    // Need to recycle before entering other code parts that could try to
    // reuse our thread-local coat, which is not reentrant.
    try {
      if( MAGIC_MATCHER.matchThenSkip( coat ) ) {
        phase = SessionLifecycle.deserialize( coat ) ;
        webSocketFrame.release() ;
      } else {
        phase = null ;
      }
    } finally {
      coating.recycle() ;
    }

    if( phase != null ) {
      forwardInbound(
          channelHandlerContext,
          decorateMaybe( phase )
      ) ;
    } else if( webSocketFrame instanceof CloseWebSocketFrame ) {
      inboundCloseFrame( channelHandlerContext ) ;
      webSocketFrame.release() ;
    } else {
      channelHandlerContext.fireChannelRead( webSocketFrame ) ;
    }

  }

  protected abstract void inboundCloseFrame( final ChannelHandlerContext channelHandlerContext ) ;

  /**
   * Override to decorate with {@link FeatureMapper}.
   */
  protected SessionLifecycle.Phase decorateMaybe( final SessionLifecycle.Phase phase ) {
    return phase ;
  }

  @Override
  protected void outboundMessage(
      final ChannelHandlerContext channelHandlerContext,
      final SessionLifecycle.Phase phase,
      final ChannelPromise promise
  ) throws Exception {
    final ByteBuf byteBuf = channelHandlerContext.alloc().buffer() ;
    final BytebufCoat coat = coating.coat( byteBuf ) ;
    try {
      coat.writeAsciiUnsafe( PHASE_MAGIC ) ;
      SessionLifecycle.serialize( coat, phase ) ;
    } finally {
      coating.recycle() ;
    }
    forwardOutbound(
        channelHandlerContext,
        new TextWebSocketFrame( byteBuf ),
        promise
    ) ;
  }
}
