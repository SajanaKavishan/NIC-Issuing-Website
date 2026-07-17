/**
 * Payment Confirmation Page Logic
 * 1. Read URL parameters passed after submission.
 * 2. Populate the confirmation card with dynamic data.
 * 3. Adjust the "Next Steps" message based on the payment method.
 */

document.addEventListener('DOMContentLoaded', function() {
    // Helper function to get URL parameter value
    function getUrlParameter(name) {
        name = name.replace(/[\[]/, '\\[').replace(/[\]]/, '\\]');
        const regex = new RegExp('[\\?&]' + name + '=([^&#]*)');
        const results = regex.exec(location.search);
        return results === null ? '' : decodeURIComponent(results[1].replace(/\+/g, ' '));
    }

    const applicationRefId = getUrlParameter('ref') || '-';
    const serviceType = getUrlParameter('service') || '-';
    const paymentMethod = getUrlParameter('method') || '-';
    const amountPaid = getUrlParameter('amount') || '-';


    // --- DOM Population ---

    document.getElementById('ref-id').textContent = applicationRefId;
    document.getElementById('service-type').textContent = serviceType;
    document.getElementById('payment-method').textContent = paymentMethod;
    document.getElementById('amount-paid').textContent = amountPaid;

    const nextStepsTextElement = document.getElementById('next-steps-text');

    // --- Conditional Next Steps based on Payment Method ---
    let nextStepsMessage = '';

    switch (paymentMethod.toLowerCase()) {
        case 'card payment':
        case 'online banking':
            // Card payments are instant, but Online Banking/Deposit are manual for verification of the slip
            nextStepsMessage = `
                Your payment via **${paymentMethod}** is confirmed. Your application is now in the queue.
                We will send an email with a tracking link within **48 hours** after our team verifies your application details and payment (especially the uploaded slip).
            `;
            break;
        case 'bank deposit':
            nextStepsMessage = `
                Your payment via **Bank Deposit** is pending verification. Since you uploaded a deposit slip, 
                our finance team must manually verify the transaction against our bank records.
                This usually takes **1-2 business days**. You will receive an email confirmation once the slip is verified and your application tracking is activated.
            `;
            break;
        default:
            nextStepsMessage = `
                Your submission is complete. You will receive an email within 24 hours with a link to track the status of your application.
            `;
    }

    // Replace the default message with the customized one, applying bolding for emphasis
    nextStepsTextElement.innerHTML = nextStepsMessage.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

    // --- Scroll Animation Trigger (Reusing home.js logic) ---
    // This part ensures the initial animation (scale-in, fade-in, slide-in) triggers immediately
    // since this is a single-screen page and we want the content to animate on load.

    const animatedElements = document.querySelectorAll('.fade-in, .slide-in-left, .slide-in-right, .scale-in');

    function animateOnLoad() {
        animatedElements.forEach(element => {
            // Check if the element has the animation class and add 'visible' immediately
            if (element.classList.length > 0) {
                element.classList.add('visible');
            }
        });
    }

    animateOnLoad();
});

