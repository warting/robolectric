package org.robolectric.shadows;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Scroller;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(Scroller.class)
public class ShadowScroller {
  private int startX;
  private int startY;
  private int finalX;
  private int finalY;
  private long startTime;
  private long stoppedTime;
  private long duration;
  private boolean started;
  private boolean aborted;

  @Implementation
  protected void abortAnimation() {
    started = false;
    aborted = true;
    stoppedTime = SystemClock.uptimeMillis();
  }

  @Implementation
  protected int getStartX() {
    return startX;
  }

  @Implementation
  protected int getStartY() {
    return startY;
  }

  @Implementation
  protected int getCurrX() {
    long dt = deltaTime();
    return dt >= duration ? finalX : startX + (int) ((deltaX() * dt) / duration);
  }

  @Implementation
  protected int getCurrY() {
    long dt = deltaTime();
    return dt >= duration ? finalY : startY + (int) ((deltaY() * dt) / duration);
  }

  @Implementation
  protected int getFinalX() {
    return finalX;
  }

  @Implementation
  protected int getFinalY() {
    return finalY;
  }

  @Implementation
  protected int getDuration() {
    return (int) duration;
  }

  @Implementation
  protected void startScroll(int startX, int startY, int dx, int dy, int duration) {
    this.startX = startX;
    this.startY = startY;
    finalX = startX + dx;
    finalY = startY + dy;
    startTime = SystemClock.uptimeMillis();
    this.duration = duration;
    started = true;
    aborted = false;
    // enqueue a dummy task so that the scheduler will actually run
    new Handler(Looper.getMainLooper()).postDelayed(() -> {}, duration);
  }

  @Implementation
  protected boolean computeScrollOffset() {
    if (!started) {
      return false;
    }
    started &= deltaTime() < duration;
    return true;
  }

  @Implementation
  protected boolean isFinished() {
    return aborted || deltaTime() > duration;
  }

  @Implementation
  protected int timePassed() {
    return (int) deltaTime();
  }

  private long deltaTime() {
    return aborted ? stoppedTime - startTime : SystemClock.uptimeMillis() - startTime;
  }

  private int deltaX() {
    return (finalX - startX);
  }

  private int deltaY() {
    return (finalY - startY);
  }
}
