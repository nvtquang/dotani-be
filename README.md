# DOTANI Backend

**DOTANI** là hệ thống quản lý đoàn viên cho Đoàn TNCS Hồ Chí Minh Phường Thượng Cát. Hệ thống số hóa công việc hằng ngày của tổ chức Đoàn cấp phường: từ hồ sơ đoàn viên ở từng tổ dân phố, sự kiện, đại hội, bảng tin, chat nội bộ đến đoàn phí, thi đua và biểu quyết.

Repository này là phần backend (REST API + WebSocket). Giao diện web nằm ở [dotani-fe](https://github.com/nvtquang/dotani-fe).

## Bài toán

Đoàn phường quản lý đoàn viên qua nhiều chi đoàn tổ dân phố (TDP). Trước đây danh sách, điểm danh, thu đoàn phí, xếp loại cuối năm đều làm trên giấy hoặc bảng tính rời rạc ở từng chi đoàn. DOTANI gom tất cả về một nơi, đồng thời bảo đảm mỗi cán bộ chỉ nhìn thấy và thao tác được trong phạm vi mình phụ trách.

## Tính năng

| Nhóm | Nội dung |
| --- | --- |
| **Tài khoản** | Đăng ký, đăng nhập bằng mật khẩu hoặc Google, quên mật khẩu qua email, khoá tạm khi đăng nhập sai nhiều lần |
| **Đoàn viên & tổ chức** | Hồ sơ đoàn viên, cây tổ chức phường → chi đoàn TDP, phân quyền chức vụ, ảnh đại diện, thông tin ngân hàng và QR Banking (số tài khoản được mã hoá khi lưu) |
| **Sự kiện** | Sự kiện, cuộc họp, đại hội; đăng ký tham gia, giới hạn số người, hạn đăng ký, tệp đính kèm, tự nhắc trước hạn |
| **Bảng tin** | Bài viết, thông báo, báo cáo hoạt động; bảng tin trộn sự kiện và bài viết; bình luận và bày tỏ cảm xúc |
| **Chat** | Chat 1-1 và nhóm thời gian thực qua WebSocket, gửi tệp, trạng thái đã xem, đang gõ |
| **Thông báo** | Tự động gửi khi có sự kiện mới, sự kiện đổi hoặc huỷ, bài viết mới, nhắc đoàn phí, kết quả chuyển sinh hoạt, biểu quyết mới |
| **Báo cáo thống kê** | Đoàn viên theo TDP, giới tính, độ tuổi; tỷ lệ tham gia sự kiện theo tháng/quý, theo TDP, theo từng sự kiện |
| **Đoàn phí** | Kỳ thu theo phường hoặc TDP, ghi nhận đóng, nhắc nợ; đoàn viên chuyển khoản qua QR Banking của người thu |
| **Chuyển sinh hoạt** | Đề nghị chuyển giữa các TDP, duyệt bởi Đoàn phường hoặc chi đoàn nơi đến, lưu lịch sử |
| **Thi đua** | Khen thưởng, kỷ luật theo quyết định; đánh giá, xếp loại đoàn viên hằng năm |
| **Khảo sát – biểu quyết** | Lấy ý kiến, bỏ phiếu một hoặc nhiều lựa chọn, bỏ phiếu kín, gắn với đại hội |
| **Nhật ký hệ thống** | Ghi lại mọi thao tác quản trị quan trọng: ai làm, lúc nào, thay đổi gì |

## Phân quyền

Quyền được xác định bởi chức vụ **và** phạm vi đơn vị. Backend kiểm tra cả hai ở mọi API; giao diện chỉ ẩn/hiện cho tiện.

| Chức vụ | Phạm vi |
| --- | --- |
| Bí thư phường | Toàn phường; là người duy nhất được phân quyền chức vụ |
| Phó bí thư phường | Toàn phường, trừ phân quyền |
| Bí thư / Phó bí thư TDP | Chỉ chi đoàn TDP của mình |
| Đoàn viên | Dữ liệu cá nhân và nội dung của phường, của TDP mình |

## Kiến trúc

Một ứng dụng Spring Boot dạng **modular monolith**: mỗi nghiệp vụ là một module riêng, dùng chung một cơ sở dữ liệu và một lớp bảo mật.

```
auth          tài khoản, JWT, Google, đặt lại mật khẩu
member        đoàn viên, tổ chức, phân quyền, QR Banking, chuyển sinh hoạt
event         sự kiện, đăng ký tham gia, nhắc hạn
content       bài viết, tệp đính kèm, bình luận, cảm xúc
feed          bảng tin trộn và tìm kiếm nhanh
chat          hội thoại, tin nhắn, WebSocket/STOMP
notification  thông báo, phát sinh từ domain event
fee           đoàn phí
emulation     khen thưởng, kỷ luật, xếp loại
poll          khảo sát, biểu quyết
report        báo cáo thống kê
audit         nhật ký thao tác
shared        bảo mật, phạm vi đơn vị, lỗi, thời gian
```

Một số nguyên tắc xuyên suốt:

- **Phạm vi đơn vị ở mọi truy vấn**: danh sách được lọc ngay trong câu truy vấn theo phường/TDP của người gọi, nên không lộ dữ liệu ngoài phạm vi.
- **Thông báo không chặn thao tác**: module nghiệp vụ phát domain event; thông báo được tạo sau khi giao dịch đã commit, chạy bất đồng bộ.
- **Xoá mềm**: đoàn viên, sự kiện, bài viết, bình luận, biểu quyết bị xoá vẫn được giữ để lịch sử (tin nhắn cũ, đăng ký cũ) không bị gãy.
- **Lỗi có mã ổn định và thông điệp tiếng Việt**: mỗi lỗi nghiệp vụ trả về một `code` cố định kèm câu giải thích sẵn sàng hiển thị.
- **Thời gian lưu theo UTC**, ngày tháng tính theo giờ Việt Nam.
- **Lược đồ CSDL do Flyway quản lý**, kiểm thử tích hợp chạy trên MySQL thật.

## Công nghệ

Java 21 · Spring Boot 3.3 · Spring Security + JWT · Spring Data JPA · Flyway · MySQL 8.4 · WebSocket/STOMP · OpenAPI/Swagger · JUnit 5 + MockMvc · Docker
