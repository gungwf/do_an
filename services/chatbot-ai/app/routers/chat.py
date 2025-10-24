from fastapi import APIRouter, HTTPException, Query
from app.database import db
from app.ai_engine import ai
from datetime import datetime
from pydantic import BaseModel

router = APIRouter(prefix="/chat", tags=["chat"])

class ChatRequest(BaseModel):
    user_id: str
    message: str

@router.post("/")
async def chat(req: ChatRequest):
    # tạo/ cập nhật conversation
    conv = await db.conversations.find_one({"user_id": req.user_id})
    if not conv:
        conv = {
            "user_id": req.user_id,
            "messages": [],
            "created_at": datetime.utcnow()
        }

    # bot trả lời
    reply = ai.reply(req.message)

    # thêm message vào conv
    conv["messages"].append({"role": "user", "content": req.message, "timestamp": datetime.utcnow()})
    conv["messages"].append({"role": "bot", "content": reply, "timestamp": datetime.utcnow()})

    # upsert
    await db.conversations.update_one({"user_id": req.user_id}, {"$set": conv}, upsert=True)

    return {"reply": reply}
