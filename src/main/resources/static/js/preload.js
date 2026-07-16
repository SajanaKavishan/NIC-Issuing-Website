// Hide preloader after 2.5 seconds
document.body.classList.add('preloader-active');
setTimeout(() => {
    const preloader = document.getElementById('preloader');
    preloader.classList.add('fade-out');

    setTimeout(() => {
        preloader.style.display = 'none';
        document.body.classList.remove('preloader-active');
    }, 500); // match fade-out duration
}, 1000);