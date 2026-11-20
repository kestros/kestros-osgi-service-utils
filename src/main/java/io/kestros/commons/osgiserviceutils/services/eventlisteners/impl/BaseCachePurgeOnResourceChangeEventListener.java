/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.kestros.commons.osgiserviceutils.services.eventlisteners.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.kestros.commons.osgiserviceutils.exceptions.CachePurgeException;
import io.kestros.commons.osgiserviceutils.services.BaseServiceResolverService;
import io.kestros.commons.osgiserviceutils.services.cache.CacheService;
import io.kestros.commons.osgiserviceutils.services.eventlisteners.CachePurgeOnResourceChangeEventListener;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Baseline logic for clearing cache based on a ResourceChange Event.
 *
 * <p>While the server starts, changes are held: bundles are still installing their initial
 * content, so every purge (and the rebuild some caches run as part of it) would be redone as the
 * next bundle's content lands. The held changes are purged once, when the framework has started,
 * Sling's repository startup thread has finished (it installs the initial content of every bundle
 * that started before the repository was up) and no change has arrived for
 * {@link #startupQuietMillis()}. From then on every change purges at once, as before. The purge on
 * activation is not held.
 */
public abstract class BaseCachePurgeOnResourceChangeEventListener extends BaseServiceResolverService
        implements CachePurgeOnResourceChangeEventListener {

  /**
   * How long no change may arrive, after the framework has started, before the changes held
   * during startup are purged. System property {@code kestros.cachePurge.startupQuietMillis}; 0
   * turns the startup hold off.
   */
  static final long STARTUP_QUIET_MILLIS = Long.getLong("kestros.cachePurge.startupQuietMillis",
          5000L);

  /**
   * The name Sling gives the thread that starts the repository and, by registering it, runs the
   * content loader for every bundle already started (AbstractSlingRepositoryManager).
   */
  static final String REPOSITORY_STARTUP_THREAD = "Apache Sling Repository Startup Thread";

  /** One small daemon thread for every listener's startup purge; it ends when idle. */
  private static final ScheduledThreadPoolExecutor STARTUP_PURGES = startupPurges();

  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Object startupLock = new Object();
  private Boolean started;
  private long lastHeldChange;
  private int heldChanges;
  private ScheduledFuture<?> startupPurge;
  private BundleContext bundleContext;

  protected abstract boolean purgeOnActivation();

  /**
   * How long no change may arrive, after the framework has started, before the changes held
   * during startup are purged; 0 purges every change at once, also during startup.
   *
   * @return milliseconds.
   */
  protected long startupQuietMillis() {
    return STARTUP_QUIET_MILLIS;
  }

  /**
   * Activates event lister service. Opens ResourceResolver for service user.
   *
   * @param ctx ComponentContext.
   */
  @Activate
  public void activate(@Nonnull final ComponentContext ctx) {
    super.activate(ctx);
    this.bundleContext = ctx != null ? ctx.getBundleContext() : null;
    if (this.purgeOnActivation()) {
      // At once, as before: the caches this builds serve pages while the server keeps starting.
      purgeNow();
    }
  }

  @Override
  public void onChange(@Nonnull final List<ResourceChange> list) {
    if (holdDuringStartup()) {
      return;
    }
    purgeNow();
  }

  /**
   * While the server starts, records the change and (re)schedules the one purge for after
   * startup.
   *
   * @return true if the change was held.
   */
  private boolean holdDuringStartup() {
    synchronized (startupLock) {
      if (started == null) {
        started = startupQuietMillis() <= 0 || bundleContext == null;
      }
      if (started) {
        return false;
      }
      lastHeldChange = System.currentTimeMillis();
      heldChanges++;
      if (startupPurge == null) {
        startupPurge = STARTUP_PURGES.schedule(this::startupPurgeIfQuiet, startupQuietMillis(),
                TimeUnit.MILLISECONDS);
      }
      return true;
    }
  }

  private void startupPurgeIfQuiet() {
    final int held;
    synchronized (startupLock) {
      startupPurge = null;
      final long quietFor = System.currentTimeMillis() - lastHeldChange;
      final long quiet = startupQuietMillis();
      if (!frameworkStarted() || repositoryStarting() || quietFor < quiet) {
        final long wait = Math.max(Math.min(1000L, quiet), quiet - quietFor);
        startupPurge = STARTUP_PURGES.schedule(this::startupPurgeIfQuiet, wait,
                TimeUnit.MILLISECONDS);
        return;
      }
      started = true;
      held = heldChanges;
    }
    log.info("{}: startup finished, purging once for {} changes held during startup.",
            getDisplayName().replaceAll("[\r\n]", ""), held);
    try {
      purgeNow();
    } catch (RuntimeException e) {
      log.warn("{}: startup purge failed: {}", getDisplayName().replaceAll("[\r\n]", ""),
              String.valueOf(e.getMessage()).replaceAll("[\r\n]", ""));
    }
  }

  private boolean frameworkStarted() {
    try {
      final Bundle system = bundleContext.getBundle(0);
      return system == null || system.getState() == Bundle.ACTIVE;
    } catch (IllegalStateException e) {
      // This bundle's context is no longer valid: the purge is moot.
      return true;
    }
  }

  /**
   * Whether Sling is still starting the repository, and with it loading bundles' initial content.
   *
   * @return true while the repository startup thread runs.
   */
  protected boolean repositoryStarting() {
    for (final Thread thread : Thread.getAllStackTraces().keySet()) {
      if (thread.isAlive() && thread.getName().startsWith(REPOSITORY_STARTUP_THREAD)) {
        return true;
      }
    }
    return false;
  }

  private static ScheduledThreadPoolExecutor startupPurges() {
    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, r -> {
      final Thread thread = new Thread(r, "kestros-startup-cache-purge");
      thread.setDaemon(true);
      return thread;
    });
    executor.setKeepAliveTime(30, TimeUnit.SECONDS);
    executor.allowCoreThreadTimeOut(true);
    executor.setRemoveOnCancelPolicy(true);
    return executor;
  }

  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  private void purgeNow() {
    try (ResourceResolver resourceResolver = getServiceResourceResolver()) {
      for (final CacheService cacheService : getCacheServices()) {
        try {
          if (cacheService != null) {
            cacheService.purgeAll(resourceResolver);
          } else {
            log.error("Failed to purge cache for{}. No cache service detected.",
                    getClass().getSimpleName().replaceAll("[\r\n]", ""));
          }
        } catch (final CachePurgeException exception) {
          if (exception.getMessage() != null) {
            log.error("Failed to create cache purge job. {}",
                    exception.getMessage().replaceAll("[\r\n]", ""));
          } else {
            log.error("Failed to create cache purge job and exception message was null.");
          }
        }
      }
    } catch (LoginException e) {
      log.error("Failed to get service resource resolver. {}",
              e.getMessage().replaceAll("[\r\n]", ""));
    }
  }
}
