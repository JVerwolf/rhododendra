const ADVANCED_OPTIONS_STATE = "index_collapse"

// REF: https://www.w3schools.com/howto/howto_js_collapsible.asp
function toggle_index_dropdown_state() {
    const dropdown = document.getElementById("advanced_index_dropdown")
    // dropdown.classList.toggle("active");
    dropdown.classList.add("advanced_search_trans");
    if (dropdown.style.maxHeight) {
        close_index_dropdown(dropdown)
    } else {
        open_index_dropdown(dropdown)
    }
}

function open_index_dropdown(dropdown) {
    dropdown.style.maxHeight = dropdown.scrollHeight + "px";
    setCookie(ADVANCED_OPTIONS_STATE, 'true', 365);
}

function close_index_dropdown(dropdown) {
    dropdown.style.maxHeight = null;
    setCookie(ADVANCED_OPTIONS_STATE, 'false', 365);
}

function load_index_dropdown_state() {
    const open = getCookie(ADVANCED_OPTIONS_STATE);
    const dropdown = document.getElementById("advanced_index_dropdown")
    dropdown.classList.remove("advanced_search_trans");
    if (open !== null && open === 'false') {
        dropdown.style.transition = null;
        close_index_dropdown(dropdown)
    } else if (open !== null && open === 'true') {
        dropdown.style.transition = null;
        open_index_dropdown(dropdown)
    }
}

// document.addEventListener("DOMContentLoaded", load_index_dropdown_state());


// REF: https://www.w3schools.com/js/js_cookies.asp
function setCookie(cname, cvalue, exdays) {
    const d = new Date();
    d.setTime(d.getTime() + (exdays * 24 * 60 * 60 * 1000));
    let expires = "expires=" + d.toUTCString();
    document.cookie = cname + "=" + cvalue + ";" + expires + ";path=/";
}

function getCookie(cname) {
    let name = cname + "=";
    let decodedCookie = decodeURIComponent(document.cookie);
    let ca = decodedCookie.split(';');
    for (let i = 0; i < ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) === ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) === 0) {
            return c.substring(name.length, c.length);
        }
    }
    return null;
}