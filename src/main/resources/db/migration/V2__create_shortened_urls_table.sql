CREATE TABLE shortened_urls (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(8) NOT NULL UNIQUE,
    original_url VARCHAR(2048) NOT NULL,
    uid VARCHAR(46) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_short_code ON shortened_urls(short_code);
CREATE INDEX idx_shortened_urls_user_id ON shortened_urls(uid);
CREATE INDEX idx_created_at ON shortened_urls(created_at);
