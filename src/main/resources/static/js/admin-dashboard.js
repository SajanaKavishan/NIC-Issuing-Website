// Admin Dashboard Functionality
document.addEventListener('DOMContentLoaded', function() {
    // Initialize animations
    initAnimations();

    // Sidebar Toggle
    const sidebarToggle = document.querySelector('.sidebar-toggle');
    const sidebar = document.querySelector('.sidebar');

    // Only attach sidebar toggle handler if elements exist (page may not have a sidebar)
    if (sidebarToggle && sidebar) {
        sidebarToggle.addEventListener('click', function() {
            sidebar.classList.toggle('active');
        });
    }

    // Navigation
    const navLinks = document.querySelectorAll('.nav-item > a, .nav-item > button');
    const navItems = document.querySelectorAll('.nav-item');
    const sections = document.querySelectorAll('.dashboard-section');

    if (navLinks && navLinks.length) {
        navLinks.forEach(link => {
            link.addEventListener('click', function(e) {
                e.preventDefault();

                // Remove active class from all items
                navItems.forEach(nav => nav.classList.remove('active'));
                sections.forEach(section => section.classList.remove('active'));

                // Add active class to clicked item's parent li
                const parentItem = this.closest('.nav-item');
                if (parentItem) parentItem.classList.add('active');

                // Show corresponding section
                const dataTarget = this.getAttribute('data-target');
                const href = this.getAttribute('href') || '';
                const targetId = dataTarget ? dataTarget : (href.startsWith('#') ? href.substring(1) : href);
                const targetSection = document.getElementById(targetId);
                if (targetSection) {
                    targetSection.classList.add('active');

                    // Re-initialize animations for the new section
                    setTimeout(() => {
                        initSectionAnimations(targetSection);
                    }, 100);

                    // If moved to Users, refresh list
                    if (targetId === 'users') {
                        loadUsers();
                    }
                }
            });
        });
    }

    // Notification bell animation
    const notificationBtn = document.querySelector('.notification-btn');
    if (notificationBtn) {
        notificationBtn.addEventListener('click', function() {
            this.classList.add('pulse');
            setTimeout(() => {
                this.classList.remove('pulse');
            }, 600);
        });
    }

    // Search functionality
    const searchInput = document.querySelector('.search-box input');
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            // Implement search functionality here
            console.log('Searching for:', this.value);
        });
    }

    // Chart animation
    animateChartBars();

    // Simulate real-time data updates
    setInterval(updateDashboardData, 10000);

    // Initialize dashboard data
    loadDashboardData();

    // Initialize Users management
    initUsersManagement();
});

// Animation initialization
function initAnimations() {
    const animatedElements = document.querySelectorAll('.fade-in, .slide-in-left, .slide-in-right, .scale-in');

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
            }
        });
    }, { threshold: 0.1 });

    animatedElements.forEach(element => {
        observer.observe(element);
    });
}

function initSectionAnimations(section) {
    const elements = section.querySelectorAll('.fade-in, .slide-in-left, .slide-in-right, .scale-in');
    elements.forEach(element => {
        element.classList.remove('visible');
        setTimeout(() => {
            element.classList.add('visible');
        }, 100);
    });
}

// Chart bar animation
function animateChartBars() {
    const bars = document.querySelectorAll('.bar');
    bars.forEach((bar, index) => {
        setTimeout(() => {
            bar.style.animation = 'barGrow 1s ease-out';
        }, index * 200);
    });
}

// Simulated data loading
async function loadDashboardData() {
    try {
        // Simulate API call
        const response = await simulateAPICall();
        updateDashboardStats(response);
    } catch (error) {
        console.error('Error loading dashboard data:', error);
    }
}

function simulateAPICall() {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                totalApplications: 1247,
                pendingApprovals: 89,
                approvedToday: 956,
                totalRevenue: 287500,
                recentActivities: [
                    { type: 'new', message: 'New user registration', time: '2 mins ago' },
                    { type: 'approved', message: 'Application approved', time: '15 mins ago' },
                    { type: 'warning', message: 'Payment issue detected', time: '1 hour ago' }
                ]
            });
        }, 1000);
    });
}

function updateDashboardStats(data) {
    // Update statistics cards
    const statCards = document.querySelectorAll('.stat-info h3');
    if (statCards.length >= 4) {
        statCards[0].textContent = data.totalApplications.toLocaleString();
        statCards[1].textContent = data.pendingApprovals;
        statCards[2].textContent = data.approvedToday;
        statCards[3].textContent = `LKR ${data.totalRevenue.toLocaleString()}`;
    }
}

function updateDashboardData() {
    // Simulate real-time data updates
    const randomIncrement = Math.floor(Math.random() * 10) + 1;
    const pendingElement = document.querySelector('.stat-card:nth-child(2) h3');
    if (pendingElement) {
        const current = parseInt(pendingElement.textContent);
        pendingElement.textContent = Math.max(0, current - randomIncrement);
    }
}

