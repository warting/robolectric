package org.robolectric.shadows;

import static org.robolectric.shadow.api.Shadow.directlyOn;

import android.graphics.fonts.Font;
import android.graphics.fonts.FontCustomizationParser.Result;
import android.graphics.fonts.FontFamily;
import android.graphics.fonts.SystemFonts;
import android.os.Build;
import android.text.FontConfig;
import android.text.FontConfig.Alias;
import android.util.ArrayMap;
import java.util.ArrayList;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

@Implements(
    className = "android.graphics.fonts.SystemFonts",
    minSdk = Build.VERSION_CODES.Q,
    isInAndroidSdk = false)
public class ShadowSystemFonts {

  @Implementation
  protected static FontConfig.Alias[] buildSystemFallback(
      String xmlPath,
      String fontDir,
      Result oemCustomization,
      ArrayMap<String, android.graphics.fonts.FontFamily[]> fallbackMap,
      ArrayList<Font> availableFonts) {
    return new Alias[] {new FontConfig.Alias("sans-serif", "sans-serif", 0)};
  }

  @Implementation
  protected static FontFamily[] getSystemFallback(String familyName) {
    FontFamily[] result =
        directlyOn(
            SystemFonts.class, "getSystemFallback", ClassParameter.from(String.class, familyName));
    if (result == null) {
      result = new FontFamily[0];
    }
    return result;
  }
}
