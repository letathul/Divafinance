const $ = (s) => document.querySelector(s);
const $$ = (s) => document.querySelectorAll(s);

const loginScreen = $('#login-screen');
const mainScreen = $('#main-screen');
const loginForm = $('#login-form');
const pinInput = $('#pin-input');
const loginError = $('#login-error');
const logoutBtn = $('#logout-btn');

async function api(path, options = {}) {
    const res = await fetch(path, {
        ...options,
        headers: { 'Content-Type': 'application/json', ...options.headers },
    });
    if (res.status === 401) {
        showLogin();
        throw new Error('Unauthorized');
    }
    return res;
}

function showLogin() {
    loginScreen.classList.add('active');
    mainScreen.classList.add('hidden');
    pinInput.value = '';
    pinInput.focus();
}

function showMain() {
    loginScreen.classList.remove('active');
    mainScreen.classList.remove('hidden');
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
        if (data.success) {
            showMain();
        } else {
            loginError.textContent = data.message || 'Invalid PIN';
            loginError.classList.remove('hidden');
        }
    } catch {
        loginError.textContent = 'Connection error';
        loginError.classList.remove('hidden');
    }
});

logoutBtn.addEventListener('click', async () => {
    await fetch('/auth/logout', { method: 'POST' });
    showLogin();
});

$$('.tab').forEach((tab) => {
    tab.addEventListener('click', () => {
        $$('.tab').forEach((t) => t.classList.remove('active'));
        $$('.tab-content').forEach((c) => c.classList.remove('active'));
        tab.classList.add('active');
        const target = tab.dataset.tab;
        $(`#${target}`).classList.add('active');

        if (target === 'cards') loadCards();
        if (target === 'transactions') loadTransactions();
        if (target === 'dashboard') loadDashboard();
    });
});

async function loadDashboard() {
    const container = $('#spending-chart');
    container.innerHTML = '<div class="loading">Loading...</div>';
    try {
        const res = await api('/api/graphs/spending');
        const data = await res.json();
        if (!data.length) {
            container.innerHTML = '<div class="empty-state">No spending data yet</div>';
            return;
        }
        const maxAmount = Math.max(...data.map((d) => d.total));
        container.innerHTML = '<div class="card" style="grid-column:1/-1"><h3>Spending by Category (30 days)</h3>' +
            data.map((d) => `
                <div class="spending-bar">
                    <span class="label">${formatCategory(d.category)}</span>
                    <div class="bar-bg"><div class="bar-fill" style="width:${(d.total / maxAmount * 100).toFixed(0)}%"></div></div>
                    <span class="amount">$${d.total.toFixed(2)}</span>
                </div>
            `).join('') + '</div>';
    } catch {
        container.innerHTML = '<div class="empty-state">Failed to load data</div>';
    }
}

async function loadCards() {
    const container = $('#cards-list');
    container.innerHTML = '<div class="loading">Loading...</div>';
    try {
        const res = await api('/api/cards');
        const cards = await res.json();
        if (!cards.length) {
            container.innerHTML = '<div class="empty-state">No cards added yet</div>';
            return;
        }
        container.innerHTML = cards.map((card) => `
            <div class="credit-card" style="background:${card.color || '#333'}">
                <h3>${card.name}</h3>
                <div class="card-number">${card.network} ${card.lastFour ? '•••• ' + card.lastFour : ''}</div>
                <div class="card-balance">
                    <div style="font-size:12px;opacity:0.8">Balance</div>
                    <div style="font-size:20px;font-weight:700">$${card.currentBalance.toFixed(2)}</div>
                    <div style="font-size:11px;opacity:0.7">Limit: $${card.creditLimit.toFixed(2)}</div>
                </div>
            </div>
        `).join('');
    } catch {
        container.innerHTML = '<div class="empty-state">Failed to load cards</div>';
    }
}

async function loadTransactions() {
    const container = $('#transactions-list');
    container.innerHTML = '<div class="loading">Loading...</div>';
    try {
        const res = await api('/api/transactions');
        const txns = await res.json();
        if (!txns.length) {
            container.innerHTML = '<div class="empty-state">No transactions yet</div>';
            return;
        }
        container.innerHTML = '<div class="card" style="padding:0">' +
            txns.slice(0, 50).map((txn) => `
                <div class="txn-row">
                    <div>
                        <div class="txn-merchant">${txn.merchantName || formatCategory(txn.category)}</div>
                        <div class="txn-meta">${formatCategory(txn.category)} &bull; ${txn.date}</div>
                    </div>
                    <div class="txn-amount ${txn.type === 'DEBIT' ? 'debit' : 'credit'}">
                        ${txn.type === 'DEBIT' ? '-' : '+'}$${txn.amount.toFixed(2)}
                    </div>
                </div>
            `).join('') + '</div>';
    } catch {
        container.innerHTML = '<div class="empty-state">Failed to load transactions</div>';
    }
}

function formatCategory(cat) {
    return cat.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase()).toLowerCase()
        .replace(/\b\w/g, (c) => c.toUpperCase());
}

pinInput.focus();
