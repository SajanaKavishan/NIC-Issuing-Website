document.addEventListener("DOMContentLoaded", () => {
    const tableBody = document.getElementById("lostnic-table-body");

    // Fetch data from the backend
    fetch("/api/lost-nic/requests")
        .then(response => {
            if (!response.ok) {
                throw new Error("Failed to fetch lost NIC requests");
            }
            return response.json();
        })
        .then(data => {
            // Populate the table with data
            data.forEach(request => {
                const row = document.createElement("tr");
                // Store id on row for reliable lookup later
                row.dataset.id = request.id;

                const statusText = (request.status || 'PENDING').toUpperCase();
                const statusClass = statusText === 'APPROVED' ? 'status-approved' : (statusText === 'REJECTED' ? 'status-rejected' : 'status-pending');

                // Build secure view URLs that use the new controller endpoint
                const bcViewUrl = `/api/lost-nic/${request.id}/file?type=birthCertificate`;
                const prViewUrl = `/api/lost-nic/${request.id}/file?type=policeReport`;

                // Use small presence icons for the file columns and icon-only action buttons
                const bcIndicator = request.birthCertificatePath ? '<i class="fas fa-paperclip" title="Has Birth Certificate"></i>' : '';
                const prIndicator = request.policeReportPath ? '<i class="fas fa-paperclip" title="Has Police Report"></i>' : '';

                row.innerHTML = `
                    <td>${request.id}</td>
                    <td>${escapeHtml(request.nicNumber || '')}</td>
                    <td>${escapeHtml(request.lostDate || '')}</td>
                    <td>${escapeHtml(request.contactNumber || '')}</td>
                    <td class="file-cell">${bcIndicator}</td>
                    <td class="file-cell">${prIndicator}</td>
                    <td class="status-cell ${statusClass}">${formatStatusLabel(statusText)}</td>
                    <td>
                        <button class="btn-action btn-update" title="Edit" aria-label="Edit"><i class="fas fa-edit"></i></button>
                        <button class="btn-action btn-delete" title="Delete" aria-label="Delete"><i class="fas fa-trash"></i></button>
                        <button class="btn-action btn-approve" title="Approve" aria-label="Approve"><i class="fas fa-check"></i></button>
                        <button class="btn-action btn-reject" title="Reject" aria-label="Reject"><i class="fas fa-times"></i></button>
                        <button class="btn-action btn-view-bc" title="View Birth Certificate" aria-label="View Birth Certificate"><i class="fas fa-file-pdf"></i></button>
                        <button class="btn-action btn-view-pr" title="View Police Report" aria-label="View Police Report"><i class="fas fa-file-alt"></i></button>
                    </td>
                `;

                tableBody.prepend(row);
            });

            // Add event listeners for Update, Delete, Approve and Reject buttons
            tableBody.addEventListener("click", (event) => {
                const clicked = event.target;

                // If a file-cell (the paperclip icon) was clicked, open modal accordingly
                const fileCell = clicked.closest('td.file-cell');
                if (fileCell) {
                    const row = fileCell.closest('tr');
                    if (!row) return;
                    const id = row.dataset.id;
                    const cellIndex = Array.prototype.indexOf.call(row.children, fileCell);
                    if (cellIndex === 4) { // birthCertificate column
                        openPreview(`/api/lost-nic/${id}/file?type=birthCertificate`);
                        return;
                    }
                    if (cellIndex === 5) { // policeReport column
                        openPreview(`/api/lost-nic/${id}/file?type=policeReport`);
                        return;
                    }
                }

                const target = event.target.closest('button') || event.target; // normalize clicks on <i> or button
                const btn = target && target.tagName === 'BUTTON' ? target : (target ? target.closest('button') : null);
                const row = btn ? btn.closest("tr") : null;
                if (!row) return;
                const id = row.dataset.id;

                // View Birth Certificate
                if (btn.classList.contains('btn-view-bc')) {
                    openPreview(`/api/lost-nic/${id}/file?type=birthCertificate`);
                    return;
                }

                // View Police Report
                if (btn.classList.contains('btn-view-pr')) {
                    openPreview(`/api/lost-nic/${id}/file?type=policeReport`);
                    return;
                }

                // Update handler
                if (btn.classList.contains("btn-update")) {
                    // Prompt user for new NIC number and contact number
                    const newNicNumber = prompt("Enter new NIC Number:", row.children[1].textContent);
                    const newContactNumber = prompt("Enter new Contact Number:", row.children[3].textContent);

                    if (newNicNumber && newContactNumber) {
                        // Send update request to the backend (fixed path)
                        fetch(`/api/lost-nic/${id}`, {
                            method: "PUT",
                            headers: {
                                "Content-Type": "application/json",
                            },
                            body: JSON.stringify({
                                nicNumber: newNicNumber,
                                contactNumber: newContactNumber,
                            }),
                        })
                            .then(response => {
                                if (!response.ok) {
                                    throw new Error("Failed to update the request");
                                }
                                return response.json();
                            })
                            .then(updatedRequest => {
                                // Update the table row with new data
                                row.children[1].textContent = updatedRequest.nicNumber || '';
                                row.children[3].textContent = updatedRequest.contactNumber || '';
                                // If server returns status, update it too
                                if (updatedRequest.status) {
                                    setRowStatus(row, updatedRequest.status);
                                }
                                alert("Request updated successfully!");
                            })
                            .catch(error => {
                                console.error("Error updating the request:", error);
                                alert("Failed to update the request. Please try again.");
                            });
                    }
                }

                // Delete handler
                if (btn.classList.contains("btn-delete")) {
                    if (confirm("Are you sure you want to delete this request?")) {
                        // Send delete request to backend (fixed path)
                        fetch(`/api/lost-nic/${id}`, {
                            method: "DELETE"
                        })
                        .then(response => {
                            if (!response.ok) {
                                throw new Error("Failed to delete request");
                            }
                            // Remove row from table
                            row.remove();
                        })
                        .catch(error => {
                            alert(error.message);
                        });
                    }
                }

                // Approve handler
                if (btn.classList.contains("btn-approve")) {
                    if (!confirm('Approve this lost NIC request?')) return;
                    setButtonsDisabled(row, true);
                    updateRequestStatus(id, 'APPROVED')
                        .then(() => {
                            setRowStatus(row, 'APPROVED');
                        })
                        .catch(err => {
                            alert(err.message || 'Failed to approve request');
                        })
                        .finally(() => setButtonsDisabled(row, false));
                }

                // Reject handler
                if (btn.classList.contains("btn-reject")) {
                    if (!confirm('Reject this lost NIC request?')) return;
                    setButtonsDisabled(row, true);
                    updateRequestStatus(id, 'REJECTED')
                        .then(() => {
                            setRowStatus(row, 'REJECTED');
                        })
                        .catch(err => {
                            alert(err.message || 'Failed to reject request');
                        })
                        .finally(() => setButtonsDisabled(row, false));
                }
            });
        })
        .catch(error => {
            console.error("Error loading lost NIC requests:", error);
        });

    // Helper: Update status on server
    function updateRequestStatus(id, status) {
        // NOTE: I assume a status endpoint exists. If your backend expects a different path
        // (e.g., PUT /api/lost-nic/{id} with a full object), we can adapt this easily.
        return fetch(`/api/lost-nic/${id}/status`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status })
        }).then(async res => {
            if (!res.ok) {
                const txt = await res.text().catch(()=>null);
                throw new Error(txt || `Status update failed (${res.status})`);
            }
            return res.json().catch(()=>null);
        });
    }

    // Modal preview functions
    const modal = document.getElementById('filePreviewModal');
    const previewFrame = document.getElementById('previewFrame');
    const modalCloseBtn = document.getElementById('modalCloseBtn');
    const modalOverlay = document.querySelector('#filePreviewModal .modal-overlay');

    function openPreview(url) {
        if (!modal || !previewFrame) {
            // fallback: open in new tab
            window.open(url, '_blank', 'noopener');
            return;
        }
        previewFrame.src = url;
        modal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }

    function closePreview() {
        if (!modal || !previewFrame) return;
        modal.style.display = 'none';
        // clear src to stop loading/playing
        previewFrame.src = '';
        document.body.style.overflow = '';
    }

    if (modalCloseBtn) modalCloseBtn.addEventListener('click', closePreview);
    if (modalOverlay) modalOverlay.addEventListener('click', closePreview);
    // close on escape
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') closePreview();
    });

    function setRowStatus(row, status) {
        const cell = row.querySelector('.status-cell');
        if (!cell) return;
        const s = (status || 'PENDING').toUpperCase();
        cell.textContent = formatStatusLabel(s);
        cell.classList.remove('status-approved','status-rejected','status-pending');
        if (s === 'APPROVED') cell.classList.add('status-approved');
        else if (s === 'REJECTED') cell.classList.add('status-rejected');
        else cell.classList.add('status-pending');
    }

    function formatStatusLabel(status) {
        if (!status) return 'Pending';
        switch (status.toUpperCase()) {
            case 'APPROVED': return 'Approved';
            case 'REJECTED': return 'Rejected';
            default: return 'Pending';
        }
    }

    function setButtonsDisabled(row, disabled) {
        const buttons = row.querySelectorAll('button');
        buttons.forEach(b => b.disabled = disabled);
    }

    // Helper to escape HTML when inserting into cells
    function escapeHtml(str) {
        if (!str) return '';
        return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
    }

});
