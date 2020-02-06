
package io.kestros.commons.osgiserviceutils.exceptions;

/**
 * Exception thrown when a cache failed to purge its cached data.
 */
public class CachePurgeException extends Exception {

  private static final long serialVersionUID = -8316555778268973921L;

  /**
   * Exception thrown when a cache failed to purge its cached data.
   *
   * @param message Cause message.
   */
  public CachePurgeException(final String message) {
    super(message);
  }
}
