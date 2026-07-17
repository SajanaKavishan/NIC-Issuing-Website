(function() {
    const AUTH_STORAGE_KEYS = [
        'loggedInEmail',
        'authToken',
        'userRole',
        'isAdmin',
        'isFinance',
        'isDelivery',
        'isPro',
        'isRecovery',
        'isAssistant'
    ];

    function authHeaders(extra = {}) {
        let token = '';
        try {
            token = localStorage.getItem('authToken') || '';
        } catch (_) {
            token = '';
        }
        return token ? { ...extra, 'X-Auth-Token': token } : extra;
    }

    function clearAuthState(options = {}) {
        try {
            AUTH_STORAGE_KEYS.forEach(key => localStorage.removeItem(key));
            if (options.clearAllLocalStorage) {
                localStorage.clear();
            }
        } catch (_) {}

        if (options.clearSessionStorage !== false) {
            try { sessionStorage.clear(); } catch (_) {}
        }
    }

    function authLogout(options = {}) {
        clearAuthState(options);

        const redirectTo = options.redirectTo || 'login.html';
        if (options.reloadIfLogin && window.location.pathname.endsWith('login.html')) {
            window.location.reload();
            return;
        }

        window.location.href = redirectTo;
    }

    window.authHeaders = authHeaders;
    window.clearAuthState = clearAuthState;
    window.authLogout = authLogout;
})();
