'use strict';

// Credentials live in sessionStorage so a refresh keeps you signed in but closing the tab signs you out.
const AUTH_KEY = 'authHeader';
const money = new Intl.NumberFormat('en-ZA', { style: 'currency', currency: 'ZAR' });

let portfolio = null;

const $ = (id) => document.getElementById(id);

// ---------- API helpers ----------

class ApiError extends Error {
    constructor(status, message, fieldErrors) {
        super(message);
        this.status = status;
        this.fieldErrors = fieldErrors || null;
    }
}

function basicAuth(username, password) {
    // Encode as UTF-8 bytes first: btoa() alone fails on non-ASCII characters
    const bytes = new TextEncoder().encode(`${username}:${password}`);
    let binary = '';
    bytes.forEach((b) => { binary += String.fromCharCode(b); });
    return `Basic ${btoa(binary)}`;
}

async function request(path, options = {}) {
    const headers = { ...(options.headers || {}), Authorization: sessionStorage.getItem(AUTH_KEY) || '' };
    const response = await fetch(path, { ...options, headers });
    if (response.ok) {
        return response;
    }
    let body = null;
    try {
        body = await response.json();
    } catch (ignored) {
        // Non-JSON error body: fall back to the generic message below
    }
    throw new ApiError(response.status, (body && body.message) || 'Something went wrong. Please try again.',
        body && body.fieldErrors);
}

async function requestJson(path, options) {
    return (await request(path, options)).json();
}

// ---------- Views ----------

function showLogin(message) {
    $('app-view').hidden = true;
    $('user-box').hidden = true;
    $('login-view').hidden = false;
    setMessage('login-error', message, 'error');
}

function showApp() {
    $('login-view').hidden = true;
    $('app-view').hidden = false;
    $('user-box').hidden = false;
    $('user-name').textContent = `${portfolio.investor.firstName} ${portfolio.investor.lastName}`;
}

function logout(message) {
    sessionStorage.removeItem(AUTH_KEY);
    portfolio = null;
    $('password').value = '';
    showLogin(message);
}

function setMessage(id, text, kind) {
    const el = $(id);
    el.hidden = !text;
    el.textContent = text || '';
    el.classList.remove('alert-danger', 'alert-success');
    if (text) {
        el.classList.add(kind === 'success' ? 'alert-success' : 'alert-danger');
    }
}

// Every dynamic value goes in via textContent, never innerHTML, so server data cannot inject markup.
function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
}

// ---------- Portfolio ----------

async function loadPortfolio() {
    portfolio = await requestJson('/api/portfolio');
    renderPortfolio();
    renderProductSelects();
    updateAmountHint();
}

function renderPortfolio() {
    const { investor, products, totalBalance } = portfolio;

    const details = $('investor-details');
    details.replaceChildren();
    [
        ['Name', `${investor.firstName} ${investor.lastName}`],
        ['Date of birth', investor.dateOfBirth],
        ['Age', String(investor.age)],
        ['Email', investor.email],
    ].forEach(([label, value]) => {
        const wrapper = el('div', 'col');
        wrapper.append(el('div', 'text-muted small', label), el('div', 'fw-semibold', value));
        details.append(wrapper);
    });

    $('total-balance').textContent = money.format(totalBalance);

    const cards = $('product-cards');
    cards.replaceChildren();
    products.forEach((product) => {
        const col = el('div', 'col');
        const card = el('div', 'card h-100 border-0 shadow-sm');
        const body = el('div', 'card-body');
        body.append(
            el('span', `badge rounded-pill badge-${product.type.toLowerCase()}`, product.type),
            el('h3', 'h6 mt-2 mb-1', product.name),
            el('div', 'product-balance', money.format(product.balance)),
            el('p', 'text-muted small mb-0 mt-1', product.withdrawable
                ? `Maximum withdrawal: ${money.format(product.maxWithdrawal)}`
                : 'Withdrawals are only allowed once the investor is older than 65.'),
        );
        card.append(body);
        col.append(card);
        cards.append(col);
    });
}

function renderProductSelects() {
    const select = $('product-select');
    const previous = select.value;
    select.replaceChildren();
    portfolio.products.forEach((product) => {
        const label = `${product.name} (${money.format(product.balance)})` +
            (product.withdrawable ? '' : ' - not eligible');
        const option = new Option(label, product.id);
        option.disabled = !product.withdrawable;
        select.append(option);
    });
    // Keep the previous choice if it is still available, otherwise pick the first eligible product
    const keep = [...select.options].find((o) => o.value === previous && !o.disabled);
    const firstEligible = [...select.options].find((o) => !o.disabled);
    select.value = keep ? keep.value : (firstEligible ? firstEligible.value : '');

    const filter = $('filter-product');
    const selectedFilter = filter.value;
    filter.replaceChildren(new Option('All products', ''));
    portfolio.products.forEach((product) => filter.append(new Option(product.name, product.id)));
    filter.value = selectedFilter;
}

// ---------- Withdrawal form ----------

function selectedProduct() {
    return portfolio && portfolio.products.find((p) => String(p.id) === $('product-select').value);
}

function updateAmountHint() {
    const product = selectedProduct();
    $('amount-hint').textContent = product
        ? `Maximum you can withdraw now: ${money.format(product.maxWithdrawal)} (90% of the balance)`
        : 'No eligible product available for withdrawal.';
}

