// =============================================
// RouteX - Home Page JavaScript
// =============================================
const API = 'http://localhost:8080/api';

// ── Page Load ──────────────────────────────
window.addEventListener('load', () => {
    setTimeout(() => {
        document.getElementById('page-loader').classList.add('hidden');
    }, 1200);
    checkAuthState();
    loadLocations();
    loadPopularRoutes();
    setMinDate();
    revealOnScroll();
    window.addEventListener('scroll', revealOnScroll);
});

// ── Auth State ──────────────────────────────
function checkAuthState() {
    const token = localStorage.getItem('routex_token');
    const user = JSON.parse(localStorage.getItem('routex_user') || 'null');
    if (token && user) {
        document.getElementById('guest-buttons').style.display = 'none';
        document.getElementById('user-menu').style.display = 'flex';
        document.getElementById('user-greeting').textContent = `Hi, ${user.fullName.split(' ')[0]} 👋`;
    }
}

function logout() {
    localStorage.removeItem('routex_token');
    localStorage.removeItem('routex_user');
    fetch(`${API}/auth/logout`, { method: 'POST', credentials: 'include' }).catch(() => {});
    location.reload();
}

// ── Load Locations into datalists ──────────
async function loadLocations() {
    try {
        const res = await fetch(`${API}/routes/locations`);
        const json = await res.json();
        if (json.success) {
            const origList = document.getElementById('origins-list');
            const destList = document.getElementById('destinations-list');
            // API returns merged city list for both dropdowns
            const cities = json.data.origins || [];
            cities.forEach(c => {
                origList.innerHTML += `<option value="${c}">`;
                destList.innerHTML += `<option value="${c}">`;
            });
        }
    } catch (e) {
        console.warn('Could not load locations (is backend running?)');
    }
}

// ── Popular Routes ──────────────────────────
const POPULAR = [
    { from: 'Colombo (Pettah)', to: 'Kandy', icon: '🏔️' },
    { from: 'Kandy', to: 'Colombo (Pettah)', icon: '🔁' },
    { from: 'Colombo (Pettah)', to: 'Galle', icon: '🌊' },
    { from: 'Galle', to: 'Colombo (Pettah)', icon: '🔁' },
    { from: 'Colombo (Pettah)', to: 'Jaffna', icon: '🕌' },
    { from: 'Jaffna', to: 'Colombo (Pettah)', icon: '🔁' },
    { from: 'Makumbura (Colombo)', to: 'Katharagama', icon: '🛕' },
    { from: 'Katharagama', to: 'Makumbura (Colombo)', icon: '🔁' },
    { from: 'Colombo (Pettah)', to: 'Trincomalee', icon: '⚓' },
    { from: 'Trincomalee', to: 'Colombo (Pettah)', icon: '🔁' },
    { from: 'Colombo (Pettah)', to: 'Badulla', icon: '🌿' },
    { from: 'Badulla', to: 'Colombo (Pettah)', icon: '🔁' },
    { from: 'Colombo (Pettah)', to: 'Matara', icon: '🌅' },
    { from: 'Matara', to: 'Colombo (Pettah)', icon: '🔁' },
    { from: 'Colombo (Pettah)', to: 'Anuradhapura', icon: '🏛️' },
    { from: 'Anuradhapura', to: 'Colombo (Pettah)', icon: '🔁' },
    { from: 'Colombo (Pettah)', to: 'Batticaloa', icon: '🏖️' },
    { from: 'Batticaloa', to: 'Colombo (Pettah)', icon: '🔁' },
];

function loadPopularRoutes() {
    const container = document.getElementById('route-cards-container');
    POPULAR.forEach(r => {
        const card = document.createElement('div');
        card.className = 'route-card reveal reveal-delay';
        card.innerHTML = `<div class="route-icon">${r.icon}</div>${r.from.split('(')[0].trim()} → ${r.to}`;
        card.onclick = () => quickSearch(r.from, r.to);
        container.appendChild(card);
    });
    revealOnScroll();
}

function quickSearch(from, to) {
    document.getElementById('from-input').value = from;
    document.getElementById('to-input').value = to;
    document.getElementById('search-section').scrollIntoView({ behavior: 'smooth' });
}

// ── Search ──────────────────────────────────
function setMinDate() {
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('date-input').min = today;
    document.getElementById('date-input').value = today;
}

