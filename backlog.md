# Antigravity Android Backlog

- Aktif WIP yok.

## [2026-09-22] Terminal Hub Runtime Bağlantıları
- Termux API'lerine gerçek bağlantı eklendi: yönetilen agent süreçleri için PID/CPU/RSS, manifest tabanlı plugin grupları ve kalıcı güvenli schedule kayıtları.
- Schedule kayıtları yalnız bilinen manifest action ID'leri ve gelecekteki timestamp'leri kabul ediyor; arbitrary shell reddediliyor.
- Android AlarmManager `setExactAndAllowWhileIdle` + `TerminalScheduleReceiver` ile AGY/Codex/OpenCode/Cline sabit controller'larını uyku modunda uyandırabiliyor.
- Terminal Hub'da güvenli action seçimi ve dakika bazlı zamanlama formu eklendi; ağ/API/alarm hataları açıkça gösteriliyor.
- `git diff --check` temiz; yerel Gradle çalıştırılmadı.

## [2026-09-22] Terminal Hub Action Grupları ve Swipe Eşiği
- AGY, Codex, OpenCode, Cline ve Vault action'ları servis başına tek grupta gösteriliyor; başlat/kapat kontrolleri aynı servis satırında.
- İki yönlü ekran geçişi için ortak minimum eşik ekranın %25'i veya 96dp; hareket yatay eksende en az 1.2x baskın değilse geçiş yapılmıyor.
- Android geri jestini koruyan yön filtresi ve geçiş animasyonu korunuyor.
- `git diff --check` başarılı; yerel Gradle çalıştırılmadı.

## [2026-09-22] Terminal Hub Agent Servisleri ve Alias Taraması
- Termux alias kaynakları tarandı: AGY, Codex, OpenCode, Cline ve Pi kısayolları doğrulandı; güvenlik nedeniyle serbest biçimli alias çalıştırma eklenmedi.
- Terminal Hub action kataloğuna Codex app-server (`127.0.0.1:4500`), OpenCode serve (`127.0.0.1:4096`) ve Cline web servisleri için sabit start/stop action'ları eklendi.
- Codex ve OpenCode için tmux + `taskset -c 0-5` + `nice -n 15` controller'ları oluşturuldu. Android tarafı mevcut dinamik `/api/actions` kartlarıyla yeni action'ları otomatik gösterir.
- Shell ve Node syntax doğrulandı; servisler test sırasında başlatılmadı.
