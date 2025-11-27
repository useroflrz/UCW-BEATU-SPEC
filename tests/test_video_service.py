import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker

from database.models import Base, Video
from schemas.api import InteractionRequest
from services.video_service import VideoService


@pytest.fixture()
def db_session() -> Session:
    engine = create_engine("sqlite:///:memory:", future=True)
    Base.metadata.create_all(engine)
    TestingSession = sessionmaker(bind=engine)
    with TestingSession() as session:
        seed_video(session)
        yield session


def seed_video(session: Session) -> None:
    video = Video(
        id="video_test",
        play_url="https://cdn.beatu.com/video.mp4",
        cover_url="https://cdn.beatu.com/video.jpg",
        title="测试视频",
        tags=["test"],
        duration_ms=1000,
        orientation="PORTRAIT",
        author_id="author_1",
        author_name="Tester",
        like_count=0,
        comment_count=0,
        favorite_count=0,
        share_count=0,
        view_count=0,
        qualities=[
            {"label": "720P", "url": "https://cdn.beatu.com/video720.m3u8"},
        ],
    )
    session.add(video)
    session.commit()


def test_list_videos_returns_items(db_session: Session):
    service = VideoService(db_session)
    response = service.list_videos(page=1, limit=10, orientation=None, channel=None, user_id="user_a")
    assert response.total == 1
    assert response.items[0].title == "测试视频"


def test_like_video_mutates_state(db_session: Session):
    service = VideoService(db_session)

    result = service.like_video("video_test", InteractionRequest(action="LIKE"), user_id="user_a")
    assert result.success is True

    video = service.get_video("video_test", user_id="user_a")
    assert video.is_liked is True
    assert video.like_count == 1

    service.like_video("video_test", InteractionRequest(action="UNLIKE"), user_id="user_a")
    video = service.get_video("video_test", user_id="user_a")
    assert video.is_liked is False
    assert video.like_count == 0



