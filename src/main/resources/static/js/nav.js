// Guard: redirect unauthenticated users from protected NIC pages
(function() {
    try {
        const currentPage = window.location.pathname.split('/').pop();
        const protectedPages = new Set(['new-nic.html', 'renew-nic.html', 'lost-nic.html']);
        const email = (function(){ try { return localStorage.getItem('loggedInEmail'); } catch (e) { return null; } })();
        if (protectedPages.has(currentPage) && !email) {
            const redirect = encodeURIComponent(currentPage);
            window.location.replace(`login.html?redirect=${redirect}`);
        }
    } catch (e) {
        // Fail-closed: if any error happens reading storage, send to login when on protected page
        try {
            const currentPage = window.location.pathname.split('/').pop();
            const protectedPages = new Set(['new-nic.html', 'renew-nic.html', 'lost-nic.html']);
            if (protectedPages.has(currentPage)) {
                window.location.replace('login.html');
            }
        } catch (_) {}
    }
})();

// Navigation functionality
document.addEventListener('DOMContentLoaded', function() {
    // Load navigation
    fetch('nav.html')
        .then(response => response.text())
        .then(data => {
            document.body.insertAdjacentHTML('afterbegin', data);

            // Load navigation styles
            const navStyles = document.createElement('link');
            navStyles.rel = 'stylesheet';
            navStyles.href = 'nav.css';
            document.head.appendChild(navStyles);

            // Initialize navigation functionality
            initNavigation();
        });
});

// Initialize navigation functionality
function initNavigation() {
    // Toggle dropdown on mobile
    const dropdowns = document.querySelectorAll('.dropdown');
    dropdowns.forEach(dropdown => {
        const dropbtn = dropdown.querySelector('.dropbtn');
        const content = dropdown.querySelector('.dropdown-content');

        if (dropbtn && content) {
            dropbtn.addEventListener('click', (e) => {
                if (window.innerWidth <= 700) {
                    e.preventDefault();
                    content.style.display = content.style.display === 'block' ? 'none' : 'block';
                }
            });
        }
    });

    // Highlight current page in navigation
    const currentPage = window.location.pathname.split('/').pop();
    const navLinks = document.querySelectorAll('nav a');

    navLinks.forEach(link => {
        const linkPage = link.getAttribute('href');
        if (linkPage === currentPage || (currentPage === '' && linkPage === 'index.html')) {
            link.classList.add('current-page');
        }

        // Handle dropdown links
        if (linkPage && linkPage !== '#' && !link.parentElement.classList.contains('dropdown')) {
            link.addEventListener('click', function(e) {
                if (window.innerWidth <= 700) {
                    const dropdown = this.closest('.dropdown');
                    if (dropdown) {
                        const content = dropdown.querySelector('.dropdown-content');
                        if (content) {
                            content.style.display = 'none';
                        }
                    }
                }
            });
        }
    });

    // Intercept Obtaining NIC links to enforce auth
    try {
        const nicLinks = Array.from(document.querySelectorAll('a[href="new-nic.html"], a[href="renew-nic.html"], a[href="lost-nic.html"]'));
        nicLinks.forEach(link => {
            link.addEventListener('click', function(e) {
                let email = null;
                try { email = localStorage.getItem('loggedInEmail'); } catch (_) { email = null; }
                if (!email) {
                    e.preventDefault();
                    const target = this.getAttribute('href') || 'login.html';
                    location.href = `login.html?redirect=${encodeURIComponent(target)}`;
                }
            });
        });
    } catch (_) {}

    // Initialize auth-aware UI for nav
    initAuthInNav();
}

