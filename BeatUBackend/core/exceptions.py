class BeatUError(Exception):
    """Base domain exception."""

    def __init__(self, message: str) -> None:
        super().__init__(message)
        self.message = message


class NotFoundError(BeatUError):
    """Raised when requested resource is missing."""


class UnauthorizedError(BeatUError):
    """Raised when request lacks required permissions."""


