// --- 1. THEMED NOTIFICATION FUNCTION (Replaces alert()) ---
/**
 * Shows a custom, themed notification box instead of using alert().
 * @param {string} message - The message to display.
 */
function showNotification(message) {
    const notificationBox = document.getElementById('notification-box');
    if (!notificationBox) return;

    notificationBox.textContent = message;
    notificationBox.classList.add('show');

    // Hide after 3 seconds
    setTimeout(() => {
        notificationBox.classList.remove('show');
    }, 3000);
}

// Helper to extract filename from a path
function fileNameFromPath(p) {
    if (!p) return '';
    const parts = p.split(/[\\\/]/);
    return parts[parts.length - 1];
}

// --- 2. SCROLL ANIMATION IMPLEMENTATION (From Homepage) ---
document.addEventListener('DOMContentLoaded', function() {
    // Select all animation classes from the CSS
    const animatedElements = document.querySelectorAll('.fade-in, .slide-in-left, .slide-in-right, .scale-in, .slide-in-up');
    const selectButtons = document.querySelectorAll('.select-btn');
    const selectedRequest = document.getElementById('selectedRequest');
    const notificationBox = document.getElementById('notification-box');
    const lostNicTableBody = document.getElementById('lostnic-table-body');

    // Function to check if element is in viewport
    function isInViewport(element) {
        const rect = element.getBoundingClientRect();
        // Check if 90% of the element's top is visible OR 10% of the bottom is visible
        return (
            rect.top <= (window.innerHeight || document.documentElement.clientHeight) * 0.9 &&
            rect.bottom >= 0
        );
    }

    // Function to handle scroll events and apply visibility
    function checkScroll() {
        animatedElements.forEach(element => {
            if (isInViewport(element)) {
                element.classList.add('visible');
            }
        });
    }

    // Initial check and event listener setup
    checkScroll();
    window.addEventListener('scroll', checkScroll);

    // --- 3. FETCH AND DISPLAY LOST NIC DATA ---
    function fetchLostNicData() {
        if (!lostNicTableBody) return;
        fetch('/api/lost-nic/all')
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(data => {
                // Clear existing table rows
                lostNicTableBody.innerHTML = '';

                // Check if data is empty
                if (!Array.isArray(data) || data.length === 0) {
                    const emptyRow = document.createElement('tr');
                    emptyRow.innerHTML = '<td colspan="7" class="text-center">No lost NIC requests found</td>';
                    lostNicTableBody.appendChild(emptyRow);
                    return;
                }

                // Populate table with data
                data.forEach(item => {
                    const row = document.createElement('tr');
                    row.className = 'request-row fade-in';

                    // Format the date (supports yyyy-mm-dd)
                    let lostDateText = '';
                    try {
                        lostDateText = item.lostDate ? new Date(item.lostDate).toLocaleDateString() : '';
                    } catch (e) {
                        lostDateText = item.lostDate || '';
                    }

                    // Create row content
                    row.innerHTML = `
                        <td><span class="tag tag-a">#${item.id}</span></td>
                        <td>${item.nicNumber || ''}</td>
                        <td>${lostDateText}</td>
                        <td>${item.contactNumber || ''}</td>
                        <td>${fileNameFromPath(item.birthCertificatePath)}</td>
                        <td>${fileNameFromPath(item.policeReportPath)}</td>
                        <td class="action-cell">
                            <button class="btn-ghost view-btn" data-id="${item.id}"><i class="fas fa-eye"></i> View</button>
                            <button class="btn-ghost edit-btn" data-id="${item.id}"><i class="fas fa-pen"></i> Edit</button>
                            <button class="btn-ghost delete-btn" data-id="${item.id}"><i class="fas fa-trash"></i> Delete</button>
                        </td>
                    `;

                    lostNicTableBody.appendChild(row);
                });

                // Re-attach event listeners to new buttons
                attachLostNicRowHandlers();

                // Show notification
                showNotification('Lost NIC data loaded successfully');
            })
            .catch(error => {
                console.error('Error fetching lost NIC data:', error);
                showNotification('Error loading lost NIC data. Please try again.');
            });
    }

    // Attach handlers for edit/delete/view
    function attachLostNicRowHandlers() {
        // View just opens the selected panel with some details for now
        document.querySelectorAll('#lostnic-table-body .view-btn').forEach(button => {
            button.addEventListener('click', function(e) {
                e.preventDefault();
                const row = this.closest('tr');
                const id = this.getAttribute('data-id');
                const nicNumber = row.querySelector('td:nth-child(2)').textContent;
                selectedRequest.style.display = 'block';
                selectedRequest.querySelector('.details-grid p:nth-child(1) .detail-value').textContent = `Request ${id}`;
                selectedRequest.querySelector('.details-grid p:nth-child(2) .detail-value').textContent = nicNumber;
                if (window.innerWidth < 1024) {
                    selectedRequest.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }
                showNotification(`Request #${id} opened.`);
            });
        });

        // Edit handler: prompt for fields and send PUT
        document.querySelectorAll('#lostnic-table-body .edit-btn').forEach(button => {
            button.addEventListener('click', async function(e) {
                e.preventDefault();
                const row = this.closest('tr');
                const id = this.getAttribute('data-id');
                const currentNic = row.querySelector('td:nth-child(2)').textContent.trim();
                const currentDate = row.querySelector('td:nth-child(3)').textContent.trim();
                const currentContact = row.querySelector('td:nth-child(4)').textContent.trim();

                const nicNumber = prompt('NIC Number:', currentNic) ?? currentNic;
                if (!nicNumber) return;
                let lostDate = prompt('Lost Date (YYYY-MM-DD):', (function() {
                    // Try to normalize to yyyy-mm-dd
                    const d = new Date(currentDate);
                    if (!isNaN(d)) {
                        const mm = String(d.getMonth() + 1).padStart(2, '0');
                        const dd = String(d.getDate()).padStart(2, '0');
                        return `${d.getFullYear()}-${mm}-${dd}`;
                    }
                    return currentDate;
                })());
                if (!lostDate) return;
                const contactNumber = prompt('Contact Number:', currentContact) ?? currentContact;
                if (!contactNumber) return;

                try {
                    const resp = await fetch(`/api/lost-nic/${id}`, {
                        method: 'PUT',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ nicNumber, lostDate, contactNumber })
                    });
                    if (!resp.ok) throw new Error('Failed to update');
                    showNotification('Record updated');
                    fetchLostNicData();
                } catch (err) {
                    console.error(err);
                    showNotification('Error updating record');
                }
            });
        });

        // Delete handler
        document.querySelectorAll('#lostnic-table-body .delete-btn').forEach(button => {
            button.addEventListener('click', async function(e) {
                e.preventDefault();
                const id = this.getAttribute('data-id');
                if (!confirm(`Delete lost NIC request #${id}? This cannot be undone.`)) return;
                try {
                    const resp = await fetch(`/api/lost-nic/${id}`, { method: 'DELETE' });
                    if (!resp.ok) throw new Error('Failed to delete');
                    showNotification('Record deleted');
                    fetchLostNicData();
                } catch (err) {
                    console.error(err);
                    showNotification('Error deleting record');
                }
            });
        });
    }

    // Fetch data when page loads
    fetchLostNicData();

    // Add refresh functionality
    const refreshBtn = document.querySelector('.btn-future.w-full.mt-4');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', function() {
            fetchLostNicData();
        });
    }

    // --- 4. DASHBOARD MOCK FUNCTIONALITY ---

    // Simulate selecting a request in the top mock table
    selectButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();

            // Find the request ID and name from the table row (for mock population)
            const row = this.closest('tr');
            const requestId = row.querySelector('.tag').textContent;
            const citizenName = row.querySelector('td:nth-child(2)').textContent;

            // Show the selected request panel
            selectedRequest.style.display = 'block';

            // Populate with mock data
            selectedRequest.querySelector('.details-grid p:nth-child(1) .detail-value').textContent = citizenName;
            selectedRequest.querySelector('.details-grid p:nth-child(2) .detail-value').textContent = requestId.replace('#', 'NIC-'); // Mock NIC gen

            // Scroll the selected request section into view on smaller screens
            if (window.innerWidth < 1024) {
                selectedRequest.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }

            // Show a selection notification
            showNotification(`Request ${requestId} selected for review.`);
        });
    });

    // Approve button functionality (mock)
    document.querySelector('.btn-approve').addEventListener('click', function() {
        showNotification('✅ Request Approved and Forwarded to Printing!');
        console.log('Action: Approve');
    });

    // Reject button functionality (mock)
    document.querySelector('.btn-reject').addEventListener('click', function() {
        showNotification('❌ Request Rejected. Citizen Notified.');
        console.log('Action: Reject');
    });

    // Escalate button functionality (mock)
    document.querySelector('.btn-escalate').addEventListener('click', function() {
        showNotification('⚠️ Request Escalated to Authority!');
        console.log('Action: Escalate');
    });

    // Manual Verify button functionality (mock)
    const manualVerifyButton = document.querySelector('.btn-manual-verify');
    if (manualVerifyButton) {
        manualVerifyButton.addEventListener('click', function() {
            showNotification('👤 Manual Verification Initiated!');
            console.log('Action: Manual Verify');
        });
    }

    // Verification tools (mock actions)
    const toolButtons = document.querySelectorAll('.right-panel .tool-btn');
    if (toolButtons) {
        toolButtons.forEach(button => {
            button.addEventListener('click', function() {
                showNotification(`Tool Activated: ${this.textContent}`);
                console.log(`Tool Action: ${this.textContent}`);
            });
        });
    }

    // Prevent default form submission on search input
    const searchInput = document.querySelector('.search-box input');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                showNotification(`Searching for: ${this.value}...`);
            }
        });
    }
});
