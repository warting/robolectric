package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.Engine;
import android.speech.tts.UtteranceProgressListener;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
public class ShadowTextToSpeechTest {
  private TextToSpeech textToSpeech;
  private Activity activity;
  private TextToSpeech.OnInitListener listener;
  private UtteranceProgressListener mockListener;

  @Before
  public void setUp() throws Exception {
    activity = Robolectric.buildActivity(Activity.class).create().get();
    listener =
        new TextToSpeech.OnInitListener() {
          @Override
          public void onInit(int i) {}
        };

    mockListener = mock(UtteranceProgressListener.class);
    textToSpeech = new TextToSpeech(activity, listener);
  }

  @Test
  public void shouldNotBeNull() throws Exception {
    assertThat(textToSpeech).isNotNull();
    assertThat(shadowOf(textToSpeech)).isNotNull();
  }

  @Test
  public void getContext_shouldReturnContext() throws Exception {
    assertThat(shadowOf(textToSpeech).getContext()).isEqualTo(activity);
  }

  @Test
  public void getOnInitListener_shouldReturnListener() throws Exception {
    assertThat(shadowOf(textToSpeech).getOnInitListener()).isEqualTo(listener);
  }

  @Test
  public void getLastSpokenText_shouldReturnSpokenText() throws Exception {
    textToSpeech.speak("Hello", TextToSpeech.QUEUE_FLUSH, null);
    assertThat(shadowOf(textToSpeech).getLastSpokenText()).isEqualTo("Hello");
  }

  @Test
  public void getLastSpokenText_shouldReturnMostRecentText() throws Exception {
    textToSpeech.speak("Hello", TextToSpeech.QUEUE_FLUSH, null);
    textToSpeech.speak("Hi", TextToSpeech.QUEUE_FLUSH, null);
    assertThat(shadowOf(textToSpeech).getLastSpokenText()).isEqualTo("Hi");
  }

  @Test
  public void clearLastSpokenText_shouldSetLastSpokenTextToNull() throws Exception {
    textToSpeech.speak("Hello", TextToSpeech.QUEUE_FLUSH, null);
    shadowOf(textToSpeech).clearLastSpokenText();
    assertThat(shadowOf(textToSpeech).getLastSpokenText()).isNull();
  }

  @Test
  public void isShutdown_shouldReturnFalseBeforeShutdown() throws Exception {
    assertThat(shadowOf(textToSpeech).isShutdown()).isFalse();
  }

  @Test
  public void isShutdown_shouldReturnTrueAfterShutdown() throws Exception {
    textToSpeech.shutdown();
    assertThat(shadowOf(textToSpeech).isShutdown()).isTrue();
  }

  @Test
  public void isStopped_shouldReturnTrueBeforeSpeak() throws Exception {
    assertThat(shadowOf(textToSpeech).isStopped()).isTrue();
  }

  @Test
  public void isStopped_shouldReturnTrueAfterStop() throws Exception {
    textToSpeech.stop();
    assertThat(shadowOf(textToSpeech).isStopped()).isTrue();
  }

  @Test
  public void isStopped_shouldReturnFalseAfterSpeak() throws Exception {
    textToSpeech.speak("Hello", TextToSpeech.QUEUE_FLUSH, null);
    assertThat(shadowOf(textToSpeech).isStopped()).isFalse();
  }

  @Test
  public void getQueueMode_shouldReturnMostRecentQueueMode() throws Exception {
    textToSpeech.speak("Hello", TextToSpeech.QUEUE_ADD, null);
    assertThat(shadowOf(textToSpeech).getQueueMode()).isEqualTo(TextToSpeech.QUEUE_ADD);
  }

  @Test
  public void threeArgumentSpeak_withUtteranceId_shouldGetCallbackUtteranceId() throws Exception {
    textToSpeech.setOnUtteranceProgressListener(mockListener);
    HashMap<String, String> paramsMap = new HashMap<>();
    paramsMap.put(Engine.KEY_PARAM_UTTERANCE_ID, "ThreeArgument");
    textToSpeech.speak("Hello", TextToSpeech.QUEUE_FLUSH, paramsMap);

    shadowMainLooper().idle();

    verify(mockListener).onStart("ThreeArgument");
    verify(mockListener).onDone("ThreeArgument");
  }

  @Test
  public void threeArgumentSpeak_withoutUtteranceId_shouldDoesNotGetCallback() throws Exception {
    textToSpeech.setOnUtteranceProgressListener(mockListener);
    textToSpeech.speak("Hello", TextToSpeech.QUEUE_FLUSH, null);

    shadowMainLooper().idle();

    verify(mockListener, never()).onStart(null);
    verify(mockListener, never()).onDone(null);
  }

