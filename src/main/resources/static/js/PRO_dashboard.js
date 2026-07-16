// --- Global Animation & Parallax Logic (From home.js) ---

// Subtle cursor parallax via CSS variables
const root = document.documentElement;
window.addEventListener('pointermove', e => {
    // Calculate normalized position (0 to 1)
    const x = e.clientX / window.innerWidth;
    const y = e.clientY / window.innerHeight;
    // Set CSS variables for background movement
    root.style.setProperty('--mx', x.toFixed(3));
    root.style.setProperty('--my', y.toFixed(3));
});

// Scroll animation implementation
document.addEventListener('DOMContentLoaded', function() {
    // Select elements marked for animation
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

    // Initial check and event listeners
    checkScroll();
    window.addEventListener('scroll', checkScroll);
    window.addEventListener('resize', checkScroll);
});

// --- PRO Dashboard Application Logic ---

// Items will be loaded from backend
let items = [];

// Reference to table body
const tableBody = document.querySelector('#complaintsTable tbody');
const modal = document.getElementById('responseModal');
const searchInput = document.getElementById('searchFilter');
const typeSelect = document.getElementById('typeFilter');
const statusSelect = document.getElementById('statusFilter');

/**
 * Fetch feedbacks from backend and update items array
 */
async function fetchFeedbacks() {
    try {
        const response = await fetch('/api/feedback');
        if (!response.ok) throw new Error('Failed to fetch feedbacks');
        const data = await response.json();
        // Map backend feedbacks to dashboard format
        items = data.map(fb => ({
            id: fb.id ? `F${String(fb.id).padStart(3, '0')}` : '',
            name: fb.name || '',
            date: fb.date || '', // If date is not present, fallback to empty
            type: fb.type ? (fb.type.toLowerCase() === 'complain' ? 'Complaint' : 'Feedback') : 'Feedback',
            status: 'Pending', // Default status, can be updated if backend provides
            description: fb.message || fb.subject || ''
        }));
        updateOverview();
        renderTable();
    } catch (err) {
        tableBody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:var(--muted); padding: 2rem;">Error loading feedbacks.</td></tr>';
        console.error(err);
    }
}

/**
 * Updates the overview cards with current data counts.
 */
function updateOverview() {
    const total = items.length;
    const pending = items.filter(i => i.status === 'Pending').length;
    const resolved = items.filter(i => i.status === 'Resolved').length;
    const reviewed = items.filter(i => i.status === 'Reviewed' && i.type === 'Feedback').length;

    document.getElementById('totalComplaints').textContent = total;
    document.getElementById('pendingComplaints').textContent = pending;
    document.getElementById('resolvedComplaints').textContent = resolved;
    document.getElementById('reviewedFeedback').textContent = reviewed;
}

/**
 * Filters data based on user input and renders the table rows.
 */
function renderTable() {
    const searchTerm = searchInput.value.toLowerCase();
    const typeFilter = typeSelect.value;
    const statusFilter = statusSelect.value;

    const filteredItems = items.filter(item => {
        const matchesSearch = item.id.toLowerCase().includes(searchTerm) ||
            item.name.toLowerCase().includes(searchTerm);

        const matchesType = typeFilter === 'all' || item.type === typeFilter;

        const matchesStatus = statusFilter === 'all' || item.status === statusFilter;

        return matchesSearch && matchesType && matchesStatus;
    });

    tableBody.innerHTML = ''; // Clear existing rows

    if (filteredItems.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:var(--muted); padding: 2rem;">No submissions match the current filters.</td></tr>';
        return;
    }

    filteredItems.forEach(item => {
        const row = tableBody.insertRow();
        row.innerHTML = `
            <td>${item.id}</td>
            <td>${item.name}</td>
            <td>${item.date}</td>
            <td>${item.type}</td>
            <td><span class="status ${item.status.toLowerCase().replace(' ', '-')}">${item.status}</span></td>
            <td>
                <button class="btn-ghost" onclick="openModal('${item.id}')">View</button>
                <button class="btn-ghost" onclick="updateRecord('${item.id}')">Update</button>
                <button class="btn-ghost" onclick="deleteRecord('${item.id}')">Delete</button>
            </td>
        `;
    });
}

/**
 * Resets all filters and re-renders the table.
 */
function resetFilters() {
    searchInput.value = '';
    typeSelect.value = 'all';
    statusSelect.value = 'all';
    renderTable();
}

/**
 * Opens the response modal, populating it with the selected submission's data.
 * @param {string} id - The ID of the submission to display.
 */
function openModal(id) {
    const item = items.find(c => c.id === id);
    if (!item) return;

    // Populate modal content
    document.getElementById('modalId').textContent = item.id;
    document.getElementById('modalName').textContent = item.name;
    document.getElementById('modalDate').textContent = item.date;
    document.getElementById('modalType').textContent = item.type;
    document.getElementById('modalStatus').textContent = item.status;
    document.getElementById('modalDescription').textContent = item.description;

    // Set initial status dropdown value
    document.getElementById('updateStatus').value = item.status;
    document.getElementById('responseText').value = '';

    modal.style.display = 'block';
}

