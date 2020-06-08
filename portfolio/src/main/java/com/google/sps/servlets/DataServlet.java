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

/** Servlet responsible for handling comments data */
@WebServlet("/comment")
public class DataServlet extends HttpServlet {

  private enum Sort { NEW, OLD, NULL }
  
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Sort sort = Sort.valueOf(request.getParameter("sort").toUpperCase());
    String imgRequest = request.getParameter("img");

    // Sort comments according to request parameter
    Query query;
    if (sort == Sort.NULL || sort == Sort.NEW) {
      query = new Query("Comment").addSort("timestamp", SortDirection.DESCENDING);
    } else {
      query = new Query("Comment").addSort("timestamp", SortDirection.ASCENDING);
    }

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    int numComments = results.countEntities(FetchOptions.Builder.withDefaults());

    // Set the number of comments to print (max)
    String quantity = request.getParameter("quantity");
    int max = -1;
    if (quantity != null && quantity.length() > 0 && Integer.parseInt(quantity) <= numComments) {
      max = Integer.parseInt(quantity);
    } else {
      max = numComments;
    }

    // Comment objects added to comments ArrayList will be displayed
    ArrayList<Comment> comments = new ArrayList<Comment>();
    // count variable is the total number of comments linked with the current img
    int count = 0;
    // Iterating over all comments with various img tags
    for (Entity entity : results.asIterable()) {
      String img = (String) entity.getProperty("img");

      // Only add the comment if it's linked with the current img
      if (img.equals(imgRequest)) {
        long id = entity.getKey().getId();  
        String name = (String) entity.getProperty("name");
        String comment = (String) entity.getProperty("comment");
        long timestamp = (long) entity.getProperty("timestamp");
        
        Comment newComment = new Comment(id, name, comment, timestamp, img);
        count++;
        /* Don't add to comments to display list if the number of comments linked with the current img (count) 
        is > the max number of comments the user wants to show (max). Keep iterating through the rest of the results, 
        in case there are more comments linked with the current img tag, to correctly count the total (count).
        */
        if (count <= max) {
            comments.add(newComment);
        }
      }
    }
    
    // Create json String with a "comments" object and a "total" object to print to /comments
    Gson gson = new Gson();
    String json = String.format("{\"comments\": %s, \"total\": %s}", gson.toJson(comments), count);

    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String comment = request.getParameter("comment");
    String name = request.getParameter("name");
    String img = request.getParameter("img");
    long timestamp = System.currentTimeMillis();

    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("comment", comment);
    commentEntity.setProperty("name", name);
    commentEntity.setProperty("img", img);
    commentEntity.setProperty("timestamp", timestamp);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(commentEntity);
    
    response.sendRedirect("/pics.html");
  }
}
