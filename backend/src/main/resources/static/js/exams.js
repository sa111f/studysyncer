(function () {
  'use strict';
  var modal = window.studysyncerModal;
  if (!modal) { console.error('modal.js must load before exams.js'); return; }

  var form = document.getElementById('examForm');
  var titleEl = document.getElementById('examModalTitle');
  var submitBtn = form ? form.querySelector('button[type=submit]') : null;
  var statusSel = form ? form.querySelector('select[name=status]') : null;
  var gradeField = document.getElementById('examGradeField');

  var menuExamId = null;
  var menuExamData = null;

  function setMode(mode, data) {
    var picker = form.querySelector('.course-picker');
    form.reset();
    picker.querySelectorAll('.course-pick').forEach(function (b) { b.classList.remove('selected'); });
    modal.seedTagChips(form.querySelector('.tag-chips'), []);
    form.querySelector('input[name=courseId]').value = '';
    form.querySelector('input[name=topics]').value = '';

    if (mode === 'create') {
      titleEl.textContent = 'Add exam';
      submitBtn.textContent = submitBtn.dataset.submitLabelCreate;
      form.dataset.method = 'POST';
      form.dataset.url = '/api/exams';
      var firstPick = picker.querySelector('.course-pick');
      if (firstPick) {
        firstPick.classList.add('selected');
        form.querySelector('input[name=courseId]').value = firstPick.dataset.courseId;
      }
    } else {
      titleEl.textContent = 'Edit exam';
      submitBtn.textContent = submitBtn.dataset.submitLabelUpdate;
      form.dataset.method = 'PUT';
      form.dataset.url = '/api/exams/' + data.id;
      form.querySelector('[name=title]').value = data.title || '';
      if (data.courseId) {
        var pick = picker.querySelector('.course-pick[data-course-id="' + data.courseId + '"]');
        if (pick) {
          pick.classList.add('selected');
          form.querySelector('input[name=courseId]').value = data.courseId;
        }
      }
      if (data.type) form.querySelector('[name=examType]').value = data.type;
      if (data.status) form.querySelector('[name=status]').value = data.status;
      if (data.starts) form.querySelector('[name=startsAtLocal]').value = data.starts;
      if (data.duration) form.querySelector('[name=durationMinutes]').value = data.duration;
      if (data.location) form.querySelector('[name=location]').value = data.location;
      if (data.grade) form.querySelector('[name=grade]').value = data.grade;
      if (data.topics) {
        modal.seedTagChips(form.querySelector('.tag-chips'),
          data.topics.split(',').map(function (s) { return s.trim(); }).filter(Boolean));
      }
    }
    toggleGradeField();
  }

  function toggleGradeField() {
    if (!statusSel || !gradeField) return;
    gradeField.style.display = statusSel.value === 'PAST' ? '' : 'none';
  }
  if (statusSel) statusSel.addEventListener('change', toggleGradeField);

  document.addEventListener('click', function (e) {
    var newBtn = e.target.closest('[data-action="exam-new"]');
    if (newBtn) {
      e.preventDefault();
      setMode('create');
      modal.open('examModalBackdrop');
      return;
    }
    var menuBtn = e.target.closest('[data-action="exam-menu"]');
    if (menuBtn) {
      e.preventDefault(); e.stopPropagation();
      menuExamId = menuBtn.dataset.examId;
      menuExamData = {
        id: menuBtn.dataset.examId,
        title: menuBtn.dataset.examTitle,
        courseId: menuBtn.dataset.examCourseId,
        type: menuBtn.dataset.examType,
        status: menuBtn.dataset.examStatus,
        starts: menuBtn.dataset.examStarts,
        duration: menuBtn.dataset.examDuration,
        location: menuBtn.dataset.examLocation,
        grade: menuBtn.dataset.examGrade,
        topics: menuBtn.dataset.examTopics
      };
      modal.openRowMenu('examRowMenu', menuBtn);
      return;
    }
    var menuAction = e.target.closest('#examRowMenu button[data-menu-action]');
    if (menuAction) {
      var action = menuAction.dataset.menuAction;
      modal.closeRowMenu();
      if (action === 'edit') {
        setMode('edit', menuExamData);
        modal.open('examModalBackdrop');
      } else if (action === 'addTopic') {
        promptAddTopic(menuExamId);
      } else if (action === 'delete') {
        if (!confirm('Delete this exam and all its topics? This cannot be undone.')) return;
        modal.delete('/api/exams/' + menuExamId)
          .then(function () { modal.toast('Exam deleted', 'success'); window.location.reload(); })
          .catch(function (err) { modal.toast(err.message || 'Could not delete', 'error'); });
      }
      return;
    }
    var addTopicBtn = e.target.closest('[data-action="topic-add"]');
    if (addTopicBtn) {
      e.preventDefault();
      promptAddTopic(addTopicBtn.dataset.examId);
      return;
    }
    var topicBtn = e.target.closest('[data-action="topic-cycle"]');
    if (topicBtn) {
      e.preventDefault();
      cycleTopic(topicBtn);
      return;
    }
  });

  function promptAddTopic(examId) {
    if (!examId) return;
    var name = prompt('New topic name:');
    if (name == null) return;
    name = name.trim();
    if (!name) return;
    var body = new URLSearchParams();
    body.append('name', name);
    fetch('/api/exams/' + examId + '/topics', {
      method: 'POST',
      headers: Object.assign({ 'Content-Type': 'application/x-www-form-urlencoded' }, modal.csrfHeader()),
      body: body.toString(),
      credentials: 'same-origin'
    }).then(function (r) {
      return r.json().then(function (j) {
        if (!r.ok || !j.ok) throw j;
        modal.toast('Topic added', 'success');
        window.location.reload();
      });
    }).catch(function (err) {
      modal.toast(err.message || 'Could not add topic', 'error');
    });
  }

  var ORDER = ['NEUTRAL', 'DONE', 'WEAK'];
  function cycleTopic(btn) {
    var current = btn.dataset.topicStatus || 'NEUTRAL';
    var prev = current;
    var prevClasses = btn.className;
    var next = ORDER[(ORDER.indexOf(current) + 1) % ORDER.length];
    var topicId = btn.dataset.topicId;

    btn.classList.remove('done', 'weak');
    if (next === 'DONE') btn.classList.add('done');
    else if (next === 'WEAK') btn.classList.add('weak');
    btn.dataset.topicStatus = next;

    var body = new URLSearchParams();
    body.append('status', next);
    fetch('/api/exams/topics/' + topicId, {
      method: 'PUT',
      headers: Object.assign({ 'Content-Type': 'application/x-www-form-urlencoded' }, modal.csrfHeader()),
      body: body.toString(),
      credentials: 'same-origin'
    }).then(function (r) {
      return r.json().then(function (j) {
        if (!r.ok || !j.ok) throw j;
        updateReadinessRing(j.examId, j.readinessPercent, j.dashOffset);
      });
    }).catch(function (err) {
      btn.className = prevClasses;
      btn.dataset.topicStatus = prev;
      modal.toast(err.message || 'Could not update topic', 'error');
    });
  }

  function updateReadinessRing(examId, percent, dashOffset) {
    var trigger = document.querySelector('[data-action="exam-menu"][data-exam-id="' + examId + '"]');
    if (!trigger) return;
    var card = trigger.closest('.exam-card') || trigger.closest('.hero');
    if (!card) return;
    var ring = card.querySelector('.ec-prep .ring, .ready-ring');
    if (!ring) return;
    var p = ring.querySelector('.p');
    var cap = ring.querySelector('.cap');
    if (p) p.setAttribute('stroke-dashoffset', dashOffset);
    if (cap) {
      cap.innerHTML = percent + '<span class="pct">%</span>';
    }
  }

  if (form) {
    form.addEventListener('submit', function (e) {
      e.preventDefault();
      modal.submit(form)
        .then(function () {
          modal.close();
          modal.toast(form.dataset.method === 'PUT' ? 'Exam updated' : 'Exam added', 'success');
          window.location.reload();
        })
        .catch(function () { /* errors already shown by modal.submit */ });
    });
  }

  document.addEventListener('keydown', function (e) {
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
    if (e.key === 'n' || e.key === 'N') {
      e.preventDefault();
      setMode('create');
      modal.open('examModalBackdrop');
    }
  });
})();