// Users management logic
function initUsersManagement() {
    const API_BASE = '/api/users';
    const btnAdd = document.getElementById('btnAddUser');
    const formWrap = document.getElementById('userFormContainer');
    const form = document.getElementById('userForm');
    const formTitle = document.getElementById('userFormTitle');
    const btnCancel = document.getElementById('btnCancelUser');

    // If any of these core elements are missing, still expose edit/delete stubs so buttons in the table won't break
    if (!btnAdd || !form || !btnCancel) {
        // Create safe no-op handlers so onclick references do not throw
        window.__editUser = (id) => { console.warn('Edit user UI not available on this page.'); };
        window.__deleteUser = async (id) => { alert('Delete not available on this page.'); };
        return; // Users section not on this page
    }

    btnAdd.addEventListener('click', () => {
        openUserForm();
    });

    btnCancel.addEventListener('click', () => {
        hideUserForm();
    });

    form.addEventListener('submit', (e) => {
        e.preventDefault();
        saveUser();
    });

    // Load initial list if users section initially active
    const usersSection = document.getElementById('users');
    if (usersSection && usersSection.classList.contains('active')) {
        loadUsers();
    }

    function openUserForm(user) {
        form.reset();
        document.getElementById('userId').value = user?.id || '';
        document.getElementById('ufFirst').value = user?.firstName || '';
        document.getElementById('ufLast').value = user?.lastName || '';
        document.getElementById('ufEmail').value = user?.email || '';
        document.getElementById('ufPassword').value = '';
        formTitle.textContent = user ? 'Edit User' : 'Add User';
        formWrap.style.display = 'block';
    }

    function hideUserForm() {
        formWrap.style.display = 'none';
        form.reset();
    }

    async function saveUser() {
        const id = document.getElementById('userId').value;
        const payload = {
            firstName: document.getElementById('ufFirst').value.trim(),
            lastName: document.getElementById('ufLast').value.trim(),
            email: document.getElementById('ufEmail').value.trim(),
            password: document.getElementById('ufPassword').value
        };

        if (!payload.firstName || !payload.lastName || !payload.email) {
            alert('Please fill all required fields');
            return;
        }

        const submitBtn = document.getElementById('btnSaveUser');
        const origText = submitBtn ? submitBtn.textContent : null;
        if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = 'Saving...'; }

        try {
            let res, text;
            if (id) {
                // Update
                res = await fetch(`${API_BASE}/${id}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                text = await res.text();
                if (!res.ok) throw new Error(text || `Status ${res.status}`);
            } else {
                // Create via signup endpoint
                res = await fetch(`${API_BASE}/signup`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                text = await res.text();
                if (!res.ok) throw new Error(text || `Status ${res.status}`);
            }
            alert(text || 'Operation succeeded');
            hideUserForm();
            loadUsers();
        } catch (e) {
            console.error('Save user error', e);
            alert(e.message || 'Request failed');
        } finally {
            if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = origText; }
        }
    }

    // Expose functions to global scope for action buttons
    window.__editUser = (id) => {
        try {
            const userId = Number(id);
            const list = Array.isArray(window.__usersCache) ? window.__usersCache : [];
            const user = list.find(u => Number(u.id) === userId);
            if (user) {
                openUserForm(user);
            } else {
                // If not found in cache, fetch the user from server
                fetch(`${API_BASE}/by-email?email=`).catch(()=>{});
                openUserForm();
            }
        } catch {
            openUserForm();
        }
    };
    window.__deleteUser = async (id) => {
        if (!confirm('Are you sure you want to delete this user?')) return;
        try {
            const res = await fetch(`${API_BASE}/${id}`, { method: 'DELETE' });
            const text = await res.text();
            if (!res.ok) throw new Error(text || `Status ${res.status}`);
            alert(text || 'User deleted');
            loadUsers();
        } catch (e) {
            console.error('Delete user error', e);
            alert(e.message || 'Request failed');
        }
    };
}

async function loadUsers() {
    const API_BASE = '/api/users';
    const tbody = document.querySelector('#usersTable tbody');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="5">Loading...</td></tr>';
    try {
        const res = await fetch(API_BASE);
        if (!res.ok) throw new Error(await res.text() || `Status ${res.status}`);
        const data = await res.json();
        const list = Array.isArray(data) ? data : [];
        // cache users for edit lookups
        window.__usersCache = list;
        renderUsers(list);
    } catch (e) {
        console.error('Load users error', e);
        tbody.innerHTML = '<tr><td colspan="5">Failed to load users</td></tr>';
    }
}

function renderUsers(users) {
    const tbody = document.querySelector('#usersTable tbody');
    if (!tbody) return;
    if (!users.length) {
        tbody.innerHTML = '<tr><td colspan="5">No users found</td></tr>';
        return;
    }
    tbody.innerHTML = users.map(u => {
        const safe = (v) => (v == null ? '' : String(v).replace(/&/g,'&amp;').replace(/</g,'&lt;'));
        return `<tr>
            <td>${safe(u.id)}</td>
            <td>${safe(u.firstName)}</td>
            <td>${safe(u.lastName)}</td>
            <td>${safe(u.email)}</td>
            <td style="text-align:right;">
                <button class="btn-ghost" onclick="__editUser(${Number(u.id)})"><i class="fas fa-edit"></i> Edit</button>
                <button class="btn-ghost" onclick="__deleteUser(${u.id})"><i class="fas fa-trash"></i> Delete</button>
            </td>
        </tr>`;
    }).join('');
}

// Export functions for potential module use
window.AdminDashboard = {
    init: function() {
        document.addEventListener('DOMContentLoaded', function() {
            // Re-initialize if needed
        });
    },
    refreshData: loadDashboardData
};