// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import com.google.sps.TimeRange;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {

    // If request duration is longer than a day
    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()) {
      return Arrays.asList();
    }

    // If no one has other events
    if (events.size() == 0) {
      return Arrays.asList(TimeRange.WHOLE_DAY);
    }

    Collection<TimeRange> mandatoryBusyTimes = getBusyTimes(events, request, false);
    Collection<TimeRange> mandatoryOpenTimes = getOpenTimes(mandatoryBusyTimes, request.getDuration());
    
    Collection<TimeRange> optionalBusyTimes = getBusyTimes(events, request, true);

    return mandatoryOpenTimes;
  }

  // Returns a list of busy times for "request", given other "events"
  // Param "optionalAttendees" is true to find times for optional attendees and false to find times for mandatory attendees
  private Collection<TimeRange> getBusyTimes(Collection<Event> events, MeetingRequest request, boolean optionalAttendees) {
    Collection<String> requestedAttendees;

    if (optionalAttendees) {
      requestedAttendees = request.getOptionalAttendees();
    } else {
      requestedAttendees = request.getAttendees();
    }

    // Find events including requested attendees and add those time ranges to "busyTimes"
    List<TimeRange> busyTimes = new ArrayList<TimeRange>();
    for (Event event: events) {
      Set<String> attendees = event.getAttendees();

      // Check if "event" includes a requested attendee
      for (String requestedAttendee : requestedAttendees) {
        if (attendees.contains(requestedAttendee)) {
          busyTimes.add(event.getWhen());
          break;
        }
      }
    }
    if (busyTimes.size() == 0) {
      return Arrays.asList(TimeRange.WHOLE_DAY);
    }
    Collections.sort(busyTimes, TimeRange.ORDER_BY_START);

    // Create new list of times with no overlapping times
    List<TimeRange> busyTimesFinal = new ArrayList<TimeRange>();
    TimeRange thisTime = busyTimes.get(0);
    TimeRange nextTime;
    for (int i = 1; i < busyTimes.size(); i++) {
      nextTime = busyTimes.get(i);

      // If the times overlap, create new merged TimeRange and set "thisTime" to it
      if (thisTime.overlaps(nextTime)) {
        int endTime = Math.max(thisTime.end(), nextTime.end());
        thisTime = TimeRange.fromStartEnd(thisTime.start(), endTime, false);
      } 
      else {
        busyTimesFinal.add(thisTime);
        thisTime = nextTime;
      }
    }
    // Need to add the last time range (or if there is only 1 time range, the above for loop will be skipped)
    busyTimesFinal.add(thisTime);

    return busyTimesFinal;
  }

  private Collection<TimeRange> getOpenTimes(Collection<TimeRange> busyTimes, long duration) {
    // Add the open time slots between the busy time slots (that are >= "request.duration") to "openTimes"
    List<TimeRange> openTimes = new ArrayList<TimeRange>();
    int start = TimeRange.START_OF_DAY;
    for (TimeRange time : busyTimes) {
      if (time.start() - start >= duration) {
        openTimes.add(TimeRange.fromStartEnd(start, time.start(), false));
      }
      start = time.end();
    }
    // Check the slot between the last busy time and the end of day
    if (TimeRange.END_OF_DAY - start >= duration) {
      openTimes.add(TimeRange.fromStartEnd(start, TimeRange.END_OF_DAY, true));
    }

    return openTimes;
  }
}
