// Cursor parallax effect
const root = document.documentElement;
window.addEventListener('pointermove', (e) => {
    const x = e.clientX / window.innerWidth;
    const y = e.clientY / window.innerHeight;
    root.style.setProperty('--mx', x.toFixed(3));
    root.style.setProperty('--my', y.toFixed(3));
});

// Mobile dropdown toggle
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

// Scroll animation
document.addEventListener('DOMContentLoaded', () => {
    const animatedElements = document.querySelectorAll('.fade-in, .slide-in-left, .slide-in-right, .scale-in');

    function isInViewport(element) {
        const rect = element.getBoundingClientRect();
        return (
            rect.top <= (window.innerHeight || document.documentElement.clientHeight) * 0.9 &&
            rect.bottom >= 0
        );
    }

    function checkScroll() {
        animatedElements.forEach(element => {
            if (isInViewport(element)) {
                element.classList.add('visible');
            }
        });
    }

    checkScroll();
    window.addEventListener('scroll', checkScroll);
});

// Form submission handler (client-side validation)
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('feedback-form');
    form.addEventListener('submit', (e) => {
        e.preventDefault();
        const formData = new FormData(form);
        const data = {
            name: formData.get('name'),
            mail: formData.get('email'), // match backend field
            type: formData.get('type'),
            subject: formData.get('subject'),
            message: formData.get('message')
        };

        // Basic validation
        if (!data.name || !data.mail || !data.type || !data.subject || !data.message) {
            alert('Please fill out all required fields.');
            return;
        }
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.mail)) {
            alert('Please enter a valid email address.');
            return;
        }

        // Send to backend
        fetch('http://localhost:8080/api/feedback', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        })
        .then(res => {
            if (res.ok) {
                alert('Thank you for your submission! We will review it soon.');
                form.reset();
            } else {
                alert('Submission failed. Please try again.');
            }
        })
        .catch(() => {
            alert('Submission failed. Please try again.');
        });
    });
});