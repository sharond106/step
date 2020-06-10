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

// Scroll to the top of the document
function topFunction() {
  document.documentElement.scrollTop = 0; 
}

// When the user scrolls down 20px from the top of the document, show the button
window.onscroll = function() {scrollFunction()};

function scrollFunction() {
  const button = document.getElementById("to-top-btn");
  if (window.pageYOffset > 20) {
    button.style.display = "block";
  } else {
    button.style.display = "none";
  }
}

// When the user clicks a photo, show image in modal
function showModal(img) {
  const modal = document.getElementById("my-modal");
  const modalImg = document.getElementById("modal-img");
  const modalCaption = document.getElementById("caption");
  modal.style.display = "block";
  modalImg.src = img.src;
  modalCaption.innerHTML = img.alt;
  getServerData();
}

function closeModal() {
  const modal = document.getElementById("my-modal");
  modal.style.display = "none";
  const maxElement = document.getElementById("quantity");
  maxElement.value = "";
}

// Create GET request to /comment servlet
function getServerData() {
  console.log("Fetching data.");

  // Get user's values from the html document
  const maxElement = document.getElementById("quantity");
  const maxToDisplay = maxElement.value;
  const sort = document.getElementById("sort").value;
  const imgsrc = document.getElementById("modal-img").src;
  
  // Don't get comments if the image has no value
  if (!imgsrc) {
    console.log("null image src");
    return;
  }
  const img = getImgName(imgsrc);
  if (img == "") {
    console.log("invalid img src");
    return;
  }

  fetch("/comment?quantity=" + maxToDisplay + "&sort=" + sort + "&img=" + img).then(response => response.json()).then(jsonObj => {
    const comments = jsonObj.comments;
    const total = jsonObj.total;

    // Display the total number of comments to user
    const totalElement = document.getElementById("total");
    totalElement.innerText = ("Comments (" + total + ")");

    // Make sure user can't try to display more than the total number of comments
    maxElement.max = total.toString();
    maxElement.placeholder = total.toString();

    // Add individual comments to html page
    const list = document.getElementById("all-comments");
    list.innerHTML = "";
    comments.forEach((comment) => {
      const listElement = document.createElement("li");
      
      // Display name, date, and comment
      const textElement = document.createElement("span");
      const date = new Date(comment.timestamp);
      textElement.innerHTML = "<b>" + comment.name + "</b>    <small>" + date.toLocaleString() + "</small><br><br>" + comment.comment;

      // Display a delete button
      const deleteButtonElement = document.createElement("button");
      deleteButtonElement.className = "delete-button";
      deleteButtonElement.innerText = "Delete";
      deleteButtonElement.addEventListener("click", () => {
        console.log("Delete button clicked");
        deleteComment(comment);
      });
      
      listElement.appendChild(textElement);
      listElement.appendChild(deleteButtonElement);
      list.append(listElement);
    });
  });
}

// Create POST request to /delete servlet to delete comment, called by delete button onclick
function deleteComment(comment) {
  console.log("Deleting comment");

  const params = new URLSearchParams();
  params.append("id", comment.id);
  const postRequest = new Request("/delete", {method: "POST", body: params});
  fetch(postRequest).then(() => getServerData()).catch(error => console.error(error));
}

// Create POST request to /comment servlet
function postComment() {
  console.log("Posting comment");

  // Get user's values 
  const nameElement = document.getElementById("name");
  const commentElement = document.getElementById("comment-box");
  const imgsrc = document.getElementById("modal-img").src;

  // Don't create comment if the comment is empty or the image has no src
  if (!commentElement.value || !imgsrc) {
    console.log("null comment or image src");
    return;
  }
  const img = getImgName(imgsrc);
  if (img == "") {
    console.log("invalid img src");
    return;
  }

  const params = new URLSearchParams();
  params.append("name", nameElement.value);
  params.append("comment", commentElement.value);
  params.append("img", img);
  const postRequest = new Request("/comment", {method: "POST", body: params});

  // Reset textboxes 
  nameElement.value = "";
  commentElement.value = "";

  fetch(postRequest).then(() => getServerData()).catch(error => console.error(error));
}

