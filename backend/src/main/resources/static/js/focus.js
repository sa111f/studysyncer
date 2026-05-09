(function () {
    'use strict';

    var csrfToken = document.querySelector('meta[name="_csrf"]') &&
        document.querySelector('meta[name="_csrf"]').content;
    var csrfHeader = (document.querySelector('meta[name="_csrf_header"]') &&
        document.querySelector('meta[name="_csrf_header"]').content) || 'X-CSRF-TOKEN';

    function postForm(url, params) {
        var body = new URLSearchParams();
        Object.keys(params || {}).forEach(function (k) { body.append(k, params[k]); });
        var headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
        return fetch(url, {
            method: 'POST',
            headers: headers,
            body: body.toString(),
            credentials: 'same-origin'
        });
    }

    /* ─── Setup view: task select auto-populates title + course ─── */
    var setupForm = document.getElementById('setupForm');
    if (setupForm) {
        var taskSel = document.getElementById('taskSelect');
        var titleInput = document.getElementById('titleInput');
        var courseSel = document.getElementById('courseSelect');
        if (taskSel) {
            taskSel.addEventListener('change', function () {
                var opt = taskSel.selectedOptions[0];
                if (!opt) return;
                if (opt.dataset.title && titleInput) titleInput.value = opt.dataset.title;
                if (opt.dataset.courseId && courseSel) courseSel.value = opt.dataset.courseId;
            });
        }
        setupForm.addEventListener('submit', function () {
            if ('Notification' in window && Notification.permission === 'default') {
                try { Notification.requestPermission(); } catch (e) { /* ignore */ }
            }
        });
        return;
    }

    /* ─── Running view ─── */
    var root = document.getElementById('focusRoot');
    if (!root || root.dataset.mode !== 'running') return;

    var sessionId = parseInt(root.dataset.sessionId, 10);
    var startedAtMs = parseInt(root.dataset.startedAt, 10);
    var serverNowMs = parseInt(root.dataset.serverNow, 10);
    var plannedSeconds = parseInt(root.dataset.planned, 10);
    var sessionType = root.dataset.sessionType;
    var clockSkew = serverNowMs - Date.now();

    var timeEl = document.querySelector('.time');
    var battBody = document.getElementById('battBody');
    var battTip = document.getElementById('battTip');
    var doneOverlay = document.getElementById('doneOverlay');
    var pauseBtn = document.getElementById('pauseBtn');
    var skipForm = document.getElementById('skipForm');
    var battWrap = document.getElementById('battWrap');
    var pageEl = document.querySelector('.page');

    var COLS = 22, ROWS = 32;

    function buildBatteryGrid() {
        if (!battTip || !battBody) return;
        battTip.innerHTML = '';
        for (var i = 0; i < 16; i++) {
            var c = document.createElement('div');
            c.className = 'cell';
            battTip.appendChild(c);
        }
        battBody.innerHTML = '';
        var total = COLS * ROWS;
        for (var k = 0; k < total; k++) {
            var cell = document.createElement('div');
            cell.className = 'cell';
            battBody.appendChild(cell);
        }
    }

    function paintBattery(remainingFraction) {
        if (!battBody) return;
        var cells = battBody.children;
        var fillRows = Math.max(0, Math.min(ROWS, Math.round(ROWS * remainingFraction)));
        var fillThreshold = ROWS - fillRows;
        for (var r = 0; r < ROWS; r++) {
            for (var col = 0; col < COLS; col++) {
                var idx = r * COLS + col;
                if (r >= fillThreshold) cells[idx].classList.add('fill');
                else cells[idx].classList.remove('fill');
            }
        }
    }

    function fmtClock(secs) {
        if (secs < 0) secs = 0;
        var m = Math.floor(secs / 60);
        var s = secs % 60;
        return m + '<span class="colon">:</span>' + (s < 10 ? '0' : '') + s;
    }

    var paused = false;
    var pausedAtMs = null;
    var totalPausedMs = 0;
    var pomoCompletedThisTick = false;

    function elapsedSeconds() {
        var now = Date.now() + clockSkew;
        if (paused && pausedAtMs !== null) now = pausedAtMs;
        return Math.floor((now - startedAtMs - totalPausedMs) / 1000);
    }

    function tick() {
        var elapsed = elapsedSeconds();
        var remaining = Math.max(0, plannedSeconds - elapsed);
        if (timeEl) timeEl.innerHTML = fmtClock(remaining);
        paintBattery(plannedSeconds > 0 ? remaining / plannedSeconds : 0);
        if (remaining === 0 && !pomoCompletedThisTick) {
            pomoCompletedThisTick = true;
            onTimerComplete();
        }
    }

    function onTimerComplete() {
        try {
            var Ctor = window.AudioContext || window.webkitAudioContext;
            if (Ctor) {
                var ac = new Ctor();
                var o = ac.createOscillator();
                var g = ac.createGain();
                o.connect(g); g.connect(ac.destination);
                o.frequency.value = 660;
                g.gain.setValueAtTime(0.0001, ac.currentTime);
                g.gain.exponentialRampToValueAtTime(0.18, ac.currentTime + 0.05);
                g.gain.exponentialRampToValueAtTime(0.0001, ac.currentTime + 0.9);
                o.start();
                o.stop(ac.currentTime + 0.9);
            }
        } catch (e) { /* audio unsupported */ }

        if ('Notification' in window && Notification.permission === 'granted') {
            try {
                new Notification(
                    sessionType === 'WORK' ? 'Pomodoro complete' : 'Break over',
                    {
                        body: sessionType === 'WORK'
                            ? 'Time for a 5-minute break.'
                            : 'Back to work?'
                    }
                );
            } catch (e) { /* notifications unsupported */ }
        }

        if (sessionType === 'WORK') {
            postForm('/focus/pomodoro/complete', { sessionId: sessionId })
                .finally(function () { showDoneOverlay(); });
        } else {
            showDoneOverlay();
        }
    }

    function showDoneOverlay() {
        if (doneOverlay) doneOverlay.classList.add('show');
    }

    function startHeartbeat() {
        setInterval(function () {
            if (!paused) postForm('/focus/heartbeat', { sessionId: sessionId });
        }, 30000);
    }

    if (pauseBtn) {
        pauseBtn.addEventListener('click', function () {
            var label = pauseBtn.querySelector('.btn-label');
            if (paused) {
                totalPausedMs += (Date.now() + clockSkew) - pausedAtMs;
                paused = false; pausedAtMs = null;
                if (label) label.textContent = 'Pause';
            } else {
                paused = true; pausedAtMs = Date.now() + clockSkew;
                if (label) label.textContent = 'Resume';
            }
        });
    }

    document.addEventListener('keydown', function (e) {
        if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA' || e.target.tagName === 'SELECT') return;
        if (e.code === 'Space') {
            e.preventDefault();
            if (pauseBtn) pauseBtn.click();
        } else if (e.key === 's' || e.key === 'S') {
            if (skipForm) skipForm.submit();
        } else if (e.key === 'Escape') {
            if (pageEl) pageEl.classList.remove('fullscreen-mode');
        }
    });

    if (battWrap && pageEl) {
        battWrap.addEventListener('click', function () {
            pageEl.classList.toggle('fullscreen-mode');
        });
    }

    if ('Notification' in window && Notification.permission === 'default' && pauseBtn) {
        var reqOnce = function () {
            try { Notification.requestPermission(); } catch (e) { /* ignore */ }
            pauseBtn.removeEventListener('click', reqOnce);
        };
        pauseBtn.addEventListener('click', reqOnce, { once: true });
    }

    buildBatteryGrid();
    tick();
    setInterval(tick, 1000);
    startHeartbeat();
})();
