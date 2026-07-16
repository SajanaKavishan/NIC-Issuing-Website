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

function initDashboard() {
    console.log('Dashboard initialized');

    // Simulate loading user data
    setTimeout(() => {
        loadUserData();
    }, 1000);
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

    // Simulate notification updates
    setInterval(() => {
        updateNotificationBadge();
    }, 30000); // Check every 30 seconds
}

function toggleNotifications() {
    // In a real application, this would show a notifications dropdown/modal
    const badge = document.querySelector('.notification-badge');
    const count = parseInt(badge.textContent);

    if (count > 0) {
        badge.textContent = '0';
        badge.style.display = 'none';

        // Show confirmation message
        showToast('Notifications marked as read', 'success');
    } else {
        showToast('No new notifications', 'info');
    }
}

function updateNotificationBadge() {
    // Simulate receiving new notifications
    const badge = document.querySelector('.notification-badge');
    if (Math.random() > 0.7) { // 30% chance of new notification
        const currentCount = parseInt(badge.textContent);
        badge.textContent = currentCount + 1;
        badge.style.display = 'flex';

        // Pulse animation for new notification
        badge.style.animation = 'pulse 1s infinite';
        setTimeout(() => {
            badge.style.animation = '';
        }, 3000);
    }
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

function renderMyApplicationStatus() {
    const list = document.getElementById('applicationStatusList');
    if (!list) return;

    const applications = getCitizenApplications();

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
}

function getCitizenApplications() {
    try {
        const savedApplications = JSON.parse(localStorage.getItem('citizenApplications') || '[]');
        if (Array.isArray(savedApplications) && savedApplications.length) {
            return savedApplications;
        }
    } catch (_) {}

    return [
        {
            type: 'New NIC Application',
            reference: 'NIC2024-00123',
            status: 'PROCESSING',
            submitted: '15 November 2024',
            nextStep: 'Officer review'
        },
        {
            type: 'NIC Renewal',
            reference: 'REN2024-00058',
            status: 'PENDING',
            submitted: '20 November 2024',
            nextStep: 'Document verification'
        },
        {
            type: 'Lost NIC Request',
            reference: 'LST2024-00017',
            status: 'APPROVED',
            submitted: '02 November 2024',
            nextStep: 'Ready for delivery'
        }
    ];
}

function updateApplicationStatusCounts(applications) {
    const inProgressCount = applications.filter(app => ['PROCESSING', 'IN_REVIEW', 'UNDER_REVIEW'].includes(normalizeApplicationStatus(app.status))).length;
    const approvedCount = applications.filter(app => normalizeApplicationStatus(app.status) === 'APPROVED').length;
    const pendingCount = applications.filter(app => normalizeApplicationStatus(app.status) === 'PENDING').length;

    setText('inProgressCount', inProgressCount);
    setText('approvedCount', approvedCount);
    setText('pendingCount', pendingCount);
}

function normalizeApplicationStatus(status) {
    return String(status || 'PENDING').trim().toUpperCase().replace(/\s+/g, '_');
}

function getApplicationStatusClass(status) {
    const normalized = normalizeApplicationStatus(status);
    if (normalized === 'APPROVED' || normalized === 'COMPLETED') return 'approved';
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
                // Handle card click - in real app, this would navigate to detailed view
                const cardTitle = this.querySelector('.card-title');
                if (cardTitle) {
                    console.log(`Navigating to ${cardTitle.textContent} details`);
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
    // Simulate API call to load user data
    const userData = {
        name: 'John Doe',
        nic: '901234567V',
        applications: 3,
        pending: 1,
        completed: 2
    };

    // Update UI with user data
    document.querySelector('.user-name').textContent = userData.name;
    document.querySelector('.user-id').textContent = `NIC: ${userData.nic}`;

    // Show loading completion
    showToast('Dashboard data loaded successfully', 'success');
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
