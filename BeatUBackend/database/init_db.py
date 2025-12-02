from __future__ import annotations

import argparse

from sqlalchemy import create_engine

from core.config import settings
from database.models import Base


def init_db(drop_existing: bool = False) -> None:
    """Create database tables according to ORM models."""
    engine = create_engine(settings.database_url, future=True)
    if drop_existing:
        Base.metadata.drop_all(engine)
    Base.metadata.create_all(engine)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Initialize BeatU database schema.")
    parser.add_argument(
        "--drop",
        action="store_true",
        help="Drop existing tables before recreating them.",
    )
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    init_db(drop_existing=args.drop)
    print(f"Database initialized using {settings.database_url}")


