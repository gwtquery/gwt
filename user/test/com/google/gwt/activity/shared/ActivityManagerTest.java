/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.activity.shared;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.UmbrellaException;
import com.google.gwt.event.shared.testing.CountingEventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceChangeRequestEvent;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

import junit.framework.TestCase;

/**
 * Eponymous unit test.
 */
public class ActivityManagerTest extends TestCase {
  private static class AsyncActivity extends SyncActivity {
    AsyncActivity(MyView view) {
      super(view);
    }

    @Override
    public void start(AcceptsOneWidget display, EventBus eventBus) {
      this.display = display;
    }

    void finish() {
      display.setWidget(view);
    }
  }

  private static class Event extends GwtEvent<Handler> {
    private static GwtEvent.Type<EventHandler> TYPE = new GwtEvent.Type<EventHandler>();

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType() {
      throw new UnsupportedOperationException("Auto-generated method stub");
    }

    @Override
    protected void dispatch(Handler handler) {
      throw new UnsupportedOperationException("Auto-generated method stub");
    }
  }

  private static class Handler implements EventHandler {
  };

  private static class MyDisplay implements AcceptsOneWidget {
    IsWidget widget = null;

    public void setWidget(IsWidget widget) {
      this.widget = widget;
    }
  }

  private static class MyPlace extends Place {
  }

  private static class MyView implements IsWidget {
    public Widget asWidget() {
      return null;
    }
  }
  private static class SyncActivity implements Activity {
    boolean canceled = false;
    boolean stopped = false;
    AcceptsOneWidget display;
    String stopWarning;
    MyView view;
    EventBus bus;

    SyncActivity(MyView view) {
      this.view = view;
    }

    public String mayStop() {
      return stopWarning;
    }

    public void onCancel() {
      canceled = true;
    }

    public void onStop() {
      stopped = true;
    }

    public void start(AcceptsOneWidget display, EventBus eventBus) {
      this.display = display;
      this.bus = eventBus;
      display.setWidget(view);
    }
  }

  private final MyPlace place1 = new MyPlace();
  private final MyPlace place2 = new MyPlace();

  private SyncActivity activity1 = new SyncActivity(new MyView());

  private SyncActivity activity2 = new SyncActivity(new MyView());

  private final MyDisplay realDisplay = new MyDisplay();
  private final ActivityMapper myMap = new ActivityMapper() {
    public Activity getActivity(Place place) {
      if (place.equals(place1)) {
        return activity1;
      }
      if (place.equals(place2)) {
        return activity2;
      }

      return null;
    }
  };

  private CountingEventBus eventBus = new CountingEventBus();

  private ActivityManager manager = new ActivityManager(
      myMap, eventBus);

  public void testAsyncDispatch() {
    final AsyncActivity asyncActivity1 = new AsyncActivity(new MyView());
    final AsyncActivity asyncActivity2 = new AsyncActivity(new MyView());

    ActivityMapper map = new ActivityMapper() {
      public Activity getActivity(Place place) {
        if (place.equals(place1)) {
          return asyncActivity1;
        }
        if (place.equals(place2)) {
          return asyncActivity2;
        }

        return null;
      }
    };

    manager = new ActivityManager(map, eventBus);
    manager.setDisplay(realDisplay);

    PlaceChangeRequestEvent event = new PlaceChangeRequestEvent(
        place1);
    eventBus.fireEvent(event);
    assertNull(event.getWarning());
    assertNull(realDisplay.widget);
    assertFalse(asyncActivity1.stopped);
    assertFalse(asyncActivity1.canceled);
    assertNull(asyncActivity1.display);

    eventBus.fireEvent(new PlaceChangeEvent(place1));
    assertNull(realDisplay.widget);
    assertFalse(asyncActivity1.stopped);
    assertFalse(asyncActivity1.canceled);
    assertNotNull(asyncActivity1.display);

    asyncActivity1.finish();
    assertEquals(asyncActivity1.view, realDisplay.widget);
    assertFalse(asyncActivity1.stopped);
    assertFalse(asyncActivity1.canceled);

    event = new PlaceChangeRequestEvent(place2);
    eventBus.fireEvent(event);
    assertNull(event.getWarning());
    assertEquals(asyncActivity1.view, realDisplay.widget);
    assertFalse(asyncActivity1.stopped);
    assertFalse(asyncActivity1.canceled);
    assertFalse(asyncActivity2.stopped);
    assertFalse(asyncActivity2.canceled);
    assertNull(asyncActivity2.display);

    eventBus.fireEvent(new PlaceChangeEvent(place2));
    /*
     * TODO until caching is in place, relying on stopped activities to be good
     * citizens to reduce flicker. This makes me very nervous.
     */
    // assertNull(realDisplay.widget);
    assertFalse(asyncActivity1.canceled);
    assertTrue(asyncActivity1.stopped);
    assertFalse(asyncActivity2.stopped);
    assertFalse(asyncActivity2.canceled);
    assertNotNull(asyncActivity2.display);

    asyncActivity2.finish();
    assertEquals(asyncActivity2.view, realDisplay.widget);
  }

