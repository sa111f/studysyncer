CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    first_name      TEXT NOT NULL,
    last_name       TEXT NOT NULL,
    email           TEXT NOT NULL,
    password_hash   TEXT NOT NULL,
    avatar_initial  TEXT NOT NULL CHECK (LENGTH(avatar_initial) = 1),
    created_at      TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX idx_users_email ON users (email);

CREATE TABLE courses (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code           TEXT NOT NULL,
    name           TEXT NOT NULL,
    color_key      TEXT NOT NULL CHECK (color_key IN ('PHYSICS','CS','ENG','PHIL','MATH','BIO','CHEM','OTHER')),
    color_variant  TEXT NOT NULL CHECK (color_variant IN ('DEFAULT','DEEP')),
    display_order  INTEGER NOT NULL
);
CREATE INDEX idx_courses_user_id ON courses (user_id);

CREATE TABLE tags (
    id       BIGSERIAL PRIMARY KEY,
    user_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name     TEXT NOT NULL,
    CONSTRAINT uk_tags_user_name UNIQUE (user_id, name)
);
CREATE INDEX idx_tags_user_id ON tags (user_id);

CREATE TABLE tasks (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id          BIGINT REFERENCES courses(id) ON DELETE SET NULL,
    title              TEXT NOT NULL,
    italic_suffix      TEXT,
    due_at             TIMESTAMPTZ,
    estimated_minutes  INTEGER,
    priority           TEXT NOT NULL CHECK (priority IN ('LOW','NORMAL','HIGH')),
    status             TEXT NOT NULL CHECK (status IN ('PENDING','COMPLETED')),
    completed_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_tasks_user_id ON tasks (user_id);
CREATE INDEX idx_tasks_course_id ON tasks (course_id);

CREATE TABLE task_tags (
    task_id  BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    tag_id   BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, tag_id)
);
CREATE INDEX idx_task_tags_tag_id ON task_tags (tag_id);

CREATE TABLE exams (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id         BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title             TEXT NOT NULL,
    exam_type         TEXT NOT NULL CHECK (exam_type IN ('MIDTERM','FINAL','QUIZ')),
    starts_at         TIMESTAMPTZ NOT NULL,
    duration_minutes  INTEGER NOT NULL,
    location          TEXT NOT NULL,
    status            TEXT NOT NULL CHECK (status IN ('UPCOMING','PAST')),
    grade             TEXT,
    created_at        TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_exams_user_id ON exams (user_id);
CREATE INDEX idx_exams_course_id ON exams (course_id);

CREATE TABLE exam_topics (
    id             BIGSERIAL PRIMARY KEY,
    exam_id        BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    name           TEXT NOT NULL,
    status         TEXT NOT NULL CHECK (status IN ('NEUTRAL','DONE','WEAK')),
    display_order  INTEGER NOT NULL
);
CREATE INDEX idx_exam_topics_exam_id ON exam_topics (exam_id);

CREATE TABLE study_sessions (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id         BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    task_id           BIGINT REFERENCES tasks(id) ON DELETE SET NULL,
    title             TEXT NOT NULL,
    italic_suffix     TEXT,
    started_at        TIMESTAMPTZ NOT NULL,
    ended_at          TIMESTAMPTZ,
    duration_seconds  INTEGER
);
CREATE INDEX idx_study_sessions_user_id ON study_sessions (user_id);
CREATE INDEX idx_study_sessions_course_id ON study_sessions (course_id);
CREATE INDEX idx_study_sessions_task_id ON study_sessions (task_id);

CREATE TABLE pomodoros (
    id                BIGSERIAL PRIMARY KEY,
    session_id        BIGINT NOT NULL REFERENCES study_sessions(id) ON DELETE CASCADE,
    started_at        TIMESTAMPTZ NOT NULL,
    duration_seconds  INTEGER NOT NULL,
    completed         BOOLEAN NOT NULL
);
CREATE INDEX idx_pomodoros_session_id ON pomodoros (session_id);

CREATE TABLE daily_goals (
    user_id          BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    minutes_per_day  INTEGER NOT NULL DEFAULT 150,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL
);
