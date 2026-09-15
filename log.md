# Antigravity Android - Günlük & Değişiklik Günlüğü (log.md)

## [2026-09-15] Görsel Çizim Editörü, Oto-Compact Kalıcılığı ve Tool Gruplama Sabitliği
- **Termux Uploads Köprüsü & Sandbox İzolasyonu Düzeltmesi:** Çizilen ve işaretlenen tüm görseller Android private sandbox'ında bırakılmayıp `server.js` (`/api/upload`) üzerinden Termux `/data/data/com.termux/files/home/uploads/` dizinine yüklendi; AI'ın görsellere doğrudan ve eksiksiz erişmesi sağlandı.
- **Oto-Compact Ayarı Kalıcılığı:** `autoCompactEnabled` ve `compactThresholdTokens` değerleri SharedPreferences yükleme/kaydetme döngüsüne (`loadSavedSettings` & `saveSettings`) dahil edildi, ayarların unutulması engellendi.
- **Görsel Üzerine Çizim & İşaretleme Editörü (Image Markup Dialog):** Kırmızı kalem, ok işareti, daire ve dikdörtgen kutu araçları, renk paleti ve kalınlık seçenekleriyle ekran görüntüleri üzerine anında çizim yapıp AI'a görsel talimat verme özelliği eklendi.
- **Tool Çağrı Grubu Sabitliği (Collapse/Expand Override):** Kullanıcı araç çağrı kutusunu daralttığında yeni araç çağrısı gelse veya üretim tamamlansa dahi kullanıcının daraltma tercihi korunarak kutunun kendiliğinden açılıp kapanması önlendi.
- **Tool Çağrıları Birleştirme & Kayıp Önleme:** Akış sırasında ve mesaj tamamlama anında tool çağrılarının tek tek ayrılması veya kaybolması engellendi, tek bir üst kapsayıcıda güvenle toplandı.
