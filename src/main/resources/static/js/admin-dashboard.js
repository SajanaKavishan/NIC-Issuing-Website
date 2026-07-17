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

                    if (targetId === 'application-review') {
                        loadReviewApplications();
                    }

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

    // Chart animation
    animateChartBars();

    // Initialize Users management
    initUsersManagement();

    // Initialize New/Renew NIC application review
    initApplicationReview();
});

const reviewState = {
    type: 'new',
    applications: [],
    selectedApplication: null
};

const reviewEndpoints = {
    new: '/api/new-nic',
    renew: '/api/renew-nic'
};

const REVIEW_STATUSES = ['PENDING', 'PROCESSING', 'APPROVED', 'REJECTED', 'DELIVERED'];

function initApplicationReview() {
    const tabs = document.querySelectorAll('[data-review-type]');
    const refreshBtn = document.getElementById('btnRefreshApplications');
    const statusFilter = document.getElementById('applicationStatusFilter');
    const searchInput = document.getElementById('applicationSearchInput');
    const modal = document.getElementById('applicationReviewModal');

    if (!tabs.length || !document.getElementById('applicationsTable')) return;

    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            reviewState.type = tab.dataset.reviewType || 'new';
            tabs.forEach(item => {
                const isActive = item === tab;
                item.classList.toggle('active', isActive);
                item.setAttribute('aria-selected', String(isActive));
            });
            loadReviewApplications();
        });
    });

    if (refreshBtn) refreshBtn.addEventListener('click', loadReviewApplications);
    if (statusFilter) statusFilter.addEventListener('change', renderReviewApplications);
    if (searchInput) searchInput.addEventListener('input', renderReviewApplications);

    document.querySelectorAll('[data-close-review-modal]').forEach(el => {
        el.addEventListener('click', closeReviewModal);
    });

    if (modal) {
        modal.querySelectorAll('[data-review-status]').forEach(button => {
            button.addEventListener('click', () => {
                if (!reviewState.selectedApplication) return;
                updateReviewStatus(reviewState.selectedApplication.id, button.dataset.reviewStatus);
            });
        });
    }

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') closeReviewModal();
    });

    loadReviewApplications();
}

function reviewAuthHeaders(extraHeaders = {}) {
    return authHeaders(extraHeaders);
}

async function loadReviewApplications() {
    const tbody = document.querySelector('#applicationsTable tbody');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="6">Loading applications...</td></tr>';

    try {
        const endpoint = reviewEndpoints[reviewState.type] || reviewEndpoints.new;
        const res = await fetch(`${endpoint}/all`, { headers: reviewAuthHeaders({ 'Accept': 'application/json' }) });
        if (!res.ok) throw new Error(await res.text() || `Status ${res.status}`);
        const data = await res.json();
        reviewState.applications = Array.isArray(data) ? data : [];
        renderReviewApplications();
    } catch (error) {
        console.error('Load application review error', error);
        tbody.innerHTML = `<tr><td colspan="6">${escapeHtml(error.message || 'Failed to load applications')}</td></tr>`;
        updateReviewSummary([]);
    }
}

function renderReviewApplications() {
    const tbody = document.querySelector('#applicationsTable tbody');
    if (!tbody) return;

    const status = document.getElementById('applicationStatusFilter')?.value || 'ALL';
    const query = (document.getElementById('applicationSearchInput')?.value || '').trim().toLowerCase();
    const filtered = reviewState.applications.filter(app => {
        const appStatus = normalizeReviewStatus(app.status);
        const matchesStatus = status === 'ALL' || appStatus === status;
        const matchesQuery = !query || getReviewSearchText(app).includes(query);
        return matchesStatus && matchesQuery;
    });

    updateReviewSummary(reviewState.applications);

    if (!filtered.length) {
        tbody.innerHTML = '<tr><td colspan="6">No applications found.</td></tr>';
        return;
    }

    tbody.innerHTML = filtered.map(app => {
        const title = getReviewApplicantTitle(app);
        const contact = app.contactNumber || '-';
        const email = app.userEmail || '-';
        const statusText = normalizeReviewStatus(app.status);
        return `<tr>
            <td>${escapeHtml(getReviewReference(app))}</td>
            <td>
                <strong>${escapeHtml(title)}</strong>
                <span>${escapeHtml(getReviewSecondaryLine(app))}</span>
            </td>
            <td>${escapeHtml(contact)}</td>
            <td>${escapeHtml(email)}</td>
            <td><span class="status-pill ${statusText.toLowerCase()}">${escapeHtml(statusText)}</span></td>
            <td class="review-actions-cell">
                <button class="btn-ghost" type="button" onclick="openReviewModal(${Number(app.id)})">
                    <i class="fas fa-eye"></i> View
                </button>
                <button class="btn-ghost" type="button" onclick="updateReviewStatus(${Number(app.id)}, 'PROCESSING')">
                    <i class="fas fa-spinner"></i>
                </button>
                <button class="btn-ghost btn-danger" type="button" onclick="updateReviewStatus(${Number(app.id)}, 'REJECTED')">
                    <i class="fas fa-xmark"></i>
                </button>
                <button class="btn-future" type="button" onclick="updateReviewStatus(${Number(app.id)}, 'APPROVED')">
                    <i class="fas fa-check"></i>
                </button>
                <button class="btn-ghost" type="button" onclick="updateReviewStatus(${Number(app.id)}, 'DELIVERED')">
                    <i class="fas fa-truck"></i>
                </button>
            </td>
        </tr>`;
    }).join('');
}

