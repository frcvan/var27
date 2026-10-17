// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package org.wpilib.command3;

import java.util.Objects;
import org.graalvm.webimage.api.JS;
import org.graalvm.webimage.api.JSObject;

/**
 * A one-shot continuation backed by the host's JSPI stack suspension support.
 *
 * <p>Like the commands framework, this implementation must only be used on one thread. The host
 * must await each scheduler tick before starting another one.
 */
final class Continuation {
  private static final ThreadLocal<Continuation> mountedContinuation = new ThreadLocal<>();
  private static Continuation activeContinuation;

  private final ContinuationScope m_scope;
  private final Runnable m_target;
  private final JSObject m_state;
  private Continuation m_parent;
  private ContinuationScope m_yieldScope;
  private Throwable m_failure;
  private boolean m_running;
  private boolean m_done;

  static final class InternalContinuationError extends Error {
    InternalContinuationError(String message, Throwable cause) {
      super(message, cause);
    }
  }

  Continuation(ContinuationScope scope, Runnable target) {
    m_scope = Objects.requireNonNull(scope);
    m_target = target;
    m_state = create(this);
  }

  /**
   * Suspends the active continuation up to this continuation's scope.
   *
   * @return true after the continuation has been resumed
   * @throws IllegalStateException if no active continuation has this scope
   */
  public boolean yield() {
    Continuation current = activeContinuation;
    Continuation scoped = current;
    while (scoped != null && scoped.m_scope != m_scope) {
      scoped = scoped.m_parent;
    }
    if (scoped == null) {
      throw new IllegalStateException("Not in scope " + m_scope);
    }
    current.yieldToScope(m_scope);
    return true;
  }

  private void yieldToScope(ContinuationScope scope) {
    m_yieldScope = scope;
    try {
      wasmContinuationYield(m_state);
    } finally {
      m_yieldScope = null;
    }
  }

  /** Runs or resumes the body until it completes or yields to this continuation's scope. */
  public void run() {
    if (m_running) {
      throw new IllegalStateException("Continuation is already running");
    }
    if (m_done) {
      throw new IllegalStateException("Continuation terminated");
    }

    Continuation previous = activeContinuation;
    m_parent = previous;
    m_running = true;
    try {
      while (true) {
        activeContinuation = this;
        wasmContinuationResume(m_state);
        activeContinuation = previous;
        if (m_yieldScope == null || m_yieldScope == m_scope) {
          break;
        }
        // A nested body yielded to an enclosing scope. Suspend its caller before resuming it.
        previous.yieldToScope(m_yieldScope);
      }
    } finally {
      activeContinuation = previous;
      m_parent = null;
      m_running = false;
    }

    if (m_failure instanceof RuntimeException exception) {
      throw exception;
    }
    if (m_failure instanceof Error error) {
      throw error;
    }
    if (m_failure != null) {
      throw new InternalContinuationError(
          "Continuation.run() encountered an unexpected error", m_failure);
    }
  }

  /**
   * Raw WASM entry point invoked once by the host's promising export wrapper.
   *
   * <p>Keep Java failures inside WASM so run() can rethrow the original exception without a
   * JavaScript interop wrapper changing its type or identity.
   */
  public void wasmContinuationBody() {
    try {
      m_target.run();
    } catch (Throwable failure) {
      m_failure = failure;
    } finally {
      m_done = true;
    }
  }

  /** Returns whether the body completed, including completion by an uncaught exception. */
  public boolean isDone() {
    return m_done;
  }

  /** Mounts a continuation, or clears the mounted continuation when passed null. */
  public static void mountContinuation(Continuation continuation) {
    if (continuation == null) {
      mountedContinuation.remove();
    } else {
      mountedContinuation.set(continuation);
    }
  }

  /** Returns the continuation mounted on the current thread. */
  public static Continuation getMountedContinuation() {
    return mountedContinuation.get();
  }

  boolean isMounted() {
    return this == getMountedContinuation();
  }

  @Override
  public String toString() {
    return super.toString() + " scope: " + m_scope;
  }

  // This is the only dependency on Graal's private Java-to-WASM reference conversion helper.
  @JS("return globalThis.wasmContinuations.create(conversion.unproxy(continuation));")
  private static native JSObject create(Continuation continuation);

  @JS("return globalThis.wasmContinuations.resume(state);")
  private static native void wasmContinuationResume(JSObject state);

  @JS("return globalThis.wasmContinuations.yield(state);")
  private static native void wasmContinuationYield(JSObject state);
}
