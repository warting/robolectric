package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

import com.android.internal.util.VirtualRefBasePtr;
import java.util.HashMap;
import java.util.Map;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(value = VirtualRefBasePtr.class, isInAndroidSdk = false)
public class ShadowVirtualRefBasePtr {
  private static final Map<Long, RefHolder> POINTERS = new HashMap<>();
  private static long nextNativeObj = 10000;

  public static synchronized <T> long put(T object) {
    long nativePtr = nextNativeObj++;
    POINTERS.put(nativePtr, new RefHolder<T>(object));
    return nativePtr;
  }

  public static synchronized <T> T get(long nativePtr, Class<T> clazz) {
    return clazz.cast(POINTERS.get(nativePtr).nativeThing);
  }

  @Implementation(minSdk = LOLLIPOP)
  public static synchronized void nIncStrong(long ptr) {
    if (ptr == 0) return;
    POINTERS.get(ptr).incr();
  }

  @Implementation(minSdk = LOLLIPOP)
  public static synchronized void nDecStrong(long ptr) {
    if (ptr == 0) return;
    if (POINTERS.get(ptr).decr()) {
      POINTERS.remove(ptr);
    }
  }

  private static class RefHolder<T> {
    T nativeThing;
    int refCount;

    public RefHolder(T object) {
      this.nativeThing = object;
    }

    public synchronized void incr() {
      refCount++;
    }

    public synchronized boolean decr() {
      refCount--;
      return refCount == 0;
    }
  }
}
