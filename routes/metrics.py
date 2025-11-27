from __future__ import annotations

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from database.connection import get_db
from schemas.api import MetricsInteraction, MetricsPlayback, success_response
from services.metrics_service import MetricsService


router = APIRouter(tags=["metrics"])


def get_metrics_service(db: Session = Depends(get_db)) -> MetricsService:
    return MetricsService(db)


@router.post("/metrics/playback")
def record_playback(
    payload: MetricsPlayback, service: MetricsService = Depends(get_metrics_service)
):
    service.record_playback(payload)
    return success_response({"success": True})


@router.post("/metrics/interaction")
def record_interaction(
    payload: MetricsInteraction, service: MetricsService = Depends(get_metrics_service)
):
    service.record_interaction(payload)
    return success_response({"success": True})



