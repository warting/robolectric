package org.robolectric.junit.rules;

import static org.hamcrest.CoreMatchers.equalTo;

import android.util.Log;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.hamcrest.Matcher;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLog.LogItem;

/**
 * Allows tests to assert about the presence of log messages, and turns logged errors that are not
 * explicitly expected into test failures.
 */
public final class ExpectedLogMessagesRule implements TestRule {
  /** Tags that apps can't prevent. We whitelist them globally. */
  private static final ImmutableSet<String> UNPREVENTABLE_TAGS =
      ImmutableSet.of("Typeface", "RingtoneManager");

  private final Set<ExpectedLogItem> expectedLogs = new HashSet<>();
  private final Set<LogItem> observedLogs = new HashSet<>();
  private final Set<LogItem> unexpectedErrorLogs = new HashSet<>();
  private final Set<String> expectedTags = new HashSet<>();
  private final Set<String> observedTags = new HashSet<>();

  private boolean shouldIgnoreMissingLoggedTags = false;

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        base.evaluate();
        List<LogItem> logs = ShadowLog.getLogs();
        Map<ExpectedLogItem, Boolean> expectedLogItemMap = new HashMap<>();
        for (ExpectedLogItem item : expectedLogs) {
          expectedLogItemMap.put(item, false);
        }
        for (LogItem log : logs) {
          LogItem logItem = new LogItem(log.type, log.tag, log.msg, log.throwable);
          if (updateExpected(logItem, expectedLogItemMap)) {
            observedLogs.add(logItem);
            continue;
          }
          if (log.type >= Log.ERROR) {
            if (UNPREVENTABLE_TAGS.contains(log.tag)) {
              continue;
            }
            if (expectedTags.contains(log.tag)) {
              observedTags.add(log.tag);
              continue;
            }
            unexpectedErrorLogs.add(log);
          }
        }
        if (!unexpectedErrorLogs.isEmpty() || expectedLogItemMap.containsValue(false)) {
          Set<ExpectedLogItem> unobservedLogs = new HashSet<>();
          for (Map.Entry<ExpectedLogItem, Boolean> entry : expectedLogItemMap.entrySet()) {
            if (!entry.getValue()) {
              unobservedLogs.add(entry.getKey());
            }
          }
          throw new AssertionError(
              "Expected and observed logs did not match."
                  + "\nExpected:                   "
                  + expectedLogs
                  + "\nExpected, and observed:     "
                  + observedLogs
                  + "\nExpected, but not observed: "
                  + unobservedLogs
                  + "\nObserved, but not expected: "
                  + unexpectedErrorLogs);
        }
        if (!expectedTags.equals(observedTags) && !shouldIgnoreMissingLoggedTags) {
          throw new AssertionError(
              "Expected and observed tags did not match. "
                  + "Expected tags should not be used to suppress errors, only expect them."
                  + "\nExpected:                   "
                  + expectedTags
                  + "\nExpected, and observed:     "
                  + observedTags
                  + "\nExpected, but not observed: "
                  + Sets.difference(expectedTags, observedTags));
        }
      }
    };
  }

  /**
   * Adds an expected log statement. If this log is not printed during test execution, the test case
   * will fail. This will also match any log statement which contain a throwable as well. For
   * verifying the throwable, please see {@link #expectLogMessageWithThrowable(int, String, String,
   * Throwable)}. Do not use this to suppress failures. Use this to test that expected error cases
   * in your code cause log messages to be printed.
   */
  public void expectLogMessage(int level, String tag, String message) {
    expectLogMessageInternal(tag, new ExpectedLogItem(level, tag, message));
  }

  /**
   * Adds an expected log statement with extra check of {@link Throwable}. If this log is not
   * printed during test execution, the test case will fail. Do not use this to suppress failures.
   * Use this to test that expected error cases in your code cause log messages to be printed.
   */
  public void expectLogMessageWithThrowable(
      int level, String tag, String message, Throwable throwable) {
    expectLogMessageInternal(tag, new ExpectedLogItem(level, tag, message, equalTo(throwable)));
  }

  /**
   * Adds an expected log statement with extra check of {@link Matcher}. If this log is not printed
   * during test execution, the test case will fail. Do not use this to suppress failures. Use this
   * to test that expected error cases in your code cause log messages to be printed.
   */
  public void expectLogMessageWithThrowableMatcher(
      int level, String tag, String message, Matcher<Throwable> throwableMatcher) {
    expectLogMessageInternal(tag, new ExpectedLogItem(level, tag, message, throwableMatcher));
  }

  /**
   * Blanket suppress test failures due to errors from a tag. If this tag is not printed at
   * Log.ERROR during test execution, the test case will fail (unless {@link
   * #ignoreMissingLoggedTags(boolean)} is used).
   *
   * <p>Avoid using this method when possible. Prefer to assert on the presence of a specific
   * message using {@link #expectLogMessage} in test cases that *intentionally* trigger an error.
   */
  public void expectErrorsForTag(String tag) {
    checkTag(tag);
    if (UNPREVENTABLE_TAGS.contains(tag)) {
      throw new AssertionError("Tag `" + tag + "` is already suppressed.");
    }
    expectedTags.add(tag);
  }

  /**
   * If set true, tests that call {@link #expectErrorsForTag(String)} but do not log errors for the
   * given tag will not fail. By default this is false.
   *
   * <p>Avoid using this method when possible. Prefer tests that print (or do not print) log
   * messages deterministically.
   */
  public void ignoreMissingLoggedTags(boolean shouldIgnore) {
    shouldIgnoreMissingLoggedTags = shouldIgnore;
  }

  private void expectLogMessageInternal(String tag, ExpectedLogItem logItem) {
    checkTag(tag);
    expectedLogs.add(logItem);
  }

  private void checkTag(String tag) {
    if (tag.length() > 23) {
      throw new IllegalArgumentException("Tag length cannot exceed 23 characters: " + tag);
    }
  }

  private static boolean updateExpected(
      LogItem logItem, Map<ExpectedLogItem, Boolean> expectedLogItemMap) {
    for (ExpectedLogItem expectedLogItem : expectedLogItemMap.keySet()) {
      if (expectedLogItem.type == logItem.type
          && equals(expectedLogItem.tag, logItem.tag)
          && equals(expectedLogItem.msg, logItem.msg)
          && matchThrowable(expectedLogItem, logItem.throwable)) {
        expectedLogItemMap.put(expectedLogItem, true);
        return true;
      }
    }

    return false;
  }

  private static boolean equals(String a, String b) {
    return a == null ? b == null : a.equals(b);
  }

  private static boolean matchThrowable(ExpectedLogItem logItem, Throwable throwable) {
    if (logItem.throwableMatcher != null) {
      return logItem.throwableMatcher.matches(throwable);
    }

    // Return true in case no throwable / throwable-matcher were specified.
    return true;
  }

  private static class ExpectedLogItem {
    final int type;
    final String tag;
    final String msg;
    private Matcher<Throwable> throwableMatcher = null;

    ExpectedLogItem(int type, String tag, String msg) {
      this.type = type;
      this.tag = tag;
      this.msg = msg;
    }

    ExpectedLogItem(int type, String tag, String msg, Matcher<Throwable> throwableMatcher) {
      this(type, tag, msg);
      this.throwableMatcher = throwableMatcher;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (!(o instanceof ExpectedLogItem)) {
        return false;
      }

      ExpectedLogItem log = (ExpectedLogItem) o;
      return type == log.type
          && !(msg != null ? !msg.equals(log.msg) : log.msg != null)
          && !(tag != null ? !tag.equals(log.tag) : log.tag != null)
          && !(throwableMatcher != null
              ? !throwableMatcher.equals(log.throwableMatcher)
              : log.throwableMatcher != null);
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, tag, msg, throwableMatcher);
    }

    @Override
    public String toString() {
      String throwableStr = (throwableMatcher == null) ? "" : (", throwable=" + throwableMatcher);
      return "ExpectedLogItem{"
          + "timeString='"
          + null
          + '\''
          + ", type="
          + type
          + ", tag='"
          + tag
          + '\''
          + ", msg='"
          + msg
          + '\''
          + throwableStr
          + '}';
    }
  }
}
