// Dashboard functionality
document.addEventListener('DOMContentLoaded', function() {
    // Initialize dashboard
    initDashboard();

    // Notification functionality
    setupNotifications();

    // Status timeline animation
    animateStatusTimeline();

    // My Application Status section
    renderMyApplicationStatus();

    // Card hover effects
    setupCardInteractions();

    // Mobile menu functionality
    setupMobileMenu();
});

const APPLICATION_STATUSES = ['PENDING', 'PROCESSING', 'APPROVED', 'REJECTED', 'DELIVERED'];

function initDashboard() {
    loadUserData();
}

function setupNotifications() {
    const notificationBtn = document.querySelector('.notification-btn');
    const notificationBadge = document.querySelector('.notification-badge');

    if (notificationBtn) {
        notificationBtn.addEventListener('click', function(e) {
            e.preventDefault();
            toggleNotifications();
        });
    }

    const badge = document.querySelector('.notification-badge');
    if (badge) badge.style.display = 'none';
}

function toggleNotifications() {
    showToast('No new notifications', 'info');
}

function animateStatusTimeline() {
    const statusSteps = document.querySelectorAll('.status-step');

    statusSteps.forEach((step, index) => {
        // Add delay for animation sequence
        step.style.opacity = '0';
        step.style.transform = 'translateY(20px)';

        setTimeout(() => {
            step.style.transition = 'all 0.5s ease';
            step.style.opacity = '1';
            step.style.transform = 'translateY(0)';
        }, index * 200);
    });
}

async function renderMyApplicationStatus() {
    const list = document.getElementById('applicationStatusList');
    if (!list) return;

    list.innerHTML = '<div class="application-status-empty">Loading application status...</div>';

    const applications = await getCitizenApplications();

    if (!applications.length) {
        list.innerHTML = '<div class="application-status-empty">No applications found yet.</div>';
        updateApplicationStatusCounts(applications);
        return;
    }

    list.innerHTML = applications.map(app => `
        <article class="application-status-item">
            <div>
                <div class="application-title">${escapeHtml(app.type)}</div>
                <div class="application-ref">Reference: ${escapeHtml(app.reference)}</div>
            </div>
            <div class="application-date">
                <strong>Submitted</strong>
                ${escapeHtml(app.submitted)}
            </div>
            <div class="application-next-step">
                <strong>Next Step</strong>
                ${escapeHtml(app.nextStep)}
            </div>
            <span class="application-status-badge ${getApplicationStatusClass(app.status)}">
                ${escapeHtml(formatApplicationStatus(app.status))}
            </span>
        </article>
    `).join('');

    updateApplicationStatusCounts(applications);
    updateCurrentApplication(applications);
}

async function getCitizenApplications() {
    try {
        const res = await fetch('/api/applications/mine', { headers: authHeaders({ 'Accept': 'application/json' }) });
        if (!res.ok) return [];
        const data = await res.json();
        return [
            ...normalizeApplicationList(data.newNic, 'New NIC Application', 'NEW', app => app.nameWithInitials || `Application ${app.id}`),
            ...normalizeApplicationList(data.renewNic, 'NIC Renewal', 'REN', app => app.oldNicNumber || `Renewal ${app.id}`),
            ...normalizeApplicationList(data.lostNic, 'Lost NIC Request', 'LST', app => app.nicNumber || `Lost NIC ${app.id}`)
        ].sort((a, b) => new Date(b.submittedRaw || 0) - new Date(a.submittedRaw || 0));
    } catch (_) {}

    return [];
}

function normalizeApplicationList(value, type, prefix, getTitle) {
    const list = Array.isArray(value) ? value : [];
    return list.map(app => {
        const id = app.id || '';
        const status = normalizeApplicationStatus(app.status);
        const submittedRaw = app.submittedAt || app.createdAt || app.birthdate || app.lostDate || '';
        return {
            type,
            reference: `${prefix}-${String(id).padStart(5, '0')}`,
            status,
            submitted: formatDate(submittedRaw),
            submittedRaw,
            nextStep: getNextStep(status),
            title: getTitle(app)
        };
    });
}

function getNextStep(status) {
    const normalized = normalizeApplicationStatus(status);
    if (normalized === 'APPROVED') return 'Awaiting delivery or pickup';
    if (normalized === 'DELIVERED') return 'Completed';
    if (normalized === 'REJECTED') return 'Contact support for details';
    if (normalized === 'PROCESSING') return 'Officer review';
    return 'Document verification';
}

function formatDate(value) {
    if (!value) return '-';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);
    return date.toLocaleDateString();
}

function updateCurrentApplication(applications) {
    const current = applications.find(app => !['DELIVERED', 'REJECTED'].includes(normalizeApplicationStatus(app.status))) || applications[0];
    setText('currentApplicationRef', current ? current.reference : 'No active application');
    setText('currentApplicationDate', current ? current.submitted : '-');
    setText('currentApplicationStatus', current ? formatApplicationStatus(current.status) : '-');
}

