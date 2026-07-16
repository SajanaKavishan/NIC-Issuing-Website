document.addEventListener('DOMContentLoaded', function() {
    // --- Global Elements ---
    const dateRangeFilter = document.getElementById('dateRange');
    const deliveryMethodFilter = document.getElementById('deliveryMethod');
    const searchFilter = document.getElementById('search');
    const applyFiltersBtn = document.getElementById('applyFilters');
    const resetFiltersBtn = document.getElementById('resetFilters');
    const generateReportBtn = document.getElementById('generateReport');
    const generateMonthlyBtn = document.getElementById('generateMonthlyReport');
    const tableBody = document.querySelector('#deliveredTable tbody');
    const noResultsDiv = document.querySelector('.no-results');

    // --- Modal Elements ---
    const modal = document.getElementById('detailsModal');
    const closeModalBtn = document.getElementById('closeModal');
    const closeBtnX = modal.querySelector('.close-btn');
    const printBtn = document.getElementById('printDetails');

    // --- Edit Modal Elements ---
    const editModal = document.getElementById('editModal');
    const closeEditModalBtn = document.getElementById('closeEditModal');
    const saveEditBtn = document.getElementById('saveEditBtn');
    const cancelEditBtn = document.getElementById('cancelEditBtn');

    // --- Utility Functions ---

    // Function to populate the table with data
    function renderTable(data) {
        tableBody.innerHTML = '';
        if (!Array.isArray(data) || data.length === 0) {
            noResultsDiv.style.display = 'block';
            return;
        }
        noResultsDiv.style.display = 'none';

        function formatDateForUI(d) {
            if (!d) return '';
            // string like "2025-10-22" or ISO
            if (typeof d === 'string') return d.split('T')[0];
            // array [2025,10,22]
            if (Array.isArray(d)) return d.join('-');
            // object {year:2025, month:10, day:22}
            if (typeof d === 'object') {
                if (d.year && d.month && d.day) {
                    return `${d.year}-${String(d.month).padStart(2,'0')}-${String(d.day).padStart(2,'0')}`;
                }
            }
            // fallback
            try {
                return new Date(d).toISOString().split('T')[0];
            } catch (e) {
                return String(d);
            }
        }

        data.forEach(item => {
            const row = tableBody.insertRow();
            // Render status using span with status class (reverted to previous behavior)
            const statusText = item.status || 'Delivered';
            row.innerHTML = `
                <td>${item.nic}</td>
                <td>${item.recipient}</td>
                <td>${formatDateForUI(item.deliveryDate)}</td>
                <td>${item.method || ''}</td>
                <td><span class="status delivered">${statusText}</span></td>
                <td>
                    <button class="btn-edit" data-nic="${item.nic}">Edit</button>
                    <button class="btn-delete" data-nic="${item.nic}">Delete</button>
                </td>
            `;
        });
        attachEditListeners();
        attachViewDetailsListeners();
        attachDeleteListeners();
    }

    // Delete handler: confirm and call backend DELETE, then update UI
    function attachDeleteListeners() {
        document.querySelectorAll('.btn-delete').forEach(button => {
            button.addEventListener('click', async function() {
                const nic = this.getAttribute('data-nic');
                if (!nic) return;
                const proceed = confirm(`Delete delivery record for NIC ${nic}? This cannot be undone.`);
                if (!proceed) return;
                try {
                    const resp = await fetch(`/api/delivery/nics/${nic}`, { method: 'DELETE' });
                    if (!resp.ok) {
                        let body = null;
                        try { body = await resp.text(); } catch (e) { /* ignore */ }
                        throw new Error(body || `Failed to delete delivery (HTTP ${resp.status})`);
                    }
                    // Remove row from table if present
                    const btn = document.querySelector(`button.btn-delete[data-nic="${nic}"]`);
                    if (btn) {
                        const tr = btn.closest('tr');
                        if (tr) tr.remove();
                    }
                    // If table becomes empty, refresh data to show appropriate message
                    if (tableBody.rows.length === 0) await loadAllDeliveries();
                } catch (err) {
                    console.error(err);
                    alert(err.message || 'Failed to delete delivery. See console for details.');
                }
            });
        });
    }

    // Function to fetch and filter data from backend
    async function filterTable() {
        const dateRange = dateRangeFilter.value;
        const deliveryMethod = deliveryMethodFilter.value;
        const searchTerm = (searchFilter.value || '').trim();

        // Build query params only when they are meaningful (avoid sending defaults)
        const query = new URLSearchParams();
        if (dateRange && dateRange !== 'all') query.append('dateRange', dateRange);
        if (deliveryMethod && deliveryMethod !== 'all') query.append('deliveryMethod', deliveryMethod);
        if (searchTerm) query.append('search', searchTerm);

        const qs = query.toString();
        const url = '/api/delivery/nics' + (qs ? `?${qs}` : '');

        // Debug: show what we're sending
        console.debug('[delivery.filterTable] filters ->', { dateRange, deliveryMethod, searchTerm, url });

        try {
            const response = await fetch(url, { headers: { 'Accept': 'application/json' } });
            console.debug('[delivery.filterTable] response status:', response.status);
            if (!response.ok) throw new Error('Failed to fetch deliveries');
            const data = await response.json();
            if (!Array.isArray(data)) {
                console.warn('[delivery.filterTable] expected array, got:', data);
                renderTable([]);
                return;
            }
            // show response summary in UI for debugging
            if (data.length === 0) {
                noResultsDiv.style.display = 'block';
                noResultsDiv.textContent = `No results (server returned 0 items). HTTP ${response.status}`;
                tableBody.innerHTML = '';
            } else {
                // briefly show a summary then hide
                noResultsDiv.style.display = 'block';
                noResultsDiv.textContent = `Showing ${data.length} result(s). HTTP ${response.status}`;
                renderTable(data);
                setTimeout(() => { noResultsDiv.style.display = 'none'; }, 2500);
            }
        } catch (err) {
            console.error(err);
            // Surface a friendly message and keep the no-results visible
            noResultsDiv.style.display = 'block';
            noResultsDiv.textContent = 'Failed to load deliveries. Check console for details.';
            tableBody.innerHTML = '';
        }
    }

    // Function to generate weekly report
    async function generateWeeklyReport() {
        try {
            const response = await fetch('/api/delivery/nics?dateRange=week', { headers: { 'Accept': 'application/json' } });
             if (!response.ok) throw new Error('Failed to fetch weekly deliveries');
             const data = await response.json();

             if (data.length === 0) {
                 alert('No deliveries found for the past week.');
                 return;
             }

            // Initialize jsPDF
            const { jsPDF } = window.jspdf;
            const doc = new jsPDF();

            // Add title and date range
            const today = new Date();
            const startOfWeek = new Date(today);
            startOfWeek.setDate(today.getDate() - 7);
            doc.setFontSize(16);
            doc.text('Weekly NIC Delivery Report', 10, 10);
            doc.setFontSize(12);
            doc.text(`Date Range: ${startOfWeek.toISOString().split('T')[0]} to ${today.toISOString().split('T')[0]}`, 10, 20);

            // Add summary
            const totalDeliveries = data.length;
            const methodCounts = {};
            data.forEach(item => {
                methodCounts[item.method] = (methodCounts[item.method] || 0) + 1;
            });
            doc.text(`Total Deliveries: ${totalDeliveries}`, 10, 30);
            let yOffset = 40;
            Object.entries(methodCounts).forEach(([method, count]) => {
                doc.text(`${method}: ${count}`, 10, yOffset);
                yOffset += 10;
            });

            // Add table header
            yOffset += 10;
            doc.text('NIC Number', 10, yOffset);
            doc.text('Recipient', 50, yOffset);
            doc.text('Delivery Date', 100, yOffset);
            doc.text('Method', 150, yOffset);
            yOffset += 5;
            doc.line(10, yOffset, 190, yOffset); // Horizontal line
            yOffset += 10;

            // Add table data
            data.forEach(item => {
                doc.text(item.nic, 10, yOffset);
                doc.text(item.recipient, 50, yOffset);
                doc.text(new Date(item.deliveryDate).toISOString().split('T')[0], 100, yOffset);
                doc.text(item.method, 150, yOffset);
                yOffset += 10;
            });

            // Save the PDF
            doc.save(`Weekly_NIC_Delivery_Report_${today.toISOString().split('T')[0]}.pdf`);
        } catch (err) {
            console.error(err);
            alert('Failed to generate report.');
        }
    }

    // Function to generate monthly report (last 30 days)
    async function generateMonthlyReport() {
        try {
            const response = await fetch('/api/delivery/nics?dateRange=month', { headers: { 'Accept': 'application/json' } });
            if (!response.ok) throw new Error('Failed to fetch monthly deliveries');
            const data = await response.json();

            if (data.length === 0) {
                alert('No deliveries found for the past 30 days.');
                return;
            }

            // Initialize jsPDF
            const { jsPDF } = window.jspdf;
            const doc = new jsPDF();

            // Add title and date range
            const today = new Date();
            const startOfMonth = new Date(today);
            startOfMonth.setDate(today.getDate() - 30);
            doc.setFontSize(16);
            doc.text('Monthly NIC Delivery Report', 10, 10);
            doc.setFontSize(12);
            doc.text(`Date Range: ${startOfMonth.toISOString().split('T')[0]} to ${today.toISOString().split('T')[0]}`, 10, 20);

            // Add summary
            const totalDeliveries = data.length;
            const methodCounts = {};
            data.forEach(item => {
                methodCounts[item.method] = (methodCounts[item.method] || 0) + 1;
            });
            doc.text(`Total Deliveries: ${totalDeliveries}`, 10, 30);
            let yOffset = 40;
            Object.entries(methodCounts).forEach(([method, count]) => {
                doc.text(`${method}: ${count}`, 10, yOffset);
                yOffset += 10;
            });

            // Add table header
            yOffset += 10;
            doc.text('NIC Number', 10, yOffset);
            doc.text('Recipient', 50, yOffset);
            doc.text('Delivery Date', 100, yOffset);
            doc.text('Method', 150, yOffset);
            yOffset += 5;
            doc.line(10, yOffset, 190, yOffset); // Horizontal line
            yOffset += 10;

            // Add table data
            data.forEach(item => {
                doc.text(item.nic, 10, yOffset);
                doc.text(item.recipient, 50, yOffset);
                // reuse format, but fallback to string if not parseable
                try {
                    doc.text(new Date(item.deliveryDate).toISOString().split('T')[0], 100, yOffset);
                } catch (e) {
                    doc.text(String(item.deliveryDate), 100, yOffset);
                }
                doc.text(item.method, 150, yOffset);
                yOffset += 10;
            });

            // Save the PDF
            doc.save(`Monthly_NIC_Delivery_Report_${today.toISOString().split('T')[0]}.pdf`);
        } catch (err) {
            console.error(err);
            alert('Failed to generate monthly report.');
        }
    }

    // --- Event Listeners for Filters ---
    applyFiltersBtn.addEventListener('click', filterTable);

    if (generateMonthlyBtn) generateMonthlyBtn.addEventListener('click', generateMonthlyReport);

    resetFiltersBtn.addEventListener('click', function() {
        // Reset to defaults
        // matches HTML default: All Time
        dateRangeFilter.value = 'all';
        deliveryMethodFilter.value = 'all';
        searchFilter.value = '';
        filterTable();
    });

    generateReportBtn.addEventListener('click', generateWeeklyReport);

    // --- Modal Logic ---

    async function showModal(nicNumber) {
        try {
            const response = await fetch(`/api/delivery/nics/${nicNumber}`, { headers: { 'Accept': 'application/json' } });
            if (!response.ok) throw new Error('Failed to fetch delivery details');
            const data = await response.json();

            // Populate Modal Fields
            document.getElementById('modalNicNumber').textContent = data.nic;
            document.getElementById('modalNic').textContent = data.nic;
            document.getElementById('modalAppId').textContent = data.appId;
            document.getElementById('modalRecipient').textContent = data.recipient;
            document.getElementById('modalDeliveryDate').textContent = new Date(data.deliveryDate).toISOString().split('T')[0];
            document.getElementById('modalDeliveryMethod').textContent = data.method;
            document.getElementById('modalContact').textContent = data.contact;
            document.getElementById('modalAddress').textContent = data.address;
            document.getElementById('modalNotes').textContent = data.notes;

            modal.style.display = 'block';
        } catch (err) {
            console.error(err);
        }
    }

    function attachViewDetailsListeners() {
        document.querySelectorAll('.btn-view').forEach(button => {
            button.addEventListener('click', function() {
                const nicNumber = this.getAttribute('data-nic');
                showModal(nicNumber);
            });
        });
    }

    // Close modal handlers
    closeModalBtn.addEventListener('click', () => modal.style.display = 'none');
    closeBtnX.addEventListener('click', () => modal.style.display = 'none');
    window.addEventListener('click', (event) => {
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    });

    // Print functionality for modal
    printBtn.addEventListener('click', function() {
        console.log('Printing details triggered for NIC:', document.getElementById('modalNicNumber').textContent);
    });

    // --- Edit Modal Logic ---
    function attachEditListeners() {
        document.querySelectorAll('.btn-edit').forEach(button => {
            button.addEventListener('click', async function() {
                const nicNumber = this.getAttribute('data-nic');
                try {
                    // Cleanup any previous temporary options
                    function removeTempOptions() {
                        const em = document.getElementById('editMethod');
                        const es = document.getElementById('editStatus');
                        if (em) Array.from(em.querySelectorAll('option[data-temp="true"]')).forEach(o => o.remove());
                        if (es) Array.from(es.querySelectorAll('option[data-temp="true"]')).forEach(o => o.remove());
                    }
                    removeTempOptions();
                    const response = await fetch(`/api/delivery/nics/${nicNumber}`, { headers: { 'Accept': 'application/json' } });
                     if (!response.ok) throw new Error('Failed to fetch delivery details');
                     const data = await response.json();

                    // Populate NIC
                    document.getElementById('editNic').value = data.nic || nicNumber;

                    // Map DB method strings to our select options (tolerant matching)
                    const editMethodSelect = document.getElementById('editMethod');
                    const methodRaw = (data.method || '').toLowerCase();
                    if (methodRaw.includes('postal')) {
                        editMethodSelect.value = 'Postal Service';
                    } else if (methodRaw.includes('courier')) {
                        editMethodSelect.value = 'Private Courier';
                    } else if (methodRaw.includes('pickup') || methodRaw.includes('pick up')) {
                        editMethodSelect.value = 'Office Pickup';
                    } else if (methodRaw.trim().length === 0) {
                        editMethodSelect.value = 'Other';
                    } else {
                        // try exact match; if not present, set to Other and create a temporary option
                        const opt = Array.from(editMethodSelect.options).find(o => o.value.toLowerCase() === data.method?.toLowerCase());
                        if (opt) editMethodSelect.value = opt.value;
                        else {
                            const tmp = document.createElement('option');
                            tmp.value = data.method || 'Other';
                            tmp.textContent = data.method || 'Other';
                            tmp.selected = true;
                            tmp.setAttribute('data-temp', 'true');
                            editMethodSelect.appendChild(tmp);
                        }
                    }

                    // Map status similarly
                    const editStatusSelect = document.getElementById('editStatus');
                    const statusRaw = (data.status || '').toLowerCase();
                    if (statusRaw.includes('deliv')) {
                        editStatusSelect.value = 'Delivered';
                    } else if (statusRaw.includes('pending')) {
                        editStatusSelect.value = 'Pending';
                    } else if (statusRaw.includes('return')) {
                        editStatusSelect.value = 'Returned';
                    } else if (statusRaw.includes('transit') || statusRaw.includes('in transit')) {
                        editStatusSelect.value = 'In Transit';
                    } else if (statusRaw.includes('cancel')) {
                        editStatusSelect.value = 'Cancelled';
                    } else if (statusRaw.trim().length === 0) {
                        editStatusSelect.value = 'Pending';
                    } else {
                        const opt = Array.from(editStatusSelect.options).find(o => o.value.toLowerCase() === data.status?.toLowerCase());
                        if (opt) editStatusSelect.value = opt.value;
                        else {
                            const tmp = document.createElement('option');
                            tmp.value = data.status || 'Pending';
                            tmp.textContent = data.status || 'Pending';
                            tmp.selected = true;
                            tmp.setAttribute('data-temp', 'true');
                            editStatusSelect.appendChild(tmp);
                        }
                    }

                    editModal.style.display = 'block';
                } catch (err) {
                    console.error(err);
                    alert('Failed to load delivery details. See console for details.');
                }
            });
        });
    }

    closeEditModalBtn.addEventListener('click', () => editModal.style.display = 'none');
    cancelEditBtn.addEventListener('click', () => editModal.style.display = 'none');
    // Remove temporary options when modal is closed
    function cleanupEditTemps() {
        const em = document.getElementById('editMethod');
        const es = document.getElementById('editStatus');
        if (em) Array.from(em.querySelectorAll('option[data-temp="true"]')).forEach(o => o.remove());
        if (es) Array.from(es.querySelectorAll('option[data-temp="true"]')).forEach(o => o.remove());
    }
    closeEditModalBtn.addEventListener('click', () => { editModal.style.display = 'none'; cleanupEditTemps(); });
    cancelEditBtn.addEventListener('click', () => { editModal.style.display = 'none'; cleanupEditTemps(); });

    saveEditBtn.addEventListener('click', async function() {
        const nic = document.getElementById('editNic').value;
        const updatedData = {
            method: document.getElementById('editMethod').value,
            status: document.getElementById('editStatus').value
        };

        // Disable button while saving and provide feedback
        const prevText = saveEditBtn.textContent;
        saveEditBtn.disabled = true;
        saveEditBtn.textContent = 'Saving...';
        try {
            // Debug: show what we're sending
            console.debug('[delivery.saveEdit] sending update for', nic, updatedData);
             const response = await fetch(`/api/delivery/nics/${nic}`, {
                 method: 'PUT',
                 headers: { 'Content-Type': 'application/json' },
                 body: JSON.stringify(updatedData)
             });
             if (!response.ok) {
                 // attempt to get server error message
                 let msg = 'Failed to update delivery.';
                 try { const body = await response.text(); if (body) msg += ' ' + body; } catch (e) {}
                 throw new Error(msg);
             }

             // Success: parse returned saved object and use its status/method
             const saved = await response.json().catch(() => null);
             console.debug('[delivery.saveEdit] server returned', saved);
             editModal.style.display = 'none';
             cleanupEditTemps();
             // Refresh table (all) and then briefly show saved status from server
             await loadAllDeliveries(); // Refresh table
             // Immediately update the table row if present so UI reflects the saved status
             if (saved && saved.nic) {
                 const btn = document.querySelector(`button.btn-edit[data-nic="${saved.nic}"]`);
                 if (btn) {
                     const tr = btn.closest('tr');
                     if (tr) {
                         // columns: 0 NIC,1 Recipient,2 Delivery Date,3 Method,4 Status,5 Actions
                         const methodCell = tr.cells[3];
                         const statusCell = tr.cells[4];
                         if (methodCell) methodCell.textContent = saved.method || '';
                         if (statusCell) {
                             statusCell.textContent = String(saved.status || 'Unknown');
                         }
                     }
                 }
             }
         } catch (err) {
             console.error(err);
             alert(err.message || 'Failed to update delivery.');
         } finally {
             saveEditBtn.disabled = false;
             saveEditBtn.textContent = prevText;
         }
     });

    // --- Scroll Animation Implementation ---
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

    // Initial check and event listeners
    checkScroll();
    window.addEventListener('scroll', checkScroll);
    window.addEventListener('resize', checkScroll);

    // --- Initialization ---
    async function loadAllDeliveries() {
        try {
            console.debug('[delivery.loadAll] trying /api/delivery/nics/all');
            let response = await fetch('/api/delivery/nics/all', {headers: {'Accept': 'application/json'}});
            let data = null;
            if (response.ok) {
                try {
                    data = await response.json();
                } catch (e) {
                    data = null;
                }
            }

            // Fallback to /api/delivery/nics if /all endpoint missing or returned unexpected data
            if (!response.ok || !Array.isArray(data)) {
                console.debug('[delivery.loadAll] /nics/all failed or returned non-array, falling back to /api/delivery/nics');
                response = await fetch('/api/delivery/nics', {headers: {'Accept': 'application/json'}});
                if (!response.ok) throw new Error('Failed to fetch deliveries from fallback endpoint');
                data = await response.json();
            }

            if (!Array.isArray(data)) {
                console.warn('[delivery.loadAll] backend returned non-array data:', data);
                noResultsDiv.style.display = 'block';
                noResultsDiv.textContent = 'Server returned unexpected data format. Check console.';
                tableBody.innerHTML = '';
                return;
            }
            renderTable(data);
        } catch (err) {
            console.error(err);
            noResultsDiv.style.display = 'block';
            noResultsDiv.textContent = 'Failed to load deliveries. Open console for details.';
            tableBody.innerHTML = '';
        }
    }

    // Initial data load
    loadAllDeliveries();

});
