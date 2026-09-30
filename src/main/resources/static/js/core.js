/* ==========================================================================
   CORE — shared frontend utilities. Load before any page script.
   Exposes a single global: window.Aff
   No dependencies. Utilities (esc, fetch, toast, modals, confirm) live here
   once and are reused on every page. Domain renderers (cards, rows, ...)
   belong in the owning page's script, not here.
   ========================================================================== */
(function () {
    'use strict';

    /* ------------------------------------------------------- escaping */
    function esc(s) {
        if (s === null || s === undefined) return '';
        const d = document.createElement('div');
        d.textContent = String(s);
        return d.innerHTML;
    }

    /* ------------------------------------------------------- data fetch */
    async function fetchJSON(url, opts) {
        const res = await fetch(url, opts);
        let data = null;
        if (res.status !== 204) {
            const ct = res.headers.get('content-type') || '';
            data = ct.indexOf('application/json') !== -1
                ? await res.json().catch(() => null)
                : await res.text().catch(() => null);
        }
        if (!res.ok) {
            let msg = 'Error: ' + res.status;
            if (data && typeof data === 'object' && data.error) msg = data.error;
            else if (typeof data === 'string' && data.trim()) msg = data.trim();
            const err = new Error(msg);
            err.status = res.status;
            err.data = data;
            throw err;
        }
        return data;
    }

    /* ----------------------------------------------------------- toast */
    function toastWrap() {
        let w = document.querySelector('.toast-wrap');
        if (!w) {
            w = document.createElement('div');
            w.className = 'toast-wrap';
            document.body.appendChild(w);
        }
        return w;
    }
    const TOAST_ICON = { success: '✓', error: '✕', info: 'ℹ' };
    function toast(msg, type) {
        type = type || 'info';
        const w = toastWrap();
        const t = document.createElement('div');
        t.className = 'toast toast-' + type;
        const icon = document.createElement('span');
        icon.className = 'toast-icon';
        icon.textContent = TOAST_ICON[type] || 'ℹ';
        const text = document.createElement('span');
        text.textContent = msg;
        t.appendChild(icon);
        t.appendChild(text);
        w.appendChild(t);
        requestAnimationFrame(() => t.classList.add('show'));
        setTimeout(() => {
            t.classList.remove('show');
            setTimeout(() => t.remove(), 320);
        }, 3200);
    }

    /* ------------------------------------------------------ form errors */
    function clearErrors() {
        document.querySelectorAll('.field.invalid').forEach(f => f.classList.remove('invalid'));
        document.querySelectorAll('[aria-invalid="true"]').forEach(e => e.removeAttribute('aria-invalid'));
        document.querySelectorAll('.form-error').forEach(e => { e.textContent = ''; });
    }
    function formError(inputEl, msg) {
        clearErrors();
        if (inputEl) {
            const field = inputEl.closest('.field');
            if (field) field.classList.add('invalid');
            inputEl.setAttribute('aria-invalid', 'true');
        }
        const scope = inputEl ? inputEl.closest('form') : null;
        const target = (scope && scope.querySelector('.form-error')) || document.querySelector('.form-error');
        if (target) target.textContent = msg;
    }

    /* ---------------------------------------------------------- modals */
    function openModal(el) { if (el) el.classList.add('active'); }
    function closeModal(el) { if (el) el.classList.remove('active'); }

    /* ---------------------------------------------------------- confirm */
    // Promise-based confirm. Self-contained: builds its own dialog overlay.
    function confirm(opts) {
        opts = opts || {};
        return new Promise((resolve) => {
            const overlay = document.createElement('div');
            overlay.className = 'overlay active';
            overlay.innerHTML =
                '<div class="dialog" role="alertdialog" aria-modal="true">' +
                '  <p></p>' +
                '  <div class="dialog-actions">' +
                '    <button type="button" class="btn btn-ghost" data-act="cancel">Cancel</button>' +
                '    <button type="button" class="btn ' + (opts.danger === false ? 'btn-primary' : 'btn-danger') + '" data-act="ok">' +
                esc(opts.confirmLabel || 'Confirm') + '</button>' +
                '  </div>' +
                '</div>';
            overlay.querySelector('p').textContent = opts.message || '';
            const okBtn = overlay.querySelector('[data-act="ok"]');
            const cancelBtn = overlay.querySelector('[data-act="cancel"]');
            const done = (val) => {
                overlay.remove();
                document.removeEventListener('keydown', onKey);
                resolve(val);
            };
            const onKey = (e) => { if (e.key === 'Escape') done(false); };
            cancelBtn.addEventListener('click', () => done(false));
            okBtn.addEventListener('click', () => done(true));
            overlay.addEventListener('click', (e) => { if (e.target === overlay) done(false); });
            document.addEventListener('keydown', onKey);
            document.body.appendChild(overlay);
            okBtn.focus();
        });
    }

    /* ----------------------------------------------------------- copy */
    function copy(text) {
        if (navigator.clipboard && navigator.clipboard.writeText) {
            return navigator.clipboard.writeText(text).then(
                () => toast('Copied: ' + text, 'success'),
                () => toast('Copy failed', 'error')
            );
        }
        // Fallback
        const ta = document.createElement('textarea');
        ta.value = text;
        ta.style.position = 'fixed';
        ta.style.opacity = '0';
        document.body.appendChild(ta);
        ta.select();
        try { document.execCommand('copy'); toast('Copied: ' + text, 'success'); }
        catch (e) { toast('Copy failed', 'error'); }
        ta.remove();
        return Promise.resolve();
    }

    /* ------------------------------------------------------- url helpers */
    function slugFromPath() {
        const params = new URLSearchParams(location.search);
        let s = params.get('slug');
        if (!s) {
            const parts = location.pathname.split('/').filter(Boolean);
            for (let i = parts.length - 1; i >= 0; i--) {
                const seg = parts[i];
                if (seg && !/^\d+$/.test(seg)) { s = seg; break; }
            }
        }
        return s || null;
    }

    /* --------------------------------------------------------- renderers */
    function emptyState(title, text, icon) {
        return '<div class="empty">' +
            '<div class="empty-icon">' + (icon || '📦') + '</div>' +
            '<h3 class="empty-title">' + esc(title) + '</h3>' +
            '<p class="empty-text">' + esc(text || '') + '</p>' +
            '</div>';
    }

    /* -------------------------------------------------------------- bind */
    // Generic delegated click handler for data-act buttons within a container.
    function bindActions(container, handlers) {
        if (!container) return;
        container.addEventListener('click', function (e) {
            const btn = e.target.closest('[data-act]');
            if (!btn) return;
            const fn = handlers[btn.getAttribute('data-act')];
            if (fn) fn(btn, e);
        });
    }

    // Escape key closes any active overlay (modals / dialogs).
    function bindEscape() {
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                document.querySelectorAll('.overlay.active').forEach(el => el.classList.remove('active'));
            }
        });
    }

    /* -------------------------------------------------------------- API */
    window.Aff = {
        esc: esc,
        fetchJSON: fetchJSON,
        toast: toast,
        clearErrors: clearErrors,
        formError: formError,
        openModal: openModal,
        closeModal: closeModal,
        confirm: confirm,
        copy: copy,
        slugFromPath: slugFromPath,
        emptyState: emptyState,
        bindActions: bindActions,
        bindEscape: bindEscape
    };
})();
