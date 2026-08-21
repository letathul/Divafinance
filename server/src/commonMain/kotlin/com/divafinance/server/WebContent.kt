package com.divafinance.server

internal val INDEX_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Diva Finance</title>
    <link rel="stylesheet" href="/style.css">
</head>
<body>
    <div id="app">
        <div id="login-screen" class="screen active">
            <div class="login-container">
                <h1 class="logo">Diva Finance</h1>
                <p class="subtitle">Enter your PIN to access</p>
                <form id="login-form">
                    <input type="password" id="pin-input" placeholder="Enter PIN" maxlength="8" autocomplete="off">
                    <button type="submit" class="btn-primary">Unlock</button>
                </form>
                <p id="login-error" class="error hidden"></p>
            </div>
        </div>
        <div id="main-screen" class="screen">
            <header>
                <h1>Diva Finance</h1>
                <button id="logout-btn" class="btn-text">Logout</button>
            </header>
            <nav class="tab-bar">
                <button class="tab active" data-tab="dashboard">Dashboard</button>
                <button class="tab" data-tab="cards">Cards</button>
                <button class="tab" data-tab="transactions">Transactions</button>
            </nav>
            <main id="content">
                <section id="dashboard" class="tab-content active">
                    <div class="card-grid" id="spending-chart"></div>
                </section>
                <section id="cards" class="tab-content">
                    <div id="cards-list"></div>
                </section>
                <section id="transactions" class="tab-content">
                    <div id="transactions-list"></div>
                </section>
            </main>
        </div>
    </div>
    <script src="/app.js"></script>
