# Kế hoạch recommendation và RAG tương lai

> Đây là P2 chưa cam kết.
> Không triển khai trước khi P0/P1 trong [roadmap](roadmap.md) hoàn tất.

## Nguyên tắc

- MySQL vẫn là nguồn sự thật cho sản phẩm, giá, trạng thái và đơn hàng.
- LLM không được tạo tên món, giá, coupon hoặc product ID ngoài danh sách backend cho phép.
- Không gửi tên, email, số điện thoại, địa chỉ, JWT hay ghi chú giao hàng cho provider.
- Luồng gợi ý phải có fallback không AI và không được làm hỏng catalog/checkout.
- Người dùng có thể tắt cá nhân hóa và xóa dữ liệu liên quan.

## Lộ trình

| Pha | Nội dung | Cổng qua pha |
| --- | --- | --- |
| `R0 Baseline` | Món bán chạy theo đơn `COMPLETED`, category, giá và khung giờ | Có metric Precision/Recall/CTR cơ sở và API fallback |
| `R1 Semantic` | Metadata sản phẩm, embedding có version và vector retrieval | Mọi ID được lọc lại bằng DB; re-index lặp lại được |
| `R2 Personal rank` | Preference profile ẩn danh, recency, affinity và diversity | Vượt baseline offline; privacy test đạt |
| `R3 Explanation` | LLM chỉ viết lý do từ candidate allowlist, JSON schema và template fallback | Không hallucinate ID/giá; timeout không làm lỗi trang |
| `R4 Experiment` | Feature flag, A/B test, theo dõi click/add-to-cart/purchase | Chất lượng không kém baseline; chi phí trong ngân sách |

## Kiến trúc dự kiến

```text
Completed orders -> preference profile ----\
                                         hybrid rank -> DB validation -> Top N
Product metadata -> vector retrieval -----/                         |
                                                                  optional LLM reason
```

## Backlog tối thiểu trước khi code

- [ ] Chốt provider abstraction, vector store và feature flag.
- [ ] Chốt metadata sản phẩm và version embedding.
- [ ] Thiết kế event `IMPRESSION`, `CLICK`, `ADD_TO_CART`, `PURCHASE`, `DISMISS`.
- [ ] Viết privacy threat model và retention policy.
- [ ] Tạo baseline/evaluation dataset ẩn danh.
- [ ] Định nghĩa API recommendation, timeout, rate limit, cache và circuit breaker.
- [ ] Viết test provider giả; CI không gọi API trả phí thật.
- [ ] Test prompt injection, malformed output, ID ngoài allowlist và provider 4xx/5xx.

## Definition of Done

- Baseline vẫn hoạt động khi vector store/LLM down.
- Tất cả kết quả được kiểm tra active, giá và tồn kho từ DB sau retrieval.
- Payload provider không chứa dữ liệu cá nhân hoặc secret.
- Embedding, preference profile và thuật toán đều có version/rollback.
- Có metric so sánh với baseline, theo dõi latency/cost và quyền opt-out.
