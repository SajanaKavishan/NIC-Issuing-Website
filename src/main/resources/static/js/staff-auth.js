(function() {
    const roleHome = {
        ADMIN: 'admin-dashboard.html',
        FINANCE: 'finance.html',
        DELIVERY: 'delivery.html',
        PRO: 'PRO_dashboard.html',
        RECOVERY: 'recovery-dashboard.html',
        ASSISTANT: 'assistant-dashboard.html',
        CITIZEN: 'home.html'
    };

    function getStoredRole() {
        const role = localStorage.getItem('userRole');
        if (role) return role.toUpperCase();
        if (localStorage.getItem('isAdmin') === 'true') return 'ADMIN';
        if (localStorage.getItem('isFinance') === 'true') return 'FINANCE';
        if (localStorage.getItem('isDelivery') === 'true') return 'DELIVERY';
        if (localStorage.getItem('isPro') === 'true') return 'PRO';
        if (localStorage.getItem('isRecovery') === 'true') return 'RECOVERY';
        if (localStorage.getItem('isAssistant') === 'true') return 'ASSISTANT';
        return 'CITIZEN';
    }

    function redirectTo(url) {
        if (!window.location.pathname.endsWith(url)) {
            window.location.replace(url);
        }
    }

    try {
        const script = document.currentScript;
        const requiredRoles = (script && script.dataset.requiredRoles ? script.dataset.requiredRoles : '')
            .split(',')
            .map(role => role.trim().toUpperCase())
            .filter(Boolean);

        const currentPage = window.location.pathname.split('/').pop() || 'home.html';
        const token = localStorage.getItem('authToken');
        const role = getStoredRole();

        if (!token) {
            redirectTo(`login.html?redirect=${encodeURIComponent(currentPage)}`);
            return;
        }

        if (requiredRoles.length && !requiredRoles.includes(role)) {
            redirectTo(roleHome[role] || 'home.html');
        }
    } catch (_) {
        window.location.replace('login.html');
    }
})();
