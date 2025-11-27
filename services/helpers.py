from __future__ import annotations

import json
from typing import Any, Callable, Dict, Iterable, List, Optional


def parse_tag_list(value: Any) -> List[str]:
    if isinstance(value, list):
        return value
    if isinstance(value, str):
        try:
            data = json.loads(value)
            if isinstance(data, list):
                return [str(item) for item in data]
        except json.JSONDecodeError:
            return [value]
    return []


def parse_quality_list(value: Any) -> List[dict]:
    if isinstance(value, list):
        return value
    if isinstance(value, str):
        try:
            data = json.loads(value)
            if isinstance(data, list):
                return data
        except json.JSONDecodeError:
            return []
    return []


def parse_bool_map(iterable: Iterable[Any], key: Callable[[Any], str]) -> Dict[str, bool]:
    return {key(item): True for item in iterable}