function swapLocations() {
    const fromInput = document.getElementById('from-input');
    const toInput = document.getElementById('to-input');
    [fromInput.value, toInput.value] = [toInput.value, fromInput.value];
}

function goToResults(e) {
    e.preventDefault();
    const from = document.getElementById('from-input').value.trim();
    const to = document.getElementById('to-input').value.trim();
    const date = document.getElementById('date-input').value;
    if (!from || !to || !date) { showToast('Please fill in all search fields', 'error'); return; }
    const params = new URLSearchParams({ from, to, date });
    window.location.href = `results.html?${params}`;
}

function scrollToSearch() { document.getElementById('search-section').scrollIntoView({ behavior: 'smooth' }); }
function scrollToRoutes() { document.getElementById('routes').scrollIntoView({ behavior: 'smooth' }); }

// ── Theme Toggle ────────────────────────────
const toggleBtn = document.getElementById('theme-toggle');
const icon = toggleBtn.querySelector('i');

function updateThemeIcon(isLight) {
    icon.className = isLight ? 'fa-solid fa-moon' : 'fa-solid fa-sun';
    toggleBtn.setAttribute('aria-label', isLight ? 'Switch to dark mode' : 'Switch to light mode');
}

const savedTheme = localStorage.getItem('theme');
if (savedTheme === 'light') {
    document.body.classList.add('light-mode');
    updateThemeIcon(true);
} else {
    updateThemeIcon(false);
}

toggleBtn.addEventListener('click', () => {
    document.body.classList.toggle('light-mode');
    const isLight = document.body.classList.contains('light-mode');
    localStorage.setItem('theme', isLight ? 'light' : 'dark');
    updateThemeIcon(isLight);
});

// ── Reveal on Scroll ────────────────────────
function revealOnScroll() {
    document.querySelectorAll('.reveal').forEach(el => {
        if (el.getBoundingClientRect().top < window.innerHeight - 80) {
            el.classList.add('active');
        }
    });
}

// ── Chatbot ──────────────────────────────────
let botOpen = false;

function toggleBot() {
    botOpen = !botOpen;
    const win = document.getElementById('chatbot-window');
    win.classList.toggle('open', botOpen);
    document.getElementById('bot-badge').style.display = botOpen ? 'none' : 'flex';
    if (botOpen) document.getElementById('bot-input').focus();
}

function botEnter(e) { if (e.key === 'Enter') sendBotMessage(); }

async function sendBotMessage() {
    const input = document.getElementById('bot-input');
    const msg = input.value.trim();
    if (!msg) return;
    input.value = '';
    appendBotMsg(msg, 'user');

    // Typing indicator
    const typingId = 'typing-' + Date.now();
    document.getElementById('chatbot-messages').innerHTML +=
        `<div class="bot-msg" id="${typingId}">✍️ RouteBot is typing...</div>`;

    try {
        const res = await fetch(`${API}/bot/message`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: msg })
        });
        const json = await res.json();
        document.getElementById(typingId).remove();
        appendBotMsg(json.data || 'Sorry, I couldn\'t understand that.', 'bot');
    } catch {
        document.getElementById(typingId).remove();
        appendBotMsg('⚠️ Unable to reach RouteBot. Please check your connection.', 'bot');
    }
}

function quickReply(msg) {
    document.getElementById('bot-input').value = msg;
    sendBotMessage();
}

function appendBotMsg(text, sender) {
    const container = document.getElementById('chatbot-messages');
    const div = document.createElement('div');
    div.className = sender === 'bot' ? 'bot-msg' : 'user-msg';
    div.innerHTML = text.replace(/\n/g, '<br>').replace(/\*\*(.*?)\*\*/g, '<b>$1</b>');
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
}

// ── Toast Notification ──────────────────────
function showToast(msg, type = 'info') {
    const toast = document.createElement('div');
    toast.style.cssText = `
        position:fixed;top:30px;right:30px;z-index:9999;
        background:${type === 'error' ? '#ff4757' : '#2ed573'};
        color:#fff;padding:14px 24px;border-radius:12px;
        font-size:14px;font-weight:600;
        box-shadow:0 10px 30px rgba(0,0,0,0.3);
        animation:fadeInUp 0.3s ease;
    `;
    toast.textContent = msg;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}