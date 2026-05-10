(function () {
  'use strict';
  var modal = window.studysyncerModal;
  if (!modal) { console.error('modal.js must load before tracker.js'); return; }

  // Goal editing — reuse shared helper from today.js if available, otherwise inline equivalent.
  if (typeof window.initGoalEditing === 'function') {
    window.initGoalEditing();
  } else {
    var goalForm = document.getElementById('goalForm');
    var goalInput = goalForm ? goalForm.querySelector('input[name=minutesPerDay]') : null;
    if (goalForm) {
      document.addEventListener('click', function (e) {
        var trig = e.target.closest('[data-action="goal-edit"]');
        if (trig) {
          e.preventDefault();
          goalInput.value = parseInt(trig.dataset.current, 10) || 150;
          modal.open('goalModalBackdrop');
          return;
        }
        var preset = e.target.closest('.preset-btn');
        if (preset) { e.preventDefault(); goalInput.value = preset.dataset.preset; goalInput.focus(); }
      });
      goalForm.addEventListener('submit', function (e) {
        e.preventDefault();
        goalForm.dataset.method = 'PUT';
        goalForm.dataset.url = '/api/daily-goal';
        modal.submit(goalForm)
          .then(function () { modal.close(); modal.toast('Daily target updated', 'success'); window.location.reload(); })
          .catch(function () {});
      });
    }
  }

  // ── Session modal ──
  var form = document.getElementById('sessionForm');
  var titleEl = document.getElementById('sessionModalTitle');
  var submitBtn = form ? form.querySelector('button[type=submit]') : null;
  var deleteBtn = document.getElementById('sessionDeleteBtn');
  var menuSessionId = null;
  var menuSessionData = null;

  function setMode(mode, data) {
    if (!form) return;
    var picker = form.querySelector('.course-picker');
    form.reset();
    picker.querySelectorAll('.course-pick').forEach(function (b) { b.classList.remove('selected'); });
    form.querySelector('input[name=courseId]').value = '';
    form.querySelector('input[name=taskId]').value = '';

    if (mode === 'create') {
      titleEl.textContent = 'Log session';
      submitBtn.textContent = submitBtn.dataset.submitLabelCreate;
      form.dataset.method = 'POST';
      form.dataset.url = '/api/sessions';
      deleteBtn.style.display = 'none';
      deleteBtn.dataset.sessionId = '';

      // Default: now rounded down to last 30 min
      var n = new Date();
      n.setMinutes(Math.floor(n.getMinutes() / 30) * 30, 0, 0);
      var pad = function (x) { return String(x).padStart(2, '0'); };
      var local = n.getFullYear() + '-' + pad(n.getMonth() + 1) + '-' + pad(n.getDate())
                  + 'T' + pad(n.getHours()) + ':' + pad(n.getMinutes());
      form.querySelector('[name=startedAtLocal]').value = local;
      form.querySelector('[name=durationMinutes]').value = 50;

      var firstPick = picker.querySelector('.course-pick');
      if (firstPick) {
        firstPick.classList.add('selected');
        form.querySelector('input[name=courseId]').value = firstPick.dataset.courseId;
      }
    } else {
      titleEl.textContent = 'Edit session';
      submitBtn.textContent = submitBtn.dataset.submitLabelUpdate;
      form.dataset.method = 'PUT';
      form.dataset.url = '/api/sessions/' + data.id;
      deleteBtn.style.display = '';
      deleteBtn.dataset.sessionId = data.id;

      if (data.title) form.querySelector('[name=title]').value = data.title;
      if (data.italic) form.querySelector('[name=italicSuffix]').value = data.italic;
      if (data.started) form.querySelector('[name=startedAtLocal]').value = data.started;
      if (data.duration) form.querySelector('[name=durationMinutes]').value = data.duration;
      if (data.pomos != null && data.pomos !== '') form.querySelector('[name=completedPomodoros]').value = data.pomos;
      if (data.type) form.querySelector('[name=sessionType]').value = data.type;
      if (data.courseId) {
        var pick = picker.querySelector('.course-pick[data-course-id="' + data.courseId + '"]');
        if (pick) {
          pick.classList.add('selected');
          form.querySelector('input[name=courseId]').value = data.courseId;
        }
      }
      form.querySelector('input[name=taskId]').value = data.taskId || '';
    }
  }

  document.addEventListener('click', function (e) {
    if (e.target.closest('[data-action="session-new"]')) {
      e.preventDefault();
      setMode('create');
      modal.open('sessionModalBackdrop');
      return;
    }
    var menuTrig = e.target.closest('[data-action="session-menu"]');
    if (menuTrig) {
      e.preventDefault(); e.stopPropagation();
      menuSessionId = menuTrig.dataset.sessionId;
      menuSessionData = {
        id: menuTrig.dataset.sessionId,
        courseId: menuTrig.dataset.courseId,
        taskId: menuTrig.dataset.taskId,
        title: menuTrig.dataset.title,
        italic: menuTrig.dataset.italic,
        started: menuTrig.dataset.started,
        duration: menuTrig.dataset.duration,
        pomos: menuTrig.dataset.pomos,
        type: menuTrig.dataset.type
      };
      modal.openRowMenu('sessionRowMenu', menuTrig);
      return;
    }
    var menuAction = e.target.closest('#sessionRowMenu button[data-menu-action]');
    if (menuAction) {
      var act = menuAction.dataset.menuAction;
      modal.closeRowMenu();
      if (act === 'edit') {
        setMode('edit', menuSessionData);
        modal.open('sessionModalBackdrop');
      } else if (act === 'delete') {
        if (!confirm('Delete this session? Your weekly stats and streak will recalculate.')) return;
        modal.delete('/api/sessions/' + menuSessionId)
          .then(function () { modal.toast('Session deleted', 'success'); window.location.reload(); })
          .catch(function (err) { modal.toast(err.message || 'Could not delete', 'error'); });
      }
      return;
    }
    if (e.target.id === 'sessionDeleteBtn') {
      var sid = e.target.dataset.sessionId;
      if (!sid) return;
      if (!confirm('Delete this session? Your weekly stats and streak will recalculate.')) return;
      modal.delete('/api/sessions/' + sid)
        .then(function () { modal.close(); modal.toast('Session deleted', 'success'); window.location.reload(); })
        .catch(function (err) { modal.toast(err.message || 'Could not delete', 'error'); });
    }
  });

  if (form) {
    form.addEventListener('submit', function (e) {
      e.preventDefault();
      modal.submit(form)
        .then(function () {
          modal.close();
          modal.toast(form.dataset.method === 'PUT' ? 'Session updated' : 'Session logged', 'success');
          window.location.reload();
        })
        .catch(function () { /* errors shown by modal.submit */ });
    });
  }

  document.addEventListener('keydown', function (e) {
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA' || e.target.tagName === 'SELECT') return;
    if (e.key === 'l' || e.key === 'L') {
      e.preventDefault();
      setMode('create');
      modal.open('sessionModalBackdrop');
    }
  });
})();
