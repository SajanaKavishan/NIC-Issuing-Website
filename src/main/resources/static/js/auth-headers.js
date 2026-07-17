(function() {
    function authHeaders(extra = {}) {
        let token = '';
        try {
            token = localStorage.getItem('authToken') || '';
        } catch (_) {
            token = '';
        }
        return token ? { ...extra, 'X-Auth-Token': token } : extra;
    }

    window.authHeaders = authHeaders;
})();
