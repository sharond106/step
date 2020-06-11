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
      textElement.innerHTML = "<b>" + file.caption + "</b><small>    " + date.toLocaleString() + "</small><br><br>";

      listElement.appendChild(imgElement);
      listElement.appendChild(textElement);
      list.append(listElement);
    });
  });
}