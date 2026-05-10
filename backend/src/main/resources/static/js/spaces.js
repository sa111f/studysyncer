(function () {
  'use strict';
  var modal = window.studysyncerModal;
  if (!modal) { console.error('modal.js must load before spaces.js'); return; }

  // ── Course modal ──
  var courseForm = document.getElementById('courseForm');
  var courseTitle = document.getElementById('courseModalTitle');
  var courseSubmit = courseForm ? courseForm.querySelector('button[type=submit]') : null;
  var courseDelete = document.getElementById('courseDeleteBtn');
  var schedRows = document.getElementById('scheduleRows');
  var schedTemplate = schedRows ? schedRows.querySelector('.sched-row[data-template]') : null;
  var addSchedBtn = document.getElementById('addScheduleRow');

  var menuCourseId = null;
  var menuCourseData = null;

  function clearScheduleRows() {
    if (!schedRows) return;
    schedRows.querySelectorAll('.sched-row:not([data-template])').forEach(function (r) { r.remove(); });
  }

  function addScheduleRow(data) {
    if (!schedTemplate) return null;
    var clone = schedTemplate.cloneNode(true);
    clone.removeAttribute('data-template');
    clone.style.display = '';
    if (data) {
      if (data.id != null) clone.querySelector('input[name=scheduleIds]').value = data.id;
      if (data.dayOfWeek != null) clone.querySelector('select[name=scheduleDays]').value = data.dayOfWeek;
      if (data.startTimeLocal) clone.querySelector('input[name=scheduleStartTimes]').value = data.startTimeLocal;
      if (data.durationMinutes != null) clone.querySelector('input[name=scheduleDurations]').value = data.durationMinutes;
      if (data.title) clone.querySelector('input[name=scheduleTitles]').value = data.title;
      if (data.location) clone.querySelector('input[name=scheduleLocations]').value = data.location;
      if (data.itemType) clone.querySelector('select[name=scheduleTypes]').value = data.itemType;
    }
    clone.querySelector('.sched-remove').addEventListener('click', function () { clone.remove(); });
    schedRows.appendChild(clone);
    return clone;
  }

  if (addSchedBtn) addSchedBtn.addEventListener('click', function () { addScheduleRow(null); });

  function setColor(key, variant) {
    if (!courseForm) return;
    courseForm.querySelector('input[name=colorKey]').value = key;
    courseForm.querySelector('input[name=colorVariant]').value = variant || 'DEFAULT';
    courseForm.querySelectorAll('.color-swatch').forEach(function (s) { s.classList.remove('selected'); });
    var match = courseForm.querySelector('.color-swatch[data-color-key="' + key + '"][data-color-variant="' + (variant || 'DEFAULT') + '"]');
    if (match) match.classList.add('selected');
  }

  if (courseForm) {
    courseForm.querySelectorAll('.color-swatch').forEach(function (sw) {
      sw.addEventListener('click', function () { setColor(sw.dataset.colorKey, sw.dataset.colorVariant); });
    });
  }

  function setCourseMode(mode, data) {
    if (!courseForm) return;
    courseForm.reset();
    clearScheduleRows();
    setColor('OTHER', 'DEFAULT');

    if (mode === 'create') {
      courseTitle.textContent = 'New space';
      courseSubmit.textContent = courseSubmit.dataset.submitLabelCreate;
      courseForm.dataset.method = 'POST';
      courseForm.dataset.url = '/api/courses';
      courseDelete.style.display = 'none';
      courseDelete.dataset.courseId = '';
      addScheduleRow(null);
    } else {
      courseTitle.textContent = 'Edit space';
      courseSubmit.textContent = courseSubmit.dataset.submitLabelUpdate;
      courseForm.dataset.method = 'PUT';
      courseForm.dataset.url = '/api/courses/' + data.id;
      courseForm.querySelector('[name=code]').value = data.code || '';
      courseForm.querySelector('[name=name]').value = data.name || '';
      if (data.section) courseForm.querySelector('[name=section]').value = data.section;
      if (data.term) courseForm.querySelector('[name=term]').value = data.term;
      if (data.professor) courseForm.querySelector('[name=professor]').value = data.professor;
      if (data.room) courseForm.querySelector('[name=room]').value = data.room;
      if (data.credits) courseForm.querySelector('[name=credits]').value = data.credits;
      if (data.description) courseForm.querySelector('[name=description]').value = data.description;
      setColor(data.colorKey || 'OTHER', data.colorVariant || 'DEFAULT');
      courseDelete.style.display = '';
      courseDelete.dataset.courseId = data.id;

      // Pre-fill schedule rows + backfill description from the API
      fetch('/api/courses/' + data.id, { credentials: 'same-origin' })
        .then(function (r) { return r.json(); })
        .then(function (j) {
          if (!j.ok) return;
          (j.course.schedule || []).forEach(function (s) { addScheduleRow(s); });
          if (j.course.description && !courseForm.querySelector('[name=description]').value) {
            courseForm.querySelector('[name=description]').value = j.course.description;
          }
        })
        .catch(function () { /* keep what's already populated */ });
    }
  }

  // ── Tag modal ──
  var tagForm = document.getElementById('tagForm');
  var tagTitle = document.getElementById('tagModalTitle');
  var tagSubmit = tagForm ? tagForm.querySelector('button[type=submit]') : null;
  var menuTagId = null;

  function setTagMode(mode, data) {
    if (!tagForm) return;
    tagForm.reset();
    if (mode === 'create') {
      tagTitle.textContent = 'New tag';
      tagSubmit.textContent = tagSubmit.dataset.submitLabelCreate;
      tagForm.dataset.method = 'POST';
      tagForm.dataset.url = '/api/tags';
    } else {
      tagTitle.textContent = 'Rename tag';
      tagSubmit.textContent = tagSubmit.dataset.submitLabelUpdate;
      tagForm.dataset.method = 'PUT';
      tagForm.dataset.url = '/api/tags/' + data.id;
      tagForm.querySelector('[name=name]').value = data.name || '';
    }
  }

  // ── Click delegation ──
  document.addEventListener('click', function (e) {
    if (e.target.closest('[data-action="space-new"]')) {
      e.preventDefault();
      setCourseMode('create');
      modal.open('courseModalBackdrop');
      return;
    }
    if (e.target.closest('[data-action="tag-new"]')) {
      e.preventDefault();
      setTagMode('create');
      modal.open('tagModalBackdrop');
      return;
    }
    var spaceMenuTrig = e.target.closest('[data-action="space-menu"]');
    if (spaceMenuTrig) {
      e.preventDefault(); e.stopPropagation();
      menuCourseId = spaceMenuTrig.dataset.courseId;
      menuCourseData = {
        id: spaceMenuTrig.dataset.courseId,
        code: spaceMenuTrig.dataset.courseCode,
        name: spaceMenuTrig.dataset.courseName,
        colorKey: spaceMenuTrig.dataset.colorKey,
        colorVariant: spaceMenuTrig.dataset.colorVariant,
        section: spaceMenuTrig.dataset.section,
        term: spaceMenuTrig.dataset.term,
        professor: spaceMenuTrig.dataset.professor,
        room: spaceMenuTrig.dataset.room,
        credits: spaceMenuTrig.dataset.credits,
        description: spaceMenuTrig.dataset.description
      };
      modal.openRowMenu('spaceRowMenu', spaceMenuTrig);
      return;
    }
    var spaceMenuAction = e.target.closest('#spaceRowMenu button[data-menu-action]');
    if (spaceMenuAction) {
      var act = spaceMenuAction.dataset.menuAction;
      modal.closeRowMenu();
      if (act === 'edit') {
        setCourseMode('edit', menuCourseData);
        modal.open('courseModalBackdrop');
      } else if (act === 'delete') {
        confirmDeleteCourse(menuCourseData);
      }
      return;
    }
    var tagEl = e.target.closest('[data-action="tag-menu"]');
    if (tagEl) {
      e.preventDefault();
      menuTagId = tagEl.dataset.tagId;
      modal.openRowMenu('tagRowMenu', tagEl);
      var menu = document.getElementById('tagRowMenu');
      menu.dataset.tagId = menuTagId;
      menu.dataset.tagName = tagEl.dataset.tagName;
      return;
    }
    var tagMenuAction = e.target.closest('#tagRowMenu button[data-menu-action]');
    if (tagMenuAction) {
      var act2 = tagMenuAction.dataset.menuAction;
      var menu2 = document.getElementById('tagRowMenu');
      var id = menu2.dataset.tagId;
      var name = menu2.dataset.tagName;
      modal.closeRowMenu();
      if (act2 === 'rename') {
        setTagMode('edit', { id: id, name: name });
        modal.open('tagModalBackdrop');
      } else if (act2 === 'delete') {
        if (!confirm('Delete tag #' + name + '? It will be removed from all tasks.')) return;
        modal.delete('/api/tags/' + id)
          .then(function () { modal.toast('Tag deleted', 'success'); window.location.reload(); })
          .catch(function (err) { modal.toast(err.message || 'Could not delete', 'error'); });
      }
      return;
    }
    if (e.target.id === 'courseDeleteBtn') {
      var cid = e.target.dataset.courseId;
      if (!cid) return;
      confirmDeleteCourse(menuCourseData);
    }
  });

  // Right-click on a tag also opens the tag mini-menu
  document.addEventListener('contextmenu', function (e) {
    var tagEl = e.target.closest('[data-action="tag-menu"]');
    if (!tagEl) return;
    e.preventDefault();
    menuTagId = tagEl.dataset.tagId;
    modal.openRowMenu('tagRowMenu', tagEl);
    var menu = document.getElementById('tagRowMenu');
    menu.dataset.tagId = menuTagId;
    menu.dataset.tagName = tagEl.dataset.tagName;
  });

  function confirmDeleteCourse(data) {
    if (!data || !data.id) return;
    var msg = 'Delete "' + (data.code || 'course') + '"?\n\n' +
              'Its EXAMS will be deleted (along with their topics).\n' +
              'Its STUDY SESSIONS will be deleted.\n' +
              'Its TASKS will lose their course attribution but be kept.\n' +
              'This cannot be undone.';
    if (!confirm(msg)) return;
    modal.delete('/api/courses/' + data.id)
      .then(function () { modal.toast('Space deleted', 'success'); window.location.reload(); })
      .catch(function (err) { modal.toast(err.message || 'Could not delete', 'error'); });
  }

  // ── Form submits ──
  if (courseForm) {
    courseForm.addEventListener('submit', function (e) {
      e.preventDefault();
      modal.submit(courseForm)
        .then(function () {
          modal.close();
          modal.toast(courseForm.dataset.method === 'PUT' ? 'Space updated' : 'Space created', 'success');
          window.location.reload();
        })
        .catch(function () { /* errors shown by modal.submit */ });
    });
  }
  if (tagForm) {
    tagForm.addEventListener('submit', function (e) {
      e.preventDefault();
      modal.submit(tagForm)
        .then(function () {
          modal.close();
          modal.toast(tagForm.dataset.method === 'PUT' ? 'Tag renamed' : 'Tag created', 'success');
          window.location.reload();
        })
        .catch(function () { /* errors shown */ });
    });
  }
})();
