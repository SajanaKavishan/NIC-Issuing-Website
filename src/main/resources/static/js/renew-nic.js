// Form validation and functionality for NIC renewal
document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('renewNicForm');
    const oldNicNumberInput = document.getElementById('oldNicNumber');
    const birthdateInput = document.getElementById('birthdate');
    const birthCertificateInput = document.getElementById('birthCertificate');
    const photoInput = document.getElementById('photo');
    const reasonSelect = document.getElementById('reason');
    const otherReasonContainer = document.getElementById('otherReasonContainer');
    const otherReasonInput = document.getElementById('otherReason');
    const paymentBtn = document.getElementById('paymentBtn');
    const helpBtn = document.getElementById('helpBtn');
    const contactNumberInput = document.getElementById('contactNumber');
    let formSubmitted = false;

    // Show/hide other reason field based on selection
    reasonSelect.addEventListener('change', function() {
        if (this.value === 'other') {
            otherReasonContainer.style.display = 'block';
            otherReasonInput.setAttribute('required', 'required');
        } else {
            otherReasonContainer.style.display = 'none';
            otherReasonInput.removeAttribute('required');
        }
    });

    // NIC number validation (Sri Lankan format)
    oldNicNumberInput.addEventListener('change', function() {
        const nicRegex = /^([0-9]{9}[xXvV]|[0-9]{12})$/;
        if (!nicRegex.test(this.value)) {
            this.setCustomValidity('Please enter a valid NIC number (e.g., 901234567V or 199012345678)');
        } else {
            this.setCustomValidity('');
        }
    });

    // Birthdate validation (must be at least 15 years old for NIC)
    birthdateInput.addEventListener('change', function() {
        const birthdate = new Date(this.value);
        const today = new Date();
        let age = today.getFullYear() - birthdate.getFullYear();
        const monthDiff = today.getMonth() - birthdate.getMonth();

        if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthdate.getDate())) {
            age--;
        }

        if (age < 15) {
            this.setCustomValidity('You must be at least 15 years old to have an NIC');
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

    // File validation for photo (JPEG or PNG only)
    photoInput.addEventListener('change', function() {
        const file = this.files[0];
        if (file) {
            const fileType = file.type;
            if (fileType !== 'image/jpeg' && fileType !== 'image/png') {
                this.setCustomValidity('Please upload a JPEG or PNG image only');
                this.value = '';
            } else if (file.size > 2 * 1024 * 1024) { // 2MB limit
                this.setCustomValidity('File size must be less than 2MB');
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
        formData.append('oldNicNumber', oldNicNumberInput.value.trim());
        formData.append('birthdate', birthdateInput.value);
        formData.append('reason', reasonSelect.value);
        if (otherReasonInput.value && reasonSelect.value === 'other') {
            formData.append('otherReason', otherReasonInput.value.trim());
        }
        formData.append('contactNumber', contactNumberInput.value.trim());
        if (birthCertificateInput.files[0]) {
            formData.append('birthCertificate', birthCertificateInput.files[0]);
        }
        if (photoInput.files[0]) {
            formData.append('photo', photoInput.files[0]);
        }

        // Call backend
        try {
            const response = await fetch('/api/renew-nic/submit', {
                method: 'POST',
                body: formData
            });

            const text = await response.text();
            if (!response.ok) {
                throw new Error(text || 'Submission failed');
            }

            formSubmitted = true;
            paymentBtn.disabled = false;
            alert(text || 'NIC renewal request submitted successfully!');
            document.querySelector('.payment-section').scrollIntoView({ behavior: 'smooth' });
        } catch (err) {
            console.error('Renew NIC submission error:', err);
            alert('Failed to submit NIC renewal request. ' + (err.message || 'Please try again.'));
        }
    });

    // Payment button functionality
    paymentBtn.addEventListener('click', function() {
        if (formSubmitted) {
            alert('Redirecting to payment gateway...');
            // In a real application, this would redirect to a payment page
        } else {
            alert('Please submit your renewal request first before making a payment.');
        }
    });

    // Help button functionality
    helpBtn.addEventListener('click', function() {
        // Show help information specific to NIC renewal
        alert('Need help with NIC renewal?\n\n1. Ensure you have your old NIC number ready\n2. Upload PDF format for birth certificate (max 5MB)\n3. Upload JPEG/PNG for photo (max 2MB)\n4. Select the appropriate reason for renewal\n5. If selecting "Other reason", please specify clearly\n6. You must be at least 15 years old to have an NIC\n\nFor further assistance, please contact our support team.');
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