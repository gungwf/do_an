from sentence_transformers import SentenceTransformer, util

class SimpleAI:
    def __init__(self):
        # model nhỏ, nhanh cho demo
        self.model = SentenceTransformer("all-MiniLM-L6-v2")
        self.knowledge = {
            "đau đầu": "Bạn nên nghỉ ngơi, uống đủ nước và đo nhiệt độ. Nếu kéo dài >48 giờ, gặp bác sĩ.",
            "sốt": "Nếu sốt >38.5°C, nên đi khám. Uống nhiều nước, chườm mát.",
            "đau bụng": "Bạn có thể gặp bác sĩ tiêu hóa. Nếu đau dữ dội hoặc nôn, đến ngay cơ sở y tế."
        }
        # pre-encode keys
        self.keys = list(self.knowledge.keys())
        self.key_emb = self.model.encode(self.keys, convert_to_tensor=True)

    def reply(self, text: str):
        query_emb = self.model.encode(text, convert_to_tensor=True)
        scores = util.cos_sim(query_emb, self.key_emb)[0].cpu().tolist()
        best_idx = max(range(len(scores)), key=lambda i: scores[i])
        if scores[best_idx] > 0.45:   # threshold tune được
            return self.knowledge[self.keys[best_idx]]
        return "Xin lỗi, tôi chưa hiểu rõ. Bạn có thể mô tả thêm hoặc muốn kết nối với bác sĩ?"

ai = SimpleAI()
