// Subtle cursor parallax via CSS variables
const root = document.documentElement;
window.addEventListener('pointermove', (e) => {
    const x = e.clientX / window.innerWidth;
    const y = e.clientY / window.innerHeight;
    root.style.setProperty('--mx', x.toFixed(3));
    root.style.setProperty('--my', y.toFixed(3));
});