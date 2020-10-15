package org.robolectric.shadows.support.v4;

import static org.junit.Assert.assertTrue;

import android.database.DataSetObserver;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.support.v4.view.PagerAdapter;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ShadowPagerAdapterTest {
  @Test
  public void shouldNotifyDataSetChanged() throws Exception {
    NullPagerAdapter pagerAdapter = new NullPagerAdapter();
    TestDataSetObserver datasetObserver = new TestDataSetObserver();
    Method method =
        PagerAdapter.class.getDeclaredMethod("registerDataSetObserver", DataSetObserver.class);
    method.setAccessible(true);
    method.invoke(pagerAdapter, datasetObserver);
    pagerAdapter.notifyDataSetChanged();

    assertTrue(datasetObserver.onChangedWasCalled);
  }

  private static class NullPagerAdapter extends PagerAdapter {
    @Override
    public int getCount() {
      return 0;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
      return false;
    }
  }

  private static class TestDataSetObserver extends DataSetObserver {
    boolean onChangedWasCalled;

    @Override
    public void onChanged() {
      onChangedWasCalled = true;
    }
  }
}
