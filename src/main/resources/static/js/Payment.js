document.addEventListener('DOMContentLoaded', () => {
  initPaymentPage();
});

function initPaymentPage() {
  bindEvents();
  hydrateTheme();
  populateBookingSummary();
  updateCardPreview();
}

function bindEvents() {
  const form = document.getElementById('paymentForm');
  const payButton = document.getElementById('payButton');
  const methodButtons = document.querySelectorAll('.method-option');
  const themeToggle = document.getElementById('themeToggle');

  methodButtons.forEach((button) => {
    button.addEventListener('click', () => selectMethod(button.dataset.method));
  });

  themeToggle.addEventListener('click', toggleTheme);

  form.addEventListener('submit', handleSubmit);

  const inputs = [
    document.getElementById('cardName'),
    document.getElementById('cardNumber'),
    document.getElementById('expiry'),
    document.getElementById('cvv'),
    document.getElementById('billingEmail'),
    document.getElementById('promoCode'),
    document.getElementById('billingAddress')
  ];

  inputs.forEach((input) => {
    input.addEventListener('input', () => {
      clearFieldError(input.id);
      updateCardPreview();
    });
  });

  document.getElementById('cardName').addEventListener('input', updateCardPreview);
  document.getElementById('cardNumber').addEventListener('input', formatCardNumber);
  document.getElementById('expiry').addEventListener('input', formatExpiry);
  document.getElementById('cvv').addEventListener('input', handleCvvInput);
  document.getElementById('cvv').addEventListener('focus', () => flipCard(true));
  document.getElementById('cvv').addEventListener('blur', () => flipCard(false));

  payButton.addEventListener('click', (event) => {
    const rect = payButton.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    payButton.style.setProperty('--ripple-x', `${x}px`);
    payButton.style.setProperty('--ripple-y', `${y}px`);
    payButton.classList.remove('ripple');
    void payButton.offsetWidth;
    payButton.classList.add('ripple');
  });
}

function selectMethod(method) {
  document.querySelectorAll('.method-option').forEach((button) => {
    button.classList.toggle('active', button.dataset.method === method);
  });

  const card = document.getElementById('paymentCard');
  card.classList.remove('visa', 'mastercard');

  if (method === 'visa') {
    card.classList.add('visa');
    document.getElementById('cardNetwork').textContent = 'VISA';
  } else if (method === 'mastercard') {
    card.classList.add('mastercard');
    document.getElementById('cardNetwork').textContent = 'MC';
  } else if (method === 'card') {
    const detected = detectCardType(document.getElementById('cardNumber').value);
    document.getElementById('cardNetwork').textContent = detected === 'mastercard' ? 'MC' : 'VISA';
  }

  showToast(`Selected ${getMethodLabel(method)}`, 'info');
}

function toggleTheme() {
  document.body.classList.toggle('light');
  const icon = document.querySelector('#themeToggle i');
  if (document.body.classList.contains('light')) {
    icon.className = 'fas fa-sun';
    localStorage.setItem('routex_theme', 'light');
  } else {
    icon.className = 'fas fa-moon';
    localStorage.setItem('routex_theme', 'dark');
  }
}

function hydrateTheme() {
  const savedTheme = localStorage.getItem('routex_theme');
  if (savedTheme === 'light') {
    document.body.classList.add('light');
    document.querySelector('#themeToggle i').className = 'fas fa-sun';
  }
}

function populateBookingSummary() {
  const booking = getStoredBooking();
  const routeLabel = `${booking.origin || '-'} → ${booking.destination || '-'}`;
  document.getElementById('summaryRoute').textContent = routeLabel;

  const summaryItems = [
    { icon: 'fa-map-location-dot', label: 'From → To', value: routeLabel },
    { icon: 'fa-calendar-days', label: 'Travel Date', value: formatDate(booking.travelDate) || 'Not provided' },
    { icon: 'fa-clock', label: 'Departure Time', value: booking.departureTime || '-' },
    { icon: 'fa-clock', label: 'Arrival Time', value: booking.arrivalTime || '-' },
    { icon: 'fa-bus', label: 'Bus Company', value: booking.operatorName || 'RouteX' },
    { icon: 'fa-van-shuttle', label: 'Bus Type', value: formatBusType(booking.busType) || 'Standard' },
    { icon: 'fa-chair', label: 'Selected Seats', value: (booking.seats || []).join(', ') || 'None' },
    { icon: 'fa-user', label: 'Passenger Name', value: booking.passengerName || 'Guest' },
    { icon: 'fa-envelope', label: 'Passenger Email', value: booking.passengerEmail || 'guest@example.com' },
    { icon: 'fa-phone', label: 'Passenger Phone', value: booking.passengerPhone || 'Not provided' }
  ];

  const summaryList = document.getElementById('summaryList');
  summaryList.innerHTML = summaryItems.map((item) => `
    <div class="summary-item">
      <i class="fas ${item.icon}"></i>
      <div>
        <span>${item.label}</span>
        <strong>${item.value}</strong>
      </div>
    </div>
  `).join('');

  const pricePerSeat = Number(booking.pricePerSeat || 0);
  const serviceFee = Number(booking.serviceFee || 0);
  const taxes = Number(booking.taxes || 0);
  const grandTotal = Number(booking.grandTotal || pricePerSeat + serviceFee + taxes);

  document.getElementById('sumPrice').textContent = `Rs. ${pricePerSeat.toLocaleString()}`;
  document.getElementById('sumFee').textContent = `Rs. ${serviceFee.toLocaleString()}`;
  document.getElementById('sumTax').textContent = `Rs. ${taxes.toLocaleString()}`;
  document.getElementById('sumGrand').textContent = `Rs. ${grandTotal.toLocaleString()}`;
}

