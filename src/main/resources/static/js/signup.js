// Subtle cursor parallax hook (optional)
const root = document.documentElement;
window.addEventListener('pointermove', (e) => {
  const x = e.clientX / window.innerWidth;
  const y = e.clientY / window.innerHeight;
  root.style.setProperty('--mx', x.toFixed(3));
  root.style.setProperty('--my', y.toFixed(3));
});

// Password strength + validation
const form = document.getElementById('signup-form');
const firstName = document.getElementById('firstName');
const lastName  = document.getElementById('lastName');
const email     = document.getElementById('email');
const pw        = document.getElementById('password');
const confirmPw = document.getElementById('confirm');
const terms     = document.getElementById('terms');
const submitBtn = document.getElementById('submitBtn');

const meterFill = document.querySelector('.meter-fill');
const strengthText = document.getElementById('pwStrengthText');
const reqItems = {
  len:   document.querySelector('[data-req="len"]'),
  upper: document.querySelector('[data-req="upper"]'),
  num:   document.querySelector('[data-req="num"]'),
  sym:   document.querySelector('[data-req="sym"]')
};

function emailIsValid(v){
  // Simple, permissive email check
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);
}
function scorePassword(v){
  let score = 0;
  const hasLen   = v.length >= 8;
  const hasUpper = /[A-Z]/.test(v);
  const hasLower = /[a-z]/.test(v);
  const hasNum   = /\d/.test(v);
  const hasSym   = /[^A-Za-z0-9]/.test(v);
  // Base scoring
  score += hasLen ? 25 : Math.min(v.length * 2, 16);
  score += hasUpper ? 15 : 0;
  score += hasLower ? 10 : 0;
  score += hasNum ? 20 : 0;
  score += hasSym ? 30 : 0;
  // Bonus for length > 12
  if (v.length >= 12) score += 10;
  return Math.max(0, Math.min(score, 100));
}

function updatePwUI(){
  const v = pw.value;
  const s = scorePassword(v);
  // Update meter
  meterFill.style.width = s + '%';
  const hue = Math.round((s / 100) * 140); // 0=red -> 140=green
  meterFill.style.setProperty('--hue', hue);

  // Update strength text
  strengthText.textContent = 'Strength: ' + (s < 35 ? 'Weak' : s < 70 ? 'Medium' : 'Strong');
  document.querySelector('.pw-meter').setAttribute('aria-valuenow', String(s));

  // Update requirements checklist
  const ok = {
    len: v.length >= 8,
    upper: /[A-Z]/.test(v),
    num: /\d/.test(v),
    sym: /[^A-Za-z0-9]/.test(v)
  };
  Object.entries(ok).forEach(([key, val]) => {
    reqItems[key].classList.toggle('ok', !!val);
  });
}

function formIsValid(){
  const basics = firstName.value.trim() && lastName.value.trim() && emailIsValid(email.value);
  const pwOk = pw.value.length >= 8 &&
               /[A-Z]/.test(pw.value) &&
               /\d/.test(pw.value) &&
               /[^A-Za-z0-9]/.test(pw.value);
  const match = pw.value && confirmPw.value && pw.value === confirmPw.value;
  return basics && pwOk && match && terms.checked;
}

function updateSubmitState(){
  submitBtn.disabled = !formIsValid();
}

// Show/hide password toggles
document.querySelectorAll('[data-toggle]').forEach(btn => {
  btn.addEventListener('click', () => {
    const targetId = btn.getAttribute('data-toggle');
    const input = targetId === 'password' ? pw : confirmPw;
    const isPw = input.getAttribute('type') === 'password';
    input.setAttribute('type', isPw ? 'text' : 'password');
    btn.setAttribute('aria-label', (isPw ? 'Hide' : 'Show') + ' password');
  });
});

// Wire up events
[pw, confirmPw].forEach(el => el.addEventListener('input', () => { updatePwUI(); updateSubmitState(); }));
[email, firstName, lastName, terms].forEach(el => el.addEventListener('input', updateSubmitState));
terms.addEventListener('change', updateSubmitState);

// Initialize
updatePwUI();
updateSubmitState();

// Demo submit handler (replace with your API call)
form.addEventListener('submit', (e) => {
    e.preventDefault();
    if (!formIsValid()) return;

    submitBtn.disabled = true;
    submitBtn.textContent = 'Creating...';

    const userData = {
        firstName: firstName.value,
        lastName: lastName.value,
        email: email.value,
        password: pw.value
    };

    fetch('http://localhost:8080/api/users/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(userData)
    })
        .then(response => response.text())
        .then(message => {
            alert(message);
            submitBtn.textContent = 'Create account';
            submitBtn.disabled = false;
            form.reset();
            updatePwUI();
            updateSubmitState();
        })
        .catch(error => {
            console.error('Signup error:', error);
            alert('Signup failed. Please try again.');
            submitBtn.textContent = 'Create account';
            submitBtn.disabled = false;
        });
});
