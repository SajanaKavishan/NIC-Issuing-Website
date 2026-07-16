document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('assistanceForm');
    const emailInput = document.getElementById('email');
    const storedEmailInput = document.getElementById('storedEmail');
    const requestList = document.getElementById('requestList');
    const messageDiv = document.getElementById('message');
    const backBtn = document.getElementById('backBtn');

    // Load or set email from local storage
    let email = localStorage.getItem('assistanceEmail') || '';
    if (email) {
        emailInput.value = email;
        storedEmailInput.value = email;
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const emailValue = emailInput.value.trim();
        const query = document.getElementById('query').value.trim();

        if (emailValue && query) {
            try {
                await axios.post('http://localhost:8080/api/assistance/request', { userId: 1, email: emailValue, query }); // Replace 1 with dynamic userId if available
                localStorage.setItem('assistanceEmail', emailValue); // Remember email
                showMessage('Request submitted successfully!', 'green');
                form.reset();
                await checkReplies(); // Update list immediately
            } catch (error) {
                showMessage('Error submitting request: ' + (error?.response?.data?.message || error.message), 'red');
            }
        } else {
            showMessage('Please fill in both email and query.', 'red');
        }
    });

    async function checkReplies() {
        try {
            const emailValue = emailInput.value.trim() || localStorage.getItem('assistanceEmail') || '';
            if (!emailValue) {
                requestList.innerHTML = '<li>Please enter an email to load your history.</li>';
                return;
            }
            const response = await axios.get(`http://localhost:8080/api/assistance/requestsByEmail?email=${encodeURIComponent(emailValue)}`);
            const requests = response.data;
            requestList.innerHTML = ''; // Clear existing list
            if (Array.isArray(requests) && requests.length > 0) {
                requests.forEach(request => {
                    const li = document.createElement('li');
                    li.dataset.requestId = request.id;
                    li.style.marginBottom = '15px';
                    li.style.padding = '10px';
                    li.style.backgroundColor = '#f0f0f0';
                    li.style.borderRadius = '5px';
                    li.style.display = 'flex';
                    li.style.justifyContent = 'space-between';
                    li.style.alignItems = 'flex-start';

                    const content = document.createElement('div');
                    content.style.flex = '1';
                    content.innerHTML = `<div><strong>Query:</strong> ${escapeHtml(request.query || '')}</div>
                                         <div style="margin-top:8px;"><strong>Reply:</strong> ${escapeHtml(request.reply || 'Awaiting reply...')}</div>`;

                    const actions = document.createElement('div');
                    actions.style.marginLeft = '12px';
                    actions.style.display = 'flex';
                    actions.style.flexDirection = 'column';
                    actions.style.gap = '8px';

                    // Edit button
                    const editBtn = document.createElement('button');
                    editBtn.type = 'button';
                    editBtn.textContent = 'Edit';
                    editBtn.style.background = 'linear-gradient(180deg, #ffd97a, #ffb347)';
                    editBtn.style.color = '#001016';
                    editBtn.style.border = '0';
                    editBtn.style.padding = '6px 10px';
                    editBtn.style.borderRadius = '8px';
                    editBtn.style.cursor = 'pointer';
                    editBtn.addEventListener('click', () => enterEditMode(request, li, content, editBtn, deleteBtn));

                    const deleteBtn = document.createElement('button');
                    deleteBtn.type = 'button';
                    deleteBtn.textContent = 'Delete';
                    deleteBtn.style.background = 'linear-gradient(180deg, #ff9a9e, #ff6f69)';
                    deleteBtn.style.color = '#001016';
                    deleteBtn.style.border = '0';
                    deleteBtn.style.padding = '6px 10px';
                    deleteBtn.style.borderRadius = '8px';
                    deleteBtn.style.cursor = 'pointer';
                    deleteBtn.addEventListener('click', () => deleteRequest(request.id, li));

                    // Append buttons (Edit on top)
                    actions.appendChild(editBtn);
                    actions.appendChild(deleteBtn);
                    li.appendChild(content);
                    li.appendChild(actions);
                    requestList.appendChild(li);
                });
            } else {
                requestList.innerHTML = '<li>No requests found.</li>';
            }
        } catch (error) {
            console.error('Error fetching replies:', error);
            requestList.innerHTML = '<li>Error loading requests. Please try again.</li>';
        }
    }

    // Check replies on load and poll every 10 seconds
    window.addEventListener('load', checkReplies);
    let pollIntervalId = setInterval(checkReplies, 10000); // 10 seconds

    emailInput.addEventListener('change', () => {
        localStorage.setItem('assistanceEmail', emailInput.value.trim());
        storedEmailInput.value = emailInput.value.trim();
        checkReplies(); // Update list when email changes
    });

    backBtn.addEventListener('click', () => {
        window.location.href = 'new-nic.html';
    });

    // Helper: escape HTML to avoid injection
    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    // Helper: show messages in the messageDiv
    function showMessage(msg, color = 'green') {
        if (!messageDiv) return;
        messageDiv.textContent = msg;
        messageDiv.style.display = 'block';
        messageDiv.style.color = color;
        setTimeout(() => { if (messageDiv) messageDiv.style.display = 'none'; }, 4000);
    }

    // Enter inline edit mode for a request
    function enterEditMode(request, li, contentEl, editBtn, deleteBtn) {
        // Pause polling to avoid overwriting user edits
        if (pollIntervalId) { clearInterval(pollIntervalId); pollIntervalId = null; }

        // Hide action buttons while editing
        editBtn.disabled = true;
        deleteBtn.disabled = true;

        const currentQuery = request.query || '';
        const replyText = request.reply || 'Awaiting reply...';

        // Build edit UI
        const textarea = document.createElement('textarea');
        textarea.value = currentQuery;
        textarea.rows = 4;
        textarea.style.width = '100%';
        textarea.style.padding = '8px';
        textarea.style.borderRadius = '6px';

        const replyDiv = document.createElement('div');
        replyDiv.style.marginTop = '8px';
        replyDiv.innerHTML = `<strong>Reply:</strong> ${escapeHtml(replyText)}`;

        const saveBtn = document.createElement('button');
        saveBtn.type = 'button';
        saveBtn.textContent = 'Save';
        saveBtn.style.background = 'linear-gradient(180deg, #7df2ff, #00bfe3)';
        saveBtn.style.color = '#001016';
        saveBtn.style.border = '0';
        saveBtn.style.padding = '6px 10px';
        saveBtn.style.borderRadius = '8px';
        saveBtn.style.cursor = 'pointer';

        const cancelBtn = document.createElement('button');
        cancelBtn.type = 'button';
        cancelBtn.textContent = 'Cancel';
        cancelBtn.style.background = 'rgba(255,255,255,0.06)';
        cancelBtn.style.color = '#001016';
        cancelBtn.style.border = '1px solid rgba(255,255,255,0.08)';
        cancelBtn.style.padding = '6px 10px';
        cancelBtn.style.borderRadius = '8px';
        cancelBtn.style.cursor = 'pointer';

        // Replace contentEl children with edit UI
        contentEl.innerHTML = '';
        const label = document.createElement('div');
        label.innerHTML = '<strong>Edit Query:</strong>';
        contentEl.appendChild(label);
        contentEl.appendChild(textarea);
        contentEl.appendChild(replyDiv);

        // Insert save/cancel into actions area (find actions div)
        const actionsDiv = li.querySelector('div[style*="flex-direction"]');
        // Clear actions and add save/cancel only
        actionsDiv.innerHTML = '';
        actionsDiv.appendChild(saveBtn);
        actionsDiv.appendChild(cancelBtn);

        cancelBtn.addEventListener('click', () => {
            exitEditMode(li, request);
            // resume polling
            if (!pollIntervalId) pollIntervalId = setInterval(checkReplies, 10000);
        });

        saveBtn.addEventListener('click', async () => {
            const newQuery = textarea.value.trim();
            if (newQuery === currentQuery) {
                showMessage('No changes to save.', 'green');
                exitEditMode(li, { ...request, query: currentQuery });
                if (!pollIntervalId) pollIntervalId = setInterval(checkReplies, 10000);
                return;
            }

            saveBtn.disabled = true;
            cancelBtn.disabled = true;
            saveBtn.textContent = 'Saving...';
            try {
                // Call backend to update request (assume PUT endpoint)
                await axios.put(`http://localhost:8080/api/assistance/request/${encodeURIComponent(request.id)}`, { query: newQuery });
                request.query = newQuery; // update local copy
                showMessage('Request updated.', 'green');
                exitEditMode(li, request);
            } catch (err) {
                console.error('Update failed', err);
                showMessage('Failed to update request.', 'red');
                // Re-enable buttons
                saveBtn.disabled = false;
                cancelBtn.disabled = false;
                saveBtn.textContent = 'Save';
            } finally {
                // resume polling
                if (!pollIntervalId) pollIntervalId = setInterval(checkReplies, 10000);
            }
        });
    }

    // Exit edit mode: rebuild a single list item from request data
    function exitEditMode(li, request) {
        const content = li.querySelector('div');
        content.innerHTML = `<div><strong>Query:</strong> ${escapeHtml(request.query || '')}</div>
                             <div style="margin-top:8px;"><strong>Reply:</strong> ${escapeHtml(request.reply || 'Awaiting reply...')}</div>`;

        const actions = li.querySelector('div[style*="flex-direction"]');
        actions.innerHTML = '';

        // Recreate Edit and Delete buttons
        const editBtn = document.createElement('button');
        editBtn.type = 'button';
        editBtn.textContent = 'Edit';
        editBtn.style.background = 'linear-gradient(180deg, #ffd97a, #ffb347)';
        editBtn.style.color = '#001016';
        editBtn.style.border = '0';
        editBtn.style.padding = '6px 10px';
        editBtn.style.borderRadius = '8px';
        editBtn.style.cursor = 'pointer';

        const deleteBtn = document.createElement('button');
        deleteBtn.type = 'button';
        deleteBtn.textContent = 'Delete';
        deleteBtn.style.background = 'linear-gradient(180deg, #ff9a9e, #ff6f69)';
        deleteBtn.style.color = '#001016';
        deleteBtn.style.border = '0';
        deleteBtn.style.padding = '6px 10px';
        deleteBtn.style.borderRadius = '8px';
        deleteBtn.style.cursor = 'pointer';

        editBtn.addEventListener('click', () => enterEditMode(request, li, content, editBtn, deleteBtn));
        deleteBtn.addEventListener('click', () => deleteRequest(request.id, li));

        actions.appendChild(editBtn);
        actions.appendChild(deleteBtn);
    }

    // Delete a request by id, call backend and remove from DOM
    async function deleteRequest(id, listItemElement) {
        if (!id) {
            console.error('No id provided for delete');
            return;
        }
        if (!confirm('Delete this request permanently?')) return;

        try {
            await axios.delete(`http://localhost:8080/api/assistance/request/${encodeURIComponent(id)}`);
            if (listItemElement && listItemElement.remove) listItemElement.remove();
            showMessage(`Request ${id} deleted.`, 'green');
        } catch (error) {
            console.error('Delete failed', error);
            showMessage(`Failed to delete request ${id}.`, 'red');
        }
    }

});