  public void testCancel() {
    final AsyncActivity asyncActivity1 = new AsyncActivity(new MyView());
    final AsyncActivity ayncActivity2 = new AsyncActivity(new MyView());

    ActivityMapper map = new ActivityMapper() {
      public Activity getActivity(Place place) {
        if (place.equals(place1)) {
          return asyncActivity1;
        }
        if (place.equals(place2)) {
          return ayncActivity2;
        }

        return null;
      }
    };

    manager = new ActivityManager(map, eventBus);
    manager.setDisplay(realDisplay);

    PlaceChangeRequestEvent event = new PlaceChangeRequestEvent(
        place1);
    eventBus.fireEvent(event);
    assertNull(event.getWarning());
    assertNull(realDisplay.widget);
    assertFalse(asyncActivity1.stopped);
    assertFalse(asyncActivity1.canceled);
    assertNull(asyncActivity1.display);

    eventBus.fireEvent(new PlaceChangeEvent(place1));
    assertNull(realDisplay.widget);
    assertFalse(asyncActivity1.stopped);
    assertFalse(asyncActivity1.canceled);
    assertNotNull(asyncActivity1.display);

    event = new PlaceChangeRequestEvent(place2);
    eventBus.fireEvent(event);
    assertNull(event.getWarning());
    assertNull(realDisplay.widget);
    assertFalse(asyncActivity1.stopped);
    assertFalse(asyncActivity1.canceled);

    eventBus.fireEvent(new PlaceChangeEvent(place2));
    assertNull(realDisplay.widget);
    assertTrue(asyncActivity1.canceled);
    assertFalse(asyncActivity1.stopped);
    assertFalse(ayncActivity2.stopped);
    assertFalse(ayncActivity2.canceled);
    assertNotNull(ayncActivity2.display);

    ayncActivity2.finish();
    assertEquals(ayncActivity2.view, realDisplay.widget);

    asyncActivity1.finish();
    assertEquals(ayncActivity2.view, realDisplay.widget);
  }

  public void testDropHandlersOnStop() {
    manager.setDisplay(realDisplay);

    assertEquals(0, eventBus.getCount(Event.TYPE));

    activity1 = new SyncActivity(null) {
      @Override
      public void start(AcceptsOneWidget panel, EventBus eventBus) {
        super.start(panel, eventBus);
        bus.addHandler(Event.TYPE, new Handler());
      }

      @Override
      public void onStop() {
        super.onStop();
        bus.addHandler(Event.TYPE, new Handler());
      }
    };

    PlaceChangeEvent event = new PlaceChangeEvent(place1);
    eventBus.fireEvent(event);
    assertEquals(1, eventBus.getCount(Event.TYPE));

    event = new PlaceChangeEvent(place2);
    eventBus.fireEvent(event);
    assertEquals(0, eventBus.getCount(Event.TYPE));

    // Make sure we didn't nuke the ActivityManager's own handlers
    assertEquals(1, eventBus.getCount(PlaceChangeEvent.TYPE));
    assertEquals(1, eventBus.getCount(PlaceChangeRequestEvent.TYPE));
  }

