INSERT INTO videos (
    id,
    play_url,
    cover_url,
    title,
    tags,
    duration_ms,
    orientation,
    author_id,
    author_name,
    author_avatar,
    like_count,
    comment_count,
    favorite_count,
    share_count,
    view_count,
    qualities
) VALUES (
    'video_001_beatu_demo',
    'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/Justin%20Bieber%20-%20Beauty%20And%20A%20Beat.mp4',
    'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/demo_cover.jpg',
    'BeatU 项目演示视频 - 沉浸式短视频体验',
    JSON_ARRAY('music', 'dance', 'demo'),
    300000,
    'PORTRAIT',
    'author_001_beatu',
    'BeatU_Demo_Author',
    'https://ucw-beatu.oss-cn-shenzhen.aliyuncs.com/demo_author.jpg',
    100,
    20,
    50,
    15,
    1000,
    JSON_ARRAY(
        JSON_OBJECT('label', '1080P', 'bitrate', 5000, 'resolution', '1920x1080', 'url', 'https://cdn.beatu.com/video1080.m3u8'),
        JSON_OBJECT('label', '720P', 'bitrate', 3000, 'resolution', '1280x720', 'url', 'https://cdn.beatu.com/video720.m3u8')
    )
);