package org.totschnig.myexpenses.test.espresso;

import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.testutils.BaseUiTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;


@RunWith(AndroidJUnit4.class)
public final class MyExpensesIntentTest extends BaseUiTest {

  @Rule
  public ActivityTestRule<MyExpenses> mActivityRule =
      new ActivityTestRule<>(MyExpenses.class, false, false);
  private static String accountLabel1;
  private static String accountLabel2;
  private static Account account1;
  private static Account account2;

  @BeforeClass
  public static void fixture() {
    accountLabel1 = "Test label 1";
    accountLabel2 = "Test label 2";
    account1 = new Account(accountLabel1, 0, "");
    account1.save();
    account2 = new Account(accountLabel2, 0, "");
    account2.save();
  }

  @AfterClass
  public static void tearDown() throws RemoteException, OperationApplicationException {
    Account.delete(account1.getId());
    Account.delete(account2.getId());
  }

  @Test
  public void shouldNavigateToAccountReceivedThroughIntent1() {
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), MyExpenses.class)
        .putExtra(KEY_ROWID, account1.getId());
    mActivityRule.launchActivity(i);
    waitForAdapter();
    onView(allOf(
        withText(accountLabel1),
        withId(R.id.action_bar_title)))
        .check(matches(isDisplayed()));
  }

  @Test
  public void shouldNavigateToAccountReceivedThroughIntent2() {
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), MyExpenses.class)
        .putExtra(KEY_ROWID, account2.getId());
    mActivityRule.launchActivity(i);
    waitForAdapter();
    onView(allOf(
        withText(accountLabel2),
        withId(R.id.action_bar_title)))
        .check(matches(isDisplayed()));
    mActivityRule.getActivity().finish();
  }

  @Override
  protected ActivityTestRule<? extends ProtectedFragmentActivity> getTestRule() {
    return mActivityRule;
  }
}