function updateApplicationStatusCounts(applications) {
    const inProgressCount = applications.filter(app => normalizeApplicationStatus(app.status) === 'PROCESSING').length;
    const approvedCount = applications.filter(app => normalizeApplicationStatus(app.status) === 'APPROVED').length;
    const pendingCount = applications.filter(app => normalizeApplicationStatus(app.status) === 'PENDING').length;

    setText('inProgressCount', inProgressCount);
    setText('approvedCount', approvedCount);
    setText('pendingCount', pendingCount);
}

function normalizeApplicationStatus(status) {
    const normalized = String(status || 'PENDING').trim().toUpperCase().replace(/\s+/g, '_');
    return APPLICATION_STATUSES.includes(normalized) ? normalized : 'PENDING';
}

function getApplicationStatusClass(status) {
    const normalized = normalizeApplicationStatus(status);
    if (normalized === 'APPROVED') return 'approved';
    if (normalized === 'REJECTED') return 'rejected';
    if (normalized === 'DELIVERED') return 'delivered';
    if (normalized === 'PENDING') return 'pending';
    return 'processing';
}

function formatApplicationStatus(status) {
    return normalizeApplicationStatus(status).replace(/_/g, ' ');
}

function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
}

function escapeHtml(value) {
    return String(value || '').replace(/[&<>"']/g, char => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    }[char]));
}

function setupCardInteractions() {
    const cards = document.querySelectorAll('.dashboard-card');

    cards.forEach(card => {
        card.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-5px) scale(1.02)';
        });

        card.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0) scale(1)';
        });

        // Click functionality for cards
        card.addEventListener('click', function(e) {
            if (e.target.tagName !== 'A' && e.target.tagName !== 'BUTTON') {
                const cardTitle = this.querySelector('.card-title');
                if (cardTitle) {
                    showToast(`${cardTitle.textContent} selected`, 'info');
                }
            }
        });
    });
}

function setupMobileMenu() {
    // Create mobile menu button for smaller screens
    if (window.innerWidth <= 768) {
        createMobileMenuButton();
    }

    window.addEventListener('resize', function() {
        if (window.innerWidth <= 768) {
            createMobileMenuButton();
        } else {
            removeMobileMenuButton();
        }
    });
}

function createMobileMenuButton() {
    if (document.querySelector('.mobile-menu-btn')) return;

    const header = document.querySelector('.dashboard-header');
    const menuBtn = document.createElement('button');
    menuBtn.className = 'btn-ghost mobile-menu-btn';
    menuBtn.innerHTML = '<i class="fas fa-bars"></i>';
    menuBtn.style.marginRight = 'auto';

    menuBtn.addEventListener('click', function() {
        const sidebar = document.querySelector('.sidebar');
        sidebar.classList.toggle('mobile-open');
    });

    header.insertBefore(menuBtn, header.firstChild);
}

function removeMobileMenuButton() {
    const menuBtn = document.querySelector('.mobile-menu-btn');
    if (menuBtn) {
        menuBtn.remove();
    }
    const sidebar = document.querySelector('.sidebar');
    sidebar.classList.remove('mobile-open');
}

function loadUserData() {
    const email = localStorage.getItem('loggedInEmail') || '';
    const name = email ? email.split('@')[0] : 'User';
    document.querySelector('.user-name').textContent = name;
    document.querySelector('.user-id').textContent = email || 'Signed in';
}

function showToast(message, type = 'info') {
    // Create toast notification
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <div class="toast-content">
            <i class="fas fa-${getToastIcon(type)}"></i>
            <span>${message}</span>
        </div>
    `;

    // Add styles for toast
    toast.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: var(--glass-bg);
        border: 1px solid var(--glass-border);
        backdrop-filter: blur(14px);
        padding: 1rem;
        border-radius: 8px;
        color: var(--fg);
        z-index: 1000;
        transform: translateX(100%);
        transition: transform 0.3s ease;
        max-width: 300px;
    `;

    document.body.appendChild(toast);

    // Animate in
    setTimeout(() => {
        toast.style.transform = 'translateX(0)';
    }, 100);

    // Remove after 3 seconds
    setTimeout(() => {
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => {
            toast.remove();
        }, 300);
    }, 3000);
}

function getToastIcon(type) {
    const icons = {
        success: 'check-circle',
        error: 'exclamation-circle',
        warning: 'exclamation-triangle',
        info: 'info-circle'
    };
    return icons[type] || 'info-circle';
}

// Export functions for use in other modules (if needed)
window.Dashboard = {
    initDashboard,
    showToast,
    toggleNotifications
};
