document.addEventListener("DOMContentLoaded", function () {
    var panel = document.getElementById("rhodo-edit-panel");
    var toggle = document.getElementById("rhodo-edit-toggle");
    var cancel = document.getElementById("rhodo-edit-cancel");
    if (!panel || !toggle) {
        return;
    }
    toggle.addEventListener("click", function () {
        panel.classList.toggle("is-open");
    });
    if (cancel) {
        cancel.addEventListener("click", function () {
            panel.classList.remove("is-open");
        });
    }
});
