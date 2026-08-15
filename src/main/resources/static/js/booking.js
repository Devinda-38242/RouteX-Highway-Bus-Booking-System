// =============================================
// RouteX - Booking Page JavaScript
// =============================================
const API = 'http://localhost:8080/api';
const params = new URLSearchParams(window.location.search);

let routeId = params.get('routeId');
let selectedSeats = [];
let bookedSeats = [];
let scheduleId = null;
let ticketPrice = parseFloat(params.get('price')) || 1800;
let otpVerified = false;
let stompClient = null;

const origin = params.get('origin') || '';
const dest = params.get('destination') || '';
const dep = params.get('departureTime') || '';
const arr = params.get('arrivalTime') || '';
const operator = params.get('operatorName') || '';
const busType = params.get('busType') || '';
const travelDate = params.get('date') || '';

// ── Init ──────────────────────────────────────
async function init() {
    if (!localStorage.getItem('routex_token')) { window.location.href = 'login.html'; return; }

    // Populate journey info
    document.getElementById('route-display').textContent = `${origin} → ${dest}`;
    document.getElementById('date-display').textContent = formatDate(travelDate);
    document.getElementById('dep-time').textContent = dep;
    document.getElementById('arr-time').textContent = arr;
    document.getElementById('operator-name').textContent = operator;
    document.getElementById('bus-type-display').textContent =
        ({ SUPER_LUXURY:'Super Luxury', LUXURY:'Luxury', EXPRESS:'Express' }[busType] || busType);
    document.getElementById('price-display').textContent = `Rs. ${ticketPrice.toLocaleString()}`;

    document.getElementById('s-route').textContent = `${origin} → ${dest}`;
    document.getElementById('s-date').textContent = formatDate(travelDate);
    document.getElementById('s-dep').textContent = dep;
    document.getElementById('s-price-unit').textContent = `Rs. ${ticketPrice.toLocaleString()}`;

    // Load schedule & seats
    await loadScheduleAndSeats();

    // Pre-fill from logged-in user
    const user = JSON.parse(localStorage.getItem('routex_user') || '{}');
    if (user.email) document.getElementById('passenger-email').value = user.email;
    if (user.fullName) document.getElementById('passenger-name').value = user.fullName;
    document.getElementById('otp-area').style.display = 'block';
}

async function loadScheduleAndSeats() {
    try {
        const res = await fetch(`${API}/routes/${routeId}/schedule?date=${travelDate}`, {
            headers: authHeaders()
        });
        if (!res.ok) {
            const errText = await res.text();
            console.error('Error fetching schedule:', res.status, errText);
            showToast(`Error ${res.status}: ${errText}`, 'error');
            return;
        }
        const json = await res.json();
        if (!json.success) {
            console.error('Schedule fetch failed:', json);
            showToast(json.message || 'Failed to load schedule', 'error');
            return;
        }
        scheduleId = json.data.id;
        bookedSeats = json.data.bookedSeats ? json.data.bookedSeats.split(',').filter(Boolean) : [];
        buildSeatGrid();
        connectWebSocket();
    } catch (e) {
        console.error('Failed to load schedule and seats:', e);
        showToast('Failed to load seat availability, please try again', 'error');
    }
}

// ── Seat Grid ──────────────────────────────────
function buildSeatGrid() {
    const grid = document.getElementById('seat-grid');
    grid.innerHTML = '';
    // 45 seats: rows A-I (5 seats each) — layout: 2 + aisle + 2
    const rows = ['A','B','C','D','E','F','G','H','I'];
    rows.forEach(row => {
        const rowEl = document.createElement('div');
        rowEl.className = 'seat-row';

        [1, 2, 'aisle', 3, 4].forEach(col => {
            if (col === 'aisle') {
                const a = document.createElement('div');
                a.className = 'seat-aisle';
                a.textContent = row;
                rowEl.appendChild(a);
                return;
            }
            const seatId = `${row}${col}`;
            const seat = document.createElement('button');
            seat.className = 'seat';
            seat.textContent = seatId;
            seat.dataset.seat = seatId;

            if (bookedSeats.includes(seatId)) {
                seat.classList.add('booked');
                seat.disabled = true;
            } else {
                seat.addEventListener('click', () => toggleSeat(seatId, seat));
            }
            rowEl.appendChild(seat);
        });
        grid.appendChild(rowEl);
    });
    // Add a few extra seats in the back row
    const lastRow = document.createElement('div');
    lastRow.className = 'seat-row last-row';
    for (let i = 1; i <= 5; i++) {
        const seatId = `J${i}`;
        const seat = document.createElement('button');
        seat.className = 'seat';
        seat.textContent = seatId;
        seat.dataset.seat = seatId;
        if (bookedSeats.includes(seatId)) { seat.classList.add('booked'); seat.disabled = true; }
        else seat.addEventListener('click', () => toggleSeat(seatId, seat));
        lastRow.appendChild(seat);
    }
    grid.appendChild(lastRow);
}

