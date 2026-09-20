# Antigravity Android - Günlük & Değişiklik Günlüğü (log.md)

## [2026-09-20] AGY + Codex Eşzamanlı Sohbet Sekmeleri
- Termux için `codex-serve` alias'ı belgelendi: `taskset -c 0-5 nice -n 15 /data/data/com.termux/files/usr/bin/codex app-server --listen ws://127.0.0.1:4500`.
- Özellik commit'i `b82be045382faaa06b46956b036ec99fd29455d3`, CI import düzeltmesi `cc11c19f62db75991c5236be9713ccf8611a513b` olarak `main` dalına pushlandı.
- CI doğrulaması başarılı: Android Lint & Unit Tests `35530671653`; Build & Release Antigravity AI APK `35530671658`.
- Sohbet alanına bağımsız `AGY` ve `Codex` sekmeleri eklendi; iki ayrı ViewModel sayesinde mesajlar, üretim durumu ve olay bağlantıları sekme değişiminde korunuyor.
- Codex CLI resmi app-server WebSocket/JSON-RPC protokolüyle bağlandı (`127.0.0.1:4500`): initialize, model/geçmiş listesi, thread açma/devam ettirme, streaming, terminal çıktıları, durdurma ve kullanıcı onayları destekleniyor.
- Codex app-server Termux `RUN_COMMAND` üzerinden `taskset -c 0-5 nice -n 15` sınırıyla otomatik başlatılıyor.
- Codex ve AGY ayarları/draft alanları ayrıldı; backend başlangıç yarışı ve iki kez AGY sunucusu başlatma riski giderildi.
- Mevcut SSE heartbeat ayrıştırma hatasının sessizce yutulması kaldırıldı. Codex RPC timeout/pending temizliği, socket kapanışları ve parça parça terminal çıktısı biriktirme davranışı güvenli hale getirildi.
- Gerçek Codex app-server üzerinde initialize, `thread/list` ve `model/list` semantik olarak doğrulandı. Android Gradle derlemesi proje kuralı gereği yerelde çalıştırılmadı; CI doğrulaması commit/push sonrasına kaldı.

## [2026-09-17] Input Bar Buton Taşma / Sıkışma Düzeltmesi ve Minimal Model Sunumu
- **Input Bar Buton Sıkışması ve Görünmezlik Engeli:** Sol eylem çubuğuna (`+` ve Model Seçici Hapı) `Modifier.weight(1f, fill = false)` eklendi ve sağ eylem çubuğu (`Sesli Yaz` + `Gönder / Live Waveform`) taşma ve sıkışmaya karşı korundu. Dar ekranlarda veya uzun model isimlerinde sağ butonların ekrandan dışarı taşması / görünmez olması tamamen engellendi.
- **Akıllı ve Minimal Model Hapı Formatı (`formatCompactModelPill`):** Giriş barındaki model hapında gereksiz parantez içi ağırlık etiketleri (`(Low)`, `(Medium)`, `(High)`, `(Thinking)`) temizlendi; akıl yürütme seviyesi `⚡` (Yüksek/Derin) veya `• Hızlı` (Düşük) şeklinde minimalleştirildi.
- **Minimal Model & Ağırlık Seçici (`QuickModelSelectorSheet` & `ModelSettingsDialog`):** Düşünme seviyesi çipleri kompakt ikonlu sekmelere (`⚡ Hızlı`, `⚖️ Dengeli`, `🧠 Derin`, `Standart`) dönüştürüldü; modeller listesinde karmaşık uzun açıklamalar yerine temiz başlıklar ve şık etiket rozetleri (`✦ Flash`, `✦ Pro`, `⚡ High`, `🧠 Thinking`) ile yüksek okunabilirlik sağlandı.

- **Termux Uploads Köprüsü & Sandbox İzolasyonu Düzeltmesi:** Çizilen ve işaretlenen tüm görseller Android private sandbox'ında bırakılmayıp `server.js` (`/api/upload`) üzerinden Termux `/data/data/com.termux/files/home/uploads/` dizinine yüklendi; AI'ın görsellere doğrudan ve eksiksiz erişmesi sağlandı.
- **Oto-Compact Ayarı Kalıcılığı:** `autoCompactEnabled` ve `compactThresholdTokens` değerleri SharedPreferences yükleme/kaydetme döngüsüne (`loadSavedSettings` & `saveSettings`) dahil edildi, ayarların unutulması engellendi.
- **Görsel Üzerine Çizim & İşaretleme Editörü (Image Markup Dialog):** Kırmızı kalem, ok işareti, daire ve dikdörtgen kutu araçları, renk paleti ve kalınlık seçenekleriyle ekran görüntüleri üzerine anında çizim yapıp AI'a görsel talimat verme özelliği eklendi.
- **Tool Çağrı Grubu Sabitliği (Collapse/Expand Override):** Kullanıcı araç çağrı kutusunu daralttığında yeni araç çağrısı gelse veya üretim tamamlansa dahi kullanıcının daraltma tercihi korunarak kutunun kendiliğinden açılıp kapanması önlendi.
- **Tool Çağrıları Birleştirme & Kayıp Önleme:** Akış sırasında ve mesaj tamamlama anında tool çağrılarının tek tek ayrılması veya kaybolması engellendi, tek bir üst kapsayıcıda güvenle toplandı.