</body>
</html>
""".trimIndent()

internal val STYLE_CSS = """
:root {
    --gold: #D4A843;
    --gold-light: #F5E6B8;
    --dark: #1E1E2E;
    --dark-surface: #2A2A3C;
    --white: #FAFAFA;
    --gray: #9E9E9E;
    --light-gray: #F0F0F0;
    --green: #4CAF50;
    --red: #E53935;
    --blue: #2196F3;
    --radius: 12px;
    --bg: var(--white);
    --text: var(--dark);
    --card-bg: #FFFFFF;
    --border: #E0E0E0;
}
@media (prefers-color-scheme: dark) {
    :root {
        --bg: var(--dark);
        --text: var(--white);
        --card-bg: var(--dark-surface);
        --border: #3A3A4C;
        --light-gray: #333346;
    }
}
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: var(--bg);
    color: var(--text);
    min-height: 100vh;
}
.screen { display: none; }
.screen.active { display: block; }
.hidden { display: none !important; }
.login-container {
    display: flex; flex-direction: column; align-items: center;
    justify-content: center; min-height: 100vh; padding: 24px;
}
.logo { font-size: 32px; font-weight: 700; color: var(--gold); margin-bottom: 8px; }
.subtitle { color: var(--gray); margin-bottom: 24px; }
#login-form { display: flex; flex-direction: column; gap: 12px; width: 100%; max-width: 300px; }
#pin-input {
    padding: 12px 16px; border: 2px solid var(--border); border-radius: var(--radius);
    font-size: 18px; text-align: center; letter-spacing: 8px;
    background: var(--card-bg); color: var(--text); outline: none;
}
#pin-input:focus { border-color: var(--gold); }
.btn-primary {
    padding: 12px; background: var(--gold); color: var(--dark); border: none;
    border-radius: var(--radius); font-size: 16px; font-weight: 600; cursor: pointer;
}
.btn-primary:hover { opacity: 0.9; }
.btn-text { background: none; border: none; color: var(--gold); font-size: 14px; cursor: pointer; }
.error { color: var(--red); font-size: 14px; margin-top: 8px; }
header {
    display: flex; justify-content: space-between; align-items: center;
    padding: 16px; border-bottom: 1px solid var(--border);
}
header h1 { font-size: 20px; color: var(--gold); }
.tab-bar { display: flex; border-bottom: 1px solid var(--border); overflow-x: auto; }
.tab {
    flex: 1; padding: 12px; background: none; border: none;
    border-bottom: 2px solid transparent; color: var(--gray);
    font-size: 14px; font-weight: 500; cursor: pointer; white-space: nowrap;
}
.tab.active { color: var(--gold); border-bottom-color: var(--gold); }
main { padding: 16px; }
.tab-content { display: none; }
.tab-content.active { display: block; }
.card {
    background: var(--card-bg); border: 1px solid var(--border);
    border-radius: var(--radius); padding: 16px; margin-bottom: 12px;
}
.card h3 { font-size: 16px; margin-bottom: 8px; }
.card-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 12px; }
.stat-card {
    background: var(--card-bg); border: 1px solid var(--border);
    border-radius: var(--radius); padding: 16px; text-align: center;
}
.stat-value { font-size: 24px; font-weight: 700; color: var(--gold); }
.stat-label { font-size: 12px; color: var(--gray); margin-top: 4px; }
.credit-card {
    border-radius: 16px; padding: 20px; color: white;
    margin-bottom: 12px; position: relative; min-height: 120px;
}
.credit-card h3 { margin-bottom: 16px; }
.credit-card .card-number { font-size: 14px; letter-spacing: 2px; opacity: 0.8; }
.credit-card .card-balance { position: absolute; bottom: 20px; right: 20px; text-align: right; }
.txn-row {
    display: flex; justify-content: space-between; align-items: center;
    padding: 12px 16px; border-bottom: 1px solid var(--border);
}
.txn-row:last-child { border-bottom: none; }
.txn-merchant { font-weight: 500; }
.txn-meta { font-size: 12px; color: var(--gray); margin-top: 2px; }
.txn-amount { font-weight: 600; }
.txn-amount.debit { color: var(--red); }
.txn-amount.credit { color: var(--green); }
.spending-bar { display: flex; align-items: center; margin-bottom: 8px; }
.spending-bar .label { width: 100px; font-size: 13px; flex-shrink: 0; }
.spending-bar .bar-bg {
    flex: 1; height: 24px; background: var(--light-gray);
    border-radius: 12px; overflow: hidden; margin: 0 8px;
}
.spending-bar .bar-fill { height: 100%; background: var(--gold); border-radius: 12px; transition: width 0.3s; }
.spending-bar .amount { width: 80px; text-align: right; font-size: 13px; font-weight: 500; }
.empty-state { text-align: center; padding: 48px 24px; color: var(--gray); }
.loading { text-align: center; padding: 48px; color: var(--gray); }
@media (max-width: 480px) { .card-grid { grid-template-columns: 1fr 1fr; } }
""".trimIndent()

internal val APP_JS = """
const ${'$'} = (s) => document.querySelector(s);
const ${'$'}${'$'} = (s) => document.querySelectorAll(s);
const loginScreen = ${'$'}('#login-screen');
const mainScreen = ${'$'}('#main-screen');
const loginForm = ${'$'}('#login-form');
const pinInput = ${'$'}('#pin-input');
const loginError = ${'$'}('#login-error');
const logoutBtn = ${'$'}('#logout-btn');

