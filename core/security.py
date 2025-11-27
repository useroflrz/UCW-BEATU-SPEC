from fastapi import Depends, Header, HTTPException, status

from core.config import settings


async def verify_api_key(x_api_key: str = Header(...)) -> None:
    if x_api_key != settings.api_key:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid API key")


def auth_dependency() -> Depends:
    return Depends(verify_api_key)


