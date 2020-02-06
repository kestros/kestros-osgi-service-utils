package io.kestros.commons.osgiserviceutils.exceptions;

/**
 * Exception thrown when a {@link io.kestros.commons.osgiserviceutils.services.cache.CacheService}
 * fails to cache a value.
 */
public class CacheBuilderException extends Exception {

  private static final long serialVersionUID = -8725381618347773510L;

  /**
   * Exception thrown when a {@link io.kestros.commons.osgiserviceutils.services.cache.CacheService}
   * fails to cache a value.
   *
   * @param message Cause message.
   */
  public CacheBuilderException(final String message) {
    super(message);
  }

}