  public void testEventSetupAndTeardown() {
    assertEquals(0, eventBus.getCount(PlaceChangeEvent.TYPE));
    assertEquals(0, eventBus.getCount(PlaceChangeRequestEvent.TYPE));

    manager.setDisplay(realDisplay);

    assertEquals(1, eventBus.getCount(PlaceChangeEvent.TYPE));
    assertEquals(1, eventBus.getCount(PlaceChangeRequestEvent.TYPE));

    manager.setDisplay(null);

    assertEquals(0, eventBus.getCount(PlaceChangeEvent.TYPE));
    assertEquals(0, eventBus.getCount(PlaceChangeRequestEvent.TYPE));
  }

  public void testExceptionsOnStopAndStart() {
    activity1 = new SyncActivity(null) {
      @Override
      public void start(AcceptsOneWidget panel, EventBus eventBus) {
        super.start(panel, eventBus);
        bus.addHandler(Event.TYPE, new Handler());
      }
      @Override
      public void onStop() {
        super.onStop();
        bus.addHandler(Event.TYPE, new Handler());
        throw new UnsupportedOperationException("Auto-generated method stub");
      }
    };

    activity2 = new SyncActivity(null) {
      @Override
      public void start(AcceptsOneWidget panel, EventBus eventBus) {
        super.start(panel, eventBus);
        throw new UnsupportedOperationException("Auto-generated method stub");
      }
    };

    manager.setDisplay(realDisplay);

    try {
      PlaceChangeEvent event = new PlaceChangeEvent(place1);
      eventBus.fireEvent(event);
      assertEquals(1, eventBus.getCount(Event.TYPE));

      event = new PlaceChangeEvent(place2);
      eventBus.fireEvent(event);

      fail("Expected exception");
    } catch (UmbrellaException e) {
      // HandlerManager throws this one
      assertEquals(1, e.getCauses().size());

      UmbrellaException nested = (UmbrellaException) e.getCause();
      assertEquals(2, nested.getCauses().size());
    }

    assertTrue(activity1.stopped);
    assertNotNull(activity2.display);
    assertEquals(0, eventBus.getCount(Event.TYPE));
  }

  public void testRejected() {
    manager.setDisplay(realDisplay);

    activity1.stopWarning = "Stop fool!";

    PlaceChangeRequestEvent event = new PlaceChangeRequestEvent(
        place1);
    eventBus.fireEvent(event);
    assertNull(event.getWarning());
    assertNull(realDisplay.widget);

    eventBus.fireEvent(new PlaceChangeEvent(place1));
    assertEquals(activity1.view, realDisplay.widget);

    event = new PlaceChangeRequestEvent(place2);
    eventBus.fireEvent(event);
    assertEquals(activity1.stopWarning, event.getWarning());
    assertEquals(activity1.view, realDisplay.widget);
    assertFalse(activity1.stopped);
    assertFalse(activity1.canceled);
  }

  public void testSyncDispatch() {
    manager.setDisplay(realDisplay);

    PlaceChangeRequestEvent event = new PlaceChangeRequestEvent(
        place1);
    eventBus.fireEvent(event);
    assertNull(event.getWarning());
    assertNull(realDisplay.widget);
    assertFalse(activity1.stopped);
    assertFalse(activity1.canceled);

    eventBus.fireEvent(new PlaceChangeEvent(place1));
    assertEquals(activity1.view, realDisplay.widget);
    assertFalse(activity1.stopped);
    assertFalse(activity1.canceled);

    event = new PlaceChangeRequestEvent(place2);
    eventBus.fireEvent(event);
    assertNull(event.getWarning());
    assertEquals(activity1.view, realDisplay.widget);
    assertFalse(activity1.stopped);
    assertFalse(activity1.canceled);

    eventBus.fireEvent(new PlaceChangeEvent(place2));
    assertEquals(activity2.view, realDisplay.widget);
    assertTrue(activity1.stopped);
    assertFalse(activity1.canceled);
  }
}