function toggleSeat(seatId, el) {
    if (selectedSeats.includes(seatId)) {
        selectedSeats = selectedSeats.filter(s => s !== seatId);
        el.classList.remove('selected');
    } else {
        if (selectedSeats.length >= 6) { showToast('Maximum 6 seats per booking', 'error'); return; }
        selectedSeats.push(seatId);
        el.classList.add('selected');
    }
    updateSummary();
}

function updateSummary() {
    const total = selectedSeats.length * ticketPrice;
    document.getElementById('selected-seats-display').innerHTML = selectedSeats.length > 0
        ? selectedSeats.map(s => `<span class="seat-tag">${s}</span>`).join(' ')
        : 'None';
    document.getElementById('s-seats').textContent = selectedSeats.join(', ') || 'None';
    document.getElementById('s-count').textContent = selectedSeats.length;
    document.getElementById('s-total').textContent = `Rs. ${total.toLocaleString()}`;

    document.getElementById('confirm-btn').disabled = selectedSeats.length === 0;
}

// ── OTP ────────────────────────────────────────
async function sendOTP() {
    const email = document.getElementById('passenger-email').value.trim();
    if (!email || !/\S+@\S+\.\S+/.test(email)) {
        showToast('Enter valid email first', 'error');
        return;
    }

    const btn = document.getElementById('send-otp-btn');
    btn.disabled = true;
    btn.textContent = 'Sending...';

    try {
        // 🔄 Pointing directly to our new endpoint under /api/bookings
        const response = await fetch(`${API}/bookings/send-otp?email=${encodeURIComponent(email)}`, {
            method: 'POST',
            headers: authHeaders()
        });

        const data = await response.json();

        if (data.success) {
            document.getElementById('otp-input-area').style.display = 'block';
            document.getElementById('otp-status').textContent = `✅ OTP sent to ${email}`;
            document.getElementById('otp-status').style.color = '#2ed573';
            btn.textContent = 'Resend OTP';
            showToast('OTP sent successfully!', 'success');
        } else {
            showToast('Failed to send OTP code.', 'error');
            document.getElementById('otp-status').textContent = '❌ Failed to send code.';
            document.getElementById('otp-status').style.color = '#ff4757';
        }
    } catch (err) {
        console.error("OTP Error: ", err);
        showToast('Network error while sending OTP.', 'error');
    } finally {
        btn.disabled = false;
    }
}

async function verifyOTP() {
    const email = document.getElementById('passenger-email').value.trim();
    const otp = document.getElementById('otp-code').value.trim();
    if (otp.length < 6) { showToast('Enter 6-digit OTP', 'error'); return; }
    try {
        const res = await fetch(`${API}/bookings/verify-otp?email=${encodeURIComponent(email)}&otp=${encodeURIComponent(otp)}`, {
            method: 'POST',
            headers: authHeaders()
        });
        const json = await res.json();
        if (json.success) {
            otpVerified = true;
            document.getElementById('otp-status').textContent = '✅ Email verified!';
            document.getElementById('otp-status').style.color = '#2ed573';
            showToast('Email verified!', 'success');
        } else {
            showToast(json.message || 'Invalid OTP', 'error');
        }
    } catch {
        otpVerified = true; // Demo fallback
        document.getElementById('otp-status').textContent = '✅ Verified (demo mode)';
    }
}

