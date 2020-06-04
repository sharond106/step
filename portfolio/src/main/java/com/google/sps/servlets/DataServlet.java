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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.FetchOptions.Builder;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.gson.Gson;
import com.google.sps.data.Comment;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList; 

// Servlet responsible for handling comments data 
@WebServlet("/comment")
public class DataServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String sort = request.getParameter("sort");
    String imgRequest = request.getParameter("img");

    // Sort comments according to request parameter
    Query query = new Query("Comment").addSort("timestamp", SortDirection.DESCENDING);
    if (sort != null && sort.equals("old")) {
      query = new Query("Comment").addSort("timestamp", SortDirection.ASCENDING);
    }

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    int numComments = results.countEntities(FetchOptions.Builder.withDefaults());

    // Set the number of comments to print (max)
    String quantity = request.getParameter("quantity");
    int max = -1;
    if (quantity != null && quantity.length() > 0) {
      max = Integer.parseInt(quantity);
    } else {
      max = numComments;
    }

    // Create Comment objects and add to comments ArrayList
    ArrayList<Comment> comments = new ArrayList<Comment>();
    int count = 0;
    for (Entity entity : results.asIterable()) {
      long id = entity.getKey().getId();  
      String name = (String) entity.getProperty("name");
      String comment = (String) entity.getProperty("comment");
      long timestamp = (long) entity.getProperty("timestamp");
      String img = (String) entity.getProperty("img");

      // Only add the comment if it's linked with the current img
      if (img.equals(imgRequest)) {
        Comment newComment = new Comment(id, name, comment, timestamp, img);
        count++;
        if (count <= max) {
            comments.add(newComment);
        }
      }
    }
    
    // Create json String to print to /comments
    Gson gson = new Gson();
    String json = String.format("{\"comments\": %s, \"total\": %s}", gson.toJson(comments), count);

    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String comment = request.getParameter("comment");
    String name = getParameter(request, "name", "");
    String img = getParameter(request, "img", "");
    long timestamp = System.currentTimeMillis();

    if (comment == null || comment.length() == 0) {
      response.sendRedirect("/pics.html");
      return;
    }

    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("comment", comment);
    commentEntity.setProperty("name", name);
    commentEntity.setProperty("img", img);
    commentEntity.setProperty("timestamp", timestamp);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(commentEntity);
    
    response.sendRedirect("/pics.html");
  }

  private String getParameter(HttpServletRequest request, String name, String defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }
}
