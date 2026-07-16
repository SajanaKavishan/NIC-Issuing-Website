// Cursor parallax for aurora pivot
const root = document.documentElement;
window.addEventListener('pointermove', (e) => {
    const x = e.clientX / window.innerWidth;
    const y = e.clientY / window.innerHeight;
    root.style.setProperty('--mx', x.toFixed(3));
    root.style.setProperty('--my', y.toFixed(3));
});

// Email helper
function emailIsValid(v){
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);
}

// Show/hide password toggle using data-toggle="inputId"
function initVisibilityToggles(){
    document.querySelectorAll('[data-toggle]').forEach(btn => {
        const targetId = btn.getAttribute('data-toggle');
        const input = document.getElementById(targetId);
        if (!input) return;
        btn.addEventListener('click', () => {
            const isPw = input.getAttribute('type') === 'password';
            input.setAttribute('type', isPw ? 'text' : 'password');
            btn.setAttribute('aria-label', (isPw ? 'Hide' : 'Show') + ' password');
        });
    });
}
initVisibilityToggles();

// Login validation
const form = document.getElementById('login-form');
const email = document.getElementById('loginEmail');
const pw = document.getElementById('loginPassword');
const submitBtn = document.getElementById('loginSubmit');

// Optional post-login redirect support, e.g., login.html?redirect=new-nic.html
let redirectTarget = null;
try {
    const params = new URLSearchParams(window.location.search);
    const r = params.get('redirect');
    if (r && typeof r === 'string') {
        redirectTarget = r;
    }
} catch (_) { redirectTarget = null; }

function validLogin(){
    return emailIsValid(email.value) && pw.value.length > 0;
}
function updateState(){
    submitBtn.disabled = !validLogin();
}

[email, pw].forEach(el => el.addEventListener('input', updateState));
updateState();

form.addEventListener('submit', (e) => {
    e.preventDefault();
    if (!validLogin()) return;
    submitBtn.disabled = true;
    const old = submitBtn.textContent;
    submitBtn.textContent = 'Signing in...';
    // Check for admin credentials (hardcoded)
    if (email.value === 'admin@gmail.com' && pw.value === '1234') {
        setTimeout(() => {
            // Persist admin session flags for navbar
            try {
                localStorage.setItem('isAdmin', 'true');
                localStorage.setItem('loggedInEmail', 'admin');
            } catch (e) {}
            submitBtn.textContent = old;
            submitBtn.disabled = false;
            window.location.href = 'admin-dashboard.html';
        }, 800);
    } else if (email.value === 'finance@gmail.com' && pw.value === '1234') {
        setTimeout(() => {
            // Persist finance session flags for navbar (if needed)
            try {
                localStorage.setItem('isFinance', 'true');
                localStorage.setItem('loggedInEmail', 'finance');
                localStorage.removeItem('isAdmin');
            } catch (e) {}
            submitBtn.textContent = old;
            submitBtn.disabled = false;
            window.location.href = 'finance.html';
        }, 800);
    } else if (email.value === 'delivery@gmail.com' && pw.value === '1234') {
        setTimeout(() => {
            // Persist delivery session flags for navbar (if needed)
            try {
                localStorage.setItem('isDelivery', 'true');
                localStorage.setItem('loggedInEmail', 'delivery');
                localStorage.removeItem('isAdmin');
                localStorage.removeItem('isFinance');
            } catch (e) {}
            submitBtn.textContent = old;
            submitBtn.disabled = false;
            window.location.href = 'delivery.html';
        }, 800);
    } else if (email.value === 'pro@gmail.com' && pw.value === '1234') {
        setTimeout(() => {
            // Persist pro session flags for navbar (if needed)
            try {
                localStorage.setItem('isPro', 'true');
                localStorage.setItem('loggedInEmail', 'pro');
                localStorage.removeItem('isAdmin');
                localStorage.removeItem('isFinance');
                localStorage.removeItem('isDelivery');
            } catch (e) {}
            submitBtn.textContent = old;
            submitBtn.disabled = false;
            window.location.href = 'PRO_dashboard.html';
        }, 800);
    } else if (email.value === 'recovery@gmail.com' && pw.value === '1234') {
        setTimeout(() => {
            // Persist recovery session flags for navbar (if needed)
            try {
                localStorage.setItem('isRecovery', 'true');
                localStorage.setItem('loggedInEmail', 'recovery');
                localStorage.removeItem('isAdmin');
                localStorage.removeItem('isFinance');
                localStorage.removeItem('isDelivery');
                localStorage.removeItem('isPro');
            } catch (e) {}
            submitBtn.textContent = old;
            submitBtn.disabled = false;
            window.location.href = 'recovery-dashboard.html';
        }, 800);
    } else if (email.value === 'assistant@gmail.com' && pw.value === '1234') {
        setTimeout(() => {
            // Persist assistant session flags for navbar (if needed)
            try {
                localStorage.setItem('isAssistant', 'true');
                localStorage.setItem('loggedInEmail', 'assistant');
                localStorage.removeItem('isAdmin');
                localStorage.removeItem('isFinance');
                localStorage.removeItem('isDelivery');
                localStorage.removeItem('isPro');
                localStorage.removeItem('isRecovery');
            } catch (e) {}
            submitBtn.textContent = old;
            submitBtn.disabled = false;
            window.location.href = 'assistant-dashboard.html';
        }, 800);
    } else {
        fetch('http://localhost:8080/api/users/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: email.value, password: pw.value })
        })
        .then(res => res.text())
        .then(text => {
            if (text && text.toLowerCase().includes('success')) {
                // Successful login
                submitBtn.textContent = old;
                submitBtn.disabled = false;
                // Store a simple flag or email (replace with real auth token later)
                try {
                    localStorage.setItem('loggedInEmail', email.value);
                    localStorage.removeItem('isAdmin'); // ensure admin flag is cleared for normal users
                } catch (e) {}
                alert('Login successful');
                window.location.href = redirectTarget || 'home.html';
            } else {
                alert(text || 'Invalid credentials');
                submitBtn.textContent = old;
                submitBtn.disabled = false;
            }
        })
        .catch(err => {
            console.error('Login error:', err);
            alert('Login failed. Please try again.');
            submitBtn.textContent = old;
            submitBtn.disabled = false;
        });
    }
});