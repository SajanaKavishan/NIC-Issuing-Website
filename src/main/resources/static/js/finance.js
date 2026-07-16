// Backend API base
const API_BASE_URL = window.API_BASE_URL || '/api/payments';

// Payments loaded from backend
let payments = [];
// Cached stats from backend
let stats = { totalRevenue: 0, pendingPayments: 0, completedPayments: 0 };
// Track the numeric DB id of the record currently being edited/deleted
let currentEditDbId = null;

let currentEditId = null;

// DOM Elements
const paymentsTableBody = document.getElementById('paymentsTableBody');
const totalRevenue = document.getElementById('totalRevenue');
const pendingPayments = document.getElementById('pendingPayments');
const completedPayments = document.getElementById('completedPayments');
const searchInput = document.getElementById('searchInput');
const statusFilter = document.getElementById('statusFilter');
const paymentModal = document.getElementById('paymentModal');
const deleteModal = document.getElementById('deleteModal');
const paymentForm = document.getElementById('paymentForm');
const modalTitle = document.getElementById('modalTitle');

// Initialize dashboard
document.addEventListener('DOMContentLoaded', function() {
    initializeDashboard();
    setupEventListeners();
    triggerAnimations();
});

async function initializeDashboard() {
    await Promise.all([fetchPayments(), fetchStats()]);
    updateStatistics();
    renderPaymentsTable();
}

function setupEventListeners() {
    // Modal controls
    document.getElementById('addPaymentBtn').addEventListener('click', openAddModal);
    document.getElementById('modalClose').addEventListener('click', closeModal);
    document.getElementById('cancelBtn').addEventListener('click', closeModal);
    document.getElementById('deleteModalClose').addEventListener('click', closeDeleteModal);
    document.getElementById('cancelDeleteBtn').addEventListener('click', closeDeleteModal);
    
    // Form submission
    paymentForm.addEventListener('submit', handleFormSubmit);
    
    // Search and filter
    searchInput.addEventListener('input', handleSearch);
    statusFilter.addEventListener('change', handleFilter);
    
    // Refresh button
    document.getElementById('refreshBtn').addEventListener('click', refreshDashboard);
    
    // Delete confirmation
    document.getElementById('confirmDeleteBtn').addEventListener('click', confirmDelete);
}

function triggerAnimations() {
    const animatedElements = document.querySelectorAll('.fade-in, .slide-in-left, .slide-in-right, .scale-in');
    
    animatedElements.forEach((element, index) => {
        setTimeout(() => {
            element.classList.add('visible');
        }, index * 200);
    });
}

function updateStatistics() {
    totalRevenue.textContent = Number(stats.totalRevenue || 0).toLocaleString();
    pendingPayments.textContent = stats.pendingPayments || 0;
    completedPayments.textContent = stats.completedPayments || 0;
}

function renderPaymentsTable(filteredPayments = payments) {
    paymentsTableBody.innerHTML = '';

    if (filteredPayments.length === 0) {
        paymentsTableBody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 3rem; color: var(--muted);">
                    <i class="fas fa-search" style="font-size: 2rem; margin-bottom: 1rem; display: block;"></i>
                    No payments found matching your criteria
                </td>
            </tr>
        `;
        return;
    }
    
    filteredPayments.forEach(payment => {
        const row = document.createElement('tr');
        const displayId = payment.paymentId || `#${payment.id}`;
        row.innerHTML = `
            <td>${displayId}</td>
            <td>${formatDate(payment.date)}</td>
            <td>${getServiceTypeLabel(payment.serviceType)}</td>
            <td>${getPaymentMethodLabel(payment.paymentMethod)}</td>
            <td>LKR ${(payment.amount || 0).toLocaleString()}</td>
            <td><span class="status-badge status-${payment.status}">${payment.status}</span></td>
            <td>
                <div class="action-buttons">
                    <button class="action-btn edit" onclick="editPayment('${displayId}')" title="Edit">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="action-btn delete" onclick="openDeleteModal('${displayId}')" title="Delete">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </td>
        `;
        paymentsTableBody.appendChild(row);
    });
}

function getServiceTypeLabel(type) {
    const types = {
        'new': 'New NIC',
        'lost': 'Lost NIC',
        'renew': 'Renew NIC'
    };
    return types[type] || type;
}

function getPaymentMethodLabel(method) {
    const methods = {
        'card': 'Card Payment',
        'online': 'Online Banking',
        'deposit': 'Bank Deposit'
    };
    return methods[method] || method;
}

function formatDate(dateString) {
    if (!dateString) return '-';
    try {
        // If it's an ISO without timezone, Date will still parse it
        const d = new Date(dateString);
        if (!isNaN(d.getTime())) {
            const options = { year: 'numeric', month: 'short', day: 'numeric' };
            return d.toLocaleDateString('en-US', options);
        }
        // Fallback to YYYY-MM-DD
        return ('' + dateString).split('T')[0] || '' + dateString;
    } catch (e) {
        return ('' + dateString).split('T')[0] || '' + dateString;
    }
}

function openAddModal() {
    currentEditId = null;
    modalTitle.textContent = 'Add New Payment';
    paymentForm.reset();
    paymentModal.classList.add('active');
}

