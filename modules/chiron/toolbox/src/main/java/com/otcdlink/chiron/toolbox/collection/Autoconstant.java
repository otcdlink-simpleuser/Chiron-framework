package com.otcdlink.chiron.toolbox.collection;

import com.google.common.collect.ImmutableMap;
import com.otcdlink.chiron.toolbox.ObjectTools;
import com.otcdlink.chiron.toolbox.ToStringTools;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Base class for declaring constants which get aware of their declaration name (inside Java code)
 * as soon as the static member is set.
 * <p>
 * This class differs from {@code Enum} because it supports subclassing, but cannot enforce
 * ordering. It should not replace {@code Enum} whenever it is possible to use {@code Enum}'s
 * tighter contract. As with {@code Enum}, equality between instances of the same class can
 * be evaluated with {@code ==} operator.
 *
 * <h1>Usage</h1>
 * <p>
 * Declare constants as instances created with no-arg constructor.
 * <pre>
public static final class Enumeration extends Autoconstant {
  private Enumeration() { }
  private static Enumeration createNew() { return new Enumeration() ; }
  public static final Enumeration A = createNew() ;
  public static final Enumeration B = createNew() ;
  // Mandatory.
  public static final ImmutableMap< String, Enumeration > MAP = valueMap( Enumeration.class ) ;
}
 * </pre>
 *
 * <h1>Consistency</h1>
 * <p>
 * A call to {@link #valueMap(Class)} or {@link #valueMap(Class, Class)} must happen after declaring
 * constants, and before further use (especially before a call to {@link #name()}).
 * <p>
 * {@link #valueMap(Class) The} {@link #valueMap(Class, Class) methods}
 * for extracting declared values take great care of enforcing consistency.
 * The same {@link Autoconstant} instance can be referenced by different subclasses (possibly
 * using a supertype), but only under the same name. This makes {@link #name()} consistent.
 * <p>
 * Instantiating a concrete {@link Autoconstant} without referencing it will cause the next call
 * to {@link #valueMap(Class, Class)} or {@link #name()} to fail.
 * <p>
 * Calling {@link #name()} before class initialisation got complete will throw an exception.
 *
 * <h1>Thread-safety</h1>
 * <p>
 * Instances of this class are thread-safe (given that subclasses respect immutability patterns).
 * Static methods for extracting constants are thread-safe.
 *
 * <h1>Generic type</h1>
 * <p>
 * Subclasses may declare a generic type, which will be reflexively extracted using
 * {@link #typeParameter()}. This is useful to propagate a type from the {@link Autoconstant}
 * instance (Java enums really can't do that). Generic type only propagates if constant
 * is defined with a subclass that captures the type (see
 * {@link com.google.common.reflect.TypeToken} for a discussion about this).
 * <pre>
public static final class Enumeration< T > extends Autoconstant< T > {
  public static final Enumeration< Integer > A = new Enumeration< Integer >( 1 ) { } ;
  public static final Enumeration< String > B = new Enumerator< String >( "a" ) { } ;
  public final T value ;
  private Enumeration( final T value ) {
    this.value = value ;
  }
  // Mandatory.
  public static final ImmutableMap< String, Enumeration > MAP = valueMap( Enumeration.class ) ;
}
assert Enumeration.A.typeParameter().equals( Integer.class ) ;
 * </pre>
 *
 * @param <INSTANCE_SPECIFIC> a type parameter for subclasses which want a per-instance type.
 */
public abstract class Autoconstant< INSTANCE_SPECIFIC > {

  private static final ConcurrentMap< Autoconstant, ObjectTools.Holder< Declaration >>
      INSTANCES = new ConcurrentHashMap<>() ;

  @SuppressWarnings( "unused" )
  private final INSTANCE_SPECIFIC muteIdeaWarningAboutUnusedTypeParameter = null ;

  private static final class Declaration {

    /**
     * Only useful to tell the origin of a declaration mismatch.
     */
    public final Class referencingClass ;

    public final Field field ;

    public Declaration( final Class declaringClass, final Field field ) {
      this.referencingClass = checkNotNull( declaringClass ) ;
      this.field = checkNotNull( field ) ;
    }

    @Override
    public String toString() {
      return ToStringTools.getNiceClassName( this ) +
          "{" + referencingClass.getName() + ";" + field + "}" ;
    }
  }

  protected Autoconstant() {
    final ObjectTools.Holder< Declaration > previous =
        INSTANCES.put( this, ObjectTools.newHolder() ) ;
    if( previous != null ) {
      checkState(
          ! previous.isSet(),
          "Already registered (how is it possible?) for " + previous + ": " +
              previous.get()
      ) ;
    }
  }

  /**
   * Returns the type parameter of a subclass, if declared with any.
   *
   * @throws DeclarationException if there is no type parameter.
   */
  public final Class< INSTANCE_SPECIFIC > typeParameter() {
    return ( Class< INSTANCE_SPECIFIC > )
        AutoconstantTools.find0( this, Autoconstant.class, "INSTANCE_SPECIFIC" ) ;
  }

  public static < THIS extends Autoconstant> ImmutableMap< String, THIS > valueMap(
      final Class< THIS > ownerClass
  ) {
    return valueMap( ownerClass, ownerClass ) ;
  }


  /**
   * Returns values declared as {@code public}, {@code static}, {@code final} members in the
   * {@code ownerClass} and its superclasses.
   * <p>
   * Do not call this method before concrete class has declared all its constants of
   * {@link Autoconstant} type.
   * <p>
   * This method scans every instance so it is better to set its result to a {@code static}
   * member of a {@link Autoconstant} subclass. This also enforces a fail-fast approach, with
   * declaration problems detected during class initialization.
   * <p>
   * This method performs various checks to enforce definition consistency.
   *
   * @param ownerClass the {@code Class} declaring the constants.
   * @param constantType the type of the constants to gather (references of non-compatible type
   *     will be ignored).
   *
   * @return a {@code Map} sorted by natural key order.
   */
  protected static <
      OWNER extends Autoconstant,
      THIS  extends Autoconstant
  > ImmutableMap< String, THIS > valueMap(
      final Class< OWNER > ownerClass,
      final Class< THIS > constantType
  ) {
    final SortedMap< String, THIS > builder = new TreeMap<>() ;
    for( final Field field : ownerClass.getFields() ) {  // Gets public fields of superclass, too.
      if( Modifier.isStatic( field.getModifiers() ) &&
          Modifier.isPublic( field.getModifiers() ) &&
          constantType.isAssignableFrom( field.getType() )
      ) {
        if( ! Modifier.isFinal( field.getModifiers() ) ) {
          throw new DeclarationException( "Should be final: " + field ) ;
        }
        final Declaration declaration = new Declaration( ownerClass, field ) ;
        final THIS instance ;
        {
          try {
            instance = ( THIS ) field.get( null ) ;
          } catch( final IllegalAccessException e ) {
            throw new Error( "Could not access to a public field: " + field, e ) ;
          }
        }
        checkState( instance != null, "No value set for " + field ) ;
        final ObjectTools.Holder< Declaration > holder = INSTANCES.get( instance ) ;
        checkState(
            holder != null,
            "Did not register " + toStringSafe( instance ) + " (how is it possible?)"
        ) ;
        final Declaration previousDeclaration = holder.setMaybe( declaration ) ;
        if( previousDeclaration != null &&
            ! previousDeclaration.field.getName().equals( declaration.field.getName() )
        ) {
          throw new DeclarationException(
              "Already referenced in " + previousDeclaration.referencingClass.getName() + " as " +
              "'" + previousDeclaration.field.getName() + "'"
          ) ;
        }
        builder.put( declaration.field.getName(), instance ) ;
      }
    }
    return ImmutableMap.copyOf( builder ) ;
  }


  /**
   * Don't call until the static reference has been set.
   *
   * @return the Java name.
   */
  public final String name() {
    return field().getName() ;
  }

  /**
   * Don't call until the static reference has been set.
   *
   * @return the Java field.
   */
  public final Field field() {
    return INSTANCES.get( this ).get().field ;
  }

  /**
   * Need a reference-based hash code for {@link #INSTANCES}.
   */
  @Override
  public final int hashCode() {
    return super.hashCode() ;
  }

  /**
   * Need a reference-based equality for {@link #INSTANCES}.
   */
  @Override
  public final boolean equals( final Object other ) {
    return super.equals( other ) ;
  }

  private static String toStringSafe( final Autoconstant instance ) {
    return ToStringTools.nameAndCompactHash( instance ) ;
  }

  @Override
  public String toString() {
    return ToStringTools.getNiceClassName( this ) + '{' + name() + '}' ;
  }

  public static class DeclarationException extends RuntimeException {
    public DeclarationException( final String message ) {
      super( message ) ;
    }
  }
}
