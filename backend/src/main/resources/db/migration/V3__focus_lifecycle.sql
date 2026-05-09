-- Focus Mode lifecycle: heartbeat tracking + session type + planned pomodoro duration
ALTER TABLE study_sessions
    ADD COLUMN last_heartbeat_at TIMESTAMPTZ;

ALTER TABLE study_sessions
    ADD COLUMN session_type TEXT NOT NULL DEFAULT 'WORK'
        CHECK (session_type IN ('WORK', 'BREAK'));

ALTER TABLE pomodoros
    ADD COLUMN planned_duration_seconds INT NOT NULL DEFAULT 1500;

CREATE INDEX IF NOT EXISTS idx_study_sessions_active
    ON study_sessions (user_id) WHERE ended_at IS NULL;