  @Test
  @Config(minSdk = LOLLIPOP)
  public void speak_withUtteranceId_shouldReturnSpokenText() throws Exception {
    textToSpeech.speak("Hello", TextToSpeech.QUEUE_FLUSH, null, "TTSEnable");
    assertThat(shadowOf(textToSpeech).getLastSpokenText()).isEqualTo("Hello");
  }

  @Test
  @Config(minSdk = LOLLIPOP)
  public void onUtteranceProgressListener_shouldGetCallbackUtteranceId() throws Exception {
    textToSpeech.setOnUtteranceProgressListener(mockListener);
    textToSpeech.speak("Hello", TextToSpeech.QUEUE_FLUSH, null, "TTSEnable");

    shadowMainLooper().idle();

    verify(mockListener).onStart("TTSEnable");
    verify(mockListener).onDone("TTSEnable");
  }

  @Test
  @Config(minSdk = LOLLIPOP)
  public void synthesizeToFile_lastSynthesizeToFileTextStored() {
    Bundle bundle = new Bundle();
    File file = new File("example.txt");

    int result = textToSpeech.synthesizeToFile("text", bundle, file, "id");

    assertThat(result).isEqualTo(TextToSpeech.SUCCESS);
    assertThat(shadowOf(textToSpeech).getLastSynthesizeToFileText()).isEqualTo("text");
  }

  @Test
  @Config(minSdk = LOLLIPOP)
  public void synthesizeToFile_neverCalled_lastSynthesizeToFileTextNull() {
    assertThat(shadowOf(textToSpeech).getLastSynthesizeToFileText()).isNull();
  }

  @Test
  public void getCurrentLanguage_languageSet_returnsLanguage() {
    Locale language = Locale.forLanguageTag("pl-pl");
    textToSpeech.setLanguage(language);
    assertThat(shadowOf(textToSpeech).getCurrentLanguage()).isEqualTo(language);
  }

  @Test
  public void getCurrentLanguage_languageNeverSet_returnsNull() {
    assertThat(shadowOf(textToSpeech).getCurrentLanguage()).isNull();
  }

  @Test
  public void isLanguageAvailable_neverAdded_returnsUnsupported() {
    assertThat(
            textToSpeech.isLanguageAvailable(
                new Locale.Builder().setLanguage("pl").setRegion("pl").build()))
        .isEqualTo(TextToSpeech.LANG_NOT_SUPPORTED);
  }

  @Test
  public void isLanguageAvailable_twoLanguageAvailabilities_returnsRequestedAvailability() {
    ShadowTextToSpeech.addLanguageAvailability(
        new Locale.Builder().setLanguage("pl").setRegion("pl").build());
    ShadowTextToSpeech.addLanguageAvailability(
        new Locale.Builder().setLanguage("ja").setRegion("jp").build());

    assertThat(
            textToSpeech.isLanguageAvailable(
                new Locale.Builder().setLanguage("pl").setRegion("pl").build()))
        .isEqualTo(TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE);
  }

  @Test
  public void isLanguageAvailable_matchingVariant_returnsCountryVarAvailable() {
    ShadowTextToSpeech.addLanguageAvailability(
        new Locale.Builder().setLanguage("en").setRegion("us").setVariant("WOLTK").build());

    assertThat(
            textToSpeech.isLanguageAvailable(
                new Locale.Builder().setLanguage("en").setRegion("us").setVariant("WOLTK").build()))
        .isEqualTo(TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE);
  }

  @Test
  public void isLanguageAvailable_matchingCountry_returnsLangCountryAvailable() {
    ShadowTextToSpeech.addLanguageAvailability(
        new Locale.Builder().setLanguage("en").setRegion("us").setVariant("ONETW").build());

    assertThat(
            textToSpeech.isLanguageAvailable(
                new Locale.Builder().setLanguage("en").setRegion("us").setVariant("THREE").build()))
        .isEqualTo(TextToSpeech.LANG_COUNTRY_AVAILABLE);
  }

  @Test
  public void isLanguageAvailable_matchingLanguage_returnsLangAvailable() {
    ShadowTextToSpeech.addLanguageAvailability(
        new Locale.Builder().setLanguage("en").setRegion("us").build());

    assertThat(
            textToSpeech.isLanguageAvailable(
                new Locale.Builder().setLanguage("en").setRegion("gb").build()))
        .isEqualTo(TextToSpeech.LANG_AVAILABLE);
  }

  @Test
  public void isLanguageAvailable_matchingNone_returnsLangNotSupported() {
    ShadowTextToSpeech.addLanguageAvailability(
        new Locale.Builder().setLanguage("en").setRegion("us").build());

    assertThat(
            textToSpeech.isLanguageAvailable(
                new Locale.Builder().setLanguage("ja").setRegion("jp").build()))
        .isEqualTo(TextToSpeech.LANG_NOT_SUPPORTED);
  }
}