function editPayment(displayId) {
    const payment = payments.find(p => (p.paymentId || `#${p.id}`) === displayId);
    if (!payment) return;

    currentEditId = payment.paymentId || '';
    currentEditDbId = payment.id;
    modalTitle.textContent = 'Edit Payment';

    // Fill form with payment data
    document.getElementById('paymentId').value = payment.paymentId || '';
    document.getElementById('serviceType').value = payment.serviceType || '';
    document.getElementById('paymentMethod').value = payment.paymentMethod || '';
    document.getElementById('amount').value = payment.amount || '';
    document.getElementById('status').value = payment.status || '';
    document.getElementById('customerInfo').value = payment.customerInfo || '';

    paymentModal.classList.add('active');
}

function openDeleteModal(displayId) {
    const payment = payments.find(p => (p.paymentId || `#${p.id}`) === displayId);
    if (!payment) return;
    currentEditId = payment.paymentId || '';
    currentEditDbId = payment.id;
    deleteModal.classList.add('active');
}

function closeModal() {
    paymentModal.classList.remove('active');
    currentEditId = null;
    currentEditDbId = null;
}

function closeDeleteModal() {
    deleteModal.classList.remove('active');
    currentEditId = null;
    currentEditDbId = null;
}

async function handleFormSubmit(e) {
    e.preventDefault();

    const body = {
        paymentId: document.getElementById('paymentId').value.trim(),
        serviceType: document.getElementById('serviceType').value,
        paymentMethod: document.getElementById('paymentMethod').value,
        amount: parseFloat(document.getElementById('amount').value),
        status: document.getElementById('status').value,
        customerInfo: document.getElementById('customerInfo').value
    };

    try {
        if (currentEditDbId) {
            const res = await fetch(`${API_BASE_URL}/${currentEditDbId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            if (!res.ok) throw new Error('Update failed');
            showNotification('Payment updated successfully!', 'success');
        } else {
            const res = await fetch(API_BASE_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            if (!res.ok) throw new Error('Create failed');
            showNotification('Payment added successfully!', 'success');
        }
        closeModal();
        await refreshDashboard();
    } catch (err) {
        console.error(err);
        showNotification('Action failed. Please try again.', 'error');
    }
}

async function confirmDelete() {
    if (!currentEditDbId) return;
    try {
        const res = await fetch(`${API_BASE_URL}/${currentEditDbId}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Delete failed');
        showNotification('Payment deleted successfully!', 'success');
        closeDeleteModal();
        await refreshDashboard();
    } catch (err) {
        console.error(err);
        showNotification('Delete failed. Please try again.', 'error');
    }
}

function handleSearch() {
    const searchTerm = searchInput.value.toLowerCase();
    const filtered = payments.filter(payment => 
        (payment.paymentId || `#${payment.id}`).toLowerCase().includes(searchTerm) ||
        (payment.customerInfo || '').toLowerCase().includes(searchTerm) ||
        getServiceTypeLabel(payment.serviceType || '').toLowerCase().includes(searchTerm)
    );
    renderPaymentsTable(filtered);
}

function handleFilter() {
    const status = statusFilter.value;
    const filtered = status === 'all' ? payments : payments.filter(payment => payment.status === status);
    renderPaymentsTable(filtered);
}

async function fetchPayments() {
    const res = await fetch(API_BASE_URL);
    if (!res.ok) throw new Error('Failed to load payments');
    payments = await res.json();
}

async function fetchStats() {
    const res = await fetch(`${API_BASE_URL}/stats`);
    if (!res.ok) throw new Error('Failed to load stats');
    stats = await res.json();
}

async function refreshDashboard() {
    await Promise.all([fetchPayments(), fetchStats()]);
    showNotification('Dashboard refreshed!', 'info');
    updateDashboard();
}

function updateDashboard() {
    updateStatistics();
    renderPaymentsTable();
}

function showNotification(message, type = 'info') {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.innerHTML = `
        <i class="fas fa-${getNotificationIcon(type)}"></i>
        <span>${message}</span>
    `;
    
    // Add styles
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: rgba(255,255,255,0.1);
        backdrop-filter: blur(20px);
        border: 1px solid rgba(255,255,255,0.2);
        border-radius: 12px;
        padding: 1rem 1.5rem;
        color: var(--fg);
        display: flex;
        align-items: center;
        gap: 0.75rem;
        z-index: 10000;
        animation: slideInRight 0.3s ease;
        max-width: 400px;
    `;
    
    document.body.appendChild(notification);
    
    // Remove after 3 seconds
    setTimeout(() => {
        notification.style.animation = 'slideOutRight 0.3s ease';
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 300);
    }, 3000);
}

function getNotificationIcon(type) {
    const icons = {
        'success': 'check-circle',
        'error': 'exclamation-circle',
        'warning': 'exclamation-triangle',
        'info': 'info-circle'
    };
    return icons[type] || 'info-circle';
}

// Add CSS for notification animations
const style = document.createElement('style');
style.textContent = `
    @keyframes slideInRight {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOutRight {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
    
    .notification-success {
        border-left: 4px solid var(--success) !important;
    }
    
    .notification-error {
        border-left: 4px solid var(--danger) !important;
    }
    
    .notification-warning {
        border-left: 4px solid var(--warning) !important;
    }
    
    .notification-info {
        border-left: 4px solid var(--a) !important;
    }
`;
document.head.appendChild(style);