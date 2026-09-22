# Antigravity Android - Günlük & Değişiklik Günlüğü (log.md)

## [2026-09-22] Saf HTML Preview ve Görsel Element Inspector
- Saf HTML dosyaları ve Static Web projeleri uygulama içi WebView preview'da açılır hale getirildi.
- Element seçimi; selector, sınırlı HTML/CSS bilgisi ve ekran görüntüsüyle aktif agente gönderiliyor.
- Preview ekran görüntüsü mevcut işaretleme akışına bağlandı; preview durumu backend geçişlerinde korunuyor.
- Güvenli WebMessage origin'i ayrı localhost preview portuna (`127.0.0.1:8081`) bağlandı.
- Commit'ler `a653711` ve `81b07ea`; CI lint/unit `35713116169`, APK build `35713116209` başarılı.

## [2026-09-22] Codex Soğuk Başlangıç ve AGY Sağlık Göstergesi Düzeltmesi
- Codex sekmesi seçildiğinde app-server hazır olana kadar bekleniyor; hazır olduğunda Codex geçmiş/model/kota verileri yeniden yükleniyor.
- Codex ViewModel açılışta eski kalan `agy-web` sağlık durumunu sekme geçişinde yeniden ölçüyor; ortak `serverHealth` alanı Codex ve AGY arasında karıştırılmıyor.
- Codex RUN_COMMAND çağrısına `WAKE_LOCK` ve `KEEP_ALIVE` sinyalleri eklendi.
- Canlı `:4500/readyz` ve `:8080/api/health` kontrolleri başarılı; `git diff --check` temiz. Termux kuralı gereği yerel Gradle çalıştırılmadı.

## [2026-09-21] Codex App-Server Approval Policy Uyumluluk Düzeltmesi
- `CodexBackend.kt` içindeki iki `thread/start` yolu ortak payload üreticisine bağlandı; geçersiz `unlessTrusted` değeri güncel `on-request` politikasıyla değiştirildi.
- `CodexBackendProtocolTest.kt` gerçek JSON serileştirmesinde `approvalPolicy == "on-request"` değerini doğruluyor.
- Payload, kurulu `codex app-server 0.155.1` sürecine gönderildi; sunucu isteği kabul ederek yeni thread ve `approvalPolicy: "on-request"` sonucu döndürdü. Yerel Gradle/Android derlemesi proje kuralı gereği çalıştırılmadı.
- Commit `774ef1a` origin/main'e pushlandı. GitHub Actions lint/unit test koşusu `35567617008` ve imzalı ARM64 APK build/release koşusu `35567617036` başarıyla tamamlandı.

## [2026-09-20] Düşünme Seviyesi Yatay Kaydırma ve Seçici Canlı Yönlendirme (Codex Only Steer)
- `QuickModelSelectorSheet.kt` güncellendi: Reasoning effort çipleri sabit sıkışık satır yerine `horizontalScroll` destekli esnek çiplere dönüştürüldü. `default` (✨ Otomatik), `low` (⚡ Düşük/Hızlı), `medium` (⚖️ Orta/Dengeli), `high` (🧠 Yüksek/Derin), `xhigh` (🚀 Ekstra Yüksek), `max` (🎯 Maksimum) ve `ultra` (🔮 Ultra Derin) rozetleri eksiksiz tanımlandı; dar ekranlarda buton metinlerinin kırpılması/bozulması tamamen önlendi.
- `CodexBackend.kt` içine `turn/steer` RPC çağrısı (`steerPrompt`, `supportsSteer = true`) entegre edildi: Model veya araçlar çalışırken kullanıcının girdiği yönlendirme mesajları Codex app-server'a iletilerek kesintisiz ve sıradaki adımın ardına enjekte ediliyor.
- `AgyBackend.kt` ve `ChatBackend.kt` mimarisi temizlendi: AGY CLI gibi `turn/steer` desteği olmayan backend'lerde `supportsSteer = false` yapıldı; sahte durdur-devam et hilesi kaldırılarak yanıltıcı davranış engellendi.
- `MessageInputBar.kt` ve `ChatScreen.kt` güncellendi: Canlı yönlendirme butonu ve steer placeholder'ı yalnızca aktif backend gerçek steer destekliyorsa (`canSteer = true`, örn. Codex) görünür kılındı. AGY CLI sekmesinde üretim esnasında sahte buton gizlendi, yalnızca Stop butonu gösteriliyor.
- `UiSemanticsTest.kt` içine `quick_model_selector_sheet_renders_efforts` ve `message_input_bar_shows_steer_only_when_supported` semantik doğrulama testleri eklendi.

