# ⚡ Antigravity AI - Jetpack Compose Native Mobile App

Android (ARM64-v8a) için özel olarak geliştirilmiş, Jetpack Compose ve Material 3 tabanlı modern AI asistan uygulaması.

---

## 🌟 Öne Çıkan Özellikler

- **🎨 Modern Material 3 & Glassmorphism Arayüzü:** Figma & Material AI tasarım prensiplerine uygun, neon indigo-mor vurgulu karanlık tema.
- **🛠️ Canlı Araç Çağrısı Kartları (Tool Cards):** Antigravity'nin çalıştırdığı terminal komutları (`run_command`), dosya okuma/yazma (`view_file`, `write_to_file`) ve aramaları açılır-kapanır interaktif kartlarla izleme.
- **📁 Çoklu Sohbet Yönetimi (Navigation Drawer):** Sohbetleri ayrı ayrı listeleme, geçmişe dönme ve yeni sohbet açabilme.
- **🎙️ Sesle Yazma (Speech-to-Text):** Android yerel ses tanıma servisi ile doğrudan sesle komut verme.
- **🔄 Kesintisiz Arka Plan & SSE Akışı:** Ekran veya uygulama kapansa dahi arka planda devam eden işlemleri yakalama ve senkronize etme.
- **⚡ ARMv8 / ARM64 Optimize:** Termux ve Android 10+ cihazlarda yüksek performans ve düşük pil tüketimi.

---

## 🏗️ Proje Mimarisi

```
app/src/main/java/com/antigravity/ai/
├── MainActivity.kt               # Ana aktivite & Edge-to-Edge Compose
├── data/
│   ├── api/                      # OkHttp & Server-Sent Events (SSE) istemcisi
│   ├── model/                    # Message, ToolCall, Conversation veri modelleri
│   └── repository/               # Veri akışı ve durum yönetimi deposu
└── ui/
    ├── components/               # ToolCard, MessageItem, CodeBlock, ChatDrawer, InputBar
    ├── screens/                  # ChatScreen ve karşılama arayüzü
    ├── theme/                    # Renkler, Tipografi, Material 3 Tema
    └── viewmodel/                # ChatViewModel ve reaktif durum akışı
```

---

## 🔌 Çift Arka Uç Desteği (AGY CLI + OpenCode)

Uygulama **aynı APK** içinde iki arka ucu da destekler; ayarlar panelinden (`⚙️ Ayarlar → Arka Uç`) seçilir veya **Otomatik** modda açık olan sunucuya bağlanır:

| Arka Uç | Sunucu | Varsayılan Port | Protokol |
|---------|--------|-----------------|----------|
| **AGY CLI** | `agy` TUI sunucusu | `127.0.0.1:8080` | HTTP REST + SSE (`/api/chat`, `/api/events`) |
| **OpenCode** | `opencode serve` | `127.0.0.1:4096` | HTTP REST + SSE (`/api/session`, `/api/event`) |

- **OpenCode modu:** `opencode serve --port 4096` (cihazda Termux'tan) çalıştırılır. Uygulama bu sunucuya bağlanır; opencode'un **izin istekleri** (`permission.v2.asked`) ve **kullanıcı soruları** (`question.v2.asked`) otomatik olarak onay dialoglarına dönüşür.
- Mimari: `data/api/ChatBackend` arayüzü + `AgyBackend` / `OpenCodeBackend` adapter'ları. Her iki backend, olaylarını ortak `StreamEvent` modeline çevirir; UI/ViewModel hangi arka ucun çalıştığından habersizdir.
- `OPENCODE_SERVER_PASSWORD` ile korunan sunucular için `OpenCodeApiService(baseUrl, password)` kullanılır.

## 🚀 GitHub Actions ile Otomatik APK Derleme

Her commit ve tag oluşturulduğunda GitHub Actions iş akışı (`build-apk.yml`) otomatik olarak tetiklenir ve ARM64-v8a uyumlu debug/release APK dosyasını derleyip GitHub Releases bölümünde yayınlar.