function updateReviewSummary(applications) {
    const counts = applications.reduce((acc, app) => {
        const status = normalizeReviewStatus(app.status);
        acc.total += 1;
        acc[status] = (acc[status] || 0) + 1;
        return acc;
    }, { total: 0 });

    setReviewText('reviewTotalCount', counts.total || 0);
    setReviewText('reviewPendingCount', counts.PENDING || 0);
    setReviewText('reviewProcessingCount', counts.PROCESSING || 0);
    setReviewText('reviewApprovedCount', counts.APPROVED || 0);
    setReviewText('reviewRejectedCount', counts.REJECTED || 0);
    setReviewText('reviewDeliveredCount', counts.DELIVERED || 0);
}

function setReviewText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
}

function openReviewModal(id) {
    const app = reviewState.applications.find(item => Number(item.id) === Number(id));
    const modal = document.getElementById('applicationReviewModal');
    const body = document.getElementById('reviewModalBody');
    if (!app || !modal || !body) return;

    reviewState.selectedApplication = app;
    document.getElementById('reviewModalType').textContent = reviewState.type === 'new' ? 'New NIC Application' : 'Renew NIC Application';
    document.getElementById('reviewModalTitle').textContent = getReviewReference(app);
    body.innerHTML = getReviewDetailRows(app).map(item => `
        <div class="review-detail-item">
            <span>${escapeHtml(item.label)}</span>
            <strong>${escapeHtml(item.value || '-')}</strong>
        </div>
    `).join('');

    modal.classList.add('open');
    modal.setAttribute('aria-hidden', 'false');
}

function closeReviewModal() {
    const modal = document.getElementById('applicationReviewModal');
    if (!modal) return;
    modal.classList.remove('open');
    modal.setAttribute('aria-hidden', 'true');
    reviewState.selectedApplication = null;
}

async function updateReviewStatus(id, status) {
    if (!id || !status) return;
    const endpoint = reviewEndpoints[reviewState.type] || reviewEndpoints.new;
    try {
        const res = await fetch(`${endpoint}/${id}/status`, {
            method: 'PUT',
            headers: reviewAuthHeaders({ 'Content-Type': 'application/json', 'Accept': 'application/json' }),
            body: JSON.stringify({ status })
        });
        const text = await res.text();
        if (!res.ok) throw new Error(text || `Status ${res.status}`);

        let updated = null;
        try { updated = text ? JSON.parse(text) : null; } catch (_) {}
        reviewState.applications = reviewState.applications.map(app => {
            if (Number(app.id) !== Number(id)) return app;
            return updated && typeof updated === 'object' ? updated : { ...app, status };
        });
        renderReviewApplications();
        if (reviewState.selectedApplication && Number(reviewState.selectedApplication.id) === Number(id)) {
            reviewState.selectedApplication = reviewState.applications.find(app => Number(app.id) === Number(id));
            if (reviewState.selectedApplication) openReviewModal(id);
        }
    } catch (error) {
        console.error('Update application status error', error);
        alert(error.message || 'Failed to update application status');
    }
}