## [2026-09-20] Kapalı Sunucu Bildirim Spam Engeli, Kapalı Backend'leri Gizleme & 2.5x Büyütülmüş Kırmızı Buton
- `ChatViewModel.kt` içine `isConnectionOrOfflineError()` koruması eklendi: Sunucu kapalıyken veya bağlantı koptuğunda Android cihaz bildirim çubuğuna durmadan hata bildirimi fırlatılması tamamen engellendi; durum yalnız ekranda offline banner olarak gösterilir.
- `FloatingBackendSwitcher.kt` içinde port kontrolüyle dinamik filtreleme yapıldı: Portu kapalı olan sunucuların yüzen balonları listede gizlenir, sadece sunucusu açık/çalışan backend'ler ve seçili olan gösterilir.
- Hamburger menü altındaki kırmızı tetikleyici buton 2.5x kalınlaştırıldı (`38dp × 8.5dp`) ve dokunma alanı (`hitbox`) genişletilerek basış ergonomisi mükemmelleştirildi.

## [2026-09-20] Codex Dinamik Modeller, Reasoning Ağırlıkları ve Canlı RateLimits Kota Entegrasyonu
- `CodexBackend.kt` içinde `getModelsConfig()` güçlendirildi: `model/list` üzerinden `supportedReasoningEfforts` array'leri dinamik olarak toplanarak Düşük (low), Orta (medium), Yüksek (high), Ekstra Yüksek (xhigh), Maksimum (max) ve Ultra (ultra) seviyeleri kullanıcı arayüzüne bağlandı.
- `CodexBackend.kt` içinde `getUsage()` tamamlandı: `account/rateLimits/read` ve `account/read` JSON-RPC çağrıları üzerinden 5 saatlik kayan limit, haftalık limit, kalan yüzde, yenilenme saati, hesap bilgisi ve kullanılabilir kota sıfırlama kredileri (`rateLimitResetCredits`) canlı `UsageData` yapısına dönüştürüldü.
- `UsageWidget.kt` içindeki `formatResetTime()` hem Unix epoch saniyelerini hem de ISO zaman damgalarını pürüzsüz çözümleyecek şekilde güncellendi.
- `UiSemanticsTest.kt` içerisine epoch ve ISO formatResetTime doğrulama testi eklendi.

## [2026-09-20] Hamburger Altı Kırmızı Çizgili Yüzen Backend Balonları (AGY, Codex, OpenCode, Cline)
- Ekranın üstünü kaplayan `WorkspaceTabRow` tamamen kaldırıldı; temiz ve ferah `ChatTopBar` görünümüne dönüldü.
- Hamburger menü ikonunun altına açılır-kapanır tek bir kırmızı tetikleyici çizgi (`FloatingBackendSwitcher`) eklendi.
- Kırmızı çizgiye tıklandığında hafif saydam (`Color(0xFF14171C).copy(alpha = 0.78f)`) arka planlı, dikey sıralanan mini yüzen geçiş balonları (`AGY`, `Codex`, `OpenCode`, `Cline`) yumuşak dikey animasyonla açılıyor/daralıyor.
- Hamburger menü (drawer) açıldığında yüzen balonlar ve kırmızı çizgi otomatik olarak gizleniyor (`!drawerState.isOpen`).
- `ChatWorkspace` 4 backend'in (AGY, Codex, OpenCode, Cline) ViewModel'lerini eşzamanlı ve bağımsız canlı tutacak şekilde güncellendi.
- `UiSemanticsTest` yeni yüzen switcher etkileşimi ve semantik doğrulamasını kapsayacak şekilde uyarlandı.

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