function getStoredBooking() {
  try {
    return JSON.parse(localStorage.getItem('routex_pending_payment')) || {};
  } catch {
    return {};
  }
}

function updateCardPreview() {
  const name = document.getElementById('cardName').value.trim().toUpperCase() || 'YOUR NAME';
  const number = document.getElementById('cardNumber').value.replace(/\s/g, '');
  const expiry = document.getElementById('expiry').value.trim() || 'MM/YY';
  const cvv = document.getElementById('cvv').value.trim();
  const type = detectCardType(number);

  document.getElementById('cardNamePreview').textContent = name;
  document.getElementById('cardExpiryPreview').textContent = expiry;
  document.getElementById('cvvPreview').textContent = cvv ? '●'.repeat(cvv.length) : '•••';
  document.getElementById('cardNumberPreview').textContent = formatCardPreview(number);

  const network = document.getElementById('cardNetwork');
  if (type === 'mastercard') {
    network.textContent = 'MC';
    document.getElementById('paymentCard').classList.add('mastercard');
    document.getElementById('paymentCard').classList.remove('visa');
  } else if (type === 'visa') {
    network.textContent = 'VISA';
    document.getElementById('paymentCard').classList.add('visa');
    document.getElementById('paymentCard').classList.remove('mastercard');
  } else {
    network.textContent = 'VISA';
    document.getElementById('paymentCard').classList.remove('visa', 'mastercard');
  }
}

function formatCardNumber(event) {
  const input = event.target;
  const digits = input.value.replace(/\D/g, '').slice(0, 16);
  input.value = digits.replace(/(.{4})/g, '$1 ').trim();
  updateCardPreview();
}

function formatExpiry(event) {
  const input = event.target;
  let digits = input.value.replace(/\D/g, '').slice(0, 4);
  if (digits.length > 2) {
    digits = `${digits.slice(0, 2)}/${digits.slice(2)}`;
  }
  input.value = digits;
  updateCardPreview();
}

function handleCvvInput(event) {
  const input = event.target;
  input.value = input.value.replace(/\D/g, '').slice(0, 4);
  updateCardPreview();
}

function flipCard(isFlipped) {
  document.getElementById('paymentCard').classList.toggle('flipped', isFlipped);
}

function validateForm() {
  const errors = [];
  const name = document.getElementById('cardName').value.trim();
  const number = document.getElementById('cardNumber').value.replace(/\s/g, '');
  const expiry = document.getElementById('expiry').value.trim();
  const cvv = document.getElementById('cvv').value.trim();
  const email = document.getElementById('billingEmail').value.trim();

  if (!name || name.length < 3) {
    errors.push({ field: 'cardName', message: 'Please enter your full name' });
  }

  if (!/^[0-9]{16}$/.test(number)) {
    errors.push({ field: 'cardNumber', message: 'Card number must be 16 digits' });
  }

  if (!/^(0[1-9]|1[0-2])\/[0-9]{2}$/.test(expiry)) {
    errors.push({ field: 'expiry', message: 'Use MM/YY format' });
  } else {
    const [month, year] = expiry.split('/').map(Number);
    const now = new Date();
    const currentYear = now.getFullYear() % 100;
    const currentMonth = now.getMonth() + 1;
    if (month < 1 || month > 12 || year < currentYear || (year === currentYear && month < currentMonth)) {
      errors.push({ field: 'expiry', message: 'Card has expired' });
    }
  }

  if (!/^[0-9]{3,4}$/.test(cvv)) {
    errors.push({ field: 'cvv', message: 'CVV must be 3 or 4 digits' });
  }

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errors.push({ field: 'billingEmail', message: 'Enter a valid email address' });
  }

  if (!document.getElementById('billingAddress').value.trim()) {
    const billingAddress = document.getElementById('billingAddress');
    billingAddress.closest('.field').classList.add('invalid');
    billingAddress.closest('.field').querySelector('.error-msg').textContent = 'Address is optional';
  }

  const fields = [
    { id: 'cardName', value: name },
    { id: 'cardNumber', value: number },
    { id: 'expiry', value: expiry },
    { id: 'cvv', value: cvv },
    { id: 'billingEmail', value: email }
  ];

  fields.forEach(({ id, value }) => {
    const field = document.getElementById(id).closest('.field');
    const errorBox = field.querySelector('.error-msg');
    const isEmpty = !value.trim();

    if (isEmpty) {
      field.classList.add('invalid');
      errorBox.textContent = 'This field is required';
    }
  });

  errors.forEach(({ field, message }) => {
    const input = document.getElementById(field);
    const fieldBox = input.closest('.field');
    fieldBox.classList.add('invalid');
    fieldBox.querySelector('.error-msg').textContent = message;
  });

  return errors.length === 0;
}

