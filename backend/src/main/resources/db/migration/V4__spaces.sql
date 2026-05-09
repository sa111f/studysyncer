-- New course detail fields
ALTER TABLE courses
    ADD COLUMN section TEXT,
    ADD COLUMN term TEXT,
    ADD COLUMN professor TEXT,
    ADD COLUMN room TEXT,
    ADD COLUMN credits INT,
    ADD COLUMN description TEXT;

-- Course meeting times (lectures, labs, etc — recurring weekly)
CREATE TABLE schedule_items (
    id                BIGSERIAL PRIMARY KEY,
    course_id         BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title             TEXT NOT NULL,
    location          TEXT,
    day_of_week       SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_minute      INT NOT NULL,
    duration_minutes  INT,
    item_type         TEXT NOT NULL DEFAULT 'LECTURE'
                          CHECK (item_type IN ('LECTURE','LAB','TUTORIAL','OTHER')),
    display_order     INT NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_schedule_items_course ON schedule_items(course_id);
