document.addEventListener('DOMContentLoaded', () => {
    const pendingRequests = document.getElementById('pendingRequests');
    const completedRequests = document.getElementById('completedRequests');
    const totalRequestsEl = document.getElementById('totalRequests');
    const pendingRequestsCountEl = document.getElementById('pendingRequestsCount');
    const completedRequestsCountEl = document.getElementById('completedRequestsCount');
    const loadingEl = document.getElementById('loading');
    const errorEl = document.getElementById('error');
    const messageEl = document.getElementById('message');
    const logoutBtn = document.getElementById('logoutBtn');
    const searchInput = document.getElementById('searchInput');
    const statusFilter = document.getElementById('statusFilter');
    const searchBtn = document.getElementById('searchBtn');
    const modal = document.getElementById('responseModal');
    const notificationsEl = document.getElementById('notifications');
    const initDateEl = document.getElementById('initDate');

    let allRequests = []; // Store all requests for filtering

    // Set initial date
    initDateEl.textContent = new Date().toLocaleDateString();

    // Scroll animations
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
    checkScroll();
    window.addEventListener('scroll', checkScroll);
    window.addEventListener('resize', checkScroll);

    /**
     * Updates the overview cards with current data counts.
     */
    function updateOverview(filteredRequests = allRequests) {
        const total = filteredRequests.length;
        const pending = filteredRequests.filter(r => r.status === 'pending').length;
        const completed = filteredRequests.filter(r => r.status === 'resolved').length;

        totalRequestsEl.textContent = total;
        pendingRequestsCountEl.textContent = pending;
        completedRequestsCountEl.textContent = completed;
    }

    async function fetchRequests() {
        loadingEl.style.display = 'block';
        pendingRequests.innerHTML = '';
        completedRequests.innerHTML = '';
        try {
            const response = await axios.get('http://localhost:8080/api/assistance/all');
            allRequests = response.data;
            if (allRequests.length === 0) {
                messageEl.textContent = 'No assistance requests.';
                messageEl.style.display = 'block';
            } else {
                messageEl.style.display = 'none';
            }

            updateOverview();
            renderFilteredRequests();
        } catch (error) {
            errorEl.textContent = 'Error fetching requests: ' + error.message;
            errorEl.style.display = 'block';
        } finally {
            loadingEl.style.display = 'none';
        }
    }

    window.openModal = (id) => {
        const request = allRequests.find(r => r.id.toString() === id);
        if (!request) return;

        // Populate modal content
        document.getElementById('modalId').textContent = request.id;
        document.getElementById('modalApplicantId').textContent = request.applicantId || 'N/A';
        document.getElementById('modalEmail').textContent = request.email;
        document.getElementById('modalQuery').textContent = request.query;
        document.getElementById('modalStatus').textContent = request.status;
        document.getElementById('modalStatus').className = `status ${request.status}`;
        document.getElementById('updateStatus').value = request.status;
        document.getElementById('responseText').value = '';

        modal.style.display = 'block';
    };

    window.closeModal = () => {
        modal.style.display = 'none';
    };

    window.saveChanges = async () => {
        const id = document.getElementById('modalId').textContent;
        const request = allRequests.find(r => r.id.toString() === id);
        if (!request) return;

        const newStatus = document.getElementById('updateStatus').value;
        const responseText = document.getElementById('responseText').value.trim();

        if (!responseText && newStatus === 'pending') {
            alert('Please enter a response or update status if keeping as pending.');
            return;
        }

        if (!confirm('Save changes?')) return;

        try {
            await axios.post(`http://localhost:8080/api/assistance/reply/${id}`, responseText, {
                headers: { 'Content-Type': 'text/plain' }
            });
            // Assuming API updates status, but if not, simulate
            request.status = newStatus;
            request.reply = responseText || request.reply;

            // Add notification
            const p = document.createElement('p');
            p.textContent = `• Response sent/Status updated for ${id} (Status: ${newStatus}) — ${new Date().toLocaleDateString()}`;
            notificationsEl.insertBefore(p, notificationsEl.firstChild);

            messageEl.textContent = `Changes saved for request ${id}`;
            messageEl.style.display = 'block';

            closeModal();
            fetchRequests(); // Refresh
        } catch (error) {
            errorEl.textContent = 'Error saving changes: ' + error.message;
            errorEl.style.display = 'block';
        }
    };

    window.deleteRequest = async (id, liElement) => {
        if (!confirm('Are you sure you want to delete this request?')) return;
        try {
            await axios.delete(`http://localhost:8080/api/assistance/${id}`);
            const p = document.createElement('p');
            p.textContent = `• Request ${id} deleted — ${new Date().toLocaleDateString()}`;
            notificationsEl.insertBefore(p, notificationsEl.firstChild);
            messageEl.textContent = `Request ${id} deleted successfully`;
            messageEl.style.display = 'block';
            liElement.remove();
            allRequests = allRequests.filter(r => r.id.toString() !== id);
            updateOverview();
        } catch (error) {
            errorEl.textContent = 'Error deleting request: ' + error.message;
            errorEl.style.display = 'block';
        }
    };

    /**
     * Filters and renders requests based on search and status.
     */
    function renderFilteredRequests() {
        const searchId = searchInput.value.trim();
        const statusFilterValue = statusFilter.value;

        let filteredRequests = allRequests;

        // Apply search filter
        if (searchId) {
            filteredRequests = filteredRequests.filter(r => r.id.toString() === searchId);
        }

        // Apply status filter
        if (statusFilterValue !== 'all') {
            filteredRequests = filteredRequests.filter(r => r.status === statusFilterValue);
        }

        pendingRequests.innerHTML = '';
        completedRequests.innerHTML = '';

        if (filteredRequests.length === 0) {
            messageEl.textContent = searchId || statusFilterValue !== 'all' ? 'No requests match the current filters.' : 'No requests.';
            messageEl.style.display = 'block';
            updateOverview(filteredRequests);
            return;
        } else {
            messageEl.style.display = 'none';
        }

        const pending = filteredRequests.filter(r => r.status === 'pending');
        const completed = filteredRequests.filter(r => r.status === 'resolved');

        // Render pending
        pending.forEach(request => {
            const li = document.createElement('li');
            li.innerHTML = `
                <p><strong>Request ID:</strong> ${request.id}</p>
                <p><strong>Applicant ID:</strong> ${request.applicantId || 'N/A'}</p>
                <p><strong>Email:</strong> ${request.email}</p>
                <p><strong>Query:</strong> ${request.query}</p>
                <p><strong>Status:</strong> <span class="status pending">${request.status}</span></p>
                <button class="btn-ghost" onclick="openModal('${request.id}')">View & Reply</button>
            `;
            pendingRequests.appendChild(li);
        });

        // Render completed
        completed.forEach(request => {
            const li = document.createElement('li');
            li.innerHTML = `
                <p><strong>Request ID:</strong> ${request.id}</p>
                <p><strong>Applicant ID:</strong> ${request.applicantId || 'N/A'}</p>
                <p><strong>Email:</strong> ${request.email}</p>
                <p><strong>Query:</strong> ${request.query}</p>
                <p><strong>Status:</strong> <span class="status resolved">${request.status}</span></p>
                <p><strong>Reply:</strong> ${request.reply || 'No reply recorded'}</p>
                <button class="delete-btn" onclick="deleteRequest('${request.id}', this.parentElement)">Delete</button>
            `;
            completedRequests.appendChild(li);
        });

        updateOverview(filteredRequests);
    }

    searchBtn.addEventListener('click', () => {
        renderFilteredRequests();
    });

    statusFilter.addEventListener('change', () => {
        renderFilteredRequests();
    });

    searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') searchBtn.click();
    });

    window.resetFilters = () => {
        searchInput.value = '';
        statusFilter.value = 'all';
        renderFilteredRequests();
        messageEl.style.display = 'none';
    };

    // Modal close on outside click
    window.addEventListener('click', (event) => {
        if (event.target === modal) {
            closeModal();
        }
    });

    logoutBtn.addEventListener('click', () => {
        const p = document.createElement('p');
        p.textContent = `• User logged out — ${new Date().toLocaleDateString()}`;
        notificationsEl.insertBefore(p, notificationsEl.firstChild);
        setTimeout(() => {
            window.location.href = 'login.html';
        }, 500);
    });

    fetchRequests();
});