function getReviewDetailRows(app) {
    if (reviewState.type === 'renew') {
        return [
            { label: 'Reference', value: getReviewReference(app) },
            { label: 'Old NIC Number', value: app.oldNicNumber },
            { label: 'Birthdate', value: app.birthdate },
            { label: 'Reason', value: app.reason },
            { label: 'Other Reason', value: app.otherReason },
            { label: 'Contact Number', value: app.contactNumber },
            { label: 'Citizen Email', value: app.userEmail },
            { label: 'Birth Certificate File', value: fileNameFromPath(app.birthCertificatePath) },
            { label: 'Photo File', value: fileNameFromPath(app.photoPath) },
            { label: 'Status', value: normalizeReviewStatus(app.status) }
        ];
    }

    return [
        { label: 'Reference', value: getReviewReference(app) },
        { label: 'Name', value: app.nameWithInitials },
        { label: 'Gender', value: app.gender },
        { label: 'Age', value: app.age },
        { label: 'Civil Status', value: app.civilStatus },
        { label: 'Profession', value: app.profession },
        { label: 'Birthdate', value: app.birthdate },
        { label: 'Address', value: app.address },
        { label: 'Contact Number', value: app.contactNumber },
        { label: 'Citizen Email', value: app.userEmail },
        { label: 'Birth Certificate File', value: fileNameFromPath(app.birthCertificatePath) },
        { label: 'Photo File', value: fileNameFromPath(app.photoPath) },
        { label: 'Status', value: normalizeReviewStatus(app.status) }
    ];
}

function getReviewReference(app) {
    const prefix = reviewState.type === 'renew' ? 'REN' : 'NEW';
    return `${prefix}-${String(app.id || '').padStart(5, '0')}`;
}

function getReviewApplicantTitle(app) {
    return reviewState.type === 'renew' ? (app.oldNicNumber || `Renew request ${app.id}`) : (app.nameWithInitials || `Applicant ${app.id}`);
}

function getReviewSecondaryLine(app) {
    return reviewState.type === 'renew' ? (app.reason || 'Renewal request') : (app.profession || app.gender || 'New NIC request');
}

function getReviewSearchText(app) {
    return [
        app.id,
        app.nameWithInitials,
        app.oldNicNumber,
        app.userEmail,
        app.contactNumber,
        app.profession,
        app.reason,
        app.status
    ].filter(Boolean).join(' ').toLowerCase();
}

function normalizeReviewStatus(status) {
    const normalized = String(status || 'PENDING').trim().toUpperCase().replace(/\s+/g, '_');
    return REVIEW_STATUSES.includes(normalized) ? normalized : 'PENDING';
}

function fileNameFromPath(path) {
    if (!path) return '';
    return String(path).split(/[\\/]/).pop();
}

function escapeHtml(value) {
    return value == null ? '' : String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

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

// Users management logic
function initUsersManagement() {
    const API_BASE = '/api/users';
    const btnAdd = document.getElementById('btnAddUser');
    const formWrap = document.getElementById('userFormContainer');
    const form = document.getElementById('userForm');
    const formTitle = document.getElementById('userFormTitle');
    const btnCancel = document.getElementById('btnCancelUser');

    if (!btnAdd || !form || !btnCancel) {
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
        document.getElementById('ufRole').value = user?.role || '';
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
            role: document.getElementById('ufRole').value,
            password: document.getElementById('ufPassword').value
        };

        if (!payload.firstName || !payload.lastName || !payload.email || !payload.role) {
            alert('Please fill all required fields');
            return;
        }
        if (!id && !payload.password) {
            alert('Password is required for new users');
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
                    headers: { 'Content-Type': 'application/json', ...authHeaders() },
                    body: JSON.stringify(payload)
                });
                text = await res.text();
                if (!res.ok) throw new Error(text || `Status ${res.status}`);
            } else {
                // Create via admin endpoint, with role validation
                res = await fetch(API_BASE, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', ...authHeaders() },
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
            const res = await fetch(`${API_BASE}/${id}`, { method: 'DELETE', headers: authHeaders() });
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
    tbody.innerHTML = '<tr><td colspan="6">Loading...</td></tr>';
    try {
        const res = await fetch(API_BASE, { headers: authHeaders() });
        if (!res.ok) throw new Error(await res.text() || `Status ${res.status}`);
        const data = await res.json();
        const list = Array.isArray(data) ? data : [];
        // cache users for edit lookups
        window.__usersCache = list;
        renderUsers(list);
    } catch (e) {
        console.error('Load users error', e);
        tbody.innerHTML = '<tr><td colspan="6">Failed to load users</td></tr>';
    }
}

function renderUsers(users) {
    const tbody = document.querySelector('#usersTable tbody');
    if (!tbody) return;
    if (!users.length) {
        tbody.innerHTML = '<tr><td colspan="6">No users found</td></tr>';
        return;
    }
    tbody.innerHTML = users.map(u => {
        const safe = (v) => (v == null ? '' : String(v).replace(/&/g,'&amp;').replace(/</g,'&lt;'));
        return `<tr>
            <td>${safe(u.id)}</td>
            <td>${safe(u.firstName)}</td>
            <td>${safe(u.lastName)}</td>
            <td>${safe(u.email)}</td>
            <td>${safe(u.role)}</td>
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