// ── Booking ────────────────────────────────────
async function confirmBooking() {
    const name = document.getElementById('passenger-name').value.trim();
    const email = document.getElementById('passenger-email').value.trim();
    const phone = document.getElementById('passenger-phone').value.trim();

    if (!name) { showFieldErr('name-err', 'Name is required'); return; }
    if (!email) { showFieldErr('email-err', 'Email is required'); return; }
    if (selectedSeats.length === 0) { showToast('Please select at least one seat', 'error'); return; }

    clearFieldErrors();
    const subtotal = selectedSeats.length * ticketPrice;
    const serviceFee = selectedSeats.length > 0 ? 150 : 0;
    const taxes = Math.round(subtotal * 0.02);
    const paymentData = {
        routeId,
        scheduleId: scheduleId || 1,
        origin,
        destination: dest,
        travelDate,
        departureTime: dep,
        arrivalTime: arr,
        operatorName: operator,
        busType,
        seats: selectedSeats,
        passengerName: name,
        passengerEmail: email,
        passengerPhone: phone || '0700000000',
        pricePerSeat: ticketPrice,
        serviceFee,
        taxes,
        grandTotal: subtotal + serviceFee + taxes,
        bookingPayload: {
        scheduleId: scheduleId || 1,
        seats: selectedSeats,
        passengerName: name,
        passengerEmail: email,
        passengerPhone: phone || '0700000000'
        }
    };

    localStorage.setItem('routex_pending_payment', JSON.stringify(paymentData));
    window.location.href = 'Payment.html';
}

function showTicket(booking) {
    document.getElementById('summary-card').style.display = 'none';
    const ticketArea = document.getElementById('ticket-area');
    ticketArea.style.display = 'block';

    if (booking.qrCode) {
        document.getElementById('qr-img').src = `data:image/png;base64,${booking.qrCode}`;
    }

    document.getElementById('ticket-details').innerHTML = `
        <div class="ticket-row"><span>Booking ID</span><b>${booking.bookingReference}</b></div>
        <div class="ticket-row"><span>Passenger</span><b>${booking.passengerName}</b></div>
        <div class="ticket-row"><span>Route</span><b>${origin} → ${dest}</b></div>
        <div class="ticket-row"><span>Date</span><b>${formatDate(travelDate)}</b></div>
        <div class="ticket-row"><span>Departure</span><b>${dep}</b></div>
        <div class="ticket-row"><span>Seats</span><b>${(booking.seats || selectedSeats).join(', ')}</b></div>
        <div class="ticket-row total"><span>Total Paid</span><b>Rs. ${Number(booking.totalAmount).toLocaleString()}</b></div>
    `;
    showToast('🎉 Booking confirmed! Check your email.', 'success');
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function generateFakeQR() {
    // placeholder light-grey square for demo
    return '';
}

function downloadTicket() {
    window.print();
}

// ── WebSocket Real-time ─────────────────────────
function connectWebSocket() {
    if (!scheduleId) return;
    try {
        const sock = new SockJS('http://localhost:8080/ws');
        stompClient = Stomp.over(sock);
        stompClient.debug = null;
        stompClient.connect({}, () => {
            stompClient.subscribe(`/topic/seats/${scheduleId}`, msg => {
                const data = JSON.parse(msg.body);
                bookedSeats = data.bookedSeats || [];
                buildSeatGrid();
                showToast('🔄 Seat availability updated!', 'info');
            });
        });
    } catch { /* WebSocket optional */ }
}

// ── Helpers ─────────────────────────────────────
function authHeaders() {
    const token = localStorage.getItem('routex_token');
    return token ? { 'Authorization': `Bearer ${token}` } : {};
}
function formatDate(d) { return d ? new Date(d).toLocaleDateString('en-US', { weekday:'long', year:'numeric', month:'long', day:'numeric' }) : ''; }
function showToast(msg, type = 'info') {
    const colors = { success:'#2ed573', error:'#ff4757', info:'#4db8ff' };
    const toast = document.createElement('div');
    toast.style.cssText = `position:fixed;top:24px;right:24px;z-index:9999;background:${colors[type]||colors.info};color:#fff;padding:14px 24px;border-radius:12px;font-size:14px;font-weight:600;box-shadow:0 10px 30px rgba(0,0,0,0.3);animation:fadeInUp 0.3s ease;max-width:320px;`;
    toast.textContent = msg;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3500);
}
function showFieldErr(id, msg) { document.getElementById(id).textContent = msg; }
function clearFieldErrors() { document.querySelectorAll('.field-err').forEach(e => e.textContent = ''); }

init();
