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

    function showAccessMessage(message) {
        let box = document.getElementById('staffAccessMessage');
        if (!box) {
            box = document.createElement('div');
            box.id = 'staffAccessMessage';
            box.setAttribute('role', 'alert');
            box.style.position = 'fixed';
            box.style.left = '50%';
            box.style.top = '24px';
            box.style.transform = 'translateX(-50%)';
            box.style.zIndex = '99999';
            box.style.maxWidth = 'min(92vw, 520px)';
            box.style.padding = '14px 18px';
            box.style.borderRadius = '10px';
            box.style.border = '1px solid rgba(255,255,255,.24)';
            box.style.background = 'rgba(10, 18, 32, .94)';
            box.style.color = '#fff';
            box.style.boxShadow = '0 18px 42px rgba(0,0,0,.32)';
            box.style.fontFamily = 'Arial, sans-serif';
            box.style.fontSize = '14px';
            box.style.lineHeight = '1.4';
            box.style.textAlign = 'center';
            document.body.appendChild(box);
        }
        box.textContent = message;
    }

    function handleForbidden(message) {
        if (window.__staffForbiddenRedirecting) return;
        window.__staffForbiddenRedirecting = true;

        allowPage();
        const role = getStoredRole();
        const target = roleHome[role] || 'home.html';
        showAccessMessage(message || 'You do not have permission to access this page or action. Redirecting...');

        window.setTimeout(() => redirectTo(target), 1600);
    }

    const originalFetch = window.fetch ? window.fetch.bind(window) : null;
    if (originalFetch) {
        window.fetch = function() {
            return originalFetch.apply(null, arguments).then(response => {
                if (response && response.status === 403) {
                    handleForbidden('Access denied. Your account role cannot perform this action. Redirecting...');
                }
                return response;
            });
        };
    }

    function installAxiosForbiddenHandler() {
        if (!window.axios || window.axios.__staffForbiddenHandlerInstalled) return;
        window.axios.__staffForbiddenHandlerInstalled = true;
        window.axios.interceptors.response.use(
            response => response,
            error => {
                if (error && error.response && error.response.status === 403) {
                    handleForbidden('Access denied. Your account role cannot perform this action. Redirecting...');
                }
                return Promise.reject(error);
            }
        );
    }

    installAxiosForbiddenHandler();
    document.addEventListener('DOMContentLoaded', installAxiosForbiddenHandler);
    window.setTimeout(installAxiosForbiddenHandler, 500);

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
            handleForbidden('This staff page is not available for your role. Redirecting...');
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
                    handleForbidden('This staff page is not available for your role. Redirecting...');
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