function clearFieldError(id) {
  const input = document.getElementById(id);
  const field = input.closest('.field');
  if (field) {
    field.classList.remove('invalid');
    field.querySelector('.error-msg').textContent = '';
  }
}

function handleSubmit(event) {
  event.preventDefault();

  if (!validateForm()) {
    showToast('Please fix the highlighted fields', 'error');
    return;
  }

  const payButton = document.getElementById('payButton');
  const progressBox = document.getElementById('progressBox');
  const form = document.getElementById('paymentForm');
  const successState = document.getElementById('successState');

  payButton.disabled = true;
  payButton.classList.add('loading');
  progressBox.classList.add('show');

  const steps = Array.from(document.querySelectorAll('.progress-step'));
  steps.forEach((step) => step.classList.remove('active', 'done'));

  let currentStep = 0;
  const stepTimer = setInterval(() => {
    if (currentStep > 0) {
      steps[currentStep - 1].classList.add('done');
    }
    if (currentStep < steps.length) {
      steps[currentStep].classList.add('active');
    }
    currentStep += 1;
    if (currentStep > steps.length) {
      clearInterval(stepTimer);
    }
  }, 850);

  const storedBooking = getStoredBooking();
  const payload = storedBooking.bookingPayload;

  const token = localStorage.getItem('routex_token');
  const headers = { 'Content-Type': 'application/json' };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  fetch('/api/bookings', {
    method: 'POST',
    headers: headers,
    body: JSON.stringify(payload)
  })
  .then(res => {
    if (!res.ok) {
      throw new Error(`Booking failed (HTTP ${res.status})`);
    }
    return res.json();
  })
  .then(data => {
    if (data.success) {
      const finalBooking = data.data;
      const combined = { ...storedBooking, ...finalBooking };
      localStorage.setItem('routex_pending_payment', JSON.stringify(combined));

      setTimeout(() => {
        form.style.display = 'none';
        progressBox.classList.remove('show');
        successState.classList.add('show');
        payButton.style.display = 'none';
        showToast('Booking confirmed & Email sent!', 'success');
      }, 3200);

      setTimeout(() => {
        window.location.href = 'Ticket.html';
      }, 5000);
    } else {
      throw new Error(data.message || 'Booking confirmation failed');
    }
  })
  .catch(err => {
    clearInterval(stepTimer);
    payButton.disabled = false;
    payButton.classList.remove('loading');
    progressBox.classList.remove('show');
    showToast(err.message || 'Network error', 'error');
  });
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.textContent = message;
  container.appendChild(toast);

  setTimeout(() => {
    toast.remove();
  }, 2800);
}

function formatCardPreview(number) {
  if (!number) return '•••• •••• •••• ••••';
  const visible = number.slice(-4);
  return `•••• •••• •••• ${visible}`;
}

function detectCardType(number) {
  const digits = number.replace(/\D/g, '');
  if (/^4/.test(digits)) return 'visa';
  if (/^(5[1-5]|2[2-7])/.test(digits)) return 'mastercard';
  return 'unknown';
}

function formatBusType(value) {
  const map = {
    SUPER_LUXURY: 'Super Luxury',
    LUXURY: 'Luxury',
    EXPRESS: 'Express'
  };
  return map[value] || value;
}

function formatDate(value) {
  if (!value) return '';
  const date = new Date(value);
  return isNaN(date) ? value : date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  });
}

function getMethodLabel(method) {
  const map = {
    card: 'Credit/Debit Card',
    payhere: 'PayHere',
    ezcash: 'Dialog eZ Cash',
    visa: 'Visa',
    mastercard: 'Mastercard'
  };
  return map[method] || 'Payment Method';
}
