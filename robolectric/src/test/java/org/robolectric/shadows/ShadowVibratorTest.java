package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.O;
import static android.os.Build.VERSION_CODES.Q;
import static android.os.VibrationEffect.EFFECT_CLICK;
import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
public class ShadowVibratorTest {
  private Vibrator vibrator;

  @Before
  public void before() {
    vibrator =
        (Vibrator)
            ApplicationProvider.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
  }

  @Test
  public void hasVibrator() {
    assertThat(vibrator.hasVibrator()).isTrue();

    shadowOf(vibrator).setHasVibrator(false);

    assertThat(vibrator.hasVibrator()).isFalse();
  }

  @Config(minSdk = O)
  @Test
  public void hasAmplitudeControl() {
    assertThat(vibrator.hasAmplitudeControl()).isFalse();

    shadowOf(vibrator).setHasAmplitudeControl(true);

    assertThat(vibrator.hasAmplitudeControl()).isTrue();
  }

  @Test
  public void vibrateMilliseconds() {
    vibrator.vibrate(5000);

    assertThat(shadowOf(vibrator).isVibrating()).isTrue();
    assertThat(shadowOf(vibrator).getMilliseconds()).isEqualTo(5000L);

    shadowMainLooper().idleFor(5, TimeUnit.SECONDS);
    assertThat(shadowOf(vibrator).isVibrating()).isFalse();
  }

  @Test
  public void vibratePattern() {
    long[] pattern = new long[] {0, 200};
    vibrator.vibrate(pattern, 1);

    assertThat(shadowOf(vibrator).isVibrating()).isTrue();
    assertThat(shadowOf(vibrator).getPattern()).isEqualTo(pattern);
    assertThat(shadowOf(vibrator).getRepeat()).isEqualTo(1);
  }

  @Config(minSdk = Q)
  @Test
  public void vibratePredefined() {
    vibrator.vibrate(VibrationEffect.createPredefined(EFFECT_CLICK));

    assertThat(shadowOf(vibrator).getEffectId()).isEqualTo(EFFECT_CLICK);
  }

  @Test
  public void cancelled() {
    vibrator.vibrate(5000);
    assertThat(shadowOf(vibrator).isVibrating()).isTrue();
    assertThat(shadowOf(vibrator).isCancelled()).isFalse();
    vibrator.cancel();

    assertThat(shadowOf(vibrator).isVibrating()).isFalse();
    assertThat(shadowOf(vibrator).isCancelled()).isTrue();
  }
}
