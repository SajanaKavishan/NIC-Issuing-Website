// Subtle cursor parallax via CSS variables
const root = document.documentElement;
window.addEventListener('pointermove', (e) => {
    const x = e.clientX / window.innerWidth;
    const y = e.clientY / window.innerHeight;
    root.style.setProperty('--mx', x.toFixed(3));
    root.style.setProperty('--my', y.toFixed(3));
});

// Toggle dropdown on mobile
const dropdowns = document.querySelectorAll('.dropdown');
dropdowns.forEach(dropdown => {
    const dropbtn = dropdown.querySelector('.dropbtn');
    const content = dropdown.querySelector('.dropdown-content');
    dropbtn.addEventListener('click', (e) => {
        if (window.innerWidth <= 700) {
            e.preventDefault();
            content.style.display = content.style.display === 'block' ? 'none' : 'block';
        }
    });
});

// Scroll animation implementation
document.addEventListener('DOMContentLoaded', function() {
    const animatedElements = document.querySelectorAll('.fade-in, .slide-in-left, .slide-in-right, .scale-in');

    // Function to check if element is in viewport
    function isInViewport(element) {
        const rect = element.getBoundingClientRect();
        return (
            rect.top <= (window.innerHeight || document.documentElement.clientHeight) * 0.9 &&
            rect.bottom >= 0
        );
    }

    // Function to handle scroll events
    function checkScroll() {
        animatedElements.forEach(element => {
            if (isInViewport(element)) {
                element.classList.add('visible');
            }
        });
    }

    // Initial check
    checkScroll();

    // Listen for scroll events
    window.addEventListener('scroll', checkScroll);
});