# Kiến trúc hệ thống

```mermaid
flowchart LR
  Browser[Frontend HTML/JS] -->|REST JSON| API[Spring Boot API]
  API --> Security[JWT + Spring Security]
  API --> Service[Service layer]
  Service --> Repo[Spring Data JPA]
  Repo --> DB[(MySQL)]
```

Frontend giữ giỏ hàng trong `localStorage`; backend là nguồn quyết định cuối cho giá, coupon, quyền và trạng thái đơn. Flyway quản lý schema trong `backend/src/main/resources/db/migration`.
