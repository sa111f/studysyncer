(function (global) {
  'use strict';

  function csrfHeader() {
    var token = document.querySelector('meta[name="_csrf"]')?.content;
    var header = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    return token ? { [header]: token } : {};
  }

  var openBackdrop = null;

  function openModal(id) {
    var b = document.getElementById(id);
    if (!b) return;
    closeModal();
    b.classList.add('open');
    openBackdrop = b;
    setTimeout(function () {
      var first = b.querySelector('input, select, textarea');
      if (first) first.focus();
    }, 50);
    document.body.style.overflow = 'hidden';
  }

  function closeModal() {
    if (openBackdrop) {
      openBackdrop.classList.remove('open');
      openBackdrop.querySelectorAll('.has-error').forEach(function (el) { el.classList.remove('has-error'); });
      openBackdrop.querySelectorAll('.err').forEach(function (el) { el.textContent = ''; });
      var banner = openBackdrop.querySelector('.form-error-banner');
      if (banner) { banner.classList.remove('show'); banner.textContent = ''; }
      openBackdrop = null;
    }
    document.body.style.overflow = '';
  }

  document.addEventListener('click', function (e) {
    if (e.target.classList && e.target.classList.contains('modal-backdrop')) {
      closeModal();
    }
    if (e.target.closest && e.target.closest('[data-modal-close]')) {
      closeModal();
    }
  });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
      closeModal();
      closeRowMenu();
    }
  });

  function submitForm(formEl, opts) {
    opts = opts || {};
    var method = opts.method || formEl.dataset.method || 'POST';
    var url = opts.url || formEl.dataset.url || formEl.action;
    var data = new FormData(formEl);

    formEl.querySelectorAll('.has-error').forEach(function (f) { f.classList.remove('has-error'); });
    formEl.querySelectorAll('.err').forEach(function (e) { e.textContent = ''; });
    var banner = formEl.querySelector('.form-error-banner') || formEl.closest('.modal')?.querySelector('.form-error-banner');
    if (banner) { banner.classList.remove('show'); banner.textContent = ''; }

    var submitBtn = formEl.querySelector('button[type=submit]');
    if (submitBtn) { submitBtn.disabled = true; }

    var body = new URLSearchParams();
    for (var pair of data.entries()) { body.append(pair[0], pair[1]); }

    return fetch(url, {
      method: method,
      headers: Object.assign({ 'Content-Type': 'application/x-www-form-urlencoded' }, csrfHeader()),
      body: body.toString(),
      credentials: 'same-origin'
    }).then(function (r) {
      return r.json().catch(function () { return { ok: r.ok, message: 'Server returned ' + r.status }; }).then(function (j) {
        if (r.ok && j.ok) return j;
        if (j.fieldErrors) {
          Object.keys(j.fieldErrors).forEach(function (k) {
            var fieldEl = formEl.querySelector('[data-field="' + k + '"]');
            if (fieldEl) {
              fieldEl.classList.add('has-error');
              var errEl = fieldEl.querySelector('.err');
              if (errEl) errEl.textContent = j.fieldErrors[k];
            }
          });
        }
        if (banner && j.message) {
          banner.textContent = j.message;
          banner.classList.add('show');
        }
        throw j;
      });
    }).finally(function () {
      if (submitBtn) submitBtn.disabled = false;
    });
  }

  function doDelete(url) {
    return fetch(url, {
      method: 'DELETE',
      headers: csrfHeader(),
      credentials: 'same-origin'
    }).then(function (r) {
      return r.json().catch(function () { return { ok: r.ok }; }).then(function (j) {
        if (r.ok && j.ok) return j;
        throw j;
      });
    });
  }

  function toast(message, kind) {
    var stack = document.getElementById('toastStack');
    if (!stack) {
      stack = document.createElement('div');
      stack.id = 'toastStack';
      stack.className = 'toast-stack';
      document.body.appendChild(stack);
    }
    var t = document.createElement('div');
    t.className = 'toast' + (kind ? ' ' + kind : '');
    t.textContent = message;
    stack.appendChild(t);
    setTimeout(function () { t.style.opacity = '0'; t.style.transform = 'translateY(8px)'; }, 2400);
    setTimeout(function () { t.remove(); }, 2700);
  }

  var openMenu = null;
  function openRowMenu(menuId, anchorEl) {
    closeRowMenu();
    var m = document.getElementById(menuId);
    if (!m) return;
    var rect = anchorEl.getBoundingClientRect();
    m.style.top = (rect.bottom + 4) + 'px';
    m.style.left = Math.min(rect.right - 140, window.innerWidth - 156) + 'px';
    m.classList.add('open');
    openMenu = m;
    setTimeout(function () {
      document.addEventListener('click', clickOutsideMenu);
    }, 0);
  }
  function closeRowMenu() {
    if (openMenu) { openMenu.classList.remove('open'); openMenu = null; }
    document.removeEventListener('click', clickOutsideMenu);
  }
  function clickOutsideMenu(e) {
    if (openMenu && !openMenu.contains(e.target)) closeRowMenu();
  }

  document.addEventListener('click', function (e) {
    var btn = e.target.closest('.course-pick');
    if (!btn) return;
    e.preventDefault();
    var picker = btn.closest('.course-picker');
    if (!picker) return;
    picker.querySelectorAll('.course-pick').forEach(function (b) { b.classList.remove('selected'); });
    btn.classList.add('selected');
    var hidden = picker.parentElement.querySelector('input[type=hidden][name="' + picker.dataset.target + '"]');
    if (hidden) hidden.value = btn.dataset.courseId;
  });

  document.addEventListener('keydown', function (e) {
    if (!e.target.classList || !e.target.classList.contains('tag-input')) return;
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      var val = e.target.value.trim().replace(/^#/, '');
      if (!val) return;
      addTagChip(e.target.parentElement, val);
      e.target.value = '';
    } else if (e.key === 'Backspace' && e.target.value === '') {
      var chips = e.target.parentElement.querySelectorAll('.tag-chip');
      var last = chips[chips.length - 1];
      if (last) last.remove();
      syncTagsHidden(e.target.parentElement);
    }
  });
  document.addEventListener('click', function (e) {
    if (e.target.classList && e.target.classList.contains('x') && e.target.parentElement.classList.contains('tag-chip')) {
      var container = e.target.closest('.tag-chips');
      e.target.parentElement.remove();
      syncTagsHidden(container);
    }
  });
  function addTagChip(container, name) {
    var existing = Array.from(container.querySelectorAll('.tag-chip')).map(function (c) { return c.textContent.replace(/×$/, '').trim().toLowerCase(); });
    if (existing.includes(name.toLowerCase())) return;
    var input = container.querySelector('.tag-input');
    var chip = document.createElement('span');
    chip.className = 'tag-chip';
    chip.textContent = name;
    var x = document.createElement('span');
    x.className = 'x'; x.textContent = '×';
    chip.appendChild(x);
    container.insertBefore(chip, input);
    syncTagsHidden(container);
  }
  function syncTagsHidden(container) {
    var hidden = container.parentElement.querySelector('input[type=hidden][name="' + container.dataset.target + '"]');
    if (!hidden) return;
    var names = Array.from(container.querySelectorAll('.tag-chip')).map(function (c) {
      return c.textContent.replace(/×$/, '').trim();
    });
    hidden.value = names.join(',');
  }
  function seedTagChips(container, names) {
    container.querySelectorAll('.tag-chip').forEach(function (c) { c.remove(); });
    (names || []).forEach(function (n) { addTagChip(container, n); });
  }

  global.studysyncerModal = {
    open: openModal,
    close: closeModal,
    submit: submitForm,
    delete: doDelete,
    toast: toast,
    openRowMenu: openRowMenu,
    closeRowMenu: closeRowMenu,
    seedTagChips: seedTagChips,
    csrfHeader: csrfHeader
  };
})(window);
