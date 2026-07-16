// Form validation and functionality
document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('nicForm');
    const nameWithInitialsInput = document.getElementById('nameWithInitials');
    const genderInputs = document.querySelectorAll('input[name="gender"]');
    const ageInput = document.getElementById('age');
    const civilStatusInput = document.getElementById('civilStatus');
    const professionInput = document.getElementById('profession');
    const birthdateInput = document.getElementById('birthdate');
    const addressInput = document.getElementById('address');
    const contactNumberInput = document.getElementById('contactNumber');
    const birthCertificateInput = document.getElementById('birthCertificate');
    const photoInput = document.getElementById('photo');
    const paymentBtn = document.getElementById('paymentBtn');
    const helpBtn = document.getElementById('helpBtn');
    let formSubmitted = false;

    // Age validation
    ageInput.addEventListener('change', function() {
        if (this.value < 18) {
            this.setCustomValidity('You must be at least 18 years old to apply for an NIC');
        } else {
            this.setCustomValidity('');
        }
    });

    // Birthdate validation to ensure user is at least 18
    birthdateInput.addEventListener('change', function() {
        const birthdate = new Date(this.value);
        const today = new Date();
        let age = today.getFullYear() - birthdate.getFullYear();
        const monthDiff = today.getMonth() - birthdate.getMonth();

        if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthdate.getDate())) {
            age--;
        }

        if (age < 18) {
            this.setCustomValidity('You must be at least 18 years old to apply for an NIC');
            ageInput.value = '';
        } else {
            this.setCustomValidity('');
            ageInput.value = age;
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

        // Determine selected gender
        let genderValue = '';
        genderInputs.forEach(input => { if (input.checked) genderValue = input.value; });

        // Prepare multipart form data matching backend API
        const formData = new FormData();
        formData.append('nameWithInitials', (nameWithInitialsInput.value || '').trim());
        formData.append('gender', genderValue);
        formData.append('age', ageInput.value);
        formData.append('civilStatus', civilStatusInput.value);
        formData.append('profession', (professionInput.value || '').trim());
        formData.append('birthdate', birthdateInput.value);
        formData.append('address', (addressInput.value || '').trim());
        formData.append('contactNumber', (contactNumberInput.value || '').trim());
        if (birthCertificateInput.files[0]) {
            formData.append('birthCertificate', birthCertificateInput.files[0]);
        }
        if (photoInput.files[0]) {
            formData.append('photo', photoInput.files[0]);
        }

        // Call backend
        try {
            const response = await fetch('/api/new-nic/submit', {
                method: 'POST',
                body: formData
            });

            const text = await response.text();
            if (!response.ok) {
                throw new Error(text || 'Submission failed');
            }

            formSubmitted = true;
            paymentBtn.disabled = false;
            alert(text || 'New NIC application submitted successfully!');
            document.querySelector('.payment-section').scrollIntoView({ behavior: 'smooth' });
        } catch (err) {
            console.error('New NIC submission error:', err);
            alert('Failed to submit New NIC application. ' + (err.message || 'Please try again.'));
        }
    });

    // Payment button functionality
    paymentBtn.addEventListener('click', function() {
        if (formSubmitted) {
            alert('Redirecting to payment gateway...');
            // In a real application, this would redirect to a payment page
        } else {
            alert('Please submit your application first before making a payment.');
        }
    });

    // Help button functionality
    helpBtn.addEventListener('click', function() {
        // Show help information
        alert('Need help with your application?\n\n1. Ensure all fields are filled accurately\n2. Upload PDF format for birth certificate (max 5MB)\n3. Upload JPEG/PNG for photo (max 2MB)\n4. You must be 18 years or older to apply\n\nFor further assistance, please contact our support team.');
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