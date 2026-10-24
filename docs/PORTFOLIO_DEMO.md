# OpsPulse-AI — คู่มือทดลองสำหรับ HR และผู้สัมภาษณ์

**เปิดทดลองทันที:** [OpsPulse-AI Guest Demo](https://opspulse-ai-gamma.vercel.app/demo)  
**GitHub:** [Praciller/opspulse-ai](https://github.com/Praciller/opspulse-ai)

ไม่ต้องสมัครสมาชิก ไม่ต้องมีบัญชีและไม่ต้องกรอกรหัสผ่าน เหมาะสำหรับดูผลงานด้าน **AI Engineering, Data Engineering, Backend และ Full-stack Engineering** ภายใน 1–3 นาที

> **ข้อสำคัญ:** ทุกชื่อบริษัท ตัวเลขคำสั่งซื้อ สินค้า ข้อมูล Supplier และผลวิเคราะห์ใน Guest Demo เป็น **Synthetic / Fictional Data** สำหรับสาธิตการออกแบบระบบ ไม่ใช่ข้อมูลลูกค้าจริงหรือ KPI ของธุรกิจจริง

## ทดลองภายใน 60 วินาที

1. เปิด [Guest Demo](https://opspulse-ai-gamma.vercel.app/demo) เพื่อดู Overview: จำนวนสินค้า, คำสั่งซื้อ, Risk Events และ Suppliers
2. เข้า **Risk events** ลองเลือกตัวกรอง `High` เพื่อดูหลักฐานที่ทำให้ระบบจัดลำดับความสำคัญของความเสี่ยง
3. เข้า **Reports** แล้วสลับ Inventory Risk, Supplier SLA, Order Delay และ Product Margin
4. เข้า **AI brief** เพื่อดูตัวอย่างแผนปฏิบัติการที่มีการอ้างอิง Risk ID และตัวเลขต้นทาง
5. หากสนใจด้านโค้ดและสถาปัตยกรรม ให้เปิด [Repository](https://github.com/Praciller/opspulse-ai) ต่อได้ทันที

## สิ่งที่โปรเจกต์แสดงให้เห็น

| ประเด็น | ตัวอย่างการนำไปใช้ |
| --- | --- |
| Data Engineering & Data Quality | ข้อมูลสินค้าและคำสั่งซื้อมีแบบจำลองที่เป็นระบบ ตรวจสอบเงื่อนไข Stock และความเสี่ยงจากข้อมูลต้นทาง |
| Rule-based Analytics | ใช้ Reorder Point, Order Delay และ Supplier SLA เป็นหลักฐานของ Risk Events |
| AI-assisted Workflows | แปลงความเสี่ยงที่มีหลักฐานเป็น Brief และ Action Plan พร้อมแนวคิด Rule-based Fallback |
| Backend Engineering | Java 21, Spring Boot, Security, JWT/RBAC, PostgreSQL, Flyway, REST API |
| Deployment & DevOps | Frontend Vercel, Backend Render, PostgreSQL, GitHub Actions CI |
| User Experience | มีหน้า Overview, รายงานโต้ตอบ, การค้นหาและตัวกรอง พร้อม Guest Mode ที่ไม่ต้องใช้ Credential |

**ข้อจำกัดในการสาธิต:** หน้า Guest แสดง *ตัวอย่างคงที่* ของ Rule-based Brief เท่านั้น ไม่ได้เรียก LLM จริงหรือสร้างคำสั่งซื้อและไม่ควรใช้เป็นหลักฐานของประสิทธิภาพ Model ใด ๆ

## Guest Mode ต่างจากระบบจริงอย่างไร?

| | Guest Demo | Authenticated Console |
| --- | --- | --- |
| URL | [`/demo`](https://opspulse-ai-gamma.vercel.app/demo) | [`/login`](https://opspulse-ai-gamma.vercel.app/login) |
| ต้องล็อกอิน | ไม่ต้อง | ต้อง |
| ข้อมูล | ข้อมูลจำลองที่ฝังใน Frontend | Backend API และ PostgreSQL |
| เปลี่ยนข้อมูล | ไม่ได้ | ตาม JWT และ RBAC ของผู้ใช้งาน |
| ต้องรอ Render Free Cold Start | ไม่ต้อง | อาจใช้เวลาหลายนาที |
| เรียก AI Model จริง | ไม่เรียก | ขึ้นอยู่กับการตั้งค่า Provider และ Rule-based Fallback |

**ด้านความปลอดภัย:** Guest ไม่ใช่บัญชี `VIEWER` ที่มี Token แต่เป็นหน้า Portfolio สาธารณะซึ่งแยกจาก Auth และ Backend API โดยตรง ไม่มี Database Query, Mutation หรือการเปิดเผย Secrets ผู้ใช้งานระบบจริงยังต้องยืนยันตัวตนตามปกติ

## อธิบายสถาปัตยกรรมสั้น ๆ ระหว่างสัมภาษณ์

```text
Guest /demo (Public)
  Vercel React + TypeScript → bundled synthetic fixtures (read-only)
  No API calls, no credentials, no database access

Authenticated Console /login
  Vercel React + TypeScript → HTTPS / JWT → Render Spring Boot
                                         → PostgreSQL / Flyway
                                         → Deterministic Risk Engine
                                         → AI Adapter + Rule-based Fallback
```

Source code: [Backend & Frontend](https://github.com/Praciller/opspulse-ai) · [Current deployment architecture](CURRENT_RELEASE_STATUS_2026-10-23.md)

## หมายเหตุสำหรับการตรวจรับ

หน้า Guest สามารถเปิดและทดลองได้ตลอดโดยไม่ต้อง Login แต่การทดสอบ End-to-End ของผู้ใช้จริงและการยืนยัน Neon Resource ที่ Render ใช้ยังติดตามแยกอยู่ใน [Issue #29](https://github.com/Praciller/opspulse-ai/issues/29) เพื่อไม่อ้างผลตรวจเกินหลักฐานที่มี
