package io.kestros.commons.osgiserviceutils.exceptions;

/**
 * Exception thrown when a CacheService fails to retrieve a cached value.
 */
public class CacheRetrievalException extends Exception {

  private static final long serialVersionUID = -8169362340090402124L;

  /**
   * Exception thrown when a CacheService fails to retrieve a cached value.
   *
   * @param message Cause message.
   */
  public CacheRetrievalException(final String message) {
    super(message);
  }

}
