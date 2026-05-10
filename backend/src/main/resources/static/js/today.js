// Shared goal-editing helper — exposed on window so tracker.js can reuse it without rebinding.
function initGoalEditing() {
  if (window.__goalEditingBound) return;
  window.__goalEditingBound = true;
  var modal = window.studysyncerModal;
  if (!modal) return;
  var form = document.getElementById('goalForm');
  if (!form) return;
  var input = form.querySelector('input[name=minutesPerDay]');

  document.addEventListener('click', function (e) {
    var trig = e.target.closest('[data-action="goal-edit"]');
    if (trig) {
      e.preventDefault();
      var current = parseInt(trig.dataset.current, 10) || 150;
      input.value = current;
      modal.open('goalModalBackdrop');
      return;
    }
    var preset = e.target.closest('.preset-btn');
    if (preset) {
      e.preventDefault();
      input.value = preset.dataset.preset;
      input.focus();
    }
  });

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    form.dataset.method = 'PUT';
    form.dataset.url = '/api/daily-goal';
    modal.submit(form)
      .then(function () {
        modal.close();
        modal.toast('Daily target updated', 'success');
        window.location.reload();
      })
      .catch(function () { /* errors shown by modal.submit */ });
  });
}

window.initGoalEditing = initGoalEditing;

(function () {
  'use strict';
  if (!window.studysyncerModal) return;
  initGoalEditing();
})();
