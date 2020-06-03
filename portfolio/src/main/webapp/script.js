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
  const maxElement = document.getElementById("quantity");
  const maxToDisplay = maxElement.value;
  const sort = document.getElementById("sort").value;

  fetch("/comment?quantity=" + maxToDisplay + "&sort=" + sort).then(response => response.json()).then(jsonObj => {
    const comments = jsonObj.comments;
    console.log(comments[comments.length - 1]);
    const total = jsonObj.total;
    console.log(total);

    const totalElement = document.getElementById("total");
    totalElement.innerText = ("Comments (" + total + ")");

    maxElement.max = total.toString();
    maxElement.placeholder = total.toString();

    const list = document.getElementById("all-comments");
    list.innerHTML = "";
    comments.forEach((comment) => {
      const listElement = document.createElement("li");

      const textElement = document.createElement("span");
      textElement.innerHTML = comment.name + "<br><br>" + comment.comment;

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
    })
  })
  .catch(error => console.error(error));
}

function deleteComment(comment) {
  console.log("Deleting comment");

  const params = new URLSearchParams();
  params.append("id", comment.id);
  const postRequest = new Request("/delete", {method: "POST", body: params});
  fetch(postRequest).then(() => getServerData()).catch(error => console.error(error));
}

function postComment() {
  console.log("Posting comment");

  const nameElement = document.getElementById("name");
  const commentElement = document.getElementById("comment-box");

  const params = new URLSearchParams();
  params.append("name", nameElement.value);
  params.append("comment", commentElement.value);
  const postRequest = new Request("/comment", {method: "POST", body: params});

  nameElement.value = "";
  commentElement.value = "";
  
  fetch(postRequest).then(() => getServerData()).catch(error => console.error(error));
}
