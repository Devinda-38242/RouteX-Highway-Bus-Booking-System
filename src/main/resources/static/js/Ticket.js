document.addEventListener('DOMContentLoaded', () => {
  initTicketPage();
});

function initTicketPage() {
  hydrateTheme();
  bindEvents();
  populateTicket();
  animateIntro();
  createConfetti();
}

function bindEvents() {
  document.getElementById('themeToggle').addEventListener('click', toggleTheme);
  document.getElementById('downloadPdfBtn').addEventListener('click', downloadPdf);
  document.getElementById('printBtn').addEventListener('click', printTicket);
  document.getElementById('shareBtn').addEventListener('click', shareTicket);
  document.getElementById('mapBtn').addEventListener('click', openMap);
  document.getElementById('emailBtn').addEventListener('click', emailTicket);
  document.getElementById('homeBtn').addEventListener('click', () => {
    window.location.href = 'index.html';
  });
}

function hydrateTheme() {
  if (localStorage.getItem('routex_theme') === 'light') {
    document.body.classList.add('light');
    document.querySelector('#themeToggle i').className = 'fas fa-sun';
  }
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

function populateTicket() {
  const booking = getBooking();
  const bookingId = booking.bookingReference || generateBookingId();
  const paymentId = generatePaymentId();
  const passengerCount = Array.isArray(booking.seats) ? booking.seats.length : 1;
  const journeyDate = booking.travelDate || new Date().toISOString().split('T')[0];
  const departureTime = booking.departureTime || '08:30';
  const arrivalTime = booking.arrivalTime || '12:45';

  document.getElementById('fromText').textContent = booking.origin || 'Colombo';
  document.getElementById('toText').textContent = booking.destination || 'Kandy';
  document.getElementById('dateText').textContent = formatDate(journeyDate);
  document.getElementById('departureText').textContent = departureTime;
  document.getElementById('arrivalText').textContent = arrivalTime;
  document.getElementById('companyText').textContent = booking.operatorName || 'RouteX Express';
  document.getElementById('busNameText').textContent = booking.busName || 'Blue Horizon';
  document.getElementById('busTypeText').textContent = formatBusType(booking.busType) || 'Luxury';
  document.getElementById('busNumberText').textContent = booking.busNumber || 'NB-402';
  document.getElementById('passengerNameText').textContent = booking.passengerName || 'Guest Passenger';
  document.getElementById('phoneText').textContent = booking.passengerPhone || '+94 77 000 0000';
  document.getElementById('emailText').textContent = booking.passengerEmail || 'guest@routex.com';
  document.getElementById('seatsText').textContent = (booking.seats || []).join(', ') || 'A1, A2';
  document.getElementById('passengerCountText').textContent = passengerCount;
  document.getElementById('bookingIdText').textContent = bookingId;
  document.getElementById('paymentIdText').textContent = paymentId;
  document.getElementById('paymentMethodText').textContent = booking.paymentMethod || 'Credit/Debit Card';
  document.getElementById('amountText').textContent = `Rs. ${(Number(booking.grandTotal || booking.totalAmount || 0)).toLocaleString()}`;

  startCountdown(journeyDate, departureTime);
  generateQRCode(`${bookingId}|${booking.passengerName}|${booking.operatorName}|${(booking.seats || []).join(',')}`);
  generateBarcode();
}

function getBooking() {
  try {
    const saved = JSON.parse(localStorage.getItem('routex_pending_payment') || '{}');
    return saved;
  } catch {
    return {};
  }
}

function generateBookingId() {
  const year = new Date().getFullYear();
  const random = Math.floor(100000 + Math.random() * 900000);
  const id = `RTX-${year}-${random}`;
  return id;
}

function generatePaymentId() {
  const id = `PMT-${Date.now().toString().slice(-6)}`;
  document.getElementById('paymentIdText').textContent = id;
  return id;
}

function formatBusType(value) {
  const map = { SUPER_LUXURY: 'Super Luxury', LUXURY: 'Luxury', EXPRESS: 'Express' };
  return map[value] || value || 'Luxury';
}

function formatDate(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString('en-US', { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric' });
}

function animateIntro() {
  const intro = document.getElementById('successIntro');
  const ticket = document.getElementById('ticketShell');
  setTimeout(() => {
    ticket.classList.add('show');
  }, 1800);
}

function createConfetti() {
  const layer = document.getElementById('confettiLayer');
  const colors = ['#4f8ef7', '#7c3aed', '#10b981', '#f59e0b', '#ff5d8f'];
  for (let i = 0; i < 45; i++) {
    const piece = document.createElement('span');
    piece.className = 'confetti-piece';
    piece.style.left = `${Math.random() * 100}%`;
    piece.style.background = colors[Math.floor(Math.random() * colors.length)];
    piece.style.setProperty('--tx', `${(Math.random() - 0.5) * 180}px`);
    piece.style.animationDelay = `${Math.random() * 0.15}s`;
    layer.appendChild(piece);
  }
  setTimeout(() => layer.innerHTML = '', 2400);
}

function startCountdown(travelDate, departureTime) {
  const target = new Date(`${travelDate}T${departureTime}`);
  if (Number.isNaN(target.getTime())) {
    document.getElementById('countdownText').textContent = 'Departs in 2h 15m';
    return;
  }

  const tick = () => {
    const now = new Date();
    const diff = target - now;
    if (diff <= 0) {
      document.getElementById('countdownText').textContent = 'Boarding now';
      return;
    }
    const hrs = Math.floor(diff / (1000 * 60 * 60));
    const mins = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    document.getElementById('countdownText').textContent = `Departs in ${hrs}h ${mins}m`;
  };

  tick();
  setInterval(tick, 60000);
}

function generateQRCode(text) {
  const container = document.getElementById('qrcode');
  container.innerHTML = '';
  QRCode.toCanvas(container, text, { width: 180, margin: 1, color: { dark: '#0f172a', light: '#ffffff' } }, () => {});
}

function generateBarcode() {
  const bar = document.getElementById('barcode');
  bar.innerHTML = '';
  const pattern = [6, 2, 4, 1, 5, 2, 4, 3, 2, 5, 2, 6, 3, 2, 7, 1, 4, 2, 5, 3, 1, 6, 2, 3, 5, 4, 2];
  pattern.forEach((value, index) => {
    const line = document.createElement('span');
    line.style.height = `${20 + value * 1.4}px`;
    line.style.opacity = `${0.55 + (index % 4) * 0.1}`;
    bar.appendChild(line);
  });
}

function downloadPdf() {
  const ticket = document.getElementById('ticketCard');
  html2canvas(ticket, { scale: 2, backgroundColor: '#0f172a' }).then((canvas) => {
    const imgData = canvas.toDataURL('image/png');
    const pdf = new window.jspdf.jsPDF('p', 'mm', 'a4');
    const pdfWidth = 210;
    const pdfHeight = (canvas.height * pdfWidth) / canvas.width;
    pdf.addImage(imgData, 'PNG', 0, 0, pdfWidth, pdfHeight);
    pdf.save(`RouteX-Ticket-${document.getElementById('bookingIdText').textContent}.pdf`);
  });
}

function printTicket() {
  window.print();
}

function shareTicket() {
  const bookingId = document.getElementById('bookingIdText').textContent;
  const shareData = {
    title: 'RouteX Ticket',
    text: `My RouteX booking is confirmed. Booking ID: ${bookingId}`
  };

  if (navigator.share) {
    navigator.share(shareData).catch(() => {
      navigator.clipboard.writeText(bookingId).then(() => showToast('Booking ID copied to clipboard'));
    });
  } else {
    navigator.clipboard.writeText(bookingId).then(() => showToast('Booking ID copied to clipboard'));
  }
}

function openMap() {
  window.open('https://www.google.com/maps?q=6.927079,79.861244', '_blank');
}

function emailTicket() {
  const bookingId = document.getElementById('bookingIdText').textContent;
  window.location.href = `mailto:support@routex.com?subject=RouteX Ticket ${bookingId}&body=Your RouteX ticket is ready. Booking ID: ${bookingId}`;
}

function showToast(message) {
  const existing = document.querySelector('.toast');
  if (existing) existing.remove();
  const toast = document.createElement('div');
  toast.className = 'toast';
  toast.textContent = message;
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 2200);
}
