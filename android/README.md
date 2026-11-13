# 📱 Emergency Mesh Rescue — Android Mobile Client

Ứng dụng di động cứu nạn khẩn cấp ngoại tuyến (Offline-First) trên nền tảng Android, phục vụ tìm kiếm cứu nạn trong điều kiện thảm họa thiên tai bão lũ, mất sóng 4G/5G và điện lưới.

> **Đồ án Cơ sở 4 — Trường Đại học Công nghệ Thông tin & Truyền thông Việt - Hàn (VKU)**  
> **Khoa:** Khoa học Máy tính  
> **Chuyên ngành:** Công nghệ Phần mềm  
> **Nhóm thực hiện:**  
> - **Trần Viết Đạt** (Web C2 Command Station & Backend)  
> - **Nguyễn Thị Bích Trâm** (Android Mobile Client & Mesh Protocol)  
> **Giảng viên hướng dẫn:** TS. Nguyễn Đỗ Công Pháp  

---

## 🌟 Tính Năng Cốt Lõi

1. **Nút Bấm SOS Khẩn Cấp (One-Touch SOS):**
   - Kích hoạt gửi tin cứu nạn tức thì kèm xung nhịp phản hồi xúc giác (Haptic Feedback).
   - Tự động lấy tọa độ GPS ngoại tuyến với độ chính xác cao.
2. **Khai Báo Hồ Sơ Y Tế Ngoại Tuyến (Offline Triage Profile):**
   - Khai báo nhóm máu (A, B, AB, O), tiền sử bệnh án, số lượng nạn nhân đang kẹt lại.
   - Lưu trữ an toàn cục bộ bằng **Jetpack DataStore** và **Room Database**.
3. **Mạng Tùy Biến Di Động (P2P Mesh Network):**
   - Tự động quét và phát hiện các thiết bị láng giềng qua **Wi-Fi Direct (Wi-Fi P2P)**.
   - Tiếp sóng đa bước nhảy (**Multi-hop Relay**) chuyển tiếp gói tin từ vùng cô lập về Trạm Chỉ Huy.
   - Dịch vụ chạy ngầm bền bỉ (**Foreground Service**) duy trì tiếp sóng ngay cả khi màn hình tắt.
4. **Động Cơ Định Tuyến Phi Tập Trung (`RoutingEngine`):**
   - Định tuyến hạn chế bão lặp (Controlled Flooding).
   - Cơ chế Time-to-Live (TTL), đếm bước nhảy (Hop Count), ghi lịch sử lộ trình (`routeHistory`).
   - Khử trùng lặp gói tin (`SeenPacketCache`) và bảo toàn dữ liệu bằng mã băm **SHA-256**.
5. **Bộ Công Cụ Sinh Tồn Dã Chiến:**
   - Đèn pin phát tín hiệu mã Morse SOS quốc tế (`... --- ...`).
   - Còi hú báo động âm lượng lớn.
   - La bàn sinh tồn định hướng thoát hiểm.
6. **Tương Tác 2 Chiều với Trạm Chỉ Huy (Reverse Dispatch):**
   - Nhận lệnh điều phối ứng cứu (`DISPATCH_CMD`) từ Trạm C2 dã chiến.
   - Phản hồi gói tin xác nhận (`ACK`) về máy chủ.

---

## 🛠️ Công Nghệ Sử Dụng

- **Ngôn ngữ:** Kotlin
- **UI Framework:** Jetpack Compose, Material3 (Tactical Dark Mode)
- **Kiến trúc:** MVVM + Clean Architecture + Repository Pattern
- **Bất đồng bộ:** Kotlin Coroutines & StateFlow
- **Mạng:** Java Socket TCP/IP, Android Wi-Fi P2P API (`WifiP2pManager`)
- **Lưu trữ:** Room Database, Jetpack DataStore Preferences

---

## 🚀 Cấu Trúc Thư Mục

```text
android/
├── app/
│   └── src/main/java/com/rescue/mesh/android/
│       ├── data/
│       │   ├── local/        # Room Database & UserPreferences (DataStore)
│       │   ├── model/        # MeshPacket data model & packet serialization
│       │   └── repository/   # MeshRepository (Quản lý kết nối & điều phối mạng)
│       ├── domain/
│       │   ├── routing/      # RoutingEngine & SeenPacketCache (Lõi P2P Mesh)
│       │   └── util/         # PacketFactory & ChecksumUtil (SHA-256)
│       ├── hardware/         # GPS, Flashlight SOS Morse, Siren, Compass
│       ├── network/
│       │   ├── p2p/          # WifiDirectManager (P2P Discovery)
│       │   └── socket/       # MeshSocketClient & MeshSocketServer
│       ├── service/          # MeshRelayService (Foreground Service tiếp sóng)
│       └── ui/
│           ├── components/   # PulseSosButton, TacticalCard, LiveGpsBadge
│           ├── screens/      # SosHome, Onboarding, RelayMonitor, Compass
│           └── theme/        # Màu sắc dã chiến Tactical Dark
```

---

## 🔗 Liên Kết Dự Án

- **Desktop Trạm Chỉ Huy C2:** [Dawtrin/emergency-mesh-desktop](https://github.com/Dawtrin/emergency-mesh-desktop)
- **Mobile Client:** [Dawtrin/EmergencyMeshRescue_Mobile](https://github.com/Dawtrin/EmergencyMeshRescue_Mobile)
