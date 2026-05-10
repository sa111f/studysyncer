(function () {
  'use strict';
  var modal = window.studysyncerModal;
  if (!modal) { console.error('modal.js must load before tasks.js'); return; }

  var form = document.getElementById('taskForm');
  var titleEl = document.getElementById('taskModalTitle');
  var submitBtn = form ? form.querySelector('button[type=submit]') : null;
  var menuTaskId = null;
  var menuTaskData = null;

  function setMode(mode, data) {
    var picker = form.querySelector('.course-picker');
    form.reset();
    picker.querySelectorAll('.course-pick').forEach(function (b) { b.classList.remove('selected'); });
    modal.seedTagChips(form.querySelector('.tag-chips'), []);
    form.querySelector('input[name=courseId]').value = '';
    form.querySelector('input[name=tags]').value = '';

    if (mode === 'create') {
      titleEl.textContent = 'New task';
      submitBtn.textContent = submitBtn.dataset.submitLabelCreate;
      form.dataset.method = 'POST';
      form.dataset.url = '/api/tasks';
      var firstPick = picker.querySelector('.course-pick');
      if (firstPick) {
        firstPick.classList.add('selected');
        form.querySelector('input[name=courseId]').value = firstPick.dataset.courseId;
      }
    } else {
      titleEl.textContent = 'Edit task';
      submitBtn.textContent = submitBtn.dataset.submitLabelUpdate;
      form.dataset.method = 'PUT';
      form.dataset.url = '/api/tasks/' + data.id;
      form.querySelector('[name=title]').value = data.title || '';
      form.querySelector('[name=italicSuffix]').value = data.italic || '';
      if (data.courseId) {
        var pick = picker.querySelector('.course-pick[data-course-id="' + data.courseId + '"]');
        if (pick) {
          pick.classList.add('selected');
          form.querySelector('input[name=courseId]').value = data.courseId;
        }
      }
      if (data.due) form.querySelector('[name=dueAtLocal]').value = data.due;
      if (data.estimate) form.querySelector('[name=estimatedMinutes]').value = data.estimate;
      if (data.priority) form.querySelector('[name=priority]').value = data.priority;
      if (data.tags) {
        modal.seedTagChips(form.querySelector('.tag-chips'),
          data.tags.split(',').map(function (s) { return s.trim(); }).filter(Boolean));
      }
    }
  }

  document.addEventListener('click', function (e) {
    var trigger = e.target.closest('[data-action="task-new"]');
    if (trigger) {
      e.preventDefault();
      setMode('create');
      modal.open('taskModalBackdrop');
      return;
    }
    var menuBtn = e.target.closest('[data-action="task-menu"]');
    if (menuBtn) {
      e.preventDefault();
      e.stopPropagation();
      menuTaskId = menuBtn.dataset.taskId;
      menuTaskData = {
        id: menuBtn.dataset.taskId,
        title: menuBtn.dataset.taskTitle,
        italic: menuBtn.dataset.taskItalic,
        courseId: menuBtn.dataset.taskCourseId,
        due: menuBtn.dataset.taskDue,
        estimate: menuBtn.dataset.taskEstimate,
        priority: menuBtn.dataset.taskPriority,
        tags: menuBtn.dataset.taskTags
      };
      modal.openRowMenu('taskRowMenu', menuBtn);
      return;
    }
    var menuAction = e.target.closest('#taskRowMenu button[data-menu-action]');
    if (menuAction) {
      var action = menuAction.dataset.menuAction;
      modal.closeRowMenu();
      if (action === 'edit') {
        setMode('edit', menuTaskData);
        modal.open('taskModalBackdrop');
      } else if (action === 'delete') {
        if (!confirm('Delete this task? This cannot be undone.')) return;
        modal.delete('/api/tasks/' + menuTaskId)
          .then(function () { modal.toast('Task deleted', 'success'); reloadList(); })
          .catch(function (err) { modal.toast(err.message || 'Could not delete', 'error'); });
      }
    }
  });

  if (form) {
    form.addEventListener('submit', function (e) {
      e.preventDefault();
      modal.submit(form)
        .then(function () {
          modal.close();
          modal.toast(form.dataset.method === 'PUT' ? 'Task updated' : 'Task created', 'success');
          reloadList();
        })
        .catch(function () {
          // Field-level errors and banner already rendered by modal.submit
        });
    });
  }

  function reloadList() {
    window.location.reload();
  }

  document.addEventListener('keydown', function (e) {
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
    if (e.key === 'n' || e.key === 'N') {
      e.preventDefault();
      setMode('create');
      modal.open('taskModalBackdrop');
    }
  });
})();
