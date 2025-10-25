// Định nghĩa cấu trúc cho một bài đánh giá
export interface Testimonial {
  name: string;
  profession: string;
  quote: string;
  image: string;
}

// Xuất (export) mảng dữ liệu để các component khác có thể import và sử dụng
export const ALL_TESTIMONIALS: Testimonial[] = [
  {
    name: 'Anh Minh Đức',
    profession: 'Giám đốc Marketing',
    quote: 'Dịch vụ tuyệt vời, giao diện dễ sử dụng. Dược sĩ tư vấn rất nhiệt tình và tôi nhận được hàng ngay trong ngày. Hoàn toàn tin tưởng vào chất lượng sản phẩm.',
    image: 'assets/images/doc4.webp'
  },
  {
    name: 'Chị Thu Huyền',
    profession: 'Kế toán trưởng',
    quote: 'Đây là nơi tôi tin tưởng gửi gắm sức khỏe của cả gia đình. Thuốc chính hãng, chất lượng, đội ngũ giao hàng nhanh. Mọi người có thể an tâm khi mua hàng ở đây.',
    image: 'https://placehold.co/80x80/E3F2FD/333?text=User2'
  },
  {
    name: 'Anh Quang Hải',
    profession: 'Nhân viên văn phòng',
    quote: 'Trước đây tôi không tin tưởng mua thuốc online, nhưng một lần đã thử và hoàn toàn bị thuyết phục. Hàng chính hãng, giá tốt. Giờ tôi chỉ mua thuốc ở đây cho tiện.',
    image: 'https://placehold.co/80x80/FFF3E0/333?text=User3'
  },
  {
    name: 'Chị Mai Lan',
    profession: 'Giáo viên',
    quote: 'Ứng dụng đặt lịch khám rất tiện lợi. Tôi có thể chủ động chọn bác sĩ và giờ khám mà không cần phải chờ đợi lâu như trước nữa. Rất hài lòng!',
    image: 'https://placehold.co/80x80/FCE4EC/333?text=User4'
  },
  {
    name: 'Chú Hùng Dũng',
    profession: 'Về hưu',
    quote: 'Các cháu dược sĩ tư vấn rất có tâm, giải thích cặn kẽ cách dùng thuốc. Tôi cảm thấy rất an tâm khi mua thuốc và được chăm sóc sức khỏe tại đây.',
    image: 'https://placehold.co/80x80/E0F2F1/333?text=User5'
  }
];
