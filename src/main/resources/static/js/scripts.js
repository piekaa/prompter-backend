/* ==========================================================================
   SCRIPTS PAGE — list + editor for teleprompter scripts.
   Depends on window.Aff (js/core.js). Talks to /api/public/scripts.
   ========================================================================== */
(function () {
    'use strict';

    const API = '/api/public/scripts';

    /* ------------------------------------------------------------ state */
    let scripts = [];          // [{id, title, content, updatedAt}]
    let currentId = null;      // null = new, unsaved script
    let dirty = false;
    let saving = false;

    /* ------------------------------------------------------------- dom */
    const $ = (sel) => document.querySelector(sel);
    const elList = $('#script-list');
    const elListEmpty = $('#list-empty');
    const elCount = $('#script-count');
    const elTitle = $('#script-title');
    const elContent = $('#script-content');
    const elStats = $('#editor-stats');
    const elSaveState = $('#save-state');
    const elSave = $('#btn-save');
    const elDelete = $('#btn-delete');

    /* -------------------------------------------------------- helpers */
    function relTime(iso) {
        if (!iso) return '';
        const s = Math.max(0, (Date.now() - new Date(iso).getTime()) / 1000);
        if (s < 60) return 'just now';
        if (s < 3600) return Math.floor(s / 60) + 'm ago';
        if (s < 86400) return Math.floor(s / 3600) + 'h ago';
        if (s < 86400 * 30) return Math.floor(s / 86400) + 'd ago';
        return new Date(iso).toLocaleDateString();
    }

    function wordCount(text) {
        const m = String(text).trim().match(/\S+/g);
        return m ? m.length : 0;
    }

    function renderStats() {
        const words = wordCount(elContent.value);
        const chars = elContent.value.length;
        elStats.textContent = words.toLocaleString() + ' words · ' + chars.toLocaleString() + ' chars';
    }

    function renderSaveState() {
        elSaveState.classList.toggle('dirty', dirty && !saving);
        elSaveState.textContent = saving ? 'Saving…' : (dirty ? 'Unsaved changes' : (currentId ? 'Saved' : 'New script'));
    }

    function setDirty(v) {
        dirty = v;
        renderSaveState();
    }

    /* ---------------------------------------------------------- list UI */
    function renderList() {
        elCount.textContent = scripts.length;
        elListEmpty.style.display = scripts.length ? 'none' : '';
        elList.innerHTML = scripts.map(s =>
            '<li class="script-item' + (s.id === currentId ? ' active' : '') + '" data-id="' + s.id + '">' +
            '  <div class="script-item-title">' + Aff.esc(s.title) + '</div>' +
            '  <div class="script-item-meta">' + relTime(s.updatedAt) + '</div>' +
            '</li>'
        ).join('');
    }

    function selectScript(id) {
        if (dirty && !confirm('You have unsaved changes. Discard them?')) return;
        const s = scripts.find(x => x.id === id);
        if (!s) return;
        currentId = s.id;
        elTitle.value = s.title;
        elContent.value = s.content;
        setDirty(false);
        renderList();
        renderStats();
    }

    function newScript() {
        if (dirty && !confirm('You have unsaved changes. Discard them?')) return;
        currentId = null;
        elTitle.value = '';
        elContent.value = '';
        setDirty(false);
        renderList();
        renderStats();
        elTitle.focus();
    }

    /* ------------------------------------------------------------- save */
    async function save() {
        const title = elTitle.value.trim();
        if (!title) {
            elTitle.classList.add('invalid');
            Aff.toast('Title is required', 'error');
            elTitle.focus();
            return;
        }
        elTitle.classList.remove('invalid');
        if (saving) return;
        saving = true;
        renderSaveState();
        elSave.disabled = true;
        const body = JSON.stringify({ title: title, content: elContent.value });
        try {
            let saved;
            if (currentId === null) {
                saved = await Aff.fetchJSON(API, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: body
                });
            } else {
                saved = await Aff.fetchJSON(API + '/' + currentId, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: body
                });
            }
            currentId = saved.id;
            setDirty(false);
            await refresh();
            Aff.toast('Script saved', 'success');
        } catch (e) {
            Aff.toast(e.message, 'error');
        } finally {
            saving = false;
            elSave.disabled = false;
            renderSaveState();
        }
    }

    /* ----------------------------------------------------------- delete */
    async function remove() {
        if (currentId === null) return;
        const s = scripts.find(x => x.id === currentId);
        const ok = await Aff.confirm({
            message: 'Delete "' + (s ? s.title : 'this script') + '"? This cannot be undone.',
            confirmLabel: 'Delete'
        });
        if (!ok) return;
        try {
            await Aff.fetchJSON(API + '/' + currentId, { method: 'DELETE' });
            Aff.toast('Script deleted', 'success');
            await refresh();
            if (scripts.length) selectScript(scripts[0].id);
            else newScript();
        } catch (e) {
            Aff.toast(e.message, 'error');
        }
    }

    /* ---------------------------------------------------------- refresh */
    // Re-fetch the list. Keeps the script open in the editor when it still
    // exists; otherwise opens the first one (only when the editor is not
    // holding unsaved edits).
    async function refresh() {
        scripts = await Aff.fetchJSON(API);
        const open = scripts.find(s => s.id === currentId);
        if (!open && !dirty) {
            if (scripts.length) {
                currentId = scripts[0].id;
                elTitle.value = scripts[0].title;
                elContent.value = scripts[0].content;
                setDirty(false);
            } else {
                currentId = null;
                setDirty(false);
            }
        } else if (!open && dirty) {
            // The open script was deleted elsewhere — drop the stale id, keep edits for a fresh save.
            currentId = null;
        }
        renderList();
        renderStats();
        renderSaveState();
    }

    /* ----------------------------------------------------------- events */
    elList.addEventListener('click', (e) => {
        const item = e.target.closest('.script-item');
        if (item) selectScript(Number(item.dataset.id));
    });

    elTitle.addEventListener('input', () => { setDirty(true); elTitle.classList.remove('invalid'); });
    elContent.addEventListener('input', () => { setDirty(true); renderStats(); });

    elSave.addEventListener('click', save);
    elDelete.addEventListener('click', remove);
    document.querySelectorAll('[data-new]').forEach(b => b.addEventListener('click', newScript));

    // Ctrl/Cmd+S saves instead of the browser dialog.
    document.addEventListener('keydown', (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
            e.preventDefault();
            save();
        }
    });

    // Warn before leaving with unsaved edits.
    window.addEventListener('beforeunload', (e) => {
        if (dirty) { e.preventDefault(); }
    });

    /* ------------------------------------------------------------- boot */
    (async function init() {
        try {
            scripts = await Aff.fetchJSON(API);
            renderList();
            renderStats();
            renderSaveState();
            if (scripts.length) {
                currentId = scripts[0].id;
                elTitle.value = scripts[0].title;
                elContent.value = scripts[0].content;
                setDirty(false);
                renderList();
                renderStats();
            } else {
                setDirty(false);
                renderSaveState();
            }
        } catch (e) {
            Aff.toast('Could not load scripts: ' + e.message, 'error');
        }
    })();
})();
