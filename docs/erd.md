# ERD chính

```mermaid
erDiagram
  APP_USER ||--o{ ORDERS : places
  ORDERS ||--|{ ORDER_ITEM : contains
  PRODUCT ||--o{ ORDER_ITEM : snapshots
  CATEGORY ||--o{ PRODUCT : groups
  PRODUCT ||--o{ PRODUCT_SIZE : offers
  COUPON ||--o{ ORDERS : applies
```
