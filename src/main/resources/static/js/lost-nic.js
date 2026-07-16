// Form validation and functionality for Lost NIC report
document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('lostNicForm');
    const nicNumberInput = document.getElementById('nicNumber');
    const lostDateInput = document.getElementById('lostDate');
    const birthCertificateInput = document.getElementById('birthCertificate');
    const policeReportInput = document.getElementById('policeReport');
    const paymentBtn = document.getElementById('paymentBtn');
    const helpBtn = document.getElementById('helpBtn');
    const contactNumberInput = document.getElementById('contactNumber');
    let formSubmitted = false;

    // NIC number validation (Sri Lankan format)
    nicNumberInput.addEventListener('change', function() {
        const nicRegex = /^([0-9]{9}[xXvV]|[0-9]{12})$/;
        if (!nicRegex.test(this.value)) {
            this.setCustomValidity('Please enter a valid NIC number (e.g., 901234567V or 199012345678)');
        } else {
            this.setCustomValidity('');
        }
    });

    // Lost date validation (cannot be a future date)
    lostDateInput.addEventListener('change', function() {
        const selectedDate = new Date(this.value);
        const today = new Date();

        if (selectedDate > today) {
            this.setCustomValidity('Lost date cannot be in the future');
        } else {
            this.setCustomValidity('');
        }
    });

    // File validation for birth certificate (PDF only)
    birthCertificateInput.addEventListener('change', function() {
        const file = this.files[0];
        if (file) {
            const fileType = file.type;
            if (fileType !== 'application/pdf') {
                this.setCustomValidity('Please upload a PDF file only');
                this.value = '';
            } else if (file.size > 5 * 1024 * 1024) { // 5MB limit
                this.setCustomValidity('File size must be less than 5MB');
                this.value = '';
            } else {
                this.setCustomValidity('');
            }
        }
    });

    // File validation for police report (PDF only)
    policeReportInput.addEventListener('change', function() {
        const file = this.files[0];
        if (file) {
            const fileType = file.type;
            if (fileType !== 'application/pdf') {
                this.setCustomValidity('Please upload a PDF file only');
                this.value = '';
            } else if (file.size > 5 * 1024 * 1024) { // 5MB limit
                this.setCustomValidity('File size must be less than 5MB');
                this.value = '';
            } else {
                this.setCustomValidity('');
            }
        }
    });

    // Form submission
    form.addEventListener('submit', async function(e) {
        e.preventDefault();

        // Basic validation
        if (!this.checkValidity()) {
            this.reportValidity();
            return;
        }

        // Prepare multipart form data matching backend API
        const formData = new FormData();
        formData.append('nicNumber', nicNumberInput.value.trim());
        formData.append('lostDate', lostDateInput.value);
        formData.append('contactNumber', contactNumberInput.value.trim());
        if (birthCertificateInput.files[0]) {
            formData.append('birthCertificate', birthCertificateInput.files[0]);
        }
        if (policeReportInput.files[0]) {
            formData.append('policeReport', policeReportInput.files[0]);
        }

        // Call backend
        try {
            const response = await fetch('/api/lost-nic/submit', {
                method: 'POST',
                body: formData
            });

            const text = await response.text();
            if (!response.ok) {
                throw new Error(text || 'Submission failed');
            }

            formSubmitted = true;
            paymentBtn.disabled = false;
            alert(text || 'Lost NIC report submitted successfully!');
            document.querySelector('.payment-section').scrollIntoView({ behavior: 'smooth' });
        } catch (err) {
            console.error('Lost NIC submission error:', err);
            alert('Failed to submit Lost NIC report. ' + (err.message || 'Please try again.'));
        }
    });

    // Payment button functionality
    paymentBtn.addEventListener('click', function() {
        if (formSubmitted) {
            alert('Redirecting to payment gateway...');
            // In a real application, this would redirect to a payment page
        } else {
            alert('Please submit your lost NIC report first before making a payment.');
        }
    });

    // Help button functionality
    helpBtn.addEventListener('click', function() {
        // Show help information specific to lost NIC reporting
        alert('Need help with reporting a lost NIC?\n\n1. File a police report at your local station first\n2. Ensure you have your original NIC number\n3. Upload PDF format for both birth certificate and police report (max 5MB each)\n4. Lost date cannot be in the future\n5. Provide accurate current address for delivery\n\nFor further assistance, please contact our support team.');
    });

    // Scroll animation implementation
    const animatedElements = document.querySelectorAll('.fade-in');

    // Function to check if element is in viewport
    function isInViewport(element) {
        const rect = element.getBoundingClientRect();
        return (
            rect.top <= (window.innerHeight || document.documentElement.clientHeight) * 0.9 &&
            rect.bottom >= 0
        );
    }

    // Function to handle scroll events
    function checkScroll() {
        animatedElements.forEach(element => {
            if (isInViewport(element)) {
                element.classList.add('visible');
            }
        });
    }

    // Initial check
    checkScroll();

    // Listen for scroll events
    window.addEventListener('scroll', checkScroll);
});