// Convenience checks for the user; the server enforces the same rules and is the source of truth.
function validateAmount() {
    const product = selectedProduct();
    const raw = $('amount').value.trim();
    if (!product) return 'Please select an eligible product.';
    if (raw === '') return 'Amount is required.';
    if (!/^\d+(\.\d{1,2})?$/.test(raw)) return 'Enter a positive amount with at most 2 decimal places.';
    const amount = Number(raw);
    if (amount < 0.01) return 'Amount must be at least R 0.01.';
    if (amount > Number(product.balance)) return 'Amount exceeds the available balance.';
    if (amount > Number(product.maxWithdrawal)) {
        return `Amount may not exceed 90% of the balance (${money.format(product.maxWithdrawal)}).`;
    }
    return null;
}

function showAmountError(error) {
    $('amount').classList.toggle('is-invalid', Boolean(error));
    $('amount').setAttribute('aria-invalid', error ? 'true' : 'false');
    $('amount-error').textContent = error || '';
    $('amount-error').classList.toggle('d-block', Boolean(error));
}

async function submitWithdrawal(event) {
    event.preventDefault();
    setMessage('withdraw-message', '');

    const error = validateAmount();
    showAmountError(error);
    if (error) return;

    const button = event.submitter;
    button.disabled = true;
    try {
        const notice = await requestJson('/api/withdrawals', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                productId: Number($('product-select').value),
                amount: $('amount').value.trim(),
            }),
        });
        setMessage('withdraw-message',
            `Withdrawal notice ${notice.reference} submitted for ${money.format(notice.amount)}.`, 'success');
        $('amount').value = '';
        await Promise.all([loadPortfolio(), loadHistory()]);
    } catch (err) {
        handleError(err, 'withdraw-message');
    } finally {
        button.disabled = false;
    }
}

function handleError(err, messageId) {
    if (err.status === 401) {
        logout('Your session is no longer valid. Please sign in again.');
        return;
    }
    const details = err.fieldErrors ? ' ' + Object.values(err.fieldErrors).join(' ') : '';
    setMessage(messageId, err.message + details, 'error');
}

// ---------- History ----------

function filterParams() {
    const params = new URLSearchParams();
    if ($('filter-from').value) params.set('from', $('filter-from').value);
    if ($('filter-to').value) params.set('to', $('filter-to').value);
    if ($('filter-product').value) params.set('productId', $('filter-product').value);
    return params;
}

function validateFilters() {
    const from = $('filter-from').value;
    const to = $('filter-to').value;
    return from && to && from > to ? "The 'From' date must not be after the 'To' date." : null;
}

async function loadHistory() {
    const error = validateFilters();
    setMessage('history-message', error, 'error');
    if (error) return;

    try {
        const rows = await requestJson(`/api/withdrawals?${filterParams()}`);
        renderHistory(rows);
    } catch (err) {
        handleError(err, 'history-message');
    }
}

function renderHistory(rows) {
    const body = $('history-body');
    body.replaceChildren();
    if (rows.length === 0) {
        const row = el('tr');
        const cell = el('td', 'empty', 'No withdrawals found for the selected filters.');
        cell.colSpan = 6;
        row.append(cell);
        body.append(row);
        return;
    }
    rows.forEach((w) => {
        const row = el('tr');
        row.append(
            el('td', null, w.reference),
            el('td', null, w.createdAt.replace('T', ' ').slice(0, 19)),
            el('td', null, w.productName),
            el('td', 'text-end', money.format(w.amount)),
            el('td', 'text-end', money.format(w.balanceBefore)),
            el('td', 'text-end', money.format(w.balanceAfter)),
        );
        body.append(row);
    });
}

async function downloadCsv() {
    const error = validateFilters();
    setMessage('history-message', error, 'error');
    if (error) return;

    try {
        // fetch() instead of a plain link, because the Authorization header must be sent
        const response = await request(`/api/withdrawals/export?${filterParams()}`);
        const url = URL.createObjectURL(await response.blob());
        const link = el('a');
        link.href = url;
        link.download = 'withdrawal-statement.csv';
        document.body.append(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
    } catch (err) {
        handleError(err, 'history-message');
    }
}

// ---------- Login ----------

async function login(event) {
    event.preventDefault();
    const username = $('username').value.trim();
    const password = $('password').value;
    if (!username || !password) {
        setMessage('login-error', 'Please enter your username and password.', 'error');
        return;
    }

    sessionStorage.setItem(AUTH_KEY, basicAuth(username, password));
    try {
        await loadPortfolio();
        setMessage('login-error', '');
        showApp();
        await loadHistory();
    } catch (err) {
        sessionStorage.removeItem(AUTH_KEY);
        showLogin(err.status === 401 ? 'Invalid username or password.' : err.message);
    }
}

async function restoreSession() {
    if (!sessionStorage.getItem(AUTH_KEY)) {
        showLogin();
        return;
    }
    try {
        await loadPortfolio();
        showApp();
        await loadHistory();
    } catch (err) {
        logout();
    }
}

// ---------- Wiring ----------

$('login-form').addEventListener('submit', login);
$('logout-btn').addEventListener('click', () => logout());
$('withdraw-form').addEventListener('submit', submitWithdrawal);
$('product-select').addEventListener('change', () => {
    updateAmountHint();
    if ($('amount').value) showAmountError(validateAmount());
});
$('amount').addEventListener('input', () => showAmountError(validateAmount()));
$('filter-form').addEventListener('submit', (event) => {
    event.preventDefault();
    loadHistory();
});
$('download-btn').addEventListener('click', downloadCsv);

restoreSession();