async function api(path, options = {}) {
    const res = await fetch(path, {
        ...options,
        headers: { 'Content-Type': 'application/json', ...options.headers },
    });
    if (res.status === 401) { showLogin(); throw new Error('Unauthorized'); }
    return res;
}
// Visibility is driven by `.active` alone. Mixing in a `!important` `.hidden` class
// meant the main screen stayed display:none even after login succeeded.
function showLogin() {
    loginScreen.classList.add('active');
    mainScreen.classList.remove('active');
    pinInput.value = '';
    pinInput.focus();
}
function showMain() {
    loginScreen.classList.remove('active');
    mainScreen.classList.add('active');
    loadDashboard();
}
loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    loginError.classList.add('hidden');
    try {
        const res = await fetch('/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ pin: pinInput.value }),
        });
        const data = await res.json();
        if (data.success) { showMain(); }
        else { loginError.textContent = data.message || 'Invalid PIN'; loginError.classList.remove('hidden'); }
    } catch { loginError.textContent = 'Connection error'; loginError.classList.remove('hidden'); }
});
logoutBtn.addEventListener('click', async () => {
    await fetch('/auth/logout', { method: 'POST' });
    showLogin();
});
${'$'}${'$'}('.tab').forEach((tab) => {
    tab.addEventListener('click', () => {
        ${'$'}${'$'}('.tab').forEach((t) => t.classList.remove('active'));
        ${'$'}${'$'}('.tab-content').forEach((c) => c.classList.remove('active'));
        tab.classList.add('active');
        const target = tab.dataset.tab;
        ${'$'}('#' + target).classList.add('active');
        if (target === 'cards') loadCards();
        if (target === 'transactions') loadTransactions();
        if (target === 'dashboard') loadDashboard();
    });
});
async function loadDashboard() {
    const container = ${'$'}('#spending-chart');
    container.innerHTML = '<div class="loading">Loading...</div>';
    try {
        const res = await api('/api/graphs/spending');
        const data = await res.json();
        if (!data.length) { container.innerHTML = '<div class="empty-state">No spending data yet</div>'; return; }
        const maxAmount = Math.max(...data.map((d) => d.total));
        container.innerHTML = '<div class="card" style="grid-column:1/-1"><h3>Spending by Category (30 days)</h3>' +
            data.map((d) => '<div class="spending-bar"><span class="label">' + formatCategory(d.category) +
                '</span><div class="bar-bg"><div class="bar-fill" style="width:' + (d.total / maxAmount * 100).toFixed(0) +
                '%"></div></div><span class="amount">${'$'}' + d.total.toFixed(2) + '</span></div>').join('') + '</div>';
    } catch { container.innerHTML = '<div class="empty-state">Failed to load data</div>'; }
}
async function loadCards() {
    const container = ${'$'}('#cards-list');
    container.innerHTML = '<div class="loading">Loading...</div>';
    try {
        const res = await api('/api/cards');
        const cards = await res.json();
        if (!cards.length) { container.innerHTML = '<div class="empty-state">No cards added yet</div>'; return; }
        container.innerHTML = cards.map((card) =>
            '<div class="credit-card" style="background:' + (card.color || '#333') + '">' +
            '<h3>' + card.name + '</h3>' +
            '<div class="card-number">' + card.network + (card.lastFour ? ' •••• ' + card.lastFour : '') + '</div>' +
            '<div class="card-balance"><div style="font-size:12px;opacity:0.8">Balance</div>' +
            '<div style="font-size:20px;font-weight:700">${'$'}' + card.currentBalance.toFixed(2) + '</div>' +
            '<div style="font-size:11px;opacity:0.7">Limit: ${'$'}' + card.creditLimit.toFixed(2) + '</div></div></div>'
        ).join('');
    } catch { container.innerHTML = '<div class="empty-state">Failed to load cards</div>'; }
}
async function loadTransactions() {
    const container = ${'$'}('#transactions-list');
    container.innerHTML = '<div class="loading">Loading...</div>';
    try {
        const res = await api('/api/transactions');
        const txns = await res.json();
        if (!txns.length) { container.innerHTML = '<div class="empty-state">No transactions yet</div>'; return; }
        container.innerHTML = '<div class="card" style="padding:0">' +
            txns.slice(0, 50).map((txn) =>
                '<div class="txn-row"><div><div class="txn-merchant">' +
                (txn.merchantName || formatCategory(txn.category)) +
                '</div><div class="txn-meta">' + formatCategory(txn.category) + ' &bull; ' + txn.date +
                '</div></div><div class="txn-amount ' + (txn.type === 'DEBIT' ? 'debit' : 'credit') + '">' +
                (txn.type === 'DEBIT' ? '-' : '+') + '${'$'}' + txn.amount.toFixed(2) + '</div></div>'
            ).join('') + '</div>';
    } catch { container.innerHTML = '<div class="empty-state">Failed to load transactions</div>'; }
}
function formatCategory(cat) {
    return cat.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
}
// Survive a browser refresh: the session cookie may still be valid, in which case
// there is no reason to ask for the PIN again.
(async function boot() {
    try {
        const res = await fetch('/auth/session');
        const data = await res.json();
        if (data.success) { showMain(); return; }
    } catch {}
    showLogin();
})();
""".trimIndent()