function initAuthInNav() {
    const container = document.querySelector('.auth-buttons');
    if (!container) return;

    function renderLoggedOut() {
        container.innerHTML = '';
        const btnLogin = document.createElement('button');
        btnLogin.className = 'btn-ghost';
        btnLogin.textContent = 'Login';
        btnLogin.onclick = () => { location.href = 'login.html'; };
        const btnSignup = document.createElement('button');
        btnSignup.className = 'btn-future';
        btnSignup.textContent = 'Sign Up';
        btnSignup.onclick = () => { location.href = 'signup.html'; };
        container.appendChild(btnLogin);
        container.appendChild(btnSignup);
    }

    function renderLoggedIn(profile) {
        container.innerHTML = '';
        const name = (profile && (profile.firstName || profile.email)) || 'User';

        // Build user dropdown menu
        const userDropdown = document.createElement('div');
        userDropdown.className = 'dropdown user-menu';

        const trigger = document.createElement('a');
        trigger.href = '#';
        trigger.className = 'dropbtn user-dropbtn';
        trigger.textContent = `Hello, ${name} \u25BE`; // ▾

        const menu = document.createElement('div');
        menu.className = 'dropdown-content';

        const dash = document.createElement('a');
        // dash.href = 'dashboard.html';
        // dash.textContent = 'Dashboard';

        const logout = document.createElement('a');
        logout.href = '#';
        logout.textContent = 'Logout';
        logout.addEventListener('click', (e) => {
            e.preventDefault();
            try { localStorage.removeItem('loggedInEmail'); } catch (e) {}
            // Simple refresh to re-render nav state
            if (window.location.pathname.endsWith('login.html')) {
                location.reload();
            } else {
                location.href = 'home.html';
            }
        });

        menu.appendChild(dash);
        menu.appendChild(logout);
        userDropdown.appendChild(trigger);
        userDropdown.appendChild(menu);

        // Ensure mobile toggle works for dynamically created dropdown
        trigger.addEventListener('click', (e) => {
            if (window.innerWidth <= 700) {
                e.preventDefault();
                menu.style.display = menu.style.display === 'block' ? 'none' : 'block';
            }
        });

        container.appendChild(userDropdown);
    }

    let email = null;
    let isAdmin = false;
    try {
        email = localStorage.getItem('loggedInEmail');
        isAdmin = localStorage.getItem('isAdmin') === 'true';
    } catch (e) {
        email = null;
        isAdmin = false;
    }

    // If admin, render a dedicated Admin greeting without hitting user API
    if (isAdmin) {
        const container = document.querySelector('.auth-buttons');
        if (container) {
            container.innerHTML = '';
            const userDropdown = document.createElement('div');
            userDropdown.className = 'dropdown user-menu';

            const trigger = document.createElement('a');
            trigger.href = '#';
            trigger.className = 'dropbtn user-dropbtn';
            trigger.textContent = 'Hello, Admin \u25BE';

            const menu = document.createElement('div');
            menu.className = 'dropdown-content';

            const dash = document.createElement('a');
            dash.href = 'admin-dashboard.html';
            dash.textContent = 'Admin Dashboard';

            const logout = document.createElement('a');
            logout.href = '#';
            logout.textContent = 'Logout';
            logout.addEventListener('click', (e) => {
                e.preventDefault();
                try {
                    localStorage.removeItem('isAdmin');
                    localStorage.removeItem('loggedInEmail');
                } catch (e) {}
                // Redirect to login after logout from admin
                location.href = 'login.html';
            });

            menu.appendChild(dash);
            menu.appendChild(logout);
            userDropdown.appendChild(trigger);
            userDropdown.appendChild(menu);

            // Mobile dropdown toggle
            trigger.addEventListener('click', (e) => {
                if (window.innerWidth <= 700) {
                    e.preventDefault();
                    menu.style.display = menu.style.display === 'block' ? 'none' : 'block';
                }
            });

            container.appendChild(userDropdown);
        }
        return;
    }

    if (!email) {
        renderLoggedOut();
        return;
    }

    fetch(`http://localhost:8080/api/users/by-email?email=${encodeURIComponent(email)}`)
        .then(r => r.ok ? r.json() : null)
        .then(profile => {
            if (profile && (profile.firstName || profile.email)) {
                renderLoggedIn(profile);
            } else if (email) {
                // Fallback: render basic logged-in state with email if API did not return profile
                renderLoggedIn({ email });
            } else {
                renderLoggedOut();
            }
        })
        .catch(() => {
            // Fallback on network error
            if (email) {
                renderLoggedIn({ email });
            } else {
                renderLoggedOut();
            }
        });
}