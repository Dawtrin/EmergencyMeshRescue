# 🛰️ Emergency Mesh Rescue System
### Hệ Thống Điều Hành Cứu Nạn Khẩn Cấp Ngoại Tuyến Qua Mạng Mesh Dã Chiến
> *"Khi viễn thông gục ngã trước thiên tai, mạng Mesh kết nối sự sống."*

---

## 📌 1. Giới Thiệu Tổng Quan (Project Overview)
**Emergency Mesh Rescue** là hệ thống cứu hộ cứu nạn dã chiến đa tầng, hoạt động **100% ngoại tuyến (Offline)**, được thiết kế chuyên biệt cho các tình huống thảm họa thiên tai nghiêm trọng (bão lũ, ngập lụt, sạt lở đất) khi lưới điện quốc gia và hạ tầng viễn thông di động (4G/5G, Internet) bị tê liệt hoàn toàn.

Hệ thống kết hợp giữa **Ứng dụng Di động Android dành cho Nạn nhân & Đội cứu hộ tại hiện trường** và **Trạm Chỉ Huy Tác Chiến C2 (Command & Control) trên nền tảng Web Cockpit Dã chiến**, liên lạc thông suốt qua giao thức định tuyến nguồn đa bước nhảy (**Multi-Hop Mesh Network**) trên nền TCP/IP.

* **Cơ sở đào tạo:** Trường Đại học Công nghệ Thông tin và Truyền thông Việt – Hàn (VKU), Đại học Đà Nẵng.
* **Tác giả:** Dawtrin (`dattrandn@gmail.com`)

---

## 🏗️ 2. Cấu Trúc Toàn Bộ Dự Án (Repository Architecture)

```text
EmergencyMeshRescue/
├── android/                         # Ứng dụng Di động Cứu nạn Dã chiến (Android / Kotlin)
│   ├── app/
│   │   ├── src/main/java/com/rescue/mesh/
│   │   │   ├── network/             # Wi-Fi Direct, TCP Client, Hotspot Gateway
│   │   │   ├── routing/             # RoutingEngine, SeenPacketCache, Multi-Hop Forwarder
│   │   │   ├── triage/              # START Triage Protocol, Medical Profile
│   │   │   └── ui/                  # Jetpack Compose / Modern Android UI
│   │   └── build.gradle.kts
│   └── build.gradle.kts
│
├── desktop/                         # Trạm Chỉ Huy Tác Chiến Dã Chiến (Spring Boot 3 / Java 21)
│   ├── src/main/java/com/rescue/mesh/
│   │   ├── config/                  # Security RBAC, WebSocket STOMP, Async Config
│   │   ├── controller/              # 13 REST Controllers (Dispatch, Dossier, NOC, etc.)
│   │   ├── db/                      # SQLite Tactical Database, DAOs, Thread-safe Pool
│   │   ├── model/                   # MeshPacket, RescueIncident, AuditLog, Triage
│   │   ├── routing/                 # Least-Load Balancer, SeenPacketCache, Checksum
│   │   ├── service/                 # Tactical C2 Controller, Inter-Agency 115/114/113
│   │   └── socket/                  # MeshTcpServer (Cổng 8888 dã chiến đa luồng)
│   ├── src/main/resources/static/   # Web C2 Cockpit Tactical UI (Pure Vanilla ES6 + Leaflet COP)
│   ├── docs/                        # Toàn bộ tài liệu đặc tả, hợp đồng dữ liệu, hướng dẫn
│   ├── scripts/                     # Kịch bản khởi chạy dã chiến (.bat, .py, ADB bridge)
│   └── pom.xml
│
└── README.md                        # Tài liệu hướng dẫn tích hợp toàn hệ thống
```

---

## ⚡ 3. Các Tính Năng Đột Phá

### 3.1. Phân hệ Trạm Chỉ Huy Tác Chiến (Desktop C2 Base Station)
- **Kiến trúc Dual-Engine Hybrid:**
  - **TCP Socket Server (Port 8888):** Tiếp nhận gói tin Mesh từ các Node/Điện thoại di động ngoài hiện trường với tốc độ cao, xác thực toàn vẹn bằng SHA-256 Checksum và tự động phản hồi ACK đảo chiều (Reverse ACK).
  - **Web Tactical Cockpit (Port 8080):** Giao diện tác chiến chuẩn quân sự NASA/DoD, hiển thị thời gian thực qua WebSocket STOMP (độ trễ < 50ms).
