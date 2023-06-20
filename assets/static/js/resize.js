function resizeBackgroundDiv(event) {
    console.log('hello world!')
    document.getElementById('background').style.minHeight = document.getElementById('dialog').offsetHeight * 1.05 + "px";
}

window.addEventListener('resize', resizeBackgroundDiv, true);
resizeBackgroundDiv(null);
