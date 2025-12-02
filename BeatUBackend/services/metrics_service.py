from __future__ import annotations

from database.models import InteractionMetric, PlaybackMetric
from schemas.api import MetricsInteraction, MetricsPlayback
from sqlalchemy.orm import Session


class MetricsService:
    def __init__(self, db: Session) -> None:
        self.db = db

    def record_playback(self, payload: MetricsPlayback) -> None:
        entity = PlaybackMetric(
            video_id=payload.video_id,
            fps=payload.fps,
            start_up_ms=payload.start_up_ms,
            rebuffer_count=payload.rebuffer_count,
            memory_mb=payload.memory_mb,
            channel=payload.channel,
        )
        self.db.add(entity)
        self.db.commit()

    def record_interaction(self, payload: MetricsInteraction) -> None:
        entity = InteractionMetric(
            event=payload.event,
            video_id=payload.video_id,
            latency_ms=payload.latency_ms,
            success=payload.success,
        )
        self.db.add(entity)
        self.db.commit()