// Extracts image name from the src
function getImgName(imgsrc) {
  console.log("Getting image name");
  const splitimg = imgsrc.split("/");

  // Get image file name in img tag's src field (ex: flowers.jpg)
  if (splitimg.length == 0) {
    return "";
  }
  const splitname = splitimg[splitimg.length - 1].split(".");
  
  // Get string before file format string (remove .jpg)
  if (splitname.length > 0) {
    return splitname[0];
  }
  return "";
}

// Create the script tag for map api and set the appropriate attributes
var mapscript = document.createElement("script");
mapscript.src = "https://maps.googleapis.com/maps/api/js?key=AIzaSyAy6bZSEG3FN2VdsI8vRcnyew6kt6BTsCg&callback=initMap";
mapscript.defer = true;
mapscript.async = true;

// Attach callback function to display map to the `window` object
window.initMap = function() {
  // Default map is centered at the middle of the Atlantic Ocean and zoomed to display the US and half of Europe
  var centeredLat = 39;
  var centeredLng = -39;
  var defaultZoom = 3;
  const map = new google.maps.Map(document.getElementById("map"), {
    center: {lat: centeredLat, lng: centeredLng},
    zoom: defaultZoom
  });

  // Get location data from server
  showLocations(map);
};

// Create GET request to /location servlet for map data
function showLocations(map) {
  fetch("/location").then(response => response.json()).then(locations => {
    locations.forEach(location => {
      createMarker(map, location);
    });
  });
}

// Create marker icon for location parameter on map
function createMarker(map, location) {
  const infowindowNode = createInfowindowNode(location);
  var infowindow = new google.maps.InfoWindow({
    content: infowindowNode
  });

  // Create marker at the location's latitude and longitude
  var marker = new google.maps.Marker({
    position: {lat: location.latitude, lng: location.longitude},
    map: map
  });

  // Create a bounce animation for 2 seconds, and show info window when marker is clicked on
  marker.addListener("click", function() {
    marker.setAnimation(google.maps.Animation.BOUNCE);
    infowindow.open(map, marker);
    var animationDuration = 2000;   // in milliseconds
    window.setTimeout(function() {marker.setAnimation(null)}, animationDuration);
  });
}

// Create infowindow with text and filter button for location parameter
function createInfowindowNode(location) {
  const infowindowNode = document.createElement("span");
  infowindowNode.innerHTML = "<div id=\"place\"><p>" + location.name + "</p><small>" + location.description + "</small></div>";
  
  // Create button to filter images by location.name
  const filterButton = document.createElement("button");
  filterButton.innerText = "Filter images";
  filterButton.id = "filter-button";
  filterButton.addEventListener("click", () => {
    console.log("Filter button clicked");
    filter(location.name);
  });

  infowindowNode.append(filterButton);
  return infowindowNode;    
}

// Displays images stored in filter_pics.html at parameter location
function filter(location) {
  console.log("filtering: " + location);
  $("#images").load("filter_pics.html #" + location);
}

// Append the "mapscript" element to "head"
document.head.appendChild(mapscript);

// Create GET request to /upload server and display file uploads
function showFiles() {
  console.log("showing files");
  fetch("/upload").then(response => response.json()).then(files => {
    // Add individual files to html page
    const list = document.getElementById("uploads");
    list.innerHTML = "";
    files.forEach((file) => { 
      const listElement = document.createElement("li");

      // Display image
      const imgElement = document.createElement("img");
      imgElement.src = file.url;
      
      // Display caption and date
      const textElement = document.createElement("span");
      const date = new Date(file.timestamp);
      textElement.innerHTML = "<b>" + file.caption + "</b>    <small>" + date.toLocaleString() + "</small><br><br>";

      listElement.appendChild(imgElement);
      listElement.appendChild(textElement);
      list.append(listElement);
    });
  });
}