from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime

class Message(BaseModel):
    role: str  # "user" or "bot"
    content: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)

class Conversation(BaseModel):
    user_id: str
    messages: List[Message]
    context: Optional[str] = None
    created_at: datetime = Field(default_factory=datetime.utcnow)
