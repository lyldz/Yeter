# Yeter! — Claude için proje bağlamı

Bu dosya, bu repoda çalışan her Claude oturumunun (VPS'teki Claude Code ve claude.ai oturumları) ortak bağlamıdır. Kalıcı bir karar ya da kural değişince burayı güncelle.

## Uygulama
Boşa para harcama dürtüsüne karşı bir fren. Kullanıcı bir hedef koyar (ör. aylık Claude Pro $20) ve dürtüye direndikçe hedefe yaklaşır. Özellik listesi için README.md'ye bak.

Temel kavramlar:
- **Dürtü akışı:** DUR butonu, 10 dk bekleme, "gerçekten ihtiyacım var mı?" sorgusu. Vazgeçilen tutar birikime eklenir.
- **Canlı birikim:** Harcanmayan her saniye birikime işler. Sabah sorulan günlük tasarruf tutarı gün boyunca saniye saniye birikir.
- **Hedefler:** Birden fazla olabilir; her biri kendi para biriminde ve aylık tekrar eder.
- **Para birimleri:** TRY, USD, EUR, GBP, CHF, JPY, CAD, AUD. Kurlar arka planda güncellenir.
- **Veri:** Tamamı cihazda (SharedPreferences). Sunucu ya da hesap yok.

## Teknik
- Kotlin + Jetpack Compose, tek modül `app`, paket `com.yeter.app`, minSdk 24, targetSdk 34, JDK 17.
- Ana dosyalar (`app/src/main/kotlin/com/yeter/app/`):
  - `YeterScreen.kt`, `YeterViewModel.kt`: ana ekran ve durum
  - `CounterD.kt`: sayaç ve dürtü tarafı
  - `Savings.kt`, `History.kt`: birikim, geçmiş, rozetler
  - `Currency.kt`: kurlar. Birden fazla yedek kaynak var: fawazahmed0 currency-api (jsdelivr + pages.dev), open.er-api, exchangerate-api
  - `Notifier.kt`, `DailyWorker.kt`, `WidgetWorker.kt`: bildirimler ve WorkManager işleri
  - `*Widget.kt`, `WidgetData.kt`, `WidgetUi.kt`: 8 ana ekran widget'ı (Resist, Goal, Today, DaysLeft, Showcase, Vault, Urge, Savings)
  - `Updater.kt`, `UpdateScreen.kt`: GitHub Releases üzerinden uygulama içi güncelleme kontrolü
  - `OnboardingScreen.kt`, `GuideScreen.kt`: ilk açılış ve rehber
- Test: `app/src/test/kotlin/com/yeter/app/CoreLogicTest.kt` → `./gradlew test`

## Sürüm ve yayın (ayrıntı: SURUMLER.md)
- Tek doğruluk kaynağı `version.properties`. Kodda elle sürüm yazılmaz.
- Uygulama koduna dokunan her değişiklikte en az PATCH artar. Yeni özellikte MINOR artar, PATCH sıfırlanır. Büyük yeniden tasarımda MAJOR artar. `VERSION_CODE` her sürümde +1 olur, asla düşmez.
- Sürüm artırırken `version.properties` ve `.github/release-notes.md` güncellenir. Yeni seri açılınca SURUMLER.md tablosu da güncellenir.
- Seri adı: 1.x.x = Kalkan.
- Sürümü kurala göre kendin artır, kullanıcıya sorma.

## CI
- `main` dalına yapılan her push `build-apk.yml`'i çalıştırır: imzalı release APK derlenir ve `v<VERSION_NAME>` etiketiyle GitHub Release oluşturulur ya da güncellenir.
- Diğer dallara push `check.yml`'i çalıştırır: yalnızca derleme kontrolü yapar.
- Uygulamayı değiştirmeyen commit'lerin (yalnızca doküman, ör. bu dosya) mesajına `[skip ci]` ekle. Yoksa mevcut sürümün APK'sı yeniden derlenip üzerine yazılır.

## Çalışma ortamı ve kurallar
- Geliştirme, VPS'te `/root`'tan çalışan Claude Code ile yapılır; oturumlar telefondan yönetilir. Bu repo VPS'te `/root/yeter` klasöründe durur. `projeler` gibi ara ya da kopya klasör açma, klasör yerini sorma.
- Kardeş uygulamalar: `/root/tam-vakti`, `/root/harf-tepsisi`.
- VPS ve claude.ai oturumları GitHub üzerinden senkron olur: işe başlarken `git pull`, iş bitince commit + push.
- İletişim Türkçe, kısa ve doğrudan olsun.
