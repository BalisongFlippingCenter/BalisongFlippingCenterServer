UPDATE posts p
SET comment_count = (
    SELECT COUNT(*)
    FROM comments c
    WHERE c.post_id = p.id
);
