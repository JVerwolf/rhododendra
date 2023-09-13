function showAlert() {
    alert("The button was clicked!");
    fetch()
}

function search() {
    var input = document.getElementById("search").value;
    console.log(input)
    console.log("http://127.0.0.1:8090/search?q=" + encodeURIComponent(input))
    fetch("http://127.0.0.1:8090/search?q=" + encodeURIComponent(input))
        .then(res => res.json())
        .then(data => console.log(data))
}