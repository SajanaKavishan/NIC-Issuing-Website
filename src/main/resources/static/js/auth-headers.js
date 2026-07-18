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

    function isApiEnvelope(value) {
        return value &&
            typeof value === 'object' &&
            Object.prototype.hasOwnProperty.call(value, 'success') &&
            Object.prototype.hasOwnProperty.call(value, 'data') &&
            Object.prototype.hasOwnProperty.call(value, 'errors') &&
            Object.prototype.hasOwnProperty.call(value, 'timestamp') &&
            Object.prototype.hasOwnProperty.call(value, 'path');
    }

    function unwrapApiResponse(value) {
        if (!isApiEnvelope(value)) return value;
        return value.success ? value.data : value;
    }

    function apiTextFromEnvelope(value, fallback) {
        if (!isApiEnvelope(value)) return fallback;
        if (typeof value.data === 'string') return value.data;
        if (value.data == null && value.message) return value.message;
        return JSON.stringify(value.data);
    }

    function installFetchUnwrapper() {
        if (!window.Response || window.Response.prototype.__apiEnvelopeUnwrapped) return;

        const originalJson = window.Response.prototype.json;
        const originalText = window.Response.prototype.text;

        window.Response.prototype.json = function() {
            return originalJson.call(this).then(unwrapApiResponse);
        };

        window.Response.prototype.text = function() {
            return originalText.call(this).then(text => {
                try {
                    return apiTextFromEnvelope(JSON.parse(text), text);
                } catch (_) {
                    return text;
                }
            });
        };

        Object.defineProperty(window.Response.prototype, '__apiEnvelopeUnwrapped', {
            value: true,
            configurable: false
        });
    }

    function installAxiosUnwrapper() {
        if (!window.axios || window.axios.__apiEnvelopeUnwrapped) return;

        window.axios.interceptors.response.use(
            response => {
                response.data = unwrapApiResponse(response.data);
                return response;
            },
            error => {
                if (error && error.response) {
                    error.response.data = unwrapApiResponse(error.response.data);
                }
                return Promise.reject(error);
            }
        );

        Object.defineProperty(window.axios, '__apiEnvelopeUnwrapped', {
            value: true,
            configurable: false
        });
    }

    installFetchUnwrapper();
    installAxiosUnwrapper();
    window.addEventListener('load', installAxiosUnwrapper);

    window.authHeaders = authHeaders;
    window.clearAuthState = clearAuthState;
    window.authLogout = authLogout;
    window.unwrapApiResponse = unwrapApiResponse;
})();
