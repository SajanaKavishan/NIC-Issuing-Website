(function() {
    document.documentElement.style.visibility = 'hidden';

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

    function clearAuth() {
        [
            'authToken',
            'userRole',
            'loggedInEmail',
            'isAdmin',
            'isFinance',
            'isDelivery',
            'isPro',
            'isRecovery',
            'isAssistant'
        ].forEach(key => localStorage.removeItem(key));
    }

    function storeRole(role) {
        ['isAdmin', 'isFinance', 'isDelivery', 'isPro', 'isRecovery', 'isAssistant'].forEach(key => localStorage.removeItem(key));
        localStorage.setItem('userRole', role);
        if (role === 'ADMIN') localStorage.setItem('isAdmin', 'true');
        if (role === 'FINANCE') localStorage.setItem('isFinance', 'true');
        if (role === 'DELIVERY') localStorage.setItem('isDelivery', 'true');
        if (role === 'PRO') localStorage.setItem('isPro', 'true');
        if (role === 'RECOVERY') localStorage.setItem('isRecovery', 'true');
        if (role === 'ASSISTANT') localStorage.setItem('isAssistant', 'true');
    }

    function redirectTo(url) {
        if (!window.location.pathname.endsWith(url)) {
            window.location.replace(url);
        }
    }

    function allowPage() {
        document.documentElement.style.visibility = '';
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
            return;
        }

        fetch('/api/users/session', {
            headers: { 'X-Auth-Token': token, 'Accept': 'application/json' },
            cache: 'no-store'
        })
            .then(async response => {
                if (!response.ok) {
                    throw new Error('Invalid session');
                }
                return response.json();
            })
            .then(user => {
                const serverRole = (user && user.role ? user.role : 'CITIZEN').toUpperCase();
                storeRole(serverRole);
                if (user && user.email) {
                    localStorage.setItem('loggedInEmail', user.email);
                }
                if (requiredRoles.length && !requiredRoles.includes(serverRole)) {
                    redirectTo(roleHome[serverRole] || 'home.html');
                    return;
                }
                allowPage();
            })
            .catch(() => {
                clearAuth();
                redirectTo(`login.html?redirect=${encodeURIComponent(currentPage)}`);
            });
    } catch (_) {
        window.location.replace('login.html');
    }
})();
