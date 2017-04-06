# --- !Ups

CREATE TABLE Tweet (
    id BIGINT NOT NULL PRIMARY KEY,
    created_at TIMESTAMP,
    text VARCHAR(255) NOT NULL,
    point POINT,
    lang VARCHAR(255),
    gid INTEGER,
    gname VARCHAR(255)
);

# --- !Downs

DROP TABLE Tweet;