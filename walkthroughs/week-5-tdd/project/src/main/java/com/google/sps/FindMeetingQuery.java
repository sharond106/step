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

    List<TimeRange> mandatoryBusyTimes = new ArrayList<TimeRange>();
    List<TimeRange> optionalBusyTimes = new ArrayList<TimeRange>();
    List<TimeRange> busyTimes = new ArrayList<TimeRange>();
    
    // Get mandatory attendees' busy times
    if (request.getAttendees().size() > 0) {
      mandatoryBusyTimes = getBusyTimes(events, request.getAttendees());
      busyTimes.addAll(mandatoryBusyTimes);
    }

    // Get optional attendees' busy times
    if (request.getOptionalAttendees().size() > 0) {
      optionalBusyTimes = getBusyTimes(events, request.getOptionalAttendees());
      busyTimes.addAll(optionalBusyTimes);
    }
    
    // Return the whole day if there are no busy times
    if (busyTimes.size() == 0) {
      return Arrays.asList(TimeRange.WHOLE_DAY);
    }

    // If there are busy times, find the open times that can fit the meeting duration
    Collections.sort(busyTimes, TimeRange.ORDER_BY_START);
    busyTimes = mergeOverlappedTimes(busyTimes);
    List<TimeRange> openTimes = getOpenTimes(busyTimes, request.getDuration());
    
    // If there are no open times with the optional + mandatory, find open times for only the mandatory 
    if (openTimes.size() > 0) {
      return openTimes;
    }
    if (request.getAttendees().size() > 0) {
      Collections.sort(mandatoryBusyTimes, TimeRange.ORDER_BY_START);
      return getOpenTimes(mandatoryBusyTimes, request.getDuration());
    }
    // If there are no open times with the optional + mandatory and no mandatory attendees for the event,
    return Arrays.asList();
  }

  // Returns a list of busy times for "requestedAttendees", given other "events"
  private List<TimeRange> getBusyTimes(Collection<Event> events, Collection<String> requestedAttendees) {

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
    return busyTimes;
  }
  
  // Return a new list of "busyTimes" with no overlapping time ranges
  private List<TimeRange> mergeOverlappedTimes(List<TimeRange> busyTimes) {

    if (busyTimes.size() == 0) {
      return new ArrayList<TimeRange>();
    }
    List<TimeRange> busyTimesFinal = new ArrayList<TimeRange>();
    TimeRange thisTime = busyTimes.get(0);
    TimeRange nextTime;
    for (int i = 1; i < busyTimes.size(); i++) {
      nextTime = busyTimes.get(i);

      // If the "thisTime" and "nextTime" overlap, create a new TimeRange that merges them and set "thisTime" to it
      if (thisTime.overlaps(nextTime)) {
        int endTime = Math.max(thisTime.end(), nextTime.end());
        thisTime = TimeRange.fromStartEnd(thisTime.start(), endTime, false);
      } 
      // If they don't overlap, add "thisTime" to the final list and move on to next pair
      else {
        busyTimesFinal.add(thisTime);
        thisTime = nextTime;
      }
    }
    // Need to add the last time range (or if there is only 1 time range, the above for loop will be skipped)
    busyTimesFinal.add(thisTime);
    return busyTimesFinal;
  }

  // Returns a list of open times outside of "busyTimes" that can fit the "duration"
  private List<TimeRange> getOpenTimes(Collection<TimeRange> busyTimes, long duration) {
    
    // Add the open time slots between the busy time slots (that are >= "request.duration") to "openTimes"
    List<TimeRange> openTimes = new ArrayList<TimeRange>();
    int start = TimeRange.START_OF_DAY;
    for (TimeRange time : busyTimes) {

      // Check if the time between the start of the open time slot ("start") to start of next busy event is long enough
      if (time.start() - start >= duration) {
        openTimes.add(TimeRange.fromStartEnd(start, time.start(), false));
      }
      start = time.end();
    }
    // Check the slot between the end of the last busy time and the end of day
    if (TimeRange.END_OF_DAY - start >= duration) {
      openTimes.add(TimeRange.fromStartEnd(start, TimeRange.END_OF_DAY, true));
    }
    return openTimes;
  }
}
