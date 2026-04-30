const API_URL = '/api';

let currentUser = null;
let userPassword = null; // Storing temporarily for simple auth demo
let userAccounts = [];

// DOM Elements
const loginForm = document.getElementById('login-form');
const loginError = document.getElementById('login-error');
const loginView = document.getElementById('login-view');
const dashboardView = document.getElementById('dashboard-view');
const userDisplay = document.getElementById('user-display');
const accountsContainer = document.getElementById('accounts-container');
const historyBody = document.getElementById('history-body');
const tabBtns = document.querySelectorAll('.tab-btn');
const actionForms = document.querySelectorAll('.action-form');
const accountSelects = document.querySelectorAll('.account-select');

// Login
loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const u = document.getElementById('username').value;
    const p = document.getElementById('password').value;

    try {
        const res = await fetch(`${API_URL}/login`, {
            method: 'POST',
            body: JSON.stringify({ username: u, password: p })
        });
        const data = await res.json();
        
        if (data.status === 'success') {
            currentUser = u;
            userPassword = p; // Simple demo, never do this in real app
            loginView.classList.remove('active');
            dashboardView.classList.add('active');
            userDisplay.textContent = u;
            await loadAccounts();
        } else {
            loginError.textContent = data.message;
        }
    } catch (err) {
        loginError.textContent = "Server error";
    }
});

// Logout
document.getElementById('logout-btn').addEventListener('click', () => {
    currentUser = null;
    userPassword = null;
    dashboardView.classList.remove('active');
    loginView.classList.add('active');
    loginForm.reset();
    loginError.textContent = '';
});

// Load Accounts
async function loadAccounts() {
    try {
        const res = await fetch(`${API_URL}/accounts?username=${currentUser}&password=${userPassword}`);
        userAccounts = await res.json();
        renderAccounts();
        updateSelects();
        renderHistory();
    } catch (err) {
        console.error("Failed to load accounts", err);
    }
}

// Render Accounts UI
function renderAccounts() {
    accountsContainer.innerHTML = '';
    userAccounts.forEach(acc => {
        accountsContainer.innerHTML += `
            <div class="account-card">
                <div class="account-type">${acc.type} ACCOUNT</div>
                <div class="account-number">${acc.accountNumber}</div>
                <div class="account-balance">$${acc.balance.toFixed(2)}</div>
            </div>
        `;
    });
}

// Update Select Dropdowns
function updateSelects() {
    accountSelects.forEach(select => {
        select.innerHTML = '';
        userAccounts.forEach(acc => {
            select.innerHTML += `<option value="${acc.accountNumber}">${acc.accountNumber} (${acc.type}) - $${acc.balance.toFixed(2)}</option>`;
        });
    });
}

// Render History
function renderHistory() {
    historyBody.innerHTML = '';
    let allTx = [];
    userAccounts.forEach(acc => {
        acc.transactions.forEach(tx => {
            allTx.push({...tx, account: acc.accountNumber});
        });
    });
    
    // Sort by date desc (naive string sort since dates are ISO)
    allTx.sort((a,b) => b.date.localeCompare(a.date));

    allTx.forEach(tx => {
        historyBody.innerHTML += `
            <tr>
                <td>${new Date(tx.date).toLocaleString()}</td>
                <td>${tx.desc} (${tx.account})</td>
                <td class="type-${tx.type}">${tx.type}</td>
                <td class="type-${tx.type}">${tx.type.includes('IN') || tx.type === 'DEPOSIT' ? '+' : '-'}$${tx.amount.toFixed(2)}</td>
            </tr>
        `;
    });
}

// Tabs Logic
tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
        tabBtns.forEach(b => b.classList.remove('active'));
        actionForms.forEach(f => f.classList.remove('active'));
        
        btn.classList.add('active');
        document.getElementById(btn.dataset.target).classList.add('active');
    });
});

// Transactions
async function handleTransaction(type, formId) {
    const form = document.getElementById(formId);
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const accNum = form.querySelector('.account-select').value;
        const amount = form.querySelector('.amount-input').value;
        
        let body = { type, accountNumber: accNum, amount };
        if (type === 'transfer') {
            body.targetNumber = document.getElementById('transfer-target').value;
        }

        try {
            const res = await fetch(`${API_URL}/transaction`, {
                method: 'POST',
                body: JSON.stringify(body)
            });
            const data = await res.json();
            if (data.status === 'success') {
                alert(data.message);
                form.reset();
                await loadAccounts(); // Refresh
            } else {
                alert(data.message);
            }
        } catch (err) {
            alert('Error processing transaction');
        }
    });
}

handleTransaction('deposit', 'deposit-form');
handleTransaction('withdraw', 'withdraw-form');
handleTransaction('transfer', 'transfer-form');
