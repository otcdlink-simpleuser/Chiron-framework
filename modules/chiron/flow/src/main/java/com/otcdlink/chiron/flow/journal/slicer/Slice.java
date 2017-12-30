package com.otcdlink.chiron.flow.journal.slicer;

import io.netty.buffer.AbstractByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ByteProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a sequence of bytes in a file, found between delimiters.
 */
public final class Slice extends AbstractByteBuf {

  protected final int indexInRecycler ;
  public final long sliceIndexInFile ;
  protected final Consumer< Slice > recycler ;
  private final ByteBuf buffer ;

  protected Slice(
      final int indexInRecycler,
      final int maxCapacity,
      final Consumer< Slice > recycler,
      final ByteBuf wrapped,
      final int readerIndex,
      final int writerIndex,
      final long sliceIndexInFile
  ) {
    super( maxCapacity ) ;
    this.recycler = checkNotNull( recycler ) ;
    this.indexInRecycler = indexInRecycler ;
    buffer = checkNotNull( wrapped ) ;
    setIndex( readerIndex, writerIndex ) ;
    this.sliceIndexInFile = sliceIndexInFile;
  }

  /**
   * Must be called only once after this instance of {@link Slice} has been passed
   * to the code processing {@link Slice}s.
   * We don't override {@link #release()} since the contract is different, this contract is
   * very specific to {@link FileChunk}.
   */
  public void recycle() {
    recycler.accept( this ) ;
  }

  public long lineIndexInFile() {
    return sliceIndexInFile;
  }


// ====================================================
// Mandatory methods, copied from UnpooledSlicedByteBuf
// ====================================================


  @Override
  protected void finalize() throws Throwable { }

  @Override
  public final int refCnt() {
    return refCnt0();
  }

  int refCnt0() {
     return unwrap().refCnt();
//    return 0 ;
  }

  @Override
  public final ByteBuf retain() {
    return retain0();
  }

  ByteBuf retain0() {
     unwrap().retain() ;
    return this;
  }

  @Override
  public final ByteBuf retain(int increment) {
    return retain0(increment);
  }

  ByteBuf retain0(int increment) {
     unwrap().retain(increment);
    return this;
  }

  @Override
  public final boolean release() {
    return release0();
  }

  boolean release0() {
//    return true ;
     return unwrap().release() ;
  }

  @Override
  public final boolean release(int decrement) {
    return release0(decrement);
  }

  boolean release0(int decrement) {
//    return true ;
     return unwrap().release( decrement ) ;
  }

  ByteBuf touch0() {
    unwrap().touch() ;
    return this ;
  }

  @Override
  public final ByteBuf touch(Object hint) {
    return touch0(hint);
  }

  ByteBuf touch0(Object hint) {
    unwrap().touch(hint);
    return this;
  }

  @Override
  public final ByteBuf touch() {
    return touch0();
  }

  @Override
  public boolean isReadOnly() {
    return unwrap().isReadOnly();
  }

  @Override
  public ByteBuffer internalNioBuffer(int index, int length) {
    return nioBuffer(index, length);
  }

  @Override
  public ByteBuffer nioBuffer(int index, int length) {
    return unwrap().nioBuffer(index, length);
  }


// ================================================
// Mandatory methods, copied from DuplicatedByteBuf
// ================================================


  @Override
  public ByteBuf unwrap() {
    return buffer;
  }

  @Override
  public ByteBufAllocator alloc() {
    return unwrap().alloc();
  }

  @Override
  @Deprecated
  public ByteOrder order() {
    return unwrap().order();
  }

  @Override
  public boolean isDirect() {
    return unwrap().isDirect();
  }

  @Override
  public int capacity() {
    return unwrap().capacity();
  }

  @Override
  public ByteBuf capacity(int newCapacity) {
    unwrap().capacity(newCapacity);
    return this;
  }

  @Override
  public boolean hasArray() {
    return unwrap().hasArray();
  }

  @Override
  public byte[] array() {
    return unwrap().array();
  }

  @Override
  public int arrayOffset() {
    return unwrap().arrayOffset();
  }

  @Override
  public boolean hasMemoryAddress() {
    return unwrap().hasMemoryAddress();
  }

  @Override
  public long memoryAddress() {
    return unwrap().memoryAddress();
  }

  @Override
  public byte getByte(int index) {
    return unwrap().getByte(index);
  }

  @Override
  protected byte _getByte(int index) {
    return unwrap().getByte(index);
  }

  @Override
  public short getShort(int index) {
    return unwrap().getShort(index);
  }

  @Override
  protected short _getShort(int index) {
    return unwrap().getShort(index);
  }

  @Override
  public short getShortLE(int index) {
    return unwrap().getShortLE(index);
  }

  @Override
  protected short _getShortLE(int index) {
    return unwrap().getShortLE(index);
  }

  @Override
  public int getUnsignedMedium(int index) {
    return unwrap().getUnsignedMedium(index);
  }

  @Override
  protected int _getUnsignedMedium(int index) {
    return unwrap().getUnsignedMedium(index);
  }

  @Override
  public int getUnsignedMediumLE(int index) {
    return unwrap().getUnsignedMediumLE(index);
  }

  @Override
  protected int _getUnsignedMediumLE(int index) {
    return unwrap().getUnsignedMediumLE(index);
  }

