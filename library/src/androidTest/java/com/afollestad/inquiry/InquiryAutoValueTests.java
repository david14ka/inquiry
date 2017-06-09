package com.afollestad.inquiry;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class InquiryAutoValueTests {

  private static final String INSTANCE_NAME = "test";

  @SuppressWarnings("CheckResult")
  @Before
  public void setup() {
    Context appContext = InstrumentationRegistry.getTargetContext();
    Inquiry inq =
        Inquiry.newInstance(appContext, "instrument_test").instanceName(INSTANCE_NAME).build();
    inq.delete(AutoValueRow.class).run();
  }

  @Test
  public void test_insert_and_query() throws Exception {
    AutoValueRow[] rows =
        new AutoValueRow[] {
          AutoValueRow.create().id(0).username("afollestad").age(21).online(true).rank(100f).build()
        };

    Long[] insertedIds = Inquiry.get(INSTANCE_NAME).insert(AutoValueRow.class).values(rows).run();
    assertEquals(insertedIds.length, 1);
    assertEquals(rows[0].id(), (long) insertedIds[0]);

    AutoValueRow query1 = Inquiry.get(INSTANCE_NAME).select(AutoValueRow.class).first();
    assertNotNull(query1);
    assertEquals(query1, rows[0]);
  }

  @After
  public void cleanup() {
    Inquiry.destroy(INSTANCE_NAME);
  }
}
