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
window.onhashchange = function() {scrollFunction()};

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
}

// Request content from server
function getServerData() {
  console.log("Fetching data.");
  const promise = fetch("/comment").then(response => response.json()).then(comments => {
    console.log(comments[comments.length - 1]);

    const thread = document.getElementById('all-comments');
    for (var i = comments.length - 1; i >= 0; i--) {
        thread.innerHTML += (comments[i] + "<br>");
    }
  });
}