  @Override
  public int getInt(int index) {
    return unwrap().getInt(index);
  }

  @Override
  protected int _getInt(int index) {
    return unwrap().getInt(index);
  }

  @Override
  public int getIntLE(int index) {
    return unwrap().getIntLE(index);
  }

  @Override
  protected int _getIntLE(int index) {
    return unwrap().getIntLE(index);
  }

  @Override
  public long getLong(int index) {
    return unwrap().getLong(index);
  }

  @Override
  protected long _getLong(int index) {
    return unwrap().getLong(index);
  }

  @Override
  public long getLongLE(int index) {
    return unwrap().getLongLE(index);
  }

  @Override
  protected long _getLongLE(int index) {
    return unwrap().getLongLE(index);
  }

  @Override
  public ByteBuf copy(int index, int length) {
    return unwrap().copy(index, length);
  }

  @Override
  public ByteBuf slice(int index, int length) {
    return unwrap().slice(index, length);
  }

  @Override
  public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
    unwrap().getBytes(index, dst, dstIndex, length);
    return this;
  }

  @Override
  public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
    unwrap().getBytes(index, dst, dstIndex, length);
    return this;
  }

  @Override
  public ByteBuf getBytes(int index, ByteBuffer dst) {
    unwrap().getBytes(index, dst);
    return this;
  }

  @Override
  public ByteBuf setByte(int index, int value) {
    unwrap().setByte(index, value);
    return this;
  }

  @Override
  protected void _setByte(int index, int value) {
    unwrap().setByte(index, value);
  }

  @Override
  public ByteBuf setShort(int index, int value) {
    unwrap().setShort(index, value);
    return this;
  }

  @Override
  protected void _setShort(int index, int value) {
    unwrap().setShort(index, value);
  }

  @Override
  public ByteBuf setShortLE(int index, int value) {
    unwrap().setShortLE(index, value);
    return this;
  }

  @Override
  protected void _setShortLE(int index, int value) {
    unwrap().setShortLE(index, value);
  }

  @Override
  public ByteBuf setMedium(int index, int value) {
    unwrap().setMedium(index, value);
    return this;
  }

  @Override
  protected void _setMedium(int index, int value) {
    unwrap().setMedium(index, value);
  }

  @Override
  public ByteBuf setMediumLE(int index, int value) {
    unwrap().setMediumLE(index, value);
    return this;
  }

  @Override
  protected void _setMediumLE(int index, int value) {
    unwrap().setMediumLE(index, value);
  }

  @Override
  public ByteBuf setInt(int index, int value) {
    unwrap().setInt(index, value);
    return this;
  }

  @Override
  protected void _setInt(int index, int value) {
    unwrap().setInt(index, value);
  }

  @Override
  public ByteBuf setIntLE(int index, int value) {
    unwrap().setIntLE(index, value);
    return this;
  }

  @Override
  protected void _setIntLE(int index, int value) {
    unwrap().setIntLE(index, value);
  }

  @Override
  public ByteBuf setLong(int index, long value) {
    unwrap().setLong(index, value);
    return this;
  }

  @Override
  protected void _setLong(int index, long value) {
    unwrap().setLong(index, value);
  }

  @Override
  public ByteBuf setLongLE(int index, long value) {
    unwrap().setLongLE(index, value);
    return this;
  }

  @Override
  protected void _setLongLE(int index, long value) {
    unwrap().setLongLE(index, value);
  }

  @Override
  public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
    unwrap().setBytes(index, src, srcIndex, length);
    return this;
  }

  @Override
  public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
    unwrap().setBytes(index, src, srcIndex, length);
    return this;
  }

  @Override
  public ByteBuf setBytes(int index, ByteBuffer src) {
    unwrap().setBytes(index, src);
    return this;
  }

  @Override
  public ByteBuf getBytes(int index, OutputStream out, int length)
      throws IOException {
    unwrap().getBytes(index, out, length);
    return this;
  }

  @Override
  public int getBytes(int index, GatheringByteChannel out, int length)
      throws IOException {
    return unwrap().getBytes(index, out, length);
  }

  @Override
  public int getBytes(int index, FileChannel out, long position, int length)
      throws IOException {
    return unwrap().getBytes(index, out, position, length);
  }

  @Override
  public int setBytes(int index, InputStream in, int length)
      throws IOException {
    return unwrap().setBytes(index, in, length);
  }

  @Override
  public int setBytes(int index, ScatteringByteChannel in, int length)
      throws IOException {
    return unwrap().setBytes(index, in, length);
  }

  @Override
  public int setBytes(int index, FileChannel in, long position, int length)
      throws IOException {
    return unwrap().setBytes(index, in, position, length);
  }

  @Override
  public int nioBufferCount() {
    return unwrap().nioBufferCount();
  }

  @Override
  public ByteBuffer[] nioBuffers(int index, int length) {
    return unwrap().nioBuffers(index, length);
  }

  @Override
  public int forEachByte(int index, int length, ByteProcessor processor) {
    return unwrap().forEachByte(index, length, processor);
  }

  @Override
  public int forEachByteDesc(int index, int length, ByteProcessor processor) {
    return unwrap().forEachByteDesc(index, length, processor);
  }}