- **Bản đồ tác chiến COP (Common Operating Picture):** Tích hợp Leaflet bản đồ số ngoại tuyến, tự động định tuyến xe cứu thương/cano cứu hộ theo thuật toán OSRM và bán kính dã chiến.
- **START Triage & Hồ sơ Nạn nhân:** Tự động phân loại 4 màu cấp cứu (Đen, Đỏ, Vàng, Xanh), trích xuất thông tin định danh y tế (Nhóm máu, bệnh nền, dị ứng) và cấp thẻ Medical ID dã chiến.
- **Điều phối liên ngành 115 / 114 / 113:** Đẩy lệnh trực tiếp đến các trạm y tế, công an, cứu hỏa gần nhất trên địa bàn TP. Đà Nẵng.

### 3.2. Phân hệ Ứng dụng Di Động (Android Mobile App)
- **Tự động bắt tay Mesh (Zero-Config):** Tự động phát hiện và chuyển tiếp gói tin SOS qua các điện thoại trung gian (Relay Node) theo cơ chế Store-and-Forward mà không cần Internet hay trạm BTS.
- **Quét mã QR Ghép đôi Dã chiến:** Kết nối trạm chỉ huy trong 1 giây qua mã QR động chứa địa chỉ LAN IP và khóa bảo mật.
- **Tiết kiệm năng lượng khẩn cấp:** Thuật toán tối ưu chu kỳ truyền tin giúp thiết bị duy trì hoạt động liên tục trong vùng lũ từ 48 - 72 giờ.

---

## 🚀 4. Hướng Dẫn Khởi Chạy Nhanh (Quick Start)

### 4.1. Khởi chạy Trạm Chỉ Huy Desktop
* **Yêu cầu:** JDK 17 hoặc JDK 21, Apache Maven 3.8+.
* **Cách 1: Khởi chạy nhanh bằng script dã chiến:**
  ```cmd
  cd desktop
  CHAY_TRAM_CHI_HUY.bat
  ```
* **Cách 2: Khởi chạy bằng Maven:**
  ```bash
  cd desktop
  mvn spring-boot:run
  ```
* Truy cập Dashboard tác chiến tại trình duyệt: `http://localhost:8080` (hoặc `http://<IP_LAN>:8080`).

### 4.2. Xây dựng Ứng dụng Android
* **Yêu cầu:** Android Studio Hedgehog | 2023.1.1 trở lên, Android SDK 34, JDK 17.
* Mở thư mục `android/` trong Android Studio.
* Đồng bộ Gradle (`Sync Project with Gradle Files`) và nhấn **Run** (Shift + F10) lên thiết bị thật hoặc máy ảo Android.

---

## 📜 5. Hợp Đồng Giao Thức Gói Tin (Mesh Packet Protocol)
Định dạng gói tin `MeshPacket` chuẩn hóa JSON truyền qua cổng TCP 8888:
```json
{
  "packetId": "a1b2c3d4",
  "type": "SOS_DATA",
  "sourceNodeId": "NODE_A",
  "destinationNodeId": "BASE_STATION",
  "route": ["NODE_A", "RELAY_B1"],
  "hopCount": 1,
  "ttl": 5,
  "timestamp": 1700000000000,
  "payload": {
    "senderName": "Nguyễn Văn A",
    "alertType": "FLOOD_TRAPPED",
    "severity": "CRITICAL",
    "victimCount": 3,
    "phoneNumber": "0901234567",
    "bloodType": "O+",
    "medicalNotes": "Bị thương chân, có người già",
    "location": {
      "latitude": 15.9753,
      "longitude": 108.2532,
      "hasFix": true,
      "accuracyM": 5,
      "fixAgeMs": 120
    }
  },
  "checksum": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
}
```

---

## 🛡️ 6. Giấy Phép & Bản Quyền
Dự án được nghiên cứu và phát triển phục vụ mục đích cứu hộ cứu nạn cộng đồng và bảo vệ đề tài khoa học công nghệ dã chiến tại Đại học Công nghệ Thông tin và Truyền thông Việt – Hàn (VKU).
