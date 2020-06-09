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

package com.google.sps.servlets;

import com.google.sps.data.Location;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet responsible for retreiving map location data from csv and returning it as a JSON*/
@WebServlet("/location")
public class LocationServlet extends HttpServlet {

  private Collection<Location> locations;

  @Override
  public void init() {
    locations = new ArrayList<>();

    // Parse csv file organized by name, description, latitude, longitude
    Scanner locationData = new Scanner(getServletContext().getResourceAsStream("/WEB-INF/locations.csv"));
    while (locationData.hasNextLine()) {
      String line = locationData.nextLine();
      String[] cells = line.split(",");
      
      // Skip this line if there are an incorrect number of values
      if (cells.length != 4) {
        continue;
      }

      String name = cells[0];
      String description = cells[1];
    
      Double lat = parseForDouble(cells[2]);
      Double lng = parseForDouble(cells[3]);
      
      // Skip this line if latitude / longitude are invalid
      if (lat == null || lng == null) {
        continue;
      }
      locations.add(new Location(name, description, lat, lng));
    }
    locationData.close();
  }

  // Returns parameter as a double
  public Double parseForDouble(String value) {
    try {
      Double num = Double.parseDouble(value);
      return num;
    } catch (NumberFormatException e) { 
      System.out.println("NumberFormatException"); 
      return null;
    } catch (Exception e) { 
      System.out.println("Exception: " + e); 
      return null;
    }
  }
  
  // Print locations ArrayList as a json object to /locations address 
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    Gson gson = new Gson();
    String json = gson.toJson(locations);
    response.getWriter().println(json);
  }
}