/**
 * Closes the response modal.
 */
function closeModal() {
    modal.style.display = 'none';
}

/**
 * Saves changes (status update and response message) to the mock data.
 */
function saveChanges() {
    const id = document.getElementById('modalId').textContent;
    const item = items.find(c => c.id === id);

    if (item) {
        const newStatus = document.getElementById('updateStatus').value;
        const responseText = document.getElementById('responseText').value;

        item.status = newStatus;

        // Mock: Add notification
        const notifDiv = document.getElementById('notifications');
        const p = document.createElement('p');

        if (responseText) {
            p.textContent = `• Response sent for ${id} (Status: ${newStatus}) — ${new Date().toLocaleDateString()}`;
            console.log(`PRO responded to ${id}: "${responseText}"`); // Replaced alert()
        } else {
            p.textContent = `• Status updated for ${id} to ${newStatus}.`;
        }

        notifDiv.insertBefore(p, notifDiv.firstChild); // Add new notification at the top

        // Log action and provide confirmation (replaces alert())
        console.log(`Changes saved successfully for Submission ID ${id}. New Status: ${newStatus}`);

        closeModal();
        updateOverview();
        renderTable(); // Refresh table
    }
}

/**
 * Opens the response modal for editing the selected record.
 */
function updateRecord(id) {
    const item = items.find(c => c.id === id);
    if (!item) return;

    // Populate modal content for editing
    document.getElementById('modalId').textContent = item.id;
    document.getElementById('modalName').textContent = item.name;
    document.getElementById('modalDate').textContent = item.date;
    document.getElementById('modalType').textContent = item.type;
    document.getElementById('modalStatus').textContent = item.status;
    document.getElementById('modalDescription').textContent = item.description;

    // Enable editing fields (if needed, convert to input fields)
    document.getElementById('modalDescription').contentEditable = true;
    document.getElementById('updateStatus').disabled = false;
    document.getElementById('responseText').disabled = false;

    // Show save button for update
    document.getElementById('saveUpdateBtn').style.display = 'inline-block';
    modal.style.display = 'block';
}

/**
 * Sends a DELETE request to backend and refreshes table.
 */
async function deleteRecord(id) {
    if (!confirm('Are you sure you want to delete this record?')) return;
    try {
        // Extract numeric ID
        const numericId = id.replace(/^F0*/, '');
        const response = await fetch(`/api/feedback/${numericId}`, { method: 'DELETE' });
        if (!response.ok) throw new Error('Failed to delete record');
        // Remove from items and refresh table
        items = items.filter(c => c.id !== id);
        updateOverview();
        renderTable();
    } catch (err) {
        alert('Error deleting record.');
        console.error(err);
    }
}

/**
 * Saves the updated record to backend.
 */
async function saveUpdate() {
    const id = document.getElementById('modalId').textContent;
    const numericId = id.replace(/^F0*/, '');
    const description = document.getElementById('modalDescription').textContent;
    const status = document.getElementById('updateStatus').value;
    try {
        const response = await fetch(`/api/feedback/${numericId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: description, status })
        });
        if (!response.ok) throw new Error('Failed to update record');
        // Update local item
        const item = items.find(c => c.id === id);
        if (item) {
            item.description = description;
            item.status = status;
        }
        updateOverview();
        renderTable();
        closeModal();
    } catch (err) {
        alert('Error updating record.');
        console.error(err);
    }
}

// --- Reporting and Export Functions (Replaced alert() calls) ---

function logout(){
    console.log("Logging out PRO user...");
}

function generate(type){
    console.log(`Generating ${type} report... (Functionality triggered)`);
}

function exportFile(fmt){
    if (fmt === 'pdf') {
        console.log('Simulating PDF generation for current table data.');
    } else if (fmt === 'excel') {
        let csv = 'ID,Name,Date,Type,Status,Description\n';
        items.forEach(c => csv += `${c.id},${c.name},${c.date},${c.type},${c.status},"${c.description.replace(/"/g, '""')}"\n`);

        // This part would trigger a download in a full browser environment
        const blob = new Blob([csv], {type: 'text/csv;charset=utf-8;'});
        const url = URL.createObjectURL(blob);
        console.log(`Generated CSV data. Simulated download URL: ${url}`);
    }
}

// Initial setup on page load

document.addEventListener('DOMContentLoaded', function() {
    updateOverview();
    fetchFeedbacks(); // Load feedbacks from backend

    // Close modal when clicking outside of it
    window.addEventListener('click', (event) => {
        if (event.target === modal) {
            closeModal();
        }
    });
});
